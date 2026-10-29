package com.colofabrix.scala.dixtra.properties

import com.colofabrix.scala.dixtra.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Property/invariant tests: the layout principles as predicates over a corpus of representative snippets, complementing
 * the golden per-construct specs.
 *
 * For every snippet we assert:
 *  - no lonely close-then-open delimiter line
 *  - newline after `=` when the RHS is multiline
 *  - idempotency: format(format(x)) == format(x)
 *  - round-trip: parse(format(x)) is structurally equal to parse(x)
 */
class InvariantSpec extends AnyWordSpec with Matchers {

  private val corpus: Vector[String] =
    Vector(
      """val x =
        |  list.filter(_ > 0).map(_ * 2).headOption
        |""".stripMargin,
      """val xs = List(
        |  elementOne,
        |  elementTwo,
        |  elementThree
        |)
        |""".stripMargin,
      "def longName[TypeParam1, TypeParam2](parameter_1: String, parameter_2: Boolean): Int = 1\n",
      """def f =
        |  if someLongCondition then firstBranch() else secondBranch()
        |""".stripMargin,
      """object A {
        |  val xs = List(
        |    elementOne,
        |    elementTwo
        |  )
        |}""".stripMargin,
      "val x = None\n",
      "def myfunc(parameter_1: String, parameter_2: String): Int = 1\n",
      "val f: Int=>String = _.toString\n",
      "package a\n\nobject A {\n  val xs = List(a,\n    b)\n}\n",
      "val x = // important\n  42\n",
      "def f =\n  val a = 1\n  // keep me\n  a\n",
      "val x = (a + b).foo.bar\n",
      "val y = (f compose g)(x)\n",
      "val s = \"\"\"a\n  b\nc\"\"\"\n",
      "def f(x: Int) = x match {\n  case 1 =>\n    \"one\"\n  case _ => \"other\"\n}\n",
      "def someFunctionName(parameterOne: String, parameterTwo: Int): Int = xs.map(f).sum\n",
      "def f(x: Int)(implicit ctx: Context): Int = 1\n",
      "def someLongFunctionName(parameterOne: Int)(using contextOne: Ctx, contextTwo: Ctx2): Int = 1\n",
    )

  private val config = Config.default

  "the formatter" should {

    "produce no lonely close-then-open delimiter lines" in {
      corpus.foreach { src =>
        Dixtra.format(src, config) match {
          case Right(out) =>
            val expected = true
            val actual   = LayoutProperties.lonelyDelimiterViolations(out).isEmpty
            actual shouldBe expected
          case Left(err) =>
            fail(s"parse/format failed for:\n$src\nerror: ${err.getMessage}")
        }
      }
    }

    "never leave a partially broken select chain" in {
      corpus.foreach { src =>
        Dixtra.format(src, config) match {
          case Right(out) =>
            val expected = true
            val actual   = LayoutProperties.partialBreakViolations(out).isEmpty
            actual shouldBe expected
          case Left(err) =>
            fail(s"parse/format failed for:\n$src\nerror: ${err.getMessage}")
        }
      }
    }

    "put a newline after = when the RHS is multiline" in {
      corpus.foreach { src =>
        Dixtra.format(src, config) match {
          case Right(out) =>
            val expected = true
            val actual   = LayoutProperties.newlineAfterEqViolations(out).isEmpty
            actual shouldBe expected
          case Left(err) =>
            fail(s"parse/format failed for:\n$src\nerror: ${err.getMessage}")
        }
      }
    }

    "be idempotent" in {
      corpus.foreach { src =>
        val expected = true
        val actual   = LayoutProperties.isIdempotent(src, config)
        withClue(s"not idempotent for:\n$src\n") {
          actual shouldBe expected
        }
      }
    }

    "preserve program structure (round-trip)" in {
      corpus.foreach { src =>
        val expected = true
        val actual   = LayoutProperties.isStructurePreserving(src, config)

        withClue(s"structure changed for:\n$src\n") {
          actual shouldBe expected
        }
      }
    }

  }

}
