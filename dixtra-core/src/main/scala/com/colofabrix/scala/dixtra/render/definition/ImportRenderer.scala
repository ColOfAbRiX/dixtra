package com.colofabrix.scala.dixtra.render.definition

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*

/**
 * Renders `import` and `export` clauses.
 *
 * The selectors keep their spelling (`_` / `*`, `=>` / `as`): only the spaces inside the braces, the braces around a
 * single selector (`braces.imports`) and the line breaks change. A clause with a single importer that does not fit packs
 * its selectors onto as few lines as fit; a clause with several importers, or with a comment, is never broken.
 *
 * @param ctx The shared render context.
 */
final class ImportRenderer(ctx: RenderContext) {

  /**
   * The `import` or `export` clause `stat` at `indent`, or None when `stat` is not one or has comments.
   */
  def render(stat: Stat, indent: Int): Option[Doc] =
    stat match {
      case clause: Import => renderClause("import", clause.importers, stat, indent)
      case clause: Export => renderClause("export", clause.importers, stat, indent)
      case _              => None
    }

  private def renderClause(keyword: String, importers: List[Importer], stat: Stat, indent: Int): Option[Doc] =
    val hasComments = ctx.commentsBetween(stat.pos.start, stat.pos.end).nonEmpty

    Option.when(!hasComments && importers.nonEmpty) {
      val parts   = importers.map(importerParts)
      val oneLine = keyword + " " + parts.map(oneLineText).mkString(", ")

      parts match {
        case (ref, selectors, true) :: Nil if !ctx.fitsWidth(indent, oneLine.length) =>
          packed(keyword + " " + ref + ".{", selectors, indent)
        case _ =>
          Doc.text(oneLine)
      }
    }

  // The path of an importer, its selectors as written, and whether they go in braces.
  private def importerParts(importer: Importer): (String, List[String], Boolean) =
    val ref       = ctx.slice(importer.ref.pos.start, importer.ref.pos.end)
    val selectors = importer.importees.map(i => ctx.slice(i.pos.start, i.pos.end))
    val written   = ctx.slice(importer.ref.pos.end, importer.pos.end).contains('{')

    (ref, selectors, braced(importer.importees, written))

  // Whether the selectors go in braces, by `braces.imports`: only a single selector that can stand without them changes.
  private def braced(importees: List[Importee], written: Boolean): Boolean =
    importees match {
      case single :: Nil =>
        ctx.config.braces.imports match {
          case ImportBraces.Keep        => written
          case ImportBraces.ForceBraces => true
          case ImportBraces.AvoidBraces => written && !standsWithoutBraces(single)
        }
      case _ =>
        true
    }

  // A selector that is valid without braces in every dialect that accepts it. A rename keeps its braces: `B => C`
  // needs them, and `B as C` without them may not be accepted by Scala 2 with `-Xsource:3`.
  private def standsWithoutBraces(importee: Importee): Boolean =
    importee match {
      case _: Importee.Name     => true
      case _: Importee.Wildcard => true
      case _: Importee.GivenAll => true
      case _                    => false
    }

  private def oneLineText(parts: (String, List[String], Boolean)): String =
    val (ref, selectors, inBraces) = parts
    if inBraces then ref + ".{ " + selectors.mkString(", ") + " }" else ref + "." + selectors.mkString(", ")

  // `header` followed by the selectors packed onto as few lines as fit one level in, and `}` on its own line.
  private def packed(header: String, selectors: List[String], indent: Int): Doc =
    val inner    = indent + 2
    val trailing = if ctx.config.trailingCommas == TrailingCommas.Always then "," else ""
    val last     = selectors.length - 1

    val items =
      selectors
        .zipWithIndex
        .map { (selector, i) =>
          if i == last then selector + trailing else selector + ","
        }

    val lines = packLines(items, inner)
    val body  = Doc.intercalate(Doc.Line, lines.map(Doc.text))

    Doc.text(header) ++ Doc.Nest(2, Doc.Line ++ body) ++ Doc.Line ++ Doc.text("}")

  // The items joined by spaces onto lines that fit from `column`; an item too long for any line stands alone.
  private def packLines(items: List[String], column: Int): List[String] =
    items
      .foldLeft(List.empty[String]) {
        case (current :: done, item) if ctx.fitsWidth(column, current.length + 1 + item.length) =>
          (current + " " + item) :: done
        case (lines, item) =>
          item :: lines
      }
      .reverse

}
