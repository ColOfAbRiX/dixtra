package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.model.SemicolonPolicy
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*

/**
 * The statements of a body written on one line and separated by `;`, laid out by `body.semicolon-statements`.
 */
private[render] object SemicolonStatements {

  /**
   * Whether `body` stays as written, on one line with its braces: with `keep`, when it is written on one line, holds a
   * `;` and has no comment.
   */
  def keptOnLine(ctx: RenderContext, body: Tree): Boolean =
    val text     = ctx.slice(body.pos.start, body.pos.end)
    val policy   = ctx.config.body.semicolonStatements
    val keep     = policy == SemicolonPolicy.Keep
    val oneLine  = !text.contains('\n') && text.contains(';')
    val noRemark = ctx.commentsBetween(body.pos.start, body.pos.end).isEmpty

    keep && oneLine && noRemark

}
