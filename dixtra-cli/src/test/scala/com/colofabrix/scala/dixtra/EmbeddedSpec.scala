package com.colofabrix.scala.dixtra

import com.colofabrix.scala.dixtra.cli.ConfigCli
import com.colofabrix.scala.dixtra.testkit.TempFiles.*
import java.nio.file.Path
import java.util.function.Consumer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*

/**
 * The command line run inside another JVM (the sbt plugin), and `--changed-since`.
 */
class EmbeddedSpec extends AnyWordSpec with Matchers {

  private val unformatted = 
    "val x =\n  list.filter(_ > 0).map(_ * 2).headOption\n"

  // The exit code and the lines given to `out` and `err`.
  private def run(args: String*): (Int, List[String], List[String]) =
    val out = ListBuffer.empty[String]
    val err = ListBuffer.empty[String]

    val code =
      Embedded.run(args.toArray, (line => out += line): Consumer[String], (line => err += line): Consumer[String])

    (code, out.toList, err.toList)

  private def git(dir: Path, args: String*): Unit =
    val command = List("git", "-c", "user.name=test", "-c", "user.email=test@example.com") ++ args

    val process =
      new ProcessBuilder(command.asJava)
        .directory(dir.toFile)
        .redirectErrorStream(true)
        .start()
    
    val _ = process.getInputStream.readAllBytes()
    val _ = process.waitFor() shouldBe 0

  "the embedded command line" should {
    "report each file and return the exit code without exiting" in {
      withTempDir { dir =>
        val file = write(dir.resolve("A.scala"), unformatted)
        run("--check", file.toString) shouldBe (
          1,
          List(s"would format: ${file.toString}", "1 file: 1 would format"),
          Nil,
        )
      }
    }

    "return 2 and the usage for invalid arguments" in {
      val (code, out, err) = run("--max-column", "0", "A.scala")
      (code, out, err.exists(_.contains("--max-column must be a positive integer"))) shouldBe (2, Nil, true)
    }
  }

  "a .dixtra.conf" should {
    "print a warning for each unknown key, once, and format with the known settings" in {
      withTempDir { dir =>
        val conf = write(dir.resolve(ConfigCli.fileName), "max-colum = 30\n")
        val a    = write(dir.resolve("A.scala"), unformatted)
        val b    = write(dir.resolve("B.scala"), unformatted)
        run("--check", a.toString, b.toString) shouldBe (
          1,
          List(s"would format: ${a.toString}", s"would format: ${b.toString}", "2 files: 2 would format"),
          List(s"warning: ${conf.toString}: unknown setting max-colum (line 1), ignored"),
        )
      }
    }

    "format the files its path-overrides match with their settings" in {
      withTempDir { dir =>
        val call = "def f =\n  compute(\n    firstArgument,\n    secondArgument,\n  )\n"
        val _ =
          write(dir.resolve(ConfigCli.fileName), """path-overrides { "legacy/**" { trailing-commas = never } }""")
        val old = write(dir.resolve("legacy").resolve("A.scala"), call)
        val src = write(dir.resolve("src").resolve("A.scala"), call)
        val _   = run("--max-column", "30", dir.toString)
        (read(old), read(src)) shouldBe (call.replace("secondArgument,", "secondArgument"), call)
      }
    }
  }

  "--changed-since" should {
    "process only the files changed since the ref, untracked ones included" in {
      withTempDir { dir =>
        git(dir, "init", "-q")
        val committed = write(dir.resolve("A.scala"), unformatted)
        git(dir, "add", "A.scala")
        git(dir, "commit", "-q", "-m", "a")
        val untracked      = write(dir.resolve("B.scala"), unformatted)
        val (code, out, _) = run("--check", "--changed-since", "HEAD", dir.toString)
        val report         = out.map(_.replace(untracked.toString, "B"))
        val listsCommitted = out.exists(_.contains(committed.getFileName.toString))
        (code, report, listsCommitted) shouldBe (1, List("would format: B", "1 file: 1 would format"), false)
      }
    }

    "process a changed file whose name is not ASCII" in {
      withTempDir { dir =>
        git(dir, "init", "-q")
        val _ = write(dir.resolve("A.scala"), "val x = 1\n")
        git(dir, "add", "A.scala")
        git(dir, "commit", "-q", "-m", "a")
        val untracked      = write(dir.resolve("Ä.scala"), unformatted)
        val (code, out, _) = run("--check", "--changed-since", "HEAD", dir.toString)
        (code, out.map(_.replace(untracked.toString, "Ä"))) shouldBe (
          1,
          List("would format: Ä", "1 file: 1 would format"),
        )
      }
    }

    "fail outside a git repository" in {
      withTempDir { dir =>
        val _              = write(dir.resolve("A.scala"), unformatted)
        val (code, _, err) = run("--changed-since", "HEAD", dir.toString)
        (code, err.exists(_.startsWith(s"failed: ${dir.toString}"))) shouldBe (1, true)
      }
    }
  }

}
