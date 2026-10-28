package com.colofabrix.scala.dixtra.cli

import cats.data.*
import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.testkit.TempFiles.*
import java.nio.file.Path
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * `path-overrides`: settings for the files that a glob, relative to the directory of the configuration, matches.
 */
class PathOverridesSpec extends AnyWordSpec with Matchers {

  // The configuration `settings` gives to each file of `files`, relative to `dir`, the configuration in `dir`
  private def configs(dir: Path, settings: String, files: String*): List[ValidatedNel[String, Config]] =
    val conf = write(dir.resolve(ConfigCli.fileName), settings)
    ConfigCli
      .loadFile(conf)
      .fold(errors => fail(errors.toList.mkString("; ")), l => files.toList.map(f => l.configFor(dir.resolve(f))))

  private val legacy =
    """path-overrides { "legacy/**" { trailing-commas = never } }"""

  "an override" should {

    "apply to the files its glob matches, and only to them" in {
      withTempDir { dir =>
        val files = List("legacy/A.scala", "legacy/deep/B.scala", "src/C.scala")
        configs(dir, legacy, files*).map(_.map(_.trailingCommas)) shouldBe
        List(TrailingCommas.Never, TrailingCommas.Never, TrailingCommas.Always).map(Validated.valid)
      }

    }

    "match a glob from the directory of the configuration file, not from the formatted directory" in {
      withTempDir { dir =>
        val conf   = write(dir.resolve(ConfigCli.fileName), legacy)
        val loaded = ConfigCli.loadFile(conf).toOption
        
        val actual = 
          (
            loaded.map(_.configFor(dir.resolve("legacy").resolve("A.scala")).map(_.trailingCommas)),
            loaded.map(_.configFor(dir.resolve("src").resolve("legacy").resolve("A.scala")).map(_.trailingCommas)),
          ) 
        
        actual shouldBe (Some(Validated.valid(TrailingCommas.Never)), Some(Validated.valid(TrailingCommas.Always)))
      }

    }

    "never match a file outside the directory of the configuration" in {
      withTempDir { dir =>
        configs(dir.resolve("conf"), """path-overrides { "**" { max-column = 50 } }""", "../A.scala")
          .map(_.map(_.maxColumn)) shouldBe
        List(Validated.valid(120))
      }

    }

    "keep the other settings of the configuration" in {
      withTempDir { dir =>
        val settings = "max-column = 90\n" + legacy
        configs(dir, settings, "legacy/A.scala").map(_.map(c => (c.maxColumn, c.trailingCommas))) shouldBe
        List(Validated.valid((90, TrailingCommas.Never)))
      }

    }

    "replace a setting of the configuration, also when either is written through an alias" in {
      withTempDir { dir =>
        val settings =
          """if.braces = force-braces
            |path-overrides { "legacy/**" { braces.if = keep }, "old/**" { if.braces = force-indent } }
            |""".stripMargin
            
        configs(dir, settings, "legacy/A.scala", "old/A.scala", "A.scala").map(_.map(_.braces(Construct.If))) shouldBe
        List(BracePolicy.Keep, BracePolicy.ForceIndent, BracePolicy.ForceBraces).map(Validated.valid)
      }

    }

    "match a quoted glob holding a dot as one file" in {
      withTempDir { dir =>
        configs(dir, """path-overrides { "src/A.scala" { max-column = 50 } }""", "src/A.scala", "src/B.scala")
          .map(_.map(_.maxColumn)) shouldBe
        List(Validated.valid(50), Validated.valid(120))
      }

    }
  }

  "several matching overrides" should {

    "all apply when they agree" in {
      withTempDir { dir =>
        val settings =
          """path-overrides {
            |  "src/**" { max-column = 50 }
            |  "**/*.scala" { trailing-commas = never, max-column = 50 }
            |}""".stripMargin
        configs(dir, settings, "src/A.scala").map(_.map(c => (c.maxColumn, c.trailingCommas))) shouldBe
        List(Validated.valid((50, TrailingCommas.Never)))
      }

    }

    "fail the file when they give a setting different values, and only that file" in {
      withTempDir { dir =>
        val settings = """path-overrides { "src/**" { max-column = 50 }, "**/A.scala" { max-column = 60 } }"""
        configs(dir, settings, "src/A.scala", "src/B.scala").map(_.map(_.maxColumn)) shouldBe
        List(
          Validated.invalidNel(
            """max-column is set to different values by path-overrides."**/A.scala" and path-overrides."src/**"""",
          ),
          Validated.valid(50),
        )
      }

    }
  }

  "an invalid override" should {

    "fail the configuration, naming the override" in {
      List(
        """path-overrides { "src/**" { max-column = wide } }""" -> """path-overrides."src/**""",
        """path-overrides { "src/**" = 1 }"""                   -> "must be a section of settings",
        "path-overrides = 1"                                    -> "must be a section of globs",
      ).foreach { (settings, problem) =>
        ConfigCli.fromString(settings).fold(_.toList.mkString, _ => "") should include(problem)
      }

    }

    "ignore an unknown key, or a key that chooses the files, with a warning" in {
      withTempDir { dir =>
        val settings =
          """path-overrides { "src/**" { max-colum = 50, paths.excluded = ["x"], config-version = "1.0.0" } }"""

        val conf = write(dir.resolve(ConfigCli.fileName), settings)
        
        ConfigCli.loadFile(conf).map(_.warnings.map(_.stripPrefix(s"${conf.toString}: "))) shouldBe
        Validated.valid(
          List("config-version", "max-colum", "paths")
            .map(key => s"""unknown setting path-overrides."src/**".$key (line 1), ignored"""),
        )
      }

    }

  }

}
