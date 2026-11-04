package com.colofabrix.scala.dixtra.render

import com.colofabrix.scala.dixtra.model.Construct
import com.colofabrix.scala.dixtra.normalize.SourceEdits
import com.colofabrix.scala.dixtra.render.context.RenderContext
import com.colofabrix.scala.dixtra.verify.Equivalence
import scala.meta.*
import scala.meta.tokens.Token

/**
 * Renders the single package clause of a file, changing between `package a { ... }` and `package a` followed by its
 * members one level out, by the brace style of `package`.
 *
 * @param ctx The shared render context.
 * @param renderRegion Renders the members found in a region of the source (supplied by the router to break the
 *                     recursion cycle).
 */
final class PackageRenderer(ctx: RenderContext, renderRegion: (Int, Int, List[Stat]) => String) {

  /**
   * The single package clause `pkg`, without nested ones, with its members formatted in place and its brace style
   * applied.
   */
  def render(pkg: Pkg): String =
    val rendered = renderRegion(pkg.pos.start, pkg.pos.end, pkg.body.stats)

    val opener =
      pkg
        .body
        .stats
        .headOption
        .flatMap(first => ctx.previousSignificant(first.pos.start))

    val braced = opener.exists(_.is[Token.LeftBrace])

    val noNestedPkg  = !pkg.body.stats.exists(_.is[Pkg])
    val wantsRestyle = ctx.usesBraces(Construct.Package, braced) != braced

    val restyled =
      (opener, pkg.body.stats.headOption) match {
        case (Some(open), Some(_)) if noNestedPkg && wantsRestyle =>
          val head = ctx.slice(pkg.pos.start, if braced then open.start else open.end).stripTrailing

          if braced then
            val body =
              rendered
                .drop(open.end - pkg.pos.start)
                .split("\n", -1)
                .toList

            Option
              .when(body.headOption.exists(_.isBlank) && body.lastOption.exists(_.trim == "}")) {
                body
                  .drop(1)
                  .dropRight(1)
                  .mkString("\n")
              }
              .flatMap(shifted(_, -2))
              .map(members => (head + "\n" + members).stripTrailing)
          else
            val rest = rendered.drop(head.length).stripTrailing
            Option
              .when(rest.takeWhile(_ != '\n').isBlank && rest.contains('\n'))(rest)
              .flatMap(shifted(_, 2))
              .map(members => head + " {" + members + "\n}")
        case _ =>
          None
      }

    val result = restyled.filter(Equivalence.isEquivalentPackage(pkg, _)).getOrElse(rendered)

    def membersOnNextLine(open: Token): Boolean =
      pkg
        .body
        .stats
        .headOption
        .exists { first =>
          !first.is[Pkg] && ctx.slice(open.end, first.pos.start).contains('\n')
        }

    opener
      .filter(membersOnNextLine)
      .map { open =>
        EdgeBlankLines(result, ctx.slice(pkg.pos.start, open.start).count(_ == '\n'), Construct.Package, ctx)
      }
      .getOrElse(result)

  /**
   * Whether rendering `pkg` adds or removes its braces.
   */
  def restyles(pkg: Pkg): Boolean =
    packageBraces(pkg).exists(braced => ctx.usesBraces(Construct.Package, braced) != braced)

  // Whether the single package clause `pkg` is written with braces, when it can change style: it has members and no
  // nested package.
  private def packageBraces(pkg: Pkg): Option[Boolean] =
    pkg
      .body
      .stats
      .headOption
      .filterNot(_ => pkg.body.stats.exists(_.is[Pkg]))
      .flatMap { first =>
        ctx.previousSignificant(first.pos.start).map(_.is[Token.LeftBrace])
      }

  // The lines of `text` indented by `delta` columns (dedented when negative, by at most the spaces a line has), or None
  // when `text` cannot be tokenised. Blank lines become empty, and a line starting inside a multi-line token other than
  // a comment (a string literal) is kept as is: its text is content.
  private def shifted(text: String, delta: Int): Option[String] =
    SourceEdits
      .tokens(text)
      .map { tokens =>
        val literals = tokens.filter(t => !t.is[Token.Comment] && t.text.contains('\n')).toList

        val starts =
          text
            .split("\n", -1)
            .toList
            .scanLeft(0)(_ + _.length + 1)

        text
          .split("\n", -1)
          .toList
          .zip(starts)
          .map { (line, start) =>
            if literals.exists(t => t.start < start && start < t.end) then
              line
            else if line.isBlank then
              ""
            else if delta >= 0 then
              " " * delta + line
            else
              line.drop(math.min(-delta, line.takeWhile(_ == ' ').length))
          }
          .mkString("\n")
      }

}
