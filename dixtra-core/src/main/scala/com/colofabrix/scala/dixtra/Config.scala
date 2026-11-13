package com.colofabrix.scala.dixtra

import com.colofabrix.scala.dixtra.model.*

/**
 * Formatting configuration for the dixtra formatter.
 *
 * @param maxColumn The maximum allowed line width before a construct is forced to break.
 * @param overflow What happens to what goes past `maxColumn`.
 * @param braces Where the braces go, and the brace style of each construct.
 * @param selectorChain When select chains break.
 * @param body The layout of the body after `=` and `=>`.
 * @param definitionSite The layout of the parameters of definitions: signatures.
 * @param callSite The layout of the arguments of calls.
 * @param alignment What aligns with what.
 * @param blankLines The blank lines at the edges of bodies, between alignment groups and before scaladoc.
 * @param housekeeping The housekeeping of a whole file.
 * @param paths The files and directories to format.
 * @param compat213 Whether the output must also be valid Scala 2.13.
 */
final case class Config(
  maxColumn: Int,
  overflow: OverflowConfig,
  braces: BraceConfig,
  selectorChain: SelectorChainConfig,
  body: BodyConfig,
  definitionSite: DefinitionSiteConfig,
  callSite: CallSiteConfig,
  alignment: AlignmentConfig,
  blankLines: BlankLinesConfig,
  housekeeping: HousekeepingConfig,
  paths: PathsConfig,
  compat213: Boolean,
) {

  /**
   * The configuration dixtra formats with: with `compat213`, the settings that keep the output valid Scala 2.13 too,
   * whatever the other settings of this configuration say.
   */
  def effective: Config =
    if !compat213 then
      this
    else
      val effectivePolicies =
        Construct
          .values
          .map(construct => construct -> compatBracePolicy(construct))
          .toMap

      val effectiveSingleExpressions =
        BraceConfig
          .singleExprConstructs
          .map(_ -> BracePolicy.Keep)
          .toMap

      val effectiveBody = body.copy(semicolonStatements = SemicolonPolicy.Keep)

      copy(
        braces = braces.copy(
          policies = effectivePolicies,
          singleExpressions = effectiveSingleExpressions,
        ),
        body = effectiveBody,
      )

  // `keep` preserves the braces of the Scala 2 form of a construct; `package` and `try` are written without braces in
  // that form, so `keep` would render them braceless, as Scala 3.
  private def compatBracePolicy(construct: Construct): BracePolicy =
    construct match {
      case Construct.Package | Construct.Try => BracePolicy.ForceBraces
      case _                                 => BracePolicy.Keep
    }

}

object Config {

  /**
   * The default configuration.
   */
  val default: Config =
    Config(
      maxColumn = 120,
      overflow = OverflowConfig(trailingComment = TrailingOverflow.Keep),
      braces = BraceConfig.default,
      selectorChain =
        SelectorChainConfig(
          breakAtCount = 3,
          namespacePath = ChainHeadPolicy.Keep,
          objectMember = ChainHeadPolicy.AlwaysBreak,
          receiverMinLength = 1,
          inlineChains = InlineChainPolicy.BreakAtCount,
          inFor = NestedChainPolicy.NeverBreak,
          inCall = NestedChainPolicy.NeverBreak,
        ),
      body =
        BodyConfig(
          defBody = SingleBodyPolicy.Keep,
          valBody = SingleBodyPolicy.Keep,
          lazyVal = SingleBodyPolicy.Keep,
          varBody = SingleBodyPolicy.Keep,
          givenAlias = SingleBodyPolicy.Keep,
          typeDef = SingleBodyPolicy.Keep,
          assignment = SingleBodyPolicy.Keep,
          namedArgument = SingleBodyPolicy.Keep,
          lambdaArgument = SingleBodyPolicy.Keep,
          lambdaBody = SingleBodyPolicy.Keep,
          blockArgument = SingleBodyPolicy.Keep,
          testGroup = SingleBodyPolicy.Keep,
          testCase = SingleBodyPolicy.Keep,
          ifBody = MultiBodyPolicy.BreakMixed,
          caseBody = MultiBodyPolicy.BreakMixed,
          tryBody = MultiBodyPolicy.BreakMixed,
          forBody = SingleBodyPolicy.Keep,
          forYield = YieldBodyPolicy.BreakComplex,
          whileBody = SingleBodyPolicy.Keep,
          semicolonStatements = SemicolonPolicy.AlwaysBreak,
        ),
      definitionSite =
        DefinitionSiteConfig(
          groupsSeparator = GroupsSeparator.NextGroup,
          trailingCommas = TrailingCommas.Always,
        ),
      callSite =
        CallSiteConfig(
          groupsSeparator = GroupsSeparator.PreviousGroup,
          groupsBreak = GroupsBreak.LastFirst,
          trailingCommas = TrailingCommas.Always,
        ),
      alignment =
        AlignmentConfig(
          assignmentGroups = List(
            Set(DefinitionKind.Val, DefinitionKind.LazyVal, DefinitionKind.Var, DefinitionKind.Def),
          ),
          operators = List("=", "+=", "-=", "*=", "/=", ":=", "++=", "%", "%%", "%%%"),
          arrows = ArrowsConfig(cases = true, pairs = true, forEnumerators = true),
          extendsClauses = true,
          namedArguments = false,
          trailingComments = false,
          scaladoc = ScaladocAlignmentConfig(tags = false),
          acrossComments = false,
        ),
      blankLines = BlankLinesConfig.default,
      housekeeping = HousekeepingConfig(
        trimTrailingWhitespace = true,
        enforceEof = true,
        lineEnding = LineEnding.Keep,
      ),
      paths = PathsConfig.everything,
      compat213 = false,
    )

  /**
   * The default configuration with a different maximum line width.
   */
  def withMaxColumn(maxColumn: Int): Config =
    default.copy(maxColumn = maxColumn)

}
