package com.colofabrix.scala.dixtra.cli

import cats.data.*
import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.testkit.TempFiles.*
import java.nio.file.Paths
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Loading the configuration: `reference.conf` as the defaults, HOCON overrides from a string or a file, the
 * `.dixtra.conf` lookup, and readable errors.
 */
class ConfigCliSpec extends AnyWordSpec with Matchers {

  private def errors(result: ValidatedNel[String, Config]): String =
    result.fold(_.toList.mkString("\n"), _ => "")

  // The warnings of loading `settings`
  private def warnings(settings: String): ValidatedNel[String, List[String]] =
    ConfigCli.load(settings, Paths.get("").toAbsolutePath).map(_.warnings)

  "the defaults" should {
    
    "be the hardcoded default configuration" in {
      ConfigCli.default shouldBe Config.default
    }

  }

  "an override" should {
    
    "change only the keys it sets" in {
      ConfigCli.fromString("max-column = 80") shouldBe Validated.valid(Config.withMaxColumn(80))
    }

    "set the brace style of a construct, keeping the others" in {
      val braces =
        ConfigCli
          .fromString("braces.if = force-braces")
          .map(b => (b.braces(Construct.If), b.braces(Construct.Match)))

      braces shouldBe Validated.valid((BracePolicy.ForceBraces, BracePolicy.ForceBraces))
    }

    "set the setting it names, trying every value of the settings with few values" in {
      val table: List[(String, Config => Any, Any)] =
        List(
          ("blank-lines.body-edges.match = always", _.blankLines(Construct.Match), BlankLinePolicy.Always),
          ("braces.style = allman", _.braces.style, BracesStyle.Allman),
          ("alignment.scaladoc.tags = true", _.alignment.scaladoc.tags, true),
          ("selector-chain.break-at-count = 4", _.selectorChain.breakAtCount, 4),
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
          ("alignment.arrows.for-enumerators = false", _.alignment.arrows.forEnumerators, false),
          ("alignment.extends-clauses = false", _.alignment.extendsClauses, false),
          ("alignment.extends-clauses = false", _.alignment.extendsClauses, false),
          ("alignment.across-comments = true", _.alignment.acrossComments, true),
          ("selector-chain.receiver-min-length = 3", _.selectorChain.receiverMinLength, 3),
          ("selector-chain.in-call = never-break", _.selectorChain.inCall, NestedChainPolicy.NeverBreak),
          ("selector-chain.in-call = keep", _.selectorChain.inCall, NestedChainPolicy.Keep),
          ("selector-chain.in-for = never-break", _.selectorChain.inFor, NestedChainPolicy.NeverBreak),
          ("selector-chain.in-for = keep", _.selectorChain.inFor, NestedChainPolicy.Keep),
          ("selector-chain.inline-chains = keep", _.selectorChain.inlineChains, InlineChainPolicy.Keep),
          ("selector-chain.object-member = always-join", _.selectorChain.objectMember, ChainHeadPolicy.AlwaysJoin),
          (
            "definition-site.groups-separator = previous-group",
            _.definitionSite.groupsSeparator,
            GroupsSeparator.PreviousGroup,
          ),
          ("call-site.groups-separator = next-group", _.callSite.groupsSeparator, GroupsSeparator.NextGroup),
          ("call-site.groups-break = break-all", _.callSite.groupsBreak, GroupsBreak.BreakAll),
          ("call-site.groups-break = last-first", _.callSite.groupsBreak, GroupsBreak.LastFirst),
          ("call-site.groups-break = independent", _.callSite.groupsBreak, GroupsBreak.Independent),
          ("alignment.named-arguments = true", _.alignment.namedArguments, true),
          ("alignment.trailing-comments = true", _.alignment.trailingComments, true),
          ("trailing-comment-overflow = keep", _.trailingCommentOverflow, TrailingOverflow.Keep),
          ("trailing-comment-overflow = move-above", _.trailingCommentOverflow, TrailingOverflow.MoveAbove),
          ("trailing-commas = always", _.trailingCommas, TrailingCommas.Always),
          ("trailing-commas = never", _.trailingCommas, TrailingCommas.Never),
          ("housekeeping.line-ending = lf", _.housekeeping.lineEnding, LineEnding.Lf),
          ("housekeeping.line-ending = crlf", _.housekeeping.lineEnding, LineEnding.Crlf),
          ("housekeeping.line-ending = cr", _.housekeeping.lineEnding, LineEnding.Cr),
          ("housekeeping.line-ending = keep", _.housekeeping.lineEnding, LineEnding.Keep),
        )

      table.foreach { (setting, read, expected) =>
        withClue(setting) {
          ConfigCli.fromString(setting).map(read) shouldBe Validated.valid(expected)
        }
      }
    }

    "keep the brace styles of the constructs when setting the braces style" in {
      val braces = ConfigCli.fromString("braces.style = stroustrup").map(_.braces.policies)
      braces shouldBe Validated.valid(Config.default.braces.policies)
    }

    "read a construct without a setting as keep" in {
      ConfigCli.default.braces(Construct.GivenAlias) shouldBe BracePolicy.Keep
    }

    "set the assignment groups" in {
      val groups =
        ConfigCli
          .fromString("alignment.assignment-groups = [[val, lazy-val], [def, given-alias, type]]")
          .map(_.alignment.assignmentGroups)

      groups shouldBe Validated.valid(
        List(
          Set(DefinitionKind.Val, DefinitionKind.LazyVal),
          Set(DefinitionKind.Def, DefinitionKind.GivenAlias, DefinitionKind.Type),
        ),
      )
    }

    "set the excluded paths, keeping the included ones" in {
      val output = 
        ConfigCli
          .fromString("""paths.excluded = ["**/gen/**"]""")
          .map(p => (p.paths.excluded, p.paths.included)) 

      output shouldBe Validated.valid((List("**/gen/**"), List("**")))
    }

    "read a configuration that states the current version" in {
      val config = ConfigCli.fromString(s"config-version = \"${ConfigCli.configVersion}\", max-column = 90")
      config.map(_.maxColumn) shouldBe Validated.valid(90)
    }

    "set where the body of each construct goes" in {
      val body =
        ConfigCli
          .fromString(
            """body {
              |  def = always-break, val = always-join, lazy-val = keep, var = always-break, given-alias = always-join
              |  type = always-break, assignment = always-join, named-argument = always-break
              |  lambda-argument = always-join, lambda-body = always-break, block-argument = always-join
              |  test-block = always-break, if = join-mixed, case-body = always-join, try = always-break
              |  for = always-join, for-yield = break-complex, while = always-join
              |}""".stripMargin,
          )
          .map(_.body)

      body shouldBe Validated.valid(
        BodyConfig(
          defBody = SingleBodyPolicy.AlwaysBreak,
          valBody = SingleBodyPolicy.AlwaysJoin,
          lazyVal = SingleBodyPolicy.Keep,
          varBody = SingleBodyPolicy.AlwaysBreak,
          givenAlias = SingleBodyPolicy.AlwaysJoin,
          typeBody = SingleBodyPolicy.AlwaysBreak,
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
        ),
      )
    }

    "ignore the settings that no longer exist, with a warning" in {
      List(
        "body.if-else = explode"      -> "body.if-else",
        "body.cases = break-together" -> "body.cases",
        "body.try-finally = keep"     -> "body.try-finally",
        "body.loops = keep"           -> "body.loops",
        "braces-style = 1tbs"         -> "braces-style",
        "body.all-constructs = keep"  -> "body.all-constructs",
        "body.after-equals = keep"    -> "body.after-equals",
      ).foreach { (setting, key) =>
        warnings(setting) shouldBe Validated.valid(List(s"unknown setting $key (line 1), ignored"))
      }
    }

    "reject the values that no longer exist" in {
      List(
        "selector-chain.inline-chains = break",
        "body.if = join",
        "body.case-body = keep",
        "body.def = break",
        "body.def = break-complex",
        "selector-chain.in-for = break",
        "selector-chain.in-call = break",
      ).foreach { setting =>
        ConfigCli.fromString(setting).isInvalid shouldBe true
      }
    }

    "set the housekeeping" in {
      val config =
        ConfigCli.fromString(
          "housekeeping { trim-trailing-whitespace = false, enforce-eof = false, line-ending = crlf }",
        )
      
      val expected = 
        Validated.valid(
        HousekeepingConfig(trimTrailingWhitespace = false, enforceEof = false, lineEnding = LineEnding.Crlf),
      )

      config.map(_.housekeeping) shouldBe expected
    }

    "set the edge blank lines of block arguments and test blocks" in {
      val result =
        ConfigCli
          .fromString("blank-lines.body-edges { block-argument = keep, test-block = never }")
          .map(_.blankLines)
          .map(e => (e(Construct.BlockArgument), e(Construct.TestBlock)))

      result shouldBe Validated.valid((BlankLinePolicy.Keep, BlankLinePolicy.Never))
    }

    "set the brace style of block arguments and test blocks, without end markers" in {
      val braces = ConfigCli.fromString("braces { block-argument = force-indent, test-block = keep }").map(_.braces)
      braces.map(b => (b(Construct.BlockArgument), b(Construct.TestBlock))) shouldBe
      Validated.valid((BracePolicy.ForceIndent, BracePolicy.Keep))
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
        errors(ConfigCli.fromString(setting)) should include(problem)
      }
    }

