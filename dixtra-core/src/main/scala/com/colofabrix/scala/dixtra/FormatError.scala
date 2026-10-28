package com.colofabrix.scala.dixtra

/**
 * Base error type for the dixtra formatter.
 */
sealed abstract class FormatError(message: String) extends Exception(message)

/**
 * Raised when the source cannot be parsed as valid Scala 3.
 */
final class ParseError(message: String) extends FormatError(message)

/**
 * Raised when the source is Scala 2: it parses only with the Scala 2 dialect of its kind of file, or it uses a
 * construct that Scala 3 dropped. Scala 2 is not formatted, and such a file is skipped rather than failed.
 */
final class Scala2Error(message: String) extends FormatError(message)

/**
 * Raised when the formatted output would not be equivalent to the input (a different program structure or lost
 * comments). The input is never rewritten in that case: this signals a formatter bug, not a problem with the source.
 */
final class UnsafeOutputError(message: String) extends FormatError(message)

object FormatError {

  /**
   * The message of `error`, or its description when it has none.
   */
  def describe(error: Throwable): String =
    Option(error.getMessage).getOrElse(error.toString)

}
