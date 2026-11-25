package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.render.*
import com.colofabrix.scala.dixtra.render.comments.CommentedList
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*

/**
 * Renders function applications and collection literals (`f(a, b)`, `List(a, b)`), their argument clauses, tuples (`(a,
 * b)`, laid out as an argument clause), and terms in operand position.
 *
 * Arguments break (one per line) only on an author break inside the call or on overflow: a break after the `=` of the
 * enclosing definition does not reach them. The parenthesised argument groups of a call (`f(a)(b)`) break together (see
 * `argumentClauses`). A trailing `{ ... }` brace block (lambda, multi-statement block, or `{ case ... }`) always keeps
 * its braces and applies the same rules to its contents (D5); a `using` clause keeps its keyword.
 *
 * @param ctx The shared render context.
 * @param terms The term renderer, for nested terms.
 * @param blocks The block renderer, for multi-statement brace blocks.
 * @param caseClauses The case clause renderer, for `{ case ... }` blocks.
 */
private[term] final class ApplicationRenderer(
  ctx: RenderContext,
  terms: TermRenderer,
  blocks: BlockRenderer,
  caseClauses: CaseClauseRenderer,
) {

  private val argumentBlocks = new ArgumentBlocks(ctx, terms, blocks, caseClauses)

  private val argumentAlignment = new ArgumentAlignment(ctx, terms, operand)

  /**
   * Render `fun` applied to `argClause`, starting at `indent`.
   */
  def render(fun: Term, argClause: Term.ArgClause, indent: Int): Doc =
    val (base, clauses) = applied(fun, List(argClause))
    val funDoc          = operand(base, indent)
    funDoc ++ argumentClauses(clauses, indent, ctx.columnAfter(funDoc, indent))

  // The function of a call and its argument clauses in order, through the calls nested in its function (`f(a)(b)`).
  @annotation.tailrec
  private def applied(fun: Term, clauses: List[Term.ArgClause]): (Term, List[Term.ArgClause]) =
    fun match {
      case app @ Term.Apply.After_4_6_0(inner, clause) if !ctx.isParenthesized(app) && !ctx.hasGapComments(app) =>
        applied(inner, clause :: clauses)
      case _ =>
        (fun, clauses)
    }

  /**
   * The argument clauses of a call in order, starting at `column` on a line indented at `indent`. Two or more
   * parenthesised clauses in a row, the argument groups, break together: when they cannot stay on one line, every group
   * breaks with each argument on its own line, glued by `call-site.groups-separator` (see [[ClauseGroups]]), and the
   * closing `)` goes on a line of its own at `indent`. With `call-site.groups-break = last-first` only the
   * last group breaks while the groups before it stay on the line of the call; with `independent` each group decides by
   * itself. A trailing brace block keeps its own rules.
   */
  def argumentClauses(clauses: List[Term.ArgClause], indent: Int, column: Int): Doc =
    val (groups, rest) = clauses.span(isGroup)
    if groups.sizeIs < 2 then
      following(clauses, Doc.Empty, indent, column)
    else
      grouped(groups, indent, column) match {
        case Some(doc) => following(rest, doc, indent, column)
        case None      => following(rest, eachGroup(groups, indent, column), indent, column)
      }

  // `clauses` after `before`, each deciding its layout from the column where it starts.
  private def following(clauses: List[Term.ArgClause], before: Doc, indent: Int, column: Int): Doc =
    clauses.foldLeft(before) { (acc, clause) =>
      acc ++ arguments(clause, indent, ctx.endColumn(acc, indent, column))
    }

  // A parenthesised argument clause, as opposed to a trailing brace block.
  private def isGroup(clause: Term.ArgClause): Boolean =
    clause.values match {
      case (block: Term.Block) :: Nil if !ctx.isParenthesized(block)     => false
      case (pf: Term.PartialFunction) :: Nil if !ctx.isParenthesized(pf) => false
      case _                                                             => true
    }

  // Whether a group breaks its line: a comment forces a break, or its clause was written over several lines.
  private def breaksLayout(clause: Term.ArgClause, comments: CommentedList): Boolean =
    comments.forcesBreak || clause.pos.text.contains('\n')

  // Whether the groups before the last stay on the line of the call: written on one line, without a comment that forces
  // a break, and fitting with the `(` of the last group.
  private def leadingStay(leading: List[(Term.ArgClause, List[Term], CommentedList)], column: Int): Boolean =
    val onLine =
      Doc.concat(
        leading.map { (clause, args, comments) =>
          ClauseGroups
            .Group(
              "",
              Syntax.clauseKeyword(clause.mod),
              args.map(arg => ctx.sourceDoc(arg)),
              comments,
            )
            .inline
        },
      )

    val writtenPlain  = !leading.exists((clause, _, comments) => breaksLayout(clause, comments))
    val fitsWithParen = ctx.fits(onLine ++ Doc.text("("), column)

    writtenPlain && fitsWithParen

  // The argument groups on one line, or broken together; None when each group decides its own layout: a group must be
  // kept as written (an empty group holding a comment), or `call-site.groups-break` lets each group decide or only the
  // last group break.
  private def grouped(clauses: List[Term.ArgClause], indent: Int, column: Int): Option[Doc] =
    val commas =
      ctx
        .config
        .callSite
        .trailingCommas

    val groups =
      clauses.map { clause =>
        val args = clause.values
        (clause, args, CommentedList.of(ctx, clause.pos.start, args, clause.pos.end, commas))
      }

    val inline =
      Doc.concat(
        groups.map { (clause, args, comments) =>
          ClauseGroups.Group("", Syntax.clauseKeyword(clause.mod), args.map(arg => ctx.sourceDoc(arg)), comments).inline
        },
      )

    val forcedBreak    = groups.exists((clause, _, comments) => breaksLayout(clause, comments))
    val doesNotFit     = !ctx.fits(inline, column)
    val breaks         = forcedBreak || doesNotFit
    val emptyCommented = groups.exists((_, args, comments) => args.isEmpty && !comments.isEmpty)

    if emptyCommented then
      None
    else if !breaks || groups.forall(_._2.isEmpty) then
      Some(inline)
    else if ctx.config.callSite.groupsBreak == GroupsBreak.Independent then
      None
    else if ctx.config.callSite.groupsBreak == GroupsBreak.LastFirst && leadingStay(groups.dropRight(1), column) then
      None
    else
      val broken =
        groups.map { (clause, args, comments) =>
          val keyword = Syntax.clauseKeyword(clause.mod)
          ClauseGroups.Group("", keyword, argumentAlignment.aligned(args, indent + 2), comments)
        }

      val separator =
        ctx
          .config
          .callSite
          .groupsSeparator

      val layout = ClauseGroups.broken(Doc.Empty, broken, separator, indent, keywordAsParameter = false)
      Some(if layout.open then layout.doc ++ Doc.Line ++ Doc.text(")") else layout.doc)

  /**
   * The argument groups of a call, each with the layout it decides for itself: a group stays on one line when it fits
   * there and the author wrote it on one line, and breaks otherwise. The `)(` between the groups is glued by
   * [[ClauseGroups]], so no lonely delimiter line appears.
   */
  private def eachGroup(clauses: List[Term.ArgClause], indent: Int, column: Int): Doc =
    val separator =
      ctx
        .config
        .callSite
        .groupsSeparator

    // The layout of each group, decided from the column where it starts: the column depends on the groups before it, so
    // they are laid out one after the other.
    val arrangements =
      clauses
        .foldLeft((Doc.Empty, List.empty[ClauseGroups.Arrangement])) { case ((prefix, arranged), clause) =>
          val startColumn            = ctx.endColumn(prefix, indent, column)
          val (example, arrangement) = arrangementOf(clause, indent, startColumn)

          (prefix ++ example, arranged :+ arrangement)
        }
        ._2

    val arrangedDoc = ClauseGroups.arranged(Doc.Empty, arrangements, separator, indent)
    arrangedDoc

  /**
   * The document of an argument group and the layout it takes: on one line, broken, or as the author wrote it.
   */
  private def arrangementOf(clause: Term.ArgClause, indent: Int, column: Int): (Doc, ClauseGroups.Arrangement) =
    val args = clause.values

    args match {
      case (_: Term.Block | _: Term.PartialFunction) :: Nil if ctx.hasGapComments(clause) =>
        val source = ctx.ownSourceDoc(clause)
        (source, ClauseGroups.AsWritten(source))

      case _ =>
        val comments = CommentedList.of(ctx, clause.pos.start, args, clause.pos.end, ctx.config.callSite.trailingCommas)
        val keyword  = Syntax.clauseKeyword(clause.mod)
        val elements = argumentAlignment.aligned(args, indent + 2)
        val group    = ClauseGroups.Group("", keyword, elements, comments)

        val inline =
          Doc.text(s"($keyword") ++ comments.inline(args.map(arg => ctx.sourceDoc(arg))) ++ Doc.text(")")

        val authorSignal =
          clause
            .pos
            .text
            .contains('\n')
        val fitsInline = ctx.fits(inline, column)

        if args.isEmpty && comments.isEmpty then
          (Doc.text(s"(${keyword.trim})"), ClauseGroups.OnOneLine(Doc.text(s"(${keyword.trim})")))
        else if args.isEmpty then
          val source = ctx.ownSourceDoc(clause)
          (source, ClauseGroups.AsWritten(source))
        else if !comments.forcesBreak && !authorSignal && fitsInline then
          (inline, ClauseGroups.OnOneLine(inline))
        else
          val layout  = ClauseGroups.broken(Doc.Empty, List(group), ctx.config.callSite.groupsSeparator, indent, false)
          val example = if layout.open then layout.doc ++ Doc.Line ++ Doc.text(")") else layout.doc
          (example, ClauseGroups.Broken(group, keywordAsParameter = false))
    }

  /**
   * A term used as an operand (chain receiver, applied function, argument, side of an infix operator): a block or a
   * `{ case ... }` keeps its braces, because significant indentation is not valid there.
   */
  def operand(term: Term, indent: Int): Doc =
    term match {
      case block: Term.Block if !ctx.isParenthesized(block)     => argumentBlocks.braceBlock(block, indent, indent)
      case pf: Term.PartialFunction if !ctx.isParenthesized(pf) => argumentBlocks.braceBlock(pf, indent, indent)
      case _                                                    => terms.render(term, indent, forceExplode = false)
    }

  /**
   * A `{ ... }` block or `{ case ... }` that keeps its braces, starting at `column` on a line indented at `indent`;
   * None for any other term.
   */
  def braced(term: Term, indent: Int, column: Int): Option[Doc] =
    term match {
      case (_: Term.Block | _: Term.PartialFunction) if !ctx.isParenthesized(term) =>
        Some(argumentBlocks.braceBlock(term, indent, column))
      case _ =>
        None
    }

  /**
   * An argument clause: a trailing brace block or a parenthesised clause. `indent` is the line indent (exploded content
   * nests under it); `column` is where the clause starts on its line, used for the fits check.
   */
  def arguments(clause: Term.ArgClause, indent: Int, column: Int): Doc =
    clause.values match {
      case (_: Term.Block | _: Term.PartialFunction) :: Nil if ctx.hasGapComments(clause) =>
        ctx.ownSourceDoc(clause)
      case (block: Term.Block) :: Nil if !ctx.isParenthesized(block) =>
        val withBraces = argumentBlocks.braceBlock(block, indent, column + 1)
        argumentBlocks
          .colonLambda(block, indent)
          .orElse(argumentBlocks.colonBlock(block, indent))
          .filter(_ => !Render.isFlat(withBraces) && !continuesAfter(clause))
          .getOrElse(Doc.text(" ") ++ withBraces)
      case (pf: Term.PartialFunction) :: Nil if !ctx.isParenthesized(pf) =>
        Doc.text(" ") ++ argumentBlocks.braceBlock(pf, indent, column + 1)
      case args =>
        parenArgs(clause, args, indent, column)
    }

  /**
   * A tuple starting at `indent`, laid out as an argument clause: inline unless a trigger fires, else one element per
   * line.
   */
  def tuple(t: Term.Tuple, indent: Int): Doc =
    parenthesised(t, "", t.args, indent, indent)

  // Whether the application of `clause` goes on after it (`xs.map { ... }.sum`, `f { ... }(a)`, `g { ... } + 1`): a
  // braceless argument would take what follows into its body, so it keeps its braces.
  private def continuesAfter(clause: Term.ArgClause): Boolean =
    clause
      .parent
      .exists { application =>
        application
          .parent
          .exists {
            case select: Term.Select       => select.qual eq application
            case apply: Term.Apply         => apply.fun eq application
            case applyType: Term.ApplyType => applyType.fun eq application
            case infix: Term.ApplyInfix    => infix.lhs eq application
            case _                         => false
          }
      }

  // A parenthesised argument clause: inline unless a trigger fires, else one argument per line. A `using` clause keeps
  // its keyword on the opening line. Comments between arguments stay attached to their argument; a line comment or a
  // comment on its own line forces the clause to explode.
  private def parenArgs(clause: Term.ArgClause, args: List[Term], indent: Int, column: Int): Doc =
    parenthesised(clause, Syntax.clauseKeyword(clause.mod), args, indent, column)

  // The elements `args` of `tree`, between its parentheses, after `keyword`: an argument clause or a tuple.
  private def parenthesised(tree: Tree, keyword: String, args: List[Term], indent: Int, column: Int): Doc =
    val comments = CommentedList.of(ctx, tree.pos.start, args, tree.pos.end, ctx.config.callSite.trailingCommas)
    val inline   = Doc.text(s"($keyword") ++ comments.inline(args.map(a => ctx.sourceDoc(a))) ++ Doc.text(")")

    val authorSignal =
      tree
        .pos
        .text
        .contains('\n')

    val fitsInline = ctx.fits(inline, column)

    if args.isEmpty && comments.isEmpty then
      Doc.text(s"(${keyword.trim})")
    else if args.isEmpty then
      ctx.ownSourceDoc(tree)
    else if !comments.forcesBreak && !authorSignal && fitsInline then
      inline
    else
      val explodedArgs = comments.exploded(argumentAlignment.aligned(args, indent + 2))
      Doc.text(s"(${keyword.trim}") ++ Doc.Nest(2, Doc.Line ++ explodedArgs) ++ Doc.Line ++ Doc.text(")")

}
