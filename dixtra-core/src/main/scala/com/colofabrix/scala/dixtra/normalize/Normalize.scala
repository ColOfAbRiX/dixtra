package com.colofabrix.scala.dixtra.normalize

import com.colofabrix.scala.dixtra.Config
import scala.meta.*

/**
 * The text-normalisation pipeline applied after tree rendering.
 *
 * These passes operate on already-rendered text (not the AST) to enforce whole-file, cross-construct layout rules:
 * placing braces by the brace indentation style, reflowing comments, canonicalising `=>` spacing, separating adjacent
 * methods, putting a blank line before scaladoc, and collapsing redundant blank-line runs. Redundant whitespace is
 * collapsed before rendering instead (see [[WhitespaceNormalizer]]), so the alignment the renderers add is kept.
 * Ordering matters — braces are placed first, then comments are reflowed and arrow spacing is enforced, then method and
 * scaladoc separation insert blanks, blank-line runs are collapsed, and finally trailing comments are aligned — so the
 * facade sequences them in one place. Every pass is token-aware and leaves the interior of string literals and comments
 * untouched.
 */
object Normalize {

  /**
   * Apply every text post-pass, in order, to the rendered source of a file of the given dialect.
   */
  def apply(rendered: String, config: Config, dialect: Dialect): String =
    apply(rendered, config, dialect, None)

  /**
   * Apply every text post-pass, in order, to the rendered source of a file of the given dialect, with `parsed` as the
   * tree of `rendered` when the caller already has it (the rendering left the source as it was).
   */
  def apply(rendered: String, config: Config, dialect: Dialect, parsed: Option[Source]): String =
    val braced =
      BraceStyleNormalizer.normalize(rendered, config.braces.style, config.maxColumn, dialect, parsed)

    val moved =
      TrailingCommentNormalizer.moveAbove(braced, config.maxColumn, config.overflow.trailingComment)

    val arrowSpaced =
      ArrowSpacingNormalizer.normalize(
        CommentNormalizer.normalize(moved, config.maxColumn, config.alignment.scaladoc.tags),
      )

    val methodsSpaced =
      MethodSpacingNormalizer.normalize(
        arrowSpaced,
        config.alignment,
        config.blankLines.betweenAlignmentGroups,
        config.blankLines.beforeScaladoc,
      )

    val docSpaced =
      DocCommentSpacingNormalizer.normalize(methodsSpaced, config.blankLines.beforeScaladoc)

    val spaced =
      BlankLineNormalizer.collapseBlankRuns(docSpaced)

    // Aligned last, once the blank lines that end the runs of consecutive lines are in place
    TrailingCommentNormalizer.align(spaced, config.maxColumn, config.alignment.trailingComments)

}
