package com.colofabrix.scala.dixtra.normalize

import scala.meta.tokens.Token

/**
 * Token-aware intra-line whitespace normalisation.
 *
 * Collapses runs of horizontal whitespace *between tokens on the same line* to a
 * single space, so `val a =      "3"` becomes `val a = "3"`. It runs on the
 * source before it is parsed, and on the source slices the renderers copy, tokenising them with Scalameta:
 *   - String, interpolation, and comment contents are single tokens, never whitespace tokens, so their interior spacing
 *     is inherently preserved.
 *   - Leading (indentation) whitespace is preserved.
 *   - Newlines and blank lines are preserved.
 *
 * If the text cannot be tokenised, the input is returned unchanged (safety net).
 */
object WhitespaceNormalizer {

  /**
   * Collapse redundant intra-line whitespace runs to a single space.
   */
  def normalize(formatted: String): String =
    SourceEdits
      .tokens(formatted)
      .fold(formatted)(collapse)

  // Scalameta emits each horizontal-space character as its own HSpace token, so a
  // run like `   ` is several consecutive HSpace tokens. We collapse *inter-token*
  // runs to a single space, but preserve *leading* whitespace (indentation) at the start of a line verbatim, and never
  // touch newlines or other tokens.
  private def collapse(tokens: Vector[Token]): String =
    // State: atLineStart (still within leading indentation) and prevWasHSpace (to coalesce a mid-line run into a single
    // emitted space).
    val (sb, _, _) =
      tokens.foldLeft((new StringBuilder, true, false)) {
        case ((acc, atLineStart, prevWasHSpace), token) =>
          token match {
            case _: Token.AtEOL =>
              (acc.append(token.text), true, false)
            case _: Token.HSpace =>
              if atLineStart then 
                (acc.append(token.text), true, false)
              else if prevWasHSpace then 
                (acc, false, true)
              else 
                (acc.append(' '), false, true)
            case _ =>
              (acc.append(token.text), false, false)
          }
      }

    sb.toString

}
