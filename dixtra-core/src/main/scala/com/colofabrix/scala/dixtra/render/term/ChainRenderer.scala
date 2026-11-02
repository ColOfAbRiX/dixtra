package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*

/**
 * Renders select chains: dotted accesses and calls (`x.member`, `x.member(args)`, `x.member(a)(b) { ... }`); a bare
 * `List(args)` is not a chain.
 *
 * A chain stays inline unless a trigger fires, else the receiver stays on its own line with every `.member` selection
 * dropped below it, one per line (D2). Each exploded segment is re-rendered through the term rules at its new indent,
 * so a fitting lambda stays inline and a multiline block re-indents (D3, D4). A chain with a single selection never
 * breaks at its dot: it stays where it was written, with a newline inside its argument block hanging off the sole
 * `.member` (D7). A path of lowercase names ending at the first capitalised one (`fs2.Stream`, `config.Defaults`) that
 * starts the chain is its receiver: it never breaks and its selections do not count, unless the author broke a line
 * inside it (D10). With `selector-chain.object-member` the first member of a capitalised receiver (`IO.pure(x)`) joins
 * it too, and so does the first member of a receiver name shorter than `selector-chain.receiver-min-length` (`v.trim`),
 * which still counts as a selection. In the enumerators of a `for` and in the arguments of a call (or the elements of a
 * tuple), a chain does not break at `selector-chain.break-at-count` selections; with `selector-chain.in-for` /
 * `selector-chain.in-call` set to `never-break`, a chain the author broke there is joined while it fits.
 *
 * Comments before a dot are re-attached (trailing the previous segment, or on their own lines); a comment after a dot
 * or between a member and its arguments keeps the chain verbatim.
 *
 * @param ctx The shared render context.
 * @param terms The term renderer, for segments that are not selections.
 * @param applications The application renderer, for receivers and arguments.
 */
