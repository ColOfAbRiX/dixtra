package com.colofabrix.scala.dixtra.properties

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.model.DefinitionKind.*
import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import com.colofabrix.scala.dixtra.verify.Equivalence
import java.nio.charset.StandardCharsets
import java.nio.file.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.util.Random

/**
 * The formatter's guarantees under many configurations, not just the default.
 *
 * Every setting combination is out of reach, so the configurations form a pairwise covering array: every value of every
 * setting meets every value of every other setting in at least one configuration. Over a corpus touching each
 * construct, each configuration must preserve the program structure and be idempotent. Brace styles must also converge:
 * converting to braces, away from them and back gives the first conversion again.
 */
class ConfigInvariantSpec extends AnyWordSpec with Matchers with FormatMatchers {

  private val corpus: List[String] =
    List(
      "class A {\n  val x = 1\n  val longer = 2\n  def f = 3\n}\n",
      "object A:\n  val x = 1\n  lazy val y = 2\n  var z = 3\nend A\n",
      "case class P(x: Int):\n  def y = x\n",
      "trait T {\n  def x: Int\n  type U = Int\n}\n",
      "enum Color {\n  case Red, Green\n}\n",
      "extension (x: Int) {\n  def a = x\n  def b = x\n}\n",
      "given Ordering[Int] with {\n  def compare(a: Int, b: Int) = a - b\n}\n",
      "package a\n\nobject B {\n  given g: Int = 1\n}\n",
      "def f(x: Int) = {\n  println(x)\n  x match {\n    case 1 => a\n    case 100 => b\n  }\n}\n",
      "def f =\n  try\n    a()\n  catch\n    case e: Exception => b()\n  finally\n    c()\n",
      "def f =\n  if (a) {\n    b\n  } else if (c) {\n    d\n  } else {\n    e\n  }\n",
      "def f =\n  for\n    a <- xs\n    if a > 0\n    bbb <- ys\n  yield a + bbb\n",
      "def f =\n  for {\n    a <- xs\n    b <- ys\n  } {\n    println(a)\n  }\n",
      "def f =\n  while (i < 10) {\n    i += 1\n    j += 1\n  }\n",
      "val r =\n  new Runnable {\n    def run() = println(1)\n  }\n",
      "def f =\n  xs.map { x =>\n    val y = x\n    y\n  }.filter(_ > 0).sum\n",
      "val g =\n  (x: Int) => {\n    val y = x\n    y\n  }\n",
      "val m =\n  Map(\n    1 -> a,\n    100 -> b,\n  )\n",
      "def f =\n  total += x\n  count = 0\n  longerName := 2\n",
      "def f =\n  // note\n  a(1) // one\n  bbb(2) // two\n" +
      "  c(3) // a trailing comment long enough to go past the maximum width of the line when aligned\n\n" +
      "  d(4) // four\n",
      "class VeryLongServiceName(firstDependency: Dependency, secondDependency: Dependency) extends ServiceBase" +
      " with Logging {\n  def run() = 1\n}\n",
    ) ++
    List(
      "Config.scala",
      "Dixtra.scala",
      "model/Alignment.scala",
      "model/Bodies.scala",
      "model/BlankLines.scala",
      "model/Braces.scala",
      "model/Chains.scala",
      "model/Construct.scala",
      "model/Housekeeping.scala",
      "model/Paths.scala",
      "model/Sites.scala",
    ).map { name =>
      val path = Paths.get("src", "main", "scala", "com", "colofabrix", "scala", "dixtra").resolve(name)
      new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
    }

  // One setting: its name and the configurations it can apply to a config.
  final private case class Setting(name: String, values: Vector[Config => Config])

  private val braceSettings: List[Setting] =
    Construct
      .values
      .toList
      .map { construct =>
        val allowed =
          BracePolicy
            .values
            .toVector
            .filter(p => construct.allowsEnd || p != BracePolicy.ForceEnd)

        Setting(construct.key, allowed.map(policy => (c: Config) => withBrace(c, construct, policy)))
      }

