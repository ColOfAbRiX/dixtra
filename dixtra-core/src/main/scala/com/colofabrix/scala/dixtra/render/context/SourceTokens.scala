package com.colofabrix.scala.dixtra.render.context

import com.colofabrix.scala.dixtra.normalize.SourceEdits
import scala.meta.*
import scala.meta.tokens.Token

/**
 * The token stream of the source being formatted, indexed for positional queries.
 *
 * The source is tokenised once; token offsets line up with tree positions because the tree was parsed from the same
 * text. Lookups use binary search, so the renderers can freely ask for the tokens, comments or keywords in any range,
 * and for the true span of a tree including its grouping parentheses.
 *
 * @param original The full source text being formatted.
 */
final class SourceTokens(original: String) {

  private val tokens: Vector[Token] =
    SourceEdits.tokens(original).getOrElse(Vector.empty)

  /**
   * The tokens lying entirely within `[start, end)`, in source order.
   */
  def tokensBetween(start: Int, end: Int): Vector[Token] =
    tokens
      .iterator
      .drop(firstTokenAtOrAfter(start))
      .takeWhile(_.start < end)
      .filter(t => t.end <= end && t.end > t.start)
      .toVector

  /**
   * The comment tokens lying entirely within `[start, end)`.
   */
  def commentsBetween(start: Int, end: Int): Vector[Token.Comment] =
    tokensBetween(start, end).collect { case c: Token.Comment => c }

  /**
   * True when the source text of `tree` contains a comment that sits in a gap between its direct children, i.e. text
   * that a renderer rebuilding the node from its children would silently drop.
   */
  def hasGapComments(tree: Tree): Boolean =
    val (start, end) = span(tree)
    val childSpans   = tree.children.map(span)
    commentsBetween(start, end).exists { c =>
      !childSpans.exists { case (cs, ce) => c.start >= cs && c.end <= ce }
    }

  /**
   * The last `=` token in `[start, end)`, if any. Used to find the `=` that separates a definition's header from its
   * body.
   */
  def equalsBefore(start: Int, end: Int): Option[Token] =
    tokensBetween(start, end).reverseIterator.collectFirst { case t: Token.Equals => t }

  /**
   * The last `=>` token in `[start, end)`, if any.
   */
  def arrowBefore(start: Int, end: Int): Option[Token] =
    tokensBetween(start, end).reverseIterator.collectFirst { case t: Token.RightArrow => t }

  /**
   * The last `.` token in `[start, end)`, if any.
   */
  def dotBefore(start: Int, end: Int): Option[Token] =
    tokensBetween(start, end).reverseIterator.collectFirst { case t: Token.Dot => t }

  /**
   * The source span of a tree including any redundant grouping parentheses that Scalameta leaves out of its position,
   * e.g. `(a + b)` in `(a + b).foo`. Parentheses that coincide with the parent's own delimiters (such as an argument
   * clause) are not included.
   */
  def span(tree: Tree): (Int, Int) =
    val parentSpan = tree.parent.map(p => (p.pos.start, p.pos.end))

    def loop(start: Int, end: Int): (Int, Int) =
      (previousSignificant(start), nextSignificant(end)) match {
        case (Some(open: Token.LeftParen), Some(close: Token.RightParen)) =>
          val within =
            parentSpan.forall { case (ps, pe) =>
              open.start >= ps && close.end <= pe && !(open.start == ps && close.end == pe)
            }
          if within && matchingParen(open).contains(close) then loop(open.start, close.end)
          else (start, end)
        case _ =>
          (start, end)
      }
    if tree.pos == Position.None then (0, 0) else loop(tree.pos.start, tree.pos.end)

  /**
   * True when the tree is wrapped in redundant grouping parentheses.
   */
  def isParenthesized(tree: Tree): Boolean =
    span(tree)._1 != tree.pos.start

  /**
   * The last token that is not whitespace or a comment ending before `offset`.
   */
  def previousSignificant(offset: Int): Option[Token] =
    (firstTokenAtOrAfter(offset) - 1 to 0 by -1)
      .iterator
      .map(tokens)
      .find(t => t.end <= offset && !isTrivia(t))

  private def nextSignificant(offset: Int): Option[Token] =
    tokens
      .iterator
      .drop(firstTokenAtOrAfter(offset))
      .find(t => !isTrivia(t))

  // Index of the first token starting at or after `offset` (binary search).
  private def firstTokenAtOrAfter(offset: Int): Int =
    @annotation.tailrec
    def search(lo: Int, hi: Int): Int =
      if lo >= hi then
        lo
      else
        val mid = (lo + hi) / 2
        if tokens(mid).start < offset then search(mid + 1, hi) else search(lo, mid)
    search(0, tokens.length)

  private def isTrivia(t: Token): Boolean =
    t match {
      case _: Token.Whitespace | _: Token.Comment => true
      case _                          => false
    }

  // The closing parenthesis matching an opening one, found by depth counting.
  private def matchingParen(open: Token.LeftParen): Option[Token] =
    val after = tokens.iterator.drop(firstTokenAtOrAfter(open.end))
    val (_, found) =
      after.foldLeft((1, Option.empty[Token])) { case ((depth, result), t) =>
        if result.isDefined then
          (depth, result)
        else
          t match {
            case _: Token.LeftParen =>
              (depth + 1, None)
            case _: Token.RightParen =>
              if depth == 1 then (0, Some(t)) else (depth - 1, None)
            case _ =>
              (depth, None)
          }
      }
    found

}
