package com.colofabrix.scala.dixtra.normalize

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.model.*
import scala.meta.*
import scala.meta.tokens.Token
import scala.util.control.NonFatal

/**
 * The housekeeping of a whole file, applied last (`housekeeping`): the whitespace at the end of the lines, the line
 * break at the end of the file, and the line endings.
 *
 * The whitespace at the end of a line inside a string literal is part of the string, and always stays. The text is
 * processed with `\n` line breaks; the line ending of the file is applied on output (see [[separator]]).
 */
object Housekeeping {

  /**
   * `source`, with `\n` line breaks, tidied up by `config`.
   */
  def apply(source: String, config: HousekeepingConfig): String =
    val stripped =
      if config.trimTrailingWhitespace then
        withoutEolBlanks(source)
      else
        source

    if config.enforceEof then
      withFinalNewline(stripped)
    else
      stripped

  /**
   * The line break of the output by `policy`, given the `original` text of the file: with `keep`, a file with a `\r\n`
   * keeps `\r\n`, one with only `\r` line breaks keeps `\r`, and any other gets `\n`.
   */
  def separator(policy: LineEnding, original: String): String =
    policy match {
      case LineEnding.Lf   => "\n"
      case LineEnding.Crlf => "\r\n"
      case LineEnding.Cr   => "\r"
      case LineEnding.Keep => written(original)
    }

  /**
   * `source` with `\n` line breaks. A file with only `\r` line breaks (classic macOS) has them turned into `\n`; in any
   * other file a lone `\r` is text.
   */
  def withLf(source: String): String =
    written(source) match {
      case "\r\n" => source.replace("\r\n", "\n")
      case "\r"   => source.replace('\r', '\n')
      case _      => source
    }

  // The line break of `source`: `\r\n` if it has one, `\r` if it has only `\r` and no `\n`, otherwise `\n`.
  private def written(source: String): String =
    if source.contains("\r\n") then
      "\r\n"
    else if source.contains('\r') && !source.contains('\n') then
      "\r"
    else
      "\n"

  // `source` without spaces and tabs at the end of its lines, except the lines that end inside a string literal.
  private def withoutEolBlanks(source: String): String =
    try {
      SourceEdits
        .tokens(source)
        .fold(source) { tokens =>
          def isMultilineLiteral(token: Token): Boolean =
            !token.is[Token.Whitespace] && !token.is[Token.Comment] && token.text.contains('\n')

          val literals = tokens.filter(isMultilineLiteral)

          val protectedEnds =
            literals
              .flatMap(t => (t.start until t.end).filter(source.charAt(_) == '\n'))
              .toSet

          val (lines, _) =
            source
              .split("\n", -1)
              .foldLeft((Vector.empty[String], 0)) { case ((acc, start), line) =>
                val end = start + line.length
                (acc :+ (if protectedEnds(end) then line else line.replaceAll("[ \t]+$", "")), end + 1)
              }

          lines.mkString("\n")
        }
    } catch {
      case NonFatal(_) => source
    }

  // `source` ending with exactly one line break, without blank lines before it; an empty source stays empty.
  private def withFinalNewline(source: String): String =
    val lines =
      source
        .split("\n", -1)
        .toList
        .reverse
        .dropWhile(_.isBlank)
        .reverse

    if lines.isEmpty then
      ""
    else
      lines.mkString("", "\n", "\n")

}
