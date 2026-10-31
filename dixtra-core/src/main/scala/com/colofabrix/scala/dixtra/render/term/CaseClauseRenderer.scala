package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.render.Alignment
import com.colofabrix.scala.dixtra.render.comments.StatementComments
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*
import scala.meta.tokens.Token

/**
 * Renders the constructs built around case clauses: `match`, `try`/`catch` and the clauses of a `{ case ... }` partial
 * function.
 *
 * `match` and `try`/`catch` always keep braces on their keyword lines and place each `case` on its own line, keeping
 * the author's blank lines and the comments between cases. A clause keeps its `case pattern if guard =>` header as
 * written, and its body follows the rules of a body after `=>` (see [[BodyLayout]]). Consecutive one-line clauses align
 * their `=>` (see [[Alignment]]).
 *
 * @param ctx The shared render context.
 * @param terms The term renderer, for the `try` body and the case bodies.
 */
private[term] final class CaseClauseRenderer(ctx: RenderContext, terms: TermRenderer) {

  /**
   * Render a `match`: braces on the keyword line, each `case` on its own line indented under the scrutinee, which keeps
   * its grouping parentheses. Comments after the `match` keyword stay with the cases; a comment before it keeps the
   * whole `match` as written.
   */
  def renderMatch(matchTerm: Term.Match, indent: Int): Doc =
    val block     = matchTerm.casesBlock
    val exprEnd   = ctx.span(matchTerm.expr)._2
    val kwMatch   = ctx.tokensBetween(exprEnd, block.pos.start).collectFirst { case t: Token.KwMatch => t }
    val commented = kwMatch.forall(kw => ctx.commentsBetween(exprEnd, kw.start).nonEmpty)

    kwMatch match {
      case Some(kw) if !commented =>
        val clauses = (edges: BlankLinePolicy) => cases(kw.end, block.cases, block.pos.end, indent + 2, edges)
        val written =
          ctx
            .tokensBetween(kw.end, block.pos.end)
            .find(!_.is[Token.Whitespace])
            .exists(_.is[Token.LeftBrace])
        if ctx.usesBraces(Construct.Match, written) then
          ctx.sourceDoc(matchTerm.expr) ++
          Doc.text(" match ") ++
          Doc.braced(clauses(ctx.config.blankLines(Construct.Match)))
        else
          ctx.sourceDoc(matchTerm.expr) ++ Doc.text(" match") ++ Doc.Nest(2, Doc.Line ++ clauses(BlankLinePolicy.Never))
      case _ =>
        ctx.ownSourceDoc(matchTerm)
    }

  /**
   * Render a `try` / `catch` / `finally`. Written on one line that fits, it stays as written, unless `body.try` puts
   * its bodies below their keywords (see [[BodyLayout.groupBelow]]). Otherwise each part is in braces (`try {`,
   * `} catch {`, `} finally {`) or braceless, by the brace style. Braceless, the bodies after `try` and `finally` are a
   * group (`body.try`): both on the lines of their keywords or both below them. A `catch` with a single handler
   * expression instead of cases is kept as written.
   */
  def renderTry(tryTerm: Term.Try, indent: Int): Doc =
    val keepWritten = ctx.hasGapComments(tryTerm) || (tryTerm.catchClause.isEmpty && tryTerm.finallyp.isEmpty)

    tryTerm.catchClause match {
      case _ if keepWritten =>
        ctx.ownSourceDoc(tryTerm)
      case catchClause =>
        val inline = ctx.ownSourceDoc(tryTerm)
        val policy =
          ctx
            .config
            .body
            .tryBody

        val oneLine = !tryTerm.pos.text.contains('\n') && ctx.fits(inline, indent)
        val members = (tryTerm.expr :: tryTerm.finallyp.toList).map(body => tryMember(false, oneLine, body))

        if oneLine && !BodyLayout.groupBelow(policy, members) then
          inline
        else
          val bracedTry =
            ctx.tokensBetween(tryTerm.pos.start, ctx.span(tryTerm.expr)._1).exists(_.is[Token.LeftBrace])

          val bracedBody = tryTerm.expr.is[Term.Block] && ctx.writtenWithBraces(tryTerm.expr)

          val written = bracedTry || bracedBody
          val braces  = ctx.usesBraces(Construct.Try, written)
          val edges   = if braces then ctx.config.blankLines(Construct.Try) else BlankLinePolicy.Never

          val catches =
            catchClause.map(c => (c.pos.start, cases(c.pos.start, c.cases, c.pos.end, indent + 2, edges)))

          if braces then

            def braced(term: Term): Doc =
              Doc.braced(terms.bracedBody(term, indent + 2, Construct.Try))

            val tryBody = Doc.text("try ") ++ braced(tryTerm.expr)

            val catchDoc =
              catches.fold(Doc.Empty) { (_, caught) =>
                Doc.text(" catch ") ++ Doc.braced(caught)
              }

            val finallyDoc =
              tryTerm
                .finallyp
                .fold(Doc.Empty) { finallyTerm =>
                  Doc.text(" finally ") ++ braced(finallyTerm)
                }

            tryBody ++ catchDoc ++ finallyDoc
          else
            bracelessTry(tryTerm, indent, policy, catches.map(_._2))
    }

  // A body of a try as a member of its group: a body holding an if, a match or a try cannot be on the line of its
  // keyword.
  private def tryMember(below: Boolean, fits: Boolean, body: Term): BodyLayout.Member =
    BodyLayout.Member(below, fits && !BodyLayout.holdsControl(body), BodyLayout.isSimple(ctx, body))

  // A braceless try: its body and its `finally` body on the lines of their keywords or below them, by `policy`; its
  // cases below `catch`.
  private def bracelessTry(tryTerm: Term.Try, indent: Int, policy: MultiBodyPolicy, catches: Option[Doc]): Doc =
    val finallyEnd =
      tryTerm
        .finallyp
        .flatMap { finallyTerm =>
          ctx
            .tokensBetween(ctx.span(tryTerm.expr)._2, ctx.span(finallyTerm)._1)
            .collect { case kw: Token.KwFinally => kw.end }
            .lastOption
        }

    val bodies =
      (("try", tryTerm.pos.start + "try".length, tryTerm.expr) ::
      tryTerm.finallyp.zip(finallyEnd).map((finallyTerm, end) => ("finally", end, finallyTerm)).toList)
        .map { (keyword, keywordEnd, term) =>
          val doc   = terms.renderBody(term, indent + 2, forceExplode = false)
          val fits  = ctx.fits(doc, indent + keyword.length + 1)
          val below = BodyLayout.writtenBelow(ctx, keywordEnd, term)
          (keyword, doc, tryMember(below, fits, term))
        }

    val allBelow = BodyLayout.groupBelow(policy, bodies.map(_._3))

    def part(keyword: String, doc: Doc): Doc =
      if allBelow then Doc.text(keyword) ++ Doc.Nest(2, Doc.Line ++ doc) else Doc.text(keyword + " ") ++ doc

    val tryParts =
      bodies
        .headOption
        .map((keyword, doc, _) => part(keyword, doc))
        .toList

    val catchParts   = catches.map(caught => Doc.text("catch") ++ Doc.Nest(2, Doc.Line ++ caught)).toList
    val finallyParts = bodies.drop(1).map((keyword, doc, _) => part(keyword, doc))

    Doc.intercalate(Doc.Line, tryParts ++ catchParts ++ finallyParts)

  /**
   * The clauses found in `[start, end)` one per line at `indent`, keeping the blank lines and the comments between
   * them. The bodies are one group (`body.case-body`, see [[BodyLayout.groupBelow]]): all on the lines of their `=>`,
   * or all below them, never mixed. On their lines, the arrows of consecutive clauses align; when a clause would not
   * fit once aligned, all the bodies go below their `=>`.
   */
  def cases(start: Int, clauses: List[Case], end: Int, indent: Int, edges: BlankLinePolicy): Doc =
    val members = clauses.flatMap(member(_, indent))

    val docs =
      Option
        .when(!BodyLayout.groupBelow(ctx.config.body.caseBody, members))(aligned(clauses, indent))
        .flatten
        .getOrElse(clauses.map(caseClause(_, indent, None, true)))

    StatementComments.of(ctx, start, clauses, end).withEdgeBlanks(edges).lines(docs)

  // The clauses with their bodies on the lines of their `=>`, the arrows of consecutive clauses aligned; None when a
  // clause would not fit once aligned.
  private def aligned(clauses: List[Case], indent: Int): Option[List[Doc]] =
    val unpadded =
      clauses
        .map { oneCase =>
          oneCase.pos.start -> caseClause(oneCase, indent, None, false)
        }
        .toMap

    val targets =
      Alignment.groupedTargets(ctx, clauses, (_, _) => true)(
        eligible = { oneCase =>
          ctx.config.alignment.arrows.cases && unpadded.get(oneCase.pos.start).flatMap(Render.flatWidth).isDefined
        },
        width = headerWidth,
        fitsAt = (oneCase, target) => paddedWidth(unpadded, oneCase, target).exists(ctx.fitsWidth(indent, _)),
      )

    Option.when(!targets.contains(Some(Alignment.Placement.Broken))) {
      clauses
        .zip(targets)
        .map {
          case (oneCase, Some(Alignment.Placement.Padded(target))) =>
            caseClause(oneCase, indent, Some(target), false)
          case (oneCase, _) =>
            unpadded.getOrElse(oneCase.pos.start, ctx.sourceDoc(oneCase))
        }
    }

  // The body of a clause as a member of the group of the bodies of its case list: it cannot be on the line of its `=>`
  // when it does not fit there, a comment is between the parts of the clause, or it holds an if, a match or a try. None
  // for a clause with an empty body, or without an arrow.
  private def member(oneCase: Case, indent: Int): Option[BodyLayout.Member] =
    (oneCase.body, header(oneCase)) match {
      case (Term.Block(Nil), _) =>
        None
      case (_, Some((_, arrowEnd))) if ctx.hasGapComments(oneCase) =>
        Some(BodyLayout.Member(BodyLayout.writtenBelow(ctx, arrowEnd, oneCase.body), false, false))
      case (_, Some((pattern, arrowEnd))) =>
        val lead   = pattern ++ Doc.text(" =>")
        val body   = BodyLayout.of(ctx, terms, arrowEnd, oneCase.body, indent + 2, Some(Construct.CaseBody))
        val column = ctx.columnAfter(lead, indent) + 1
        val fits   = body.onSeparatorLine(ctx.fits(_, column)).isDefined && !BodyLayout.holdsControl(oneCase.body)
        Some(BodyLayout.Member(body.writtenBelow, fits, body.simple))
      case _ =>
        None
    }

  /**
   * A single clause at the given indent, its body one level in, placed by `body.case-body` as a group of its own.
   */
  def clause(oneCase: Case, indent: Int): Doc =
    caseClause(
      oneCase,
      indent,
      None,
      member(oneCase, indent).exists(m => BodyLayout.groupBelow(ctx.config.body.caseBody, List(m))),
    )

  // The width of a one-line clause once its header is padded to `target`.
  private def paddedWidth(unpadded: Map[Int, Doc], oneCase: Case, target: Int): Option[Int] =
    for
      line  <- unpadded.get(oneCase.pos.start).flatMap(Render.flatWidth)
      width <- headerWidth(oneCase)
    yield
      line + math.max(0, target - width)

  private def headerWidth(oneCase: Case): Option[Int] =
    header(oneCase).flatMap { case (doc, _) => Render.flatWidth(doc) }

  // A clause: its header as written, padded to `padTo` columns when aligned, and its body after `=>`, below it when
  // `below`, else on the `=>` line when it can be there. A clause with comments between its parts, or with an empty
  // body, is kept as written.
  private def caseClause(oneCase: Case, indent: Int, padTo: Option[Int], below: Boolean): Doc =
    (oneCase.body, header(oneCase)) match {
      case (Term.Block(Nil), _) =>
        ctx.sourceDoc(oneCase)
      case (_, Some((pattern, arrowEnd))) if !ctx.hasGapComments(oneCase) =>
        val pad    = Alignment.padding(padTo, Render.flatWidth(pattern))
        val lead   = pattern ++ pad ++ Doc.text(" =>")
        val body   = BodyLayout.of(ctx, terms, arrowEnd, oneCase.body, indent + 2, Some(Construct.CaseBody))
        val column = ctx.columnAfter(lead, indent) + 1
        if below then
          body.belowSeparator(lead)
        else
          body.placed(lead, ctx.fits(_, column), SingleBodyPolicy.AlwaysJoin)
      case _ =>
        ctx.sourceDoc(oneCase)
    }

  // The `case pattern if guard` part of a clause, before its arrow, and the end of the arrow.
  private def header(oneCase: Case): Option[(Doc, Int)] =
    ctx
      .arrowBefore(oneCase.pos.start, ctx.span(oneCase.body)._1)
      .map { arrow =>
        (ctx.trimmedSourceDoc(oneCase.pos.start, arrow.start), arrow.end)
      }

}
