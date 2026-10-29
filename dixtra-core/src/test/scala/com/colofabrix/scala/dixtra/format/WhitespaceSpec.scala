package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Infix / intra-line whitespace normalisation: runs of horizontal whitespace between tokens collapse to a single space,
 * without touching string literals, comments, indentation, or newlines.
 */
class WhitespaceSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "extra spaces around = and around an infix operator" should {
    "collapse to a single space" in {
      val table = List("val a =      \"3\"\n" -> "val a = \"3\"\n", "val a = 1   +   2\n" -> "val a = 1 + 2\n")
      table.foreach((input, expected) => input should formatTo(expected))
    }
  }

  "extra spaces inside a string literal" should {
    "be preserved" in {
      val input = "val a = \"hello     world\"\n"

      val expected = input

      input should formatTo(expected)
    }
  }

  "indentation" should {
    "be preserved while inner runs collapse" in {
      val input =
        """object A {
          |  val a =   1
          |}""".stripMargin

      val expected =
        """object A {
          |  val a = 1
          |}""".stripMargin
      input should formatTo(expected)
    }
  }

}
