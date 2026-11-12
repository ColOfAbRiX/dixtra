package com.colofabrix.scala.dixtra.normalize

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.model.*
import scala.annotation.tailrec

/**
 * Blank-line-between-methods normalisation.
 *
 * Ensures a single blank line precedes every method definition that directly follows another statement, so adjacent
 * `def`s are always visually separated. `given` definitions and `extension` groups are methods too and are separated
 * exactly like `def`s.
 *
 * A method's decorations — the annotations, scaladoc, and comments on the lines directly above its `def` (multi-line
 * scaladoc included) — form one group with it, so the blank line goes before the whole group, never between a
 * decoration and the `def`. The blank line is inserted only when the line before the group is non-blank and is not an
 * opener: a line ending with `{` or `:` (Scala 3 braceless `object A:`) or with `with` (`given A: B with`), an
 * `extension (...)` header, or a line ending with `=` or `=>` that opens a braceless body (so a local `given` or `def`
 * at the top of a method body is not pushed down). The blank lines at the top of a body follow `blank-lines.body-edges`
 * instead (see `EdgeBlankLines`).
 *
 * One-line definitions (`val`, `lazy val`, `var`, `def`, `given`, `type`, a one-line declaration without a body
 * included) at the same indent whose keywords are both in an alignment group follow the groups instead: two adjacent
 * one-liners of the same group are a block and are not separated; between two of different groups the blank line
 * follows [[BlankLinesConfig.betweenAlignmentGroups]] (kept as written, always, or never). Comments and annotations
 * above a one-liner do not change that, but a scaladoc does: the blank line before a definition with a scaladoc follows
 * `blank-lines.before-scaladoc` (added with `always`, and with `always-multiline` for a multi-line scaladoc), and a
 * one-line one also gets a blank line after it, before the next statement at its indent.
 *
 * This is a line pass keyed off lines whose first token (after optional modifiers, including qualified `private[x]` /
 * `protected[x]`) is `def`, `given`, or `extension`, which is robust for the rendered output where signatures start
 * their own line. It is token-aware: a line that starts inside a multi-line string or comment is never a method start,
 * and no line is ever inserted inside such a token. If the text cannot be tokenised, the input is returned unchanged
 * (safety net).
 */
object MethodSpacingNormalizer {

  private val modifier =
    List(
      """(?:private|protected)(?:\[[^\]]*\])?""",
      "final|override|inline|transparent|open|lazy|implicit|abstract|sealed|infix|opaque",
    ).mkString("(?:", "|", ")")

  private val annotation =
    """@[\w.]+(?:\[[^\]]*\])?(?:\(.*\))?"""

  private val defStart =
    s"""^\\s*(?:$annotation\\s+)*(?:$modifier\\s+)*(?:def|given|extension)\\b""".r

  private val definitionStart =
    s"""^\\s*((?:$modifier\\s+)*)(val|var|def|given|type)\\b""".r

  private val annotationOnly =
    s"""^\\s*(?:$annotation\\s*)+$$""".r

  /**
   * Insert a blank line before each method (and its decorations) that follows adjacent content, with the given
   * alignment groups and blank line between them, and the given blank line before a scaladoc.
   */
  def normalize(
    source: String,
    alignment: AlignmentConfig,
    betweenGroups: BlankLinePolicy,
    beforeScaladoc: ScaladocSpacing,
  ): String =
    LineLayout
      .of(source)
      .fold(source)(separate(_, alignment, betweenGroups, beforeScaladoc))

  private def separate(
    layout: LineLayout,
    alignment: AlignmentConfig,
    betweenGroups: BlankLinePolicy,
    beforeScaladoc: ScaladocSpacing,
  ): String =
    val insertBefore = blankBefore(layout, alignment, betweenGroups, beforeScaladoc) ++ blankAfterDocumented(layout)
    val removed      = blanksToRemove(layout, alignment, betweenGroups)

    layout
      .lines
      .zipWithIndex
      .flatMap { case (line, i) =>
        if removed.contains(i) then
          Vector.empty
        else if insertBefore.contains(i) then
          Vector("", line)
        else
          Vector(line)
      }
      .mkString("\n")

  // The lines that get a blank line before them: the first line of a method with its decorations after other content,
  // and a one-liner after one of another alignment group when those are always separated.
  private def blankBefore(
    layout: LineLayout,
    alignment: AlignmentConfig,
    betweenGroups: BlankLinePolicy,
    beforeScaladoc: ScaladocSpacing,
  ): Set[Int] =
    layout
      .lines
      .indices
      .filter { i =>
        startsMethod(layout, i) || kindOf(layout, i).isDefined
      }
      .flatMap { i =>
        val start = groupStart(layout, i)

        val previous =
          oneLinerPair(layout, start - 1, i)
            .filter { (a, b) =>
              alignment.groupOf(a).isDefined && alignment.groupOf(b).isDefined
            }
            .map { (a, b) =>
              alignment.sameGroup(a, b)
            }

        val notInsideToken = !layout.insideToken(start)

        val afterContent =
          layout
            .lines
            .lift(start - 1)
            .exists(p => p.trim.nonEmpty && !LineLayout.isOpener(p))

        val separated = notInsideToken && afterContent

        val isDocumented = documented(layout, start, i)

        val needsBlank =
          previous match {
            case _ if isDocumented =>
              documented(layout, start, i, beforeScaladoc.wantsBlank) && separated
            case Some(_) if documentedAt(layout, start - 1) =>
              true
            case Some(true) =>
              false
            case Some(false) =>
              betweenGroups == BlankLinePolicy.Always
            case None =>
              startsMethod(layout, i) && separated
          }

        if needsBlank then
          Some(start)
        else
          None
      }
      .toSet

