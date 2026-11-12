package com.colofabrix.scala.dixtra.normalize

import scala.meta.tokens.Token

/**
 * Token-aware `=>` spacing normalisation.
 *
 * Ensures exactly one space on each side of every `=>` (`Token.RightArrow`), so `Int=>String` becomes `Int => String`.
 * It applies uniformly to function types, lambdas, `case` clauses, and by-name parameters. Because it operates on
 * `RightArrow` tokens, occurrences of `=>` inside string literals or comments are single string/comment tokens and are
 * therefore left untouched.
 *
 * A space is never inserted before a line break (so a lambda whose body starts on the next line keeps no trailing space
 * after `=>`). This pass assumes redundant whitespace has already been collapsed, so at most one space is ever present
 * on either side before it runs.
 *
 * If the text cannot be tokenised (should not happen after rendering), the input is returned unchanged (safety net).
 */
object ArrowSpacingNormalizer {

  /**
   * Insert a single space around each `=>` where one is missing.
   */
  def normalize(formatted: String): String =
    // Without an arrow there is no `RightArrow` token, and the text is rebuilt as it was: skip tokenising it
    if !formatted.contains("=>") && !formatted.contains('⇒') then
      formatted
    else
      SourceEdits.tokens(formatted).fold(formatted)(space)

  private def space(tokens: Vector[Token]): String =
    tokens
      .zipWithIndex
      .foldLeft(new StringBuilder) { case (acc, (token, i)) =>
        token match {
          case _: Token.RightArrow =>
            if endsWithGluedChar(acc) then
              acc.append(' ')
            acc.append("=>")
            if needsSpaceAfter(tokens.lift(i + 1)) then
              acc.append(' ')
            acc
          case _ =>
            acc.append(token.text)
        }
      }
      .toString

  // True when the builder ends with a real character (not a space or newline), so the arrow would glue to it without an
  // inserted space.
  private def endsWithGluedChar(acc: StringBuilder): Boolean =
    acc.nonEmpty && {
      val last = acc.charAt(acc.length - 1)
      last != ' ' && last != '\n'
    }

  // A space is needed after `=>` only when the next token is real content, i.e. not an existing space and not a line
  // break.
  private def needsSpaceAfter(next: Option[Token]): Boolean =
    next.exists {
      case _: Token.HSpace => false
      case _: Token.AtEOL  => false
      case _               => true
    }

}
