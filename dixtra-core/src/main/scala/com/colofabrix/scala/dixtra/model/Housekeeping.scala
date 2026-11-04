package com.colofabrix.scala.dixtra.model

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
