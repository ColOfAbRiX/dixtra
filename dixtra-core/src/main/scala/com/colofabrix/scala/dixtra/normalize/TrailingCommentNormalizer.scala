package com.colofabrix.scala.dixtra.normalize

import com.colofabrix.scala.dixtra.TrailingOverflow
import com.colofabrix.scala.dixtra.normalize.SourceEdits.Edit
import scala.meta.*
import scala.meta.tokens.Token
import scala.util.control.NonFatal

/**
 * Trailing comments: the one-line comments at the end of a line of code.
 *
 * A trailing comment never counts in the width of its line, so the code before it never breaks because of it. Two
 * passes change where it goes:
 *
 *   - [[moveAbove]], with `trailing-comment-overflow = move-above`: a line that goes past the maximum width with its
 *     trailing comment gets the comment on a line of its own above, at the indent of the line. When the line above is a
 *     `//` comment, an empty `//` line keeps the two comments apart, so that the comment reflow does not join them.
 *   - [[align]], with `alignment.trailing-comments`: the trailing comments of consecutive lines start on one column,
 *     one space after the longest code. A line that would go past the maximum width once aligned stays as it is, and
 *     the lines before and after it align separately.
 *
 * Both passes are token-aware; if the text cannot be tokenised, it is returned unchanged (safety net).
 */
object TrailingCommentNormalizer {

  /**
   * Move above its line each trailing comment whose line goes past `maxColumn`, when `policy` is `move-above`.
   */
  def moveAbove(source: String, maxColumn: Int, policy: TrailingOverflow): String =
    policy match {
      case TrailingOverflow.Keep =>
        source
      case TrailingOverflow.MoveAbove =>
        edited(source) { trailings =>
          trailings
            .filter(t => t.width > maxColumn && t.movable)
            .flatMap { t =>
              val above =
                source
                  .slice(SourceEdits.lineStart(source, t.lineStart - 1), (t.lineStart - 1).max(0))
                  .trim

              val separator = if t.lineStart > 0 && above.startsWith("//") then t.indent + "//\n" else ""
              List(
                Edit(t.lineStart, t.lineStart, separator + t.indent + t.text + "\n"),
                Edit(t.codeEnd, t.end, ""),
              )
            }
        }
    }

  /**
   * Align on one column the trailing comments of consecutive lines, when `enabled`, never past `maxColumn`.
   */
  def align(source: String, maxColumn: Int, enabled: Boolean): String =
    if !enabled then 
      source
    else 
      edited(source) { trailings => 
        runs(trailings).flatMap(aligned(_, maxColumn))
      }

  // A trailing comment and its line.
  private final case class Trailing(
    lineStart: Int,
    lineEnd: Int,
    codeEnd: Int,
    start: Int,
    end: Int,
    text: String,
    indent: String,
    movable: Boolean,
  ) {

    def width: Int = 
      end - lineStart

    def codeWidth: Int = 
      codeEnd - lineStart

  }

  // The edits of `change` applied to `source`, given its trailing comments.
  private def edited(source: String)(change: List[Trailing] => List[Edit]): String =
    try {
      SourceEdits
        .tokens(source)
        .fold(source) { tokens => 
          SourceEdits.applyEdits(source, change(trailings(source, tokens)))
        }
    } catch {
      case NonFatal(_) => source
    }

  // The trailing comments of `source`: a one-line comment with only whitespace after it on its line, after code that
  // ends on its line.
  private def trailings(source: String, tokens: Vector[Token]): List[Trailing] =
    val solid = tokens.filterNot(t => t.is[Token.Whitespace] || t.is[Token.BOF] || t.is[Token.EOF])

    // After code on its line, on one line, with only whitespace after it.
    def isTrailing(code: Token, comment: Token.Comment): Boolean =
      val realCode   = !code.is[Token.Comment]
      val oneLine    = !comment.text.contains('\n')
      val afterCode  = code.end > SourceEdits.lineStart(source, comment.start)
      val blankAfter = source.slice(comment.end, SourceEdits.lineEnd(source, comment.end)).isBlank

      realCode && oneLine && afterCode && blankAfter

    solid
      .zip(solid.drop(1))
      .collect {
        case (code, comment: Token.Comment) if isTrailing(code, comment) =>
          val lineStart = SourceEdits.lineStart(source, comment.start)
          Trailing(
            lineStart = lineStart,
            lineEnd = SourceEdits.lineEnd(source, comment.end),
            codeEnd = code.end,
            start = comment.start,
            end = comment.end,
            text = comment.text,
            indent = source.slice(lineStart, comment.start).takeWhile(c => c == ' ' || c == '\t'),
            movable = !tokens.exists(t => t.start < lineStart && t.end > lineStart),
          )
      }
      .toList

  // The trailing comments split in runs of consecutive lines.
  private def runs(trailings: List[Trailing]): List[List[Trailing]] =
    trailings.foldRight(List.empty[List[Trailing]]) {
      case (t, (run @ (next :: _)) :: rest) if next.lineStart == t.lineEnd + 1 => (t :: run) :: rest
      case (t, acc)                                                            => List(t) :: acc
    }

  // The edits that align a run, the lines that would not fit once aligned left as they are and splitting the run.
  private def aligned(run: List[Trailing], maxColumn: Int): List[Edit] =
    if run.sizeIs < 2 then
      Nil
    else
      val column = run.map(_.codeWidth).foldLeft(0)(math.max) + 1
      if run.forall(t => column + t.end - t.start <= maxColumn) then
        run.map(t => Edit(t.codeEnd, t.start, " " * (column - t.codeWidth)))
      else
        runs(run.filter(t => column + t.end - t.start <= maxColumn)).flatMap(aligned(_, maxColumn))

}
