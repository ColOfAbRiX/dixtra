package com.colofabrix.scala.dixtra.render.definition

import com.colofabrix.scala.dixtra.TrailingCommas
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.render.*
import com.colofabrix.scala.dixtra.render.ClauseGroups.Group
import com.colofabrix.scala.dixtra.render.comments.CommentedList
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*

/**
 * Lays out a signature: modifiers, name, type parameters, value parameter clauses and return type, up to (not
 * including) the `=` of a `def`, or the header of a class or trait up to its constructor's last parameter clause.
 *
 * A signature stays inline unless its line overflows, together with whatever the caller keeps on that line after it.
 * When it breaks, value parameters explode one per line (Tier 1); if even the type-parameter header overflows, type
 * parameters also break and the closing `](` glues to the first value parameter (Tier 2). A type parameter's bounds and
 * context bounds never break; a higher-kinded one whose line does not fit breaks its own type parameters, recursively.
 * One written over several lines is joined. Across multiple clauses the `)(` join stays glued so no lonely delimiter
 * line appears, at the start of the next clause or after the last parameter of the clause before
 * (`definition-site.groups-separator`). A `using` / `implicit` clause keeps its keyword on the clause's opening line.
 * Empty parameter clauses `()` never explode.
 *
 * Comments between parameters (or type parameters) stay attached to their parameter, and a line comment or an own-line
 * comment forces the signature to explode.
 *
 * @param ctx The shared render context.
 */
