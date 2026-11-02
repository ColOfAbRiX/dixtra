package com.colofabrix.scala.dixtra.model

/**
 * How a construct writes its body.
 */
enum BracePolicy(val key: String) {

  /**
   * In curly braces: `{ ... }` (for `if` / `while`: `if (c) { ... }`).
   */
  case ForceBraces extends BracePolicy("force-braces")

  /**
   * Braceless, closed by an `end` marker.
   */
  case ForceEnd extends BracePolicy("force-end")

  /**
   * Braceless, without an `end` marker: the body is only indented.
   */
  case ForceIndent extends BracePolicy("force-indent")

  /**
   * As the author wrote it.
   */
  case Keep extends BracePolicy("keep")

}

/**
 * Where the braces go relative to the code around them.
 */
enum BracesStyle(val key: String) {

  /**
   * `{` at the end of the header line, `} else {` on one line.
   */
  case OneTrueBrace extends BracesStyle("1tbs")

  /**
   * As 1TBS, with `else`, `catch`, `finally` and `yield` starting the line after the closing `}`.
   */
  case Stroustrup extends BracesStyle("stroustrup")

  /**
   * As Stroustrup, with every opening `{` of a body on its own line.
   */
  case Allman extends BracesStyle("allman")

}

/**
 * The braces: where they go, and the brace style of each construct; a construct without a setting is
 * [[BracePolicy.Keep]].
 *
 * @param style Where the braces go relative to the code around them.
 * @param policies The brace policy of each construct.
 */
final case class BraceConfig(style: BracesStyle, policies: Map[Construct, BracePolicy]) {

  /**
   * The brace style of `construct`.
   */
  def apply(construct: Construct): BracePolicy =
    policies.getOrElse(construct, BracePolicy.Keep)

}

object BraceConfig {
  import BracePolicy.*
  import Construct.*

  val default: BraceConfig =
    BraceConfig(
      BracesStyle.OneTrueBrace,
      Map(
        Class          -> ForceBraces,
        AbstractClass  -> ForceBraces,
        CaseClass      -> ForceBraces,
        Object         -> ForceBraces,
        Trait          -> ForceBraces,
        Enum           -> ForceBraces,
        Extension      -> ForceBraces,
        GivenInstance  -> ForceBraces,
        NewAnonymous   -> ForceBraces,
        Try            -> ForceBraces,
        Match          -> ForceBraces,
        While          -> ForceBraces,
        LambdaArgument -> ForceBraces,
        BlockArgument  -> ForceBraces,
        TestBlock      -> ForceBraces,
        Package        -> ForceIndent,
        If             -> ForceIndent,
        For            -> ForceIndent,
        ForYield       -> ForceIndent,
        Def            -> ForceIndent,
        Val            -> ForceIndent,
        LazyVal        -> ForceIndent,
        Var            -> ForceIndent,
        CaseBody       -> ForceIndent,
        LambdaBody     -> Keep,
        GivenAlias     -> Keep,
        PackageObject  -> Keep,
      ),
    )
}
