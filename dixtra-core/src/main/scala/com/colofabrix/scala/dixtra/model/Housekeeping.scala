package com.colofabrix.scala.dixtra.model

/**
 * Whether the last element of a list laid out one element per line (arguments, parameters, type parameters, tuple
 * elements) is followed by a comma.
 */
enum TrailingCommas(val key: String) {

  /**
   * A comma after the last element too.
   */
  case Always extends TrailingCommas("always")

  /**
   * No comma after the last element.
   */
  case Never extends TrailingCommas("never")

}

/**
 * What happens to a line of code that goes past the maximum width only because of the comment at its end.
 */
enum TrailingOverflow(val key: String) {

  /**
   * The line stays as it is: a trailing comment does not count in the width.
   */
  case Keep extends TrailingOverflow("keep")

  /**
   * The comment moves to a line of its own above the code, at its indent.
   */
  case MoveAbove extends TrailingOverflow("move-above")

}

/**
 * The line break written at the end of each line.
 */
enum LineEnding(val key: String) {

  /**
   * `\n`, as on Linux and macOS.
   */
  case Lf extends LineEnding("lf")

  /**
   * `\r\n`, as on Windows.
   */
  case Crlf extends LineEnding("crlf")

  /**
   * `\r`, as on classic macOS (up to Mac OS 9).
   */
  case Cr extends LineEnding("cr")

  /**
   * The line break of the file as it is written.
   */
  case Keep extends LineEnding("keep")

}

/**
 * The housekeeping of a whole file.
 *
 * @param trimTrailingWhitespace Whether the spaces and tabs at the end of each line are removed; those inside a string
 *                               literal always stay.
 * @param enforceEof Whether the file ends with exactly one line break.
 * @param lineEnding The line break written at the end of each line.
 */
final case class HousekeepingConfig(
  trimTrailingWhitespace: Boolean,
  enforceEof: Boolean,
  lineEnding: LineEnding,
)
