package com.colofabrix.scala.dixtra.safety

import com.colofabrix.scala.dixtra.testkit.Parsing.parse
import com.colofabrix.scala.dixtra.verify.Equivalence
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.meta.*

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

  "the verification of a statement that is only realigned" should {

    "accept wider runs of spaces between its tokens" in {
      val original = statement("val x = f(\"a b\")\n")
      val rendered = "val x   =   f(\"a b\")"

      val expected = true

      val actual = Equivalence.isEquivalentStat(original, rendered)

      actual shouldBe expected
    }

    "reject a change of the spaces inside a string literal" in {
      val original = statement("val x = f(\"a b\")\n")
      val rendered = "val x = f(\"a  b\")"

      val expected = false

      val actual = Equivalence.isEquivalentStat(original, rendered)

      actual shouldBe expected
    }

    "reject a line break that splits it in two" in {
      val original = statement("val x = f (b)\n")
      val rendered = "val x = f\n(b)"

      val expected = false

      val actual = Equivalence.isEquivalentStat(original, rendered)

      actual shouldBe expected
    }

    "reject a dedent that moves a statement out of its body" in {
      val original = statement("def f =\n  a\n  b\n")
      val rendered = "def f =\n  a\nb"

      val expected = false

      val actual = Equivalence.isEquivalentStat(original, rendered)

      actual shouldBe expected
    }

    "reject a changed token" in {
      val original = statement("val x = f(\"a b\")\n")
      val rendered = "val y = f(\"a b\")"

      val expected = false

      val actual = Equivalence.isEquivalentStat(original, rendered)

      actual shouldBe expected
    }

  }

  "the verification of a statement" should {

    "accept a layout change" in {
      val original = statement("val x = List(a, b)\n")
      val rendered = "val x =\n  List(\n    a,\n    b,\n  )"

      val expected = true

      val actual = Equivalence.isEquivalentStat(original, rendered)

      actual shouldBe expected
    }

    "reject a lost statement" in {
      val original = statement("def f = {\n  a()\n  b()\n}\n")
      val rendered = "def f = {\n  a()\n}"

      val expected = false

      val actual = Equivalence.isEquivalentStat(original, rendered)

      actual shouldBe expected
    }

    "reject a changed literal" in {
      val original = statement("val x = 1\n")
      val rendered = "val x = 2"

      val expected = false

      val actual = Equivalence.isEquivalentStat(original, rendered)

      actual shouldBe expected
    }

    "reject a lost comment" in {
      val original = statement("val x = // note\n  42\n")
      val rendered = "val x = 42"

      val expected = false

      val actual = Equivalence.isEquivalentStat(original, rendered)

      actual shouldBe expected
    }

  }

  "the verification of a package clause" should {

    "accept a change of its braces" in {
      val original = packageClause("package a {\n  object B\n}\n")
      val rendered = "package a\n\nobject B"

      val expected = true

      val actual = Equivalence.isEquivalentPackage(original, rendered)

      actual shouldBe expected
    }

    "reject a second top-level statement" in {
      val original = packageClause("package a {\n  object B\n}\n")
      val rendered = "package a {\n  object B\n}\npackage a {\n  object B\n}"

      val expected = false

      val actual = Equivalence.isEquivalentPackage(original, rendered)

      actual shouldBe expected
    }

  }

  private def equivalent(before: String, after: String): Boolean =
    Equivalence.isEquivalentSource(parse(before), before, after)

  private def statement(text: String): Stat =
    parse(text).stats.head

  private def packageClause(text: String): Pkg =
    parse(text).stats.collect { case pkg: Pkg => pkg }.head

}
