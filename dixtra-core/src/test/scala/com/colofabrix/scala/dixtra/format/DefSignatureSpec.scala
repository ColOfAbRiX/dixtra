package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Method definition signatures (P6).
 *
 * Tier 1: value params break, type params stay inline. Tier 2: type params break with `](param` gluing. Multi-clause
 * `))(param` gluing, `using` and `implicit` clauses, empty clauses, and the interplay between the signature and a body
 * that does not fit after `=` are also covered.
 */
class DefSignatureSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "Tier 1 (value params break, type params inline)" should {

    "break value params but keep type params inline" in {
      val input =
        "def longName[TypeParam1, TypeParam2](parameter_1: String, parameter_2: Boolean): Int = 1\n"

      val expected =
        """def longName[TypeParam1, TypeParam2](
          |  parameter_1: String,
          |  parameter_2: Boolean,
          |): Int = 1
          |""".stripMargin

      input should formatTo(expected, 40)
    }

  }

  "an abstract def" should {

    "break its value params like a def" in {
      val input =
        "trait A {\n  def longName[TypeParam1](parameter_1: String, parameter_2: Boolean): Int\n}\n"

      val expected =
        """trait A {
          |  def longName[TypeParam1](
          |    parameter_1: String,
          |    parameter_2: Boolean,
          |  ): Int
          |}
          |""".stripMargin

      input should formatTo(expected, 50)
    }

    "join params written over several lines when they fit" in {
      val input    = "trait A {\n  def f(\n    a: Int,\n    b: Int,\n  ): Int\n}\n"
      val expected = "trait A {\n  def f(a: Int, b: Int): Int\n}\n"
      input should formatTo(expected)
    }

  }

  "a given alias with parameters" should {

    "break its using clause like a def" in {
      val input = "given showList[A](using showElement: Show[A], ordering: Ordering[A]): Show[List[A]] = ShowList()\n"

      val expected =
        """given showList[A](using
          |  showElement: Show[A],
          |  ordering: Ordering[A],
          |): Show[List[A]] = ShowList()
          |""".stripMargin

      input should formatTo(expected, 60)
    }

  }

  "Tier 2 (type params break)" should {

    "break type params and glue ](param" in {
      val input =
        "def longName[TypeParam1, TypeParam2, TypeParam3](parameter_1: String, parameter_2: Boolean): Int = 1\n"

      val expected =
        """def longName[
          |  TypeParam1,
          |  TypeParam2,
          |  TypeParam3,
          |](parameter_1: String,
          |  parameter_2: Boolean,
          |): Int = 1
          |""".stripMargin

      input should formatTo(expected, 40)
    }

  }

  "short signature (must stay inline)" should {

    "keep a fitting signature on a single line, also with a using or an implicit clause" in {
      List(
        "def myfunc(parameter_1: String, parameter_2: String): Int = 1\n",
        "def myfunc[A](parameter_1: String)(using Numeric[A]): Int = 1\n",
        "def f(x: Int)(implicit ctx: Context): Int = 1\n",
      ).foreach { input =>
        val expected = input
        input should formatTo(expected)
      }
    }

  }

  "multi-clause signature" should {

    "glue )(param when broken" in {
      val input =
        "def longFunctioName[TypeParam](parameter_1: String, parameter_2: Boolean)(numeric: Numeric[TypeParam]): Int" +
        " = 1\n"

      val expected =
        """def longFunctioName[TypeParam](
          |  parameter_1: String,
          |  parameter_2: Boolean,
          |)(numeric: Numeric[TypeParam],
          |): Int = 1
          |""".stripMargin

      input should formatTo(expected, 80)
    }

  }

  "definition-site.groups-separator = previous-group" should {

    val lastParameter =
      Config.withMaxColumn(80).copy(definitionSite = DefinitionSiteConfig(GroupsSeparator.PreviousGroup))

    "close a broken clause on its last parameter and open the next one there" in {
      val input =
        "def longFunctioName[TypeParam](parameter_1: String, parameter_2: Boolean)(numeric: Numeric[TypeParam]): Int" +
        " = 1\n"

      val expected =
        """def longFunctioName[TypeParam](
          |  parameter_1: String,
          |  parameter_2: Boolean)(
          |  numeric: Numeric[TypeParam],
          |): Int = 1
          |""".stripMargin

      input should formatStablyTo(expected, lastParameter)
    }

    "keep the using keyword on the junction line" in {
      val input =
        "def longFunctioName[TypeParam](parameter_1: String, parameter_2: Boolean)(using Numeric[TypeParam]): Int = 1\n"

      val expected =
        """def longFunctioName[TypeParam](
          |  parameter_1: String,
          |  parameter_2: Boolean)(using
          |  Numeric[TypeParam],
          |): Int = 1
          |""".stripMargin

      input should formatStablyTo(expected, lastParameter)
    }

    "join every junction of three clauses" in {
      val input =
        "def someLongFunctionName(parameterOne: Int)(parameterTwo: String)(using contextOne: Ctx): Int = 1\n"

      val config = Config.withMaxColumn(40).copy(definitionSite = lastParameter.definitionSite)

      val expected =
        """def someLongFunctionName(
          |  parameterOne: Int)(
          |  parameterTwo: String)(using
          |  contextOne: Ctx,
          |): Int = 1
          |""".stripMargin

      input should formatStablyTo(expected, config)
    }

    "keep a signature that fits on one line" in {
      val input    = "def f(x: Int)(y: Int): Int = 1\n"
      val expected = input
      input should formatTo(expected, lastParameter)
    }

  }

  "using clause" should {

    "place using on its own line with the return type on a new line" in {
      val input =
        "def longFunctioName[TypeParam](parameter_1: String, parameter_2: Boolean)(using Numeric[TypeParam]): Int = 1\n"

      val expected =
        """def longFunctioName[TypeParam](
          |  parameter_1: String,
          |  parameter_2: Boolean,
          |)(using
          |  Numeric[TypeParam],
          |): Int = 1
          |""".stripMargin

      input should formatTo(expected, 80)
    }

  }

  "an implicit clause" should {

    "keep the implicit keyword on the opening line when exploded" in {
      val input = "def someName(parameterOne: Int)(implicit ctx: Context): Int = 1\n"

      val expected =
        """def someName(
          |  parameterOne: Int,
          |)(implicit
          |  ctx: Context,
          |): Int = 1
          |""".stripMargin

      input should formatTo(expected, 40)
    }

  }

  "a using clause with several parameters" should {

    "separate the parameters with commas" in {
      val input =
        "def someLongFunctionName(parameterOne: Int)(using contextOne: Ctx, contextTwo: Ctx2): Int = 1\n"

      val expected =
        """def someLongFunctionName(
          |  parameterOne: Int,
          |)(using
          |  contextOne: Ctx,
          |  contextTwo: Ctx2,
          |): Int = 1
          |""".stripMargin

      input should formatTo(expected, 80)
    }

  }

  "an empty parameter clause" should {

    "never explode into a whitespace-only line" in {
      val input = "def someVeryLongFunctionNameThatIsReallyLongIndeed(): SomeReallyLongReturnType = 1\n"

      val expected =
        """def someVeryLongFunctionNameThatIsReallyLongIndeed(): SomeReallyLongReturnType =
          |  1
          |""".stripMargin

      input should formatTo(expected, 80)
    }

    "stay glued when a later clause explodes" in {
      val input = "def f()(alpha: Int, beta: Int): Int = 1\n"

      val expected =
        """def f()(
          |  alpha: Int,
          |  beta: Int,
          |): Int = 1
          |""".stripMargin

      input should formatTo(expected, 30)
    }

  }

  "an annotated def" should {

    "measure its signature from the def line, not the annotation" in {
      val input    = "@deprecated(\"x\", \"1\")\ndef f(a: Int): Int = 1\n"
      val expected = input
      input should formatTo(expected, 30)
    }

  }

  "a body that does not fit after =" should {

    "keep a fitting signature on one line and drop the body below =" in {
      val input =
        "private def errorMessage(error: Throwable): String = Option(error.getMessage).getOrElse(error.toString)\n"

      val expected =
        """private def errorMessage(error: Throwable): String =
          |  Option(error.getMessage).getOrElse(error.toString)
          |""".stripMargin

      input should formatTo(expected, 80)
    }

    "not explode the parameters of a fitting signature when the body is below =" in {
      val input =
        """def startsMethod(layout: LineLayout, i: Int): Boolean =
          |  !layout.insideToken(i) && defStart.findFirstIn(layout.lines(i)).isDefined && ok
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

  }

  "a body that would change below =" should {

    "stay on the ): Type = line of an exploded signature with inline-chains = keep" in {
      val input = "def someFunctionName(parameterOne: String, parameterTwo: Int): Int = xs.map(f).filter(p).sum\n"

      val expected =
        """def someFunctionName(
          |  parameterOne: String,
          |  parameterTwo: Int,
          |): Int = xs.map(f).filter(p).sum
          |""".stripMargin

      input should formatStablyTo(expected, keepInlineChains(Config.withMaxColumn(80)))
    }

  }

  "type parameters with bounds" should {

    "break between type parameters but never inside a bound" in {
      val input =
        "def something[VeryLongTypeA <: VeryLongTypeB, VeryLongTypeC: VeryLongTypeD](something: Int): Int = 1\n"

      val expected =
        """def something[
          |  VeryLongTypeA <: VeryLongTypeB,
          |  VeryLongTypeC: VeryLongTypeD,
          |](something: Int,
          |): Int = 1
          |""".stripMargin

      input should formatTo(expected, 40)
    }

    "join a bound written over several lines" in {
      val input =
        """def something[VeryLongTypeA <:
          |    VeryLongTypeB, C](something: Int): Int = 1
          |""".stripMargin

      val expected =
        """def something[
          |  VeryLongTypeA <: VeryLongTypeB,
          |  C,
          |](something: Int,
          |): Int = 1
          |""".stripMargin

      input should formatTo(expected, 40)
    }

  }

  "higher-kinded type parameters" should {

    "stay on one line when they fit" in {
      val input = "def something[F[_, _]: Async, G[_]](something: Int): Int = 1\n"

      val expected =
        """def something[F[_, _]: Async, G[_]](
          |  something: Int,
          |): Int = 1
          |""".stripMargin

      input should formatTo(expected, 40)
    }

    "break their own type parameters only when their line does not fit" in {
      val input =
        "def something[Effect[VeryLongTypeA, VeryLongTypeB] <: Bound, G[_]](something: Int): Int = 1\n"

      val expected =
        """def something[
          |  Effect[
          |    VeryLongTypeA,
          |    VeryLongTypeB,
          |  ] <: Bound,
          |  G[_],
          |](something: Int,
          |): Int = 1
          |""".stripMargin

      input should formatTo(expected, 40)
    }

  }

}
