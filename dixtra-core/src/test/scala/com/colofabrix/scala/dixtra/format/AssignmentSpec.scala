package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Assignments: `val` / `lazy val` definitions, whose body follows the newline-after-= rule (P4). Inside a container it
 * is covered by [[ContainerSpec]], and the rules about where the body goes by [[BodyPlacementSpec]].
 */
class AssignmentSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "val and lazy val with a multiline collection RHS" should {

    "push the RHS onto the next line after =" in {
      List("val", "lazy val").foreach { keyword =>
        val input =
          s"""$keyword xs = List(
             |  elementOne,
             |  elementTwo,
             |  elementThree
             |)
             |""".stripMargin

        val expected =
          s"""$keyword xs =
             |  List(
             |    elementOne,
             |    elementTwo,
             |    elementThree,
             |  )
             |""".stripMargin

        input should formatTo(expected)
      }
    }

  }

  "val with a short RHS (must stay inline)" should {

    "keep a fitting single-expression body inline after =" in {
      val input    = "val x = None\n"
      val expected = input
      input should formatTo(expected)
    }

  }

}
