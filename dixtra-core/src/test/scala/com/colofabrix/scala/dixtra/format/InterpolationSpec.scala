package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Interpolated strings: the text parts are never touched; a braced splice holding a single expression is inlined as
 * `${expr}` when it fits.
 */
class InterpolationSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "an interpolated string" should {
    "inline a single-expression splice spread over several lines" in {
      val input = "val s = s\"\"\"a ${\n\n\n  x\n} b\"\"\"\n"
      val expected = "val s = s\"\"\"a ${x} b\"\"\"\n"
      input should formatTo(expected)
    }

    "keep simple and multi-statement splices as written" in {
      val input = "val s = s\"a $y ${ val z = 1; z } c\"\n"
      val expected = input
      input should formatTo(expected)
    }

    "never touch multiline string content" in {
      val input = "val s =\n  s\"\"\"a ${x}\n  b\"\"\"\n"
      val expected = input
      input should formatTo(expected)
    }
  }

}
