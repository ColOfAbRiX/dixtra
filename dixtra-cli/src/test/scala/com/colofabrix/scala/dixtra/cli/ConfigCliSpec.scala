package com.colofabrix.scala.dixtra.cli

import cats.data.*
import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.testkit.TempFiles.*
import com.typesafe.config.ConfigFactory
import java.nio.file.Paths
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Loading the configuration: `reference.conf` as the defaults, HOCON overrides from a string or a file, the
 * `.dixtra.conf` lookup, and readable errors.
 */
class ConfigCliSpec extends AnyWordSpec with Matchers {

  "the defaults" should {

    "be the hardcoded default configuration" in {
      val expected = Config.default
      val actual = ConfigCli.default
      actual shouldBe expected
    }

    "be the default configuration when read from the schema alone" in {
      val expected = Validated.valid((Config.default, Nil))

      val actual =
        ConfigCli
          .load("", Paths.get("").toAbsolutePath, ConfigFactory.empty())
          .map(loaded => (loaded.base, loaded.warnings))

      actual shouldBe expected
    }

  }

  "a setting missing from the defaults" should {

    "be read as its default, without failing or warning" in {
      val refs     = ConfigCli.references.withoutPath("max-column")
      val expected = Validated.valid((Config.default, Nil))

      val actual =
        ConfigCli
          .load("", Paths.get("").toAbsolutePath, refs)
          .map(loaded => (loaded.base, loaded.warnings))

      actual shouldBe expected
    }

    "be accepted when the configuration sets it, without warning" in {
      val refs     = ConfigCli.references.withoutPath("max-column")
      val expected = Validated.valid((Config.withMaxColumn(80), Nil))

      val actual =
        ConfigCli
          .load("max-column = 80", Paths.get("").toAbsolutePath, refs)
          .map(loaded => (loaded.base, loaded.warnings))

      actual shouldBe expected
    }

    "not turn an unknown setting into a known one" in {
      val refs     = ConfigCli.references.withoutPath("max-column")
      val expected = Validated.valid(List("unknown setting nonsense (line 1), ignored"))

      val actual =
        ConfigCli
          .load("nonsense = 1", Paths.get("").toAbsolutePath, refs)
          .map(_.warnings)

      actual shouldBe expected
    }

    "read a configuration with no version as the current one" in {
      val refs     = ConfigCli.references.withoutPath("config-version")
      val expected = Validated.valid((Config.default, Nil))

      val actual =
        ConfigCli
          .load("", Paths.get("").toAbsolutePath, refs)
          .map(loaded => (loaded.base, loaded.warnings))

      actual shouldBe expected
    }

  }