    "reject a wrong value beside an unknown key" in {
      errors(ConfigCli.fromString("max-colum = 80\nmax-column = wide")) should include("max-column")
    }

    "reject malformed HOCON" in {
      ConfigCli.fromString("braces {").isInvalid shouldBe true
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
        (warnings(setting), ConfigCli.fromString(setting)) shouldBe
        (Validated.valid(List(s"unknown setting $key, ignored")), Validated.valid(ConfigCli.default))
      }
    }

    "leave the known settings beside it" in {
      ConfigCli.fromString("max-colum = 80\nmax-column = 90").map(_.maxColumn) shouldBe Validated.valid(90)
    }

    "be named with its file" in {
      withTempDir { dir =>
        val conf = write(dir.resolve(ConfigCli.fileName), "max-colum = 60")
        ConfigCli.loadFile(conf).map(_.warnings) shouldBe
        Validated.valid(List(s"${conf.toString}: unknown setting max-colum (line 1), ignored"))
      }
    }

  }

  "the configuration of a path" should {
    
    "be the defaults when no .dixtra.conf is above it" in {
      withTempDir { dir =>
        val source = write(dir.resolve("A.scala"), "object A\n")
        ConfigCli.discover(source).map(_.base.maxColumn) shouldBe Validated.valid(ConfigCli.default.maxColumn)
      }
    }

    "come from the nearest .dixtra.conf in a parent directory" in {
      withTempDir { dir =>
        val _      = write(dir.resolve(ConfigCli.fileName), "max-column = 60")
        val _      = write(dir.resolve("sub").resolve(ConfigCli.fileName), "max-column = 70")
        val source = write(dir.resolve("sub").resolve("deep").resolve("A.scala"), "object A\n")
        ConfigCli.discover(source).map(_.base.maxColumn) shouldBe Validated.valid(70)
      }
    }

    "report an invalid file with its path" in {
      withTempDir { dir =>
        val conf = write(dir.resolve(ConfigCli.fileName), "max-column = wide")
        errors(ConfigCli.discover(dir).map(_.base)) should include(conf.toString)
      }

    }

  }

}