  // The line after each one-liner with a scaladoc, when it holds the next statement at its indent.
  private def blankAfterDocumented(layout: LineLayout): Set[Int] =
    layout
      .lines
      .indices
      .filter { i =>
        val documented = kindOf(layout, i).isDefined && oneLiner(layout, i) && documentedAt(layout, i)

        val followedByStatement =
          layout
            .lines
            .lift(i + 1)
            .exists { next =>
              val atIndent  = SourceEdits.indent(next) == SourceEdits.indent(layout.lines(i))
              val notCloser = !next.trim.startsWith("}") && !next.trim.startsWith(")") && !next.trim.startsWith("end ")

              !layout.insideToken(i + 1) && next.trim.nonEmpty && atIndent && notCloser
            }

        documented && followedByStatement
      }
      .map(_ + 1)
      .toSet

  // True when the definition starting on line `i` has a scaladoc among its decorations.
  private def documentedAt(layout: LineLayout, i: Int): Boolean =
    i >= 0 && kindOf(layout, i).isDefined && documented(layout, groupStart(layout, i), i)

  // True when a scaladoc starts among the decoration lines `[start, i)`, one that `wanted` accepts given its first
  // line.
  private def documented(layout: LineLayout, start: Int, i: Int, wanted: String => Boolean): Boolean =
    (start until i).exists { j =>
      val line       = layout.lines(j).trim
      val isScaladoc = layout.commentOnly(j) && !layout.insideToken(j) && SourceEdits.isScaladoc(line)

      isScaladoc && wanted(line)
    }

  // True when a scaladoc starts among the decoration lines `[start, i)`.
  private def documented(layout: LineLayout, start: Int, i: Int): Boolean =
    documented(layout, start, i, _ => true)

  // The blank lines between one-liners of different alignment groups, when those are never separated.
  private def blanksToRemove(layout: LineLayout, alignment: AlignmentConfig, betweenGroups: BlankLinePolicy): Set[Int] =
    if betweenGroups != BlankLinePolicy.Never then
      Set.empty
    else
      layout
        .lines
        .indices
        .filter { i =>
          val blank = layout.lines(i).trim.isEmpty && !layout.insideToken(i)

          val separatesGroups =
            oneLinerPair(layout, i - 1, i + 1).exists { (a, b) =>
              alignment.groupOf(a).isDefined && alignment.groupOf(b).isDefined && !alignment.sameGroup(a, b)
            }

          blank && separatesGroups
        }
        .toSet

  // A method starts on a `def` line that does not begin inside a string/comment.
  private def startsMethod(layout: LineLayout, i: Int): Boolean =
    !layout.insideToken(i) && defStart.findFirstIn(layout.lines(i)).isDefined

  // The kinds of the one-line definitions on lines `a` and `b`, when both are undecorated one-liners at the same
  // indent.
  private def oneLinerPair(layout: LineLayout, a: Int, b: Int): Option[(DefinitionKind, DefinitionKind)] =
    def indentOf(line: Int) =
      SourceEdits.indent(layout.lines(line))

    val outOfRange = a < 0 || b >= layout.lines.length

    if outOfRange || indentOf(a) != indentOf(b) then
      None
    else
      for
        ka <- kindOf(layout, a).filter(_ => oneLiner(layout, a))
        kb <- kindOf(layout, b).filter(_ => oneLiner(layout, b))
      yield
        (ka, kb)

  // The definition keyword starting line `i`, when it is not inside a token.
  private def kindOf(layout: LineLayout, i: Int): Option[DefinitionKind] =
    if layout.insideToken(i) then
      None
    else
      definitionStart
        .findFirstMatchIn(layout.lines(i))
        .map { m =>
          m.group(2) match {
            case "val" if m.group(1).contains("lazy") => DefinitionKind.LazyVal
            case "val"                                => DefinitionKind.Val
            case "var"                                => DefinitionKind.Var
            case "def"                                => DefinitionKind.Def
            case "given"                              => DefinitionKind.GivenAlias
            case _                                    => DefinitionKind.TypeDef
          }
        }

  // A definition line with a `=` body that ends on the same line, or a declaration without a body: it does not open a
  // body, and the next line is not a continuation indented under it.
  private def oneLiner(layout: LineLayout, i: Int): Boolean =
    val line = layout.lines(i)

    val noContinuation =
      layout
        .lines
        .lift(i + 1)
        .forall { next =>
          val notIndentedUnder = next.trim.isEmpty || SourceEdits.indent(next) <= SourceEdits.indent(line)

          !layout.insideToken(i + 1) && notIndentedUnder
        }

    !LineLayout.isOpener(line) && noContinuation

  // First line of the decoration group (annotations and comments) directly above the method starting on line `i`, or
  // `i` itself when undecorated.
  @tailrec
  private def groupStart(layout: LineLayout, i: Int): Int =
    if i > 0 && isDecoration(layout, i - 1) then
      groupStart(layout, i - 1)
    else
      i

  private def isDecoration(layout: LineLayout, i: Int): Boolean =
    val annotation = !layout.insideToken(i) && annotationOnly.matches(layout.lines(i))
    layout.commentOnly(i) || annotation

}
