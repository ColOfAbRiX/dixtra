package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.render.comments.StatementComments
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*
import scala.meta.tokens.Token

/**
 * Renders `for ... yield` comprehensions and `for` loops.
 *
 * A comprehension written on one line that fits is kept as written, unless
 * `body.for-yield = break`. Otherwise its enumerators go one per line, in braces
 * (`for { ... } yield`) or braceless (`for ... yield`) by the brace style (one written with parentheses over several
 * lines is kept as written). The yield expression is placed by `body.for-yield` (see [[BodyLayout.yieldBelow]]), like
 * the body of a `for` loop after `do` by `body.for`: on the `yield` line without braces, or below it, inside braces
 * when the brace style uses them. Enumerators keep their header (`pattern <-`, `pattern =`, `if`) and their expression
 * follows the term rules; the comments and blank lines between them are kept. The generators and value definitions
 * align the start of their expression, also past a multi-line one, whose lines are laid out from that column. A guard
 * written after an enumerator on its line stays there while it fits; a guard on its own line starts its `if` at that
 * column.
 *
 * @param ctx The shared render context.
 * @param terms The term renderer, for the enumerator expressions and the yield.
 */
private[term] final class ForRenderer(ctx: RenderContext, terms: TermRenderer) {

  /**
   * Render a `for ... yield` at the given indent.
   */
  def render(forYield: Term.ForYield, indent: Int): Doc =
    val inline = ctx.ownSourceDoc(forYield)
    if oneLine(forYield, inline, indent, ctx.config.body.forYield == YieldBodyPolicy.AlwaysBreak) then
      inline
    else
      enumsOpener(forYield) match {
        case Some(_: Token.LeftParen) =>
          inline
        case opener =>
          val enumLines = enumeratorLines(forYield.enumsBlock, indent)
          if ctx.usesBraces(Construct.ForYield, opener.isDefined) then
            val opening = Doc.text("for {") ++ enumLines ++ Doc.Line ++ Doc.text("} yield")

            opening ++ yieldPart(forYield, indent, true)
          else
            val opening = Doc.text("for") ++ enumLines ++ Doc.Line ++ Doc.text("yield")

            opening ++ yieldPart(forYield, indent, false)
      }

  // A loop written on one line that fits there, which stays as written.
  private def oneLine(loop: Term, inline: Doc, indent: Int, breaks: Boolean): Boolean =
    !breaks && !loop.pos.text.contains('\n') && ctx.fits(inline, indent)

  /**
   * Render a `for` loop at the given indent: written on one line that fits in the syntax of its brace style, as
   * written; else braceless (`for x <- xs do`, or the enumerators one per line between `for` and `do`) with the body
   * placed by `body.for`, or in the braces syntax, by the brace style. In the braces syntax the body placed on its line
   * puts the loop on one line without braces (`for (x <- xs) f(x)`), and below it the enumerators and the body are each
   * in braces (`for { ... } { ... }`).
   */
  def renderFor(forLoop: Term.For, indent: Int): Doc =
    val inline = ctx.ownSourceDoc(forLoop)
    val braced = enumsOpener(forLoop).isDefined
    val braces = ctx.usesBraces(Construct.For, braced)

    val policy =
      ctx
        .config
        .body
        .forBody

    if braces == braced && oneLine(forLoop, inline, indent, policy == SingleBodyPolicy.AlwaysBreak) then
      inline
    else if braces then
      val body = terms.renderBody(BodyLayout.unwrapped(ctx, forLoop.body), indent + 2, forceExplode = false)

      val onLine =
        enumeratorsOnLine(forLoop.enumsBlock, indent)
          .map(enumerators => Doc.text("for (") ++ enumerators ++ Doc.text(") ") ++ body)
          .filter(ctx.fits(_, indent))

      val below  = BodyLayout.writtenBelow(ctx, forLoop.enumsBlock.pos.end, forLoop.body)
      val member = BodyLayout.Member(below, onLine.isDefined, BodyLayout.isSimple(ctx, forLoop.body))

      onLine
        .filterNot(_ => BodyLayout.singleBelow(policy, member))
        .getOrElse {
          val bracedBody = terms.bracedBody(forLoop.body, indent + 2, Construct.For)
          val opening =
            Doc.text("for {") ++ enumeratorLines(forLoop.enumsBlock, indent) ++ Doc.Line ++ Doc.text("} ")

          opening ++ Doc.braced(bracedBody)
        }
    else
      val body = terms.renderBody(forLoop.body, indent + 2, forceExplode = false)

      val single =
        forLoop.enumsBlock.enums match {
          case only :: Nil if ctx.commentsBetween(forLoop.enumsBlock.pos.start, forLoop.enumsBlock.pos.end).isEmpty =>
            Some(enumerator(only, indent + 4, None)).filter(
              enumerated => ctx.fits(Doc.text("for ") ++ enumerated ++ Doc.text(" do"), indent),
            )
          case _ =>
            None
        }

      val bodyStart = ctx.span(forLoop.body)._1

      val doEnd =
        ctx
          .tokensBetween(forLoop.enumsBlock.pos.end, bodyStart)
          .collectFirst { case t: Token.KwDo => t.end }
      val below = BodyLayout.writtenBelow(ctx, doEnd.getOrElse(forLoop.enumsBlock.pos.end), forLoop.body)

      def afterDo(lineWidth: Int): Doc =
        val member = BodyLayout.Member(below, ctx.fits(body, lineWidth + 1), BodyLayout.isSimple(ctx, forLoop.body))
        if BodyLayout.singleBelow(policy, member) then Doc.Nest(2, Doc.Line ++ body)
        else Doc.text(" ") ++ body

      single match {
        case Some(enumerators) =>
          val head = Doc.text("for ") ++ enumerators ++ Doc.text(" do")

          head ++ afterDo(ctx.columnAfter(head, indent))
        case None =>
          val opening = Doc.text("for") ++ enumeratorLines(forLoop.enumsBlock, indent) ++ Doc.Line ++ Doc.text("do")

          opening ++ afterDo(indent + 2)
      }

  // The enumerators on one line, `;` before a generator or a value definition and a space before a guard; None with
  // comments between them, or when one does not render on one line.
  private def enumeratorsOnLine(block: Term.EnumeratorsBlock, indent: Int): Option[Doc] =
    Option
      .when(ctx.commentsBetween(block.pos.start, block.pos.end).isEmpty) {
        block
          .enums
          .zipWithIndex
          .foldLeft(Doc.Empty) { case (acc, (current, index)) =>
            val separator = if index == 0 then Doc.Empty else if isGuard(current) then Doc.text(" ") else Doc.text("; ")

            acc ++ separator ++ enumerator(current, indent, None)
          }
      }
      .filter(Render.isFlat)

  // The enumerators one per line, one level in, with their comments.
  private def enumeratorLines(block: Term.EnumeratorsBlock, indent: Int): Doc =
    val enums    = block.enums
    val lines    = withTrailingGuards(enums)
    val comments = StatementComments.of(ctx, block.pos.start, lines.map(_._1), block.pos.end)

    val column =
      Option
        .when(ctx.config.alignment.arrows.forEnumerators) {
          enums.flatMap(header).map(_.width).maxOption
        }
        .flatten
      
    val docs = lines.map { case (lead, guards) => enumeratorLine(lead, guards, indent + 2, column) }

    Doc.Nest(2, Doc.Line ++ comments.lines(docs))

  // The brace or parenthesis that opens the enumerators, None when braceless.
  private def enumsOpener(forTerm: Term.For | Term.ForYield): Option[Token] =
    val block =
      forTerm match {
        case loop: Term.For        => loop.enumsBlock
        case yields: Term.ForYield => yields.enumsBlock
      }

    block
      .enums
      .headOption
      .flatMap { first =>
        ctx
          .tokensBetween(forTerm.pos.start, ctx.span(first)._1)
          .collectFirst {
            case t @ (_: Token.LeftBrace | _: Token.LeftParen) => t
          }
      }

  // The enumerators, each with the guards the author wrote after it on the same line (with no comment in between).
  private def withTrailingGuards(enums: List[Enumerator]): List[(Enumerator, List[Enumerator.Guard])] =
    enums.foldLeft(List.empty[(Enumerator, List[Enumerator.Guard])]) {
      case (acc :+ ((lead, guards)), guard: Enumerator.Guard) if sameLine(guards.lastOption.getOrElse(lead), guard) =>
        acc :+ (lead, guards :+ guard)
      case (acc, enumeratorTree) =>
        acc :+ (enumeratorTree, Nil)
    }

  private def isGuard(enumeratorTree: Enumerator): Boolean =
    enumeratorTree match {
      case _: Enumerator.Guard => true
      case _                   => false
    }

  private def sameLine(prev: Enumerator, next: Enumerator): Boolean =
    val (prevEnd, nextStart) = (ctx.span(prev)._2, ctx.span(next)._1)
    !ctx.slice(prevEnd, nextStart).contains('\n') && ctx.commentsBetween(prevEnd, nextStart).isEmpty

  // An enumerator with its trailing guards, which stay on its line while it fits; a guard that does not fit goes on its
  // own line, aligned like one.
  private def enumeratorLine(lead: Enumerator, guards: List[Enumerator.Guard], indent: Int, column: Option[Int]): Doc =
    guards.foldLeft(enumerator(lead, indent, column)) { (acc, guard) =>
      val onLine = acc ++ Doc.text(" ") ++ enumerator(guard, indent, None)
      if ctx.fits(onLine, indent) then onLine else acc ++ Doc.Line ++ enumerator(guard, indent, column)
    }

  // An enumerator: its header as written, then its expression. A generator or a value definition starts its expression
  // at `column` within the enumerator, and a guard starts its `if` there.
  private def enumerator(enumeratorTree: Enumerator, indent: Int, column: Option[Int]): Doc =
    (header(enumeratorTree), column, expression(enumeratorTree)) match {
      case (Some(head), Some(col), Some(term)) =>
        val pad       = Doc.text(" " * (col - head.width + 1))
        val headerDoc = Doc.text(head.pattern) ++ pad ++ Doc.text(head.operator + " ")
        val rendered  = Doc.Nest(col, terms.render(term, indent + col, forceExplode = false))
        headerDoc ++ rendered
      case (None, Some(col), Some(term)) if isGuard(enumeratorTree) =>
        val start    = ctx.span(term)._1
        val leading  = Doc.text(" " * col) ++ ctx.trimmedSourceDoc(enumeratorTree.pos.start, start) ++ Doc.text(" ")
        val rendered = Doc.Nest(col, terms.render(term, indent + col, forceExplode = false))
        leading ++ rendered
      case (_, _, Some(term)) =>
        val start = ctx.span(term)._1
        ctx.trimmedSourceDoc(enumeratorTree.pos.start, start) ++
        Doc.text(" ") ++
        terms.render(term, indent, forceExplode = false)
      case _ =>
        ctx.sourceDoc(enumeratorTree)
    }

  // The one-line pattern and the `<-` or `=` of a generator or value definition, when only whitespace separates them
  // from the expression.
  private def header(enumeratorTree: Enumerator): Option[EnumeratorHeader] =
    generated(enumeratorTree).flatMap { rhs =>
      val rhsStart = ctx.span(rhs)._1
      ctx
        .tokensBetween(enumeratorTree.pos.start, rhsStart)
        .reverseIterator
        .collectFirst { case t @ (_: Token.LeftArrow | _: Token.Equals) => t }
        .filter(op => ctx.slice(op.end, rhsStart).isBlank)
        .map(op => EnumeratorHeader(ctx.slice(enumeratorTree.pos.start, op.start).stripTrailing, op.text))
        .filterNot(_.pattern.contains('\n'))
    }

  private def generated(enumeratorTree: Enumerator): Option[Term] =
    enumeratorTree match {
      case g: Enumerator.Generator     => Some(g.rhs)
      case g: Enumerator.CaseGenerator => Some(g.rhs)
      case v: Enumerator.Val           => Some(v.rhs)
      case _                           => None
    }

  private def expression(enumeratorTree: Enumerator): Option[Term] =
    enumeratorTree match {
      case g: Enumerator.Guard => Some(g.cond)
      case _                   => generated(enumeratorTree)
    }

  // The yield expression after `yield`, placed by `body.for-yield`: on the `yield` line out of any braces it had, or
  // below it, inside braces when the brace style uses them (for `keep`, when the author wrote them).
  private def yieldPart(forYield: Term.ForYield, indent: Int, braces: Boolean): Doc =
    val body    = forYield.body
    val value   = BodyLayout.unwrapped(ctx, body)
    val doc     = terms.renderBody(value, indent + 2, forceExplode = false)
    val keyword = if braces then "} yield " else "yield "
    
    val yieldEnd =
      ctx
        .tokensBetween(forYield.enumsBlock.pos.end, ctx.span(body)._1)
        .collect { case t: Token.KwYield => t.end }

    val below   = yieldEnd.lastOption.exists(BodyLayout.writtenBelow(ctx, _, body))
    val member  = BodyLayout.Member(below, ctx.fits(doc, indent + keyword.length), BodyLayout.isSimple(ctx, body))
    val written = body.is[Term.Block] && ctx.writtenWithBraces(body)

    if !BodyLayout.yieldBelow(ctx.config.body.forYield, member) then
      Doc.text(" ") ++ doc
    else if ctx.usesBraces(Construct.ForYield, written) then
      Doc.text(" ") ++ Doc.braced(terms.bracedBody(body, indent + 2, Construct.ForYield))
    else
      Doc.Nest(2, Doc.Line ++ doc)

}

// The header of an enumerator before its expression: `width` is the column at which the expression starts after it.
private final case class EnumeratorHeader(pattern: String, operator: String) {
  def width: Int = pattern.length + 1 + operator.length + 1
}
