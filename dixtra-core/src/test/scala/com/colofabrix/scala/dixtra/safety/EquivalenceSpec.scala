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

  private def equivalent(before: String, after: String): Boolean =
    Equivalence.isEquivalentSource(parse(before), before, after)

  "output verification" should {
    
    "accept a pure layout change" in {
      equivalent("val x = List(a,\n  b)\n", "val x =\n  List(\n    a,\n    b\n  )\n") shouldBe true
    }

    "accept unwrapping a single-expression block" in {
      equivalent("def f = { x }\n", "def f = x\n") shouldBe true
    }

    "reject a change of program structure" in {
      equivalent("val x = (a + b) * c\n", "val x = a + b * c\n") shouldBe false
    }

    "reject output that does not parse" in {
      equivalent("val x = (a + b).foo\n", "val x = a + b).foo\n") shouldBe false
    }

    "reject a lost comment" in {
      equivalent("val x = // note\n  42\n", "val x = 42\n") shouldBe false
    }

    "reject a changed string literal" in {
      equivalent("val s = \"\"\"a\nb\"\"\"\n", "val s = \"\"\"a\n  b\"\"\"\n") shouldBe false
    }
  }

}
