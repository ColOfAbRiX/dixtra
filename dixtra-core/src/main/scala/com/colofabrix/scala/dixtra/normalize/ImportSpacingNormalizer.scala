package com.colofabrix.scala.dixtra.normalize

import com.colofabrix.scala.dixtra.normalize.SourceEdits.Edit
import scala.meta.*
import scala.meta.tokens.Token
import scala.util.control.NonFatal

/**
 * The blank lines between imports: always removed.
 *
 * Between an import and the next one only comments stay, each on its line; an import ends at the first line break
 * outside its braces that does not follow a comma. A gap with a comment over several lines is left as it is, so that
 * the blank lines inside the comment stay. The pass runs before rendering, so the layout sees the imports as one group.
 *
 * The pass is token-aware; if the text cannot be tokenised, it is returned unchanged (safety net).
 */
object ImportSpacingNormalizer {

  /**
   * `source` without the blank lines between its imports.
   */
  def normalize(source: String): String =
    try {
      SourceEdits
        .tokens(source)
        .fold(source) { tokens =>
          SourceEdits.applyEdits(source, edits(source, tokens))
        }
    } catch {
      case NonFatal(_) => source
    }

  private def edits(source: String, tokens: Vector[Token]): List[Edit] =
    tokens
      .indices
      .toList
      .filter(i => tokens(i).is[Token.KwImport])
      .flatMap { i =>
        val end = statementEnd(tokens, i)
        nextImport(tokens, end)
          .filterNot(next => multiline(tokens, end, next))
          .flatMap { next =>
            val from    = SourceEdits.lineEnd(source, tokens(end).start) + 1
            val to      = SourceEdits.lineStart(source, tokens(next).start)
            val between = source.slice(from, to)

            val text =
              between
                .split("\n", -1)
                .toList
                .dropRight(1)
                .filterNot(_.isBlank)
                .map(_ + "\n")
                .mkString

            Option.when(from <= to && text != between)(Edit(from, to, text))
          }
      }

  // The index of the last token of the import starting at token `start`.
  private def statementEnd(tokens: Vector[Token], start: Int): Int =
    // A whitespace with a newline, outside any braces and not right after a comma: the statement ended before it.
    def endsStatement(token: Token, depth: Int, last: Int): Boolean =
      val outsideBraces = depth == 0
      val afterComma    = tokens(last).is[Token.Comma]
      isSpace(token) && token.text.contains('\n') && outsideBraces && !afterComma
      
    @annotation.tailrec
    def loop(i: Int, depth: Int, last: Int): Int =
      if i >= tokens.length then
        last
      else
        tokens(i) match {
          case _: Token.LeftBrace | _: Token.LeftParen =>
            loop(i + 1, depth + 1, i)
          case _: Token.RightBrace | _: Token.RightParen =>
            if depth == 0 then last else loop(i + 1, depth - 1, i)
          case t if endsStatement(t, depth, last) =>
            last
          case t if isSpace(t) || t.is[Token.EOF] =>
            loop(i + 1, depth, last)
          case _: Token.Comment =>
            loop(i + 1, depth, last)
          case _ =>
            loop(i + 1, depth, i)
        }

    loop(start + 1, 0, start)

  // The index of the import that follows the token `end` with only whitespace and comments in between.
  private def nextImport(tokens: Vector[Token], end: Int): Option[Int] =
    (end + 1 until tokens.length)
      .find { i =>
        !isSpace(tokens(i)) && !tokens(i).is[Token.Comment]
      }
      .filter { i =>
        tokens(i).is[Token.KwImport]
      }

  // Whether a comment over several lines is between the tokens `from` and `to`.
  private def multiline(tokens: Vector[Token], from: Int, to: Int): Boolean =
    (from + 1 until to).exists(i => tokens(i).is[Token.Comment] && tokens(i).text.contains('\n'))

  private def isSpace(token: Token): Boolean =
    token.is[Token.Whitespace]

}
