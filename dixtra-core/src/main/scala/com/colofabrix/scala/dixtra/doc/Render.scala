package com.colofabrix.scala.dixtra.doc

/**
 * Renders a fully-decided [[Doc]] to a string, and measures documents for the fits check of the renderers
 * (`RenderContext.fits`).
 *
 * The renderer performs no layout decisions: it simply walks the IR, emitting text and honouring hard `Line` breaks at
 * the accumulated indentation. Two consecutive `Line`s produce a blank line.
 */
object Render {

  /**
   * Render a document to its final string form.
   */
  def render(doc: Doc): String =
    render(doc, baseIndent = 0)

  /**
   * Render a document whose continuation lines start at the given base indent.
   *
   * The first line is not prefixed (its column is set by surrounding context);
   * every hard line break emits `baseIndent` plus any accumulated nesting. This lets nested members (e.g. inside
   * `object A { ... }`) keep their absolute source indentation while their internal breaks stay correctly aligned.
   */
  def render(doc: Doc, baseIndent: Int): String =
    val sb = new StringBuilder
    go(doc, indent = baseIndent, sb)
    sb.toString

  /**
   * True when the document renders on a single line: it holds no hard line break.
   */
  def isFlat(doc: Doc): Boolean =
    flatWidth(doc).isDefined

  /**
   * The width of a document rendered as a single line, or None if it contains a hard line break (and therefore cannot
   * be a single line). Used by the renderers to decide the overflow trigger.
   */
  def flatWidth(doc: Doc): Option[Int] =
    doc match {
      case Doc.Empty =>
        Some(0)
      case Doc.Text(value) =>
        if value.contains('\n') then None else Some(value.length)
      case Doc.Line =>
        None
      case Doc.Standalone(_) =>
        None
      case Doc.Nest(_, inner) =>
        flatWidth(inner)
      case Doc.Concat(l, r) =>
        for
          lw <- flatWidth(l)
          rw <- flatWidth(r)
        yield
          lw + rw
      case Doc.Verbatim(value, _, _) =>
        if value.contains('\n') then None else Some(value.length)
    }

  private def go(doc: Doc, indent: Int, sb: StringBuilder): Unit =
    doc match {
      case Doc.Empty =>
        ()
      case Doc.Text(value) =>
        sb.append(value)
        ()
      case Doc.Concat(l, r) =>
        go(l, indent, sb)
        go(r, indent, sb)
      case Doc.Line =>
        trimTrailingSpaces(sb)
        sb.append('\n')
        sb.append(" " * indent)
        ()
      case Doc.Nest(spaces, inner) =>
        go(inner, indent + spaces, sb)
      case Doc.Standalone(inner) =>
        go(inner, indent, sb)
      case Doc.Verbatim(value, sourceIndent, frozenLines) =>
        appendVerbatim(value, sourceIndent, frozenLines, indent, sb)
    }

  // A line never ends with spaces: two consecutive breaks leave an empty line, not one holding the indentation.
  private def trimTrailingSpaces(sb: StringBuilder): Unit =
    while (sb.lastOption.contains(' ')) sb.setLength(sb.length - 1)

  // Emit a raw slice. Each continuation line keeps its indentation relative to
  // the source line the slice started on, re-based onto the current indent;
  // frozen lines (string literal content) and blank lines are emitted as-is.
  private def appendVerbatim(
    value: String,
    sourceIndent: Int,
    frozenLines: Set[Int],
    indent: Int,
    sb: StringBuilder,
  ): Unit =
    val lines = value.split("\n", -1)
    lines
      .zipWithIndex
      .foreach { case (line, i) =>
        if i == 0 then
          sb.append(line)
        else
          sb.append('\n')
          if frozenLines.contains(i) || line.trim.isEmpty then
            sb.append(line)
          else
            val leading  = line.takeWhile(_ == ' ').length
            val relative = math.max(0, leading - sourceIndent)
            sb.append(" " * (indent + relative))
            sb.append(line.drop(leading))
        ()
      }

}
