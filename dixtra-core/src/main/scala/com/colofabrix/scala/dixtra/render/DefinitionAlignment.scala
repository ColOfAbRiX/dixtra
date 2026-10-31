package com.colofabrix.scala.dixtra.render

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.normalize.SourceEdits
import com.colofabrix.scala.dixtra.render.context.RenderContext
import com.colofabrix.scala.dixtra.render.definition.*
import scala.meta.*

/**
 * Renders the definitions among a run of statements (`val`, `lazy val`, `var`, `def`, `given` alias, `type` alias),
 * aligning the `=` of consecutive one-line definitions of the same alignment group (`alignment.assignment-groups`, see
 * [[Alignment]]). A definition that would not fit once aligned has its body moved below the `=`, and the definitions
 * before and after it align separately.
 *
 * @param ctx The shared render context.
 * @param assignments The renderer of `val`, `var` and `given` aliases.
 * @param defs The renderer of `def`s.
 */
final class DefinitionAlignment(ctx: RenderContext, assignments: AssignmentRenderer, defs: DefRenderer) {

  /**
   * The definitions among `stats` (None for other statements), with the `=` of consecutive one-line definitions of the
   * same alignment group aligned. Each definition is laid out once unpadded: a one-line definition still fits once
   * padded when its line plus the padding does, and one that does not breaks after its `=`.
   */
  def aligned(stats: List[Stat], indentOf: Stat => Int): List[Option[Doc]] =
    val alignment = ctx.config.alignment
    val unpadded  = stats.map(s => s.pos.start -> definitionDoc(s, indentOf(s), None, false)).toMap

    def lineWidth(stat: Stat): Option[Int] =
      unpadded
        .get(stat.pos.start)
        .flatten
        .map(Render.render(_, indentOf(stat)))
        .filter(_.linesIterator.length == 1)
        .map(_.length)

    def fitsAt(stat: Stat, target: Int): Boolean =
      (lineWidth(stat), headerWidth(stat)) match {
        case (Some(line), Some(width)) => ctx.fitsWidth(indentOf(stat), line + math.max(0, target - width))
        case _                         => false
      }

    val sameGroup =
      (a: Stat, b: Stat) => kindOf(a).zip(kindOf(b)).exists(alignment.sameGroup)

    def isEligible(stat: Stat): Boolean =
      val alignedKind = kindOf(stat).exists(alignment.groupOf(_).isDefined)
      val sized       = lineWidth(stat).isDefined
      alignedKind && sized && !documented(stat)

    val targets =
      Alignment.groupedTargets(ctx, stats, sameGroup)(
        eligible = isEligible,
        width = headerWidth,
        fitsAt = fitsAt,
      )

    val previous = None :: stats.zip(targets).map(Some(_))

    stats
      .zip(targets)
      .zip(previous)
      .map {
        case ((stat, Some(Alignment.Placement.Padded(target))), _) =>
          definitionDoc(stat, indentOf(stat), Some(target), false)
        case ((stat, Some(Alignment.Placement.Broken)), before) if separatesWhenBroken(stat) =>
          // Broken, it would get a blank line before it, which ends its group; with `always-join` its body would then
          // move up again
          val grouped =
            before.exists { (prev, target) =>
              val oneLineApart = ctx.slice(prev.pos.end, stat.pos.start).count(_ == '\n') == 1

              target.isDefined && oneLineApart
            }

          unpadded
            .get(stat.pos.start)
            .flatten
            .map { doc =>
              if grouped then Doc.Line ++ doc else doc
            }
        case ((stat, Some(Alignment.Placement.Broken)), _) =>
          definitionDoc(stat, indentOf(stat), None, true)
        case ((stat, None), _) =>
          unpadded.get(stat.pos.start).flatten
      }

  // A definition right after a scaladoc, which gets blank lines around it (see MethodSpacingNormalizer), so it never
  // aligns with its neighbours.
  private def documented(stat: Stat): Boolean =
    val after = ctx.previousSignificant(stat.pos.start).fold(0)(_.end)
    ctx
      .commentsBetween(after, stat.pos.start)
      .lastOption
      .exists(c => SourceEdits.isScaladoc(c.text))

