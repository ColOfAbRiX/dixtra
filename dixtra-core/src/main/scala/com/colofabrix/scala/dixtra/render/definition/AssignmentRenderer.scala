package com.colofabrix.scala.dixtra.render.definition

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.model.SingleBodyPolicy
import com.colofabrix.scala.dixtra.render.*
import com.colofabrix.scala.dixtra.render.context.RenderContext
import com.colofabrix.scala.dixtra.render.term.*
import scala.meta.*

/**
 * Renders definitions with a header kept as written followed by `=` and a body: `val` / `lazy val`, `var`,
 * parameterless `def` and `given` aliases.
 *
 * The body follows the rules of [[com.colofabrix.scala.dixtra.render.term.BodyLayout]], placed by the `body` setting of
 * its keyword (`body.val`, `body.lazy-val`, `body.var`, `body.def`, `body.given-alias`): it drops below the `=` when it
 * does not fit or renders multiline, and a newline after `=` is an author signal for it. Only a newline *after* the `=`
 * counts: a break earlier in the header (e.g. after an annotation) is not a signal. The header can be padded so that
 * the `=` lines up with the definitions around it (see [[com.colofabrix.scala.dixtra.render.Alignment]]).
 *
 * @param ctx The shared render context.
 * @param terms The term renderer used to lay out the right-hand side.
 */
final class AssignmentRenderer(ctx: RenderContext, terms: TermRenderer) {

  /**
   * Render an assignment given its defining node and right-hand-side term, with its header padded to `padTo` columns
   * when given and its body below the `=` when `below`, or None when it must be kept as written.
   */
  def render(node: Tree, body: Term, baseIndent: Int, padTo: Option[Int], below: Boolean): Option[Doc] =
    ctx
      .equalsBefore(node.pos.start, ctx.span(body)._1)
      .map { eq =>
        val name      = ctx.trimmedSourceDoc(node.pos.start, eq.start)
        val header    = name ++ Alignment.padding(padTo, Render.flatWidth(name)) ++ Doc.text(" =")
        val headerEnd = ctx.columnAfterSlice(header, node.pos.start, eq.end, baseIndent)
        val rhs       = BodyLayout.of(ctx, terms, eq.end, body, baseIndent + 2, Constructs.of(node))

        if below then
          rhs.belowSeparator(header)
        else
          rhs.placed(header, ctx.fits(_, headerEnd + 1), policy(node))
      }

  // The policy of the body of a definition, by its keyword.
  private def policy(node: Tree): SingleBodyPolicy =
    node match {
      case valDef: Defn.Val if valDef.mods.exists(_.is[Mod.Lazy]) =>
        ctx
          .config
          .body
          .lazyVal
      case _: Defn.Var =>
        ctx
          .config
          .body
          .varBody
      case _: Defn.Def =>
        ctx
          .config
          .body
          .defBody
      case _: Defn.GivenAlias =>
        ctx
          .config
          .body
          .givenAlias
      case _ =>
        ctx
          .config
          .body
          .valBody
    }

  /**
   * The width of the header before the '=', when it is on one line.
   */
  def headerWidth(node: Tree, body: Term): Option[Int] =
    ctx
      .equalsBefore(node.pos.start, ctx.span(body)._1)
      .flatMap { eq =>
        Render.flatWidth(ctx.trimmedSourceDoc(node.pos.start, eq.start))
      }

}
