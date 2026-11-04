package com.colofabrix.scala.dixtra.render

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.model.Construct
import com.colofabrix.scala.dixtra.render.context.RenderContext
import com.colofabrix.scala.dixtra.render.definition.*
import scala.meta.*
import scala.meta.tokens.Token

/**
 * Renders the header of a class or trait: its constructor parameters, by the signature rules of a `def`, and its
 * `extends` / `with` / `derives` list, with the rest of the header line counting towards the width.
 *
 * @param ctx The shared render context.
 * @param signatures The signature renderer, for constructor parameters.
 */
final class ContainerHeader(ctx: RenderContext, signatures: SignatureRenderer) {

  /**
   * The rendered header of a container up to the end of its extends list or, without one, up to the end of its
   * constructor's last parameter clause, with the offset where the original header continues. `brace` is the `{` that
   * opens the body, if any.
   */
  def render(container: Tree, templ: Template, brace: Option[Token]): Option[Header] =
    val parts =
      container match {
        case cls: Defn.Class =>
          Some(constructorParts(cls, cls.name, cls.tparamClause, cls.ctor))
        case traitDef: Defn.Trait =>
          Some(constructorParts(traitDef, traitDef.name, traitDef.tparamClause, traitDef.ctor))
        case _ =>
          None
      }

    val indent = ctx.lineIndentOf(container.pos.start)

    val restEnd =
      brace
        .map(_.end)
        .getOrElse {
          templ
            .body
            .stats
            .headOption
            .map(_.pos.start)
            .getOrElse(container.pos.end)
        }

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

          signatures
            .render(p, indent, Doc.text(rest))
            .map(s => Header(Render.render(s.doc, indent), headerEnd))
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

    val noBreaks = !(texts ++ derives).exists(_.contains('\n'))

    for
      start <- keyword
      stop  <- end
               if templ.earlyClause.isEmpty
               if ctx.commentsBetween(start, stop).isEmpty
               if noBreaks
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

    allOnOneLine
      .orElse(partsBelow)
      .orElse(onClosingLine)
      .orElse(bothBroken)
      .orElse(withRest.map(_.doc ++ below(ext)))

  // The extends parts each on its own line, two columns in.
  private def below(ext: ExtendsList): Doc =
    below(ext.items)

  private def below(items: List[String]): Doc =
    Doc.Nest(2, Doc.concat(items.map(item => Doc.Line ++ Doc.text(item))))

  private def fits(width: Int): Boolean =
    ctx.fitsWidth(0, width)

  // A case class whose parameters the author wrote over several lines keeps them one per line; a one-line one stays on
  // one line while it fits. The constructor modifiers (`private`, `@Inject()`) are kept as written, one by one.
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

    val modifiers = ctor.mods.map(mod => ctx.slice(mod.pos.start, mod.pos.end))

    SignatureParts(
      prefixStart = container.pos.start,
      nameEnd = name.pos.end,
      afterName = "",
      tparamClause = tparams,
      constructorModifiers = modifiers,
      clauseOpenings = Nil,
      paramClauses = clauses,
      returnType = None,
      keepBroken = isCase && written,
    )

}

/**
 * A container header rendered anew.
 *
 * @param text The rendered header.
 * @param end Where the header it replaces ends in the source.
 */
private[render] final case class Header(text: String, end: Int)

/**
 * The `extends` / `with` / `derives` parts of a container header.
 *
 * @param items Each part with its keyword (`extends A`, `with B`, `derives C`).
 * @param start Where the first keyword starts.
 * @param end Where the last part ends.
 */
private final case class ExtendsList(items: List[String], start: Int, end: Int)
