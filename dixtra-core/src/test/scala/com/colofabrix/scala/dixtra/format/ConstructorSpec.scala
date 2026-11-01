package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Constructor parameters of classes and traits follow the signature rules of a `def` (P6): they stay inline unless the
 * header line overflows (the rest of the header up to `{` included), then the value parameters explode one per line
 * with a trailing comma (Tier 1); type parameters break too when even they overflow (Tier 2), and multiple clauses glue
 * `)(`.
 */
class ConstructorSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "the parameters of a class" should {

    "explode when the header overflows, also in a trait" in {
      List("class MyClass" -> "val x = 1", "trait MyTrait" -> "def x: Int").foreach { (header, member) =>
        val input =
          s"""$header(firstParameter: String, secondParameter: Int) {
             |  $member
             |}""".stripMargin

        val expected =
          s"""$header(
             |  firstParameter: String,
             |  secondParameter: Int,
             |) {
             |  $member
             |}""".stripMargin

        input should formatTo(expected, 40)
      }
    }

    "stay inline when the header fits" in {
      val input =
        """class MyClass(first: String, second: Int) {
          |  val x = 1
          |}""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "count the rest of the header towards the width" in {
      val input =
        """class MyClass(first: String) extends SomeBase(first) {
          |  val x = 1
          |}""".stripMargin

      val expected =
        """class MyClass(first: String)
          |  extends SomeBase(first) {
          |  val x = 1
          |}""".stripMargin

      input should formatTo(expected, 50)
    }

    "break the type parameters when they overflow too" in {
      val input =
        """class MyClass[TypeParam1, TypeParam2, TypeParam3](parameter_1: String) {
          |   val x = 1
          |}""".stripMargin

      val expected =
        """class MyClass[
          |  TypeParam1,
          |  TypeParam2,
          |  TypeParam3,
          |](parameter_1: String,
          |) {
          |   val x = 1
          |}""".stripMargin

      input should formatTo(expected, 40)
    }

    "break the parameters first, keeping fitting type parameters inline" in {
      val input =
        """class MyClass[TypeParam1, TypeParam2](parameter_1: String, parameter_2: Int) {
          |   val x = 1
          |}""".stripMargin

      val expected =
        """class MyClass[TypeParam1, TypeParam2](
          |  parameter_1: String,
          |  parameter_2: Int,
          |) {
          |   val x = 1
          |}""".stripMargin

      input should formatTo(expected, 50)
    }

    "glue multiple clauses" in {
      val input =
        """class MyClass(firstParameter: String)(using context: Context) {
          |   val x = 1
          |}""".stripMargin

      val expected =
        """class MyClass(
          |  firstParameter: String,
          |)(using
          |  context: Context,
          |) {
          |   val x = 1
          |}""".stripMargin

      input should formatTo(expected, 40)
    }

  }

  "the parameters of a case class without a body" should {

    "explode when they over-flow" in {
      val input = "final case class Person(firstName: String, lastName: String, age: Int)\n"

      val expected =
        """final case class Person(
          |  firstName: String,
          |  lastName: String,
          |  age: Int,
          |)
          |""".stripMargin

      input should formatTo(expected, 40)
    }

    "stay one per line when the author wrote them so, even if they fit" in {
      val input =
        """final private case class PhaseTransition(
          |   phase: FlightPhase,
          |   index: Long, altitude: Double
          |)
          |""".stripMargin

      val expected =
        """final private case class PhaseTransition(
          |  phase: FlightPhase,
          |  index: Long,
          |  altitude: Double,
          |)
          |""".stripMargin

      input should formatTo(expected)
    }

    "stay on one line when the author wrote them so and they fit" in {
      val input = "final private case class PhaseTransition(phase: FlightPhase, index: Long, altitude: Double)\n"

      val expected = input

      input should formatTo(expected)
    }

  }

  "the parameters of a plain class written one per line" should {

    "join when they fit" in {
      val input =
        """class Point(
          |  x: Int,
          |  y: Int,
          |)
          |""".stripMargin

      val expected = "class Point(x: Int, y: Int)\n"

      input should formatTo(expected)
    }

  }

}
