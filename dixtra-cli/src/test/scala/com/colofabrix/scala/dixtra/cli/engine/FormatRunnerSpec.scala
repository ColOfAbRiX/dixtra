package com.colofabrix.scala.dixtra.cli.engine

import cats.effect.unsafe.implicits.global
import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.cli.files.FileResult
import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.testkit.TempFiles.*
import java.nio.file.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.jdk.CollectionConverters.*

/**
 * File-system behaviour of the CLI runner: single files, multiple files, whole directories, check mode, and Scala 2
 * files.
 */
class FormatRunnerSpec extends AnyWordSpec with Matchers {

  // An author-broken-but-fitting chain that the formatter explodes.
  private val unformatted =
    """val x =
      |  list.filter(_ > 0).map(_ * 2).headOption
      |""".stripMargin

  private val formatted =
    """val x =
      |  list
      |    .filter(_ > 0)
      |    .map(_ * 2)
      |    .headOption
      |""".stripMargin

  private def runner(check: Boolean): FormatRunner =
    FormatRunner(Config.default, check)

  private def ignoring(patterns: String*): FormatRunner =
    selecting(List("**"), patterns.toList)

  private def selecting(include: List[String], exclude: List[String]): FormatRunner =
    FormatRunner.withFormat(false, (_, source) => Dixtra.format(source, Config.default), PathsConfig(include, exclude))

  // Formats normally, but throws on any source mentioning `boom`.
  private def explodingFormat(error: Throwable)(source: String): Either[FormatError, String] =
    if source.contains("boom") then throw error
    else Dixtra.format(source, Config.default)

  // The results, a failure as `failed <file name>`.
  private def described(results: List[FileResult]): List[String] =
    results.map {
      case FileResult.Failed(p, _) => s"failed ${p.getFileName.toString}"
      case other                   => other.toString
    }

  private def listDir(dir: Path): List[String] =
    val stream = Files.list(dir)
    try {
      stream
        .iterator()
        .asScala
        .map(_.getFileName.toString)
        .toList
        .sorted
    } finally {
      stream.close()
    }

