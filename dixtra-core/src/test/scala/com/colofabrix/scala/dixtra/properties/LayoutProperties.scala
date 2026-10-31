package com.colofabrix.scala.dixtra.properties

import com.colofabrix.scala.dixtra.*
import scala.meta.*

/**
 * Property/invariant checkers for the dixtra layout.
 *
 * These generalize the normative principles into predicates that can be asserted over any formatted output,
 * complementing the example-based golden fixtures. Each returns a list of human-readable violations (empty means the
 * property holds).
 */
object LayoutProperties {

  /**
   * No line may strand a closing bracket against a following opening bracket.
   *
   * A line that consists solely of delimiters is only a violation when it contains an opening bracket (`(`, `[`, `{`) —
   * i.e. a close-then-open join such as `](` or `)(` that carries zero content. A line that is solely closing brackets
   * (e.g. a lone `)` terminating a collection) is valid and not flagged.
   */
  def lonelyDelimiterViolations(formatted: String): Vector[String] =
    val delimitersOnly = """^\s*[()\[\]{}]+\s*$""".r
    val hasOpening     = Set('(', '[', '{')

    formatted
      .split("\n", -1)
      .toVector
      .zipWithIndex
      .collect {
        case (line, i) if delimitersOnly.matches(line) && line.exists(hasOpening) =>
          s"lonely delimiter at line ${i + 1}: ${line.trim}"
      }

  /**
   * If a def/val RHS spans multiple lines, `=` must be the last token on its line. Approximated textually: no
   * non-comment line may contain `=` with trailing content when the following line is indented deeper (a continuation).
   */
  def newlineAfterEqViolations(formatted: String): Vector[String] =
    val lines = formatted.split("\n", -1).toVector
    lines
      .zipWithIndex
      .flatMap {
        case (line, lineNo) =>
          val next      = lines.lift(lineNo + 1)
          val assignIdx = indexOfBareEquals(line)

          // A trailing comment after `=` is not RHS content.
          val hasTrailing =
            assignIdx >= 0 && {
              val rest = line.substring(assignIdx + 1).trim
              rest.nonEmpty && !rest.startsWith("//")
            }

          val nextIsDeeper =
            next.exists(n => n.trim.nonEmpty && leadingSpaces(n) > leadingSpaces(line))

          if hasTrailing && nextIsDeeper then
            Vector(s"newline-after-= violation at line ${lineNo + 1}: ${line.trim}")
          else
            Vector.empty
      }

  /** 
   * Within a select chain, breaking is complete: either every `.member` selection begins a new line, or none do.
   *
   * Detected textually per top-level statement: if any line (after the receiver) starts with `.` then *all* subsequent
   * `.member` selections of that chain must also start their own line. We approximate by flagging a line that contains
   * a mid-line ` .` (space-dot) selection while some sibling selection is on its own line — i.e. a partially broken
   * chain.
   */
  def partialBreakViolations(formatted: String): Vector[String] =
    val lines         = formatted.split("\n", -1).toVector
    val anyLeadingDot = lines.exists(_.trim.startsWith("."))

    if !anyLeadingDot then
      Vector.empty
    else
      lines
        .zipWithIndex
        .collect {
          case (line, i) if hasMidLineSelectionAfterReceiver(line) =>
            s"partial break at line ${i + 1}: ${line.trim}"
        }

  // A joined selection like `recv.member.other` on a line, when the file also breaks chains elsewhere, signals an
  // inconsistent (partial) break.
  private def hasMidLineSelectionAfterReceiver(line: String): Boolean =
    val trimmed = line.trim
    // Two or more `.` selections joined on one non-leading-dot line.
    !trimmed.startsWith(".") && countSelections(trimmed) >= 2

  private def countSelections(s: String): Int =
    """\.\p{Alpha}"""
      .r
      .findAllIn(s)
      .length

  /**
   * Idempotency — formatting an already-formatted output yields itself.
   */
  def isIdempotent(source: String, config: Config): Boolean =
    val result =
      for
        once  <- Dixtra.format(source, config)
        twice <- Dixtra.format(once, config)
        result = once == twice
      yield result

    result.getOrElse(false)

  /**
   * Round-trip safety — parsing the formatted output yields a structurally equal tree to parsing the original
   * (formatting never changes semantics).
   */
  def isStructurePreserving(source: String, config: Config): Boolean =
    given Dialect = dialects.Scala3

    Dixtra.format(source, config) match {
      case Left(_) =>
        false
      case Right(formatted) =>
        val before = source.parse[Source].toOption
        val after  = formatted.parse[Source].toOption

        (before, after) match {
          case (Some(b), Some(a)) => b.structure == a.structure
          case _                  => false
        }
    }

  private def leadingSpaces(line: String): Int =
    line.takeWhile(_ == ' ').length

  // Index of a top-level `=` used as assignment, ignoring ==, =>, <=, >=, != .
  private def indexOfBareEquals(line: String): Int =
    val idx = line.indexOf('=')
    if idx < 0 then
      -1
    else
      val prev         = if idx > 0 then line.charAt(idx - 1) else ' '
      val nextC        = if idx + 1 < line.length then line.charAt(idx + 1) else ' '
      val isComparison = prev == '!' || prev == '<' || prev == '>' || nextC == '='
      val isArrow      = nextC == '>'
      if isComparison || isArrow then -1 else idx

}
