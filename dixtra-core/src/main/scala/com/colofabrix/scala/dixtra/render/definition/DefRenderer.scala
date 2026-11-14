package com.colofabrix.scala.dixtra.render.definition

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.render.Alignment
import com.colofabrix.scala.dixtra.render.context.RenderContext
import com.colofabrix.scala.dixtra.render.term.*
import scala.meta.*

/**
 * Renders method definitions (`def`): a signature (see [[SignatureRenderer]]) followed by `=` and a body (see
 * [[com.colofabrix.scala.dixtra.render.term.BodyLayout]]).
 *
 * Of the Tier 1 break-points, the newline after `=` is taken first when that is stable: a body that does not fit after
 * `=` drops onto the next line with the signature kept on one line, as long as the body renders the same once below `=`
 * (where the newline counts as an author signal on the next run). Otherwise the whole `signature = body` line decides
 * whether the parameters explode, so the body can stay on the `): Type =` line. `body.def` places the body; with
 * `always-join` a body below the `=` moves up only when the whole definition fits on one line, and with `always-break`
 * the body is always below the `=`. The signature can be padded so that the `=` lines up with the definitions around
 * it.
 *
 * A signature holding comments that cannot be re-attached is kept as written, and a parameterless `def` is rendered
 * like a `val` by the assignment renderer. A `given` alias with parameters is laid out like a `def`, and an abstract
 * `def` is its signature alone.
 *
 * @param ctx The shared render context.
 * @param terms The term renderer used to lay out the body.
 * @param assignments The assignment renderer for parameterless definitions.
 * @param signatures The signature renderer.
 */
