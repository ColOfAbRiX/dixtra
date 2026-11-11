package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.model.BracesStyle.*
import com.colofabrix.scala.dixtra.normalize.BraceStyleNormalizer
import com.colofabrix.scala.dixtra.testkit.LayoutRows
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The braces styles (`braces.style`), one row at a time: each row formats an input with one style and checks the
 * output, then that the output is stable.
 */
class BracesStyleSettingSpec extends AnyWordSpec with Matchers with LayoutRows {

  // The braces `style`, with every construct keeping its braces as written so only the style changes them.
  private def styled(style: BracesStyle): Config =
    Config.default.copy(braces = BraceConfig(style, ImportBraces.Keep, Map.empty, Map.empty))

  private val ifBody =
    "def f =\n  if (c) {\n    g()\n  } else if (d) {\n    h()\n  } else {\n    k()\n  }\n"

  private val ifElseStroustrup =
    "def f =\n  if (c) {\n    g()\n  }\n  else if (d) {\n    h()\n  }\n  else {\n    k()\n  }\n"

  private val ifElseAllman =
    "def f =\n  if (c)\n  {\n    g()\n  }\n  else if (d)\n  {\n    h()\n  }\n  else\n  {\n    k()\n  }\n"

  private val tryCatch =
    "def f =\n  try {\n    g()\n  } catch {\n    case e: Exception => h()\n  } finally {\n    k()\n  }\n"

  private val tryCatchStroustrup =
    "def f =\n  try {\n    g()\n  }\n  catch {\n    case e: Exception => h()\n  }\n  finally {\n    k()\n  }\n"

  private val tryCatchAllman =
    "def f =\n  try\n  {\n    g()\n  }\n  catch\n  {\n    case e: Exception => h()\n  }\n  finally\n  {\n    k()\n  }\n"

  private val forYield =
    "def f =\n  for {\n    a <- xs\n    b <- ys\n  } yield {\n    g(a)\n    h(b)\n  }\n"

  private val forYieldStroustrup =
    "def f =\n  for {\n    a <- xs\n    b <- ys\n  }\n  yield {\n    g(a)\n    h(b)\n  }\n"

  private val forYieldAllman =
    "def f =\n  for\n  {\n    a <- xs\n    b <- ys\n  }\n  yield\n  {\n    g(a)\n    h(b)\n  }\n"

  private val container =
    "class A extends B {\n  def f = 1\n}\n"

  private val containerAllman =
    "class A extends B\n{\n  def f = 1\n}\n"

  private val matchBody =
    "def f(x: Int) =\n  x match {\n    case 1 => a\n    case _ => b\n  }\n"

  private val matchBodyAllman =
    "def f(x: Int) =\n  x match\n  {\n    case 1 => a\n    case _ => b\n  }\n"

  private val lambdaArgument =
    "def f =\n  xs.foreach { x =>\n    g(x)\n    h(x)\n  }\n"

  private val rows: List[(String, BracesStyle, String, String)] =
    List(
      ("if/else", OneTrueBrace, ifElseStroustrup, ifBody),
      ("if/else", OneTrueBrace, ifElseAllman, ifBody),
      ("if/else", Stroustrup, ifBody, ifElseStroustrup),
      ("if/else", Allman, ifBody, ifElseAllman),
      ("try/catch/finally", OneTrueBrace, tryCatchAllman, tryCatch),
      ("try/catch/finally", Stroustrup, tryCatch, tryCatchStroustrup),
      ("try/catch/finally", Allman, tryCatch, tryCatchAllman),
      ("for/yield", OneTrueBrace, forYieldStroustrup, forYield),
      ("for/yield", Stroustrup, forYield, forYieldStroustrup),
      ("for/yield", Allman, forYield, forYieldAllman),
      ("a container", OneTrueBrace, containerAllman, container),
      ("a container", Stroustrup, container, container),
      ("a container", Allman, container, containerAllman),
      ("a match", Allman, matchBody, matchBodyAllman),
      ("a lambda argument", Allman, lambdaArgument, lambdaArgument),
      ("a one-line block", Allman, "def f = xs.map { x => x + 1 }\n", "def f = xs.map { x => x + 1 }\n"),
    )

  "each braces style" should {
    layoutRows(
      rows.map { case (what, style, input, expected) => (s"$what as ${style.key}", styled(style), input, expected) },
    )
  }

  "a block standing as a statement" should {

    "not join the line before it in 1tbs" in {
      val input    = "def f = {\n  val a = 1\n\n  {\n    g(a)\n  }\n}\n"
      val expected = input
      val actual   = BraceStyleNormalizer.normalize(input, OneTrueBrace, 120)
      actual shouldBe expected
    }

  }

  "a braceless for whose last enumerator ends with a brace" should {

    "keep yield on its own line in 1tbs" in {
      val input =
        """def f(x: Int) =
          |  for
          |    a <- Some(x)
          |    b <- a match {
          |           case 1 => Some(1)
          |           case _ => None
          |         }
          |  yield b
          |""".stripMargin

      val expected = input

      val actual = BraceStyleNormalizer.normalize(input, OneTrueBrace, 120)

      actual shouldBe expected
    }

  }

}
