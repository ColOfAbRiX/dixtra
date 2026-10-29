package com.colofabrix.scala.dixtra.render.context

import com.colofabrix.scala.dixtra.testkit.Parsing.parse
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.meta.*

/**
 * The token index behind every renderer: true tree spans including grouping parentheses, positional token and comment
 * lookups, and the detection of comments a rebuild from the children would drop.
 */
class SourceTokensSpec extends AnyWordSpec with Matchers {

  "the span of a tree" should {

    "include redundant grouping parentheses" in {
      val source   = "val x = (a + b).foo\n"
      val qual     = find(parse(source)) { case s: Term.Select => s.qual }
      val expected = (8, 15)

      val actual = new SourceTokens(source).span(qual)

      actual shouldBe expected
    }

    "not include the parentheses of an argument clause" in {
      val source   = "val x = f(a)\n"
      val arg      = find(parse(source)) { case n @ Term.Name("a") => n }
      val expected = (10, 11)

      val actual = new SourceTokens(source).span(arg)

      actual shouldBe expected
    }

  }

  "a positional lookup" should {

    "find the comments in a range" in {
      val expected = Vector("// c")
      val actual   = new SourceTokens("val x = 1 // c\n").commentsBetween(0, 15).map(_.text)
      actual shouldBe expected
    }

    "find the = of a definition, not an == operator" in {
      val expected = Some(6)
      val actual   = new SourceTokens("val x = a == b\n").equalsBefore(0, 14).map(_.start)
      actual shouldBe expected
    }

    "find the arrow of a lambda, not one in a parameter type" in {
      val expected = Some(20)
      val actual   = new SourceTokens("val f = (g: A => B) => g\n").arrowBefore(0, 23).map(_.start)
      actual shouldBe expected
    }

  }

  "comments in the gaps of a tree" should {

    "be detected between its children" in {
      val source   = "val x = if c then a // c\nelse b\n"
      val ifTerm   = find(parse(source)) { case t: Term.If => t }
      val expected = true

      val actual = new SourceTokens(source).hasGapComments(ifTerm)

      actual shouldBe expected
    }

    "not be reported inside one of its children" in {
      val source   = "val x = f(a /* c */)\n"
      val apply    = find(parse(source)) { case t: Term.Apply => t }
      val expected = false

      val actual = new SourceTokens(source).hasGapComments(apply)

      actual shouldBe expected
    }

  }

  // The first tree matching `pf` in a depth-first walk.
  private def find[T](tree: Tree)(pf: PartialFunction[Tree, T]): T =
    def loop(t: Tree): Option[T] =
      pf
        .lift(t)
        .orElse(t.children.iterator.flatMap(c => loop(c).iterator).nextOption())

    loop(tree).get

}