  "the runner in format mode" should {

    "rewrite a single unformatted file" in {
      withTempDir { dir =>
        val file = write(dir.resolve("A.scala"), unformatted)

        val expected = (List(FileResult.Formatted(file)), formatted)

        val actual = (runner(check = false).run(List(file)).unsafeRunSync(), read(file))

        actual shouldBe expected
      }
    }

    "rewrite an sbt file found in a directory, with its top-level settings" in {
      withTempDir { dir =>
        val file = write(dir.resolve("build.sbt"), "name := \"demo\"\n\n" + unformatted)

        val expected = (List(FileResult.Formatted(file)), "name := \"demo\"\n\n" + formatted)

        val actual = (runner(check = false).run(List(dir)).unsafeRunSync(), read(file))

        actual shouldBe expected
      }
    }

    "leave an already-formatted file unchanged" in {
      withTempDir { dir =>
        val file = write(dir.resolve("A.scala"), formatted)

        val expected = (List(FileResult.Unchanged(file)), formatted)

        val actual = (runner(check = false).run(List(file)).unsafeRunSync(), read(file))

        actual shouldBe expected
      }
    }

    "format multiple files given individually" in {
      withTempDir { dir =>
        val a = write(dir.resolve("A.scala"), unformatted)
        val b = write(dir.resolve("B.scala"), unformatted)

        val expected =
          (Set(FileResult.Formatted(a), FileResult.Formatted(b)), formatted, formatted)

        val results = runner(check = false).run(List(a, b)).unsafeRunSync()
        val actual  = (results.toSet, read(a), read(b))

        actual shouldBe expected
      }
    }

    "report the files in the order they were given" in {
      withTempDir { dir =>
        // Alternate unformatted and formatted files, given in reverse name order.
        val expected = (1 to 20).toList.reverse.map { number =>
          if number % 2 == 0 then
            FileResult.Unchanged(write(dir.resolve(s"F$number.scala"), formatted))
          else
            FileResult.Formatted(write(dir.resolve(s"F$number.scala"), unformatted))
        }

        val files = expected.map {
          case FileResult.Unchanged(path) => path
          case FileResult.Formatted(path) => path
          case other => fail(other.toString)
        }

        val actual = runner(check = false).run(files).unsafeRunSync()

        actual shouldBe expected
      }
    }

    "recurse into a directory and skip non-Scala files" in {
      withTempDir { dir =>
        val a = write(dir.resolve("A.scala"), unformatted)
        val b = write(dir.resolve("nested/B.scala"), unformatted)
        val _ = write(dir.resolve("notes.txt"), "not scala")

        val expected = List(FileResult.Formatted(a), FileResult.Formatted(b))

        val actual = runner(check = false).run(List(dir)).unsafeRunSync()

        actual shouldBe expected
      }
    }

    "skip hidden and target directories inside a directory" in {
      withTempDir { dir =>
        val a = write(dir.resolve("src/A.scala"), unformatted)
        val _ = write(dir.resolve(".history/src/A_20261001.scala"), unformatted)
        val _ = write(dir.resolve("target/gen/B.scala"), unformatted)

        val expected = List(FileResult.Formatted(a))

        val actual = runner(check = false).run(List(dir)).unsafeRunSync()

        actual shouldBe expected
      }
    }

    "skip the directories matched by an ignore pattern" in {
      withTempDir { dir =>
        val a = write(dir.resolve("src/A.scala"), unformatted)
        val _ = write(dir.resolve("src/generated/B.scala"), unformatted)
        val _ = write(dir.resolve("generated/C.scala"), unformatted)

        val expected = List(FileResult.Formatted(a))

        val actual = ignoring("**/generated/**").run(List(dir)).unsafeRunSync()

        actual shouldBe expected
      }
    }

    "skip the files matched by an ignore pattern" in {
      withTempDir { dir =>
        val a = write(dir.resolve("src/A.scala"), unformatted)
        val _ = write(dir.resolve("src/BuildInfo.scala"), unformatted)

        val expected = List(FileResult.Formatted(a))

        val actual = ignoring("src/Build*.scala").run(List(dir)).unsafeRunSync()

        actual shouldBe expected
      }
    }

    "match a single star within one directory only" in {
      withTempDir { dir =>
        val a = write(dir.resolve("src/deep/A.scala"), unformatted)
        val _ = write(dir.resolve("src/B.scala"), unformatted)

        val expected = List(FileResult.Formatted(a))

        val actual = ignoring("src/*.scala").run(List(dir)).unsafeRunSync()

        actual shouldBe expected
      }
    }

    "format only the files matched by an include pattern" in {
      withTempDir { dir =>
        val a = write(dir.resolve("src/main/A.scala"), unformatted)
        val _ = write(dir.resolve("scripts/B.scala"), unformatted)

        val expected = List(FileResult.Formatted(a))

        val actual = selecting(List("src/**"), Nil).run(List(dir)).unsafeRunSync()

        actual shouldBe expected
      }
    }

    "exclude from the included files" in {
      withTempDir { dir =>
        val a = write(dir.resolve("src/main/A.scala"), unformatted)
        val _ = write(dir.resolve("src/gen/B.scala"), unformatted)

        val expected = List(FileResult.Formatted(a))

        val actual = selecting(List("src/**"), List("src/gen")).run(List(dir)).unsafeRunSync()

        actual shouldBe expected
      }
    }

    "always format a file given explicitly" in {
      withTempDir { dir =>
        val a = write(dir.resolve("gen/A.scala"), unformatted)

        val expected = List(FileResult.Formatted(a))

        val actual = selecting(List("src/**"), List("gen")).run(List(a)).unsafeRunSync()

        actual shouldBe expected
      }
    }

    "report a failure for unparseable input without writing" in {
      withTempDir { dir =>
        val file = write(dir.resolve("Broken.scala"), "val x = (")

        val expected = (List("failed"), "val x = (")

        val results = runner(check = false).run(List(file)).unsafeRunSync()

        val outcomes =
          results.map {
            case _: FileResult.Failed => "failed"
            case _                    => "other"
          }

        val actual = (outcomes, read(file))

        actual shouldBe expected
      }
    }

  }