  private val otherSettings: List[Setting] =
    List(
      Setting(
        "braces.style",
        BracesStyle.values.toVector.map(s => (c: Config) => c.copy(braces = c.braces.copy(style = s))),
      ),
      Setting(
        "break-at-count",
        Vector(2, 3, 4).map(n => (c: Config) => withSelectorChain(_.copy(breakAtCount = n), c)),
      ),
      Setting(
        "groups",
        Vector(
          List(Set(Val, LazyVal, Var, Def)),
          List(Set(Val, LazyVal, Var), Set(Def)),
          List(Set(Val, LazyVal, Var, Def, GivenAlias, TypeDef)),
          Nil,
        ).map(groups => (c: Config) => alignment(c)(_.copy(assignmentGroups = groups))),
      ),
      Setting(
        "before-scaladoc",
        ScaladocSpacing.values.toVector.map(p => (c: Config) => withBlankLines(_.copy(beforeScaladoc = p), c)),
      ),
      Setting(
        "between-alignment-groups",
        BlankLinePolicy.values.toVector.map(p => (c: Config) => withBlankLines(_.copy(betweenAlignmentGroups = p), c)),
      ),
      Setting(
        "arrows.cases",
        Vector(true, false).map(b => (c: Config) => arrows(c)(_.copy(cases = b))),
      ),
      Setting(
        "arrows.pairs",
        Vector(true, false).map(b => (c: Config) => arrows(c)(_.copy(pairs = b))),
      ),
      Setting(
        "arrows.for-enumerators",
        Vector(true, false).map(b => (c: Config) => arrows(c)(_.copy(forEnumerators = b))),
      ),
      Setting(
        "operators",
        Vector(Config.default.alignment.operators, Nil, List("=", ":="))
          .map(ops => (c: Config) => alignment(c)(_.copy(operators = ops))),
      ),
      Setting(
        "scaladoc.tags",
        Vector(true, false).map(b => (c: Config) => alignment(c)(_.copy(scaladoc = ScaladocAlignmentConfig(b)))),
      ),
      Setting(
        "extends-clauses",
        Vector(true, false).map(b => (c: Config) => alignment(c)(_.copy(extendsClauses = b))),
      ),
      Setting(
        "named-arguments",
        Vector(true, false).map(b => (c: Config) => alignment(c)(_.copy(namedArguments = b))),
      ),
      Setting(
        "trailing-comments",
        Vector(true, false).map(b => (c: Config) => alignment(c)(_.copy(trailingComments = b))),
      ),
      Setting(
        "definition-site.trailing-commas",
        TrailingCommas.values.toVector.map(p => (c: Config) => withDefinitionSite(_.copy(trailingCommas = p), c)),
      ),
      Setting(
        "call-site.trailing-commas",
        TrailingCommas.values.toVector.map(p => (c: Config) => withCallSite(_.copy(trailingCommas = p), c)),
      ),
      Setting(
        "overflow.trailing-comment",
        TrailingOverflow.values.toVector.map(p => (c: Config) => c.copy(overflow = OverflowConfig(p))),
      ),
      Setting(
        "trim-trailing-whitespace",
        Vector(true, false).map(
          b => (c: Config) => c.copy(housekeeping = c.housekeeping.copy(trimTrailingWhitespace = b)),
        ),
      ),
      Setting(
        "enforce-eof",
        Vector(true, false).map(b => (c: Config) => c.copy(housekeeping = c.housekeeping.copy(enforceEof = b))),
      ),
      Setting(
        "across-comments",
        Vector(true, false).map(b => (c: Config) => alignment(c)(_.copy(acrossComments = b))),
      ),
      Setting(
        "receiver-min-length",
        Vector(1, 3).map(n => (c: Config) => withSelectorChain(_.copy(receiverMinLength = n), c)),
      ),
      body(
        "def",
        (b, p) => b.copy(defBody = p),
      ),
      body(
        "val",
        (b, p) => b.copy(valBody = p),
      ),
      body(
        "lazy-val",
        (b, p) => b.copy(lazyVal = p),
      ),
      body(
        "var",
        (b, p) => b.copy(varBody = p),
      ),
      body(
        "given-alias",
        (b, p) => b.copy(givenAlias = p),
      ),
      body(
        "type-def",
        (b, p) => b.copy(typeDef = p),
      ),
      body(
        "assignment",
        (b, p) => b.copy(assignment = p),
      ),
      body(
        "named-argument",
        (b, p) => b.copy(namedArgument = p),
      ),
      body(
        "lambda-argument",
        (b, p) => b.copy(lambdaArgument = p),
      ),
      body(
        "lambda-body",
        (b, p) => b.copy(lambdaBody = p),
      ),
      body(
        "block-argument",
        (b, p) => b.copy(blockArgument = p),
      ),
      body(
        "test-group",
        (b, p) => b.copy(testGroup = p),
      ),
      body(
        "test-case",
        (b, p) => b.copy(testCase = p),
      ),
      multiBody(
        "if",
        (b, p) => b.copy(ifBody = p),
      ),
      multiBody(
        "case-body",
        (b, p) => b.copy(caseBody = p),
      ),
      multiBody(
        "try",
        (b, p) => b.copy(tryBody = p),
      ),
      body(
        "for",
        (b, p) => b.copy(forBody = p),
      ),
      yieldBody(
        "for-yield",
        (b, p) => b.copy(forYield = p),
      ),
      body(
        "while",
        (b, p) => b.copy(whileBody = p),
      ),
      Setting(
        "semicolon-statements",
        SemicolonPolicy.values.toVector.map(p => (c: Config) => c.copy(body = c.body.copy(semicolonStatements = p))),
      ),
      Setting(
        "definition-site.groups-separator",
        GroupsSeparator.values.toVector.map(g => (c: Config) => withDefinitionSite(_.copy(groupsSeparator = g), c)),
      ),
      Setting(
        "call-site.groups-separator",
        GroupsSeparator.values.toVector.map(g => (c: Config) => withCallSite(_.copy(groupsSeparator = g), c)),
      ),
      Setting(
        "call-site.groups-break",
        GroupsBreak.values.toVector.map(g => (c: Config) => withCallSite(_.copy(groupsBreak = g), c)),
      ),
      Setting(
        "inline-chains",
        InlineChainPolicy.values.toVector.map(p => (c: Config) => withSelectorChain(_.copy(inlineChains = p), c)),
      ),
      Setting(
        "in-for",
        NestedChainPolicy.values.toVector.map(p => (c: Config) => withSelectorChain(_.copy(inFor = p), c)),
      ),
      Setting(
        "in-call",
        NestedChainPolicy.values.toVector.map(p => (c: Config) => withSelectorChain(_.copy(inCall = p), c)),
      ),
      Setting(
        "namespace-path",
        ChainHeadPolicy.values.toVector.map(p => (c: Config) => withSelectorChain(_.copy(namespacePath = p), c)),
      ),
      Setting(
        "object-member",
        ChainHeadPolicy.values.toVector.map(p => (c: Config) => withSelectorChain(_.copy(objectMember = p), c)),
      ),
    )

