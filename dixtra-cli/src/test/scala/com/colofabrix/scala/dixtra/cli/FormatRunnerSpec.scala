package com.colofabrix.scala.dixtra.cli

import cats.effect.unsafe.implicits.global
import com.colofabrix.scala.dixtra.*
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

  // An author-broken-but-fitting chain that the AON formatter explodes.
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
      stream.iterator().asScala.map(_.getFileName.toString).toList.sorted
    } finally {
      stream.close()
    }

  "the runner in format mode" should {
    "rewrite a single unformatted file" in {
      withTempDir { dir =>
        val file    = write(dir.resolve("A.scala"), unformatted)
        val results = runner(check = false).run(List(file)).unsafeRunSync()
        (results, read(file)) shouldBe (List(FileResult.Formatted(file)), formatted)
      }
    }

    "rewrite an sbt file found in a directory, with its top-level settings" in {
      withTempDir { dir =>
        val file    = write(dir.resolve("build.sbt"), "name := \"demo\"\n\n" + unformatted)
        val results = runner(check = false).run(List(dir)).unsafeRunSync()
        (results, read(file)) shouldBe (List(FileResult.Formatted(file)), "name := \"demo\"\n\n" + formatted)
      }
    }

    "leave an already-formatted file unchanged" in {
      withTempDir { dir =>
        val file    = write(dir.resolve("A.scala"), formatted)
        val results = runner(check = false).run(List(file)).unsafeRunSync()
        (results, read(file)) shouldBe (List(FileResult.Unchanged(file)), formatted)
      }
    }

    "format multiple files given individually" in {
      withTempDir { dir =>
        val a       = write(dir.resolve("A.scala"), unformatted)
        val b       = write(dir.resolve("B.scala"), unformatted)
        val results = runner(check = false).run(List(a, b)).unsafeRunSync()
        (results.toSet, read(a), read(b)) shouldBe (
          Set(FileResult.Formatted(a), FileResult.Formatted(b)),
          formatted,
          formatted,
        )
      }
    }

    "report the files in the order they were given" in {
      withTempDir { dir =>
        // Alternate unformatted and formatted files, given in reverse name order.
        val expected = (1 to 20).toList.reverse.map { i =>
          if i % 2 == 0 then FileResult.Unchanged(write(dir.resolve(s"F$i.scala"), formatted))
          else FileResult.Formatted(write(dir.resolve(s"F$i.scala"), unformatted))
        }
        val files = expected.map {
          case FileResult.Unchanged(path) => path
          case FileResult.Formatted(path) => path
          case other                      => fail(other.toString)
        }
        runner(check = false).run(files).unsafeRunSync() shouldBe expected
      }
    }

    "recurse into a directory and skip non-Scala files" in {
      withTempDir { dir =>
        val a       = write(dir.resolve("A.scala"), unformatted)
        val b       = write(dir.resolve("nested/B.scala"), unformatted)
        val _       = write(dir.resolve("notes.txt"), "not scala")
        val results = runner(check = false).run(List(dir)).unsafeRunSync()
        results shouldBe List(FileResult.Formatted(a), FileResult.Formatted(b))
      }
    }

    "skip hidden and target directories inside a directory" in {
      withTempDir { dir =>
        val a       = write(dir.resolve("src/A.scala"), unformatted)
        val _       = write(dir.resolve(".history/src/A_20261001.scala"), unformatted)
        val _       = write(dir.resolve("target/gen/B.scala"), unformatted)
        val results = runner(check = false).run(List(dir)).unsafeRunSync()
        results shouldBe List(FileResult.Formatted(a))
      }
    }

    "skip the directories matched by an ignore pattern" in {
      withTempDir { dir =>
        val a       = write(dir.resolve("src/A.scala"), unformatted)
        val _       = write(dir.resolve("src/generated/B.scala"), unformatted)
        val _       = write(dir.resolve("generated/C.scala"), unformatted)
        val results = ignoring("**/generated/**").run(List(dir)).unsafeRunSync()
        results shouldBe List(FileResult.Formatted(a))
      }
    }

    "skip the files matched by an ignore pattern" in {
      withTempDir { dir =>
        val a       = write(dir.resolve("src/A.scala"), unformatted)
        val _       = write(dir.resolve("src/BuildInfo.scala"), unformatted)
        val results = ignoring("src/Build*.scala").run(List(dir)).unsafeRunSync()
        results shouldBe List(FileResult.Formatted(a))
      }
    }

    "match a single star within one directory only" in {
      withTempDir { dir =>
        val a       = write(dir.resolve("src/deep/A.scala"), unformatted)
        val _       = write(dir.resolve("src/B.scala"), unformatted)
        val results = ignoring("src/*.scala").run(List(dir)).unsafeRunSync()
        results shouldBe List(FileResult.Formatted(a))
      }
    }

    "format only the files matched by an include pattern" in {
      withTempDir { dir =>
        val a       = write(dir.resolve("src/main/A.scala"), unformatted)
        val _       = write(dir.resolve("scripts/B.scala"), unformatted)
        val results = selecting(List("src/**"), Nil).run(List(dir)).unsafeRunSync()
        results shouldBe List(FileResult.Formatted(a))
      }
    }

    "exclude from the included files" in {
      withTempDir { dir =>
        val a       = write(dir.resolve("src/main/A.scala"), unformatted)
        val _       = write(dir.resolve("src/gen/B.scala"), unformatted)
        val results = selecting(List("src/**"), List("src/gen")).run(List(dir)).unsafeRunSync()
        results shouldBe List(FileResult.Formatted(a))
      }
    }

    "always format a file given explicitly" in {
      withTempDir { dir =>
        val a       = write(dir.resolve("gen/A.scala"), unformatted)
        val results = selecting(List("src/**"), List("gen")).run(List(a)).unsafeRunSync()
        results shouldBe List(FileResult.Formatted(a))
      }
    }

    "report a failure for unparseable input without writing" in {
      withTempDir { dir =>
        val file     = write(dir.resolve("Broken.scala"), "val x = (")
        val results  = runner(check = false).run(List(file)).unsafeRunSync()
        val outcomes =
          results.map {
            case _: FileResult.Failed => "failed"
            case _                    => "other"
          }
        (outcomes, read(file)) shouldBe (List("failed"), "val x = (")
      }
    }
  }

  "the runner isolating per-file failures" should {
    "keep processing other files when the formatter throws" in {
      withTempDir { dir =>
        val bad     = write(dir.resolve("A.scala"), "val boom = 1\n")
        val good    = write(dir.resolve("B.scala"), "val y =       1\n")
        val format  = explodingFormat(new RuntimeException("kaboom"))
        val results =
          FormatRunner
            .withFormat(false, format)
            .run(List(bad, good))
            .unsafeRunSync()
        (results, read(bad), read(good)) shouldBe (
          List(FileResult.Failed(bad, "kaboom"), FileResult.Formatted(good)),
          "val boom = 1\n",
          "val y = 1\n",
        )
      }
    }

    "report a stack overflow in the formatter as a failure" in {
      withTempDir { dir =>
        val bad     = write(dir.resolve("A.scala"), "val boom = 1\n")
        val good    = write(dir.resolve("B.scala"), "val x = 1\n")
        val format  = explodingFormat(new StackOverflowError())
        val results =
          FormatRunner
            .withFormat(false, format)
            .run(List(bad, good))
            .unsafeRunSync()
        described(results) shouldBe List("failed A.scala", FileResult.Unchanged(good).toString)
      }
    }

    "keep processing other files when one path cannot be read" in {
      withTempDir { dir =>
        val missing = dir.resolve("Missing.scala")
        val good    = write(dir.resolve("B.scala"), "val y =        1\n")
        val results = runner(check = false).run(List(missing, good)).unsafeRunSync()
        (described(results), read(good)) shouldBe
        (List("failed Missing.scala", FileResult.Formatted(good).toString), "val y = 1\n")
      }
    }
  }

  "the runner writing files" should {
    "Fully replace the content and leave no temporary files behind" in {
      withTempDir { dir =>
        val file    = write(dir.resolve("A.scala"), "val y =      1\n")
        val results = runner(check = false).run(List(dir)).unsafeRunSync()
        (results, read(file), listDir(dir)) shouldBe (List(FileResult.Formatted(file)), "val y = 1\n", List("A.scala"))
      }
    }
  }

  "the runner in check mode" should {
    "report a would-format result without modifying the file" in {
      withTempDir { dir =>
        val file    = write(dir.resolve("A.scala"), unformatted)
        val results = runner(check = true).run(List(file)).unsafeRunSync()
        (results, read(file)) shouldBe (List(FileResult.WouldFormat(file)), unformatted)
      }
    }

    "report unchanged for an already-formatted file" in {
      withTempDir { dir =>
        val file    = write(dir.resolve("A.scala"), formatted)
        val results = runner(check = true).run(List(file)).unsafeRunSync()
        results shouldBe List(FileResult.Unchanged(file))
      }
    }
  }

  "the runner with a Scala 2 file" should {
    // Procedure syntax: Scala 2.13, dropped by Scala 3.
    val scala2 = "object A {\n  def f()   { println(1) }\n}\n"

    List(false, true).foreach { check =>
      s"skip it without modifying it (check = ${check.toString})" in {
          withTempDir { dir =>
            val file    = write(dir.resolve("A.scala"), scala2)
            val results = runner(check).run(List(file)).unsafeRunSync()
            (results, read(file)) shouldBe (List(FileResult.Skipped(file, "scala 2")), scala2)
          }
        }
    }

    "skip an sbt 1 build definition" in {
      withTempDir { dir =>
        val file    = write(dir.resolve("build.sbt"), "name := \"demo\"\n\ndef f() { println(1) }\n")
        val results = runner(check = false).run(List(file)).unsafeRunSync()
        results shouldBe List(FileResult.Skipped(file, "scala 2"))
      }
    }
  }

}
