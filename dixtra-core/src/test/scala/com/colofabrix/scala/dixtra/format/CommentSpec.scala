package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Comments are never lost: they stay attached to the element they belong to while the construct still follows the AON
 * rules, and a construct whose comments cannot be re-attached is kept as written.
 */
class CommentSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "an overflowing argument list with a block comment" should {
    val input = "val xs = List(firstElement, /* note */ secondElement, thirdElement, fourthElement, fifthElement)\n"
    val expected =
      """val xs =
        |  List(
        |    firstElement,
        |    /* note */ secondElement,
        |    thirdElement,
        |    fourthElement,
        |    fifthElement,
        |  )
        |""".stripMargin

    "explode one element per line, keeping the comment on its element" in {
      input should formatStablyTo(expected, 80)
    }
  }

  "a fitting argument list with a block comment" should {
    "stay inline with the comment" in {
      val input = "val xs = List(a, /* n */ b)\n"
      input should formatTo(input)
    }
  }

  "an argument list with a line comment" should {
    "explode even when it would fit" in {
      val input = "val xs = List(a, // n\n  b)\n"
      val expected =
        """val xs =
          |  List(
          |    a, // n
          |    b,
          |  )
          |""".stripMargin
      input should formatTo(expected)
    }

    "keep the comment after its argument when re-indented" in {
      val input =
        """val xs = List(
          |  a, // first
          |  b
          |)
          |""".stripMargin
      val expected =
        """val xs =
          |  List(
          |    a, // first
          |    b,
          |  )
          |""".stripMargin
      input should formatTo(expected)
    }
  }

  "an argument list with an own-line comment" should {
    "keep the comment on its own line before its element" in {
      val input =
        """val xs = List(
          |  a,
          |  // about b
          |  b
          |)
          |""".stripMargin
      val expected =
        """val xs =
          |  List(
          |    a,
          |    // about b
          |    b,
          |  )
          |""".stripMargin
      input should formatTo(expected)
    }
  }

  "an overflowing signature with a comment between parameters" should {
    "explode its parameters, keeping the comment on its parameter" in {
      val input = "def f(a: Int, /* legacy */ b: Int, c: Int, d: Int, e: Int, f: Int, g: Int): Int = 1\n"
      val expected =
        """def f(
          |  a: Int,
          |  /* legacy */ b: Int,
          |  c: Int,
          |  d: Int,
          |  e: Int,
          |  f: Int,
          |  g: Int,
          |): Int = 1
          |""".stripMargin
      input should formatTo(expected, 80)
    }
  }

  "a signature with a line comment on a parameter" should {
    "explode and keep the comment after the comma" in {
      val input = "def f(a: Int, // first\n  b: Int): Int = 1\n"
      val expected =
        """def f(
          |  a: Int, // first
          |  b: Int,
          |): Int = 1
          |""".stripMargin
      input should formatStablyTo(expected)
    }
  }

  "a chain with a comment before a dot" should {
    "explode, keeping the comment after the previous selection" in {
      val input = "val x = xs.map(f) /* c */ .filter(g).map(h).filter(anotherVeryLongPredicateName).toList\n"
      val expected =
        """val x =
          |  xs
          |    .map(f) /* c */
          |    .filter(g)
          |    .map(h)
          |    .filter(anotherVeryLongPredicateName)
          |    .toList
          |""".stripMargin
      input should formatTo(expected, 80)
    }

    "keep the comment after its selection when re-indented" in {
      val input =
        """val x = xs
          |  .map(f) // m
          |  .filter(g)
          |""".stripMargin
      val expected =
        """val x =
          |  xs
          |    .map(f) // m
          |    .filter(g)
          |""".stripMargin
      input should formatTo(expected)
    }
  }

  "an if/else with a comment" should {
    val input = "val x = if cond then a // why\n  else b\n"
    val expected =
      """val x =
        |  if cond then a // why
        |  else b
        |""".stripMargin

    "keep the author's breaks with else aligned to if" in {
      input should formatStablyTo(expected)
    }

    "keep an own-line comment in its branch" in {
      val input =
        """val x =
          |  if a then
          |    // keep me
          |    b
          |  else c
          |""".stripMargin
      input should formatTo(input)
    }
  }

  "a comment after =" should {
    "not swallow the body of a val" in {
      val input = "val x = // important\n  42\n"
      input should formatTo(input)
    }

    "not swallow the body of a def" in {
      val input = "def f(x: Int): Int = // c\n  x\n"
      input should formatTo(input)
    }
  }

  "a comment in a block" should {
    "stay on its own line between statements" in {
      val input =
        """def f =
          |  val a = 1
          |  // keep me
          |  a
          |""".stripMargin
      input should formatTo(input)
    }

    "trail its statement when the block loses its braces" in {
      val input =
        """def f = {
          |  val a = 1 // one
          |  a
          |}
          |""".stripMargin
      val expected =
        """def f =
          |  val a = 1 // one
          |  a
          |""".stripMargin
      input should formatTo(expected)
    }
  }

}
