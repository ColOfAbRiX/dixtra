package com.colofabrix.scala.dixtra.normalize

import com.colofabrix.scala.dixtra.ScaladocSpacing

/**
 * Blank-line-before-scaladoc normalisation, by `blank-lines.before-scaladoc`.
 *
 * With `always`, a scaladoc (or javadoc) comment starting its own line has a blank line before it (with
 * `always-multiline`, only a multi-line one), except at the start of the file and directly after a line that opens a
 * block (see [[LineLayout.isOpener]]): there only a multiline scaladoc gets one, while a single-line `/** ... */` stays
 * glued to the opener (the edges of a container body follow `blank-lines.body-edges`). With `never`, the blank lines
 * before a scaladoc that follows another line of code are removed, except after an opener or a package clause. With
 * `keep`, nothing changes. Other comments never cause a blank line to be added or removed.
 *
 * The pass is token-aware: a line that starts inside a multi-line string or comment is never a scaladoc start. If the
 * text cannot be tokenised, the input is returned unchanged (safety net).
 */
object DocCommentSpacingNormalizer {

  /**
   * Add or remove the blank line before each scaladoc by `policy`.
   */
  def normalize(source: String, policy: ScaladocSpacing): String =
    policy match {
      case ScaladocSpacing.Always | ScaladocSpacing.AlwaysMultiline =>
        LineLayout.of(source).fold(source)(separate(_, policy))
      case ScaladocSpacing.Never =>
        LineLayout.of(source).fold(source)(glue)
      case ScaladocSpacing.Keep =>
        source
    }

  /**
   * The source without the blank lines before its scaladocs when `policy` is `never`, unchanged otherwise: applied
   * before rendering, so the layout (the alignment groups a blank line ends) sees them removed.
   */
  def glued(source: String, policy: ScaladocSpacing): String =
    if policy == ScaladocSpacing.Never then
      LineLayout.of(source).fold(source)(glue)
    else
      source

  private def separate(layout: LineLayout, policy: ScaladocSpacing): String =
    val lines = layout.lines

    val out =
      lines
        .indices
        .flatMap { i =>
          if needsBlankBefore(layout, i, policy) then Vector("", lines(i)) else Vector(lines(i))
        }

    out.mkString("\n")

  // The source without the blank lines between a line of code and the scaladoc after it, unless that line opens a block
  // or is a package clause.
  private def glue(layout: LineLayout): String =
    val lines = layout.lines

    val removed =
      lines
        .indices
        .filter(isDoc(layout, _))
        .flatMap { i =>
          val blanks   = (i - 1 to 0 by -1).takeWhile(j => lines(j).isBlank && !layout.insideToken(j))
          val previous = lines.lift(i - 1 - blanks.size)
          val glued    = previous.exists(p => !LineLayout.isOpener(p) && !p.trim.startsWith("package "))
          if glued then blanks else Nil
        }
        .toSet

    lines.indices.filterNot(removed).map(lines).mkString("\n")

  private def isDoc(layout: LineLayout, i: Int): Boolean =
    !layout.insideToken(i) && SourceEdits.isScaladoc(layout.lines(i).trim)

  private def needsBlankBefore(layout: LineLayout, i: Int, policy: ScaladocSpacing): Boolean =
    val line    = layout.lines(i).trim
    val noBlank = !isDoc(layout, i) || i == 0 || !policy.wantsBlank(line)

    if noBlank then
      false
    else
      val previous = layout.lines(i - 1)
      if previous.trim.isEmpty then
        false
      else if LineLayout.isOpener(previous) then
        !line.contains("*/")
      else
        true

}
