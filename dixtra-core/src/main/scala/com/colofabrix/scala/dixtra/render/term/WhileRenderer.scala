package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*
import scala.meta.tokens.Token

/**
 * Renders `while` loops.
 *
 * A loop written on one line that fits in the syntax of its brace style is kept
 * as written, unless `body.while = always-break`. Otherwise it is braceless,
 * `while cond do` with the body placed by `body.while` (see [[BodyLayout.singleBelow]]), or in the braces syntax, by
 * the brace style: `do` only in the braceless form, parentheses only in the braces one. In the braces syntax the body
 * placed on its line puts the loop on one line without braces (`while (cond) f()`), and below it the body is in braces.
 * A condition written over several lines keeps the loop as written.
 *
 * @param ctx The shared render context.
 * @param terms The term renderer, for the body.
 */
private[term] final class WhileRenderer(ctx: RenderContext, terms: TermRenderer) {

  /**
   * Render a `while` loop at the given indent.
   */
  def render(whileTerm: Term.While, indent: Int): Doc =
    val inline = ctx.ownSourceDoc(whileTerm)
    val cond   = ctx.ownSourceDoc(whileTerm.expr)

    val policy =
      ctx
        .config
        .body
        .whileBody

    val hasDo =
      ctx
        .tokensBetween(ctx.span(whileTerm.expr)._2, ctx.span(whileTerm.body)._1)
        .exists(_.is[Token.KwDo])

    val braces     = ctx.usesBraces(Construct.While, !hasDo)
    val inlineFits = ctx.fits(inline, indent)

    val asWritten =
      braces != hasDo &&
      policy != SingleBodyPolicy.AlwaysBreak &&
      !whileTerm.pos.text.contains('\n') &&
      inlineFits

    if asWritten || !Render.isFlat(cond) then
      inline
    else if braces then
      val body   = terms.renderBody(BodyLayout.unwrapped(ctx, whileTerm.body), indent + 2, forceExplode = false)
      val onLine = Doc.text("while (") ++ cond ++ Doc.text(") ") ++ body
      val below  = BodyLayout.writtenBelow(ctx, ctx.span(whileTerm.expr)._2, whileTerm.body)
      val member = BodyLayout.Member(below, ctx.fits(onLine, indent), BodyLayout.isSimple(ctx, whileTerm.body))

      if BodyLayout.singleBelow(policy, member) then
        val opening = Doc.text("while (") ++ cond ++ Doc.text(") ")
        opening ++ Doc.braced(terms.bracedBody(whileTerm.body, indent + 2, Construct.While))
      else
        onLine
    else
      val body   = terms.renderBody(whileTerm.body, indent + 2, forceExplode = false)
      val head   = Doc.text("while ") ++ cond ++ Doc.text(" do")
      val onLine = head ++ Doc.text(" ") ++ body
      val below  = BodyLayout.writtenBelow(ctx, ctx.span(whileTerm.expr)._2, whileTerm.body)
      val member = BodyLayout.Member(below, ctx.fits(onLine, indent), BodyLayout.isSimple(ctx, whileTerm.body))
      
      if BodyLayout.singleBelow(policy, member) then
        head ++ Doc.Nest(2, Doc.Line ++ body)
      else
        onLine

}
