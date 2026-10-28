package com.colofabrix.scala.dixtra.safety

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * End-to-end non-corruption guarantees: errors instead of broken output, line endings, multiline string literals,
 * verbatim slices and grouping parentheses all survive formatting.
 */
class NonCorruptionSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "unparsable input" should {

    "report unparsable input as a ParseError" in {
      val isParseError =
        Dixtra.format("val = ==") match {
          case Left(_: ParseError) => true
          case _                   => false
        }
      isParseError shouldBe true
    }

    "name the line and column of the error" in {
      Dixtra.format("object A {\n  val = 1\n}\n").left.map(_.getMessage).left.getOrElse("") should
      startWith("line 2, column 7:")
    }

  }

  "Windows line endings" should {

    "be preserved without mixing in bare newlines" in {
      val input  = "object A {\r\n  val x = 1\r\n  val xs = List(a,\r\n    b)\r\n}\r\n"
      val output = Dixtra.format(input).getOrElse("")
      output.replace("\r\n", "").contains('\n') shouldBe false
    }

    "still be formatted" in {
      val input    = "val xs = List(a,\r\n  b)\r\n"
      val expected = "val xs =\r\n  List(\r\n    a,\r\n    b,\r\n  )\r\n"
      input should formatTo(expected)
    }

  }

  "multiline string literals" should {

    "keep their content when moved below =" in {
      val input    = "val s = \"\"\"a\n  b\nc\"\"\"\n"
      val expected = "val s =\n  \"\"\"a\n  b\nc\"\"\"\n"
      input should formatTo(expected)
    }

    "keep their content when blank lines are inside" in {
      val input    = "val s = \"\"\"a\n\n\nb\"\"\"\n"
      val expected = "val s =\n  \"\"\"a\n\n\nb\"\"\"\n"
      input should formatTo(expected)
    }

  }

  "verbatim continuation lines" should {
    val input =
      """def f(x: Int) = x match {
        |  case 1 =>
        |    "one"
        |  case _ => "other"
        |}
        |""".stripMargin
    val expected =
      """def f(x: Int) =
        |  x match {
        |    case 1 =>
        |      "one"
        |    case _ =>
        |      "other"
        |  }
        |""".stripMargin

    "keep their relative indentation" in {
      input should formatStablyTo(expected)
    }
  }

  "grouping parentheses" should {

    "be kept around a chain receiver" in {
      val input = "val x = (a + b).foo.bar\n"
      input should formatTo(input)
    }

    "be kept around a match scrutinee" in {
      val input =
        """val x = (if c then a else b) match {
          |  case 1 => 2
          |}
          |""".stripMargin

      val expected =
        """val x =
          |  (if c then a else b) match {
          |    case 1 => 2
          |  }
          |""".stripMargin

      input should formatTo(expected)
    }

    "be kept around an applied function" in {
      val input = "val y = (f compose g)(x)\n"
      input should formatTo(input)
    }

    "be kept around a lambda body" in {
      val input = "val z = xs.map(x => (x + 1) * 2)\n"
      input should formatTo(input)
    }

  }

}
