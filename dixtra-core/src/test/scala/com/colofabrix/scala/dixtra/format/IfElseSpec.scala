package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * if / else: both branches are on the lines of their `then` / `else` or both are below them; an `else if` stays on the
 * `else` line and the whole chain breaks together.
 */
class IfElseSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "a long if/else in a def body (bare)" should {
    "break both branches consistently" in {
      val input =
        """def f =
          |  if someLongCondition then firstBranch() else secondBranch()
          |""".stripMargin
      val expected =
        """def f =
          |  if someLongCondition then
          |    firstBranch()
          |  else
          |    secondBranch()
          |""".stripMargin
      input should formatTo(expected, Config.withMaxColumn(50))
    }
  }

  "an explicit else ()" should {
    "be kept, as a bare if has the same tree" in {
      val input =
        """def f =
          |  if enabled then
          |    run()
          |  else ()
          |""".stripMargin
      val expected =
        """def f =
          |  if enabled then
          |    run()
          |  else
          |    ()
          |""".stripMargin
      input should formatTo(expected)
    }

    "not be added to a bare if" in {
      val input = "def f =\n  if enabled then\n    run()\n"
      input should formatTo(input)
    }
  }

  "an if/else with a multiline branch" should {
    "explode instead of inlining the child's line breaks" in {
      val input    = "val x = if c then { a; b } else d\n"
      val expected =
        """val x =
          |  if c then
          |    a
          |    b
          |  else
          |    d
          |""".stripMargin
      input should formatTo(expected)
    }
  }

  "an else-if chain" should {
    "keep each branch on its line and each else on its own line" in {
      val input =
        """def f(x: Int): String =
          |  if x == 0 then "zero"
          |  else if x == 1 then "one"
          |  else "many"
          |""".stripMargin
      input should
      formatTo(input, withBody(_.copy(ifBody = MultiBodyPolicy.AlwaysJoin)))
    }

    "keep each else if on the else line when exploded" in {
      val input =
        """def f(x: Int): String =
          |  if x == 0 then
          |    "zero"
          |  else if x == 1 then "one"
          |  else "many"
          |""".stripMargin
      val expected =
        """def f(x: Int): String =
          |  if x == 0 then
          |    "zero"
          |  else if x == 1 then
          |    "one"
          |  else
          |    "many"
          |""".stripMargin
      input should formatTo(expected)
    }

    "stay on one line when it fits" in {
      val input = "val s = if x == 0 then \"zero\" else if x == 1 then \"one\" else \"many\"\n"
      input should formatTo(input)
    }
  }

}
