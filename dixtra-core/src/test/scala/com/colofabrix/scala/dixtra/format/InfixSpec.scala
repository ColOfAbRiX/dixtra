package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Infix operations whose operand before an operator is over several lines break after every operator, whatever the
 * operators: each operator at the end of its line, each operand at the start of a line, a `{ ... }` operand opening on
 * the line of its operator, and the operands after a broken chain under its selections.
 */
class InfixSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "an operation with an operand over several lines before an operator" should {

    "break after every operator, a braced operand opening on its operator's line" in {
      val input =
        """def a =
          |  IO.blocking {
          |    val a = 1
          |    a
          |  } >> replaceRetrying(temp, target, MoveAttempts) >> nextFunction >> { argument =>
          |    argument + 1
          |  } >> somethingElse(3)
          |""".stripMargin

      val expected =
        """def a =
          |  IO.blocking {
          |    val a = 1
          |    a
          |  } >>
          |  replaceRetrying(temp, target, MoveAttempts) >>
          |  nextFunction >> { argument =>
          |    argument + 1
          |  } >>
          |  somethingElse(3)
          |""".stripMargin

      input should formatStablyTo(expected)
    }

    "treat every operator the same" in {
      val input =
        """def c = foo {
          |  bar
          |} ++ baz * qux
          |""".stripMargin

      val expected =
        """def c =
          |  foo {
          |    bar
          |  } ++
          |  baz *
          |  qux
          |""".stripMargin

      input should formatStablyTo(expected)
    }

    "line up the operands after a broken chain with its selections" in {
      val input =
        ("""def b =
          |  xs.filter(isValidAndNotEmpty).collect { case x => transform(x) }.map(someFunctionName)""" +
        """.getOrElse(defaultValue) >> { argument =>
          |    newCode(argument)
          |  } >> other
          |""").stripMargin

      val expected =
        """def b =
          |  xs
          |    .filter(isValidAndNotEmpty)
          |    .collect { case x => transform(x) }
          |    .map(someFunctionName)
          |    .getOrElse(defaultValue) >> { argument =>
          |      newCode(argument)
          |    } >>
          |    other
          |""".stripMargin

      input should formatStablyTo(expected)
    }

  }

  "an operation without an operand over several lines before an operator" should {

    "keep a braced last operand on the line of its operator" in {
      val input =
        """class S {
          |  "name" should {
          |    "work" in {
          |      x shouldBe 1
          |    }
          |  }
          |}
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "stay on one line" in {
      val input    = "def d = a ++ b ++ c\n"
      val expected = input
      input should formatTo(expected)
    }

  }

  "an operation with several arguments" should {

    "stay on one line when it fits" in {
      val input    = "def t = x shouldBe (1, 2)\n"
      val expected = input
      input should formatTo(expected)
    }

    "break its arguments as a call does, after the operator" in {
      val input = "def t =\n  result shouldBe (firstValue, secondValue)\n"

      val expected =
        """def t =
          |  result shouldBe (
          |    firstValue,
          |    secondValue,
          |  )
          |""".stripMargin

      input should formatTo(expected, 30)
    }

    "keep the author's line break after the operator" in {
      val input    = "def t =\n  result shouldBe\n  (firstValue, secondValue)\n"
      val expected = input
      input should formatTo(expected)
    }

  }

}