  "an override" should {

    "change only the keys it sets" in {
      val expected = Validated.valid(Config.withMaxColumn(80))
      val actual = ConfigCli.fromString("max-column = 80")
      actual shouldBe expected
    }

    "set the brace style of a construct, keeping the others" in {
      val expected = Validated.valid((BracePolicy.ForceBraces, BracePolicy.ForceBraces))

      val actual =
        ConfigCli
          .fromString("braces.if = force-braces")
          .map(config => (config.braces(Construct.If), config.braces(Construct.Match)))

      actual shouldBe expected
    }

    "set the setting it names, trying every value of the settings with few values" in {
      val table: List[(String, Config => Any, Any)] =
        List(
          (
            "blank-lines.body-edges.match = always",
            _.blankLines(Construct.Match),
            BlankLinePolicy.Always,
          ),
          (
            "braces.style = allman",
            _.braces.style,
            BracesStyle.Allman,
          ),
          (
            "alignment.scaladoc.tags = true",
            _.alignment.scaladoc.tags,
            true,
          ),
          (
            "selector-chain.break-at-count = 4",
            _.selectorChain.breakAtCount,
            4,
          ),
          (
            "blank-lines.between-alignment-groups = always",
            _.blankLines.betweenAlignmentGroups,
            BlankLinePolicy.Always,
          ),
          (
            "blank-lines.before-scaladoc = always-multiline",
            _.blankLines.beforeScaladoc,
            ScaladocSpacing.AlwaysMultiline,
          ),
          (
            "alignment.arrows.for-enumerators = false",
            _.alignment.arrows.forEnumerators,
            false,
          ),
          (
            "alignment.extends-clauses = false",
            _.alignment.extendsClauses,
            false,
          ),
          (
            "alignment.extends-clauses = false",
            _.alignment.extendsClauses,
            false,
          ),
          (
            "alignment.across-comments = true",
            _.alignment.acrossComments,
            true,
          ),
          (
            "selector-chain.receiver-min-length = 3",
            _.selectorChain.receiverMinLength,
            3,
          ),
          (
            "selector-chain.in-call = never-break",
            _.selectorChain.inCall,
            NestedChainPolicy.NeverBreak,
          ),
          (
            "selector-chain.in-call = keep",
            _.selectorChain.inCall,
            NestedChainPolicy.Keep,
          ),
          (
            "selector-chain.in-for = never-break",
            _.selectorChain.inFor,
            NestedChainPolicy.NeverBreak,
          ),
          (
            "selector-chain.in-for = keep",
            _.selectorChain.inFor,
            NestedChainPolicy.Keep,
          ),
          (
            "selector-chain.inline-chains = keep",
            _.selectorChain.inlineChains,
            InlineChainPolicy.Keep,
          ),
          (
            "selector-chain.object-member = always-join",
            _.selectorChain.objectMember,
            ChainHeadPolicy.AlwaysJoin,
          ),
          (
            "definition-site.groups-separator = previous-group",
            _.definitionSite.groupsSeparator,
            GroupsSeparator.PreviousGroup,
          ),
          (
            "call-site.groups-separator = next-group",
            _.callSite.groupsSeparator,
            GroupsSeparator.NextGroup,
          ),
          (
            "call-site.groups-break = break-all",
            _.callSite.groupsBreak,
            GroupsBreak.BreakAll,
          ),
          (
            "call-site.groups-break = last-first",
            _.callSite.groupsBreak,
            GroupsBreak.LastFirst,
          ),
          (
            "call-site.groups-break = independent",
            _.callSite.groupsBreak,
            GroupsBreak.Independent,
          ),
          (
            "alignment.named-arguments = true",
            _.alignment.namedArguments,
            true,
          ),
          (
            "alignment.trailing-comments = true",
            _.alignment.trailingComments,
            true,
          ),
          (
            "trailing-comment-overflow = keep",
            _.trailingCommentOverflow,
            TrailingOverflow.Keep,
          ),
          (
            "trailing-comment-overflow = move-above",
            _.trailingCommentOverflow,
            TrailingOverflow.MoveAbove,
          ),
          (
            "trailing-commas = always",
            _.trailingCommas,
            TrailingCommas.Always,
          ),
          (
            "trailing-commas = never",
            _.trailingCommas,
            TrailingCommas.Never,
          ),
          (
            "housekeeping.line-ending = lf",
            _.housekeeping.lineEnding,
            LineEnding.Lf,
          ),
          (
            "housekeeping.line-ending = crlf",
            _.housekeeping.lineEnding,
            LineEnding.Crlf,
          ),
          (
            "housekeeping.line-ending = cr",
            _.housekeeping.lineEnding,
            LineEnding.Cr,
          ),
          (
            "housekeeping.line-ending = keep",
            _.housekeeping.lineEnding,
            LineEnding.Keep,
          ),
        )

      table.foreach { (setting, read, expected) =>
        val actual = ConfigCli.fromString(setting).map(read)

        withClue(setting) {
          actual shouldBe Validated.valid(expected)
        }
      }
    }

    "keep the brace styles of the constructs when setting the braces style" in {
      val expected = Validated.valid(Config.default.braces.policies)
      val actual = ConfigCli.fromString("braces.style = stroustrup").map(_.braces.policies)
      actual shouldBe expected
    }

    "read a construct without a setting as keep" in {
      val expected = BracePolicy.Keep
      val actual = ConfigCli.default.braces(Construct.GivenAlias)
      actual shouldBe expected
    }

    "set the assignment groups" in {
      val expected =
        Validated.valid(
          List(
            Set(DefinitionKind.Val, DefinitionKind.LazyVal),
            Set(DefinitionKind.Def, DefinitionKind.GivenAlias, DefinitionKind.TypeDef),
          ),
        )

      val actual =
        ConfigCli
          .fromString("alignment.assignment-groups = [[val, lazy-val], [def, given-alias, type-def]]")
          .map(_.alignment.assignmentGroups)

      actual shouldBe expected
    }

    "set the excluded paths, keeping the included ones" in {
      val expected = Validated.valid((List("**/gen/**"), List("**")))

      val actual =
        ConfigCli
          .fromString("""paths.excluded = ["**/gen/**"]""")
          .map(paths => (paths.paths.excluded, paths.paths.included))

      actual shouldBe expected
    }

    "read a configuration that states the current version" in {
      val expected = Validated.valid(90)

      val actual =
        ConfigCli.fromString(s"config-version = \"${ConfigCli.configVersion}\", max-column = 90").map(_.maxColumn)

      actual shouldBe expected
    }

    "set where the body of each construct goes" in {
      val expected =
        Validated.valid(
          BodyConfig(
            defBody = SingleBodyPolicy.AlwaysBreak,
            valBody = SingleBodyPolicy.AlwaysJoin,
            lazyVal = SingleBodyPolicy.Keep,
            varBody = SingleBodyPolicy.AlwaysBreak,
            givenAlias = SingleBodyPolicy.AlwaysJoin,
            typeDef = SingleBodyPolicy.AlwaysBreak,
            assignment = SingleBodyPolicy.AlwaysJoin,
            namedArgument = SingleBodyPolicy.AlwaysBreak,
            lambdaArgument = SingleBodyPolicy.AlwaysJoin,
            lambdaBody = SingleBodyPolicy.AlwaysBreak,
            blockArgument = SingleBodyPolicy.AlwaysJoin,
            testBlock = SingleBodyPolicy.AlwaysBreak,
            ifBody = MultiBodyPolicy.JoinMixed,
            caseBody = MultiBodyPolicy.AlwaysJoin,
            tryBody = MultiBodyPolicy.AlwaysBreak,
            forBody = SingleBodyPolicy.AlwaysJoin,
            forYield = YieldBodyPolicy.BreakComplex,
            whileBody = SingleBodyPolicy.AlwaysJoin,
            semicolonStatements = SemicolonPolicy.Keep,
          ),
        )

      val actual =
        ConfigCli
          .fromString(
            """body {
              |  def = always-break, val = always-join, lazy-val = keep, var = always-break, given-alias = always-join
              |  type-def = always-break, assignment = always-join, named-argument = always-break
              |  lambda-argument = always-join, lambda-body = always-break, block-argument = always-join
              |  test-block = always-break, if = join-mixed, case-body = always-join, try = always-break
              |  for = always-join, for-yield = break-complex, while = always-join, semicolon-statements = keep
              |}""".stripMargin,
          )
          .map(_.body)

      actual shouldBe expected
    }

    "ignore the settings that no longer exist, with a warning" in {
      val table =
        List(
          "body.if-else = explode"      -> "body.if-else",
          "body.cases = break-together" -> "body.cases",
          "body.try-finally = keep"     -> "body.try-finally",
          "body.loops = keep"           -> "body.loops",
          "braces-style = 1tbs"         -> "braces-style",
          "body.all-constructs = keep"  -> "body.all-constructs",
          "body.after-equals = keep"    -> "body.after-equals",
        )

      table.foreach { (setting, key) =>
        val expected = Validated.valid(List(s"unknown setting $key (line 1), ignored"))

        val actual = warnings(setting)

        actual shouldBe expected
      }
    }

    "reject the values that no longer exist" in {
      val table =
        List(
          "selector-chain.inline-chains = break",
          "body.if = join",
          "body.case-body = keep",
          "body.def = break",
          "body.def = break-complex",
          "selector-chain.in-for = break",
          "selector-chain.in-call = break",
        )

      table.foreach { setting =>
        val expected = true

        val actual = ConfigCli.fromString(setting).isInvalid

        actual shouldBe expected
      }
    }

    "set the housekeeping" in {
      val expected =
        Validated.valid(
          HousekeepingConfig(trimTrailingWhitespace = false, enforceEof = false, lineEnding = LineEnding.Crlf),
        )

      val actual =
        ConfigCli
          .fromString("housekeeping { trim-trailing-whitespace = false, enforce-eof = false, line-ending = crlf }")
          .map(_.housekeeping)

      actual shouldBe expected
    }

    "set the edge blank lines of block arguments and test blocks" in {
      val expected = Validated.valid((BlankLinePolicy.Keep, BlankLinePolicy.Never))

      val actual =
        ConfigCli
          .fromString("blank-lines.body-edges { block-argument = keep, test-block = never }")
          .map(_.blankLines)
          .map(edges => (edges(Construct.BlockArgument), edges(Construct.TestBlock)))

      actual shouldBe expected
    }

    "set the brace style of block arguments and test blocks, without end markers" in {
      val expected = Validated.valid((BracePolicy.ForceIndent, BracePolicy.Keep))

      val actual =
        ConfigCli
          .fromString("braces { block-argument = force-indent, test-block = keep }")
          .map(_.braces)
          .map(braces => (braces(Construct.BlockArgument), braces(Construct.TestBlock)))

      actual shouldBe expected
    }

  }

