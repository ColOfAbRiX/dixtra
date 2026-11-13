package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The regions between `// dixtra: off` and `// dixtra: on` stay as written.
 */
class FormatterOffSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "a region between dixtra: off and dixtra: on" should {

    "stay as written while the code around it is formatted" in {
      val input =
        """object A {
          |  val a    =    1
          |  // dixtra: off
          |  val m = Map(
          |    "a"   -> 1,   "b" -> 2,
          |  )
          |  // dixtra: on
          |  val b    =    2
          |}
          |""".stripMargin

      val expected =
        """object A {
          |  val a = 1
          |  // dixtra: off
          |  val m = Map(
          |    "a"   -> 1,   "b" -> 2,
          |  )
          |  // dixtra: on
          |  val b = 2
          |}
          |""".stripMargin

      input should formatStablyTo(expected)
    }

    "recognise the markers in any letter case, with any spaces, and as block comments" in {
      // The markers are comments, laid out as comments: only the code between them stays as written
      val table =
        List(
          ("//DIXTRA:OFF", "//Dixtra:On", "// DIXTRA:OFF", "// Dixtra:On"),
          ("//   dixtra  :  off", "// dixtra : on", "//   dixtra  :  off", "// dixtra : on"),
          ("/* dixtra: off */", "/*dixtra:on*/", "/* dixtra: off */", "/*dixtra:on*/"),
        )

      table.foreach { (offInput, onInput, offOutput, onOutput) =>
        val input    = s"object A {\n  $offInput\n  val m    =    1\n  $onInput\n  val b    =    2\n}\n"
        val expected = s"object A {\n  $offOutput\n  val m    =    1\n  $onOutput\n  val b = 2\n}\n"
        input should formatTo(expected)
      }
    }

    "run to the end of the file without dixtra: on" in {
      val input    = "object A {\n  val a    =    1\n}\n// dixtra: off\nobject B {\n  val b    =    2\n}\n"
      val expected = "object A {\n  val a = 1\n}\n// dixtra: off\nobject B {\n  val b    =    2\n}\n"
      input should formatTo(expected)
    }

    "keep several regions" in {
      val input =
        "object A {\n  // dixtra: off\n  val a    =    1\n  // dixtra: on\n  val b    =    2\n  // dixtra: off\n" +
        "  val c    =    3\n  // dixtra: on\n}\n"

      val expected =
        "object A {\n  // dixtra: off\n  val a    =    1\n  // dixtra: on\n  val b = 2\n  // dixtra: off\n" +
        "  val c    =    3\n  // dixtra: on\n}\n"

      input should formatStablyTo(expected)
    }

    "not be opened by a marker in a string or in a longer comment" in {
      val input =
        "object A {\n  val s    =    \"// dixtra: off\"\n  // dixtra: off is a marker\n  val b    =    2\n}\n"

      val expected = "object A {\n  val s = \"// dixtra: off\"\n  // dixtra: off is a marker\n  val b = 2\n}\n"
      
      input should formatTo(expected)
    }

  }

}