  "the runner isolating per-file failures" should {

    "keep processing other files when the formatter throws" in {
      withTempDir { dir =>
        val bad    = write(dir.resolve("A.scala"), "val boom = 1\n")
        val good   = write(dir.resolve("B.scala"), "val y =       1\n")
        val format = explodingFormat(new RuntimeException("kaboom"))

        val expected =
          (List(FileResult.Failed(bad, "kaboom"), FileResult.Formatted(good)), "val boom = 1\n", "val y = 1\n")

        val results =
          FormatRunner
            .withFormat(false, format)
            .run(List(bad, good))
            .unsafeRunSync()
        val actual = (results, read(bad), read(good))

        actual shouldBe expected
      }
    }

    "report a stack overflow in the formatter as a failure" in {
      withTempDir { dir =>
        val bad    = write(dir.resolve("A.scala"), "val boom = 1\n")
        val good   = write(dir.resolve("B.scala"), "val x = 1\n")
        val format = explodingFormat(new StackOverflowError())

        val expected = List("failed A.scala", FileResult.Unchanged(good).toString)

        val results =
          FormatRunner
            .withFormat(false, format)
            .run(List(bad, good))
            .unsafeRunSync()
        val actual = described(results)

        actual shouldBe expected
      }
    }

    "keep processing other files when one path cannot be read" in {
      withTempDir { dir =>
        val missing = dir.resolve("Missing.scala")
        val good    = write(dir.resolve("B.scala"), "val y =        1\n")

        val expected = (List("failed Missing.scala", FileResult.Formatted(good).toString), "val y = 1\n")

        val results = runner(check = false).run(List(missing, good)).unsafeRunSync()
        val actual  = (described(results), read(good))

        actual shouldBe expected
      }
    }

  }

  "the runner writing files" should {

    "Fully replace the content and leave no temporary files behind" in {
      withTempDir { dir =>
        val file = write(dir.resolve("A.scala"), "val y =      1\n")

        val expected = (List(FileResult.Formatted(file)), "val y = 1\n", List("A.scala"))

        val results = runner(check = false).run(List(dir)).unsafeRunSync()
        val actual  = (results, read(file), listDir(dir))

        actual shouldBe expected
      }
    }

  }

  "the runner in check mode" should {

    "report a would-format result without modifying the file" in {
      withTempDir { dir =>
        val file = write(dir.resolve("A.scala"), unformatted)

        val expected = (List(FileResult.WouldFormat(file)), unformatted)

        val results = runner(check = true).run(List(file)).unsafeRunSync()
        val actual  = (results, read(file))

        actual shouldBe expected
      }
    }

    "report unchanged for an already-formatted file" in {
      withTempDir { dir =>
        val file = write(dir.resolve("A.scala"), formatted)

        val expected = List(FileResult.Unchanged(file))

        val actual = runner(check = true).run(List(file)).unsafeRunSync()

        actual shouldBe expected
      }
    }

  }

  "the runner with a Scala 2 file" should {

    // Procedure syntax: Scala 2.13, dropped by Scala 3.
    val scala2 = "object A {\n  def f()   { println(1) }\n}\n"

    List(false, true).foreach { check =>
      s"skip it without modifying it (check = ${check.toString})" in {
        withTempDir { dir =>
          val file = write(dir.resolve("A.scala"), scala2)

          val expected = (List(FileResult.Skipped(file, "scala 2")), scala2)

          val results = runner(check).run(List(file)).unsafeRunSync()
          val actual  = (results, read(file))

          actual shouldBe expected
        }
      }
    }

    "skip an sbt 1 build definition" in {
      withTempDir { dir =>
        val file = write(dir.resolve("build.sbt"), "name := \"demo\"\n\ndef f() { println(1) }\n")

        val expected = List(FileResult.Skipped(file, "scala 2"))

        val actual = runner(check = false).run(List(file)).unsafeRunSync()

        actual shouldBe expected
      }
    }

  }

}
