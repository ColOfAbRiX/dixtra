package com.colofabrix.scala.dixtra.render.context

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.normalize.*
import scala.meta.*
import scala.meta.tokens.Token

/**
 * Turns slices of the original source into documents.
 *
 * Single-line text is emitted already normalised (collapsed whitespace, canonical `=>` spacing) so that width
 * measurements match the final output; multiline text becomes a verbatim block that keeps its relative indentation and
 * never touches the interior of multiline string literals.
 *
 * @param original The full source text being formatted.
 * @param tokens The token index of the same source.
 */
final class SourceDocs(original: String, tokens: SourceTokens) {

  /**
   * The raw source text between two offsets.
   */
  def slice(start: Int, end: Int): String =
    original.substring(start, end)

  /**
   * The 0-based column of a source offset (chars since the previous newline).
   */
  def columnOf(offset: Int): Int =
    offset - SourceEdits.lineStart(original, offset)

  /**
   * The indentation (leading spaces) of the source line containing `offset`.
   */
  def lineIndentOf(offset: Int): Int =
    SourceEdits.indent(original.drop(SourceEdits.lineStart(original, offset)))

  /**
   * A source slice as a document.
   */
  def sourceDoc(start: Int, end: Int): Doc =
    val text = slice(start, end)
    if !text.contains('\n') then Doc.Text(normalizeInline(text))
    else Doc.Verbatim(text, lineIndentOf(start), frozenLines(start, end))

  /**
   * The source text of a tree (with its grouping parentheses) as a document.
   */
  def sourceDoc(tree: Tree): Doc =
    val (start, end) = tokens.span(tree)
    sourceDoc(start, end)

  /**
   * The source text of a tree without its grouping parentheses, for renderers that emit the parentheses themselves.
   */
  def ownSourceDoc(tree: Tree): Doc =
    sourceDoc(tree.pos.start, tree.pos.end)

  /**
   * A source slice with trailing whitespace removed, as a document.
   */
  def trimmedSourceDoc(start: Int, end: Int): Doc =
    sourceDoc(start, start + slice(start, end).stripTrailing.length)

  /**
   * The comment tokens of `comments`, each as a document.
   */
  def commentDocs(comments: Iterable[Token.Comment]): List[Doc] =
    comments.toList.map(c => sourceDoc(c.start, c.end))

  /**
   * True when the source between two offsets holds at least one blank line.
   */
  def hasBlankLine(start: Int, end: Int): Boolean =
    slice(start, end)
      .split("\n", -1)
      .drop(1)
      .dropRight(1)
      .exists(_.trim.isEmpty)

  // Collapse redundant whitespace and canonicalise `=>` in a single-line slice. A fragment ending in `=>` must not gain
  // a trailing space (the fragment's end is not content), so trailing whitespace is only kept if the source had it.
  private def normalizeInline(text: String): String =
    val normalized = ArrowSpacingNormalizer.normalize(WhitespaceNormalizer.normalize(text))
    if text.lastOption.exists(_.isWhitespace) then normalized else normalized.stripTrailing

  // Indices (relative to the slice) of the lines that begin inside a multiline string literal: their content is
  // significant and must never be reindented.
  private def frozenLines(start: Int, end: Int): Set[Int] =
    val stringRanges =
      tokens
        .tokensBetween(start, end)
        .collect {
          case t: Token.Constant.String if t.text.contains('\n')    => (t.start, t.end)
          case t: Token.Interpolation.Part if t.text.contains('\n') => (t.start, t.end)
        }
    val lineStarts =
      slice(start, end).zipWithIndex.collect { case ('\n', i) => start + i + 1 }
    lineStarts
      .zipWithIndex
      .collect {
        case (offset, i) if stringRanges.exists { case (s, e) => offset > s && offset < e } => i + 1
      }
      .toSet

}
