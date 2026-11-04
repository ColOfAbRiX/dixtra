package com.colofabrix.scala.dixtra.normalize.comments

import com.colofabrix.scala.dixtra.normalize.SourceEdits
import scala.meta.tokens.Token

/**
 * The source being normalised, with the line queries the comment rules need.
 *
 * @param source The full source text.
 */
private[normalize] final class CommentSource(val source: String) {

  /**
   * The offset where the line holding `offset` starts.
   */
  def lineStart(offset: Int): Int =
    SourceEdits.lineStart(source, offset)

  /**
   * The offset where the line holding `offset` ends.
   */
  def lineEnd(offset: Int): Int =
    SourceEdits.lineEnd(source, offset)

  /**
   * The column of `offset` on its line.
   */
  def column(offset: Int): Int =
    offset - lineStart(offset)

  /**
   * The width of the line holding `offset`.
   */
  def lineWidth(offset: Int): Int =
    lineEnd(offset) - lineStart(offset)

  /**
   * Whether `comment` is on lines of its own: only whitespace before it on its first line and after it on its last.
   */
  def ownLine(comment: Token): Boolean =
    val before = source.slice(lineStart(comment.start), comment.start)
    val after  = source.slice(comment.end, lineEnd(comment.end))
    before.isBlank && after.isBlank

}
