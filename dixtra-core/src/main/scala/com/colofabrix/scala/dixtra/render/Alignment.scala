package com.colofabrix.scala.dixtra.render

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.annotation.tailrec
import scala.meta.*

/**
 * Vertical alignment of a separator (`=`, `=>`, `->`) across a run of consecutive one-line items, decided before line
 * breaking so the padding counts towards the max column.
 *
 * Items form a group while they sit on consecutive lines (one line break apart, with no comment line between them
 * unless `alignment.across-comments`) and each is eligible. Within a group the text before the separator is padded to
 * the widest one; an item that would not fit once padded leaves the group, and the rest are realigned without it. A
 * definition or a `case` that would not fit breaks after its `=` or `=>` instead, ending its group.
 */
private[render] object Alignment {

  /**
   * How adjacent items form a group.
   */
  enum Grouping {

    /**
     * Statements: on consecutive lines, with no comment line between them.
     */
    case Lines

    /**
     * The elements of a list laid out one per line: also when written on the same line or with blank lines between them
     * (which lists do not keep), as long as no comment line separates them.
     */
    case ListElements

  }

  /**
   * The spaces that pad a text of `width` columns to `padTo`, when it is aligned and narrower.
   */
  def padding(padTo: Option[Int], width: Option[Int]): Doc =
    (padTo, width) match {
      case (Some(target), Some(w)) if target > w => Doc.text(" " * (target - w))
      case _                                     => Doc.Empty
    }

  /**
   * The width to pad each item's text to before its separator, or None for an item that is not aligned.
   *
   * @param items The items, in source order.
   * @param grouping How adjacent items form a group.
   * @param eligible True for an item that can be aligned (renders on one line).
   * @param width The width of the item's text before the separator.
   * @param fitsAt True when the item still renders on one line padded to a width.
   */
  def targets[T <: Tree](
    ctx: RenderContext,
    items: List[T],
    grouping: Grouping,
  )(eligible: T => Boolean,
    width: T => Option[Int],
    fitsAt: (T, Int) => Boolean,
  ): List[Option[Int]] =
    aligned(items, adjacency[T](ctx, grouping))(eligible, width, fitsAt)

  /**
   * What the alignment of a group decides for one of the items in it (see [[groupedTargets]]).
   */
  enum Placement {

    /**
     * Aligned: the text before the separator is padded to `target` columns.
     */
    case Padded(target: Int)

    /**
     * Too long to fit once padded: the item breaks after its separator (its body goes below it), which ends its group.
     */
    case Broken

  }

  /**
   * Like [[targets]] for statements, where two adjacent items group only when `sameGroup` holds for them (e.g.
   * definitions whose keywords are in the same alignment group). An item that would not fit once padded is broken after
   * its separator instead of left out: the items before and after it align among themselves.
   *
   * @return The placement of each item, or None for an item that is not aligned.
   */
  def groupedTargets[T <: Tree](
    ctx: RenderContext,
    items: List[T],
    sameGroup: (T, T) => Boolean,
  )(eligible: T => Boolean,
    width: T => Option[Int],
    fitsAt: (T, Int) => Boolean,
  ): List[Option[Placement]] =
    val indexed    = items.toVector
    val candidates = indexed.map(item => eligible(item) && width(item).isDefined)
    val adjacency  = (prev: T, next: T) => sameGroup(prev, next) && adjacent(ctx, prev, next)

    val settled =
      runs(indexed, candidates, adjacency)
        .flatMap { group =>
          split(group, i => width(indexed(i)).getOrElse(0), (i, w) => fitsAt(indexed(i), w))
        }
        .toMap

    indexed.indices.toList.map(settled.get)

  /**
   * Column by column alignment of items with several separators, like the operators of `"org" %% "lib" % "1.0"`: the
   * k-th separators of the items that have one start in the same column, after the columns before them are aligned. An
   * item with fewer separators stops aligning after its last one.
   *
   * @param items The items, in source order.
   * @param grouping How adjacent items form a group.
   * @param eligible True for an item that can be aligned (renders on one line).
   * @param segments The widths of the item's text before each of its separators, each measured from the previous
   *                 separator.
   * @param fitsAt True when the item still renders on one line with the given padding before each of its separators.
   * @return The padding before each separator of an item, or None for an item that is not aligned.
   */
  def columnTargets[T <: Tree](
    ctx: RenderContext,
    items: List[T],
    grouping: Grouping,
  )(eligible: T => Boolean,
    segments: T => Option[List[Int]],
    fitsAt: (T, List[Int]) => Boolean,
  ): List[Option[List[Int]]] =
    val indexed    = items.toVector
    val candidates = indexed.map(item => eligible(item) && segments(item).exists(_.nonEmpty))
    val settled =
      runs(indexed, candidates, adjacency[T](ctx, grouping))
        .flatMap { group =>
          settleColumns(group, i => segments(indexed(i)).getOrElse(Nil), (i, p) => fitsAt(indexed(i), p))
        }
        .toMap
    indexed.indices.toList.map(settled.get)

  private def adjacency[T <: Tree](ctx: RenderContext, grouping: Grouping): (T, T) => Boolean =
    grouping match {
      case Grouping.Lines        => (prev, next) => adjacent(ctx, prev, next)
      case Grouping.ListElements => (prev, next) => listAdjacent(ctx, prev, next)
    }

  private def aligned[T <: Tree](
    items: List[T],
    adjacent: (T, T) => Boolean,
  )(eligible: T => Boolean,
    width: T => Option[Int],
    fitsAt: (T, Int) => Boolean,
  ): List[Option[Int]] =
    val indexed    = items.toVector
    val candidates = indexed.map(item => eligible(item) && width(item).isDefined)
    val groups     = runs(indexed, candidates, adjacent)
    val settled =
      groups
        .flatMap { group =>
          settle(group, i => width(indexed(i)).getOrElse(0), (i, w) => fitsAt(indexed(i), w))
        }
        .toMap
    indexed.indices.toList.map(settled.get)

  // Index runs of consecutive eligible items.
  private def runs[T <: Tree](
    items: Vector[T],
    candidates: Vector[Boolean],
    adjacent: (T, T) => Boolean,
  ): List[List[Int]] =
    val joins =
      items
        .indices
        .map { i =>
          i > 0 && candidates(i - 1) && candidates(i) && adjacent(items(i - 1), items(i))
        }

    val (done, current) =
      items
        .indices
        .foldLeft((List.empty[List[Int]], List.empty[Int])) { case ((acc, run), i) =>
          if !candidates(i) then (acc :+ run, Nil)
          else if joins(i) then (acc, run :+ i)
          else (acc :+ run, List(i))
        }

    (done :+ current).filter(_.length > 1)

  // True when `next` starts on the line after `prev` ends, with no comment line in between (a comment trailing `prev`
  // on its own line is allowed). With `alignment.across-comments` comment lines are allowed too, but not blank lines.
  private def adjacent(ctx: RenderContext, prev: Tree, next: Tree): Boolean =
    val prevEnd   = ctx.span(prev)._2
    val nextStart = ctx.span(next)._1
    val gap       = ctx.slice(prevEnd, nextStart)
    val newline   = gap.indexOf('\n')
    if ctx.config.alignment.acrossComments then
      // Each comment becomes one character, so only the blank lines stay blank
      val comments = ctx.commentsBetween(prevEnd, nextStart)
      val lines    = comments.foldRight(gap)((c, g) => g.patch(c.start - prevEnd, "c", c.end - c.start)).split("\n", -1)
      lines.length >= 2 && lines.drop(1).dropRight(1).forall(_.trim.nonEmpty)
    else
      gap.count(_ == '\n') == 1 && ctx.commentsBetween(prevEnd + newline, nextStart).isEmpty

  // True when no comment line separates two list elements (a comment trailing `prev` on its line is allowed; blank
  // lines are not kept in lists), or always with `alignment.across-comments`.
  private def listAdjacent(ctx: RenderContext, prev: Tree, next: Tree): Boolean =
    val prevEnd   = ctx.span(prev)._2
    val nextStart = ctx.span(next)._1
    val newline   = ctx.slice(prevEnd, nextStart).indexOf('\n')
    if ctx.config.alignment.acrossComments then true
    else if newline < 0 then ctx.commentsBetween(prevEnd, nextStart).isEmpty
    else ctx.commentsBetween(prevEnd + newline, nextStart).isEmpty

  // Like [[settle]], with a padding for each separator of each member.
  @tailrec
  private def settleColumns(
    group: List[Int],
    segments: Int => List[Int],
    fitsAt: (Int, List[Int]) => Boolean,
  ): List[(Int, List[Int])] =
    if group.length < 2 then
      Nil
    else
      val padded = group.zip(paddings(group.map(segments)))
      val kept   = padded.filter(fitsAt.tupled).map(_._1)
      if kept.length == group.length then padded
      else settleColumns(kept, segments, fitsAt)

  // The padding before each separator of each row, so that the k-th separators of the rows that have one start in the
  // same column.
  private def paddings(rows: List[List[Int]]): List[List[Int]] =
    val columns = rows.map(_.length).foldLeft(0)(math.max)
    val start   = rows.map(_ => (0, List.empty[Int]))
    val result =
      (0 until columns).foldLeft(start) { (acc, k) =>
        val ends   = rows.zip(acc).map { case (row, (from, _)) => row.lift(k).map(from + _) }
        val column = ends.flatten.foldLeft(0)(math.max)
        acc
          .zip(ends)
          .map {
            case ((_, pads), Some(end)) => (column, pads :+ (column - end))
            case (state, None)          => state
          }
      }
    result.map(_._2)

  // Pad a group to its widest member. The members that do not fit are broken, and the members between them are settled
  // again as groups of their own.
  private def split(group: List[Int], width: Int => Int, fitsAt: (Int, Int) => Boolean): List[(Int, Placement)] =
    if group.length < 2 then
      Nil
    else
      val target = group.map(width).foldLeft(0)(math.max)
      val broken = group.filterNot(fitsAt(_, target))
      if broken.isEmpty then
        group.map(i => (i, Placement.Padded(target)))
      else
        val between =
          group.foldRight(List(List.empty[Int])) {
            case (i, runs) if broken.contains(i) => Nil :: runs
            case (i, current :: rest)            => (i :: current) :: rest
            case (i, Nil)                        => List(List(i))
          }
        broken.map(i => (i, Placement.Broken)) ++ between.flatMap(split(_, width, fitsAt))

  // Pad a group to its widest member, dropping members that no longer fit, until every remaining member fits. Groups of
  // fewer than two members are not aligned.
  @tailrec
  private def settle(group: List[Int], width: Int => Int, fitsAt: (Int, Int) => Boolean): List[(Int, Int)] =
    if group.length < 2 then
      Nil
    else
      val target = group.map(width).foldLeft(0)(math.max)
      val kept   = group.filter(fitsAt(_, target))
      if kept.length == group.length then group.map(i => (i, target))
      else settle(kept, width, fitsAt)

}