private[render] final class SignatureRenderer(ctx: RenderContext) {

  /**
   * True when the signature holds comments the layout cannot re-attach: between the name and `end`, outside every
   * parameter clause and the return type, or inside an empty clause.
   */
  def hasUnattachableComments(parts: SignatureParts, end: Int): Boolean =
    val clauses = parts.tparamClause :: parts.paramClauses
    val spans   = clauses.map(c => (c.pos.start, c.pos.end)) ++ parts.returnType.toList.map(ctx.span)
    val outside =
      ctx
        .commentsBetween(parts.nameEnd, end)
        .exists { comment =>
          val insideAnySpan = spans.exists { case (from, to) => comment.start >= from && comment.end <= to }
          !insideAnySpan
        }
    val inEmptyClause =
      clauses.exists { clause =>
        clause.values.isEmpty && ctx.commentsBetween(clause.pos.start, clause.pos.end).nonEmpty
      }
    outside || inEmptyClause

  /**
   * The width of the signature laid out on one line, if it can be.
   */
  def inlineWidth(parts: SignatureParts): Option[Int] =
    val layout = new Layout(parts, 0)
    Render.flatWidth(layout.prefix ++ layout.restInline)

  /**
   * The signature, deciding its breaks so that the signature line, followed by
   * `lineRest` (what the caller keeps on it, e.g. ` =`, ` = body` or
   * ` extends A {`), fits. None when the signature cannot be laid out and must be kept verbatim.
   */
  def render(parts: SignatureParts, baseIndent: Int, lineRest: Doc): Option[Signature] =
    val layout = new Layout(parts, baseIndent)
    import layout.*
    val prefixEnd = ctx.columnAfterSlice(prefix, parts.prefixStart, parts.nameEnd, baseIndent)

    // A line comment or an own-line comment between parameters forces the break.
    val commentBreak = tcomments.forcesBreak || clauses.exists(_.comments.forcesBreak)
    val canExplode   = tparams.nonEmpty || clauses.exists(_.elements.nonEmpty)
    val restFits     = ctx.fits(restInline ++ lineRest, prefixEnd)
    val overflow     = canExplode && (commentBreak || parts.keepBroken || !restFits)
    val breakTparams =
      tparams.nonEmpty && (tcomments.forcesBreak || !ctx.fits(tparamsInline ++ Doc.text("("), prefixEnd))

    if !overflow then
      Some(Signature(prefix ++ restInline, ctx.columnAfter(restInline, prefixEnd)))
    else
      clauses match {
        case single :: Nil if breakTparams =>
          Some(tier2(prefix, tparams, tcomments, single, returnPart, baseIndent))
        case _ if tcomments.forcesBreak =>
          None // type parameters that must break need Tier 2, which needs a single clause
        case _ =>
          Some(explodedClauses(prefix ++ tparamsInline, clauses, returnPart, baseIndent))
      }

 // The pieces of a signature as documents, before deciding its breaks.
  private final class Layout(signature: SignatureParts, baseIndent: Int) {
    val prefix: Doc    = ctx.sourceDoc(signature.prefixStart, signature.nameEnd) // modifiers + keyword + name
    val tclause   = signature.tparamClause
    val tparams: List[Doc]   = tclause.values.map(typeParam(_, baseIndent + 2)) // one per line (Tier 2)
    val tcomments: CommentedList = CommentedList.of(ctx, tclause.pos.start, tclause.values, tclause.pos.end)
    val clauses: List[Group]   = signature.paramClauses.map(toClause)
    val returnPart: Doc =
      signature
        .returnType
        .map(t => Doc.text(": ") ++ ctx.sourceDoc(t))
        .getOrElse(Doc.Empty)

    val tparamsInline: Doc =
      if tparams.isEmpty then Doc.Empty
      else Doc.text("[") ++ tcomments.inline(tclause.values.map(oneLine)) ++ Doc.text("]")
    val restInline: Doc = tparamsInline ++ Doc.concat(clauses.map(_.inline)) ++ returnPart
  }

  // A type parameter on its own line at `indent`. Its bounds and context bounds never break; a higher-kinded one whose
  // line does not fit breaks its own type parameters one per line, by the same rule. One with comments is kept as
  // written.
  private def typeParam(tparam: Type.Param, indent: Int): Doc =
    val (start, end) = ctx.span(tparam)
    val inner        = tparam.tparamClause
    val fitsOneLine  = ctx.fits(oneLine(tparam) ++ Doc.text(","), indent)

    if ctx.commentsBetween(start, end).nonEmpty then
      ctx.sourceDoc(tparam)
    else if inner.values.isEmpty || fitsOneLine then
      oneLine(tparam)
    else
      val comma = ctx.config.trailingCommas == TrailingCommas.Always
      val last  = inner.values.length - 1

      val params =
        inner
          .values
          .zipWithIndex
          .map { (param, index) =>
            typeParam(param, indent + 2) ++ (if index < last || comma then Doc.text(",") else Doc.Empty)
          }

      val opening  = oneLine(start, inner.pos.start) ++ Doc.text("[")
      val indented = Doc.Nest(2, Doc.Line ++ Doc.intercalate(Doc.Line, params))
      val closing  = Doc.Line ++ Doc.text("]") ++ oneLine(inner.pos.end, end)

      opening ++ indented ++ closing

  private def oneLine(tparam: Type.Param): Doc =
    val (start, end) = ctx.span(tparam)
    if ctx.commentsBetween(start, end).nonEmpty then ctx.sourceDoc(tparam)
    else oneLine(start, end)

  // The source in `[start, end)` on one line: line breaks become a space, or nothing next to a bracket (dropping a
  // trailing comma before `]` or `)`).
  private def oneLine(start: Int, end: Int): Doc =
    val text = ctx.slice(start, end)
    if !text.contains('\n') then
      ctx.sourceDoc(start, end)
    else
      Doc.text(
        text
          .replaceAll(",\\s*\\n\\s*(?=[\\])])", "")
          .replaceAll("(?<=[\\[(])\\s*\\n\\s*|\\s*\\n\\s*(?=[\\])])", "")
          .replaceAll("\\s*\\n\\s*", " "),
      )

  private def toClause(clause: Term.ParamClause): Group =
    Group(
      Syntax.clauseKeyword(clause.mod),
      clause.values.map(ctx.sourceDoc),
      CommentedList.of(ctx, clause.pos.start, clause.values, clause.pos.end),
    )

  // Tier 1 + multi-clause: type params stay inline; value clauses explode one param per line, glued by
  // `definition-site.groups-separator` (see [[ClauseGroups]]).
  private def explodedClauses(lead: Doc, clauses: List[Group], trailer: Doc, baseIndent: Int): Signature =
    val trailerWidth = Render.flatWidth(trailer).getOrElse(0)
    val layout       = ClauseGroups.broken(lead, clauses, ctx.config.definitionSite.groupsSeparator, baseIndent)
    if layout.open then Signature(layout.doc ++ Doc.Line ++ Doc.text(")") ++ trailer, baseIndent + 1 + trailerWidth)
    else Signature(layout.doc ++ trailer, layout.lastWidth + trailerWidth)

  // Tier 2 (delimiter gluing): type params one per line, then `](firstParam` glued so the closing `]` line never stands
  // alone.
  private def tier2(
    prefix: Doc,
    tparams: List[Doc],
    tcomments: CommentedList,
    clause: Group,
    trailer: Doc,
    baseIndent: Int,
  ): Signature =
    val header       = prefix ++ Doc.text("[") ++ Doc.Nest(2, Doc.Line ++ tcomments.exploded(tparams)) ++ Doc.Line
    val trailerWidth = Render.flatWidth(trailer).getOrElse(0)
    clause.elements match {
      case Nil =>
        Signature(header ++ Doc.text(s"](${clause.keyword.trim})") ++ trailer, baseIndent + 3 + trailerWidth)
      case _ =>
        val params =
          if clause.keyword.nonEmpty then ClauseGroups.openContent(clause, false)
          else ClauseGroups.gluedElements(clause, false)
        Signature(
          header ++ Doc.text("](") ++ params ++ Doc.Line ++ Doc.text(")") ++ trailer,
          baseIndent + 1 + trailerWidth,
        )
    }

}

/**
 * The parts of a signature, from the source.
 *
 * @param prefixStart Where the signature starts (its modifiers).
 * @param nameEnd Where the name ends: the prefix is kept as written up to there.
 * @param tparamClause The type parameters.
 * @param paramClauses The value parameter clauses.
 * @param returnType The declared return type, if any.
 * @param keepBroken True when the parameters break even if they fit, because the author wrote them over several lines
 *                   (case classes).
 */
private[render] final case class SignatureParts(
  prefixStart: Int,
  nameEnd: Int,
  tparamClause: Type.ParamClause,
  paramClauses: List[Term.ParamClause],
  returnType: Option[Type],
  keepBroken: Boolean,
)

/**
 * A laid-out signature.
 *
 * @param doc The signature document.
 * @param lastLineWidth The column reached at the end of its last line.
 */
private[render] final case class Signature(doc: Doc, lastLineWidth: Int)
