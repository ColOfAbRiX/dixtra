package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.render.*
import com.colofabrix.scala.dixtra.render.comments.CommentedList
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*
import scala.meta.tokens.Token

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
   * closing `)` goes on
   * a line of its own at `indent`. With `call-site.groups-break = last-first` only the
   * last group breaks while the groups before it stay on the line of the call;
   * with `keep` each group decides by itself. A trailing brace block keeps its own rules.
   */
  def argumentClauses(clauses: List[Term.ArgClause], indent: Int, column: Int): Doc =
    val (groups, rest) = clauses.span(isGroup)
    if groups.sizeIs < 2 then
      following(clauses, Doc.Empty, indent, column)
    else
      grouped(groups, indent, column) match {
        case Some(doc) => following(rest, doc, indent, column)
        case None      => following(clauses, Doc.Empty, indent, column)
      }

  // `clauses` after `before`, each deciding its layout from the column where it starts.
  private def following(clauses: List[Term.ArgClause], before: Doc, indent: Int, column: Int): Doc =
    clauses.foldLeft(before)((acc, clause) => acc ++ arguments(clause, indent, ctx.endColumn(acc, indent, column)))

  // A parenthesised argument clause, as opposed to a trailing brace block.
  private def isGroup(clause: Term.ArgClause): Boolean =
    clause.values match {
      case (block: Term.Block) :: Nil if !ctx.isParenthesized(block)     => false
      case (pf: Term.PartialFunction) :: Nil if !ctx.isParenthesized(pf) => false
      case _                                                             => true
    }

  // Whether the groups before the last stay on the line of the call: written on one line, without a comment that forces
  // a break, and fitting with the `(` of the last group.
  private def leadingStay(leading: List[(Term.ArgClause, List[Term], CommentedList)], column: Int): Boolean =
    val onLine =
      Doc.concat(
        leading.map { (clause, args, comments) =>
          ClauseGroups.Group(Syntax.clauseKeyword(clause.mod), args.map(a => ctx.sourceDoc(a)), comments).inline
      },
    )
    !leading.exists((clause, _, comments) => comments.forcesBreak || clause.pos.text.contains('\n')) &&
    ctx.fits(onLine ++ Doc.text("("), column)

  // The argument groups on one line, or broken together; None when each group decides its own layout: a group must be
  // kept as written (an empty group holding a comment), or `call-site.groups-break` lets each group decide or only the
  // last group break.
  private def grouped(clauses: List[Term.ArgClause], indent: Int, column: Int): Option[Doc] =
    val groups =
      clauses.map { clause =>
        val args = clause.values
        (clause, args, CommentedList.of(ctx, clause.pos.start, args, clause.pos.end))
      }
    val inline =
      Doc.concat(
        groups.map { (clause, args, comments) =>
          ClauseGroups.Group(Syntax.clauseKeyword(clause.mod), args.map(a => ctx.sourceDoc(a)), comments).inline
        }
      )
    val breaks =
      groups.exists((clause, _, comments) => comments.forcesBreak || clause.pos.text.contains('\n')) ||
      !ctx.fits(inline, column)
    if groups.exists((_, args, comments) => args.isEmpty && !comments.isEmpty) then
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
          ClauseGroups.Group(Syntax.clauseKeyword(clause.mod), aligned(args, indent + 2), comments)
        }
      val layout = ClauseGroups.broken(Doc.Empty, broken, ctx.config.callSite.groupsSeparator, indent)
      Some(if layout.open then layout.doc ++ Doc.Line ++ Doc.text(")") else layout.doc)

  /**
   * A term used as an operand (chain receiver, applied function, argument, side of an infix operator): a block or a
   * `{ case ... }` keeps its braces, because significant indentation is not valid there.
   */
  def operand(term: Term, indent: Int): Doc =
    term match {
      case block: Term.Block if !ctx.isParenthesized(block)     => braceBlock(block, indent, indent)
      case pf: Term.PartialFunction if !ctx.isParenthesized(pf) => braceBlock(pf, indent, indent)
      case _                                                    => terms.render(term, indent, forceExplode = false)
    }

  /**
   * A `{ ... }` block or `{ case ... }` that keeps its braces, starting at `column` on a line indented at
   * `indent`; None for any other term.
   */
  def braced(term: Term, indent: Int, column: Int): Option[Doc] =
    term match {
      case (_: Term.Block | _: Term.PartialFunction) if !ctx.isParenthesized(term) =>
        Some(braceBlock(term, indent, column))
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
        val withBraces = braceBlock(block, indent, column + 1)
        colonLambda(block, indent)
          .orElse(colonBlock(block, indent))
          .filter(_ => !Render.isFlat(withBraces))
          .getOrElse(Doc.text(" ") ++ withBraces)
      case (pf: Term.PartialFunction) :: Nil if !ctx.isParenthesized(pf) =>
        Doc.text(" ") ++ braceBlock(pf, indent, column + 1)
      case args =>
        parenArgs(clause, args, indent, column)
    }

  /**
   * A tuple starting at `indent`, laid out as an argument clause: inline unless a trigger fires, else one element per
   * line.
   */
  def tuple(t: Term.Tuple, indent: Int): Doc =
    parenthesised(t, "", t.args, indent, indent)

  // A parenthesised argument clause: inline unless a trigger fires, else one argument per line. A `using` clause keeps
  // its keyword on the opening line. Comments between arguments stay attached to their argument; a line comment or a
  // comment on its own line forces the clause to explode.
  private def parenArgs(clause: Term.ArgClause, args: List[Term], indent: Int, column: Int): Doc =
    parenthesised(clause, Syntax.clauseKeyword(clause.mod), args, indent, column)

  // The elements `args` of `tree`, between its parentheses, after `keyword`: an argument clause or a tuple.
  private def parenthesised(tree: Tree, keyword: String, args: List[Term], indent: Int, column: Int): Doc =
    val comments = CommentedList.of(ctx, tree.pos.start, args, tree.pos.end)
    val inline   = Doc.text(s"($keyword") ++ comments.inline(args.map(a => ctx.sourceDoc(a))) ++ Doc.text(")")

    val authorSignal =
      tree
        .pos
        .text
        .contains('\n')

    if args.isEmpty && comments.isEmpty then
      Doc.text(s"(${keyword.trim})")
    else if args.isEmpty then
      ctx.ownSourceDoc(tree)
    else if !comments.forcesBreak && !authorSignal && ctx.fits(inline, column) then
      inline
    else
      val explodedArgs = comments.exploded(aligned(args, indent + 2))
        Doc.text(s"(${keyword.trim}") ++ Doc.Nest(2, Doc.Line ++ explodedArgs) ++ Doc.Line ++ Doc.text(")")

  // The exploded arguments, aligning the `->` of consecutive one-line pairs and, column by column, the operators of
  // consecutive one-line operations from `alignment.operators` (see [[Alignment]]); the comma after each argument
  // counts towards the max column. Named arguments are not aligned.
  private def aligned(args: List[Term], indent: Int): List[Doc] =
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
  // breaks as it does not fit with the comma. Every argument has one, but the last without `trailing-commas = always`.
  private def withComma(followed: Boolean, indent: Int, render: Int => Doc): Doc =
    val doc   = render(indent)
    val comma = followed || ctx.config.trailingCommas == TrailingCommas.Always
    if comma && Render.flatWidth(doc).exists(w => indent + w == ctx.config.maxColumn) then render(indent + 1) else doc

  // The width to pad the name of each named argument to (see `aligned`), given the width of each argument on one line,
  // with `alignment.named-arguments`.
  private def nameTargets(args: List[Term], indent: Int, line: Term => Option[Int]): List[Option[Int]] =
    def name(a: Term): Option[Int] =
      a match {
        case assign: Term.Assign if !ctx.isParenthesized(assign) => terms.namedArgumentWidth(assign)
        case _                                    => None
      }
    Alignment.targets(ctx, args, Alignment.Grouping.ListElements)(
      eligible = a => ctx.config.alignment.namedArguments && line(a).isDefined && name(a).isDefined,
      width = name,
      fitsAt =
        (a, target) =>
          (line(a), name(a)) match {
            case (Some(1), Some(w)) => ctx.fitsWidth(indent, 1 + math.max(0, target - w) + 1)
            case _           => false
          },
    )

  // The width to pad the left side of each `a -> b` pair to (see `aligned`), given the width of each argument on one
  // line.
  private def arrowTargets(args: List[Term], indent: Int, line: Term => Option[Int]): List[Option[Int]] =
    def left(a: Term): Option[Int] =
      a match {
        case pair: Term.ApplyInfix if pair.op.value == "->" => terms.infixLeftWidth(pair, indent)
         case _                    => None
       }
    Alignment.targets(ctx, args, Alignment.Grouping.ListElements)(
      eligible = a => ctx.config.alignment.arrows.pairs && line(a).isDefined,
      width = left,
      fitsAt =
        (a, target) =>
          (line(a), left(a)) match {
            case (Some(l), Some(w)) => ctx.fitsWidth(indent, l + math.max(0, target - w) + 1)
            case _         => false
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

  // A lambda argument over several lines in the Scala 3 braceless form (`xs.map: x =>` with the body indented below),
  // when its brace style does not use braces; None for any other block.
  private def colonLambda(block: Term.Block, indent: Int): Option[Doc] =
    block match {
      case Term.Block((fn: Term.Function) :: Nil) if !ctx.hasGapComments(block) && !ctx.hasGapComments(fn) =>
        val written = !ctx.previousSignificant(ctx.span(block)._1).exists(_.is[Token.Colon])
        Option.when(!ctx.usesBraces(Construct.LambdaArgument, written)) {
          val arrowEnd = BodyLayout.arrowEnd(ctx, fn)
          val header   = ctx.trimmedSourceDoc(fn.pos.start, arrowEnd)
          val below =
            fn.body match {
              case Term.Block(stats @ (_ :: _ :: _)) =>
                blocks.statementsIn(arrowEnd, stats, block.pos.end, indent + 2, BlankLinePolicy.Never)
              case _ =>
                BodyLayout.of(ctx, terms, arrowEnd, fn.body, indent + 2).below
            }
          Doc.text(": ") ++ header ++ Doc.Nest(2, Doc.Line ++ below)
        }
      case _ =>
        None
    }

  // A block of statements over several lines in the Scala 3 braceless form (`test("a"):` with the statements indented
  // below), when its brace style does not use braces; None for a lambda, an empty block, or a block with a comment
  // before its first statement.
  private def colonBlock(block: Term.Block, indent: Int): Option[Doc] =
    block.stats match {
      case (_: Term.Function) :: Nil =>
        None
      case first :: _ if ctx.commentsBetween(block.pos.start, ctx.span(first)._1).isEmpty =>
        val construct = blockConstruct(block)
        Option.when(!ctx.usesBraces(construct, ctx.writtenWithBraces(block))) {
          val body = blocks.bracelessStatements(block, indent + 2, ctx.config.blankLines(construct))
          Doc.text(":") ++ Doc.Nest(2, Doc.Line ++ body)
        }
      case _ =>
        None
    }

   // The blank lines at the edges of a lambda or a `{ case ... }` given as an argument.
  private def argumentEdges: BlankLinePolicy =
    ctx.config.blankLines(Construct.LambdaArgument)
 
  // A block of statements given as an argument is a test block when the call it is given to is labelled by a string:
  // `"a test" in { ... }`, `it should "work" in { ... }`, `test("a test") { ... }`.
  private def blockConstruct(block: Term): Construct =
    def label(term: Tree): Boolean =
      term match {
        case _: Lit.String | _: Term.Interpolate => true
        case _                                   => false
      }
 
    def labelled(lhs: Term): Boolean =
      lhs match {
        case infix: Term.ApplyInfix =>
          infix
            .argClause
            .values
            .lastOption
            .exists(label)
        case other =>
          label(other)
      }

    val test =
      block
        .parent
        .flatMap(_.parent)
        .exists {
          case infix: Term.ApplyInfix =>
            labelled(infix.lhs)
          case Term.Apply.After_4_6_0(Term.Apply.After_4_6_0(_, args), _) =>
            args
              .values
              .headOption
              .exists(label)
          case _ =>
            false
        }

    if test then Construct.TestBlock 
    else Construct.BlockArgument

  // The policy of the statements of a block argument or a test block.
  private def blockBody(construct: Construct): SingleBodyPolicy =
    if construct == Construct.TestBlock then
      ctx
        .config
        .body
        .testBlock
    else
      ctx
        .config
        .body
        .blockArgument

  // A brace block (D5): a lambda, a multi-statement block, or a `{ case ... }` partial function. Braces are always
  // kept; the contents follow the same AON rules and re-indent when exploded. A lambda's body follows the `=>` rules of
  // [[BodyLayout]], placed by `body.lambda-argument`: on the `=>` line it stays on one line with the braces,
  // `{ x => body }`.
  private def braceBlock(arg: Term, indent: Int, column: Int): Doc =
    val authorSignal =
      arg
        .pos
        .text
        .contains('\n')

    arg match {
      case block @ Term.Block((fn: Term.Function) :: Nil) =>
        if ctx.hasGapComments(block) || ctx.hasGapComments(fn) then
          ctx.ownSourceDoc(arg)
        else
          val arrowEnd = BodyLayout.arrowEnd(ctx, fn)
          val header   = ctx.trimmedSourceDoc(fn.pos.start, arrowEnd)
          val body     = BodyLayout.of(ctx, terms, arrowEnd, fn.body, indent + 2)
          val braced   = (doc: Doc) => Doc.text("{ ") ++ header ++ Doc.text(" ") ++ doc ++ Doc.text(" }")

          val onLine =
            body
              .onSeparatorLine(doc => ctx.fits(braced(doc), column))
              .map(braced)
              .filter(ctx.fits(_, column))

          val member = BodyLayout.Member(body.writtenBelow, onLine.isDefined, body.simple)

          // A multi-statement body keeps the blank lines between `=>` and `}`.
          val below =
            fn.body match {
              case Term.Block(stats @ (_ :: _ :: _)) =>
                blocks.statementsIn(arrowEnd, stats, block.pos.end, indent + 2, argumentEdges)
              case _ =>
                body.below
            }

          onLine
            .filterNot(_ => BodyLayout.singleBelow(ctx.config.body.lambdaArgument, member))
            .getOrElse(Doc.text("{ ") ++ header ++ Doc.Nest(2, Doc.Line ++ below) ++ Doc.Line ++ Doc.text("}"))

      case block @ Term.Block(stats) if stats.nonEmpty =>
        val construct   = blockConstruct(block)
        val hasComments = ctx.commentsBetween(block.pos.start, block.pos.end).nonEmpty
        val inline      = Doc.text("{ ") ++ Doc.intercalate(Doc.text("; "), stats.map(ctx.sourceDoc)) ++ Doc.text(" }")
        val member      = BodyLayout.Member(authorSignal, !hasComments && ctx.fits(inline, column), stats.sizeIs == 1)
        if !BodyLayout.singleBelow(blockBody(construct), member) then inline
        else Doc.braced(blocks.statements(block, indent + 2, ctx.config.blankLines(construct)))

      case pf: Term.PartialFunction =>
        val hasComments = ctx.commentsBetween(pf.pos.start, pf.pos.end).nonEmpty

        val inline =
          Doc.text("{ ") ++ Doc.intercalate(Doc.text(" "), pf.cases.map(ctx.sourceDoc)) ++ Doc.text(" }")
        if !authorSignal && !hasComments && ctx.fits(inline, column) then
          inline
        else
          hangingCase(pf, indent, hasComments)
            .getOrElse(Doc.braced(caseClauses.cases(pf.pos.start, pf.cases, pf.pos.end, indent + 2, argumentEdges)))

      case block: Term.Block =>
        ctx.ownSourceDoc(block)

      case other =>
        terms.render(other, indent, forceExplode = false)
    }

  // A single `case` that the author wrote on the line of its `{` stays there, with its body below it, like the
  // parameters of a lambda: `{ case (a, b) =>`.
  private def hangingCase(pf: Term.PartialFunction, indent: Int, hasComments: Boolean): Option[Doc] =
    pf.cases match {
      case only :: Nil if !hasComments && !ctx.slice(pf.pos.start, only.pos.start).contains('\n') =>
        val clause = caseClauses.clause(only, indent)
        Option.when(Render.render(clause, indent).contains('\n'))(Doc.text("{ ") ++ clause ++ Doc.Line ++ Doc.text("}"))
      case _ =>
        None
    }

}
