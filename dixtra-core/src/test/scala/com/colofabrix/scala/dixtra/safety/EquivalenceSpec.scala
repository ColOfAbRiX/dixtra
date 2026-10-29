package com.colofabrix.scala.dixtra.safety

import com.colofabrix.scala.dixtra.testkit.Parsing.parse
import com.colofabrix.scala.dixtra.verify.Equivalence
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The output verification behind the non-corruption guarantee: a rendering is accepted only when it is equivalent to
 * the input in structure and comments.
 */
class EquivalenceSpec extends AnyWordSpec with Matchers {

  "output verification" should {

    "accept a pure layout change" in {
      val expected = true
      val actual   = equivalent("val x = List(a,\n  b)\n", "val x =\n  List(\n    a,\n    b\n  )\n")
      actual shouldBe expected
    }

    "accept unwrapping a single-expression block" in {
      val expected = true
      val actual   = equivalent("def f = { x }\n", "def f = x\n")
      actual shouldBe expected
    }

    "reject a change of program structure" in {
      val expected = false
      val actual   = equivalent("val x = (a + b) * c\n", "val x = a + b * c\n")
      actual shouldBe expected
    }

    "reject output that does not parse" in {
      val expected = false
      val actual   = equivalent("val x = (a + b).foo\n", "val x = a + b).foo\n")
      actual shouldBe expected
    }

    "reject a lost comment" in {
      val expected = false
      val actual   = equivalent("val x = // note\n  42\n", "val x = 42\n")
      actual shouldBe expected
    }

    "reject a changed string literal" in {
      val expected = false
      val actual   = equivalent("val s = \"\"\"a\nb\"\"\"\n", "val s = \"\"\"a\n  b\"\"\"\n")
      actual shouldBe expected
    }

  }

  private def equivalent(before: String, after: String): Boolean =
    Equivalence.isEquivalentSource(parse(before), before, after)

}
