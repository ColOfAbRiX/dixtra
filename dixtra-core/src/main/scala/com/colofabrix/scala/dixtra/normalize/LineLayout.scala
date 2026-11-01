package com.colofabrix.scala.dixtra.normalize

import scala.collection.Searching.*
import scala.collection.immutable.BitSet
import scala.meta.*
import scala.meta.tokens.Token

/**
 * Token-derived facts about each line of a source text, shared by the line-oriented normalisers.
 *
 * Line-oriented passes (blank-line collapsing, method separation) must never insert or remove a line break that lies
 * inside a multi-line token — a `"""` string, an interpolation part, or a block/scaladoc comment — because that would
 * change the literal's content or the comment's text. This layout marks such lines so the passes can leave them alone.
 *
 * @param lines The source split on `\n` (trailing empty line preserved).
 * @param insideToken `insideToken(i)` is true when line `i` starts inside a multi-line token, i.e. the line break
 *                    preceding it belongs to a string literal or comment.
 * @param commentOnly `commentOnly(i)` is true when the only non-whitespace tokens touching line `i` are comments.
 */
private[normalize] final case class LineLayout(
  lines: Vector[String],
  insideToken: Vector[Boolean],
  commentOnly: Vector[Boolean],
)

private[normalize] object LineLayout {

  private val extensionHeader =
    """^\s*extension\b""".r

  private val trailingWith =
    """(?:^|\s)with$""".r

  /**
   * True for a line that opens a container or a braceless body: ending with `{`, `:`, `=`, `=>` or `with`, or an
   * `extension` header.
   */
  def isOpener(line: String): Boolean =
    val trimmed = line.trim

    val endsLikeContainer =
      List("{", ":", "=", "=>").exists(trimmed.endsWith) || trailingWith.findFirstIn(trimmed).isDefined

    val isExtensionHeader =
      extensionHeader.findFirstIn(line).isDefined

    endsLikeContainer || isExtensionHeader

  /**
   * Compute the layout of `source`, or `None` if it cannot be tokenised.
   */
  def of(source: String): Option[LineLayout] =
    SourceEdits.tokens(source).map(build(source, _))

  private def build(source: String, tokens: Vector[Token]): LineLayout =
    val lines      = source.split("\n", -1).toVector
    val lineStarts = lines.scanLeft(0)((start, line) => start + line.length + 1).dropRight(1)

    // Index of the line containing the character at `offset`.
    def lineOf(offset: Int): Int =
      lineStarts.search(offset) match {
        case Found(i)          => i
        case InsertionPoint(i) => i - 1
      }

    // Each token with content: whether it is a comment, its first and last line.
    val spans =
      tokens
        .filter(t => t.text.nonEmpty && !t.is[Token.Whitespace])
        .map(t => (t.is[Token.Comment], lineOf(t.start), lineOf(math.max(t.start, t.end - 1))))

    val inside = BitSet.fromSpecific(spans.iterator.flatMap((_, first, last) => first + 1 to last))

    val commented =
      BitSet.fromSpecific(
        spans
          .iterator
          .flatMap(
            (isComment, first, last) =>
              if isComment then first to last else Nil,
          ),
      )
    val coded =
      BitSet.fromSpecific(
        spans
          .iterator
          .flatMap(
            (isComment, first, last) =>
              if isComment then Nil else first to last,
          ),
      )

    LineLayout(
      lines,
      lines.indices.map(inside).toVector,
      lines.indices.map(i => commented(i) && !coded(i)).toVector,
    )

}
