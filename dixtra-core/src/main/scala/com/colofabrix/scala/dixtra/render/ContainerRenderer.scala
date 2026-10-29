package com.colofabrix.scala.dixtra.render

import com.colofabrix.scala.dixtra.Construct
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.render.context.RenderContext
import com.colofabrix.scala.dixtra.render.definition.*
import com.colofabrix.scala.dixtra.verify.Equivalence
import scala.meta.*
import scala.meta.tokens.Token

/**
 * Renders containers (`object` / `class` / `trait`).
 *
 * The constructor parameters of a class or trait follow the signature rules of a `def` (see
 * [[com.colofabrix.scala.dixtra.render.definition.SignatureRenderer]]), with the rest of the header line (e.g. `
 * extends A {`) counting towards the width; the rest of the header, the braces and the surrounding whitespace are
 * preserved, and each member statement is formatted in place at its own source column. A container whose new header
 * does not verify keeps its original header.
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

  /**
   * Render a container: its header, its formatted members, and its padding.
   */
  def render(container: Tree, templ: Template): String =
    val stats = templ.body.stats
    val brace = bodyOpenBrace(container, templ)

    val (rendered, header) =
      constructorHeader(container, templ, brace) match {
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

    opener.collect {
      case colon: Token.Colon if !isGiven && ctx.commentsBetween(colon.end, first.pos.start).isEmpty =>
        val at   = offset(colon.start)
        val head = rendered.take(at).stripTrailing + " {"
        edgeBlanks(container, head + rendered.drop(at + 1) + "\n" + closing(container), head.count(_ == '\n'))
      case kw: Token.KwWith if isGiven && ctx.commentsBetween(kw.end, first.pos.start).isEmpty =>
        val at   = offset(kw.end)
        val head = rendered.take(at) + " {"
        edgeBlanks(container, head + rendered.drop(at) + "\n" + closing(container), head.count(_ == '\n'))
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
    
    val keyword    = 
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

  // The rendered header of a container up to the end of its extends list or, without one, up to the end of its
  // constructor's last parameter clause, with the offset where the original header continues.
  private def constructorHeader(container: Tree, templ: Template, brace: Option[Token]): Option[Header] =
    val parts =
      container match {
        case c: Defn.Class if c.ctor.mods.isEmpty => Some(constructorParts(c, c.name, c.tparamClause, c.ctor))
        case t: Defn.Trait if t.ctor.mods.isEmpty => Some(constructorParts(t, t.name, t.tparamClause, t.ctor))
        case _                                    => None
      }

    val restEnd = brace.map(_.end).getOrElse(templ.body.stats.headOption.map(_.pos.start).getOrElse(container.pos.end))
    val indent  = ctx.lineIndentOf(container.pos.start)

    val signature =
      parts.filter(p => p.paramClauses.nonEmpty && !signatures.hasUnattachableComments(p, headEnd(p)))

    extendsList(container, templ) match {
      case Some(ext) =>
        withExtends(container, signature, ext, ctx.slice(ext.end, restEnd).takeWhile(_ != '\n').stripTrailing, indent)
      case None =>
        signature.flatMap { p =>
          val headerEnd = headEnd(p)

          val rest =
            ctx
              .slice(headerEnd, restEnd)
              .takeWhile(_ != '\n')
              .stripTrailing

          signatures.render(p, indent, Doc.text(rest)).map(s => Header(Render.render(s.doc, indent), headerEnd))
        }
    }

  // Where the part of the header before the extends list ends: after the last parameter clause, the type parameters or
  // the name.
  private def headEnd(parts: SignatureParts): Int =
    parts
      .paramClauses
      .map(_.pos.end)
      .foldLeft(math.max(parts.tparamClause.pos.end, parts.nameEnd))(math.max)

  // The `extends` / `with` / `derives` parts of a header, each with its keyword, when they can be laid out again:
  // written one per line or on one line, with no comments and no early definitions.
  private def extendsList(container: Tree, templ: Template): Option[ExtendsList] =
    val parents = templ.inits ++ templ.derives
    val first   = parents.headOption.map(_.pos.start)

    val keyword =
      first.flatMap { f =>
        ctx
          .tokensBetween(container.pos.start, f)
          .collect {
            case t: Token.KwExtends                     => t.start
            case t: Token.Ident if t.value == "derives" => t.start
          }
          .lastOption
      }

    val end   = parents.map(_.pos.end).maxOption
    val texts = templ.inits.map(i => ctx.slice(i.pos.start, i.pos.end))

    val comma =
      templ
        .inits
        .zip(templ.inits.drop(1))
        .exists { (a, b) =>
          ctx.tokensBetween(a.pos.end, b.pos.start).exists(_.is[Token.Comma])
        }

    val inits =
      texts match {
        case Nil          => Nil
        case all if comma => List("extends " + all.mkString(", "))
        case head :: rest => ("extends " + head) :: rest.map("with " + _)
      }

    val derives =
      if templ.derives.isEmpty then 
        Nil
      else 
        List("derives " + templ.derives.map(d => ctx.slice(d.pos.start, d.pos.end)).mkString(", "))

    for
      start <- keyword
      stop  <- end
               if templ.earlyClause.isEmpty
               if ctx.commentsBetween(start, stop).isEmpty
               if !(texts ++ derives).exists(_.contains('\n'))
    yield
      ExtendsList(inits ++ derives, start, stop)

  // The header with its extends list, trying in order: everything on one line; the parameters on one line and each
  // extends part on its own line at +2; the parameters exploded and the extends list on the `)` line; both broken, with
  // the first extends part on the `)` line and the others below it at +2. A break written inside the extends list skips
  // the one-line layout.
  private def withExtends(
    container: Tree,
    signature: Option[SignatureParts],
    ext: ExtendsList,
    tail: String,
    indent: Int,
  ): Option[Header] =
    val oneLine = " " + ext.items.mkString(" ")
    val before  = ctx.slice(container.pos.start, ext.start)

    val broken =
      before.reverse.takeWhile(_.isWhitespace).contains('\n') || ctx.slice(ext.start, ext.end).contains('\n')

    val doc =
      signature match {
        case None =>
          Option.when(!before.trim.contains('\n')) {
            val prefix = Doc.text(before.trim)
            if !broken && fits(indent + before.trim.length + oneLine.length + tail.length) then
              prefix ++ Doc.text(oneLine)
            else
              prefix ++ below(ext)
          }
        case Some(p) =>
          signatureWithExtends(p, ext, oneLine, tail, broken, indent)
      }

    doc.map(d => Header(Render.render(d, indent), ext.end))

  // The layouts of `withExtends` for a header with a signature, in order.
  private def signatureWithExtends(
    p: SignatureParts,
    ext: ExtendsList,
    oneLine: String,
    tail: String,
    broken: Boolean,
    indent: Int,
  ): Option[Doc] =
    val withRest = signatures.render(p, indent, Doc.text(oneLine + tail))
    val alone    = signatures.render(p, indent, Doc.Empty)

    val partsFit =
      ext
        .items
        .zipWithIndex
        .forall { (item, i) =>
          fits(indent + 2 + item.length + (if i == ext.items.length - 1 then tail.length else 0))
        }

    val singleLine = 
      (doc: Doc) => !Render.render(doc, indent).contains('\n')

    def allOnOneLine =
      withRest.filter(s => !broken && singleLine(s.doc)).map(_.doc ++ Doc.text(oneLine))

    def partsBelow =
      alone.filter(s => singleLine(s.doc) && partsFit).map(_.doc ++ below(ext))

    def onClosingLine =
      withRest
        .filter(s => !singleLine(s.doc) && fits(s.lastLineWidth + oneLine.length + tail.length))
        .map(_.doc ++ Doc.text(oneLine))

    def bothBroken =
      ext.items match {
        case first :: rest if rest.nonEmpty =>
          val restFit =
            rest
              .zipWithIndex
              .forall { (item, i) =>
                fits(indent + 2 + item.length + (if i == rest.length - 1 then tail.length else 0))
              }
          signatures
            .render(p, indent, Doc.text(" " + first))
            .filter(s => restFit && !singleLine(s.doc) && fits(s.lastLineWidth + 1 + first.length))
            .map(_.doc ++ Doc.text(" " + first) ++ below(rest))
        case _ =>
          None
      }

    allOnOneLine.orElse(partsBelow).orElse(onClosingLine).orElse(bothBroken).orElse(withRest.map(_.doc ++ below(ext)))

  // The extends parts each on its own line, two columns in.
  private def below(ext: ExtendsList): Doc =
    below(ext.items)

  private def below(items: List[String]): Doc =
    Doc.Nest(2, Doc.concat(items.map(item => Doc.Line ++ Doc.text(item))))

  private def fits(width: Int): Boolean =
    ctx.fitsWidth(0, width)

  // A case class whose parameters the author wrote over several lines keeps them one per line; a one-line one stays on
  // one line while it fits.
  private def constructorParts(
    container: Tree,
    name: Name,
    tparams: Type.ParamClause,
    ctor: Ctor.Primary,
  ): SignatureParts =
    val clauses = ctor.paramClauses.toList
    val isCase  = Constructs.of(container).contains(Construct.CaseClass)
    val written =
      (clauses.headOption, clauses.lastOption) match {
        case (Some(first), Some(last)) => ctx.slice(first.pos.start, last.pos.end).contains('\n')
        case _                         => false
      }
    SignatureParts(
      prefixStart = container.pos.start,
      nameEnd = name.pos.end,
      tparamClause = tparams,
      paramClauses = clauses,
      returnType = None,
      keepBroken = isCase && written,
    )

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
        .flatMap(
          self => ctx.tokensBetween(self.pos.start, container.pos.end).collectFirst { case t: Token.RightArrow => t },
        )
        
    brace match {
      case Some(open) =>
        edgeBlanks(container, rendered, linesBefore(selfArrow.getOrElse(open).start))
      case None =>
        templ
          .body
          .stats
          .headOption
          .flatMap(first => ctx.previousSignificant(first.pos.start))
          .filter(opener => opener.is[Token.Colon] || opener.is[Token.KwWith])
          .map(opener => linesBefore(opener.start))
          .filter(at => rendered.split("\n", -1).lift(at).exists(l => l.endsWith(":") || l.endsWith("with")))
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
      .map(
        construct =>
          EdgeBlankLines.braced(
            rendered,
            openIdx,
            ctx.config.blankLines(construct),
            ctx.config.blankLines.beforeScaladoc,
          ),
      )
      .getOrElse(rendered)

  // The blank line at the top of a braceless container body, opened by the end of line `openIdx` of `rendered`, by the
  // policy of the container's construct.
  private def leadingBlanks(container: Tree, rendered: String, openIdx: Int): String =
    Constructs
      .of(container)
      .map(
        construct =>
          EdgeBlankLines.leading(
            rendered,
            openIdx,
            ctx.config.blankLines(construct),
            ctx.config.blankLines.beforeScaladoc,
          ),
      )
      .getOrElse(rendered)

}

/**
 * A container header rendered anew.
 *
 * @param text The rendered header.
 * @param end Where the header it replaces ends in the source.
 */
private final case class Header(text: String, end: Int)

/**
 * The `extends` / `with` / `derives` parts of a container header.
 *
 * @param items Each part with its keyword (`extends A`, `with B`, `derives C`).
 * @param start Where the first keyword starts.
 * @param end Where the last part ends.
 */
private final case class ExtendsList(items: List[String], start: Int, end: Int)
