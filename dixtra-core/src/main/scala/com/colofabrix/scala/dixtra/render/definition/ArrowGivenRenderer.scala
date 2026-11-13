package com.colofabrix.scala.dixtra.render.definition

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.render.*
import com.colofabrix.scala.dixtra.render.ClauseGroups.Group
import com.colofabrix.scala.dixtra.render.comments.CommentedList
import com.colofabrix.scala.dixtra.render.context.RenderContext
import com.colofabrix.scala.dixtra.render.term.*
import scala.meta.*
import scala.meta.tokens.Token

/**
 * Renders a `given` alias in the syntax of Scala 3.6, its parameters before its type, each followed by `=>`:
 * `given listOrd: [A: Ord] => Ord[List[A]] = ...`.
 *
 * The signature stays on one line when it fits with its ` =`; otherwise each value clause breaks, one parameter per
 * line, its `=>` after the closing `)`. Type clauses never break. The body follows `body.given-alias`, as for any
 * `given` alias.
 *
 * @param ctx The shared render context.
 * @param terms The term renderer used to lay out the body.
 */
private[definition] final class ArrowGivenRenderer(ctx: RenderContext, terms: TermRenderer) {

  /**
   * The `given`, its body below the `=` when `below`, or None to keep it as written: a comment in its signature.
   */
  def render(givenAlias: Defn.GivenAlias, baseIndent: Int, below: Boolean): Option[Doc] =
    for
      lead <- leadDoc(givenAlias)
      eq   <- ctx.equalsBefore(givenAlias.decltpe.pos.end, ctx.span(givenAlias.body)._1)
              if ctx.commentsBetween(givenAlias.pos.start, eq.start).isEmpty
    yield
      val signature = signatureDoc(givenAlias, lead, baseIndent)
      val body      = BodyLayout.of(ctx, terms, eq.end, givenAlias.body, baseIndent + 2, Some(Construct.GivenAlias))
      placed(signature ++ Doc.text(" ="), body, baseIndent, below)

  /**
   * Whether `givenAlias` is written in the syntax of Scala 3.6: a `=>` after its parameters, outside any parentheses or
   * brackets (the position of a clause group already includes it).
   */
  def isArrowSyntax(givenAlias: Defn.GivenAlias): Boolean =
    givenAlias
      .paramClauseGroups
      .headOption
      .exists { first =>
        val tokens = ctx.tokensBetween(first.pos.start, givenAlias.decltpe.pos.start)
        topLevelArrow(tokens.toList)
      }

  // Whether a `=>` appears among `tokens` at depth 0, outside parentheses and brackets.
  private def topLevelArrow(tokens: List[Token]): Boolean =
    val depths =
      tokens.scanLeft(0) { (depth, token) =>
        token match {
          case _: Token.LeftParen | _: Token.LeftBracket   => depth + 1
          case _: Token.RightParen | _: Token.RightBracket => depth - 1
          case _                                           => depth
        }
      }

    tokens
      .zip(depths)
      .exists { (token, depth) => token.is[Token.RightArrow] && depth == 0 }

  // The text before the parameters: the modifiers and `given` as written, then ` name:` for a named one.
  private def leadDoc(givenAlias: Defn.GivenAlias): Option[Doc] =
    val keywordEnd =
      ctx
        .tokensBetween(givenAlias.pos.start, givenAlias.decltpe.pos.start)
        .collectFirst { case t: Token.KwGiven => t.end }

    val name = givenAlias.name.value

    keywordEnd.map { end =>
      val named = if name.isEmpty then Doc.Empty else Doc.text(s" $name:")
      ctx.sourceDoc(givenAlias.pos.start, end) ++ named
    }

  // The signature on one line when it fits with its ` =`, else with its value clauses broken.
  private def signatureDoc(givenAlias: Defn.GivenAlias, lead: Doc, indent: Int): Doc =
    val clauses    = givenAlias.paramClauseGroups.flatMap(clausesOf)
    val returnType = ctx.sourceDoc(givenAlias.decltpe)
    val oneLine    = joined(lead, clauses.map(_.inline), returnType)

    if ctx.fits(oneLine ++ Doc.text(" ="), indent) then
      oneLine
    else
      joined(lead, clauses.map(_.broken), returnType)

  private def joined(lead: Doc, clauses: List[Doc], returnType: Doc): Doc =
    lead ++ Doc.text(" ") ++ Doc.intercalate(Doc.text(" => "), clauses :+ returnType)

  // A clause of the signature on one line, and broken.
  private final case class ArrowClause(inline: Doc, broken: Doc)

  // The clauses of a group: its type clause, which never breaks, then its value clauses.
  private def clausesOf(group: Member.ParamClauseGroup): List[ArrowClause] =
    val typeClause =
      Option
        .when(group.tparamClause.values.nonEmpty)(ctx.sourceDoc(group.tparamClause))
        .map(doc => ArrowClause(doc, doc))

    val valueClauses =
      group
        .paramClauses
        .collect { case clause: Term.ParamClause => valueClause(clause) }

    typeClause.toList ++ valueClauses

  private def valueClause(clause: Term.ParamClause): ArrowClause =
    val commas =
      ctx
        .config
        .definitionSite
        .trailingCommas

    val comments = CommentedList.of(ctx, clause.pos.start, clause.values, clause.pos.end, commas)
    val group    = Group("", Syntax.clauseKeyword(clause.mod), clause.values.map(ctx.sourceDoc), comments)

    val broken =
      if clause.values.isEmpty then group.inline
      else Doc.text("(") ++ ClauseGroups.keywordOnOwnLine(group, false) ++ Doc.Line ++ Doc.text(")")

    ArrowClause(group.inline, broken)

  // The body after `lead` (the signature and its `=`), on its line or below it by `body.given-alias`.
  private def placed(lead: Doc, body: BodyLayout, indent: Int, below: Boolean): Doc =
    val policy =
      ctx
        .config
        .body
        .givenAlias

    val down   = below || policy == SingleBodyPolicy.AlwaysBreak
    val column = ctx.endColumn(lead, indent) + 1

    if down then
      body.belowSeparator(lead)
    else
      body.placed(lead, ctx.fits(_, column), policy)

}
