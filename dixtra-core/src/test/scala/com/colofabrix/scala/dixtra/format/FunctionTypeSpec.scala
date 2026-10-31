package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Function types: exactly one space around `=>` in a `Type.Function`, e.g. `A => B` and `(A, B) => C`.
 *
 * Spacing is canonicalised by the normalisation passes: the whitespace pass collapses redundant spaces and the
 * arrow-spacing pass inserts a single space where one is missing.
 */
class FunctionTypeSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "a function type" should {

    "have exactly one space around =>" in {
      val table =
        List(
          "val f: Int => String = _.toString\n"     -> "val f: Int => String = _.toString\n",
          "val f: (Int, Int) => Int = _ + _\n"      -> "val f: (Int, Int) => Int = _ + _\n",
          "val f: Int   =>   String = _.toString\n" -> "val f: Int => String = _.toString\n",
          "val f: Int=>String = _.toString\n"       -> "val f: Int => String = _.toString\n",
        )
        
      table.foreach { (input, expected) => 
        input should formatTo(expected)
      }
    }

  }

}
