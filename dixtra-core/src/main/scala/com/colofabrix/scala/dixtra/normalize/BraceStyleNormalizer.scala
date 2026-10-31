package com.colofabrix.scala.dixtra.normalize

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.normalize.SourceEdits.Edit
import com.colofabrix.scala.dixtra.verify.Equivalence
import scala.collection.Searching.Found
import scala.meta.*
import scala.meta.tokens.Token
import scala.util.control.NonFatal

/**
 * Brace indentation style normalisation (`braces.style`).
 *
 * The renderers write braces in the 1TBS style; this pass moves the braces of the whole file to the configured style:
 *
 *   - `1tbs`: an opening `{` on its own line joins the line before it, and a continuation keyword (`else`, `catch`,
 *     `finally`, `yield`) starting the line after a closing `}` joins that line (`} else {`), when the joined line
 *     fits.
 *   - `stroustrup`: like 1TBS, except that a continuation keyword after a closing `}` starts its own line at the indent
 *     of the `}`.
 *   - `allman`: like Stroustrup, and every opening `{` ending a line that has other code goes on its own line at the
 *     indent of its closing `}`.
 *
 * Only the braces of bodies change: templates, packages, blocks, the cases of a `match` / `catch` and the enumerators
 * of a `for`. The braces of an argument (`xs.map { x => ... }`), of a body after `=>` and of a block standing as a
 * statement stay where they are, and so does a brace with a comment next to it, a one-line `{ ... }` and a `}` that
 * does not start its line.
 *
 * The pass parses the text: if it cannot, or if the result would not be equivalent to the input, the input is returned
 * unchanged (safety net).
 */
object BraceStyleNormalizer {

  /**
   * Apply the brace indentation `style` to `source`, keeping joined lines within `maxColumn`.
   */
  def normalize(source: String, style: BracesStyle, maxColumn: Int): String =
    normalize(source, style, maxColumn, SourceDialect.scala)

  /**
   * Apply the brace indentation `style` to `source`, a whole file of `dialect`, keeping joined lines within
   * `maxColumn`.
   */
  def normalize(source: String, style: BracesStyle, maxColumn: Int, dialect: Dialect): String =
    try {
      parse(source, dialect).fold(source) { tree =>
        val text  = new Text(source, tree.tokens.filter(t => t.end > t.start).toVector)
        val edits = continuations(text, tree, style, maxColumn) ++ openings(text, tree, style, maxColumn)
        val out   = SourceEdits.applyEdits(source, edits)

        val noEdits       = edits.isEmpty
        val notEquivalent = !Equivalence.isEquivalentSource(tree, source, out, dialect)

        if noEdits || notEquivalent then source
        else out
      }
    } catch {
      case NonFatal(_) => source
    }

  // The source with its non-empty tokens, and the token queries the rules need.
  private final class Text(val source: String, tokens: Vector[Token]) {

    // The tokens are in source order and not empty, so their starts are sorted and distinct.
    private val starts: Vector[Int] =
      tokens.map(_.start)

    // The index of the token starting at `offset`.
    private def index(offset: Int): Option[Int] =
      starts.search(offset) match {
        case Found(i) => Some(i)
        case _        => None
      }

    def lineStart(offset: Int): Int =
      SourceEdits.lineStart(source, offset)

    def lineEnd(offset: Int): Int =
      SourceEdits.lineEnd(source, offset)

    def indentOf(offset: Int): Int =
      SourceEdits.indent(source.slice(lineStart(offset), offset))

    def startsLine(offset: Int): Boolean =
      source.slice(lineStart(offset), offset).isBlank

    def at(offset: Int): Option[Token] =
      index(offset).map(tokens)

    // The nearest token before (or after) the one starting at `offset` that is not whitespace; a comment counts.
    def previous(offset: Int): Option[Token] =
      index(offset).flatMap { i =>
        (i - 1 to 0 by -1)
          .iterator
          .map(tokens)
          .find(!_.is[Token.Whitespace])
      }

    def next(offset: Int): Option[Token] =
      index(offset).flatMap { i =>
        (i + 1 until tokens.length)
          .iterator
          .map(tokens)
          .find(!_.is[Token.Whitespace])
      }

  }

  private def parse(source: String, dialect: Dialect): Option[Source] =
    given Dialect = dialect
    source.parse[Source].toOption

