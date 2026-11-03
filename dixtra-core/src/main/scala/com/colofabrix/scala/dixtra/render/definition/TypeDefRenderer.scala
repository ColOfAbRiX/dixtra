package com.colofabrix.scala.dixtra.render.definition

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.render.Alignment
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*
import scala.meta.tokens.Token

/**
 * Renders a type definition (`type A = ...`): its name, padded when aligned, then `=` and its type, placed by
 * `body.type-def`.
 *
 * A run of `|`, `&` or `=>` at one level is one operation, laid out like an operation between terms: on one line, or
 * one operand per line with the operator at the end of the line before, all the operands at the same indent. It breaks
 * when it does not fit, even below the `=`, or when the author broke it. A parenthesised operand is opaque.
 *
 * @param ctx The shared render context.
 */
private[render] final class TypeDefRenderer(ctx: RenderContext) {

  /**
   * The type definition, its `=` padded to `padTo` when aligned and its type below the `=` when `below`, or None to
   * keep it as written: a comment in it, or a type written over several lines that is not an operation.
   */
  def render(t: Defn.Type, padTo: Option[Int], below: Boolean): Option[Doc] =
    val indent = ctx.lineIndentOf(t.pos.start)

    for
      name <- nameDoc(t)
              if ctx.commentsBetween(t.pos.start, t.pos.end).isEmpty
      lead  = name ++ Alignment.padding(padTo, Render.flatWidth(name)) ++ Doc.text(" =")
      doc  <- bodyDoc(t, lead, indent, below)
    yield
      doc

  // The type after `lead` (the name and its `=`): a match type always below the `=`, any other placed by
  // `body.type-def`.
  private def bodyDoc(t: Defn.Type, lead: Doc, indent: Int, below: Boolean): Option[Doc] =
    t.body match {
      case matchType: Type.Match =>
        Some(lead ++ Doc.Nest(2, Doc.Line ++ matchTypeDoc(matchType, indent + 2)))
      case other =>
        bodyOf(other).map(placed(lead, _, indent, below || writtenBelow(t)))
    }

  /**
   * The text of a type definition before its `=`.
   */
  def nameDoc(t: Defn.Type): Option[Doc] =
    ctx
      .equalsBefore(t.pos.start, t.body.pos.start)
      .map(eq => ctx.trimmedSourceDoc(t.pos.start, eq.start))

  // The type of a definition, on one line and broken: an operation one operand per line, any other type the same both
  // ways. None for a type written over several lines that is not an operation.
  private final case class TypeBody(oneLine: Doc, broken: Doc, authorBroke: Boolean)

  private def bodyOf(body: Type): Option[TypeBody] =
    val text    = ctx.slice(body.pos.start, body.pos.end)
    val written = text.contains('\n')

    operationOf(body) match {
      case Some(operation) =>
        Some(TypeBody(joined(operation, Doc.text(" ")), joined(operation, Doc.Line), written))
      case None =>
        Option.unless(written)(TypeBody(Doc.text(text), Doc.text(text), authorBroke = false))
    }

  // An operation of types: its operator and the text of its operands in order, each on one line.
  private final case class Operation(operator: String, operands: List[String])

  // The operands of a run of one operator at the top level of `tpe`: `(A & B) | C | D` as `(A & B)`, `C` and `D`. The
  // operands are cut at the operator tokens outside parentheses and brackets, so a parenthesised operand stays whole.
  // None when `tpe` is not such an operation.
  private def operationOf(tpe: Type): Option[Operation] =
    operatorOf(tpe).flatMap { operator =>
      val tokens = ctx.tokensBetween(tpe.pos.start, tpe.pos.end).toList
      val cuts   = topLevelOperators(tokens, operator)

      Option.when(cuts.nonEmpty) {
        val starts   = tpe.pos.start :: cuts.map(_.end)
        val ends     = cuts.map(_.start) :+ tpe.pos.end
        val operands = starts.zip(ends).map((start, end) => oneLine(ctx.slice(start, end)))
        Operation(operator, operands)
      }
    }

  // The operator of a binary type: Scalameta reads `A | B` and `A & B` as infix types named by the operator.
  private def operatorOf(tpe: Type): Option[String] =
    tpe match {
      case Type.ApplyInfix(_, op, _) if typeOperators(op.value)         => Some(op.value)
      case Type.Function.After_4_6_0(Type.FuncParamClause(_ :: Nil), _) => Some("=>")
      case _                                                            => None
    }

  private val typeOperators = Set("|", "&")

  // The tokens of `operator` among `tokens` at depth 0, outside parentheses and brackets.
  private def topLevelOperators(tokens: List[Token], operator: String): List[Token] =
    val depths =
      tokens.scanLeft(0) { (depth, token) =>
        token match {
          case _: Token.LeftParen | _: Token.LeftBracket   => depth + 1
          case _: Token.RightParen | _: Token.RightBracket => depth - 1
          case _                                           => depth
        }
      }

    tokens
      .zip(depths)
      .collect { case (token, 0) if token.text == operator => token }

  // The operands of an operation joined by `separator`, each operator at the end of the line of its left operand.
  private def joined(operation: Operation, separator: Doc): Doc =
    val operator = Doc.text(s" ${operation.operator}")
    Doc.intercalate(operator ++ separator, operation.operands.map(Doc.text))

  private def oneLine(text: String): String =
    text.trim.replaceAll("\\s*\\n\\s*", " ")

  // A match type, always below the `=`, laid out like a `match` term: its cases one per line, in braces or indented by
  // `braces.match`, their arrows aligned by `alignment.arrows.cases`.
  private def matchTypeDoc(matchType: Type.Match, indent: Int): Doc =
    val scrutinee = oneLine(ctx.slice(matchType.tpe.pos.start, matchType.tpe.pos.end))
    val braced    = ctx.usesBraces(Construct.Match, writtenWithBraces(matchType))
    val lines     = Doc.intercalate(Doc.Line, caseLines(matchType.casesBlock.cases, indent + 2))
    val open      = Doc.text(s"$scrutinee match")

    if braced then
      open ++ Doc.text(" ") ++ Doc.braced(lines)
    else
      open ++ Doc.Nest(2, Doc.Line ++ lines)

  // Whether the cases of a match type are written in braces: a `{` right after its `match`.
  private def writtenWithBraces(matchType: Type.Match): Boolean =
    ctx
      .slice(matchType.tpe.pos.end, matchType.pos.end)
      .trim
      .stripPrefix("match")
      .trim
      .startsWith("{")

  // The cases one per line, `case pattern => type`, the arrows padded to one column when `alignment.arrows.cases` is
  // on and every case fits once padded.
  private def caseLines(cases: List[TypeCase], indent: Int): List[Doc] =
    val heads  = cases.map(c => "case " + oneLine(ctx.slice(c.pat.pos.start, c.pat.pos.end)))
    val bodies = cases.map(c => oneLine(ctx.slice(c.body.pos.start, c.body.pos.end)))
    val target = heads.map(_.length).maxOption.getOrElse(0)

    val padded =
      heads
        .zip(bodies)
        .map((head, body) => head.padTo(target, ' ') + " => " + body)

    val aligned = ctx.config.alignment.arrows.cases && padded.forall(line => ctx.fitsWidth(indent, line.length))

    val lines =
      if aligned then padded
      else heads.zip(bodies).map((head, body) => head + " => " + body)

    lines.map(Doc.text)
  private def writtenBelow(t: Defn.Type): Boolean =
    val keep = ctx.config.body.typeDef == SingleBodyPolicy.Keep

    val written =
      ctx
        .equalsBefore(t.pos.start, t.body.pos.start)
        .exists(eq => ctx.slice(eq.end, t.body.pos.start).contains('\n'))

    keep && written

  // The type after `lead` (the name and its `=`): on its line when it fits there on one line; else below it, on one
  // line when it fits there and the author did not break it, broken otherwise.
  private def placed(lead: Doc, body: TypeBody, indent: Int, below: Boolean): Doc =
    val policy     = ctx.config.body.typeDef
    val column     = ctx.endColumn(lead, indent) + 1
    val down       = below || policy == SingleBodyPolicy.AlwaysBreak
    val fitsOnLine = !body.authorBroke && ctx.fits(body.oneLine, column)
    val fitsBelow  = !body.authorBroke && ctx.fits(body.oneLine, indent + 2)
    val belowDoc   = if fitsBelow then body.oneLine else body.broken

    if fitsOnLine && !down then
      lead ++ Doc.text(" ") ++ body.oneLine
    else
      lead ++ Doc.Nest(2, Doc.Line ++ belowDoc)

}
