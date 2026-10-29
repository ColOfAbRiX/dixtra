package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Formatting is recursive: a definition or an expression nested inside another construct (a block, a case body, a
 * lambda) follows the same rules as at the top level.
 */
class NestingSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "a val inside a def body" should {
    "be formatted like a top-level val" in {
      val input =
        """def f =
          |  val ys =
          |    xs.map(_ + 1).filter(_ > 0).sum
          |  ys
          |""".stripMargin

      val expected =
        """def f =
          |  val ys =
          |    xs
          |      .map(_ + 1)
          |      .filter(_ > 0)
          |      .sum
          |  ys
          |""".stripMargin
      input should formatTo(expected)
    }
  }

  "a def inside a def body" should {
    "be formatted like a top-level def" in {
      val input =
        """def f =
          |   def g(first: Int, second: Int): Int = first
          |   g(1, 2)
          |""".stripMargin

      val expected =
        """def f =
          |  def g(first: Int, second: Int): Int =
          |    first
          |  g(1, 2)
          |""".stripMargin
      input should formatTo(expected, 40)
    }

    "brace a match in an annotated local def" in {
      val input =
        """def flatten(term: Term): (Term, List[Term]) =
          |   @annotation.tailrec
          |   def loop(t: Term, acc: List[Term]): (Term, List[Term]) =
          |     parts(t) match
          |       case Some((qual, _, _)) => loop(qual, t :: acc)
          |       case None             => (t, acc)
          |   loop(term, Nil)
          |""".stripMargin

      val expected =
        """def flatten(term: Term): (Term, List[Term]) =
          |  @annotation.tailrec
          |  def loop(t: Term, acc: List[Term]): (Term, List[Term]) =
          |    parts(t) match {
          |      case Some((qual, _, _)) => loop(qual, t :: acc)
          |      case None               => (t, acc)
          |    }
          |  loop(term, Nil)
          |""".stripMargin
      input should formatTo(expected)
    }
  }

  "a val inside a lambda body" should {
    "be formatted like a top-level val" in {
      val input =
        """def f =
          |  xs.foreach { x =>
          |    val ys =
          |      x.map(_ + 1).filter(_ > 0).sum
          |    println(ys)
          |  }
          |""".stripMargin

      val expected =
        """def f =
          |  xs.foreach { x =>
          |    val ys =
          |      x
          |        .map(_ + 1)
          |        .filter(_ > 0)
          |        .sum
          |    println(ys)
          |  }
          |""".stripMargin
      input should formatTo(expected)
    }
  }

  "a case body" should {
    "follow the rules of a body after => (breaking after any receiver by default), the other cases below too" in {
      val input =
        """def f(x: Option[String]) =
          |  x match {
          |    case Some(v) =>
          |      v.trim.toLowerCase.nonEmpty
          |    case None => false
          |  }
          |""".stripMargin

      val expected =
        """def f(x: Option[String]) =
          |  x match {
          |    case Some(v) =>
          |      v
          |        .trim
          |        .toLowerCase
          |        .nonEmpty
          |    case None =>
          |      false
          |  }
          |""".stripMargin
      input should formatTo(expected)
    }

    "explode an if/else whose else branch is a block" in {
      val input =
        """def f(x: Option[Int]) =
          |  x match {
          |    case Some(v) =>
          |      if v > 0 then v
          |      else
          |        val w = v * 2
          |        w
          |    case None => 0
          |  }
          |""".stripMargin

      val expected =
        """def f(x: Option[Int]) =
          |  x match {
          |    case Some(v) =>
          |      if v > 0 then
          |        v
          |      else
          |        val w = v * 2
          |        w
          |    case None =>
          |      0
          |  }
          |""".stripMargin
      input should formatTo(expected)
    }

    "be formatted in a partial function argument" in {
      val input =
        """def f =
          |   xs.collect {
          |     case Some(v) =>
          |       v.trim.toLowerCase.nonEmpty
          |   }
          |""".stripMargin

      val expected =
        """def f =
          |  xs.collect {
          |    case Some(v) =>
          |      v
          |        .trim
          |        .toLowerCase
          |        .nonEmpty
          |  }
          |""".stripMargin
      input should formatTo(expected)
    }

    "be formatted in a catch clause" in {
      val input =
        """def f =
          |   try {
          |      g()
          |    } catch {
          |      case e: Exception =>
          |        e.getMessage.trim.nonEmpty
          |    }
          |""".stripMargin

      val expected =
        """def f =
          |  try {
          |    g()
          |  } catch {
          |    case e: Exception =>
          |      e
          |        .getMessage
          |        .trim
          |        .nonEmpty
          |  }
          |""".stripMargin
      input should formatTo(expected)
    }
  }

  "a match with comments between its cases" should {
    "be braced, keeping the comments on their cases" in {
      val input =
        """def f(x: Int) =
          |  x match
          |    // the small ones
          |    case 1 => "a" // one
          |    case _ => "b"
          |""".stripMargin

      val expected =
        """def f(x: Int) =
          |  x match {
          |    // the small ones
          |    case 1 => "a" // one
          |    case _ => "b"
          |  }
          |""".stripMargin
      input should formatTo(expected)
    }
  }

  "a named argument" should {
    "have its value formatted like a body after =" in {
      val input =
        """def f =
          |  g(
          |    first = a,
          |    second = x match
          |      case 1 => true
          |      case _ => false
          |  )
          |""".stripMargin

      val expected =
        """def f =
          |  g(
          |    first = a,
          |    second =
          |      x match {
          |        case 1 => true
          |        case _ => false
          |      },
          |  )
          |""".stripMargin
      input should formatTo(expected)
    }
  }

  "a named argument with an application that fits only at the argument's indent" should {
    "hang the exploded application from the = line, also when written below the = with always-join" in {
      val input =
        """def f =
          |  g(
          |    b =
          |      Test(first = aaaaa, second = bbbbbb),
          |  )
          |""".stripMargin
      val config = joinAfterEquals.copy(maxColumn = 40)

      val expected =
        """def f =
          |  g(
          |    b = Test(
          |      first = aaaaa,
          |      second = bbbbbb,
          |    ),
          |  )
          |""".stripMargin

      input should formatStablyTo(expected, config)
    }
  }

  "a named argument with a multi-line application" should {
    "move the application up to the = line with always-join" in {
      val input =
        """def f =
          |  my_func(
          |    a = Test(s = "bb", i = 3),
          |    b =
          |      Test(
          |        s = "aa",
          |        i = 2
          |      )
          |  )
          |""".stripMargin

      val expected =
        """def f =
          |  my_func(
          |    a = Test(s = "bb", i = 3),
          |    b = Test(
          |      s = "aa",
          |      i = 2,
          |    ),
          |  )
          |""".stripMargin

      input should formatTo(expected, joinAfterEquals)
    }

    "move the application below = when its first line does not fit" in {
      val input =
        """def f =
          |  g(
          |    aVeryLongArgumentName = SomeConstructor(
          |      s = "aa",
          |    ),
          |  )
          |""".stripMargin

      val expected =
        """def f =
          |  g(
          |    aVeryLongArgumentName =
          |      SomeConstructor(
          |        s = "aa",
          |      ),
          |  )
          |""".stripMargin

      input should formatTo(expected, 32)
    }

    "keep a value that fits on one line" in {
      val input = "def f =\n  g(\n    a = Test(1, 2),\n    b = 3,\n  )\n"

      val expected = input

      input should formatTo(expected)
    }

    "hang an exploded chain from the = line" in {
      val input =
        """def f =
          |  g(
          |    a = xs.map(_ + 1).filter(_ > 0).sum,
          |  )
          |""".stripMargin

      val expected =
        """def f =
          |  g(
          |    a = xs
          |      .map(_ + 1)
          |      .filter(_ > 0)
          |      .sum,
          |  )
          |""".stripMargin

      input should formatTo(expected, 30)
    }
  }

  "an infix application" should {
    "have a block operand formatted" in {
      val input =
        """class ASpec {
          |  "a" should {
          |    val ys =
          |      xs.map(_ + 1).filter(_ > 0).sum
          |    ys
          |  }
          |}""".stripMargin

      val expected =
        """class ASpec {
          |  "a" should {
          |    val ys =
          |      xs
          |        .map(_ + 1)
          |        .filter(_ > 0)
          |        .sum
          |    ys
          |  }
          |}""".stripMargin
      input should formatTo(expected)
    }

    "indent the value of an assignment operator broken after the operator" in {
      val input = "object A {\n  xs ++=\n  Set(\n    a,\n    b,\n  )\n}\n"

      val expected = "object A {\n  xs ++=\n    Set(\n      a,\n      b,\n    )\n}\n"

      input should formatTo(expected)
    }

    "keep the parentheses of its argument" in {
      val input = "def f = a * (b + c)\n"

      val expected = input

      input should formatTo(expected)
    }

    "keep the author's break after the operator, the operands at one column" in {
      List("val x =\n  a ++\n  b\n", "def f =\n  a &&\n  b\n").foreach { input =>
        val expected = input

        input should formatTo(expected)
      }
    }

    "keep the author's break before the operator" in {
      val input =
        """def f =
          |  a
          |  && b
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }
  }

}
