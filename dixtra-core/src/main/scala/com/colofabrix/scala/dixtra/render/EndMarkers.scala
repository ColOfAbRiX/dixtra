package com.colofabrix.scala.dixtra.render

import com.colofabrix.scala.dixtra.BracePolicy
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*

/**
 * The `end` markers of a statement list.
 *
 * A marker belongs to the statement it closes, written right after it. The owner of the list renders the two as one:
 * the marker the source had is dropped and the brace style decides whether one is written again (see [[marker]]).
 */
private[render] object EndMarkers {

  /**
   * Each statement with the `end` marker that closes it right after it (only whitespace between them); the markers
   * themselves are left out of the list.
   */
  def pair(ctx: RenderContext, stats: List[Stat]): List[(Stat, Option[Term.EndMarker])] =
    def whitespaceBetween(prev: Stat, marker: Term.EndMarker): Boolean =
      ctx.slice(prev.pos.end, marker.pos.start).forall(_.isWhitespace)

    stats
      .foldLeft(List.empty[(Stat, Option[Term.EndMarker])]) {
        case ((prev, None) :: rest, marker: Term.EndMarker)
            if Constructs.closes(prev, marker) && whitespaceBetween(prev, marker) =>
          (prev, Some(marker)) :: rest
        case (acc, stat) =>
          (stat, None) :: acc
      }
      .reverse

  /**
   * The `end` marker to write after `stat`, rendered as `rendered`, when its brace style asks for one: it is braceless
   * (it does not end with `}`) and spans several lines. `existing` is the marker the source had.
   */
  def marker(ctx: RenderContext, stat: Tree, existing: Option[Term.EndMarker], rendered: String): Option[String] =
    def braceless(text: String): Boolean =
      text.trim.contains('\n') && !text.stripTrailing.endsWith("}")

    for
      construct <- Constructs.of(stat)
      name      <- Constructs.endName(stat)
                   if construct.allowsEnd
                   if braceless(rendered)
                   if ctx.braceStyle(construct, braced = false, ended = existing.isDefined) == BracePolicy.ForceEnd
    yield
      "end " + name

}