  private val edgeSettings: List[Setting] =
    Construct
      .values
      .toList
      .map { construct =>
        Setting(
          s"blank-lines.${construct.key}",
          BlankLinePolicy
            .values
            .toVector
            .map(
              policy =>
                (c: Config) =>
                  c.copy(blankLines = c.blankLines.copy(bodyEdges = c.blankLines.bodyEdges + (construct -> policy))),
            ),
        )
      }

  private val settings: Vector[Setting] =
    (braceSettings ++ otherSettings ++ edgeSettings).toVector

  private def body(name: String, change: (BodyConfig, SingleBodyPolicy) => BodyConfig): Setting =
    Setting(name, SingleBodyPolicy.values.toVector.map(p => (c: Config) => c.copy(body = change(c.body, p))))

  private def yieldBody(name: String, change: (BodyConfig, YieldBodyPolicy) => BodyConfig): Setting =
    Setting(name, YieldBodyPolicy.values.toVector.map(p => (c: Config) => c.copy(body = change(c.body, p))))

  private def multiBody(name: String, change: (BodyConfig, MultiBodyPolicy) => BodyConfig): Setting =
    Setting(name, MultiBodyPolicy.values.toVector.map(p => (c: Config) => c.copy(body = change(c.body, p))))

  private def withBrace(config: Config, construct: Construct, policy: BracePolicy): Config =
    config.copy(braces = config.braces.copy(policies = config.braces.policies + (construct -> policy)))

  private def alignment(config: Config)(change: AlignmentConfig => AlignmentConfig): Config =
    withAlignment(change, config)

  private def arrows(config: Config)(change: ArrowsConfig => ArrowsConfig): Config =
    withArrows(change, config)

  private def allBraces(policy: BracePolicy): Config =
    Construct
      .values
      .foldLeft(Config.default) { (config, construct) =>
        withBrace(
          config,
          construct,
          if construct.allowsEnd || policy != BracePolicy.ForceEnd then policy else BracePolicy.ForceIndent,
        )
      }

