package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*
import scala.meta.tokens.Token

/**
 * A trailing `{ ... }` block given as an argument or an operand: a lambda, a multi-statement block, or a `{ case ... }`
 * partial function, in its brace and braceless forms (D5).
 *
 * @param ctx The shared render context.
 * @param terms The term renderer, for nested terms.
 * @param blocks The block renderer, for multi-statement brace blocks.
 * @param caseClauses The case clause renderer, for `{ case ... }` blocks.
 */
private[term] final class ArgumentBlocks(
  ctx: RenderContext,
  terms: TermRenderer,
  blocks: BlockRenderer,
  caseClauses: CaseClauseRenderer,
) {

  // A lambda argument over several lines in the Scala 3 braceless form (`xs.map: x =>` with the body indented below),
  // when its brace style does not use braces; None for any other block.
  def colonLambda(block: Term.Block, indent: Int): Option[Doc] =
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
  def colonBlock(block: Term.Block, indent: Int): Option[Doc] =
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

    if test then
      Construct.TestBlock
    else
      Construct.BlockArgument

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
  // kept; the contents follow the same layout rules and re-indent when exploded. A lambda's body follows the `=>` rules
  // of [[BodyLayout]], placed by `body.lambda-argument`: on the `=>` line it stays on one line with the braces,
  // `{ x => body }`.
  def braceBlock(arg: Term, indent: Int, column: Int): Doc =
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

        if !BodyLayout.singleBelow(blockBody(construct), member) then
          inline
        else
          Doc.braced(blocks.statements(block, indent + 2, ctx.config.blankLines(construct)))

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
