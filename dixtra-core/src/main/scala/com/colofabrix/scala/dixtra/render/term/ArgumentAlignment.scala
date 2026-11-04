package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.render.*
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*

/**
 * The layout of exploded arguments: consecutive one-line `->` pairs, operators and named arguments aligned column by
 * column, and the comma that counts towards the max column (see [[Alignment]]).
 *
 * @param ctx The shared render context.
 * @param terms The term renderer, for the widths and paddings of infix operators and named arguments.
 * @param operand Renders a term in operand position.
 */
private[term] final class ArgumentAlignment(ctx: RenderContext, terms: TermRenderer, operand: (Term, Int) => Doc) {

  // The exploded arguments, aligning the `->` of consecutive one-line pairs and, column by column, the operators of
  // consecutive one-line operations from `alignment.operators` (see [[Alignment]]); the comma after each argument
  // counts towards the max column. Named arguments are not aligned.
  def aligned(args: List[Term], indent: Int): List[Doc] =
    val last = args.length - 1
    val docs =
      args
        .zipWithIndex
        .map {
          case (assign: Term.Assign, i) if !ctx.isParenthesized(assign) =>
            withComma(i < last, indent, at => terms.namedArgument(assign, at, None))
          case (a, i) =>
            withComma(i < last, indent, operand(a, _))
        }

    val lines =
      args
        .zip(docs)
        .map { case (a, d) => a.pos.start -> Render.flatWidth(d) }
        .toMap

    val line = (a: Term) => lines.get(a.pos.start).flatten
    val operators =
      ctx
        .config
        .alignment
        .operators
        .toSet

    val targets = arrowTargets(args, indent, line)
    val pads    = operatorPads(args, indent, operators, line)
    val names   = nameTargets(args, indent, line)

    args
      .zip(docs)
      .zip(targets.zip(pads).zip(names))
      .map {
        case ((pair: Term.ApplyInfix, doc), ((Some(target), _), _)) =>
          terms.paddedInfix(pair, indent, target).getOrElse(doc)
        case ((infix: Term.ApplyInfix, doc), ((_, Some(p)), _)) =>
          terms.paddedInfixColumns(infix, indent, operators, p).getOrElse(doc)
        case ((assign: Term.Assign, _), (_, Some(target))) =>
          terms.namedArgument(assign, indent, Some(target))
        case ((_, doc), _) =>
          doc
      }

  // An exploded argument: one that fits on its line only without its comma is laid out a column further, so that it
  // breaks as it does not fit with the comma. Every argument has one, but the last without
  // `call-site.trailing-commas = always`.
  private def withComma(followed: Boolean, indent: Int, render: Int => Doc): Doc =
    val doc         = render(indent)
    val comma       = followed || ctx.config.callSite.trailingCommas == TrailingCommas.Always
    val exactlyFull = Render.flatWidth(doc).exists(w => indent + w == ctx.config.maxColumn)

    if comma && exactlyFull then
      render(indent + 1)
    else
      doc

  // The width to pad the name of each named argument to (see `aligned`), given the width of each argument on one line,
  // with `alignment.named-arguments`.
  private def nameTargets(args: List[Term], indent: Int, line: Term => Option[Int]): List[Option[Int]] =
    def name(arg: Term): Option[Int] =
      arg match {
        case assign: Term.Assign if !ctx.isParenthesized(assign) => terms.namedArgumentWidth(assign)
        case _                                                   => None
      }

    Alignment.targets(ctx, args, Alignment.Grouping.ListElements)(
      eligible = arg => ctx.config.alignment.namedArguments && line(arg).isDefined && name(arg).isDefined,
      width = name,
      fitsAt = { (arg, target) =>
        (line(arg), name(arg)) match {
          case (Some(1), Some(w)) => ctx.fitsWidth(indent, 1 + math.max(0, target - w) + 1)
          case _                  => false
        }
      },
    )

  // The width to pad the left side of each `a -> b` pair to (see `aligned`), given the width of each argument on one
  // line.
  private def arrowTargets(args: List[Term], indent: Int, line: Term => Option[Int]): List[Option[Int]] =
    def left(arg: Term): Option[Int] =
      arg match {
        case pair: Term.ApplyInfix if pair.op.value == "->" => terms.infixLeftWidth(pair, indent)
        case _                                              => None
      }

    Alignment.targets(ctx, args, Alignment.Grouping.ListElements)(
      eligible = arg => ctx.config.alignment.arrows.pairs && line(arg).isDefined,
      width = left,
      fitsAt = { (arg, target) =>
        (line(arg), left(arg)) match {
          case (Some(l), Some(w)) => ctx.fitsWidth(indent, l + math.max(0, target - w) + 1)
          case _                  => false
        }
      },
    )

  // The padding before each operator of each operation from `operators` (see `aligned`), given the width of each
  // argument on one line. Pairs are left to `arrowTargets` while their arrows align.
  private def operatorPads(
    args: List[Term],
    indent: Int,
    operators: Set[String],
    line: Term => Option[Int],
  ): List[Option[List[Int]]] =
    val segments: Term => Option[List[Int]] = {
      case pair: Term.ApplyInfix if pair.op.value == "->" && ctx.config.alignment.arrows.pairs =>
        None
      case infix: Term.ApplyInfix =>
        terms.infixSegments(infix, indent, operators)
      case _ =>
        None
    }

    Alignment.columnTargets(ctx, args, Alignment.Grouping.ListElements)(
      eligible = a => line(a).isDefined,
      segments = segments,
      fitsAt = (a, p) => line(a).exists(l => ctx.fitsWidth(indent, l + p.sum + 1)),
    )

}
