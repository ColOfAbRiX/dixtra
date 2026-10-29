package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Lambdas: `=>` works like the `=` of a definition (D7). The body keeps the author's placement on or below the `=>`
 * line, and a newline after `=>` is an author signal for the body; the `param =>` header is never split. A lambda on or
 * below the `=` of its definition is in the "bodies after =" tests of [[LayoutSettingsSpec]].
 *
 * The re-indentation of a multiline string literal inside a moved lambda body is a roadmap item (ignored).
 */
class LambdaSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "a lambda with a multiline string body (roadmap)" should {
    // The param-on-own-line rule is implemented; what remains roadmap is the interior reindentation of a multiline
    // string literal's continuation lines (shifting `|string` to match the deeper nesting). That is a distinct
    // verbatim-reindent concern tracked separately.
    "keep the param on its own line and re-indent the body" ignore {
      val tripleQuote = "\"\"\""
      val input =
        List(
          "val tmp: String => String =",
          "  s =>",
          s"    ${tripleQuote}Multiline",
          s"    |string${tripleQuote}.stripMargin",
        ).mkString("\n")

      val expected =
        List(
          "val tmp: String => String =",
          "  s =>",
          s"    ${tripleQuote}Multiline",
          s"      |string${tripleQuote}.stripMargin",
        ).mkString("\n")

      input should formatTo(expected)
    }
  }

  "a lambda body" should {
    "stay on the = line when written there" in {
      List("val f = x => x + 1\n", "val f = (x: Int) => x + 1\n").foreach { input =>
        val expected = input

        input should formatTo(expected)
      }
    }

    "keep its body below => when written there" in {
      val input =
        """val f =
          |  x =>
          |    x + 1
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "treat a newline after => as an author signal for its body" in {
      val input =
        """val f =
          |  x =>
          |    x.filter(_ > 0).map(_ * 2).sum
          |""".stripMargin

      val expected =
        """val f =
          |  x =>
          |    x
          |      .filter(_ > 0)
          |      .map(_ * 2)
          |      .sum
          |""".stripMargin

      input should formatTo(expected)
    }
  }

}
