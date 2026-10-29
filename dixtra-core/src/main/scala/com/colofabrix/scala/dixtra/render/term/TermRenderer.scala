package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.render.Alignment
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*
import scala.meta.tokens.Token

/**
 * Renders terms (expressions) into a fully-decided [[com.colofabrix.scala.dixtra.doc.Doc]]: the routing point that
 * dispatches each term to the renderer of its construct.
 *
 * Each supported term applies the whole-construct decision: a construct stays inline unless a trigger fires
 * (author-signalled newline, forced explode, or overflow past the configured max column), in which case it explodes
 * fully. A construct whose inline form would contain a multiline child always explodes. Unsupported terms fall back to
 * a verbatim slice of their original source.
 *
 * Two safety rules apply to every construct that is rebuilt from its children:
 *   - grouping parentheses that Scalameta leaves out of a term's position are re-emitted around the rendered term
 *     (here), so precedence never changes;
 *   - if a comment sits in a gap the rebuild would drop, the term is kept verbatim (statement blocks and lists instead
 *     re-attach their comments).
 *
 * @param ctx The shared render context.
 * @param definitions Renders the definitions among the statements of a block (local `val`s and `def`s) at an indent:
 *                    None for a statement that is not a definition or must be kept as written.
 */
final class TermRenderer(ctx: RenderContext, definitions: (List[Stat], Int) => List[Option[Doc]]) {

  private val blocks         = new BlockRenderer(ctx, this, definitions)
  private val caseClauses    = new CaseClauseRenderer(ctx, this)
  private val applications   = new ApplicationRenderer(ctx, this, blocks, caseClauses)
  private val chains         = new ChainRenderer(ctx, this, applications)
  private val ifElses        = new IfElseRenderer(ctx, this)
  private val fors           = new ForRenderer(ctx, this)
  private val whiles         = new WhileRenderer(ctx, this)
  private val infixes        = new InfixRenderer(ctx, applications, chains)
  private val assigns        = new AssignRenderer(ctx, this, applications)
  private val lambdas        = new LambdaRenderer(ctx, this)
  private val interpolations = new InterpolationRenderer(ctx, this)

  /**
   * A block of statements in braces: `{`, the statements one per line at `indent`, and `}` on a line of its own, one
   * level out, with the blank lines at its edges set for `construct`.
   */
  private[render] def bracedBlock(block: Term.Block, indent: Int, construct: Construct): Doc =
    Doc.braced(blocks.statements(block, indent, ctx.config.blankLines(construct)))

  /**
   * The content of a braced body of `construct` (the `if` branches, a loop body) at `indent`, with the blank lines at
   * its edges set for `construct`.
   */
  private[term] def bracedBody(body: Term, indent: Int, construct: Construct): Doc =
    val edges = ctx.config.blankLines(construct)
    body match {
      case block: Term.Block if !ctx.isParenthesized(block) && block.stats.sizeIs > 1 =>
        blocks.statements(block, indent, edges)
      case _ =>
        val doc = render(body, indent, forceExplode = false)
        if edges == BlankLinePolicy.Always then Doc.intercalate(Doc.Line, List(Doc.Empty, doc, Doc.Empty)) else doc
    }

  // An anonymous class written on one line that fits stays as written; else its members go one per line below `new A {`
  // / `new A:`, by the brace style. One with a self type, early definitions or a header over several lines is kept as
  // written.
  private def newAnonymous(n: Term.NewAnonymous, indent: Int): Doc =
    val templ  = n.templ
    val inline = ctx.ownSourceDoc(n)
    val opener =
      templ
        .body
        .stats
        .headOption
        .flatMap { first =>
          ctx
            .tokensBetween(n.pos.start, first.pos.start)
            .filterNot(_.is[Token.Whitespace])
            .lastOption
        }
    val header = opener.map(o => ctx.slice(n.pos.start, o.start).trim)
    (opener, header) match {
      case _ if !n.pos.text.contains('\n') && ctx.fits(inline, indent) =>
        inline
      case (Some(o @ (_: Token.LeftBrace | _: Token.Colon)), Some(h))
          if !h.contains('\n') && templ.body.selfOpt.isEmpty && templ.earlyClause.isEmpty =>
        val braced  = o.is[Token.LeftBrace]
        val bodyEnd = if braced then n.pos.end - 1 else n.pos.end
        val members =
          (edges: BlankLinePolicy) => blocks.statementsIn(o.end, templ.body.stats, bodyEnd, indent + 2, edges)
        if ctx.usesBraces(Construct.NewAnonymous, braced) then
          val edges = ctx.config.blankLines(Construct.NewAnonymous)
          Doc.text(h + " {") ++ Doc.Nest(2, Doc.Line ++ members(edges)) ++ Doc.Line ++ Doc.text("}")
        else
          Doc.text(h + ":") ++ Doc.Nest(2, Doc.Line ++ members(BlankLinePolicy.Never))
      case _ =>
        inline
    }

