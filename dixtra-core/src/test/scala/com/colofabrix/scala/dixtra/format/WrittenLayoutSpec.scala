package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The layouts the renderers keep as the author wrote them. Breaking one of these rules gives the same tree, so the
 * output check cannot see it: only these tests can.
 */
class WrittenLayoutSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "a while loop with a condition over several lines" should {

    "keep its condition as written" in {
      val input    = "def f =\n  while (a &&\n    b) {\n    x    =    1\n  }\n"
      val expected = "def f =\n  while (a &&\n    b) {\n    x = 1\n  }\n"
      input should formatStablyTo(expected)
    }

  }

  "an anonymous class" should {

    "stay on one line when it fits" in {
      val input    = "val r = new Runnable { def run()    =    () }\n"
      val expected = "val r = new Runnable { def run() = () }\n"
      input should formatStablyTo(expected)
    }

    "keep a self type in its header" in {
      val input    = "val r = new A { self: B =>\n  def f    =    1\n}\n"
      val expected = "val r =\n  new A { self: B =>\n    def f = 1\n  }\n"
      input should formatStablyTo(expected)
    }

  }

  "a type alias written over several lines" should {

    "keep its body below the =" in {
      val input    = "object A {\n  type T =\n    Int\n  val a    =    1\n}\n"
      val expected = "object A {\n  type T =\n    Int\n  val a = 1\n}\n"
      input should formatStablyTo(expected)
    }

  }

  "an infix call with type arguments" should {

    "stay as written" in {
      val input    = "val x = a op[Int] b\n"
      val expected = input
      input should formatStablyTo(expected)
    }

  }

  "a method whose body breaks below the = with body.def = always-join" should {

    "be separated from the aligned definitions above it by a blank line" in {
      val input =
        "object A {\n  def a(x: Int) = 1\n  def bb(y: Int) = someVeryLongFunctionName(argumentOne, argumentTwo)\n}\n"

      val expected =
        "object A {\n  def a(x: Int) = 1\n\n  def bb(y: Int) =\n    someVeryLongFunctionName(\n      argumentOne,\n" +
        "      argumentTwo,\n    )\n}\n"

      val config = withBody(_.withAfterEquals(SingleBodyPolicy.AlwaysJoin)).copy(maxColumn = 50)

      input should formatStablyTo(expected, config)
    }

  }

}
