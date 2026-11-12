package com.colofabrix.scala.dixtra.cli.engine

import cats.effect.unsafe.implicits.global
import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.cli.files.FileResult
import com.colofabrix.scala.dixtra.testkit.TempFiles.*
import java.nio.file.*
import java.util.concurrent.atomic.AtomicInteger
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.jdk.CollectionConverters.*

/**
 * The cache of the files found formatted: which files a run with the cache of the last run formats again.
 */
class ResultCacheSpec extends AnyWordSpec with Matchers {

  private val formatted =
    "val x = 1\n"

  private val unformatted =
    "val x =     1\n"

  // A format that counts its calls.
  private final class Counting {
    val calls = new AtomicInteger

    def format(source: String): Either[FormatError, String] =
      calls.incrementAndGet()
      Dixtra.format(source)
  }

  // Run on `file` with the cache saved in `cacheFile` by the last run, for the code `code`, saving it again. The
  // results, and how many times the run formatted.
  private def run(
    file: Path,
    cacheFile: Path,
    check: Boolean = true,
    settings: String = "settings",
    code: Option[String] = Some("code"),
  ): (List[FileResult], Int) =
    val counting = new Counting
    
    val results =
      for
        cache   <- ResultCache.load(cacheFile, code)
        results <- FormatRunner.withFormat(check, counting.format, cache, settings).run(List(file))
        _       <- cache.save(cacheFile)
      yield results

    (results.unsafeRunSync(), counting.calls.get)

  "a run with the cache of the last run" should {
    "not format a file that the last run found formatted" in {
      withTempDir { dir =>
        val file      = write(dir.resolve("A.scala"), formatted)
        val cacheFile = dir.resolve("cache").resolve("results")

        val expected = (List(FileResult.Unchanged(file)), 0)

        val _      = run(file, cacheFile)
        val actual = run(file, cacheFile)

        actual shouldBe expected
      }
    }

    "not format a file that the last run formatted in format mode, once it is found formatted" in {
      withTempDir { dir =>
        val file      = write(dir.resolve("A.scala"), unformatted)
        val cacheFile = dir.resolve("results")

        val expected = (List(FileResult.Unchanged(file)), 0)

        val _      = run(file, cacheFile, check = false)
        val _      = run(file, cacheFile, check = false)
        val actual = run(file, cacheFile, check = false)

        actual shouldBe expected
      }
    }

    "format again a file that changed" in {
      withTempDir { dir =>
        val file      = write(dir.resolve("A.scala"), formatted)
        val cacheFile = dir.resolve("results")

        val expected = (List(FileResult.Unchanged(file)), 1)

        val _      = run(file, cacheFile)
        val _      = write(file, "val y = 2\n")
        val actual = run(file, cacheFile)

        actual shouldBe expected
      }
    }

    "format again a file whose configuration changed" in {
      withTempDir { dir =>
        val file      = write(dir.resolve("A.scala"), formatted)
        val cacheFile = dir.resolve("results")

        val expected = (List(FileResult.Unchanged(file)), 1)

        val _      = run(file, cacheFile, settings = "before")
        val actual = run(file, cacheFile, settings = "after")

        actual shouldBe expected
      }
    }

    "format again every file when the code of dixtra changed" in {
      withTempDir { dir =>
        val file      = write(dir.resolve("A.scala"), formatted)
        val cacheFile = dir.resolve("results")

        val expected = (List(FileResult.Unchanged(file)), 1)

        val _      = run(file, cacheFile, code = Some("before"))
        val actual = run(file, cacheFile, code = Some("after"))

        actual shouldBe expected
      }
    }

    "format again a file that would change" in {
      withTempDir { dir =>
        val file      = write(dir.resolve("A.scala"), unformatted)
        val cacheFile = dir.resolve("results")

        val expected = (List(FileResult.WouldFormat(file)), 1)

        val _      = run(file, cacheFile)
        val actual = run(file, cacheFile)

        actual shouldBe expected
      }
    }

    "format every file when the code of dixtra is not known, without writing the cache" in {
      withTempDir { dir =>
        val file      = write(dir.resolve("A.scala"), formatted)
        val cacheFile = dir.resolve("results")

        val expected = ((List(FileResult.Unchanged(file)), 1), false)

        val _      = run(file, cacheFile, code = None)
        val actual = (run(file, cacheFile, code = None), Files.exists(cacheFile))

        actual shouldBe expected
      }
    }

    "keep only the files found unchanged by the last run" in {
      withTempDir { dir =>
        val file      = write(dir.resolve("A.scala"), formatted)
        val cacheFile = dir.resolve("results")

        val expected = 0

        val _ = run(file, cacheFile)
        val _ = write(file, unformatted)
        val _ = run(file, cacheFile)
        val actual =
          Files
            .readAllLines(cacheFile)
            .asScala
            .count(_.nonEmpty)

        actual shouldBe expected
      }
    }

    "format every file when the cache file is not there" in {
      withTempDir { dir =>
        val file = write(dir.resolve("A.scala"), formatted)

        val expected = (List(FileResult.Unchanged(file)), 1)

        val actual = run(file, dir.resolve("missing"))

        actual shouldBe expected
      }
    }
  }

  "the cache file of the command line" should {
    "be in the dixtra cache directory of the user, one for each set of paths" in {
      val first = ResultCache.userFile(List(Paths.get("a"), Paths.get("b")))
      val same  = ResultCache.userFile(List(Paths.get("b"), Paths.get("a")))
      val other = ResultCache.userFile(List(Paths.get("c")))

      val expected = (true, true, true)

      val actual =
        (
          first.exists(_.getParent.getFileName.toString == "dixtra"),
          first == same,
          first != other,
        )

      actual shouldBe expected
    }
  }

}