final class DefRenderer(
  ctx: RenderContext,
  terms: TermRenderer,
  assignments: AssignmentRenderer,
  signatures: SignatureRenderer,
) {

  private val arrowGivens = new ArrowGivenRenderer(ctx, terms)

  /**
   * Render a `def` with its signature padded to `padTo` columns when given and its body below the `=` when `below`, or
   * None to keep unsupported clause shapes as written.
   */
  def render(defn: Defn.Def, baseIndent: Int, padTo: Option[Int], below: Boolean): Option[Doc] =
    defn.paramClauseGroups match {
      case Nil =>
        assignments.render(defn, defn.body, baseIndent, padTo, below)
      case groups =>
        val signature = parts(defn.pos.start, defn.name, groups, defn.decltpe)
        renderWithClauses(signature, defn.body, Construct.Def, ctx.config.body.defBody, baseIndent, padTo, below)
    }

  /**
   * Render a `given` alias with parameters, named or anonymous, its body below the `=` when `below`. One in the syntax
   * of Scala 3.6 (`given a: [A] => B = ...`) goes to [[ArrowGivenRenderer]].
   */
  def renderGiven(givenAlias: Defn.GivenAlias, baseIndent: Int, below: Boolean): Option[Doc] =
    def blankBefore(group: Tree): Boolean =
      ctx.slice(givenAlias.name.pos.end, group.pos.start).isBlank

    givenAlias.paramClauseGroups match {
      case _ :: _ if arrowGivens.isArrowSyntax(givenAlias) =>
        arrowGivens.render(givenAlias, baseIndent, below)
      case groups @ (first :: _) if blankBefore(first) =>
        val signature = givenParts(givenAlias, groups)
        renderWithClauses(
          signature,
          givenAlias.body,
          Construct.GivenAlias,
          ctx.config.body.givenAlias,
          baseIndent,
          None,
          below,
        )
      case _ =>
        None
    }

  /**
   * Render an abstract `def`: its signature, breaking as the signature of a `def` does. None to keep it as written: a
   * parameterless one, or one holding comments that cannot be re-attached.
   */
  def renderDecl(defn: Decl.Def, baseIndent: Int): Option[Doc] =
    defn.paramClauseGroups match {
      case Nil =>
        None
      case groups =>
        val signature = parts(defn.pos.start, defn.name, groups, Some(defn.decltpe))
        if signatures.hasUnattachableComments(signature, defn.pos.end) then
          None
        else
          signatures.render(signature, baseIndent, Doc.Empty).map(_.doc)
    }

  /**
   * Render a secondary constructor (`def this(...) = this(...)`): its signature breaks as the signature of a `def`. A
   * body that is the call alone goes on the `=` line when it fits there; a body of several statements goes below the
   * `=`, one statement per line, or stays as written on one line by `body.semicolon-statements`. None to keep it as
   * written: a statement that is not a term, or comments that cannot be re-attached.
   */
  def renderSecondary(ctor: Ctor.Secondary, baseIndent: Int): Option[Doc] =
    for
      nameEnd  <- thisKeywordEnd(ctor)
      eq       <- ctx.equalsBefore(nameEnd, ctor.body.init.pos.start)
      signature = secondaryParts(ctor, nameEnd)
                  if !signatures.hasUnattachableComments(signature, eq.start)
                  if ctx.commentsBetween(eq.end, ctor.pos.end).isEmpty
      body     <- secondaryBody(ctor, eq.end, baseIndent + 2)
      lineRest  = if Render.isFlat(body) then Doc.text(" = ") ++ body else Doc.text(" =")
      laidOut  <- signatures.render(signature, baseIndent, lineRest)
    yield
      placeBody(laidOut, body)

  /**
   * The width of the signature before the `=`, when it is on one line.
   */
  def headerWidth(defn: Defn.Def): Option[Int] =
    defn.paramClauseGroups match {
      case Nil    => assignments.headerWidth(defn, defn.body)
      case groups => signatures.inlineWidth(parts(defn.pos.start, defn.name, groups, defn.decltpe))
    }

  private def renderWithClauses(
    signature: SignatureParts,
    definitionBody: Term,
    construct: Construct,
    policy: SingleBodyPolicy,
    baseIndent: Int,
    padTo: Option[Int],
    below: Boolean,
  ): Option[Doc] =
    ctx.equalsBefore(signature.nameEnd, ctx.span(definitionBody)._1) match {
      case Some(eq) if !signatures.hasUnattachableComments(signature, eq.start) =>
        val body = BodyLayout.of(ctx, terms, eq.end, definitionBody, baseIndent + 2, Some(construct))
        val pad  = Alignment.padding(padTo, signatures.inlineWidth(signature))
        val down = below || policy == SingleBodyPolicy.AlwaysBreak

        // What stays on the signature line decides the Tier 1 trigger: only a body that would change by moving below
        // `=` must fit there.
        val canJoin = !down && Render.isFlat(body.onLine) && !body.hasComments && !body.stableBelow

        val lineRest =
          if canJoin then pad ++ Doc.text(" = ") ++ body.onLine
          else pad ++ Doc.text(" =")

        signatures
          .render(signature, baseIndent, lineRest)
          .map { laidOut =>
            val lead   = laidOut.doc ++ pad ++ Doc.text(" =")
            val column = laidOut.lastLineWidth + Render.flatWidth(pad).getOrElse(0) + 3

            // Only a one-line signature takes the body up onto its line
            val placement =
              if policy == SingleBodyPolicy.AlwaysJoin && !Render.isFlat(laidOut.doc) then
                SingleBodyPolicy.Keep
              else
                policy

            if down then
              body.belowSeparator(lead)
            else
              body.placed(lead, ctx.fits(_, column), placement)
          }

      case _ =>
        None
    }

  // The signature of a `given` alias with parameters. An anonymous one has no name: its signature keeps the `given`
  // keyword, and one space before its parameters.
  private def givenParts(givenAlias: Defn.GivenAlias, groups: List[Member.ParamClauseGroup]): SignatureParts =
    val keywordEnd =
      ctx
        .tokensBetween(givenAlias.pos.start, givenAlias.name.pos.end + 1)
        .collectFirst { case t: scala.meta.tokens.Token.KwGiven => t.end }

    val anonymous =
      givenAlias
        .name
        .value
        .isEmpty

    val nameEnd =
      if anonymous then
        keywordEnd.getOrElse(givenAlias.name.pos.end)
      else
        givenAlias
          .name
          .pos
          .end

    val afterName = if anonymous then " " else ""

    parts(givenAlias.pos.start, nameEnd, afterName, groups, Some(givenAlias.decltpe))

  // The end of the `this` keyword of a secondary constructor, which ends the part of its signature kept as written.
  private def thisKeywordEnd(ctor: Ctor.Secondary): Option[Int] =
    ctx
      .tokensBetween(ctor.pos.start, ctor.pos.end)
      .collectFirst { case t: scala.meta.tokens.Token.KwThis => t.end }

  // The body of a secondary constructor at `indent`: its call alone, the body as written on one line (with
  // `body.semicolon-statements = keep`), or the call and the statements in braces or one per line, following
  // `braces.def` as the body of a `def` does. None when a statement is not a term.
  private def secondaryBody(ctor: Ctor.Secondary, equalsEnd: Int, indent: Int): Option[Doc] =
    val call          = terms.renderInit(ctor.body.init, indent)
    val stats         = ctor.body.stats
    val statements    = stats.collect { case term: Term => term }
    val allTerms      = statements.sizeIs == stats.size
    val writtenBraced = ctx.slice(equalsEnd, ctor.pos.end).trim.startsWith("{")
    val braced        = ctx.braceStyle(Construct.Def, writtenBraced, ended = false) == BracePolicy.ForceBraces

    if stats.isEmpty then
      Some(call)
    else if SemicolonStatements.keptOnLine(ctx, ctor) then
      Some(Doc.text(ctx.slice(equalsEnd, ctor.pos.end).trim))
    else
      Option.when(allTerms) {
        val docs = Doc.intercalate(Doc.Line, call :: statements.map(terms.render(_, indent, forceExplode = false)))
        if braced then Doc.braced(docs) else docs
      }

  private def secondaryParts(ctor: Ctor.Secondary, nameEnd: Int): SignatureParts =
    SignatureParts(
      prefixStart = ctor.pos.start,
      nameEnd = nameEnd,
      afterName = "",
      tparamClause = Type.ParamClause(Nil),
      constructorModifiers = Nil,
      paramClauses = ctor.paramClauses.toList,
      clauseOpenings = Nil,
      returnType = None,
      keepBroken = false,
    )

  // The body on the `=` line of the signature when it is on one line and fits there, below the `=` otherwise.
  private def placeBody(laidOut: Signature, body: Doc): Doc =
    val column = laidOut.lastLineWidth + 3
    val down   = ctx.config.body.defBody == SingleBodyPolicy.AlwaysBreak
    val onLine = Render.isFlat(body) && ctx.fits(body, column)
    val equals = laidOut.doc ++ Doc.text(" =")

    if onLine && !down then
      equals ++ Doc.text(" ") ++ body
    else
      equals ++ Doc.Nest(2, Doc.Line ++ body)

  // The signature of clause groups: the type clause of the first group follows the name, and the type clause of each
  // later group opens its first value clause (`)[B](`).
  private def parts(
    start: Int,
    name: Name,
    groups: List[Member.ParamClauseGroup],
    returnType: Option[Type],
  ): SignatureParts =
    parts(start, name.pos.end, "", groups, returnType)

  private def parts(
    start: Int,
    nameEnd: Int,
    afterName: String,
    groups: List[Member.ParamClauseGroup],
    returnType: Option[Type],
  ): SignatureParts =
    val valueClauses = groups.map(_.paramClauses.collect { case c: Term.ParamClause => c })
    val laterTypes   = None :: groups.drop(1).map(g => Option.when(g.tparamClause.values.nonEmpty)(g.tparamClause))

    val openings =
      valueClauses
        .zip(laterTypes)
        .flatMap { (clauses, opening) =>
          clauses
            .indices
            .toList
            .map(i => if i == 0 then opening else None)
        }

    SignatureParts(
      prefixStart = start,
      nameEnd = nameEnd,
      afterName = afterName,
      tparamClause = groups.headOption.fold(Type.ParamClause(Nil))(_.tparamClause),
      constructorModifiers = Nil,
      paramClauses = valueClauses.flatten,
      clauseOpenings = openings,
      returnType = returnType,
      keepBroken = false,
    )

}
