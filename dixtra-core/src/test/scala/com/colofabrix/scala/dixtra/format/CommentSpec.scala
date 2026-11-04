package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Comments are never lost: they stay attached to the element they belong to while the construct still follows the
 * layout rules. A comment where the layout has no place for it moves to the nearest place that has one, never across a
 * statement; a construct whose comments still cannot be re-attached is kept as written.
 */
class CommentSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "a comment where the layout has no place for it" should {

    "move from after a dot to before it" in {
      val input    = "val r = xs.filter(p). /* keep evens */ map(f).sum\n"
      val expected = "val r =\n  xs\n    .filter(p) /* keep evens */\n    .map(f)\n    .sum\n"
      input should formatStablyTo(expected)
    }

    "move from between a function and its arguments to after the arguments" in {
      val input    = "val s = compute /* heavy */ (a, b)\n"
      val expected = "val s = compute(a, b) /* heavy */\n"
      input should formatStablyTo(expected)
    }

    "move from between a selection and its arguments to after the arguments" in {
      val input    = "val t = xs.compute /* heavy */ (a, b).sum.max\n"
      val expected = "val t =\n  xs\n    .compute(a, b) /* heavy */\n    .sum\n    .max\n"
      input should formatStablyTo(expected)
    }

    "move from before the return type to after the last parameter" in {
      val input    = "def f(a: Int) /* the id */ : String = a.toString\n"
      val expected = "def f(a: Int /* the id */): String = a.toString\n"
      input should formatStablyTo(expected)
    }

    "trail the last parameter after its comma when the signature breaks" in {
      val input    = "def someLongFunctionName(parameterOne: Int) /* the id */ : String = parameterOne.toString\n"
      val expected = "def someLongFunctionName(\n  parameterOne: Int, /* the id */\n): String = parameterOne.toString\n"
      input should formatStablyTo(expected, 60)
    }

  }

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

      val expected = input

      input should formatTo(expected)
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

      val expected = input

      input should formatTo(expected)
    }

  }

  "a comment after =" should {

    "not swallow the body of a val" in {
      val input = "val x = // important\n  42\n"

      val expected = input

      input should formatTo(expected)
    }

    "not swallow the body of a def" in {
      val input = "def f(x: Int): Int = // c\n  x\n"

      val expected = input

      input should formatTo(expected)
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

      val expected = input

      input should formatTo(expected)
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
