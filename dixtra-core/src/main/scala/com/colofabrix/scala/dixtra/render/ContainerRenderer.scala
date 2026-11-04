package com.colofabrix.scala.dixtra.render

import com.colofabrix.scala.dixtra.render.context.RenderContext
import com.colofabrix.scala.dixtra.render.definition.*
import com.colofabrix.scala.dixtra.verify.Equivalence
import scala.meta.*
import scala.meta.tokens.Token

/**
 * Renders containers (`object` / `class` / `trait`).
 *
 * The header of a class or trait is laid out by [[ContainerHeader]]; the rest of the header, the braces and the
 * surrounding whitespace are preserved, and each member statement is formatted in place at its own source column. A
 * container whose new header does not verify keeps its original header.
 *
 * The blank lines at the edges of the body follow `blank-lines.body-edges` for the container's construct (see
 * `EdgeBlankLines`); a braceless (Scala 3 `:`) body has no closing edge.
 *
 * @param ctx The shared render context.
 * @param signatures The signature renderer, for constructor parameters.
 * @param renderRegion Renders the members found in a region of the source (supplied by the router to break the
 *                     recursion cycle).
 */
final class ContainerRenderer(
  ctx: RenderContext,
  signatures: SignatureRenderer,
  renderRegion: (Int, Int, List[Stat]) => String,
) {

  private val headers =
    new ContainerHeader(ctx, signatures)

  /**
   * Render a container: its header, its formatted members, and its padding.
   */
  def render(container: Tree, templ: Template): String =
    val stats = templ.body.stats
    val brace = bodyOpenBrace(container, templ)

    val (rendered, header) =
      headers.render(container, templ, brace) match {
        case Some(header) if header.text != ctx.slice(container.pos.start, header.end) =>
          val members  = renderRegion(header.end, container.pos.end, stats)
          val rendered = withPadding(header.text + members, container, templ, brace, Some(header))
          if Equivalence.isEquivalentStat(container, rendered) then
            (rendered, Some(header))
          else
            (plain(container, templ, brace), None)
        case _ =>
          (plain(container, templ, brace), None)
      }

    restyled(container, templ, brace, rendered, header)
      .filter(Equivalence.isEquivalentStat(container, _))
      .getOrElse(rendered)

  // The rendered container with the brace style of its construct applied: a braceless body (`object A:`, `given A: B
  // with`) gets braces, or a braced one loses them. Only a body with members, without a self type, whose opener ends
  // the header line and whose "}" stands on its own line, is restyled.
  private def restyled(
    container: Tree,
    templ: Template,
    brace: Option[Token],
    rendered: String,
    header: Option[Header],
  ): Option[String] =
    // The offset in `rendered` of the original offset `at`, after the header.
    val offset =
      (at: Int) =>
        header.fold(at - container.pos.start)(h => h.text.length + (at - h.end))

    val first =
      templ
        .body
        .stats
        .headOption

    val wanted = Constructs.of(container).map(ctx.usesBraces(_, brace.isDefined))

    val hasBody = first.isDefined
    val noSelf =
      templ
        .body
        .selfOpt
        .isEmpty

    val noEarly     = templ.earlyClause.isEmpty
    val wantsChange = wanted.exists(_ != brace.isDefined)

    val eligible = hasBody && noSelf && noEarly && wantsChange

    (brace, first) match {
      case _ if !eligible           => None
      case (None, Some(stat))       => addBraces(container, stat, rendered, offset)
      case (Some(open), Some(stat)) => removeBraces(container, stat, open, rendered, offset)
      case _                        => None
    }

  // A braceless body (`object A:`, `given A: B with`) put in braces.
  private def addBraces(container: Tree, first: Stat, rendered: String, offset: Int => Int): Option[String] =
    val isGiven = container.is[Defn.Given]
    val opener =
      ctx
        .tokensBetween(container.pos.start, first.pos.start)
        .filterNot(_.is[Token.Whitespace])
        .lastOption

    def noCommentsBetween(from: Int, to: Int): Boolean =
      ctx.commentsBetween(from, to).isEmpty

    opener.collect {
      case colon: Token.Colon if !isGiven && noCommentsBetween(colon.end, first.pos.start) =>
        val at     = offset(colon.start)
        val head   = rendered.take(at).stripTrailing + " {"
        val body   = rendered.drop(at + 1) + "\n" + closing(container)
        val openIx = head.count(_ == '\n')
        edgeBlanks(container, head + body, openIx)
      case kw: Token.KwWith if isGiven && noCommentsBetween(kw.end, first.pos.start) =>
        val at     = offset(kw.end)
        val head   = rendered.take(at) + " {"
        val body   = rendered.drop(at) + "\n" + closing(container)
        val openIx = head.count(_ == '\n')
        edgeBlanks(container, head + body, openIx)
    }

  // A braced body made braceless, when `{` ends the header line and `}` stands on its own line.
  private def removeBraces(
    container: Tree,
    first: Stat,
    open: Token,
    rendered: String,
    offset: Int => Int,
  ): Option[String] =
    val at     = offset(open.start)
    val before = rendered.take(at).stripTrailing
    val lines =
      rendered
        .drop(at + 1)
        .split("\n", -1)
        .toList

    val opensLine  = lines.headOption.exists(_.isBlank) && ctx.commentsBetween(open.end, first.pos.start).isEmpty
    val closesLine = lines.lastOption.exists(_.trim == "}")

    val keyword =
      if container.is[Defn.Given] then
        Option.when(before.endsWith("with"))("")
      else
        Some(":")

    keyword
      .filter(_ => opensLine && closesLine)
      .map(colon => before + colon + lines.dropRight(1).mkString("\n").stripTrailing)
      .map(braceless => leadingBlanks(container, braceless, before.count(_ == '\n')))

  // The closing `}` of a container, on its own line at the container's indent.
  private def closing(container: Tree): String =
    " " * ctx.lineIndentOf(container.pos.start) + "}"

  // The container with its header kept as written.
  private def plain(container: Tree, templ: Template, brace: Option[Token]): String =
    withPadding(renderRegion(container.pos.start, container.pos.end, templ.body.stats), container, templ, brace, None)

  // Apply the edge blank-line rule to a rendered container whose `{` (if braced) is the one found in the source, or
  // whose braceless body opens with the `:` or `with` ending a line; `header` replaces the source up to its end offset.
  // A self type (`self: A =>`) belongs to the opening line.
  private def withPadding(
    rendered: String,
    container: Tree,
    templ: Template,
    brace: Option[Token],
    header: Option[Header],
  ): String =
    def linesBefore(offset: Int): Int =
      header match {
        case Some(h) if h.end <= offset => (h.text + ctx.slice(h.end, offset)).count(_ == '\n')
        case _                          => ctx.slice(container.pos.start, offset).count(_ == '\n')
      }

    val selfArrow =
      templ
        .body
        .selfOpt
        .flatMap { self =>
          ctx.tokensBetween(self.pos.start, container.pos.end).collectFirst { case t: Token.RightArrow => t }
        }

    brace match {
      case Some(open) =>
        edgeBlanks(container, rendered, linesBefore(selfArrow.getOrElse(open).start))
      case None =>
        def opensBody(opener: Token): Boolean =
          opener.is[Token.Colon] || opener.is[Token.KwWith]

        def lineEndsWithOpener(at: Int): Boolean =
          rendered
            .split("\n", -1)
            .lift(at)
            .exists(line => line.endsWith(":") || line.endsWith("with"))

        templ
          .body
          .stats
          .headOption
          .flatMap(first => ctx.previousSignificant(first.pos.start))
          .filter(opensBody)
          .map(opener => linesBefore(opener.start))
          .filter(lineEndsWithOpener)
          .map(leadingBlanks(container, rendered, _))
          .getOrElse(rendered)
    }

  // The `{` that opens the template body, if the container has a braced body whose closing `}` ends the container.
  private def bodyOpenBrace(container: Tree, templ: Template): Option[Token] =
    val bodyEnd =
      templ
        .body
        .stats
        .headOption
        .map(_.pos.start)
        .getOrElse(container.pos.end)

    val headerEnd =
      (templ.inits.map(_.pos.end) ++ List(templ.pos.start)).maxOption.getOrElse(templ.pos.start)

    val closesContainer =
      ctx
        .slice(container.pos.start, container.pos.end)
        .stripTrailing
        .endsWith("}")

    ctx
      .tokensBetween(headerEnd, bodyEnd)
      .collectFirst { case t: Token.LeftBrace => t }
      .filter(_ => closesContainer)

  // The blank lines at the edges of a braced container body, whose `{` ends line `openIdx` of `rendered`, by the policy
  // of the container's construct.
  private def edgeBlanks(container: Tree, rendered: String, openIdx: Int): String =
    Constructs
      .of(container)
      .map { construct =>
        EdgeBlankLines.braced(
          rendered,
          openIdx,
          ctx.config.blankLines(construct),
          ctx.config.blankLines.beforeScaladoc,
        )
      }
      .getOrElse(rendered)

  // The blank line at the top of a braceless container body, opened by the end of line `openIdx` of `rendered`, by the
  // policy of the container's construct.
  private def leadingBlanks(container: Tree, rendered: String, openIdx: Int): String =
    Constructs
      .of(container)
      .map { construct =>
        EdgeBlankLines.leading(
          rendered,
          openIdx,
          ctx.config.blankLines(construct),
          ctx.config.blankLines.beforeScaladoc,
        )
      }
      .getOrElse(rendered)

}