private[term] final class ChainRenderer(ctx: RenderContext, terms: TermRenderer, applications: ApplicationRenderer) {

  /**
   * True when the term is a select chain with at least one selection.
   */
  def isChain(term: Term): Boolean =
    flatten(term)._2.nonEmpty

  /**
   * Render a select chain starting at `indent`; Unless it is part of an expression (see `inExpression`), it also breaks
   * by `selector-chain.break-at-count`.
   */
  def render(term: Term, indent: Int, forceExplode: Boolean, asBody: Boolean): Doc =
    val (start, all) = flatten(term)
    val head         = chainHead(start, all)

    val pathEnd =
      if head.path > 0 then
        all
          .drop(head.path - 1)
          .headOption
          .getOrElse(start)
      else
        start

    val member     = if head.member then all.lift(head.path) else None
    val base       = member.getOrElse(pathEnd)
    val selections = all.drop(head.path + member.size)

    if (member.toList ++ selections).exists(hasUnattachableComments) then
      ctx.ownSourceDoc(term)
    else
      val headDoc =
        if head.path > 0 || member.isDefined then
          pathDoc(pathEnd)
        else
          applications.operand(start, indent)

      val memberDoc = member.fold(Doc.Empty)(segment(_, indent, headDoc))
      val baseDoc   = headDoc ++ memberDoc
      val comments  = selections.map(commentsBeforeDot)

      lazy val inline =
        selections
          .zip(comments)
          .foldLeft(baseDoc) { case (before, (selection, comment)) =>
            val onLine = before ++ comment.trailing

            onLine ++ segment(selection, indent, onLine)
          }

      val chain            = ctx.config.selectorChain
      val call             = inCall(term)
      val statement        = asBody || !inExpression(term)
      val neverBreakInFor  = chain.inFor == NestedChainPolicy.NeverBreak && inEnumerator(term)
      val neverBreakInCall = chain.inCall == NestedChainPolicy.NeverBreak && call
      val joins            = !comments.exists(_.forcesBreak) && (neverBreakInFor || neverBreakInCall)

      if joins && ctx.fits(inline, indent) then
        inline
      else if !explodes(term, base, selections, head.counted, comments, indent, forceExplode, statement && !call) then
        inline
      else
        val selectionDocs =
          selections
            .zip(comments)
            .map { (selection, comment) =>
              val leading  = comment.trailing ++ Doc.concat(comment.ownLines.map(Doc.Line ++ _))
              val selected = Doc.Line ++ segment(selection, indent + 2, Doc.Empty)

              leading ++ selected
            }

        baseDoc ++ Doc.Nest(2, Doc.concat(selectionDocs))

  // Whether the term is part of an expression, where a chain does not break at `selector-chain.brak-at-count`: an
  // operand of an operation, as argument, a tuple element, an enumerator, a condition, a scrutinee, the target of an
  // assignment, a qualifier, or an ascribed, unary, interpolated or default value. Anywhere else, as a body or a
  // statement of a block, at any position, it is not.
  private def inExpression(tree: Tree): Boolean =
    tree
      .parent
      .exists {
        case _: Enumerator       => true
        case _: Term.Annotate    => true
        case _: Term.Apply       => true
        case _: Term.ApplyInfix  => true
        case _: Term.ApplyType   => true
        case _: Term.ApplyUnary  => true
        case _: Term.ArgClause   => true
        case _: Term.Ascribe     => true
        case _: Term.Eta         => true
        case _: Term.Interpolate => true
        case _: Term.Param       => true
        case _: Term.Repeated    => true
        case _: Term.Select      => true
        case _: Term.Tuple       => true
        case parent: Case        => parent.cond.exists(_ eq tree)
        case parent: Term.Assign => parent.lhs eq tree
        case parent: Term.If     => parent.cond eq tree
        case parent: Term.Match  => parent.expr eq tree
        case parent: Term.While  => parent.expr eq tree
        case _                   => false
      }

  // Whether the term is in the enumerators of a `for`: a generator, a guard or a value definition, at any depth.
  private def inEnumerator(tree: Tree): Boolean =
    tree
      .parent
      .exists {
        case _: Enumerator => true
        case parent        => inEnumerator(parent)
      }

  // Whether the term is in the arguments of a call or of a constructor, positional or named, in the elements of a
  // tuple, or in the arguments of an infix application with several of them (`x shouldBe (a, b)`). A body inside them,
  // as the body of a lambda or of a block, is not: it breaks at `selector-chain.break-at-count` selections as any body.
  private def inCall(tree: Tree): Boolean =
    tree
      .parent
      .exists {
        case args: Term.ArgClause =>
          args
            .parent
            .exists {
              case _: Term.Apply | _: Init => true
              case _: Term.ApplyInfix      => args.values.sizeIs > 1
              case _                       => false
            }
        case _: Term.Tuple =>
          true
        case _: Term.Function | _: Term.ContextFunction | _: Term.PolyFunction | _: Term.Block |
            _: Term.PartialFunction | _: Case | _: Defn | _: Template | _: Term.If | _: Term.Try | _: Term.While |
            _: Term.For | _: Term.ForYield =>
          false
        case parent =>
          inCall(parent)
      }

  // The start of a chain kept together as its receiver: `path` selections of a namespace path, and with `member` the
  // selection after it too. `counted` of them still count towards `selector-chain.break-at-count`.
  private final case class ChainHead(path: Int, member: Boolean, counted: Int)

  // The receiver of a chain (D10): a namespace path (`java.nio.file.Files`) by `selector-chain.namespace-path`, then
  // the first member of the capitalised name that ends it, or of a capitalised root (`IO.pure(x)`), by
  // `selector-chain.object-member`; or the first member of a root name shorter than
  // `selector-chain.receiver-min-length`, unless the author broke the line before its dot.
  private def chainHead(start: Term, selections: List[Term]): ChainHead =
    val chain = ctx.config.selectorChain
    val path  = namespacePath(start, selections, chain.namespacePath)

    val (capitalRoot, shortRoot) =
      start match {
        case root: Term.Name if !ctx.isParenthesized(root) =>
          (root.value.headOption.exists(_.isUpper), root.value.length < chain.receiverMinLength)
        case _ =>
          (false, false)
      }

    val joinsObject = selections.lift(path).exists(joinsHead(chain.objectMember, _))
    val joinsShort  = selections.headOption.exists(joinsHead(ChainHeadPolicy.Keep, _))

    val objectMember = (path > 0 || capitalRoot) && joinsObject
    val shortMember  = path == 0 && shortRoot && joinsShort

    ChainHead(path, objectMember || shortMember, if shortMember && !objectMember then 1 else 0)

  // How many selections belong to a path that starts the chain: plain lowercase names up to and including the first
  // capitalised one, as in `fs2.Stream`, `java.nio.file.Files` or `config.Defaults`. 0 when there is none, when it
  // holds a comment, or when the author broke a line inside it and the policy is `keep` (then the whole chain breaks as
  // usual).
  private def namespacePath(start: Term, selections: List[Term], policy: ChainHeadPolicy): Int =
    // A plain lowercase root, kept where it is written.
    def plainRoot(root: Term.Name): Boolean =
      policy != ChainHeadPolicy.AlwaysBreak && root.value.headOption.exists(_.isLower) && !ctx.isParenthesized(root)

    start match {
      case root: Term.Name if plainRoot(root) =>
        val names =
          selections
            .map {
              case s @ Term.Select(_, name) if !ctx.isParenthesized(s) => Some(name.value)
              case _                                                   => None
            }
            .takeWhile(_.isDefined)
            .flatten

        val capital = names.indexWhere(_.headOption.exists(_.isUpper))

        val end =
          selections
            .drop(capital)
            .headOption
            .map(_.pos.end)
            .getOrElse(root.pos.end)

        val broken     = ctx.slice(root.pos.start, end).contains('\n') && policy == ChainHeadPolicy.Keep
        val noComments = ctx.commentsBetween(root.pos.start, end).isEmpty

        if capital >= 0 && !broken && noComments then
          capital + 1
        else
          0
      case _ =>
        0
    }

  // Whether the first member of a capitalised name joins the chain's receiver: never with a comment before it, nor when
  // the author broke the line before its dot and the policy is `keep`.
  private def joinsHead(policy: ChainHeadPolicy, member: Term): Boolean =
    parts(member).exists { s =>
      val qualEnd = ctx.span(s.qual)._2
      val clean   = ctx.commentsBetween(qualEnd, s.name.pos.start).isEmpty
      policy match {
        case ChainHeadPolicy.AlwaysJoin  => clean
        case ChainHeadPolicy.Keep        => clean && !gapBeforeDot(member).contains('\n')
        case ChainHeadPolicy.AlwaysBreak => false
      }
    }

  // A package path on one line.
  private def pathDoc(path: Term): Doc =
    Doc.text(ctx.slice(path.pos.start, path.pos.end).filterNot(_.isWhitespace))

  // The explode triggers (D2): a break before a dot or a comment that forces it; with two or more selections also an
  // overflow or any newline after the receiver (a newline inside the receiver itself, e.g. a multiline string literal,
  // does not count); for a chain that is a body or a statement (`asBody`), with `selector-chain.break-at-count` or more
  // selections (`counted` more from the receiver) also an author signal from the parent (a newline after `=` or `=>`),
  // or nothing with `selector-chain.inline-chains = break-at-count`. A chain inside an operation, an argument or a condition does not count.
  private def explodes(
    term: Term,
    base: Term,
    selections: List[Term],
    counted: Int,
    comments: List[DotComments],
    indent: Int,
    forceExplode: Boolean,
    asBody: Boolean,
  ): Boolean =
    val breakBeforeDot  = selections.exists(sel => gapBeforeDot(sel).contains('\n'))
    val commentBreak    = comments.exists(_.forcesBreak)
    val internalNewline = ctx.slice(ctx.span(base)._2, term.pos.end).contains('\n')
    val overflow        = ctx.overflows(ctx.ownSourceDoc(term), indent)
    val authorBreak     = breakBeforeDot || commentBreak
    val tooLong         = selections.sizeIs >= 2 && (overflow || internalNewline)
    val overBudget      = selections.size + counted >= ctx.config.selectorChain.breakAtCount
    val alwaysBreak     = forceExplode || ctx.config.selectorChain.inlineChains == InlineChainPolicy.BreakAtCount
    authorBreak || tooLong || (asBody && overBudget && alwaysBreak)

  // The comments between a selection's qualifier and its dot: those on the qualifier's line trail the previous segment,
  // the others get their own lines.
  private final case class DotComments(trailing: Doc, ownLines: List[Doc], forcesBreak: Boolean)

  private def commentsBeforeDot(selection: Term): DotComments =
    parts(selection) match {
      case None =>
        DotComments(Doc.Empty, Nil, forcesBreak = false)
      case Some(Selection(qual, name, _, _)) =>
        val qualEnd = ctx.span(qual)._2

        val dotStart =
          ctx
            .dotBefore(qualEnd, name.pos.start)
            .map(_.start)
            .getOrElse(name.pos.start)

        val comments    = ctx.commentsBetween(qualEnd, dotStart).toList
        val (same, own) = comments.partition(c => !ctx.slice(qualEnd, c.start).contains('\n'))
        val trailing    = Doc.concat(ctx.commentDocs(same).map(Doc.text(" ") ++ _))
        val lineComment = same.exists(_.text.startsWith("//"))

        DotComments(trailing, ctx.commentDocs(own), lineComment || own.nonEmpty)
    }

  // A selection whose comments cannot be re-attached: between the dot and the member name, or between a member and its
  // argument clause.
  private def hasUnattachableComments(selection: Term): Boolean =
    def afterDot(qual: Term, name: Term.Name): Boolean =
      val qualEnd = ctx.span(qual)._2

      val dotEnd =
        ctx
          .dotBefore(qualEnd, name.pos.start)
          .map(_.end)
          .getOrElse(qualEnd)

      ctx.commentsBetween(dotEnd, name.pos.start).nonEmpty

    selection match {
      case app @ Term.Apply.After_4_6_0(fun, _)     => ctx.hasGapComments(app) || hasUnattachableComments(fun)
      case app @ Term.ApplyType.After_4_6_0(fun, _) => ctx.hasGapComments(app) || hasUnattachableComments(fun)
      case Term.Select(qual, name)                  => afterDot(qual, name)
      case other                                    => ctx.hasGapComments(other)
    }

  // The source text between a selection's qualifier and its member name (the dot region). A newline here means the
  // author broke the chain before that dot.
  private def gapBeforeDot(selection: Term): String =
    parts(selection).fold("") { s => ctx.slice(ctx.span(s.qual)._2, s.name.pos.start) }

  // One selection at the given indent, after `before` on its line, its argument clauses deciding their layout from the
  // column where they start.
  private def segment(selection: Term, indent: Int, before: Doc): Doc =
    parts(selection) match {
      case Some(Selection(qual, name, memberEnd, clauses)) =>
        val member     = memberDoc(qual, name, memberEnd)
        val clausesDoc = applications.argumentClauses(clauses, indent, ctx.endColumn(before ++ member, indent))
        member ++ clausesDoc
      case None =>
        terms.render(selection, indent, forceExplode = false)
    }

  // A selection: the qualifier before its dot, the member name, the end of the member with its type arguments
  // (`.as[Int]`), and its argument clauses in order.
  private final case class Selection(qual: Term, name: Term.Name, memberEnd: Int, clauses: List[Term.ArgClause])

  // A selection split into its parts. None when the term is not a selection or a parenthesised part makes it opaque; a
  // parenthesised qualifier is not (`(0 to n).map(f)` selects `map` on `(0 to n)`).
  private def parts(term: Term): Option[Selection] =
    term match {
      case Term.Select(qual, name) =>
        Some(Selection(qual, name, name.pos.end, Nil))
      case Term.ApplyType.After_4_6_0(fun, targs) if !ctx.isParenthesized(fun) =>
        parts(fun).collect { case s @ Selection(_, _, _, Nil) => s.copy(memberEnd = targs.pos.end) }
      case Term.Apply.After_4_6_0(fun, clause) if !ctx.isParenthesized(fun) =>
        parts(fun).map(s => s.copy(clauses = s.clauses :+ clause))
      case _ =>
        None
    }

  // The `.member[T]` slice of a selection, from its dot to `memberEnd`, so neither whitespace before the dot nor the
  // qualifier's closing parenthesis is included.
  private def memberDoc(qual: Term, name: Term.Name, memberEnd: Int): Doc =
    val qualEnd = ctx.span(qual)._2
    ctx.dotBefore(qualEnd, name.pos.start) match {
      case Some(_) => Doc.text(".") ++ ctx.sourceDoc(name.pos.start, memberEnd) // a trailing dot joins its member
      case None    => ctx.sourceDoc(qualEnd, memberEnd)
    }

  // A chain as its base receiver and its selections, outermost last. A parenthesised qualifier is an opaque base: the
  // chain never reaches inside it.
  private def flatten(term: Term): (Term, List[Term]) =
    @annotation.tailrec
    def loop(t: Term, acc: List[Term]): (Term, List[Term]) =
      parts(t) match {
        case Some(s) if ctx.isParenthesized(s.qual) => (s.qual, t :: acc)
        case Some(s)                                => loop(s.qual, t :: acc)
        case None                                   => (t, acc)
      }
    loop(term, Nil)

}
