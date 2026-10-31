package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.render.Alignment
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*

/**
 * Renders infix applications (`a + b`, `xs map f`, `"name" should { ... }`).
 *
 * Both sides are rendered as operands, so a `{ ... }` right-hand side keeps its braces and its content follows the
 * block rules. The author's line break around the operator is kept (before it for a leading operator, after it for a
 * trailing one), and every line starts at the column of the first operand. An expression written on one line that does
 * not fit breaks after its operator; a run of the same operator (`a ++ b ++ c`) breaks after every operator. The value
 * of an assignment operator (`xs ++= ...`, `name := ...`) breaking after the operator is indented one level instead,
 * like a body after `=`. The arguments of an application with several of them (`x shouldBe (1, 2)`) stay on the line of
 * the operator and break as the arguments of a call. An application with type arguments is kept as written.
 *
 * When an operand before an operator is over several lines (`IO.blocking { ... }
 * >> next`), the operation breaks after every operator, whatever the operators: each operator stays at the end of its
 * line and each operand starts a line at the column of the first one, except a `{ ... }` operand, which opens on the
 * line of its operator. After an operand that is a broken selector chain, the following operands line up with its
 * selections. An operation with an assignment operator or a break before an operator is not part of this.
 *
 * @param ctx The shared render context.
 * @param applications The application renderer, for the operands.
 * @param chains The selector chain renderer, to tell a broken chain operand.
 */
