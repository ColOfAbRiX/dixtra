package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.model.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.meta.*

/**
 * The `compat213` setting: a source that is valid Scala 2.13 and Scala 3 stays valid in both after formatting.
 */
class Compat213Spec extends AnyWordSpec with Matchers {

  // A configuration that would write Scala 3 syntax, to check that compat213 overrides the settings it enforces.
  private val configured: Config =
    Config
      .default
      .copy(
        compat213 = true,
        braces = Config
          .default
          .braces
          .copy(
            policies = Construct.values.map(_ -> BracePolicy.ForceIndent).toMap,
            singleExpressions = BraceConfig.singleExprConstructs.map(_ -> BracePolicy.ForceIndent).toMap,
          ),
        body = Config.default.body.copy(semicolonStatements = SemicolonPolicy.AlwaysBreak),
      )

  private val sources: List[String] =
    List(
      "object A {\n  def f = {\n    a()\n    b()\n  }\n}\n",
      "object A {\n  val x = {\n    a()\n    b()\n  }\n}\n",
      "object A {\n  lazy val y = {\n    a()\n    b()\n  }\n}\n",
      "object A {\n  var z = {\n    a()\n    b()\n  }\n}\n",
      "object A {\n  def f =\n    if (c) {\n      a()\n    } else {\n      b()\n    }\n}\n",
      "object A {\n  def f =\n    try {\n      a()\n    } catch {\n      case e: Exception => b()\n    }\n}\n",
      "object A {\n  def f =\n    try a()\n    catch {\n      case e: Exception => b()\n    }\n}\n",
      "object A {\n  def f =\n    for (x <- xs) {\n      a(x)\n      b(x)\n    }\n}\n",
      "object A {\n  def f =\n    while (i < 10) {\n      i += 1\n    }\n}\n",
      "object A {\n  def f(x: Int) =\n    x match {\n      case 1 => a\n      case _ => b\n    }\n}\n",
      "package a\n\nobject B {\n  val x = 1\n}\n",
      "class A {\n  def f = 1\n}\n",
      "class A(s: String) {\n  def this() = { this(\"a\"); println(s) }\n}\n",
      "class A(m: String) {\n  def this() = {\n    this(\"a\")\n    println(m)\n  }\n}\n",
      "object A {\n  val g =\n    (x: Int) => {\n      val y = x\n      y + 1\n    }\n}\n",
    )

  private def formatted(source: String): String =
    Dixtra.format(source, configured) match {
      case Right(output) => output
      case Left(error)   => fail(s"formatting failed: ${error.getMessage}")
    }

  private def parsesAsScala213(source: String): Boolean =
    given Dialect = dialects.Scala213

    source.parse[Source] match {
      case Parsed.Success(_) => true
      case _                 => false
    }

  "the compat213 setting" should {

    "keep the Scala 2 form of every construct, forcing braces only on package and try" in {
      val config =
        Config
          .default
          .copy(compat213 = true)
          .effective

      val policies =
        Construct
          .values
          .map { construct =>
            val policy =
              if construct == Construct.Package || construct == Construct.Try then BracePolicy.ForceBraces
              else BracePolicy.Keep

            construct -> policy
          }
          .toMap

      val expected = (policies, BraceConfig.singleExprConstructs.map(_ -> BracePolicy.Keep).toMap, SemicolonPolicy.Keep)

      val actual =
        (
          Construct.values.map(construct => construct -> config.braces(construct)).toMap,
          BraceConfig.singleExprConstructs.map(construct => construct -> config.braces.singleExpr(construct)).toMap,
          config.body.semicolonStatements,
        )

      actual shouldBe expected
    }

    "override the configured values, keeping the style and the import braces" in {
      val config =
        Config
          .default
          .copy(
            compat213 = true,
            braces = BraceConfig(
              BracesStyle.Allman,
              ImportBraces.ForceBraces,
              Construct.values.map(_ -> BracePolicy.ForceBraces).toMap,
              BraceConfig.singleExprConstructs.map(_ -> BracePolicy.ForceBraces).toMap,
            ),
            body = Config.default.body.copy(semicolonStatements = SemicolonPolicy.AlwaysBreak),
          )
          .effective

      val expected =
        (
          BracesStyle.Allman,
          ImportBraces.ForceBraces,
          BracePolicy.Keep,
          BracePolicy.Keep,
          SemicolonPolicy.Keep,
        )

      val actual =
        (
          config.braces.style,
          config.braces.imports,
          config.braces(Construct.Def),
          config.braces.singleExpr(Construct.Def),
          config.body.semicolonStatements,
        )

      actual shouldBe expected
    }

  }

  "a source that is valid Scala 2.13 and Scala 3" should {

    "stay valid in both, whatever the configured brace settings" in {
      sources.foreach { source =>
        withClue(source) {
          val output = formatted(source)

          val expected = (true, true)
          val actual   = (parsesAsScala213(output), Dixtra.format(output, configured) == Right(output))

          actual shouldBe expected
        }
      }
    }

    "keep the braces of a block that Scala 2 needs" in {
      val source = "def f = {\n  a()\n  b()\n}\n"
      val actual = formatted(source)

      actual should include("def f = {")
    }

    "give braces to a try whose Scala 2 form has none" in {
      val source = "def f =\n  try a()\n  catch {\n    case e: Exception => b()\n  }\n"
      val actual = formatted(source)

      actual should include("try {")
    }

    "give braces to the package clause" in {
      val source = "package a\n\nobject B {\n  val x = 1\n}\n"
      val actual = formatted(source)

      actual should include("package a {")
    }

    "keep the statements of a one-line block on one line" in {
      val source = "class A(s: String) {\n  def this() = { this(\"a\"); println(s) }\n}\n"
      val actual = formatted(source)

      actual should include("this(\"a\"); println(s)")
    }

    "keep the braces of a multi-line secondary constructor" in {
      val source = "class A(m: String) {\n  def this() = {\n    this(\"a\")\n    println(m)\n  }\n}\n"
      val actual = formatted(source)

      actual should include("def this() = {")
    }

  }

  "without compat213" should {

    "write Scala 3 syntax that Scala 2.13 rejects" in {
      val source = "object A {\n  def f =\n    if (c) {\n      a()\n    } else {\n      b()\n    }\n}\n"

      val output =
        Dixtra.format(source, Config.default) match {
          case Right(formatted) => formatted
          case Left(error)      => fail(s"formatting failed: ${error.getMessage}")
        }

      parsesAsScala213(output) shouldBe false
    }

  }

}
