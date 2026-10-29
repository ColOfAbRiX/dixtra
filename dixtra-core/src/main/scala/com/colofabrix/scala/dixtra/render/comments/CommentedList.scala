package com.colofabrix.scala.dixtra.render.comments

import com.colofabrix.scala.dixtra.TrailingCommas
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*
import scala.meta.tokens.Token

/**
 * The comments found between the elements of a delimited, comma-separated list (arguments, parameters, type
 * parameters), assigned to the element they belong to so the list can be laid out inline or exploded without losing
 * them.
 *
 * Each comment in a gap is classified as:
 *   - a prefix of the next element: a block comment with nothing but spaces between it and that element (`/* note */
 *     b`);
 *   - trailing the previous element: on the same line as the end of that element, emitted after its comma (`a, //
 *     first`);
 *   - an own-line comment: anything else, placed on its own line before the next element (or before the closing
 *     delimiter, after the last element).
 *
 * A line comment, or any own-line comment, forces the list to explode.
 *
 * @param prefixes Per element, the block comments glued before it.
 * @param ownLines Per element, the comments on their own lines before it.
 * @param trailings Per element, the comments following it on its line.
 * @param closing The own-line comments after the last element.
 * @param forcesBreak True when the list cannot be laid out on a single line.
 * @param trailingComma True when the last element of the exploded list is followed by a comma (`trailing-commas`).
 */
private[render] final case class CommentedList(
  prefixes: List[Doc],
  ownLines: List[List[Doc]],
  trailings: List[Doc],
  closing: List[Doc],
  forcesBreak: Boolean,
  trailingComma: Boolean,
) {

  /**
   * True when the list carries no comment at all.
   */
  def isEmpty: Boolean =
    val noInlineComments      = closing.isEmpty && ownLines.forall(_.isEmpty)
    val noSurroundingComments = (prefixes ++ trailings).forall(_ == Doc.Empty)

    noInlineComments && noSurroundingComments

  /**
   * The elements on one line, comma-separated, with their comments (the enclosing delimiters are emitted by the
   * caller).
   */
  def inline(elements: List[Doc]): Doc =
    Doc.intercalate(Doc.text(" "), lines(elements, trailingComma = false))

  /**
   * The elements one per line, each followed by a comma (the last one too with `trailingComma`), with own-line comments
   * on their own lines. The caller emits the first line break and the delimiters, the closing one on a line of its own.
   */
  def exploded(elements: List[Doc]): Doc =
    Doc.intercalate(
      Doc.Line,
      ownLines.zip(lines(elements, trailingComma)).flatMap { case (own, line) => own :+ line } ++ closing,
    )

  /**
   * The elements one per line like [[exploded]], without a comma after the last one so that the closing delimiter can
   * follow it on its line; None when a comment follows the last element.
   */
  def explodedClosedOnLast(elements: List[Doc]): Option[Doc] =
    Option.when(closing.isEmpty && trailings.lastOption.forall(_ == Doc.Empty)) {
      Doc.intercalate(
        Doc.Line,
        ownLines.zip(lines(elements, trailingComma = false)).flatMap { case (own, line) => own :+ line },
      )
    }

  /**
   * True when the first element can be glued onto the opening line.
   */
  def firstCanGlue: Boolean =
    ownLines.headOption.forall(_.isEmpty)

  // Each element with its prefix comments, its comma, and its trailing comments.
  private def lines(elements: List[Doc], trailingComma: Boolean): List[Doc] =
    val last = elements.length - 1
    elements
      .zipWithIndex
      .map { case (element, i) =>
        val comma = if i < last || trailingComma then Doc.text(",") else Doc.Empty
        prefixes(i) ++ element ++ comma ++ trailings(i)
      }

}

private[render] object CommentedList {

  /**
   * Classify the comments of a list delimited by `[open, close)` (the offsets of the whole list, delimiters included)
   * whose elements are `elements`.
   */
  def of(ctx: RenderContext, open: Int, elements: List[Tree], close: Int): CommentedList =
    val bounds = elements.map(ctx.span)
    val starts = bounds.map(_._1)
    val ends   = bounds.map(_._2)
    val gaps   = (open :: ends).zip(starts :+ close)

    val classified =
      gaps
        .zipWithIndex
        .map { case ((gapStart, gapEnd), i) =>
          val prevEnd   = if i == 0 then None else Some(gapStart)
          val nextStart = if i < elements.length then Some(gapEnd) else None
          ctx.commentsBetween(gapStart, gapEnd).toList.map(c => (c, classify(ctx, c, prevEnd, nextStart)))
        }

    def docs(comments: List[(Token.Comment, Kind)], kind: Kind): List[Doc] =
      comments.collect { case (c, k) if k == kind => ctx.sourceDoc(c.start, c.end) }

    def spaced(ds: List[Doc]): Doc =
      Doc.concat(ds.map(d => Doc.text(" ") ++ d))

    val prefixes =
      elements
        .indices
        .toList
        .map(i => Doc.concat(docs(classified(i), Kind.Prefix).map(_ ++ Doc.text(" "))))
    val ownLines =
      elements
        .indices
        .toList
        .map(i => docs(classified(i), Kind.OwnLine))
    val trailings =
      elements
        .indices
        .toList
        .map(i => spaced(docs(classified(i + 1), Kind.Trailing)))
    val closing =
      classified
        .lastOption
        .map(docs(_, Kind.OwnLine))
        .getOrElse(Nil)

    val all         = classified.flatten
    val lineComment = all.exists { case (c, _) => isLineComment(c) }
    val ownLine     = all.exists { case (_, k) => k == Kind.OwnLine }

    CommentedList(
      prefixes,
      ownLines,
      trailings,
      closing,
      forcesBreak = lineComment || ownLine,
      trailingComma = ctx.config.trailingCommas == TrailingCommas.Always,
    )

  private enum Kind {
    case Prefix, Trailing, OwnLine
  }

  private def classify(ctx: RenderContext, c: Token.Comment, prevEnd: Option[Int], nextStart: Option[Int]): Kind =
    val gluedToNext =
      !isLineComment(c) && nextStart.exists(n => !ctx.slice(c.end, n).contains('\n'))
    val sameLineAsPrev = prevEnd.exists(p => !ctx.slice(p, c.start).contains('\n'))
    if gluedToNext then Kind.Prefix
    else if sameLineAsPrev then Kind.Trailing
    else Kind.OwnLine

  private def isLineComment(c: Token.Comment): Boolean =
    c.text.startsWith("//")

}
