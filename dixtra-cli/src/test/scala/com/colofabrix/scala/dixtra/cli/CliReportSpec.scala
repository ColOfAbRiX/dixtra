package com.colofabrix.scala.dixtra.cli

import cats.effect.*
import cats.effect.unsafe.implicits.global
import java.nio.file.Paths
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.collection.mutable.ListBuffer

/**
 * The exit code of a run, which makes check mode usable as a CI gate.
 */
class CliReportSpec extends AnyWordSpec with Matchers {

  private val path        = Paths.get("A.scala")
  private val unchanged   = FileResult.Unchanged(path)
  private val formatted   = FileResult.Formatted(path)
  private val wouldFormat = FileResult.WouldFormat(path)
  private val skipped     = FileResult.Skipped(path, "scala 2")
  private val failed      = FileResult.Failed(path, "parse error")

  // The exit code of a run reporting `results`, the lines thrown away.
  private def exitCode(results: List[FileResult], check: Boolean): ExitCode =
    CliReport.emit(results, check, quiet = false, CliReport.Printer(_ => IO.unit, _ => IO.unit)).unsafeRunSync()

  // The lines a run reporting `results` prints, each marked with where it went.
  private def report(results: List[FileResult], quiet: Boolean): List[String] =
    val lines   = ListBuffer.empty[String]
    val printer = CliReport.Printer(line => IO(lines += s"out $line"), line => IO(lines += s"err $line"))
    val _       = CliReport.emit(results, check = false, quiet, printer).unsafeRunSync()
    lines.toList

  "the report" should {
    "start the lines with the prefixes the sbt plugin logs them by, and end with a summary" in {
      val expected =
        List("out unchanged: A.scala", "err skipped (scala 2): A.scala", "out 2 files: 1 unchanged, 1 skipped")

      val actual = report(List(unchanged, skipped), quiet = false)

      actual shouldBe expected
    }

    "leave the unchanged files out when quiet, but count them" in {
      val expected = List("out formatted: A.scala", "out 2 files: 1 formatted, 1 unchanged")

      val actual = report(List(unchanged, formatted), quiet = true)

      actual shouldBe expected
    }

    "sum up a run without files" in {
      val expected = "0 files"

      val actual = CliReport.summary(Nil)

      actual shouldBe expected
    }
  }

  "the path of a file" should {
    // The lines `results` print on the standard output, without the summary.
    def printed(results: List[FileResult]): List[String] =
      val lines = ListBuffer.empty[String]
      val _ =
        CliReport
          .emit(results, check = false, quiet = false, CliReport.Printer(line => IO(lines += line), _ => IO.unit))
          .unsafeRunSync()
      lines.toList.dropRight(1)

    "be relative to the working directory when the file is under it" in {
      val absolute = Paths.get("src", "A.scala").toAbsolutePath

      val expected = List(s"formatted: ${Paths.get("src", "A.scala").toString}")

      val actual = printed(List(FileResult.Formatted(absolute)))

      actual shouldBe expected
    }

    "lose a leading ./" in {
      val expected = List("formatted: A.scala")

      val actual = printed(List(FileResult.Formatted(Paths.get(".", "A.scala"))))

      actual shouldBe expected
    }

    "stay absolute when the file is outside the working directory" in {
      val outside =
        Paths
          .get("")
          .toAbsolutePath
          .getRoot
          .resolve("elsewhere")
          .resolve("A.scala")

      val expected = List(s"formatted: ${outside.toString}")

      val actual = printed(List(FileResult.Formatted(outside)))

      actual shouldBe expected
    }
  }

  "a run in format mode" should {
    "succeed when files are unchanged, formatted or skipped" in {
      val expected = ExitCode.Success

      val actual = exitCode(List(unchanged, formatted, skipped), check = false)

      actual shouldBe expected
    }

    "fail when a file failed" in {
      val expected = ExitCode.Error

      val actual = exitCode(List(formatted, failed), check = false)

      actual shouldBe expected
    }
  }

  "a run in check mode" should {
    "succeed when files are unchanged or skipped" in {
      val expected = ExitCode.Success

      val actual = exitCode(List(unchanged, skipped), check = true)

      actual shouldBe expected
    }

    "fail when a file would change" in {
      val expected = ExitCode.Error

      val actual = exitCode(List(unchanged, wouldFormat), check = true)

      actual shouldBe expected
    }

    "fail when a file failed" in {
      val expected = ExitCode.Error

      val actual = exitCode(List(unchanged, failed), check = true)

      actual shouldBe expected
    }
  }

}
