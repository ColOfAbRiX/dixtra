package com.colofabrix.scala.dixtra.docfx

import com.colofabrix.scala.dixtra.Config
import com.colofabrix.scala.dixtra.model.*

/**
 * Reads and writes the `group.setting = value` assignments of a documentation example.
 *
 * The two directions are kept together on purpose: the default of a setting and the way it is applied must never drift
 * apart, or a page could document a default it does not honour.
 */
trait SettingsResolver {

  def set(config: Config, key: String, value: String): Either[String, Config]

  def default(key: String): Option[String]

}

/**
 * The settings documented so far in the settings reference.
 *
 * A page cannot be checked until its group is listed here: an unknown setting fails the documentation check instead of
 * silently formatting with the default.
 */
object ReferenceSettings extends SettingsResolver {

  private val setters: Map[String, (Config, String) => Either[String, Config]] =
    Map(
      "max-column" -> ((config, value) =>
        value
          .toIntOption
          .map(column => config.copy(maxColumn = column))
          .toRight(s"'$value' is not a number")),
      "call-site.trailing-commas" -> ((config, value) =>
        enumValue(TrailingCommas.values)(_.key, value).map(
          commas => config.copy(callSite = config.callSite.copy(trailingCommas = commas)),
        )),
      "call-site.groups-separator" -> ((config, value) =>
        enumValue(GroupsSeparator.values)(_.key, value).map(
          separator => config.copy(callSite = config.callSite.copy(groupsSeparator = separator)),
        )),
      "call-site.groups-break" -> ((config, value) =>
        enumValue(GroupsBreak.values)(_.key, value).map(
          groupsBreak => config.copy(callSite = config.callSite.copy(groupsBreak = groupsBreak)),
        )),
      "definition-site.trailing-commas" -> ((config, value) =>
        enumValue(TrailingCommas.values)(_.key, value).map(
          commas => config.copy(definitionSite = config.definitionSite.copy(trailingCommas = commas)),
        )),
      "definition-site.groups-separator" -> ((config, value) =>
        enumValue(GroupsSeparator.values)(_.key, value).map(
          separator => config.copy(definitionSite = config.definitionSite.copy(groupsSeparator = separator)),
        )),
      "overflow.trailing-comment" -> ((config, value) =>
        enumValue(TrailingOverflow.values)(_.key, value).map(
          overflow => config.copy(overflow = config.overflow.copy(trailingComment = overflow)),
        )),
      "alignment.assignment-groups" -> ((config, value) =>
        parseGroups(value).map(groups => config.copy(alignment = config.alignment.copy(assignmentGroups = groups)))),
      "alignment.operators" -> ((config, value) =>
        parseOperators(value).map(operators => config.copy(alignment = config.alignment.copy(operators = operators)))),
      "braces.style" -> ((config, value) =>
        enumValue(BracesStyle.values)(_.key, value).map(
          style =>
            config.copy(braces = config.braces.copy(style = style)),
        )),
      "braces.imports" -> ((config, value) =>
        enumValue(ImportBraces.values)(_.key, value).map(
          imports =>
            config.copy(braces = config.braces.copy(imports = imports)),
        )),
      "braces.abstract-class" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.AbstractClass, policy)),
            ),
        )),
      "braces.block-argument" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.BlockArgument, policy)),
            ),
        )),
      "braces.case-body" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.CaseBody, policy)),
            ),
        )),
      "braces.case-class" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.CaseClass, policy)),
            ),
        )),
      "braces.class" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(braces = config.braces.copy(policies = config.braces.policies.updated(Construct.Class, policy))),
        )),
      "braces.def" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(braces = config.braces.copy(policies = config.braces.policies.updated(Construct.Def, policy))),
        )),
      "braces.enum" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(braces = config.braces.copy(policies = config.braces.policies.updated(Construct.Enum, policy))),
        )),
      "braces.extension" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.Extension, policy)),
            ),
        )),
      "braces.for" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(braces = config.braces.copy(policies = config.braces.policies.updated(Construct.For, policy))),
        )),
      "braces.for-yield" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.ForYield, policy)),
            ),
        )),
      "braces.given-alias" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.GivenAlias, policy)),
            ),
        )),
      "braces.given-instance" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.GivenInstance, policy)),
            ),
        )),
      "braces.if" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(braces = config.braces.copy(policies = config.braces.policies.updated(Construct.If, policy))),
        )),
      "braces.lambda-argument" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.LambdaArgument, policy)),
            ),
        )),
      "braces.lambda-body" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.LambdaBody, policy)),
            ),
        )),
      "braces.lazy-val" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.LazyVal, policy)),
            ),
        )),
      "braces.match" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(braces = config.braces.copy(policies = config.braces.policies.updated(Construct.Match, policy))),
        )),
      "braces.new-anonymous" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.NewAnonymous, policy)),
            ),
        )),
      "braces.object" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.Object, policy)),
            ),
        )),
      "braces.package" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.Package, policy)),
            ),
        )),
      "braces.package-object" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.PackageObject, policy)),
            ),
        )),
      "braces.test-case" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.TestCase, policy)),
            ),
        )),
      "braces.test-group" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config.braces.copy(policies = config.braces.policies.updated(Construct.TestGroup, policy)),
            ),
        )),
      "braces.trait" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(braces = config.braces.copy(policies = config.braces.policies.updated(Construct.Trait, policy))),
        )),
      "braces.try" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(braces = config.braces.copy(policies = config.braces.policies.updated(Construct.Try, policy))),
        )),
      "braces.val" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(braces = config.braces.copy(policies = config.braces.policies.updated(Construct.Val, policy))),
        )),
      "braces.var" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(braces = config.braces.copy(policies = config.braces.policies.updated(Construct.Var, policy))),
        )),
      "braces.while" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(braces = config.braces.copy(policies = config.braces.policies.updated(Construct.While, policy))),
        )),
      "braces.single-expr.case-body" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config
                .braces
                .copy(singleExpressions = config.braces.singleExpressions.updated(Construct.CaseBody, policy)),
            ),
        )),
      "braces.single-expr.def" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces =
                config.braces.copy(singleExpressions = config.braces.singleExpressions.updated(Construct.Def, policy)),
            ),
        )),
      "braces.single-expr.given-alias" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config
                .braces
                .copy(singleExpressions = config.braces.singleExpressions.updated(Construct.GivenAlias, policy)),
            ),
        )),
      "braces.single-expr.lambda-body" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config
                .braces
                .copy(singleExpressions = config.braces.singleExpressions.updated(Construct.LambdaBody, policy)),
            ),
        )),
      "braces.single-expr.lazy-val" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces = config
                .braces
                .copy(singleExpressions = config.braces.singleExpressions.updated(Construct.LazyVal, policy)),
            ),
        )),
      "braces.single-expr.val" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces =
                config.braces.copy(singleExpressions = config.braces.singleExpressions.updated(Construct.Val, policy)),
            ),
        )),
      "braces.single-expr.var" -> ((config, value) =>
        enumValue(BracePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              braces =
                config.braces.copy(singleExpressions = config.braces.singleExpressions.updated(Construct.Var, policy)),
            ),
        )),
      "blank-lines.body-edges.abstract-class" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.AbstractClass, policy)),
            ),
        )),
      "blank-lines.body-edges.block-argument" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.BlockArgument, policy)),
            ),
        )),
      "blank-lines.body-edges.case-body" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.CaseBody, policy)),
            ),
        )),
      "blank-lines.body-edges.case-class" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.CaseClass, policy)),
            ),
        )),
      "blank-lines.body-edges.class" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.Class, policy)),
            ),
        )),
      "blank-lines.body-edges.def" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.Def, policy)),
            ),
        )),
      "blank-lines.body-edges.enum" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.Enum, policy)),
            ),
        )),
      "blank-lines.body-edges.extension" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.Extension, policy)),
            ),
        )),
      "blank-lines.body-edges.for" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.For, policy)),
            ),
        )),
      "blank-lines.body-edges.for-yield" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.ForYield, policy)),
            ),
        )),
      "blank-lines.body-edges.given-alias" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.GivenAlias, policy)),
            ),
        )),
      "blank-lines.body-edges.given-instance" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.GivenInstance, policy)),
            ),
        )),
      "blank-lines.body-edges.if" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.If, policy)),
            ),
        )),
      "blank-lines.body-edges.lambda-argument" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config
                  .blankLines
                  .copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.LambdaArgument, policy)),
            ),
        )),
      "blank-lines.body-edges.lambda-body" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.LambdaBody, policy)),
            ),
        )),
      "blank-lines.body-edges.lazy-val" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.LazyVal, policy)),
            ),
        )),
      "blank-lines.body-edges.match" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.Match, policy)),
            ),
        )),
      "blank-lines.body-edges.new-anonymous" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.NewAnonymous, policy)),
            ),
        )),
      "blank-lines.body-edges.object" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.Object, policy)),
            ),
        )),
      "blank-lines.body-edges.test-case" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.TestCase, policy)),
            ),
        )),
      "blank-lines.body-edges.test-group" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.TestGroup, policy)),
            ),
        )),
      "blank-lines.body-edges.trait" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.Trait, policy)),
            ),
        )),
      "blank-lines.body-edges.try" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.Try, policy)),
            ),
        )),
      "blank-lines.body-edges.val" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.Val, policy)),
            ),
        )),
      "blank-lines.body-edges.var" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.Var, policy)),
            ),
        )),
      "blank-lines.body-edges.while" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(
              blankLines =
                config.blankLines.copy(bodyEdges = config.blankLines.bodyEdges.updated(Construct.While, policy)),
            ),
        )),
      "blank-lines.between-alignment-groups" -> ((config, value) =>
        enumValue(BlankLinePolicy.values)(_.key, value).map(
          policy =>
            config.copy(blankLines = config.blankLines.copy(betweenAlignmentGroups = policy)),
        )),
      "blank-lines.before-scaladoc" -> ((config, value) =>
        enumValue(ScaladocSpacing.values)(_.key, value).map(
          spacing =>
            config.copy(blankLines = config.blankLines.copy(beforeScaladoc = spacing)),
        )),
      "body.def" -> ((config, value) =>
        enumValue(SingleBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(defBody = policy)),
        )),
      "body.val" -> ((config, value) =>
        enumValue(SingleBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(valBody = policy)),
        )),
      "body.lazy-val" -> ((config, value) =>
        enumValue(SingleBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(lazyVal = policy)),
        )),
      "body.var" -> ((config, value) =>
        enumValue(SingleBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(varBody = policy)),
        )),
      "body.given-alias" -> ((config, value) =>
        enumValue(SingleBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(givenAlias = policy)),
        )),
      "body.type-def" -> ((config, value) =>
        enumValue(SingleBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(typeDef = policy)),
        )),
      "body.assignment" -> ((config, value) =>
        enumValue(SingleBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(assignment = policy)),
        )),
      "body.named-argument" -> ((config, value) =>
        enumValue(SingleBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(namedArgument = policy)),
        )),
      "body.lambda-argument" -> ((config, value) =>
        enumValue(SingleBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(lambdaArgument = policy)),
        )),
      "body.lambda-body" -> ((config, value) =>
        enumValue(SingleBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(lambdaBody = policy)),
        )),
      "body.block-argument" -> ((config, value) =>
        enumValue(SingleBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(blockArgument = policy)),
        )),
      "body.test-case" -> ((config, value) =>
        enumValue(SingleBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(testCase = policy)),
        )),
      "body.test-group" -> ((config, value) =>
        enumValue(SingleBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(testGroup = policy)),
        )),
      "body.for" -> ((config, value) =>
        enumValue(SingleBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(forBody = policy)),
        )),
      "body.while" -> ((config, value) =>
        enumValue(SingleBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(whileBody = policy)),
        )),
      "body.if" -> ((config, value) =>
        enumValue(MultiBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(ifBody = policy)),
        )),
      "body.case-body" -> ((config, value) =>
        enumValue(MultiBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(caseBody = policy)),
        )),
      "body.try" -> ((config, value) =>
        enumValue(MultiBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(tryBody = policy)),
        )),
      "body.for-yield" -> ((config, value) =>
        enumValue(YieldBodyPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(forYield = policy)),
        )),
      "body.semicolon-statements" -> ((config, value) =>
        enumValue(SemicolonPolicy.values)(_.key, value).map(
          policy =>
            config.copy(body = config.body.copy(semicolonStatements = policy)),
        )),
      "alignment.arrows.cases" -> ((config, value) =>
        parseBoolean(value).map(
          cases =>
            config.copy(alignment = config.alignment.copy(arrows = config.alignment.arrows.copy(cases = cases))),
        )),
      "alignment.arrows.pairs" -> ((config, value) =>
        parseBoolean(value).map(
          pairs =>
            config.copy(alignment = config.alignment.copy(arrows = config.alignment.arrows.copy(pairs = pairs))),
        )),
      "alignment.arrows.for-enumerators" -> ((config, value) =>
        parseBoolean(value).map(
          forEnumerators =>
            config.copy(
              alignment =
                config.alignment.copy(arrows = config.alignment.arrows.copy(forEnumerators = forEnumerators)),
            ),
        )),
      "alignment.extends-clauses" -> ((config, value) =>
        parseBoolean(value).map(
          extendsClauses => config.copy(alignment = config.alignment.copy(extendsClauses = extendsClauses)),
        )),
      "alignment.named-arguments" -> ((config, value) =>
        parseBoolean(value).map(
          namedArguments => config.copy(alignment = config.alignment.copy(namedArguments = namedArguments)),
        )),
      "alignment.trailing-comments" -> ((config, value) =>
        parseBoolean(value).map(
          trailingComments => config.copy(alignment = config.alignment.copy(trailingComments = trailingComments)),
        )),
      "alignment.across-comments" -> ((config, value) =>
        parseBoolean(value).map(
          acrossComments => config.copy(alignment = config.alignment.copy(acrossComments = acrossComments)),
        )),
      "alignment.scaladoc.tags" -> ((config, value) =>
        parseBoolean(value).map(
          tags => config.copy(alignment = config.alignment.copy(scaladoc = ScaladocAlignmentConfig(tags = tags))),
        )),
      "selector-chain.inline-chains" -> ((config, value) =>
        enumValue(InlineChainPolicy.values)(_.key, value).map(
          policy =>
            config.copy(selectorChain = config.selectorChain.copy(inlineChains = policy)),
        )),
      "selector-chain.break-at-count" -> ((config, value) =>
        parseInt(value, 2).map(count => config.copy(selectorChain = config.selectorChain.copy(breakAtCount = count)))),
      "selector-chain.in-for" -> ((config, value) =>
        enumValue(NestedChainPolicy.values)(_.key, value).map(
          policy =>
            config.copy(selectorChain = config.selectorChain.copy(inFor = policy)),
        )),
      "selector-chain.in-call" -> ((config, value) =>
        enumValue(NestedChainPolicy.values)(_.key, value).map(
          policy =>
            config.copy(selectorChain = config.selectorChain.copy(inCall = policy)),
        )),
      "selector-chain.namespace-path" -> ((config, value) =>
        enumValue(ChainHeadPolicy.values)(_.key, value).map(
          policy =>
            config.copy(selectorChain = config.selectorChain.copy(namespacePath = policy)),
        )),
      "selector-chain.object-member" -> ((config, value) =>
        enumValue(ChainHeadPolicy.values)(_.key, value).map(
          policy =>
            config.copy(selectorChain = config.selectorChain.copy(objectMember = policy)),
        )),
      "selector-chain.receiver-min-length" -> ((config, value) =>
        parseInt(value, 1).map(
          length => config.copy(selectorChain = config.selectorChain.copy(receiverMinLength = length)),
        )),
    )

  private val getters: Map[String, Config => String] =
    Map(
      "max-column"                       -> (_.maxColumn.toString),
      "call-site.trailing-commas"        -> (_.callSite.trailingCommas.key),
      "call-site.groups-separator"       -> (_.callSite.groupsSeparator.key),
      "call-site.groups-break"           -> (_.callSite.groupsBreak.key),
      "definition-site.trailing-commas"  -> (_.definitionSite.trailingCommas.key),
      "definition-site.groups-separator" -> (_.definitionSite.groupsSeparator.key),
      "overflow.trailing-comment"        -> (_.overflow.trailingComment.key),
      "alignment.assignment-groups"      -> (config => renderGroups(config.alignment.assignmentGroups)),
      "alignment.operators"              -> (config => renderOperators(config.alignment.operators)),
      "braces.style"                     -> (_.braces.style.key),
      "braces.imports"                   -> (_.braces.imports.key),
      "braces.abstract-class"            -> (config => config.braces(Construct.AbstractClass).key),
      "braces.block-argument"            -> (config => config.braces(Construct.BlockArgument).key),
      "braces.case-body"                 -> (config => config.braces(Construct.CaseBody).key),
      "braces.case-class"                -> (config => config.braces(Construct.CaseClass).key),
      "braces.class"                     -> (config => config.braces(Construct.Class).key),
      "braces.def"                       -> (config => config.braces(Construct.Def).key),
      "braces.enum"                      -> (config => config.braces(Construct.Enum).key),
      "braces.extension"                 -> (config => config.braces(Construct.Extension).key),
      "braces.for"                       -> (config => config.braces(Construct.For).key),
      "braces.for-yield"                 -> (config => config.braces(Construct.ForYield).key),
      "braces.given-alias"               -> (config => config.braces(Construct.GivenAlias).key),
      "braces.given-instance"            -> (config => config.braces(Construct.GivenInstance).key),
      "braces.if"                        -> (config => config.braces(Construct.If).key),
      "braces.lambda-argument"           -> (config => config.braces(Construct.LambdaArgument).key),
      "braces.lambda-body"               -> (config => config.braces(Construct.LambdaBody).key),
      "braces.lazy-val"                  -> (config => config.braces(Construct.LazyVal).key),
      "braces.match"                     -> (config => config.braces(Construct.Match).key),
      "braces.new-anonymous"             -> (config => config.braces(Construct.NewAnonymous).key),
      "braces.object"                    -> (config => config.braces(Construct.Object).key),
      "braces.package"                   -> (config => config.braces(Construct.Package).key),
      "braces.package-object"            -> (config => config.braces(Construct.PackageObject).key),
      "braces.test-case"                 -> (config => config.braces(Construct.TestCase).key),
      "braces.test-group"                -> (config => config.braces(Construct.TestGroup).key),
      "braces.trait"                     -> (config => config.braces(Construct.Trait).key),
      "braces.try"                       -> (config => config.braces(Construct.Try).key),
      "braces.val"                       -> (config => config.braces(Construct.Val).key),
      "braces.var"                       -> (config => config.braces(Construct.Var).key),
      "braces.while"                     -> (config => config.braces(Construct.While).key),
      "braces.single-expr.case-body" -> (config =>
        config
          .braces
          .singleExpr(Construct.CaseBody)
          .key),
      "braces.single-expr.def" -> (config =>
        config
          .braces
          .singleExpr(Construct.Def)
          .key),
      "braces.single-expr.given-alias" -> (config =>
        config
          .braces
          .singleExpr(Construct.GivenAlias)
          .key),
      "braces.single-expr.lambda-body" -> (config =>
        config
          .braces
          .singleExpr(Construct.LambdaBody)
          .key),
      "braces.single-expr.lazy-val" -> (config =>
        config
          .braces
          .singleExpr(Construct.LazyVal)
          .key),
      "braces.single-expr.val" -> (config =>
        config
          .braces
          .singleExpr(Construct.Val)
          .key),
      "braces.single-expr.var" -> (config =>
        config
          .braces
          .singleExpr(Construct.Var)
          .key),
      "blank-lines.body-edges.abstract-class" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.AbstractClass, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.block-argument" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.BlockArgument, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.case-body" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.CaseBody, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.case-class" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.CaseClass, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.class" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.Class, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.def" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.Def, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.enum" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.Enum, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.extension" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.Extension, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.for" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.For, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.for-yield" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.ForYield, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.given-alias" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.GivenAlias, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.given-instance" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.GivenInstance, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.if" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.If, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.lambda-argument" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.LambdaArgument, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.lambda-body" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.LambdaBody, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.lazy-val" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.LazyVal, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.match" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.Match, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.new-anonymous" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.NewAnonymous, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.object" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.Object, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.test-case" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.TestCase, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.test-group" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.TestGroup, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.trait" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.Trait, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.try" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.Try, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.val" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.Val, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.var" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.Var, BlankLinePolicy.Keep)
          .key),
      "blank-lines.body-edges.while" -> (config =>
        config
          .blankLines
          .bodyEdges
          .getOrElse(Construct.While, BlankLinePolicy.Keep)
          .key),
      "blank-lines.between-alignment-groups" -> (_.blankLines.betweenAlignmentGroups.key),
      "blank-lines.before-scaladoc"          -> (_.blankLines.beforeScaladoc.key),
      "body.def"                             -> (_.body.defBody.key),
      "body.val"                             -> (_.body.valBody.key),
      "body.lazy-val"                        -> (_.body.lazyVal.key),
      "body.var"                             -> (_.body.varBody.key),
      "body.given-alias"                     -> (_.body.givenAlias.key),
      "body.type-def"                        -> (_.body.typeDef.key),
      "body.assignment"                      -> (_.body.assignment.key),
      "body.named-argument"                  -> (_.body.namedArgument.key),
      "body.lambda-argument"                 -> (_.body.lambdaArgument.key),
      "body.lambda-body"                     -> (_.body.lambdaBody.key),
      "body.block-argument"                  -> (_.body.blockArgument.key),
      "body.test-case"                       -> (_.body.testCase.key),
      "body.test-group"                      -> (_.body.testGroup.key),
      "body.for"                             -> (_.body.forBody.key),
      "body.while"                           -> (_.body.whileBody.key),
      "body.if"                              -> (_.body.ifBody.key),
      "body.case-body"                       -> (_.body.caseBody.key),
      "body.try"                             -> (_.body.tryBody.key),
      "body.for-yield"                       -> (_.body.forYield.key),
      "body.semicolon-statements"            -> (_.body.semicolonStatements.key),
      "alignment.arrows.cases"               -> (_.alignment.arrows.cases.toString),
      "alignment.arrows.pairs"               -> (_.alignment.arrows.pairs.toString),
      "alignment.arrows.for-enumerators"     -> (_.alignment.arrows.forEnumerators.toString),
      "alignment.extends-clauses"            -> (_.alignment.extendsClauses.toString),
      "alignment.named-arguments"            -> (_.alignment.namedArguments.toString),
      "alignment.trailing-comments"          -> (_.alignment.trailingComments.toString),
      "alignment.across-comments"            -> (_.alignment.acrossComments.toString),
      "alignment.scaladoc.tags"              -> (_.alignment.scaladoc.tags.toString),
      "selector-chain.inline-chains"         -> (_.selectorChain.inlineChains.key),
      "selector-chain.break-at-count"        -> (_.selectorChain.breakAtCount.toString),
      "selector-chain.in-for"                -> (_.selectorChain.inFor.key),
      "selector-chain.in-call"               -> (_.selectorChain.inCall.key),
      "selector-chain.namespace-path"        -> (_.selectorChain.namespacePath.key),
      "selector-chain.object-member"         -> (_.selectorChain.objectMember.key),
      "selector-chain.receiver-min-length"   -> (_.selectorChain.receiverMinLength.toString),
    )

  def set(config: Config, key: String, value: String): Either[String, Config] =
    setters
      .get(key)
      .toRight(s"unknown setting '$key'")
      .flatMap(setter => setter(config, value))

  def default(key: String): Option[String] =
    getters.get(key).map(getter => getter(Config.default))

  private def enumValue[A](values: Array[A])(keyOf: A => String, key: String): Either[String, A] =
    values.find(value => keyOf(value) == key).toRight(s"'$key' is not one of ${values.map(keyOf).mkString(", ")}")

  private def parseInt(value: String, minimum: Int): Either[String, Int] =
    value
      .toIntOption
      .filter(_ >= minimum)
      .toRight(s"'$value' is not a number of at least $minimum")

  private def parseBoolean(value: String): Either[String, Boolean] =
    value.toBooleanOption.toRight(s"'$value' is not true or false")

  private val groupPattern = """\[([^\]]*)\]""".r

  private def parseGroups(value: String): Either[String, List[Set[DefinitionKind]]] =
    val inner = value.stripPrefix("[").stripSuffix("]")
    if inner.isBlank then
      Right(Nil)
    else
      val groups =
        groupPattern
          .findAllMatchIn(inner)
          .map(_.group(1))
          .toList
      collect(groups.map(group => parseGroup(group)))

  private def parseGroup(group: String): Either[String, Set[DefinitionKind]] =
    collect(group.split(",").toList.map(key => enumValue(DefinitionKind.values)(_.key, key.trim))).map(_.toSet)

  private def collect[A](results: List[Either[String, A]]): Either[String, List[A]] =
    results.foldRight[Either[String, List[A]]](Right(Nil)) { (result, accumulated) =>
      for
        head <- result
        tail <- accumulated
      yield
        head :: tail
    }

  private def renderGroups(groups: List[Set[DefinitionKind]]): String =
    groups
      .map(group => group.toList.sortBy(_.ordinal).map(_.key).mkString("[", ", ", "]"))
      .mkString("[", ", ", "]")

  private def parseOperators(value: String): Either[String, List[String]] =
    val inner = value.stripPrefix("[").stripSuffix("]")
    if inner.isBlank then Right(Nil)
    else Right(inner.split(",").toList.map(_.trim.stripPrefix("\"").stripSuffix("\"")))

  private def renderOperators(operators: List[String]): String =
    operators.map(operator => s"\"$operator\"").mkString("[", ", ", "]")

}
