package com.colofabrix.scala.dixtra.render

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.normalize.SourceEdits
import com.colofabrix.scala.dixtra.render.context.RenderContext
import com.colofabrix.scala.dixtra.render.definition.*
import com.colofabrix.scala.dixtra.render.term.TermRenderer
import com.colofabrix.scala.dixtra.verify.Equivalence
import scala.meta.*
import scala.meta.tokens.Token

/**
 * The dixtra tree renderer: the single routing point over a parsed compilation unit.
 *
 * It owns the shared [[com.colofabrix.scala.dixtra.render.context.RenderContext]] and the per-construct renderers,
 * dispatching each node to its rule. Any construct without a rule falls back to the safety net (verbatim source), so
 * the tool never corrupts unsupported code. Text between handled nodes (whitespace, blank lines, comments) is preserved
 * exactly by splicing formatted node slices back into the original source.
 *
 * Formatting is recursive: the members of containers, and the definitions and expressions nested in blocks, case bodies
 * and lambdas, all follow the same rules. Consecutive one-line definitions align their `=` (see [[Alignment]]). Every
 * rendered definition or statement is verified, at whatever depth: if it does not parse back to an equivalent tree with
 * the same comments, its original source is kept instead.
 *
 * @param original The full source text being formatted.
 * @param config The active formatting configuration.
 */
final class TreeRenderer(original: String, config: Config) {

  private val ctx         = new RenderContext(original, config)
  private val terms       = new TermRenderer(ctx, renderDefinitions)
  private val signatures  = new SignatureRenderer(ctx)
  private val assignments = new AssignmentRenderer(ctx, terms)
  private val defs        = new DefRenderer(ctx, terms, assignments, signatures)
  private val definitions = new DefinitionAlignment(ctx, assignments, defs)
  private val containers  = new ContainerRenderer(ctx, signatures, renderRegion)
  private val packages    = new PackageRenderer(ctx, renderRegion)
  private val extensions  = new ExtensionGroupRenderer(ctx, renderRegion)

  /**
   * Render a whole compilation unit by splicing its top-level statements.
   */
  def renderSource(source: Source): String =
    source.stats match {
      case (pkg: Pkg) :: Nil =>
        original.take(pkg.pos.start) + packages.render(pkg) + original.drop(pkg.pos.end)
      case stats =>
        renderRegion(0, original.length, stats)
    }

  /**
   * Whether rendering `source` adds or removes the braces of its single package clause. Its members are then laid out
   * at their old column and moved by two columns afterwards, so they must be laid out again where they end up.
   */
  def restylesPackage(source: Source): Boolean =
    source.stats match {
      case (pkg: Pkg) :: Nil =>
        packages.restyles(pkg)
      case _ =>
        false
    }

  /**
   * Render the statements found in `[start, end)` in place, aligning the consecutive one-line definitions among them.
   */
  def renderRegion(start: Int, end: Int, stats: List[Stat]): String =
    val aligned     = definitions.aligned(stats, s => ctx.columnOf(s.pos.start))
    val assignments = terms.alignedAssignments(stats, s => ctx.columnOf(s.pos.start))

    val prerendered =
      stats
        .map(_.pos.start)
        .zip(aligned.zip(assignments).map(_ orElse _))
        .toMap

    val extendsAt = alignedExtends(stats, prerendered)

    val spans =
      EndMarkers
        .pair(ctx, stats)
        .map { (stat, marker) =>
          val definition = prerendered.get(stat.pos.start).flatten
          val render     = (indent: Int) => renderStat(stat, marker, indent, definition, extendsAt.get(stat.pos.start))
          (stat.pos.start, marker.fold(stat.pos.end)(_.pos.end), render)
        }

    ctx.spliceSpans(start, end, spans)

  // A statement of a region at `indent`, its `extends` padded to `extendsTarget` when aligned, followed by its `end`
  // marker when it has or gets one. A definition or an assignment comes already rendered as `definition`.
  private def renderStat(
    stat: Stat,
    marker: Option[Term.EndMarker],
    indent: Int,
    definition: Option[Doc],
    extendsTarget: Option[Int],
  ): String =
    val node = renderNode(stat, indent, definition)

    val rendered =
      extendsTarget.fold(node) { target =>
        padExtends(node, target).getOrElse(node)
      }

    EndMarkers
      .marker(ctx, stat, marker, rendered)
      .fold(rendered) { end =>
        val indent = " " * ctx.lineIndentOf(stat.pos.start)
        rendered + "\n" + indent + end
      }

