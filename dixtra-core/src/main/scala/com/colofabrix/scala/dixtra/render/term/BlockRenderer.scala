package com.colofabrix.scala.dixtra.render.term

import com.colofabrix.scala.dixtra.doc.*
import com.colofabrix.scala.dixtra.doc.Doc.++
import com.colofabrix.scala.dixtra.model.BlankLinePolicy
import com.colofabrix.scala.dixtra.render.EndMarkers
import com.colofabrix.scala.dixtra.render.comments.StatementComments
import com.colofabrix.scala.dixtra.render.context.RenderContext
import scala.meta.*

/**
 * Renders blocks in statement position: a `def`/`val` body, an if/else branch, a lambda or try body.
 *
 * Such a block never keeps its braces: a single expression is unwrapped to that expression, and a multi-statement block
 * becomes a Scala 3 significant-indentation block (one statement per line at the body indent). Each statement is
 * re-rendered with the same rules as at the top level (local definitions through `definitions`, which also aligns
 * them), its comments are re-attached, and the author's blank lines between statements are kept. A
 * block holding a single non-expression statement (e.g. `{ val _ = f() }`)
 * becomes an indented block too, so no lonely `{` line appears; only an empty block keeps its braces.
 *
 * @param ctx The shared render context.
 * @param terms The term renderer, for the statements.
 * @param definitions Renders the local definitions among statements at an indent: None for a statement that is not a
 *                    definition or must be kept as written.
 */
private[term] final class BlockRenderer(
  ctx: RenderContext,
  terms: TermRenderer,
  definitions: (List[Stat], Int) => List[Option[Doc]],
) {

  /**
   * Render a block in statement position. One written on one line with `;` stays so with
   * `body.semicolon-statements = keep`.
   */
  def render(block: Term.Block, indent: Int, forceExplode: Boolean): Doc =
    val comments = StatementComments.of(ctx, block)
    block.stats match {
      case _ if SemicolonStatements.keptOnLine(ctx, block) =>
        ctx.ownSourceDoc(block)
      case Nil =>
        ctx.ownSourceDoc(block)
      case (stat: Term) :: Nil if comments.isEmpty =>
        terms.render(stat, indent, forceExplode)
      case (_: Term) :: Nil =>
        statements(block, indent, BlankLinePolicy.Never)
      case _ :: Nil =>
        Doc.Standalone(statements(block, indent, BlankLinePolicy.Never))
      case _ =>
        statements(block, indent, BlankLinePolicy.Never)
    }

  /**
   * The statements of a block one per line, with their comments re-attached and the blank lines at the edges of the
   * block decided by `edges`.
   */
  def statements(block: Term.Block, indent: Int, edges: BlankLinePolicy): Doc =
    val paired   = EndMarkers.pair(ctx, block.stats)
    val comments = StatementComments.of(ctx, block.pos.start, paired.map(_._1), block.pos.end)
    comments.withEdgeBlanks(edges).lines(renderStats(paired, indent))

  /**
   * The statements of a block without braces, one per line, with the blank line at their opening edge decided by
   * `edges`: a braceless body has no closing edge.
   */
  def bracelessStatements(block: Term.Block, indent: Int, edges: BlankLinePolicy): Doc =
    val paired   = EndMarkers.pair(ctx, block.stats)
    val comments = StatementComments.of(ctx, block.pos.start, paired.map(_._1), block.pos.end)
    comments.withOpeningBlank(edges).lines(renderStats(paired, indent))

  /**
   * The statements found in `[start, end)` one per line, keeping the comments and blank lines around them, for a braced
   * region wider than the block itself (e.g. a lambda body, from its `=>` to the closing brace), with the blank lines
   * at its edges decided by `edges`.
   */
  def statementsIn(start: Int, stats: List[Stat], end: Int, indent: Int, edges: BlankLinePolicy): Doc =
    val paired = EndMarkers.pair(ctx, stats)
    StatementComments
      .of(ctx, start, paired.map(_._1), end)
      .withEdgeBlanks(edges)
      .lines(renderStats(paired, indent))

  // The statements inside a block: a term runs through the term rules, a local definition through the definition rules;
  // anything else is copied verbatim, keeping its relative layout. Each is followed by the `end` marker its brace style
  // asks for (see [[EndMarkers]]).
  private def renderStats(paired: List[(Stat, Option[Term.EndMarker])], indent: Int): List[Doc] =
    val stats       = paired.map(_._1)
    val assignments = terms.alignedAssignments(stats, _ => indent)
    paired
      .zip(definitions(stats, indent))
      .zip(assignments)
      .map { case (((stat, marker), definition), assignment) =>
        val doc =
          (stat, definition, assignment) match {
            case (_: Term, _, Some(assignment)) => assignment
            case (term: Term, _, None)          => terms.render(term, indent, forceExplode = false)
            case (_, Some(definition), _)       => definition
            case (other, None, _)               => ctx.sourceDoc(other)
          }
        EndMarkers
          .marker(ctx, stat, marker, Render.render(doc, indent))
          .fold(doc)(end => doc ++ Doc.Line ++ Doc.text(end))
      }

}
