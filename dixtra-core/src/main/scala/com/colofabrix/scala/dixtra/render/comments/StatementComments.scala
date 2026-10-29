package com.colofabrix.scala.dixtra.render.comments

import com.colofabrix.scala.dixtra.BlankLinePolicy
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.render.EdgeBlankLines
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*
import scala.meta.tokens.Token

/**
 * The comments and blank lines found between the statements of a block (or the clauses of a match, the enumerators of a
 * for), assigned to the statement they belong to so the statements can be re-rendered one per line without losing them.
 *
 * A comment on the same line as the end of a statement trails it; every other comment gets its own line before the next
 * statement (or after the last one). A blank line the author left between statements and own-line comments is kept
 * (runs collapse to one); the blank lines at the edges of the block follow the construct's `blank-lines.body-edges`
 * policy (see `withEdgeBlanks`).
 *
 * @param leading Per statement, the lines before it: own-line comments, and `Doc.Empty` for a blank line.
 * @param trailing Per statement, the comments following it on its line.
 * @param closing The lines after the last statement, as for `leading`.
 */
private[render] final case class StatementComments(
  leading: List[List[Doc]],
  trailing: List[Doc],
  closing: List[Doc],
) {

  /**
   * True when the block carries no comment between its statements.
   */
  def isEmpty: Boolean =
    leading.forall(_.forall(_ == Doc.Empty)) && trailing.forall(_ == Doc.Empty) && closing.forall(_ == Doc.Empty)

  /**
   * The same comments without the blank lines before the first statement and after the last one, for a block whose
   * braces are removed.
   */
  def withoutEdgeBlanks: StatementComments =
    val first =
      leading
        .headOption
        .map(_.dropWhile(_ == Doc.Empty))
        .toList
    val rest = leading.drop(1)

    StatementComments(first ++ rest, trailing, closing.reverse.dropWhile(_ == Doc.Empty).reverse)

  /**
   * The same comments with the blank lines before the first statement and after the last one decided by `policy`
   * (`keep`: a blank line at either edge means one at both).
   */
  def withOpeningBlank(policy: BlankLinePolicy): StatementComments =
    val edged = withEdgeBlanks(policy)
    edged.copy(closing = edged.closing.reverse.dropWhile(_ == Doc.Empty).reverse)

  def withEdgeBlanks(policy: BlankLinePolicy): StatementComments =
    val trimmed = withoutEdgeBlanks
    val written = leading.headOption.exists(_.headOption.contains(Doc.Empty)) || closing.lastOption.contains(Doc.Empty)
    if !EdgeBlankLines.wanted(policy, written) || leading.isEmpty then
      trimmed
    else
      StatementComments(
        trimmed.leading.headOption.map(Doc.Empty :: _).toList ++ trimmed.leading.drop(1),
        trimmed.trailing,
        trimmed.closing :+ Doc.Empty,
      )

  /**
   * The rendered statements one per line, with their comments re-attached.
   */
  def lines(statements: List[Doc]): Doc =
    val statLines =
      statements.zipWithIndex.flatMap { case (stat, i) => leading(i) :+ (stat ++ trailing(i)) }
    Doc.intercalate(Doc.Line, statLines ++ closing)

}

private[render] object StatementComments {

  /**
   * Assign the comments and blank lines in the gaps of `block` to its statements.
   */
  def of(ctx: RenderContext, block: Term.Block): StatementComments =
    of(ctx, block.pos.start, block.stats, block.pos.end)

  /**
   * Assign the comments and blank lines found in `[start, end)` around `items` (statements, case clauses, enumerators)
   * to those items.
   */
  def of(ctx: RenderContext, start: Int, items: List[Tree], end: Int): StatementComments =
    val bounds = items.map(ctx.span)
    val gaps   = (start :: bounds.map(_._2)).zip(bounds.map(_._1) :+ end)

    // Split each gap into comments trailing the previous statement and the lines before the next one (the first gap has
    // no previous statement).
    val split =
      gaps
        .zipWithIndex
        .map { case ((start, end), i) =>
          val comments = ctx.commentsBetween(start, end).toList
          val (trailing, own) =
            if i == 0 then (Nil, comments)
            else comments.partition(c => !ctx.slice(start, c.start).contains('\n'))

          val ownStart =
            trailing
              .lastOption
              .map(_.end)
              .getOrElse(start)

          (trailing, ownLines(ctx, ownStart, own, end))
        }

    val statements = items.indices.toList

    StatementComments(
      leading = statements.map(i => split(i)._2),
      trailing = statements.map(i => Doc.concat(ctx.commentDocs(split(i + 1)._1).map(Doc.text(" ") ++ _))),
      closing = split.lastOption.map(_._2).getOrElse(Nil),
    )

  // The own-line comments of a gap ending at `end` as lines, with `Doc.Empty` wherever the author left a blank line
  // before a comment or before `end`.
  private def ownLines(ctx: RenderContext, start: Int, comments: List[Token.Comment], end: Int): List[Doc] =
    val (lines, lastEnd) =
      comments.foldLeft((List.empty[Doc], start)) { case ((acc, from), c) =>
        (acc ++ blank(ctx, from, c.start) ++ ctx.commentDocs(List(c)), c.end)
      }
    lines ++ blank(ctx, lastEnd, end)

  private def blank(ctx: RenderContext, start: Int, end: Int): List[Doc] =
    if ctx.hasBlankLine(start, end) then List(Doc.Empty) else Nil

}
