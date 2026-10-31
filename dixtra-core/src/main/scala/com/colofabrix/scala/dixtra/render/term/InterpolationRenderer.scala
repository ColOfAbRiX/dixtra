package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*

/**
 * Renders interpolated strings.
 *
 * The text parts are never touched. Each `${ ... }` splice holding a single expression is rendered inline as `${expr}`
 * when the expression fits on one line, so author line breaks inside the braces go away. Splices with several
 * statements or comments, and simple `$name` splices, are kept as written.
 *
 * @param ctx The shared render context.
 * @param terms The term renderer, for splice expressions.
 */
private[term] final class InterpolationRenderer(ctx: RenderContext, terms: TermRenderer) {

  /**
   * Render an interpolated string.
   */
  def render(interpolate: Term.Interpolate): Doc =
    val parts = interpolate.parts

    val splices =
      interpolate
        .args
        .zip(parts.zip(parts.drop(1)))
        .map { case (arg, (before, after)) =>
          spliceText(arg).getOrElse(ctx.slice(before.pos.end, after.pos.start))
        }

    val lastPart = parts.length - 1

    val texts =
      parts
        .zipWithIndex
        .map { case (part, i) =>
          val start = if i == 0 then interpolate.pos.start else part.pos.start
          val end   = if i == lastPart then interpolate.pos.end else part.pos.end
          ctx.slice(start, end)
        }

    val spliced =
      splices
        .zip(texts.drop(1))
        .map { case (splice, textPart) => splice + textPart }
        .mkString
    val text      = texts.headOption.getOrElse("") + spliced
    val lineCount = text.count(_ == '\n')

    if lineCount == 0 then
      Doc.Text(text)
    else
      Doc.Verbatim(text, sourceIndent = 0, frozenLines = (1 to lineCount).toSet)

  // The inline `${expr}` form of a braced single-expression splice, if it fits on one line and carries no comments.
  private def spliceText(arg: Term): Option[String] =
    def inlineSplice(block: Term.Block): Boolean =
      ctx.writtenWithBraces(block) && ctx.commentsBetween(block.pos.start, block.pos.end).isEmpty

    arg match {
      case block @ Term.Block((single: Term) :: Nil) if inlineSplice(block) =>
        val inner = terms.render(single, 0, forceExplode = false)
        Render.flatWidth(inner).map(_ => "${" + Render.render(inner) + "}")
      case _ =>
        None
    }

}
