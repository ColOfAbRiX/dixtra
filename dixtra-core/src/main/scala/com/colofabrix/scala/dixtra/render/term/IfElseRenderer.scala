package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.Construct
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.render.comments.CommentGaps
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*
import scala.meta.tokens.Token

/**
 * Renders if/else.
 *
 * The brace style decides between `if cond then ... else ...` and `if (cond) { ... } else { ... }`; the two are never
 * mixed.
 *
 * The branches of an if/else, with those of its `else if` chain, are one group of bodies (`body.if`, see
 * [[BodyLayout.groupBelow]]): either each is on the line of its `then` / `else`, or each is on its own indented line
 * below it, never mixed. On their lines, the if/else stays on one line when the author wrote each `else` on the line of
 * the branch before it, and otherwise each `else` starts a line of its own; an if/else written on one line that does
 * not fit goes below. Below, an `else if` continuation stays on the `else` line, aligned with the outer `if`. With
 * braces, the branches on their lines are the same without braces (`if (cond) a else b`, or each `else` on a line of
 * its own), and below them each branch is in braces. An if/else holding comments keeps the author's line breaks instead
 * (see `commented`).
 *
 * @param ctx The shared render context.
 * @param terms The term renderer, for the branches.
 */
private[term] final class IfElseRenderer(ctx: RenderContext, terms: TermRenderer) {

  /**
   * Render an if/else (or a bare if).
   */
  def render(ifTerm: Term.If, indent: Int): Doc =
    layout(ifTerm, indent, explode = false)

  // The if/else, its branches below their `then` / `else` when `explode`, else by the `body.if` policy.
  private def layout(ifTerm: Term.If, indent: Int, explode: Boolean): Doc =
    val (braces, ifThen) = header(ifTerm)
    val branches         = new Branches(ifTerm, indent, ifThen)
    val policy =
      ctx
        .config
        .body
        .ifBody
    if ctx.hasGapComments(ifTerm) then
      if braces then ctx.ownSourceDoc(ifTerm) else commented(ifTerm, branches, indent)
    else if explode then
      if braces then withBraces(ifTerm, branches, indent) else plain(ifTerm, branches, indent)
    else
      val sameLine = !elsesOnOwnLines(ifTerm)
      val onLines  = branchesOnLines(ifTerm, indent, sameLine).filter(ctx.linesFit(_, indent))
      val members  = simpleMembers(ifTerm, onLines.isDefined)
      onLines
        .filterNot(_ => BodyLayout.groupBelow(policy, members))
        .getOrElse {
          if braces then withBraces(ifTerm, branches, indent) else plain(ifTerm, branches, indent)
        }

  // Whether the if/else uses braces, and its `if (cond)` / `if cond then` part.
  private def header(ifTerm: Term.If): (Boolean, Doc) =
    val condEnd = ctx.span(ifTerm.cond)._2
    val hasThen = ctx.tokensBetween(condEnd, ctx.span(ifTerm.thenp)._1).exists(_.is[Token.KwThen])
    val braces  = ctx.usesBraces(Construct.If, !hasThen)
    val ifThen =
      if braces then Doc.text("if (") ++ ctx.ownSourceDoc(ifTerm.cond) ++ Doc.text(")")
      else Doc.text("if ") ++ ctx.ownSourceDoc(ifTerm.cond) ++ Doc.text(" then")
    (braces, ifThen)

  // The `if`s of an if/else and of its `else if` chain.
  private def chain(ifTerm: Term.If): List[Term.If] =
    ifTerm :: elseIf(ifTerm).toList.flatMap(chain)

 // Whether the author wrote an `else` of the chain on a line of its own.
  private def elsesOnOwnLines(ifTerm: Term.If): Boolean =
    chain(ifTerm).exists { link =>
      elseKeyword(link).exists(kw => ctx.slice(ctx.span(link.thenp)._2, kw.start).contains('\n'))
    }

  // The branches of the chain as members of their group, `fits` telling whether they can be on the lines of their
  // `then` / `else`; a branch holding an if, a match or a try cannot.
  private def simpleMembers(ifTerm: Term.If, fits: Boolean): List[BodyLayout.Member] =
    def member(below: Boolean, branch: Term): BodyLayout.Member =
      BodyLayout.Member(below, fits && !BodyLayout.holdsControl(branch), BodyLayout.isSimple(ctx, branch))
    chain(ifTerm).flatMap { link =>
      val elseMember =
        elseKeyword(link)
          .filter(_ => elseIf(link).isEmpty)
          .map(kw => member(BodyLayout.writtenBelow(ctx, kw.end, link.elsep), link.elsep))
      member(BodyLayout.writtenBelow(ctx, ctx.span(link.cond)._2, link.thenp), link.thenp) :: elseMember.toList
    }

  // The if/else with each branch on the line of its `then` / `else`, each `else` on the line of the branch before it
  // when `sameLine`, else on a line of its own; None when a branch does not render on one line.
  private def branchesOnLines(ifTerm: Term.If, indent: Int, sameLine: Boolean): Option[Doc] =
    val branches = new Branches(ifTerm, indent, header(ifTerm)._2)
    val before   = if sameLine then Doc.text(" else ") else Doc.Line ++ Doc.text("else ")
    val rest =
      (branches.elseKeyword, elseIf(ifTerm)) match {
        case (None, _) =>
          Some(Doc.Empty)
        case (Some(_), Some(nested)) if ctx.hasGapComments(nested) =>
          None
        case (Some(_), Some(nested)) =>
          branchesOnLines(nested, indent, sameLine).map(before ++ _)
        case (Some(_), None) =>
          oneLine(branches.elseDoc).map(before ++ _)
      }
    for
      thenDoc <- oneLine(branches.thenDoc)
      elses   <- rest
    yield
      branches.ifThen ++ Doc.text(" ") ++ thenDoc ++ elses

  private def oneLine(doc: Doc): Option[Doc] =
    Option.when(Render.isFlat(doc))(doc)

  // The parts of an if/else: `if cond then`, the `else` keyword if written, and the branches, which are rendered only
  // when a layout uses them (an exploded `else if` chain renders its nested ifs again, exploded).
  private final class Branches(ifTerm: Term.If, indent: Int, val ifThen: Doc) {

    // A bare `if` has a `()` else branch in the tree, like an explicit `else ()`, which must be kept (`Def.taskIf`
    // needs it): only the keyword tells them apart.
    val elseKeyword: Option[Token] =
      IfElseRenderer.this.elseKeyword(ifTerm)

    lazy val thenDoc: Doc = terms.renderBody(ifTerm.thenp, indent + 2, forceExplode = false)
    lazy val elseDoc: Doc = terms.renderBody(ifTerm.elsep, indent + 2, forceExplode = false)

    lazy val inline: Doc =
      if elseKeyword.isDefined then ifThen ++ Doc.text(" ") ++ thenDoc ++ Doc.text(" else ") ++ elseDoc
      else ifThen ++ Doc.text(" ") ++ thenDoc

  }

  // Each branch on its own line, one level in; an `else if` continues on the `else` line.
  private def plain(ifTerm: Term.If, branches: Branches, indent: Int): Doc =
    val thenPart = branches.ifThen ++ Doc.Nest(2, Doc.Line ++ branches.thenDoc)
    if branches.elseKeyword.isEmpty then
      thenPart
    else
      val elseBranch =
        elseIf(ifTerm) match {
          case Some(nested) => Doc.text(" ") ++ layout(nested, indent, explode = true)
          case None         => Doc.Nest(2, Doc.Line ++ branches.elseDoc)
        }
      thenPart ++ Doc.Line ++ Doc.text("else") ++ elseBranch

  // The braces style: each branch in braces (`if (cond) {`, `} else {`, `}`), an `else if` continuing on the `} else`
  // line.
  private def withBraces(ifTerm: Term.If, branches: Branches, indent: Int): Doc =
    def braced(branch: Term): Doc = Doc.braced(terms.bracedBody(branch, indent + 2, Construct.If))
    val thenPart                  = branches.ifThen ++ Doc.text(" ") ++ braced(ifTerm.thenp)
    if branches.elseKeyword.isEmpty then
      thenPart
    else
      elseIf(ifTerm) match {
        case Some(nested) => thenPart ++ Doc.text(" else ") ++ layout(nested, indent, explode = true)
        case None         => thenPart ++ Doc.text(" else ") ++ braced(ifTerm.elsep)
      }

  private def elseKeyword(ifTerm: Term.If): Option[Token] =
    ctx.tokensBetween(ctx.span(ifTerm.thenp)._2, ifTerm.pos.end).collectFirst { case t: Token.KwElse => t }

  // The `if` of an `else if` continuation.
  private def elseIf(ifTerm: Term.If): Option[Term.If] =
    ifTerm.elsep match {
      case nested: Term.If if !ctx.isParenthesized(nested) => Some(nested)
      case _                                               => None
    }

  // An if/else with comments between its parts keeps the author's choice of line breaks at each break-point (after
  // `then`, before and after `else`), with the comments in place and canonical indentation: branch bodies one level in,
  // `else` aligned with `if`. A comment before the condition keeps the whole term verbatim.
  private def commented(ifTerm: Term.If, branches: Branches, indent: Int): Doc =
    val (thenStart, thenEnd) = ctx.span(ifTerm.thenp)
    val condComments         = ctx.commentsBetween(ifTerm.pos.start, ifTerm.cond.pos.start)
    val thenGap              = CommentGaps.separator(ctx, ifTerm.cond.pos.end, thenStart)
    val thenPart             = branches.ifThen ++ Doc.Nest(2, thenGap ++ branches.thenDoc)
    branches.elseKeyword match {
      case _ if condComments.nonEmpty =>
        ctx.ownSourceDoc(ifTerm)
      case None =>
        thenPart
      case Some(kwElse) =>
        val elseStart = ctx.span(ifTerm.elsep)._1
        val elseGap   = CommentGaps.separator(ctx, kwElse.end, elseStart)
        val elseBranch =
          elseIf(ifTerm) match {
            case Some(nested) if !ctx.slice(kwElse.end, elseStart).contains('\n') =>
              elseGap ++ render(nested, indent)
            case _ =>
              Doc.Nest(2, elseGap ++ branches.elseDoc)
          }
        thenPart ++ CommentGaps.separator(ctx, thenEnd, kwElse.start) ++ Doc.text("else") ++ elseBranch
    }

}
