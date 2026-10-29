package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * for-comprehensions: a multiline `for ... yield` puts its enumerators one per line and never adds braces: a braced
 * `for` keeps its braces, a braceless one stays braceless. The yield expression follows `body.for-yield`
 * (`break-complex` by default): on the `yield` line without braces, or below `yield`, inside braces only when the
 * (`break-complex` by default): on the `yield` line without braces, or below `yield`, inside braces only when the
 * author wrote them. A `for` written on one line that fits is kept as written, and so is a `for` loop without `yield`.
 */
class ForComprehensionSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "a braceless multiline for-yield" should {
    "stay braceless and keep a yield expression on the yield line with body.for-yield = keep" in {
      val input =
        """def f =
          |  for
          |    tree <- parse(lf)
          |    formatted <- render(lf, tree, config)
          |    verified <- verify(lf, tree, formatted)
          |  yield if crlf then verified.replace("\n", "\r\n") else verified
          |""".stripMargin

      val expected =
        """def f =
          |  for
          |    tree      <- parse(lf)
          |    formatted <- render(lf, tree, config)
          |    verified  <- verify(lf, tree, formatted)
          |  yield if crlf then verified.replace("\n", "\r\n") else verified
          |""".stripMargin

      input should formatTo(expected, withBody(_.copy(forYield = YieldBodyPolicy.Keep)))
    }

    "keep the yield of a single variable on the yield line" in {
      val input =
        """def f =
          |  for
          |    tree <- parse(lf)
          |    verified <- verify(lf, tree)
          |  yield verified
          |""".stripMargin

      val expected =
        """def f =
          |  for
          |    tree     <- parse(lf)
          |    verified <- verify(lf, tree)
          |  yield verified
          |""".stripMargin

      input should formatTo(expected)
    }

    "keep the yield of a literal on the yield line" in {
      val input =
        """def f =
          |  for
          |    a <- xs
          |    b <- ys
          |  yield ()
          |
          |def g =
          |  for
          |    a <- xs
          |    b <- ys
          |  yield "done"
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "keep a path of plain names on the yield line" in {
      val input =
        """def f =
          |  for
          |    a <- xs
          |    b <- ys
          |  yield ExitCode.Success
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "keep a call below yield where the author wrote it" in {
      val input =
        """def f =
          |  for
          |    a <- xs
          |    b <- ys
          |  yield
          |    a.combine(b)
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "keep value definitions and guards as enumerators" in {
      val input =
        """def f =
          |  for
          |    a <- xs
          |    b = a + 1
          |    if b > 2
          |  yield b
          |""".stripMargin

      val expected =
        """def f =
          |  for
          |    a <- xs
          |    b  = a + 1
          |         if b > 2
          |  yield b
          |""".stripMargin

      input should formatTo(expected)
    }

    "put a yield block below yield without braces" in {
      val input =
        """def f =
          |  for
          |    a <- xs
          |  yield
          |    val b = a + 1
          |    b * 2
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }
  }

  "a braced multiline for-yield with for-yield = keep" should {
    "be stable with a braced yield block" in {
      val input =
        """def f =
          |  for {
          |    a <- xs
          |    b <- ys
          |  } yield {
          |    a + b
          |  }
          |""".stripMargin

      val expected = input

      input should formatTo(expected, withBraces(Construct.ForYield -> BracePolicy.Keep))
    }

    "keep a yield expression on the yield line without adding braces" in {
      val input =
        """def f =
          |  for {
          |    a <- xs
          |    b <- ys
          |  } yield a + b
          |""".stripMargin
      val config = withBraces(Construct.ForYield -> BracePolicy.Keep)

      val expected = input

      input should formatTo(expected, withBody(_.copy(forYield = YieldBodyPolicy.Keep), config))
    }

    "take a braced yield out of its braces onto the yield line with body.for-yield = join" in {
      val input =
        """def f =
          |  for {
          |    a <- xs
          |    b <- ys
          |  } yield {
          |    ()
          |  }
          |""".stripMargin
      val config = withBraces(Construct.ForYield -> BracePolicy.Keep)

      val expected =
        """def f =
          |  for {
          |    a <- xs
          |    b <- ys
          |  } yield ()
          |""".stripMargin

      input should formatTo(expected, withBody(_.copy(forYield = YieldBodyPolicy.AlwaysJoin), config))
    }

    "put enumerators sharing a line on their own lines" in {
      val input =
        """def f =
          |  for {
          |    a <- xs; b <- ys
          |  } yield a
          |""".stripMargin

      val expected =
        """def f =
          |  for {
          |    a <- xs
          |    b <- ys
          |  } yield a
          |""".stripMargin

      input should formatTo(expected, withBraces(Construct.ForYield -> BracePolicy.Keep))
    }
  }

  "a for-yield on one line" should {
    "stay as written" in {
      val input =
        """def f = for x <- xs yield x
          |def g = for (x <- xs) yield x * 2
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }
  }

  "a multiline for-yield in parentheses" should {
    "stay as written" in {
      val input =
        """def f =
          |  for (a <- xs;
          |        b <- ys) yield a + b
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }
  }

  "a for loop without yield" should {
    "stay as written" in {
      val input =
        """def f =
          |  for
          |    a <- xs
          |    b <- ys
          |  do println(a + b)
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }
  }

}
