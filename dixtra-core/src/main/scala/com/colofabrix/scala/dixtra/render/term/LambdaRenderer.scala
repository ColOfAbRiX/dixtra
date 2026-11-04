package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.model.Construct
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*

/**
 * Renders a lambda written without braces (`x => body`); a `{ x => body }` argument is rendered by
 * [[ApplicationRenderer]].
 *
 * `=>` works like the `=` of a definition (see [[BodyLayout]]): the body is placed by `body.lambda-body`, drops below
 * when it does not fit, and a newline after `=>` is an author signal for the body. The `param =>` header is taken from
 * source so its exact parenthesisation is preserved, and it is never split.
 *
 * @param ctx The shared render context.
 * @param terms The term renderer, for the body.
 */
private[term] final class LambdaRenderer(ctx: RenderContext, terms: TermRenderer) {

  /**
   * Render a lambda whose header starts at `indent`.
   */
  def render(f: Term.Function, indent: Int): Doc =
    val arrowEnd = BodyLayout.arrowEnd(ctx, f)
    val header   = ctx.trimmedSourceDoc(f.pos.start, arrowEnd)
    val body     = BodyLayout.of(ctx, terms, arrowEnd, f.body, indent + 2, Some(Construct.LambdaBody))
    body.placed(header, doc => ctx.fits(header ++ Doc.text(" ") ++ doc, indent), ctx.config.body.lambdaBody)

}