  /**
   * A named argument `name = value` of an exploded argument list.
   */
  private[term] def namedArgument(assign: Term.Assign, indent: Int, padTo: Option[Int]): Doc =
    assigns.renderArgument(assign, indent, padTo)

  /**
   * The width of the name of a named argument when it renders on one line.
   */
  private[term] def namedArgumentWidth(assign: Term.Assign): Option[Int] =
    assigns.leftWidth(assign)

  /**
   * The width of the left operand of `a op b` when it renders on one line.
   */
  private[term] def infixLeftWidth(infix: Term.ApplyInfix, indent: Int): Option[Int] =
    infixes.leftWidth(infix, indent)

  /**
   * `a op b` on one line with its left operand padded to `padTo` columns.
   */
  private[term] def paddedInfix(infix: Term.ApplyInfix, indent: Int, padTo: Int): Option[Doc] =
    infixes.padded(infix, indent, padTo)

  /**
   * The widths of the text before each operator of `a op1 b op2 c` on one line, when its operators are all from
   * `operators`.
   */
  private[term] def infixSegments(infix: Term.ApplyInfix, indent: Int, operators: Set[String]): Option[List[Int]] =
    infixes.segments(infix, indent, operators)

  /**
   * `a op1 b op2 c` on one line with `pads` spaces added before each operator.
   */
  private[term] def paddedInfixColumns(
    infix: Term.ApplyInfix,
    indent: Int,
    operators: Set[String],
    pads: List[Int],
  ): Option[Doc] =
    infixes.paddedColumns(infix, indent, operators, pads)

  /**
   * The operator statements among `stats` (None for other statements), with the operators of adjacent one-line ones
   * aligned column by column
   * (`alignment.operators`): `x = 1` and `x += 1` alike pad their left side so
   * the operators start in the same column, and in `"a" %% "b" % "1"` the `%` aligns too. The operation that is the
   * only argument of a call statement (`addSbtPlugin("org" % "plugin" % "1.0")`) aligns as well.
   */
  def alignedAssignments(stats: List[Stat], indentOf: Stat => Int): List[Option[Doc]] =
    val operators =
      ctx
        .config
        .alignment
        .operators
        .toSet
    val rows =
      stats
        .map { stat =>
          val operation = operatorStatement(stat, operators)
          val unpadded  = operation.map(o => render(o.term, indentOf(stat), forceExplode = false))
          stat.pos.start -> OperatorRow(operation, unpadded, unpadded.flatMap(Render.flatWidth))
        }
        .toMap
    val row = (s: Stat) => rows(s.pos.start)
    val segments =
      (s: Stat) =>
        row(s)
          .operation
          .flatMap {
            case OperatorStatement.Assign(a) =>
              assigns.leftWidth(a).map(List(_))
            case OperatorStatement.Infix(i) =>
              infixSegments(i, indentOf(s), operators)
            case OperatorStatement.Call(_, fun, infix) =>
              infixSegments(infix, indentOf(s), operators).map {
                case first :: rest => first + fun.length + 1 :: rest
                case Nil           => Nil
              }
          }
    val pads =
      Alignment.columnTargets(ctx, stats, Alignment.Grouping.Lines)(
        eligible = s => row(s).operation.isDefined && row(s).width.isDefined,
        segments = segments,
        fitsAt = (s, p) => row(s).width.exists(w => ctx.fitsWidth(indentOf(s), w + p.sum)),
      )
    stats
      .zip(pads)
      .map { (stat, pad) =>
        (row(stat).operation, pad) match {
          case (Some(OperatorStatement.Assign(a)), Some(p)) =>
            Some(assigns.render(a, indentOf(stat), assigns.leftWidth(a).map(_ + p.sum)))
          case (Some(OperatorStatement.Infix(i)), Some(p)) =>
            paddedInfixColumns(i, indentOf(stat), operators, p)
          case (Some(OperatorStatement.Call(_, fun, infix)), Some(p)) =>
            paddedInfixColumns(infix, indentOf(stat), operators, p).map(
              arg => Doc.text(fun + "(") ++ arg ++ Doc.text(")"),
            )
          case _ =>
            row(stat).unpadded
        }
      }

  // A statement whose operators align (see `alignedAssignments`): an assignment
  // `x = 1`, an operation `x += 1`, or a call `name(a op b)` written as
  // `name(` + operation + `)`.
  private enum OperatorStatement(val term: Term) {
    case Assign(assign: Term.Assign)                                 extends OperatorStatement(assign)
    case Infix(infix: Term.ApplyInfix)                               extends OperatorStatement(infix)
    case Call(call: Term.Apply, fun: String, infix: Term.ApplyInfix) extends OperatorStatement(call)
  }

