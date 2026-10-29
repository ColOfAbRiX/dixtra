package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The body after `=` (D7): an expression keeps the placement the author gave it, on or below the `=` line; a newline
 * after `=` is an author signal that explodes multi-selection chains, if/else and try/catch but not the arguments of a
 * function call; and a body the formatter moves below `=` takes the layout that newline forces, so formatting again
 * leaves it unchanged. Where a body that fits goes, on or below the `=`, is in the "bodies after =" tests of
 * [[LayoutSettingsSpec]], and a fitting if/else in its `body.if` table.
 */
class BodyPlacementSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "an expression after =" should {
    "explode a chain with several selections, one per line" in {
      val input =
        """val y =
          |  xs.map { x =>
          |    x + 1
          |  }
          |  .filter(_ > 0)
          |""".stripMargin

      val expected =
        """val y =
          |  xs
          |    .map { x =>
          |      x + 1
          |    }
          |    .filter(_ > 0)
          |""".stripMargin

      input should formatTo(expected)
    }
  }

  "a newline after =" should {
    "keep a fitting chain with two selections on one line" in {
      val input =
        """def foldLeft[A, B](fa: NonEmptyFilled[A], b: B)(f: (B, A) => B): B =
          |  fa.toVector.foldLeft(b)(f)
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "still explode a chain with two selections that the author broke" in {
      val input =
        """val x =
          |  list.filter(_ > 0)
          |      .map(_ * 2)
          |""".stripMargin

      val expected =
        """val x =
          |  list
          |    .filter(_ > 0)
          |    .map(_ * 2)
          |""".stripMargin

      input should formatTo(expected)
    }

    "still explode a chain with two selections that the author broke after the receiver" in {
      val input =
        """val x =
          |  list
          |    .filter(_ > 0).map(_ * 2)
          |""".stripMargin

      val expected =
        """val x =
          |  list
          |    .filter(_ > 0)
          |    .map(_ * 2)
          |""".stripMargin

      input should formatTo(expected)
    }

    "keep a fitting try/catch on its line" in {
      val input =
        """def f =
          |  try g() catch { case _: E => h() }
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "not explode the arguments of a fitting function call" in {
      val input =
        """def g(s: String) =
          |  format(s, Config.default)
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }
  }

  "a newline before =" should {
    "not be an author signal after the annotation of a def" in {
      val input = "@tailrec\ndef f(x: Int): Int = xs.map(f).sum\n"

      val expected = input

      input should formatTo(expected)
    }

    "not be an author signal after the annotation of a val" in {
      val input = "@nowarn\nval x = xs.map(f).sum\n"

      val expected = input

      input should formatTo(expected)
    }
  }

  "a body that does not fit after =" should {
    "drop the body below = in its exploded form, which stays as it is" in {
      val input = "val someRatherLongValueName = firstCollection.map(transform).filter(keepThem).sum\n"

      val expected =
        """val someRatherLongValueName =
          |  firstCollection
          |    .map(transform)
          |    .filter(keepThem)
          |    .sum
          |""".stripMargin

      input should formatStablyTo(expected, 80)
    }
  }

}
