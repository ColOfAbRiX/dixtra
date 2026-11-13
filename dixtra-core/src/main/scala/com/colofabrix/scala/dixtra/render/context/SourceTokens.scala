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

  // The offsets of the tokens, searched without going through the tokens themselves
  private val starts: Array[Int] =
    tokens
      .iterator
      .map(_.start)
      .toArray

  private val ends: Array[Int] =
    tokens
      .iterator
      .map(_.end)
      .toArray

  // The comments alone, in source order: the renderers ask for the comments of a range far more often than for its
  // tokens
  private val comments: Vector[Token.Comment] =
    tokens.collect { case c: Token.Comment => c }

  private val commentStarts: Array[Int] =
    comments
      .iterator
      .map(_.start)
      .toArray

  // The span of each tree already asked for: the renderers ask again for the same trees, and a span is computed by
  // walking the tokens around the tree. Keyed by identity, as a tree is a position in this source.
  private val spans =
    new java.util.IdentityHashMap[Tree, (Int, Int)]

  /**
   * The tokens lying entirely within `[start, end)`, in source order.
   */
  def tokensBetween(start: Int, end: Int): Vector[Token] =
    tokens
      .slice(firstAtOrAfter(starts, start), firstAtOrAfter(starts, end))
      .filter(t => t.end <= end && t.end > t.start)

  /**
   * The comment tokens lying entirely within `[start, end)`.
   */
  def commentsBetween(start: Int, end: Int): Vector[Token.Comment] =
    comments
      .slice(firstAtOrAfter(commentStarts, start), firstAtOrAfter(commentStarts, end))
      .filter(_.end <= end)

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
    Option(spans.get(tree)).getOrElse {
      val computed = spanOf(tree)
      spans.put(tree, computed)
      computed
    }

  private def spanOf(tree: Tree): (Int, Int) =
    val parentSpan = tree.parent.map(p => (p.pos.start, p.pos.end))

    def loop(start: Int, end: Int): (Int, Int) =
      (previousSignificant(start), nextSignificant(end)) match {
        case (Some(open: Token.LeftParen), Some(close: Token.RightParen)) =>
          val within =
            parentSpan.forall { case (ps, pe) =>
              val insideParent = open.start >= ps && close.end <= pe
              val ownParens    = open.start == ps && close.end == pe

              insideParent && !ownParens
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
    @annotation.tailrec
    def loop(i: Int): Option[Token] =
      if i < 0 then
        None
      else if ends(i) <= offset && !isTrivia(tokens(i)) then
        Some(tokens(i))
      else
        loop(i - 1)
    loop(firstAtOrAfter(starts, offset) - 1)

  private def nextSignificant(offset: Int): Option[Token] =
    @annotation.tailrec
    def loop(i: Int): Option[Token] =
      if i >= tokens.length then
        None
      else if !isTrivia(tokens(i)) then
        Some(tokens(i))
      else
        loop(i + 1)
    loop(firstAtOrAfter(starts, offset))

  // Index of the first of the `sorted` offsets (of tokens in source order) at or after `offset` (binary search).
  private def firstAtOrAfter(sorted: Array[Int], offset: Int): Int =
    @annotation.tailrec
    def search(lo: Int, hi: Int): Int =
      if lo >= hi then
        lo
      else
        val mid = (lo + hi) >>> 1
        if sorted(mid) < offset then search(mid + 1, hi) else search(lo, mid)

    search(0, sorted.length)

  private def isTrivia(t: Token): Boolean =
    t match {
      case _: Token.Whitespace | _: Token.Comment => true
      case _                                      => false
    }

  // The closing parenthesis matching an opening one, found by depth counting, stopping at it.
  private def matchingParen(open: Token.LeftParen): Option[Token] =
    @annotation.tailrec
    def loop(i: Int, depth: Int): Option[Token] =
      if i >= tokens.length then
        None
      else
        tokens(i) match {
          case _: Token.LeftParen =>
            loop(i + 1, depth + 1)
          case close: Token.RightParen =>
            if depth == 1 then Some(close) else loop(i + 1, depth - 1)
          case _ =>
            loop(i + 1, depth)
        }
        
    loop(firstAtOrAfter(starts, open.end), 1)

}
