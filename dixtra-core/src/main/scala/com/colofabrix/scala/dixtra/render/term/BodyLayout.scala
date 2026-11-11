package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*

/**
 * The body that follows a separator: the `=` of a definition or the `=>` of a lambda or a case, which follow the same
 * rules (D7, D22).
 *
 * A body goes on the separator line or below it by its `body` policy (see [[BodyLayout.singleBelow]] and
 * [[BodyLayout.groupBelow]]). It can be on the separator line only when it renders on one line, fits there and no
 * comment follows the separator. A newline after the separator is also an author signal for the body (it explodes
 * multi-selection chains). A comment after the separator on its line stays there; a comment on its own line stays on
 * its own line above the body. A `{ case ... }` body always opens its `{` on the separator line, and so does a block of
 * several statements whose construct's brace style uses braces, whatever the policy.
 *
 * Below the separator the body takes the layout the newline forces, so formatting the output again leaves it unchanged.
 * That layout is computed only when needed: nested lambdas would otherwise render every body twice per level.
 *
 * @param authorBreak True when the author broke the line after the separator.
 * @param trailing The comments after the separator on its line, each preceded by a space.
 * @param leading The comments on their own lines between the separator and the body, which stay on their own lines
 *                above the body.
 * @param hasComments True when a comment follows the separator.
 * @param onLine The body as rendered on the separator line.
 * @param braced The body when it is a `{ case ... }`, whose `{` always stays on the separator line (never on a line of
 *               its own).
 * @param renderBelow Renders the body as laid out below the separator.
 * @param simple True when the body is a single name, path or literal.
 */
private[render] final class BodyLayout(
  authorBreak: Boolean,
  trailing: Doc,
  leading: List[Doc],
  val hasComments: Boolean,
  val onLine: Doc,
  braced: Option[Doc],
  renderBelow: () => Doc,
  val simple: Boolean,
) {

  /**
   * The body as rendered below the separator.
   */
  lazy val below: Doc =
    renderBelow()

  /**
   * True when moving the body below the separator does not change its layout, so dropping it below is as good as
   * keeping it on the line.
   */
  def stableBelow: Boolean =
    authorBreak || Render.render(below) == Render.render(onLine)

   /**
    * True when the author wrote the body below the separator. A braced body, which always opens on the separator line,
    * never is.
    */
  def writtenBelow: Boolean =
    authorBreak && braced.isEmpty

  /**
   * The body as it goes on the separator line, if it can: a braced body always can; any other renders on one line and
   * `fits` there, with no comment after the separator. A body the author wrote below takes the layout it has there.
   */
  def onSeparatorLine(fits: Doc => Boolean): Option[Doc] =
    braced.orElse {
      val doc       = if authorBreak then below else onLine
      val onOneLine = !hasComments && Render.isFlat(doc) && fits(doc)
      Option.when(onOneLine)(doc)
    }

  /**
   * The separator line `lead` (ending with the separator) followed by the body below it, indented; a braced body still
   * opens on the separator line.
   */
  def belowSeparator(lead: Doc): Doc =
    braced match {
      case Some(block) =>
        lead ++ Doc.text(" ") ++ block
      case None =>
        lead ++ trailing ++ Doc.Nest(2, Doc.Line ++ Doc.concat(leading.map(_ ++ Doc.Line)) ++ below)
    }

  /**
   * The separator line `lead` followed by the body, a group of its own, placed by `policy`. `fits` tells whether a body
   * fits on the separator line.
   */
  def placed(lead: Doc, fits: Doc => Boolean, policy: SingleBodyPolicy): Doc =
    onSeparatorLine(fits) match {
      case Some(doc) if !BodyLayout.singleBelow(policy, BodyLayout.Member(writtenBelow, true, simple)) =>
        lead ++ Doc.text(" ") ++ doc
      case _ =>
        belowSeparator(lead)
    }

}