  // The continuation keywords following a closing `}` of their own construct: joined to it (1TBS) or moved to their own
  // line (Stroustrup, Allman).
  private def continuations(text: Text, tree: Tree, style: BracesStyle, maxColumn: Int): List[Edit] =
    descendants(tree)
      .flatMap {
        case term: Term.If       => List(term.thenp).filter(_.is[Term.Block])
        case term: Term.Try      => List(term.expr).filter(_.is[Term.Block]) ++ term.catchClause.toList
        case term: Term.ForYield => List(term.enumsBlock)
        case _                   => Nil
      }
      .flatMap { closed =>
        // A construct without braces can still end with a `}`, as the enumerators of a braceless `for` ending with a
        // braced `match`: the keyword after it is not a continuation of a brace
        val close = closed.pos.end - 1
        for
          _ <- text.at(closed.pos.start).filter(_.is[Token.LeftBrace])
          brace <- text.at(close).filter(_.is[Token.RightBrace])
          kw <- text.next(brace.start).filter(isContinuation)
          edit <- continuation(text, brace, kw, style, maxColumn)
        yield edit
      }

  private def continuation(text: Text, brace: Token, kw: Token, style: BracesStyle, maxColumn: Int): Option[Edit] =
    val gap = text.source.slice(brace.end, kw.start)
    style match {
      case BracesStyle.OneTrueBrace =>
        val joined = (brace.end - text.lineStart(brace.end)) + 1 + (text.lineEnd(kw.start) - kw.start)
        Option.when(gap.contains('\n') && joined <= maxColumn) {
          Edit(brace.end, kw.start, " ")
        }
      case BracesStyle.Stroustrup | BracesStyle.Allman =>
        Option.when(!gap.contains('\n') && text.startsLine(brace.start)) {
          Edit(brace.end, kw.start, "\n" + " " * text.indentOf(brace.start))
        }
    }

  private def isContinuation(token: Token): Boolean =
    token.is[Token.KwElse] || token.is[Token.KwCatch] || token.is[Token.KwFinally] || token.is[Token.KwYield]

  // The opening braces of bodies: put on their own line (Allman) or joined to the line before them (1TBS, Stroustrup).
  private def openings(text: Text, tree: Tree, style: BracesStyle, maxColumn: Int): List[Edit] =
    descendants(tree)
      .filter {
        case b @ (_: Template.Body | _: Pkg.Body | _: Term.Block | _: Term.CasesBlock | _: Term.EnumeratorsBlock | _: Term.PartialFunction) =>
          isBodyBrace(text, b)
        case _ =>
          false
      }
      .flatMap { body =>
        for
          open  <- text.at(body.pos.start)
          close <- text.at(body.pos.end - 1)
          prev  <- text.previous(open.start).filterNot(_.is[Token.Comment])
          edit  <- style match {
                     case BracesStyle.Allman => allmanOpening(text, prev, open, close)
                     case _                  => joinedOpening(text, prev, open, maxColumn)
                   }
        yield edit
      }

  // A brace of a body: written as `{ ... }`, not an argument, not inside an interpolation, not after `=>`, and not a
  // block standing as a statement.
  private def isBodyBrace(text: Text, body: Tree): Boolean =
    val braced =
      text.source.lift(body.pos.start).contains('{') && text.source.lift(body.pos.end - 1).contains('}')
    val parent =
      body.parent
    val inArgumentOrString =
      parent.exists(p => p.is[Term.ArgClause] || p.is[Term.Interpolate])

    val asStatement =
      parent.exists {
        case p: Term.Block    => p.stats.contains(body)
        case p: Template.Body => p.stats.contains(body)
        case p: Pkg.Body      => p.stats.contains(body)
        case p: Source        => p.stats.contains(body)
        case _                => false
      }

    val afterArrow =
      text
        .previous(body.pos.start)
        .exists { token =>
          token.is[Token.RightArrow] || token.is[Token.ContextArrow]
        }

    braced && !inArgumentOrString && !asStatement && !afterArrow

  // Allman: a `{` ending a line with other code moves to its own line, at the indent of its closing `}` when that
  // starts its line.
  private def allmanOpening(text: Text, prev: Token, open: Token, close: Token): Option[Edit] =
    val endsLine =
      text
        .next(open.start)
        .forall { token =>
          text
            .source
            .slice(open.end, token.start)
            .contains('\n')
        }

    Option.when(endsLine && !text.startsLine(open.start) && text.startsLine(close.start)) {
      Edit(prev.end, open.start, "\n" + " " * text.indentOf(close.start))
    }

  // 1TBS and Stroustrup: a `{` starting its line joins the line before it.
  private def joinedOpening(text: Text, prev: Token, open: Token, maxColumn: Int): Option[Edit] =
    val width    = (prev.end - text.lineStart(prev.end)) + 1 + (text.lineEnd(open.start) - open.start)
    val gap      = text.source.slice(prev.end, open.start)
    val blankGap = gap.contains('\n') && gap.isBlank

    Option.when(text.startsLine(open.start) && blankGap && width <= maxColumn) {
      Edit(prev.end, open.start, " ")
    }

   // `tree` and all the trees below it.
   private def descendants(tree: Tree): List[Tree] =
     preorder(tree).toList

   private def preorder(tree: Tree): Iterator[Tree] =
     Iterator.single(tree) ++ tree.children.iterator.flatMap(preorder)

}