  // A method that does not fit once aligned, with `body.def` / `body.given-alias` set to `always-join`: the blank line
  // that separates a method over several lines from the statements before it would end its group, so it stays on one
  // line after that blank line instead of breaking.
  private def separatesWhenBroken(stat: Stat): Boolean =
    kindOf(stat).exists { k =>
      val methodLike = k == DefinitionKind.Def || k == DefinitionKind.GivenAlias
      val alwaysJoin = ctx.config.body.afterEquals(k) == SingleBodyPolicy.AlwaysJoin
      methodLike && alwaysJoin
    }

  // A definition at the given indent, its `=` padded to `padTo` when aligned and its body below the `=` when `below`,
  // or None when it has no rule.
  private def definitionDoc(stat: Stat, indent: Int, padTo: Option[Int], below: Boolean): Option[Doc] =
    (stat, assignmentBody(stat)) match {
      case (_, Some(body))                  => assignments.render(stat, body, indent, padTo, below)
      case (defn: Defn.Def, _)              => defs.render(defn, indent, padTo, below)
      case (givenAlias: Defn.GivenAlias, _) => defs.renderGiven(givenAlias, indent, below)
      case (decl: Decl.Def, _)              => defs.renderDecl(decl, indent)
      case (alias: Defn.Type, _)            => typeAlias(alias, padTo, below)
      case _                                => None
    }

  // The width of a definition's text before its `=` on one line.
  private def headerWidth(stat: Stat): Option[Int] =
    (stat, assignmentBody(stat)) match {
      case (_, Some(body))       => assignments.headerWidth(stat, body)
      case (defn: Defn.Def, _)   => defs.headerWidth(defn)
      case (alias: Defn.Type, _) => typeAliasName(alias).flatMap(Render.flatWidth)
      case _                     => None
    }

  // The alignment keyword of a definition, if it can align its `=`.
  private def kindOf(stat: Stat): Option[DefinitionKind] =
    stat match {
      case valDef: Defn.Val if valDef.mods.exists(_.is[Mod.Lazy]) =>
        Some(DefinitionKind.LazyVal)
      case _: Defn.Val =>
        Some(DefinitionKind.Val)
      case _: Defn.Var =>
        Some(DefinitionKind.Var)
      case _: Defn.Def =>
        Some(DefinitionKind.Def)
      case givenAlias: Defn.GivenAlias if givenAlias.paramClauseGroups.isEmpty =>
        Some(DefinitionKind.GivenAlias)
      case _: Defn.Type =>
        Some(DefinitionKind.Type)
      case _ =>
        None
    }

  // The body after `=` of a definition rendered as an assignment: a `val`, a `var`, or a `given` alias without
  // parameters.
  private def assignmentBody(stat: Stat): Option[Term] =
    stat match {
      case valDef: Defn.Val =>
        Some(valDef.rhs)
      case varDef: Defn.Var =>
        Some(varDef.body)
      case givenAlias: Defn.GivenAlias if givenAlias.paramClauseGroups.isEmpty =>
        Some(givenAlias.body)
      case _ =>
        None
    }

  // A one-line type alias, its `=` padded to `padTo` when aligned and its body
  // below the `=` when `below` or with `body.type = always-break`.
  private def typeAlias(t: Defn.Type, padTo: Option[Int], below: Boolean): Option[Doc] =
    if ctx.slice(t.pos.start, t.pos.end).contains('\n') then
      None
    else
      typeAliasName(t).map { name =>
        val body = ctx.trimmedSourceDoc(t.body.pos.start, t.pos.end)
        val lead = name ++ Alignment.padding(padTo, Render.flatWidth(name)) ++ Doc.text(" =")
        if below || ctx.config.body.typeBody == SingleBodyPolicy.AlwaysBreak then
          lead ++ Doc.Nest(2, Doc.Line ++ body)
        else
          lead ++ Doc.text(" ") ++ body
      }

  // The text of a type alias before its `=`.
  private def typeAliasName(t: Defn.Type): Option[Doc] =
    ctx
      .equalsBefore(t.pos.start, t.body.pos.start)
      .map(eq => ctx.trimmedSourceDoc(t.pos.start, eq.start))

}