  /**
   * Render the definitions among the statements of a block at the given indent, aligning the consecutive one-line ones.
   * Each result is None for a statement that is not a definition, or a definition that must be kept as written.
   */
  def renderDefinitions(stats: List[Stat], indent: Int): List[Option[Doc]] =
    stats
      .zip(definitions.aligned(stats, _ => indent))
      .map { case (stat, doc) => doc.flatMap(verified(stat, _, indent)) }

  // Route a single node to its per-construct rule at the given base indent. A definition or an assignment statement
  // comes already rendered (and aligned) as `definition`.
  private def renderNode(tree: Tree, baseIndent: Int, definition: Option[Doc]): String =
    tree match {
      case p: Pkg =>
        renderRegion(p.pos.start, p.pos.end, p.body.stats)
      case o: Pkg.Object =>
        containers.render(o, o.templ)
      case o: Defn.Object =>
        containers.render(o, o.templ)
      case c: Defn.Class =>
        containers.render(c, c.templ)
      case t: Defn.Trait =>
        containers.render(t, t.templ)
      case g: Defn.Given =>
        containers.render(g, g.templ)
      case e: Defn.Enum =>
        containers.render(e, e.templ)
      case e: Defn.ExtensionGroup =>
        extensions.render(e)
      case t: Term =>
        verified(t, definition.getOrElse(terms.render(t, baseIndent, forceExplode = false)), baseIndent)
          .fold(SafetyNet.verbatim(tree))(Render.render(_, baseIndent))
      case s: Stat =>
        definition.flatMap(verified(s, _, baseIndent)).fold(SafetyNet.verbatim(tree))(Render.render(_, baseIndent))
      case _ =>
        SafetyNet.verbatim(tree)
    }

  // The width each statement pads the text before its `extends` to, by start offset, for consecutive one-line
  // definitions with an `extends` clause and no body (enum cases, classes, objects, traits), when
  // `alignment.extends-clauses` is on.
  private def alignedExtends(stats: List[Stat], definitions: Map[Int, Option[Doc]]): Map[Int, Int] =
    if !config.alignment.extendsClauses then
      Map.empty
    else
      val oneLine =
        stats
          .filter(hasBodilessExtends)
          .flatMap { stat =>
            val rendered = renderNode(stat, ctx.lineIndentOf(stat.pos.start), definitions.get(stat.pos.start).flatten)
            Option.when(!rendered.contains('\n'))(stat.pos.start -> rendered)
          }
          .toMap

      val width =
        (stat: Stat) =>
          oneLine
            .get(stat.pos.start)
            .flatMap(beforeExtends)

      val fitsAt =
        (stat: Stat, target: Int) =>
          oneLine
            .get(stat.pos.start)
            .zip(width(stat))
            .exists { (line, w) =>
              ctx.fitsWidth(ctx.lineIndentOf(stat.pos.start), line.length + math.max(0, target - w))
            }
      val targets =
        Alignment.targets(ctx, stats, Alignment.Grouping.Lines)(
          eligible = s => width(s).isDefined,
          width = width,
          fitsAt = fitsAt,
        )

      stats
        .zip(targets)
        .collect { case (stat, Some(target)) => stat.pos.start -> target }
        .toMap

  private def hasBodilessExtends(stat: Stat): Boolean =
    stat match {
      case defn: Defn.EnumCase => defn.inits.nonEmpty
      case defn: Defn.Class    => defn.templ.inits.nonEmpty && defn.templ.body.stats.isEmpty
      case defn: Defn.Object   => defn.templ.inits.nonEmpty && defn.templ.body.stats.isEmpty
      case defn: Defn.Trait    => defn.templ.inits.nonEmpty && defn.templ.body.stats.isEmpty
      case _                   => false
    }

  // The offset of the `extends` keyword in a one-line rendered definition.
  private def extendsKeyword(line: String): Option[Int] =
    SourceEdits.tokens(line).flatMap(_.collectFirst { case t: Token.KwExtends => t.start })

  // The width of the text before ` extends` in a one-line rendered definition.
  private def beforeExtends(line: String): Option[Int] =
    extendsKeyword(line).map(at => line.take(at).stripTrailing.length)

  // A one-line rendered definition with the text before ` extends` padded to `target` columns.
  private def padExtends(line: String, target: Int): Option[String] =
    extendsKeyword(line).map { at =>
      val before = line.take(at).stripTrailing
      before + " " * (math.max(0, target - before.length) + 1) + line.drop(at)
    }

  // Accept a rendered definition or statement only if it is equivalent to the original. Containers and packages are not
  // checked here: their members are verified individually and the whole file is verified by [[Dixtra.format]].
  private def verified(tree: Tree, doc: Doc, indent: Int): Option[Doc] =
    Option.when(Equivalence.isEquivalentStat(tree, Render.render(doc, indent)))(doc)

}