  // A greedy pairwise covering array (AETG): each row is the best of a few random candidates at covering the value
  // pairs no row covers yet. The seed keeps the configurations the same from run to run.
  private val rows: List[Vector[Int]] =
    val random = new Random(20261005L)

    val pairs =
      for
        leftSetting  <- settings.indices
        rightSetting <- settings.indices if leftSetting < rightSetting
        leftValue    <- settings(leftSetting).values.indices
        rightValue   <- settings(rightSetting).values.indices
      yield
        (leftSetting, leftValue, rightSetting, rightValue)

    def covered(row: Vector[Int]): Set[(Int, Int, Int, Int)] =
      (for
        leftSetting  <- settings.indices
        rightSetting <- settings.indices if leftSetting < rightSetting
      yield
        (leftSetting, row(leftSetting), rightSetting, row(rightSetting))).toSet

    @scala.annotation.tailrec
    def loop(uncovered: Set[(Int, Int, Int, Int)], acc: List[Vector[Int]]): List[Vector[Int]] =
      if uncovered.isEmpty then
        acc.reverse
      else
        // Seed each candidate with one uncovered pair so every row makes progress
        val (leftSetting, leftValue, rightSetting, rightValue) = uncovered.head

        val candidates =
          List.fill(30)(
            settings
              .map(s => random.nextInt(s.values.length))
              .updated(leftSetting, leftValue)
              .updated(rightSetting, rightValue),
          )

        val best = candidates.maxBy(row => covered(row).count(uncovered.contains))

        loop(uncovered -- covered(best), best :: acc)

    loop(pairs.toSet, Nil)

  private def equivalent(original: String, formatted: String): Boolean =
    import scala.meta.*

    given Dialect = dialects.Scala3
    original
      .parse[Source]
      .toOption
      .exists(Equivalence.isEquivalentSource(_, original, formatted))

  private def configOf(row: Vector[Int]): Config =
    settings.zip(row).foldLeft(Config.default) { case (c, (setting, value)) => setting.values(value)(c) }

  private def describe(row: Vector[Int]): String =
    settings
      .zip(row)
      .map((s, v) => s"${s.name}#${v.toString}")
      .mkString(" ")

  "the pairwise configurations" should {

    "cover every pair of setting values in a few dozen configurations" in {
      val expected = true
      val actual   = rows.length < 80
      actual shouldBe expected
    }

    rows
      .zipWithIndex
      .foreach { (row, configIndex) =>
        s"preserve structure and be idempotent with configuration ${configIndex.toString}" in {
          val config = configOf(row)

          val expected = true

          val actual =
            corpus
              .zipWithIndex
              .flatMap { (src, idx) =>
                Dixtra.format(src, config) match {
                  case Left(error) =>
                    List(s"snippet ${idx.toString} fails: ${error.getMessage}")
                  case Right(once) if !equivalent(src, once) =>
                    List(s"snippet ${idx.toString} changes structure:\n$once")
                  case Right(once) if Dixtra.format(once, config) != Right(once) =>
                    List(
                      s"snippet ${idx.toString} is not idempotent:\n$once\n---\n${Dixtra.format(once, config).toString}",
                    )
                  case Right(_) =>
                    Nil
                }
              }
              .isEmpty

          withClue(s"${describe(row)}\n") {
            actual shouldBe expected
          }
        }
      }

  }

  // Formatting with `there`, then `back`, then `there` again gives the first result
  private def converges(source: String, there: Config, back: Config): Boolean =
    val first = Dixtra.format(source, there)
    first.flatMap(Dixtra.format(_, back)).flatMap(Dixtra.format(_, there)) == first

  "the brace styles" should {

    "converge from braces to braceless and back" in {
      val braces    = allBraces(BracePolicy.ForceBraces)
      val braceless = allBraces(BracePolicy.ForceIndent)
      val expected  = true

      val actual = corpus.filterNot(converges(_, braces, braceless)).isEmpty

      actual shouldBe expected
    }

    "converge from end markers to braces and back" in {
      val braces   = allBraces(BracePolicy.ForceBraces)
      val ended    = allBraces(BracePolicy.ForceEnd)
      val expected = true

      val actual = corpus.filterNot(converges(_, ended, braces)).isEmpty

      actual shouldBe expected
    }

  }

}