  "an invalid override" should {

    "be rejected with an error that names the problem" in {
      val table =
        List(
          "config-version = \"1.1.0\""                        -> "later dixtra",
          "config-version = \"2.0.0\""                        -> "not read by this dixtra",
          "config-version = 1"                                -> "must be a version like",
          "selector-chain.inline-chains = many"               -> "many",
          "definition-site.groups-separator = middle"         -> "previous-group",
          "braces.test-block = force-end"                     -> "no end marker",
          "blank-lines.body-edges.if = sometimes"             -> "sometimes",
          "braces.style = gnu"                                -> "allman",
          "braces.if = sometimes"                             -> "force-braces",
          "braces.case-body = force-end"                      -> "no end marker",
          "alignment.assignment-groups = [[val], [val, def]]" -> "only one alignment group",
          """alignment.operators = ["=", " "]"""              -> "cannot be empty",
          "selector-chain.break-at-count = 1"                 -> "at least 2",
          "housekeeping.line-ending = macos"                  -> "macos",
        )

      table.foreach { (setting, problem) =>
        val actual = errors(ConfigCli.fromString(setting))
        actual should include(problem)
      }
    }

    "reject a wrong value beside an unknown key" in {
      val message = errors(ConfigCli.fromString("max-colum = 80\nmax-column = wide"))
      message should include("max-column")
    }

    "reject malformed HOCON" in {
      val expected = true
      val actual = ConfigCli.fromString("braces {").isInvalid
      actual shouldBe expected
    }

  }