private[term] final class InfixRenderer(ctx: RenderContext, applications: ApplicationRenderer, chains: ChainRenderer) {

  // An operand of a flattened operation, with the parentheses of the argument clause around it (as in `a + (b)`).
  private final case class Operand(term: Term, open: String, close: String) {
    def doc(at: Int): Doc = Doc.text(open) ++ applications.operand(term, at) ++ Doc.text(close)

    // A `{ ... }` operand, which opens on the line of its operator.
    def braced: Boolean =
      open.isEmpty && !ctx.isParenthesized(term) && (term.is[Term.Block] || term.is[Term.PartialFunction])
  }

  /**
   * Render an infix application at the given indent.
   */
  def render(infix: Term.ApplyInfix, indent: Int): Doc =
    val severalArgs =
      infix.targClause.values.isEmpty && infix.argClause.values.sizeIs > 1 && !ctx.hasGapComments(infix)

    operation(infix) match {
      case Some(arg) =>
        flowing(infix, indent).getOrElse(layout(infix, arg, indent))
      case None if severalArgs =>
        severalArguments(infix, indent)
      case None =>
        ctx.ownSourceDoc(infix)
    }

  // An application with several arguments, keeping the author's line break around the operator like any operation, its
  // arguments breaking as those of a call.
  private def severalArguments(infix: Term.ApplyInfix, indent: Int): Doc =
    val lhs    = applications.operand(infix.lhs, indent)
    val op     = ctx.ownSourceDoc(infix.op)
    val before = ctx.slice(ctx.span(infix.lhs)._2, infix.op.pos.start).contains('\n')
    val after  = ctx.slice(infix.op.pos.end, infix.argClause.pos.start).contains('\n')

    if before then
      val lead      = op ++ Doc.text(" ")
      val arguments = applications.arguments(infix.argClause, indent, ctx.endColumn(lead, indent))
      lhs ++ Doc.Line ++ lead ++ arguments
    else if after then
      val arguments = applications.arguments(infix.argClause, indent, indent)
      lhs ++ Doc.text(" ") ++ op ++ Doc.Line ++ arguments
    else
      val lead      = lhs ++ Doc.text(" ") ++ op ++ Doc.text(" ")
      val arguments = applications.arguments(infix.argClause, indent, ctx.endColumn(lead, indent))
      lead ++ arguments

  // An operation without an operand over several lines before an operator.
  private def layout(infix: Term.ApplyInfix, arg: Term, indent: Int): Doc =
    val (breakBefore, breakAfter) = authorBreaks(infix, arg)
    val lhs                       = applications.operand(infix.lhs, indent)
    val op                        = ctx.ownSourceDoc(infix.op)
    def assigned                  = lhs ++ Doc.text(" ") ++ op ++ Doc.Nest(2, Doc.Line ++ rhs(infix, arg, indent + 2))
    if breakBefore then
      lhs ++ Doc.Line ++ op ++ Doc.text(" ") ++ rhs(infix, arg, indent)
    else if breakAfter && isAssignment(infix.op.value) then
      assigned
    else if breakAfter then
      lhs ++ Doc.text(" ") ++ op ++ Doc.Line ++ rhs(infix, arg, indent)
    else
      val inline = lhs ++ Doc.text(" ") ++ op ++ Doc.text(" ") ++ rhs(infix, arg, indent)
      if !ctx.overflows(inline, indent) then inline
      else if isAssignment(infix.op.value) then assigned
      else Doc.intercalate(Doc.text(" ") ++ op ++ Doc.Line, run(infix, arg, indent))

  // The operation broken after every operator, when an operand before an operator is over several lines; None
  // otherwise. A chain operand that does not fit with what follows it on its line (` >>`, ` >> { x =>`) breaks, and the
  // operands after a broken chain are indented under its selections.
  private def flowing(infix: Term.ApplyInfix, indent: Int): Option[Doc] =
    flattenable(infix).flatMap { _ =>
      val (head, rest) = flattened(infix, "", "")
      val operands     = head :: rest.map(_._2)
      val docs         = operands.map(_.doc(indent))

      val anyMultiline = docs.dropRight(1).exists(doc => !Render.isFlat(doc))

      Option.when(rest.nonEmpty && anyMultiline) {
        val separators = Doc.Empty :: rest.map((op, operand) => separator(op, operand))
        val trailers =
          rest
            .zip(docs.drop(1))
            .map { case ((op, operand), doc) => Some(trailerWidth(op, operand, doc, indent)) }

        val pieces =
          separators
            .lazyZip(operands)
            .lazyZip(docs)
            .toList
            .zip(trailers :+ None)

        // State: the lines at `indent`, the lines after a broken chain, and whether one was seen.
        val (before, after, _) =
          pieces.foldLeft((Doc.Empty, Doc.Empty, false)) { case ((before, after, shifted), (piece, trailer)) =>
            val (sep, operand, cached) = piece

            val at   = if shifted then indent + 2 else indent
            val base = if shifted then operand.doc(at) else cached
            val doc =
              trailer match {
                case Some(width) if isChain(operand) && !ctx.fitsWidth(ctx.endColumn(base, at), width) =>
                  chains.render(operand.term, at, forceExplode = true, asBody = true)
                case _ =>
                  base
              }

            val breaks = trailer.isDefined && isChain(operand) && brokenChain(doc, at)
            if shifted then
              (before, after ++ sep ++ doc, true)
            else
              (before ++ sep ++ doc, after, breaks)
          }

        if after == Doc.Empty then
          before
        else
          before ++ Doc.Nest(2, after)
      }
    }

  // The width of what follows an operand on its line: its operator, and the first line of the next operand when that
  // opens with `{`.
  private def trailerWidth(op: Doc, next: Operand, nextDoc: Doc, indent: Int): Int =
    val opWidth = 1 + Render.flatWidth(op).getOrElse(0)
    if next.braced then
      opWidth + 1 + Render.render(nextDoc, indent).linesIterator.nextOption().fold(0)(_.length)
    else
      opWidth

  private def isChain(operand: Operand): Boolean =
    operand.open.isEmpty && !operand.braced && chains.isChain(operand.term)

  // Whether `doc`, a chain laid out at `indent`, is broken at its dots.
  private def brokenChain(doc: Doc, indent: Int): Boolean =
    Render
      .render(doc, indent)
      .linesIterator
      .drop(1)
      .nextOption()
      .exists(_.trim.startsWith("."))

  // The operator before `operand`: at the end of its line, with `operand` on the next line unless it opens with `{`.
  private def separator(op: Doc, operand: Operand): Doc =
    if operand.braced then
      Doc.text(" ") ++ op ++ Doc.text(" ")
    else
      Doc.text(" ") ++ op ++ Doc.Line

  // The argument of an operation that flattens with the operations in its operands: not parenthesised, without comments
  // between its parts, not an assignment and not broken before its operator.
  private def flattenable(infix: Term.ApplyInfix): Option[Term] =
    operation(infix).filter { arg =>
      val plain    = !ctx.isParenthesized(infix) && !ctx.hasGapComments(infix)
      val unbroken = !authorBreaks(infix, arg)._1
      plain && !isAssignment(infix.op.value) && unbroken
    }

  // The first operand and the (operator, operand) pairs after it of `term`, flattening the operations nested in its
  // operands in source order, whatever their operators.
  private def flattened(term: Term, open: String, close: String): (Operand, List[(Doc, Operand)]) =
    term match {
      case infix: Term.ApplyInfix if open.isEmpty && close.isEmpty =>
        flattenable(infix) match {
          case Some(arg) =>
            val (head, left)        = flattened(infix.lhs, "", "")
            val (argOpen, argClose) = clauseDelimiters(infix.argClause, arg).getOrElse(("", ""))
            val (first, right)      = flattened(arg, argOpen, argClose)
            (head, left ++ ((ctx.ownSourceDoc(infix.op), first) :: right))
          case None =>
            (Operand(term, open, close), Nil)
        }
      case _ =>
        (Operand(term, open, close), Nil)
    }

  /**
   * The width of the left operand of `a op b` when it renders on one line.
   */
  def leftWidth(infix: Term.ApplyInfix, indent: Int): Option[Int] =
    operation(infix).flatMap(_ => Render.flatWidth(applications.operand(infix.lhs, indent)))

  /**
   * `a op b` on one line with its left operand padded to `padTo` columns.
   */
  def padded(infix: Term.ApplyInfix, indent: Int, padTo: Int): Option[Doc] =
    operation(infix).map { arg =>
      val left        = applications.operand(infix.lhs, indent)
      val leftPadded  = left ++ Alignment.padding(Some(padTo), Render.flatWidth(left))
      val theOperator = Doc.text(" ") ++ ctx.ownSourceDoc(infix.op) ++ Doc.text(" ")
      leftPadded ++ theOperator ++ rhs(infix, arg, indent)
    }

  /**
   * The widths of the text before each operator of `a op1 b op2 c` on one line, each measured from the previous
   * operator, when its operators are all from `operators` (see [[columns]]).
   */
  def segments(infix: Term.ApplyInfix, indent: Int, operators: Set[String]): Option[List[Int]] =
    columns(infix, indent, operators).flatMap { (head, rest) =>
      val widths = (head :: rest.map((op, operand) => Doc.text("  ") ++ op ++ operand)).map(Render.flatWidth)
      Option.when(widths.forall(_.isDefined))(widths.flatten.dropRight(1))
    }

  /**
   * `a op1 b op2 c` on one line, with `pads` spaces added before each operator.
   */
  def paddedColumns(infix: Term.ApplyInfix, indent: Int, operators: Set[String], pads: List[Int]): Option[Doc] =
    columns(infix, indent, operators).map { (head, rest) =>
      rest
        .zip(pads.padTo(rest.size, 0))
        .foldLeft(head) { case (acc, ((op, operand), pad)) =>
          acc ++ Doc.text(" " * pad + " ") ++ op ++ Doc.text(" ") ++ operand
        }
    }

  // The first operand and the (operator, operand) pairs after it of an operation whose operators are all from
  // `operators`, flattening the operations from the list nested in its operands in source order (`"a" %% "b" % "1"`, A
  // plainly written operation from `operators`: not parenthesised and without comments in its gaps.
  private def plainOperation(infix: Term.ApplyInfix, operators: Set[String]): Boolean =
    operators(infix.op.value) && !ctx.isParenthesized(infix) && !ctx.hasGapComments(infix)

  // and `deps += "a" % "b"`); None for any other term.
  private def columns(term: Term, indent: Int, operators: Set[String]): Option[(Doc, List[(Doc, Doc)])] =
    term match {
      case infix: Term.ApplyInfix if plainOperation(infix, operators) =>
        operation(infix).map { arg =>
          val (head, left) =
            columns(infix.lhs, indent, operators).getOrElse((applications.operand(infix.lhs, indent), Nil))

          val op     = ctx.ownSourceDoc(infix.op)
          val bare   = clauseDelimiters(infix.argClause, arg).contains(("", ""))
          val nested = if bare then columns(arg, indent, operators) else None

          nested match {
            case Some((first, right)) => (head, left ++ ((op, first) :: right))
            case None                 => (head, left :+ (op, rhs(infix, arg, indent)))
          }
        }
      case _ =>
        None
    }

  // An assignment operator by the language rules: ending with `=`, not starting with `=` and not a comparison (`+=`,
  // `++=`, `:=`, but not `==`, `<=`).
  private def isAssignment(op: String): Boolean =
    op.endsWith("=") && !op.startsWith("=") && !Set("<=", ">=", "!=").contains(op)

  // The single argument of an infix application that is not kept as written.
  private def operation(infix: Term.ApplyInfix): Option[Term] =
    (infix.targClause.values, infix.argClause.values) match {
      case (Nil, arg :: Nil) if clauseDelimiters(infix.argClause, arg).isDefined => Some(arg)
      case _                                                                     => None
    }

  // Whether the author broke the line before and after the operator.
  private def authorBreaks(infix: Term.ApplyInfix, arg: Term): (Boolean, Boolean) =
    val before = ctx.slice(ctx.span(infix.lhs)._2, infix.op.pos.start).contains('\n')
    val after  = ctx.slice(infix.op.pos.end, ctx.span(arg)._1).contains('\n')
    (before, after)

  // The right-hand side, with the parentheses of its argument clause.
  private def rhs(infix: Term.ApplyInfix, arg: Term, at: Int): Doc =
    val (open, close) = clauseDelimiters(infix.argClause, arg).getOrElse(("", ""))
    Doc.text(open) ++ applications.operand(arg, at) ++ Doc.text(close)

  // The operands, left to right, of the run of the same operator that ends with this application and that the author
  // wrote on one line.
  private def run(infix: Term.ApplyInfix, arg: Term, indent: Int): List[Doc] =
    val left =
      infix.lhs match {
        case inner: Term.ApplyInfix if sameRun(infix, inner) =>
          operation(inner) match {
            case Some(innerArg) => run(inner, innerArg, indent)
            case None           => List(applications.operand(inner, indent))
          }
        case lhs =>
          List(applications.operand(lhs, indent))
      }

    left :+ rhs(infix, arg, indent)

  private def sameRun(outer: Term.ApplyInfix, inner: Term.ApplyInfix): Boolean =
    val sameOperator = inner.op.value == outer.op.value && !ctx.isParenthesized(inner)
    val unbroken     = operation(inner).exists(arg => authorBreaks(inner, arg) == (false, false))

    sameOperator && unbroken

  // The parentheses of the argument clause around `arg` (as in `a + (b * c)`), which belong to the clause rather than
  // to the argument; None when the clause holds anything else around the argument.
  private def clauseDelimiters(clause: Term.ArgClause, arg: Term): Option[(String, String)] =
    val (argStart, argEnd) = ctx.span(arg)
    val open               = ctx.slice(clause.pos.start, argStart)
    val close              = ctx.slice(argEnd, clause.pos.end)
    val onlyParens         = (open + close).forall(c => c == '(' || c == ')' || c.isWhitespace)
    Option.when(onlyParens)((open.filterNot(_.isWhitespace), close.filterNot(_.isWhitespace)))

}
