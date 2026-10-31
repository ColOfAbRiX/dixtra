package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Curly braces with the default settings.
 *
 * Templates (trait, object, class, abstract class, enum), try/catch, match, given, extension and lambdas keep their
 * braces, opened and closed on their keyword line. If/then/else and def bodies are `force-indent` by default: a block
 * becomes a Scala 3 indented block. The other styles are covered by `BraceStyleSpec` (`braces.<construct>`), whose rows
 * with a default value cover if/else, match and extension, and `BracesStyleSettingSpec` (`braces.style`).
 */
class BraceSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "def definitions (never braces)" should {

    "keep a braceless single-expression body unchanged" in {
      val input = "def f = 1\n"

      val expected = input

      input should formatTo(expected)
    }

    "remove braces from a single-expression braced body" in {
      val input =
        """def f = {
          |  1
          |}
          |""".stripMargin

      val expected = "def f = 1\n"

      input should formatTo(expected)
    }

  }

  "given (always braces)" should {

    "keep an already-braced given body unchanged" in {
      val input =
        """given intOrd: Ord[Int] {
          |  def compare(a: Int, b: Int) = a - b
          |}""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

  }

  "a block holding a single definition" should {

    "become an indented block below =" in {
      val input = "def f(): Unit = { val x = 1 }\n"

      val expected =
        """def f(): Unit =
          |  val x = 1
          |""".stripMargin

      input should formatTo(expected)
    }

    "never leave a lonely brace line in an if branch" in {
      val input =
        """def f(): Unit =
          |  if c then {
          |    val _ = g()
          |  }
          |""".stripMargin

      val expected =
        """def f(): Unit =
          |  if c then
          |    val _ = g()
          |""".stripMargin
          
      input should formatTo(expected)
    }

  }

}