  "an unknown key" should {

    "be ignored with a warning naming it and its line, the other settings read" in {
      val table =
        List(
          "max-colum = 80"                       -> "max-colum (line 1)",
          "braces.klass = force-indent"          -> "braces.klass (line 1)",
          "blank-lines.body-edges.klass = never" -> "blank-lines.body-edges.klass (line 1)",
          "alignment.case-arrows = false"        -> "alignment.case-arrows (line 1)",
          "alignment.scaladoc.params = true"     -> "alignment.scaladoc.params (line 1)",
          "alignment.arrows.for = false"         -> "alignment.arrows.for (line 1)",
          "if.brace = keep"                      -> "if.brace (line 1)",
          "\nfoo { bar = 1, baz = 2 }"           -> "foo (line 2)",
        )

      table.foreach { (setting, key) =>
        val expected =
          (Validated.valid(List(s"unknown setting $key, ignored")), Validated.valid(ConfigCli.default))

        val actual = (warnings(setting), ConfigCli.fromString(setting))

        actual shouldBe expected
      }
    }

    "leave the known settings beside it" in {
      val expected = Validated.valid(90)
      val actual = ConfigCli.fromString("max-colum = 80\nmax-column = 90").map(_.maxColumn)
      actual shouldBe expected
    }

    "be named with its file" in {
      withTempDir { dir =>
        val conf = write(dir.resolve(ConfigCli.fileName), "max-colum = 60")

        val expected = Validated.valid(List(s"${conf.toString}: unknown setting max-colum (line 1), ignored"))

        val actual = ConfigCli.loadFile(conf).map(_.warnings)

        actual shouldBe expected
      }
    }

  }

  "the configuration of a path" should {

    "be the defaults when no .dixtra.conf is above it" in {
      withTempDir { dir =>
        val source = write(dir.resolve("A.scala"), "object A\n")

        val expected = Validated.valid(ConfigCli.default.maxColumn)

        val actual = ConfigCli.discover(source).map(_.base.maxColumn)

        actual shouldBe expected
      }
    }

    "come from the nearest .dixtra.conf in a parent directory" in {
      withTempDir { dir =>
        val _      = write(dir.resolve(ConfigCli.fileName), "max-column = 60")
        val _      = write(dir.resolve("sub").resolve(ConfigCli.fileName), "max-column = 70")
        val source = write(dir.resolve("sub").resolve("deep").resolve("A.scala"), "object A\n")

        val expected = Validated.valid(70)

        val actual = ConfigCli.discover(source).map(_.base.maxColumn)

        actual shouldBe expected
      }
    }

    "report an invalid file with its path" in {
      withTempDir { dir =>
        val conf = write(dir.resolve(ConfigCli.fileName), "max-column = wide")
        val actual = errors(ConfigCli.discover(dir).map(_.base))
        actual should include(conf.toString)
      }
    }

  }

  private def errors(result: ValidatedNel[String, Config]): String =
    result.fold(_.toList.mkString("\n"), _ => "")

  // The warnings of loading `settings`
  private def warnings(settings: String): ValidatedNel[String, List[String]] =
    ConfigCli.load(settings, Paths.get("").toAbsolutePath).map(_.warnings)

}
