package com.colofabrix.scala.dixtra.render.definition

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
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

  /**
   * Render a `def` with its signature padded to `padTo` columns when given and its body below the `=` when `below`, or
   * None to keep unsupported clause shapes as written.
   */
  def render(d: Defn.Def, baseIndent: Int, padTo: Option[Int], below: Boolean): Option[Doc] =
    d.paramClauseGroups match {
      case group :: Nil =>
        val signature = parts(d.pos.start, d.name, group, d.decltpe)
        renderWithClauses(signature, d.body, Construct.Def, ctx.config.body.defBody, baseIndent, padTo, below)
      case Nil =>
        assignments.render(d, d.body, baseIndent, padTo, below)
      case _ =>
        None
    }

  /**
   * Render a `given` alias with parameters, its body below the `=` when `below`, or None to keep it as written: an
   * anonymous one, one with several clause groups, or one in the syntax of Scala 3.6 (`given a: [A] => B = ...`),
   * whose parameters do not follow its name.
   */
  def renderGiven(g: Defn.GivenAlias, baseIndent: Int, below: Boolean): Option[Doc] =
    g.paramClauseGroups match {
      case group :: Nil if g.name.value.nonEmpty && ctx.slice(g.name.pos.end, group.pos.start).isBlank =>
        val signature = parts(g.pos.start, g.name, group, Some(g.decltpe))
        renderWithClauses(signature, g.body, Construct.GivenAlias, ctx.config.body.givenAlias, baseIndent, None, below)
      case _ =>
        None
    }

  /**
   * Render an abstract `def`: its signature, breaking as the signature of a `def` does. None to keep it as written: a
   * parameterless one, one with several clause groups, or one holding comments that cannot be re-attached.
   */
  def renderDecl(d: Decl.Def, baseIndent: Int): Option[Doc] =
    d.paramClauseGroups match {
      case group :: Nil =>
        val signature = parts(d.pos.start, d.name, group, Some(d.decltpe))
        if signatures.hasUnattachableComments(signature, d.pos.end) then None
        else signatures.render(signature, baseIndent, Doc.Empty).map(_.doc)
      case _ =>
        None
    }

  /**
   * The width of the signature before the `=`, when it is on one line.
   */
  def headerWidth(d: Defn.Def): Option[Int] =
    d.paramClauseGroups match {
      case group :: Nil => signatures.inlineWidth(parts(d.pos.start, d.name, group, d.decltpe))
      case Nil          => assignments.headerWidth(d, d.body)
      case _            => None
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
          .map { signature =>
            val lead   = signature.doc ++ pad ++ Doc.text(" =")
            val column = signature.lastLineWidth + Render.flatWidth(pad).getOrElse(0) + 3

            // Only a one-line signature takes the body up onto its line
            val placement =
              if policy == SingleBodyPolicy.AlwaysJoin && !Render.isFlat(signature.doc) then
                SingleBodyPolicy.Keep
              else
                policy

            if down then body.belowSeparator(lead) else body.placed(lead, ctx.fits(_, column), placement)
          }

      case _ =>
        None
    }

  private def parts(start: Int, name: Name, group: Member.ParamClauseGroup, returnType: Option[Type]): SignatureParts =
    SignatureParts(
      prefixStart = start,
      nameEnd = name.pos.end,
      tparamClause = group.tparamClause,
      paramClauses = group.paramClauses.collect { case c: Term.ParamClause => c },
      returnType = returnType,
      keepBroken = false,
    )

}
