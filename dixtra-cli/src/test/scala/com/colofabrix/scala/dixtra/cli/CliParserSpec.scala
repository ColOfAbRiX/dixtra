package com.colofabrix.scala.dixtra.cli

import com.monovore.decline.*
import java.nio.file.Paths
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Option parsing and validation of the CLI arguments.
 */
class CliParserSpec extends AnyWordSpec with Matchers {

  private val command: Command[CliOptions] =
    Command("dixtra", "test")(CliParser.options)

  private def parse(args: String*): Either[Help, CliOptions] =
    command.parse(args)

  private def errorsOf(args: String*): List[String] =
    parse(args*)
      .left
      .toOption
      .toList
      .flatMap(_.errors)

  "the CLI parser" should {
    "use the default max column when none is given" in {
      val expected =
        Right(CliOptions(List(Paths.get("A.scala")), None, None, check = false, changedSince = None, quiet = false))

      val actual = parse("A.scala")

      actual shouldBe expected
    }

    "accept a positive max column and the check flag" in {
      val expected =
        Right(CliOptions(List(Paths.get("A.scala")), None, Some(100), check = true, changedSince = None, quiet = false))

      val actual = parse("--max-column", "100", "--check", "A.scala")

      actual shouldBe expected
    }

    "accept the quiet flag" in {
      val expected = Right(true)

      val actual = parse("-q", "A.scala").map(_.quiet)

      actual shouldBe expected
    }

    "accept a git ref to limit the files to" in {
      val expected = Right(Some("main"))

      val actual = parse("--changed-since", "main", "A.scala").map(_.changedSince)

      actual shouldBe expected
    }

    "accept an invalid configuration, leaving its errors to the paths that use it" in {
      val expected = Right(true)

      val actual = parse("--config-string", "max-column = wide", "A.scala").map(_.config.exists(_.isInvalid))

      actual shouldBe expected
    }

    "reject a max column that is not positive, given with the long or the short flag" in {
      val table = List(List("--max-column", "0") -> "0", List("-w", "-3") -> "-3")

      table.foreach { (flags, value) =>
        val expected = List(s"--max-column must be a positive integer, got $value")

        val actual = errorsOf((flags :+ "A.scala")*)

        actual shouldBe expected
      }
    }
  }

}