private[render] object BodyLayout {

  /**
   * A body of a group: whether the author wrote it below its separator, whether it can go on its separator line, and
   * whether it is simple (a single name, path or literal).
   */
  final case class Member(writtenBelow: Boolean, fitsOnLine: Boolean, simple: Boolean)

  /**
   * Whether the body of a construct with a single body goes below its separator: always with `always-break`; with
   * `keep` when the author wrote it below; and with any policy when it cannot go on its separator line.
   */
  def singleBelow(policy: SingleBodyPolicy, member: Member): Boolean =
    policy match {
      case SingleBodyPolicy.AlwaysBreak => true
      case SingleBodyPolicy.Keep        => member.writtenBelow || !member.fitsOnLine
      case SingleBodyPolicy.AlwaysJoin  => !member.fitsOnLine
    }

  /**
   * Whether the expression after a `yield` goes below it: as [[singleBelow]], and with `break-complex` when it is not
   * simple.
   */
  def yieldBelow(policy: YieldBodyPolicy, member: Member): Boolean =
    policy match {
      case YieldBodyPolicy.AlwaysBreak  => true
      case YieldBodyPolicy.Keep         => member.writtenBelow || !member.fitsOnLine
      case YieldBodyPolicy.AlwaysJoin   => !member.fitsOnLine
      case YieldBodyPolicy.BreakComplex => !member.simple || !member.fitsOnLine
    }

  /**
   * Whether the bodies of a group, which are never mixed, all go below their separators. A group all on its separator
   * lines, or all below them, stays as it is with `join-mixed` and `break-mixed`; a mixed one goes on them with
   * `join-mixed` and below them with `break-mixed`. A group that should be on its separator lines goes below when one
   * body cannot be joined.
   */
  def groupBelow(policy: MultiBodyPolicy, members: List[Member]): Boolean =
    val cannotJoin = members.exists(!_.fitsOnLine)
    val allBelow   = members.nonEmpty && members.forall(_.writtenBelow)

    policy match {
      case MultiBodyPolicy.AlwaysBreak => true
      case MultiBodyPolicy.AlwaysJoin  => cannotJoin
      case MultiBodyPolicy.JoinMixed   => cannotJoin || allBelow
      case MultiBodyPolicy.BreakMixed  => cannotJoin || members.exists(_.writtenBelow)
    }

  /**
   * Whether `body` holds an if, a match or a try, which keeps the body of a group from being joined on its separator
   * line.
   */
  def holdsControl(body: Term): Boolean =
    body.collect { case _: Term.If | _: Term.Match | _: Term.Try => () }.nonEmpty

  /**
   * Whether `body` is simple: a name, a path of names (`ExitCode.Success`) or a literal, also as the only statement of
   * a block without comments.
   */
  def isSimple(ctx: RenderContext, body: Term): Boolean =
    unwrapped(ctx, body) match {
      case _: Lit               => true
      case path if isPath(path) => true
      case _                    => false
    }

  /**
   * The only statement of `body` when it is a block of one statement without comments, recursively; `body` itself
   * otherwise.
   */
  def unwrapped(ctx: RenderContext, body: Term): Term =
    body match {
      case Term.Block((stat: Term) :: Nil) if ctx.commentsBetween(body.pos.start, body.pos.end).isEmpty =>
        unwrapped(ctx, stat)
      case _ =>
        body
    }

  private def isPath(term: Term): Boolean =
    term match {
      case _: Term.Name         => true
      case Term.Select(qual, _) => isPath(qual)
      case _                    => false
    }

  /**
   * Whether the author wrote `body`, after a separator ending at `separatorEnd`, below the separator. For a body in
   * braces it is whether its first statement starts a line after the `{`, so it keeps its place when the braces go.
   */
  def writtenBelow(ctx: RenderContext, separatorEnd: Int, body: Term): Boolean =
    val start =
      body match {
        case block: Term.Block if ctx.writtenWithBraces(block) =>
          block
            .stats
            .headOption
            .fold(ctx.span(block)._1)(first => ctx.span(first)._1)
        case _ =>
          ctx.span(body)._1
      }

    ctx.slice(separatorEnd, start).contains('\n')

  /**
   * The layout of `body`, which follows a separator ending at `separatorEnd`, at the indent it takes below the
   * separator.
   */
  def of(ctx: RenderContext, terms: TermRenderer, separatorEnd: Int, body: Term, indent: Int): BodyLayout =
    of(ctx, terms, separatorEnd, body, indent, None)

  /**
   * The layout of the body of `construct`, whose brace style decides whether a block of several statements keeps (or
   * gets) its braces, opened on the separator line.
   */
   def of(
     ctx: RenderContext,
     terms: TermRenderer,
     separatorEnd: Int,
     body: Term,
     indent: Int,
     construct: Option[Construct],
   ): BodyLayout =
     val bodyStart                   = ctx.span(body)._1
     val authorBreak                 = ctx.slice(separatorEnd, bodyStart).contains('\n')
     val found                       = ctx.commentsBetween(separatorEnd, bodyStart).toList
     val comments                    = ctx.commentDocs(found)
     val (onSeparatorLine, ownLines) = found.partition(c => !ctx.slice(separatorEnd, c.start).contains('\n'))
     val onLine                      = terms.renderBody(body, indent, forceExplode = authorBreak)

     // A `{ case ... }` opens on the separator line, its cases one level in.
     val braced =
       body match {
         case pf: Term.PartialFunction if !ctx.isParenthesized(pf) && comments.isEmpty =>
           Some(terms.render(pf, indent - 2, forceExplode = false))
         case block: Term.Block if comments.isEmpty && construct.exists(bracesBlock(ctx, _, block)) =>
           construct.map(terms.bracedBlock(block, indent, _))
         case _ if comments.isEmpty =>
           construct.flatMap(singleExprBraced(ctx, terms, _, body, indent))
         case _ =>
           None
       }

     val trailing = Doc.concat(ctx.commentDocs(onSeparatorLine).map(Doc.text(" ") ++ _))

     new BodyLayout(
       authorBreak,
       trailing = trailing,
       leading = ctx.commentDocs(ownLines),
       hasComments = comments.nonEmpty,
       onLine = onLine,
       braced = braced,
       renderBelow = () => if authorBreak then onLine else terms.renderBody(body, indent, forceExplode = true),
       simple = isSimple(ctx, body),
     )

   // A body that is a single expression in braces by `braces.single-expr`: with keep when the author wrote the braces,
   // with force-braces when it renders over several lines. The braces open on the separator line, the expression one
   // level in.
   private def singleExprBraced(
     ctx: RenderContext,
     terms: TermRenderer,
     construct: Construct,
     body: Term,
     indent: Int,
   ): Option[Doc] =
     val expression = singleExpression(ctx, body)
     val inBraces   = (term: Term) => Doc.braced(terms.renderBody(term, indent, forceExplode = false))

     expression.flatMap { term =>
       ctx.config.braces.singleExpr(construct) match {
         case BracePolicy.Keep if body.is[Term.Block] =>
           Some(inBraces(term))
         case BracePolicy.ForceBraces if !Render.isFlat(terms.renderBody(term, indent, forceExplode = false)) =>
           Some(inBraces(term))
         case _ =>
           None
       }
     }

   // The expression of a body that is a single expression: the only statement of a block in braces, written by the
   // author without parentheses around it, or the body itself when it is not a block.
   private def singleExpression(ctx: RenderContext, body: Term): Option[Term] =
     body match {
       case block @ Term.Block((stat: Term) :: Nil) if ctx.writtenWithBraces(block) && !ctx.isParenthesized(block) =>
         Some(stat).filterNot(_.is[Term.Block])
       case _: Term.Block =>
         None
       case term =>
         Some(term)
     }

   // A block of several statements (or of a single definition) written with braces keeps them, and one without gets
   // them, when the brace style uses braces.
   private def bracesBlock(ctx: RenderContext, construct: Construct, block: Term.Block): Boolean =
     val statements =
       block.stats.filterNot(_.is[Term.EndMarker]) match {
         case (_: Term) :: Nil => false
         case Nil              => false
         case _                => true
       }

     val notParenthesised = !ctx.isParenthesized(block)
     val writtenBraced    = ctx.writtenWithBraces(block)

     statements && notParenthesised && ctx.usesBraces(construct, writtenBraced)

  /**
   * The end offset of a lambda's `=>`, or the start of its body if not found.
   */
  def arrowEnd(ctx: RenderContext, fn: Term.Function): Int =
    val bodyStart = ctx.span(fn.body)._1
    ctx
      .arrowBefore(fn.pos.start, bodyStart)
      .map(_.end)
      .getOrElse(bodyStart)

}
