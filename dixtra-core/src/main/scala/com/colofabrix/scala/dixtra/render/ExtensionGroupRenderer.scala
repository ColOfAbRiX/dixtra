package com.colofabrix.scala.dixtra.render

import com.colofabrix.scala.dixtra.model.Construct
import com.colofabrix.scala.dixtra.render.context.RenderContext
import com.colofabrix.scala.dixtra.verify.Equivalence
import scala.meta.*

/**
 * Renders an `extension` group with its members formatted in place, in braces or braceless by the brace style of
 * `extension`.
 *
 * @param ctx The shared render context.
 * @param renderRegion Renders the members found in a region of the source (supplied by the router to break the
 *                     recursion cycle).
 */
final class ExtensionGroupRenderer(ctx: RenderContext, renderRegion: (Int, Int, List[Stat]) => String) {

  /**
   * The `extension` group `e`. Only a group of several members on their own lines, whose `{` ends the header line and
   * whose `}` stands on its own line, changes style; an `end` marker is not a member.
   */
  def render(e: Defn.ExtensionGroup): String =
    val members  = extensionMembers(e)
    val rendered = renderRegion(e.pos.start, e.pos.end, members)

    val restyled =
      (e.body, members.headOption) match {
        case (block: Term.Block, Some(first)) if members.count(!_.is[Term.EndMarker]) > 1 =>
          val braced = ctx.writtenWithBraces(block)
          if ctx.usesBraces(Construct.Extension, braced) == braced then
            None
          else if braced then
            unbraced(e, block, first, rendered)
          else
            withBraces(e, first, rendered)
        case _ =>
          None
      }

    val result = restyled.filter(Equivalence.isEquivalentStat(e, _)).getOrElse(rendered)

    members
      .headOption
      .flatMap { first =>
        ctx.previousSignificant(first.pos.start).filter(o => ctx.slice(o.end, first.pos.start).contains('\n'))
      }
      .map { open =>
        EdgeBlankLines(result, ctx.slice(e.pos.start, open.start).count(_ == '\n'), Construct.Extension, ctx)
      }
      .getOrElse(result)

  // The methods of an `extension` group: a single one or a block of them.
  private def extensionMembers(e: Defn.ExtensionGroup): List[Stat] =
    e.body match {
      case block: Term.Block => block.stats
      case single            => List(single)
    }

  // The rendered braced group `e` without its braces.
  private def unbraced(e: Defn.ExtensionGroup, block: Term.Block, first: Stat, rendered: String): Option[String] =
    val at = block.pos.start - e.pos.start

    val lines =
      rendered
        .drop(at + 1)
        .split("\n", -1)
        .toList

    val bracedBlock = rendered.lift(at).contains('{') && lines.lastOption.exists(_.trim == "}")
    val blankFirst  = lines.headOption.exists(_.isBlank)
    val noComments  = ctx.commentsBetween(block.pos.start, first.pos.start).isEmpty

    val unwrappable = bracedBlock && blankFirst && noComments

    Option.when(unwrappable) {
      rendered.take(at).stripTrailing + lines.dropRight(1).mkString("\n").stripTrailing
    }

  // The rendered braceless group `e` in braces.
  private def withBraces(e: Defn.ExtensionGroup, first: Stat, rendered: String): Option[String] =
    val at     = first.pos.start - e.pos.start
    val header = rendered.take(at)
    val end    = header.stripTrailing.length

    val brokenHeader = header.drop(end).contains('\n')
    val noComments   = ctx.commentsBetween(e.pos.start, first.pos.start).isEmpty
    val indent       = " " * ctx.lineIndentOf(e.pos.start)

    Option.when(brokenHeader && noComments) {
      header.take(end) +
      " {" +
      header.drop(end) +
      rendered.drop(at) +
      "\n" +
      indent + "}"
    }

}