  // A statement among those `alignedAssignments` lays out: its operation if it has one, rendered without padding, and
  // the width of that on one line.
  private final case class OperatorRow(operation: Option[OperatorStatement], unpadded: Option[Doc], width: Option[Int])

  private def operatorStatement(stat: Stat, operators: Set[String]): Option[OperatorStatement] =
    stat match {
      case a: Term.Assign if operators("=") && !ctx.isParenthesized(a) =>
        Some(OperatorStatement.Assign(a))
      case i: Term.ApplyInfix if operators(i.op.value) && !ctx.isParenthesized(i) =>
        Some(OperatorStatement.Infix(i))
      case c: Term.Apply =>
        callOperation(c, operators).map((fun, infix) => OperatorStatement.Call(c, fun, infix))
      case _ =>
        None
    }

  // The name and the only argument of a call `name(a op b)` written as `name(` + argument + `)`, when the argument is
  // an operation from `operators`.
  private def callOperation(call: Term.Apply, operators: Set[String]): Option[(String, Term.ApplyInfix)] =
    (call.fun, call.argClause.values) match {
      case (fun: Term.Name, (infix: Term.ApplyInfix) :: Nil) if writtenOperation(call, infix, operators) =>
        val (start, end) = ctx.span(infix)
        val written      = ctx.slice(fun.pos.end, start).trim == "(" && ctx.slice(end, call.pos.end).trim == ")"
        Option.when(written)((fun.value, infix))
      case _ =>
        None
    }

  // The call is plainly written: no modifiers on its clause, no parentheses around it, no comments in its gaps, and an
  // operation among `operators`.
  private def writtenOperation(call: Term.Apply, infix: Term.ApplyInfix, operators: Set[String]): Boolean =
    val plain      = call.argClause.mod.isEmpty && !ctx.isParenthesized(call)
    val noComments = !ctx.hasGapComments(call) && !ctx.hasGapComments(call.argClause)

    plain && noComments && operators(infix.op.value)

  /**
   * Render a term at the given indentation. `forceExplode` carries an author signal from the enclosing construct (a
   * newline after `=` or `=>`).
   */
  def render(term: Term, indent: Int, forceExplode: Boolean): Doc =
    render(term, indent, forceExplode, asBody = false)

  /**
   * Render a term that is a body, after a separator (`=`, `=>`, `then`, `do`, ...): a select chain there also breaks by
   * `selector-chain.break-at-count`.
   */
  def renderBody(term: Term, indent: Int, forceExplode: Boolean): Doc =
    render(term, indent, forceExplode, asBody = true)

  private def render(term: Term, indent: Int, forceExplode: Boolean, asBody: Boolean): Doc =
    val (start, end) = ctx.span(term)
    if start == term.pos.start then
      renderUnwrapped(term, indent, forceExplode, asBody)
    else
      val open  = ctx.slice(start, term.pos.start)
      val close = ctx.slice(term.pos.end, end)
      if (open + close).forall(c => c == '(' || c == ')' || c.isWhitespace) then
        Doc.text(open.filterNot(_.isWhitespace)) ++
        renderUnwrapped(term, indent, forceExplode, asBody) ++
        Doc.text(close.filterNot(_.isWhitespace))
      else
        ctx.sourceDoc(term)

  private def renderUnwrapped(term: Term, indent: Int, forceExplode: Boolean, asBody: Boolean): Doc =
    term match {
      case block: Term.Block =>
        blocks.render(block, indent, forceExplode)
      case ifTerm: Term.If =>
        ifElses.render(ifTerm, indent)
      case interpolate: Term.Interpolate =>
        interpolations.render(interpolate)
      case _ if chains.isChain(term) =>
        chains.render(term, indent, forceExplode, asBody)
      case m: Term.Match =>
        caseClauses.renderMatch(m, indent)
      case pf: Term.PartialFunction =>
        applications.operand(pf, indent)
      case _ if ctx.hasGapComments(term) =>
        ctx.ownSourceDoc(term)
      case f: Term.ForYield =>
        fors.render(f, indent)
      case f: Term.For =>
        fors.renderFor(f, indent)
      case w: Term.While =>
        whiles.render(w, indent)
      case n: Term.NewAnonymous =>
        newAnonymous(n, indent)
      case infix: Term.ApplyInfix =>
        infixes.render(infix, indent)
      case assign: Term.Assign =>
        assigns.render(assign, indent)
      case t: Term.Try =>
        caseClauses.renderTry(t, indent)
      case f: Term.Function =>
        lambdas.render(f, indent)
      case Term.Apply.After_4_6_0(fun, argClause) =>
        applications.render(fun, argClause, indent)
      case t: Term.Tuple =>
        applications.tuple(t, indent)
      case _ =>
        ctx.ownSourceDoc(term)
    }

}
