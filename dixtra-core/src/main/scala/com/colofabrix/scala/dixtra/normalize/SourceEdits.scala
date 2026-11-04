package com.colofabrix.scala.dixtra.normalize

import com.colofabrix.scala.dixtra.SourceDialect
import scala.meta.*
import scala.meta.tokens.Token
import scala.util.control.NonFatal

/**
 * The text edits and the source queries the passes over the formatted text share.
 */
private[dixtra] object SourceEdits {

  /**
   * A replacement of the text in `[start, end)`.
   */
  final case class Edit(start: Int, end: Int, replacement: String)

  /**
   * `source` with `edits` applied; the edits must not overlap, and one given twice applies once.
   */
  def applyEdits(source: String, edits: List[Edit]): String =
    edits
      .distinct
      .sortBy(-_.start)
      .foldLeft(source)((text, e) => text.take(e.start) + e.replacement + text.drop(e.end))

  /**
   * The offset of the start of the line that contains `offset`.
   */
  def lineStart(source: String, offset: Int): Int =
    source.lastIndexOf('\n', offset - 1) + 1

  /**
   * The offset of the end of the line that contains `offset` (its `\n`, or the end of the source).
   */
  def lineEnd(source: String, offset: Int): Int =
    val end = source.indexOf('\n', offset)
    if end < 0 then source.length else end

  /**
   * The number of spaces that `line` starts with.
   */
  def indent(line: String): Int =
    line.takeWhile(_ == ' ').length

  /**
   * Whether `comment`, the text of a comment, is a scaladoc: it opens with a slash and two stars, and is neither an
   * empty block comment nor a banner of stars.
   */
  def isScaladoc(comment: String): Boolean =
    comment.startsWith("/**") &&
    !comment.startsWith("/**/") &&
    !comment.startsWith("/***")

  /**
   * The Scala 3 tokens of `source`, or None when it cannot be tokenised.
   */
  def tokens(source: String): Option[Vector[Token]] =
    given Dialect = SourceDialect.scala
    try {
      source
        .tokenize
        .toOption
        .map(_.toVector)
    } catch {
      case NonFatal(_) => None
    }

}
