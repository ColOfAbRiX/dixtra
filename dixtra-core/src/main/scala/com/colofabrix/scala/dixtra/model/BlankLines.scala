package com.colofabrix.scala.dixtra.model

/**
 * Where blank lines go.
 *
 * @param bodyEdges The blank lines at the edges of the body of each construct: after its opening `{` (or its header,
 *                  braceless) and before its closing `}`. `keep` (the default for a construct not listed) makes the two
 *                  edges agree: a blank line at either edge means a blank line at both.
 * @param betweenAlignmentGroups The blank line between two adjacent one-line definitions of different alignment groups.
 * @param beforeScaladoc The blank line before a scaladoc. At the start of a body, a scaladoc that wants one counts as a
 *                       blank line written at its opening edge, and the body edges decide.
 */
final case class BlankLinesConfig(
  bodyEdges: Map[Construct, BlankLinePolicy],
  betweenAlignmentGroups: BlankLinePolicy,
  beforeScaladoc: ScaladocSpacing,
) {

  /**
   * The edge blank-line policy of `construct`.
   */
  def apply(construct: Construct): BlankLinePolicy =
    bodyEdges.getOrElse(construct, BlankLinePolicy.Keep)

}

object BlankLinesConfig {

  /**
   * The default edge blank lines: kept as the author wrote them (made symmetric) for containers and test blocks, always
   * for a package, never for the other bodies.
   */
  val default: BlankLinesConfig =
    import Construct.*
    import BlankLinePolicy.*
    BlankLinesConfig(
      Map(
        Class          -> Keep,
        AbstractClass  -> Keep,
        CaseClass      -> Keep,
        Object         -> Keep,
        PackageObject  -> Keep,
        Trait          -> Keep,
        Enum           -> Keep,
        Extension      -> Keep,
        GivenInstance  -> Keep,
        GivenAlias     -> Never,
        NewAnonymous   -> Never,
        Try            -> Never,
        Match          -> Never,
        While          -> Never,
        LambdaArgument -> Never,
        BlockArgument  -> Never,
        TestBlock      -> Keep,
        If             -> Never,
        For            -> Never,
        ForYield       -> Never,
        Def            -> Never,
        Val            -> Never,
        LazyVal        -> Never,
        Var            -> Never,
        CaseBody       -> Never,
        LambdaBody     -> Never,
        Package        -> Always,
      ),
      betweenAlignmentGroups = Keep,
      beforeScaladoc = ScaladocSpacing.Always,
    )

}

/**
 * Whether a blank line goes before a scaladoc that follows a line of code.
 */
enum ScaladocSpacing(val key: String) {

  /**
   * Before every scaladoc.
   */
  case Always extends ScaladocSpacing("always")

  /**
   * Before every multi-line scaladoc; a one-line scaladoc as the author wrote it.
   */
  case AlwaysMultiline extends ScaladocSpacing("always-multiline")

  /**
   * As the author wrote it.
   */
  case Keep extends ScaladocSpacing("keep")

  /**
   * Never.
   */
  case Never extends ScaladocSpacing("never")

  /**
   * Whether a scaladoc whose first line is `firstLine` wants a blank line before it.
   */
  def wantsBlank(firstLine: String): Boolean =
    this match {
      case Always          => true
      case AlwaysMultiline => !firstLine.contains("*/")
      case Keep | Never    => false
    }

}

/**
 * Whether a blank line separates two things.
 */
enum BlankLinePolicy(val key: String) {

  /**
   * As the author wrote it.
   */
  case Keep extends BlankLinePolicy("keep")

  /**
   * Always a blank line.
   */
  case Always extends BlankLinePolicy("always")

  /**
   * Never a blank line.
   */
  case Never extends BlankLinePolicy("never")

}
