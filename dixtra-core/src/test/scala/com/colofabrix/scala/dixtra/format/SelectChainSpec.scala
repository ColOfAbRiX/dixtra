package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.Config
import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Select chains (D2, D3, D4): complete breaking, one selection per line. A chain with a single selection never breaks
 * at its dot.
 */
class SelectChainSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "a fitting-but-author-broken chain (bare)" should {

    "explode so each selection is on its own line" in {
      val input =
        """val x =
          |  list.filter(_ > 0).map(_ * 2).headOption
          |""".stripMargin

      val expected =
        """val x =
          |  list
          |    .filter(_ > 0)
          |    .map(_ * 2)
          |    .headOption
          |""".stripMargin

      input should formatStablyTo(expected)
    }

  }

  "a chain whose receiver has its arguments expanded" should {

    "align the selections with the closing parenthesis of the receiver" in {
      val input =
        """val candidates =
          |  Seq(
          |    sys.env.get("ProgramFiles(x86)"),
          |    sys.env.get("ProgramFiles"),
          |  )
          |    .flatten
          |    .map(home => file(home) / "vswhere.exe")
          |""".stripMargin

      val expected =
        """val candidates =
          |  Seq(
          |    sys.env.get("ProgramFiles(x86)"),
          |    sys.env.get("ProgramFiles"),
          |  )
          |  .flatten
          |  .map(home => file(home) / "vswhere.exe")
          |""".stripMargin

      input should formatStablyTo(expected)
    }

    "align them inside an object body, where the receiver is expanded by an overflow" in {
      val input =
        "object A {\n  val x = Seq(alphaValue, betaValue, gammaValue, deltaValue).flatten.map(toPath).headOption\n}\n"

      val expected =
        """object A {
          |  val x =
          |    Seq(
          |      alphaValue,
          |      betaValue,
          |      gammaValue,
          |      deltaValue,
          |    )
          |    .flatten
          |    .map(toPath)
          |    .headOption
          |}
          |""".stripMargin

      input should formatStablyTo(expected, Config.withMaxColumn(44))
    }

    "align them in the arguments of a call" in {
      val input =
        """val w = sink(
          |  Seq(
          |    alphaValue,
          |    betaValue,
          |  )
          |    .flatten
          |    .map(toPath),
          |)
          |""".stripMargin

      val expected =
        """val w =
          |  sink(
          |    Seq(
          |      alphaValue,
          |      betaValue,
          |    )
          |    .flatten
          |    .map(toPath),
          |  )
          |""".stripMargin

      input should formatStablyTo(expected)
    }

  }

  "a fitting-but-author-broken chain (wrapped)" should {

    "explode within an object body, preserving indentation" in {
      val input =
        """object A {
          |  val x =
          |    list.filter(_ > 0).map(_ * 2).headOption
          |}""".stripMargin

      val expected =
        """object A {
          |  val x =
          |    list
          |      .filter(_ > 0)
          |      .map(_ * 2)
          |      .headOption
          |}""".stripMargin

      input should formatTo(expected)
    }

  }

  "a chain with a single higher-order-function call" should {

    "hang the lambda block below = without exploding the sole selection (D1)" in {
      val input =
        """val x = list.map { x =>
          |    x + 1
          |}""".stripMargin

      val expected =
        """val x =
          |  list.map { x =>
          |    x + 1
          |  }""".stripMargin

      input should formatStablyTo(expected)
    }

  }

  "a chain with multiple calls including a lambda block" should {

    "explode each selection onto its own line, keeping the lambda block" in {
      val input =
        """val x = list.map { x =>
          |    x + 1
          |}.filter(_ > 0)""".stripMargin

      val expected =
        """val x =
          |  list
          |    .map { x =>
          |      x + 1
          |    }
          |    .filter(_ > 0)""".stripMargin

      input should formatTo(expected)
    }

  }

  "an exploded chain with a one-line lambda (D3)" should {

    "break after = and keep the fitting lambda inline on its selection" in {
      val input =
        """val x = list
          |  .map { x => x + 1 }
          |  .filter(_ > 0)
          |""".stripMargin

      val expected =
        """val x =
          |  list
          |    .map { x => x + 1 }
          |    .filter(_ > 0)
          |""".stripMargin

      input should formatStablyTo(expected)
    }

  }

  "a two-selection chain whose only break is inside its trailing lambda (D2)" should {

    "explode the selections" in {
      val input =
        """val x = list.filter(_ > 0).map { x =>
          |  x + 1
          |}
          |""".stripMargin

      val expected =
        """val x =
          |  list
          |    .filter(_ > 0)
          |    .map { x =>
          |      x + 1
          |    }
          |""".stripMargin

      input should formatStablyTo(expected)
    }

  }

  "a too-long chain (D4)" should {

    "break the selectors first, then the overflowing argument clause" in {
      val input =
        "val x = alpha.transform(argumentOne, argumentTwo, argumentThree).headOption\n"

      val expected =
        """val x =
          |  alpha
          |    .transform(
          |      argumentOne,
          |      argumentTwo,
          |      argumentThree,
          |    )
          |    .headOption
          |""".stripMargin

      input should formatStablyTo(expected, 40)
    }

  }

  "a single-selection chain after a long receiver" should {

    "measure its arguments from where they start on the line" in {
      val input =
        """val c =
          |  Some(enumerator(only, indent + 4, None)).filter(e => e > 1000000000)
          |""".stripMargin

      val expected =
        """val c =
          |  Some(enumerator(only, indent + 4, None)).filter(
          |    e => e > 1000000000,
          |  )
          |""".stripMargin

      input should formatStablyTo(expected, 60)
    }

  }

  "a chain with redundant whitespace" should {

    "be measured on the normalised text, not on redundant source whitespace" in {
      val input = "val x = xs.map(f)          .filter(g)\n"

      val expected = "val x = xs.map(f).filter(g)\n"

      input should formatTo(expected, 28)
    }

  }

  "a curried call" should {

    "keep a single case on the line of its brace when written there" in {
      val input =
        """def f(docs: List[Doc]): Doc =
          |  docs
          |    .iterator
          |    .foldLeft(Doc.Empty: Doc) { case (acc, d) =>
          |      acc ++ d
          |    }
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "indent its trailing brace block under its segment" in {
      val input =
        """def f(docs: List[Doc]): Doc =
          |  docs
          |    .iterator
          |    .foldLeft(Doc.Empty: Doc) {
          |  case (acc, d) =>
          |    acc ++ d
          |  }
          |""".stripMargin

      val expected =
        """def f(docs: List[Doc]): Doc =
          |  docs
          |    .iterator
          |    .foldLeft(Doc.Empty: Doc) {
          |      case (acc, d) =>
          |        acc ++ d
          |    }
          |""".stripMargin

      input should formatTo(expected)
    }

  }

  "an exploded chain with a call that has type arguments" should {

    "indent the call's arguments under its own line" in {
      val input =
        """val input: Opts[Path] =
          |  Opts
          |    .option[String](
          |      long = "input",
          |      short = "i",
          |    )
          |    .map(Paths.get(_))
          |    .withDefault(default.input)
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

  }

  "a chain that starts with a path ending at a capitalised name" should {

    "keep the path together and count only the selections after it" in {
      val input =
        """def f =
          |  xs.flatMap {
          |    case Right(a) =>
          |      fs2.Stream.emit(a)
          |    case _ =>
          |      fs2.Stream.empty
          |  }
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "keep the path on the receiver line when the chain explodes" in {
      val input =
        "val files = java.nio.file.Files.walk(baseDirectory).iterator().asScala.filter(Files.isRegularFile(_))" +
        ".toList.sorted.distinct\n"

      val expected =
        """val files =
          |  java.nio.file.Files
          |    .walk(baseDirectory)
          |    .iterator()
          |    .asScala
          |    .filter(Files.isRegularFile(_))
          |    .toList
          |    .sorted
          |    .distinct
          |""".stripMargin

      input should formatTo(expected)
    }

    "treat a value path the same way" in {
      val input =
        """def f =
          |  xs.map { x =>
          |    config.Defaults.value(x).toString.trim
          |  }
          |""".stripMargin

      val expected =
        """def f =
          |  xs.map { x =>
          |    config.Defaults
          |      .value(x)
          |      .toString
          |      .trim
          |  }
          |""".stripMargin

      input should formatTo(expected)
    }

    "break everywhere when the author broke a line inside the path" in {
      val input =
        """def f =
          |  config.
          |    Defaults.value(x)
          |""".stripMargin

      val expected =
        """def f =
          |  config
          |    .Defaults
          |    .value(x)
          |""".stripMargin

      input should formatTo(expected)
    }

    "not stop at a lowercase name" in {
      val input =
        """def f =
          |  xs.map { x =>
          |    config.input.value(x).trim
          |  }
          |""".stripMargin

      val expected =
        """def f =
          |  xs.map { x =>
          |    config
          |      .input
          |      .value(x)
          |      .trim
          |  }
          |""".stripMargin

      input should formatTo(expected)
    }

  }

  "a chain on a parenthesised receiver" should {

    "re-indent its lambda arguments with its selections" in {
      val input =
        """object A {
          |  val data = (0 to 20)
          |    .map { i =>
          |      f(i)
          |    }
          |    .toVector
          |}
          |""".stripMargin

      val expected =
        """object A {
          |  val data =
          |    (0 to 20)
          |      .map { i =>
          |        f(i)
          |      }
          |      .toVector
          |}
          |""".stripMargin

      input should formatStablyTo(expected)
    }

    "not reach inside the parentheses" in {
      val input    = "val d =\n  (xs.map(f)).filter(p).map(g).sum\n"
      val expected = "val d =\n  (xs.map(f))\n    .filter(p)\n    .map(g)\n    .sum\n"
      input should formatTo(expected)
    }

  }

}
