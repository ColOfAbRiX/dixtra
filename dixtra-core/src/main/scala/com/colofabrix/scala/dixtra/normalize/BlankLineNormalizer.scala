package com.colofabrix.scala.dixtra.normalize

/**
 * Blank-line normalisation.
 *
 * Collapses runs of two or more consecutive blank lines (empty or whitespace-only) into a single blank line, preserving
 * all non-blank content and the file's final newline shape.
 *
 * The pass is token-aware: blank lines that lie inside a multi-line token (a `"""` string, an interpolation part, or a
 * block comment) are part of that token's content and are never removed. If the text cannot be tokenised, the input is
 * returned unchanged (safety net).
 */
object BlankLineNormalizer {

  /**
   * Collapse consecutive blank lines outside strings and comments to at most one.
   */
  def collapseBlankRuns(source: String): String =
    LineLayout.of(source).fold(source)(collapse)

  private def collapse(layout: LineLayout): String =
    val (kept, _) =
      layout
        .lines
        .zip(layout.insideToken)
        .foldLeft((Vector.empty[String], false)) {
          case ((acc, prevBlank), (line, inside)) =>
            val blank = line.trim.isEmpty && !inside
            if blank && prevBlank then
              (acc, true)
            else
              (acc :+ line, blank)
        }

    kept.mkString("\n")

}
