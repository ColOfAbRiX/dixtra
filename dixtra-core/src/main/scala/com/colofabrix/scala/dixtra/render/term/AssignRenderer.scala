package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.SingleBodyPolicy
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.render.Alignment
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*

/**
 * Renders `name = value` terms: named arguments and assignment statements.
 *
 * The value follows the rules of a body after the `=` of a definition (see [[BodyLayout]]), placed by `body.assignment`
 * or `body.named-argument`, and a newline after `=` is an author signal for it. A value that is a `{ ... }` block or a
 * `{ case ... }` keeps its braces and always opens them on the `=` line.
 *
 * @param ctx The shared render context.
 * @param terms The term renderer, for the value.
 * @param applications The application renderer, for braced values.
 */
private[term] final class AssignRenderer(ctx: RenderContext, terms: TermRenderer, applications: ApplicationRenderer) {

  /**
   * Render `lhs = rhs` at the given indent.
   */
  def render(assign: Term.Assign, indent: Int): Doc =
    render(assign, indent, None)

  /**
   * Render `lhs = rhs` at the given indent, with `lhs` padded to `padTo` columns
   * when aligned.
   */
  def render(assign: Term.Assign, indent: Int, padTo: Option[Int]): Doc =
    layout(assign, indent, padTo, 0, ctx.config.body.assignment)

  // `lhs = rhs`, followed on its last line by `suffix` more columns (the comma
  // after a named argument), its value placed by `policy`.
  private def layout(assign: Term.Assign, indent: Int, padTo: Option[Int], suffix: Int, policy: SingleBodyPolicy): Doc =
    ctx.equalsBefore(ctx.span(assign.lhs)._2, ctx.span(assign.rhs)._1) match {
      case Some(eq) =>
        val lhs    = ctx.sourceDoc(assign.lhs)
        val lead   = lhs ++ Alignment.padding(padTo, Render.flatWidth(lhs)) ++ Doc.text(" =")
        val column = ctx.columnAfter(lead, indent) + 1
        bracedValue(assign, indent, column) match {
          case Some(braced) =>
            lead ++ Doc.text(" ") ++ braced
          case None =>
            val body = BodyLayout.of(ctx, terms, eq.end, assign.rhs, indent + 2)
            body.placed(lead, ctx.fits(_, column + suffix), policy)
        }
      case None =>
        ctx.ownSourceDoc(assign)
    }

  /**
   * Render a named argument `name = value` at the given indent. An application,
   * constructor or tuple over several lines that ends with its closing bracket
   * (`Test(\n  a = 1,\n)`), or an exploded select chain (`xs\n  .map(f)`), stays
   * on the `=` line when its first line fits there, its lines indented from the argument, unless `body.named-argument`
   * puts it below (`always-break`, or `keep` when the author wrote it below); any other value is rendered like an
   * assignment.
   */
  def renderArgument(assign: Term.Assign, indent: Int): Doc =
    renderArgument(assign, indent, None)

  /**
   * Render a named argument `name = value` at the given indent, with `name`
   * padded to `padTo` columns when aligned.
   */
  def renderArgument(assign: Term.Assign, indent: Int, padTo: Option[Int]): Doc =
    ctx.equalsBefore(ctx.span(assign.lhs)._2, ctx.span(assign.rhs)._1) match {
      case Some(eq) if hangsFromEq(assign, eq) =>
        val lhs    = ctx.sourceDoc(assign.lhs)
        val lead   = lhs ++ Alignment.padding(padTo, Render.flatWidth(lhs)) ++ Doc.text(" =")
        val column = ctx.columnAfter(lead, indent) + 1

        // A newline after `=` is an author signal for the value, as below a definition's `=`
        val broken = ctx.slice(eq.end, ctx.span(assign.rhs)._1).contains('\n')

        // Laid out where its lines go, below the `=` or hanging from it, so a value that fits only at the argument's
        // own indent breaks the same either way
        val value = terms.render(assign.rhs, indent + 2, forceExplode = broken)

        val lines =
          Render
            .render(value, indent)
            .split("\n")
            .toList

        val firstFit = lines.headOption.exists(first => ctx.fitsWidth(column, first.trim.length))
        val closes   = lines.lastOption.exists(last => last.trim.headOption.exists(")]}".contains(_)))
        val chain    = lines.lift(1).exists(_.trim.startsWith("."))

        val notBraced = bracedValue(assign, indent, column).isEmpty

        // The value stays hanging from the `=`: not braced, more than one line, the first line fits and the rest closes
        // there or continues as a chain.
        val hanging = notBraced && lines.sizeIs > 1 && firstFit && (closes || chain)

        if hanging then
          lead ++ Doc.text(" ") ++ value
        else
          layout(assign, indent, padTo, 1, ctx.config.body.namedArgument)
      case _ =>
        layout(assign, indent, padTo, 1, ctx.config.body.namedArgument)
    }

  /**
   * The width of `lhs` in `lhs = rhs` when it renders on one line.
   */
  def leftWidth(assign: Term.Assign): Option[Int] =
    Render.flatWidth(ctx.sourceDoc(assign.lhs))

  // Whether `policy` lets a value hang from the `=` line, `broken` telling whether the author wrote it below the `=`.
  private def hangsBy(policy: SingleBodyPolicy, broken: Boolean): Boolean =
    policy match {
      case SingleBodyPolicy.AlwaysJoin  => true
      case SingleBodyPolicy.Keep        => !broken
      case SingleBodyPolicy.AlwaysBreak => false
    }

  // The value can hang from the `=`: by its shape, with no comments between `=` and it, and the policy allows hanging
  // given whether the author wrote it below.
  private def hangsFromEq(assign: Term.Assign, eq: Token): Boolean =
    val rhsStart  = ctx.span(assign.rhs)._1
    val noComment = ctx.commentsBetween(eq.start, rhsStart).isEmpty
    val byPolicy  = hangsBy(ctx.config.body.namedArgument, ctx.slice(eq.end, rhsStart).contains('\n'))

    hangs(assign.rhs) && noComment && byPolicy

  // A value that can hang from the `=` line of a named argument.
  private def hangs(value: Term): Boolean =
    value match {
      case _: Term.Apply | _: Term.ApplyType | _: Term.Select | _: Term.New | _: Term.NewAnonymous | _: Term.Tuple =>
        true
      case _ =>
        false
    }

  // The braced value, when it is a block or a partial function.
  private def bracedValue(assign: Term.Assign, indent: Int, column: Int): Option[Doc] =
    if ctx.hasGapComments(assign) then None
    else applications.braced(assign.rhs, indent, column)

}
