package com.colofabrix.scala.dixtra.render.context

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.doc.*
import scala.meta.*

/**
 * The shared context for one formatting run.
 *
 * Every renderer receives this context so the source, its tokens and the configuration are captured once rather than
 * threaded through every call. It is a facade: positional token queries come from [[SourceTokens]] and source slices as
 * documents from [[SourceDocs]], both exported here; the context itself adds the width checks and the splicing of
 * rendered children into the source.
 *
 * @param original The full source text being formatted.
 * @param config The active formatting configuration.
 */
final class RenderContext(original: String, val config: Config) {

  private val tokens = new SourceTokens(original)
  private val docs   = new SourceDocs(original, tokens)

  export tokens.*
  export docs.*

  /**
   * True when a fully-decided document renders on one line and fits within the max column at the given indent. A
   * document containing a hard line break never fits: a multiline child always forces its parent to explode.
   */
  def fits(doc: Doc, indent: Int): Boolean =
    Render.flatWidth(doc).exists(fitsWidth(indent, _))

  /**
   * True when `width` more columns from `column` stay within the max column.
   */
  def fitsWidth(column: Int, width: Int): Boolean =
    column + width <= config.maxColumn

  /**
   * True when a document that renders on one line goes past the max column at the given indent. A document over several
   * lines does not overflow: whether it breaks was already decided.
   */
  def overflows(doc: Doc, indent: Int): Boolean =
    Render.flatWidth(doc).exists(w => !fitsWidth(indent, w))

  /**
   * True when every line of `doc`, which starts at `indent`, fits within the maximum width.
   */
  def linesFit(doc: Doc, indent: Int): Boolean =
    (" " * indent + Render.render(doc, indent)).linesIterator.forall(_.length <= config.maxColumn)

  /**
   * True when the source of `tree` starts with `{`: a block the author wrote in braces.
   */
  def writtenWithBraces(tree: Tree): Boolean =
    slice(tree.pos.start, tree.pos.end).startsWith("{")

  /**
   * The column reached after a document that starts at `indent`. For a multiline document only the indent is known (its
   * last line is not tracked).
   */
  def columnAfter(doc: Doc, indent: Int): Int =
    indent + Render.flatWidth(doc).getOrElse(0)

  /**
   * The column at the end of a document that starts at `indent`: after its last line when it is over several lines.
   */
  def endColumn(doc: Doc, indent: Int): Int =
    endColumn(doc, indent, indent)

  /**
   * The same for a document whose first line starts at `column`, its other lines at `indent`.
   */
  def endColumn(doc: Doc, indent: Int, column: Int): Int =
    Render.flatWidth(doc) match {
      case Some(width) =>
        column + width
      case None =>
        Render
          .render(doc, indent)
          .linesIterator
          .toList
          .lastOption
          .fold(indent)(_.length)
    }

  /**
   * The column reached after the document of the source slice `[start, end)` that starts at `indent`. For a multiline
   * slice (e.g. an annotation on its own line) it is the width of the slice's last line, which keeps its absolute
   * source indentation.
   */
  def columnAfterSlice(doc: Doc, start: Int, end: Int, indent: Int): Int =
    Render.flatWidth(doc) match {
      case Some(width) =>
        indent + width
      case None =>
        val text = slice(start, end)
        text.length - text.lastIndexOf('\n') - 1
    }

  /**
   * Replace each span `(start, end, render)` within `[regionStart, regionEnd)` with its rendered form, preserving all
   * the text around the spans (gaps) verbatim. A span need not match a single tree (e.g. a definition together with its
   * `end` marker).
   *
   * Spans must be in source order; one starting inside the previous one is skipped. `render` receives the source column
   * of the span's start, so the internal breaks of each span stay aligned.
   */
  def spliceSpans(regionStart: Int, regionEnd: Int, spans: List[(Int, Int, Int => String)]): String =
    val (sb, cursor) =
      spans.foldLeft((new StringBuilder, regionStart)) { case ((acc, pos), (start, end, render)) =>
        if start >= pos then
          acc.append(original.substring(pos, start))
          acc.append(render(columnOf(start)))
          (acc, end)
        else
          (acc, pos)
      }
    sb.append(original.substring(cursor, regionEnd))
    sb.toString

  /**
   * The brace style of `construct`: the configured one, or for `keep` the one the author wrote (`braced` / `ended` tell
   * whether the source has braces / an `end` marker).
   */
  def braceStyle(construct: Construct, braced: Boolean, ended: Boolean): BracePolicy =
    config.braces(construct) match {
      case BracePolicy.Keep if braced => BracePolicy.ForceBraces
      case BracePolicy.Keep if ended  => BracePolicy.ForceEnd
      case BracePolicy.Keep           => BracePolicy.ForceIndent
      case policy                     => policy
    }

  /**
   * True when `construct` is written with braces, deciding `keep` by `braced`.
   */
  def usesBraces(construct: Construct, braced: Boolean): Boolean =
    braceStyle(construct, braced, ended = false) == BracePolicy.ForceBraces

}
