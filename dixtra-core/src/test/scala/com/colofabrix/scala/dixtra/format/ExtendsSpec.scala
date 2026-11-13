package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Class headers with an extends list: one line; else `extends` / `with` on their own lines at +2; else the parameters
 * broken with the extends list on the `)` line; else both broken, `)` closing the last parameter and the extends list
 * at +4.
 */
class ExtendsSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "a class header with an extends list" should {

    "stay on one line when it fits" in {
      val input    = "class A(x: Int) extends B with C {\n  val y = 1\n}"
      val expected = input
      input should formatTo(expected)
    }

    "break at extends first, one part per line at +2" in {
      val input =
        """class VeryLongName(very: Long, parameters: Int) extends TraitA with TraitB with TraitC {
          |  val y = 1
          |}""".stripMargin

      val expected =
        """class VeryLongName(very: Long, parameters: Int)
          |  extends TraitA
          |  with TraitB
          |  with TraitC {
          |  val y = 1
          |}""".stripMargin

      input should formatStablyTo(expected, 60)
    }

    "break the parameters and keep the extends list on the `)` line" in {
      val input =
        """class VeryLongName(veryLongParameter: Long, anotherParameter: Int) extends TraitA with TraitB {
          |   val y = 1
          |}""".stripMargin

      val expected =
        """class VeryLongName(
          |  veryLongParameter: Long,
          |  anotherParameter: Int,
          |) extends TraitA with TraitB {
          |   val y = 1
          |}""".stripMargin

      input should formatStablyTo(expected, 50)
    }

    "break both, with the first extends part on the `)` line and the others at +2" in {
      val input =
        ("""class VeryLongName(veryLongParameter: Long, anotherParameter: Int) extends TraitAAAAAAAAA """ +
        """with TraitBBBBBBBBB {
         |   val y = 1
         |}""").stripMargin

      val expected =
        """class VeryLongName(
          |  veryLongParameter: Long,
          |  anotherParameter: Int,
          |) extends TraitAAAAAAAAA
          |  with TraitBBBBBBBBB {
          |   val y = 1
          |}""".stripMargin

      input should formatStablyTo(expected, 40)
    }

    "break at extends when the author broke the list" in {
      val input =
        """class A(x: Int) extends B
          |  with C {
          |  val y = 1
          |}""".stripMargin

      val expected =
        """class A(x: Int)
          |  extends B
          |  with C {
          |  val y = 1
          |}""".stripMargin

      input should formatTo(expected)
    }

    "break at extends when the author broke the list after with" in {
      val input =
        """class A(x: Int) extends B with
          |C {
          |  val y = 1
          |}""".stripMargin

      val expected =
        """class A(x: Int)
          |  extends B
          |  with C {
          |  val y = 1
          |}""".stripMargin

      input should formatTo(expected)
    }

    "keep a comma list on its extends line" in {
      val input =
        """object VeryLongObjectName extends TraitAAAAAAAAA, TraitBBBBBBBBBB {
          |  val y = 1
          |}""".stripMargin

      val expected =
        """object VeryLongObjectName
          |  extends TraitAAAAAAAAA, TraitBBBBBBBBBB {
          |  val y = 1
          |}""".stripMargin

      input should formatTo(expected, 50)
    }

    "put derives on its own line too" in {
      val input =
        """case class Point(x: Int, y: Int) extends Shape derives CanEqual, Ordering {
          |   val z = 1
          |}""".stripMargin

      val expected =
        """case class Point(x: Int, y: Int)
          |  extends Shape
          |  derives CanEqual, Ordering {
          |   val z = 1
          |}""".stripMargin

      input should formatTo(expected, 50)
    }

    "keep an extends list with a comment as written, breaking only the parameters" in {
      val input =
        """class A(x: Int) extends B /* note */ with C {
          |   val y = 1
          |}""".stripMargin

      val expected =
        """class A(
          |  x: Int,
          |) extends B /* note */ with C {
          |   val y = 1
          |}""".stripMargin

      input should formatTo(expected, 30)
    }

  }

}
