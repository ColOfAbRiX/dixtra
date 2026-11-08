package com.colofabrix.scala.dixtra.properties

import com.colofabrix.scala.dixtra.cli.config.ConfigCli
import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import java.nio.charset.StandardCharsets
import java.nio.file.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.jdk.CollectionConverters.*
import scala.util.Using

/**
 * Golden fixtures for settings that interact: each directory under `src/test/resources/golden` holds a real
 * `.dixtra.conf`, an `input.scala.txt` and the `expected.scala.txt` it formats to. The `.txt` suffix keeps the fixtures
 * out of compilation and out of the formatter's own runs over the project.
 */
class GoldenSpec extends AnyWordSpec with Matchers with FormatMatchers {

  private val root: Path = Paths.get("src", "test", "resources", "golden")

  private val cases: List[Path] =
    Using.resource(Files.list(root))(_.iterator().asScala.filter(Files.isDirectory(_)).toList.sorted)

  private def read(path: Path): String =
    new String(Files.readAllBytes(path), StandardCharsets.UTF_8)

  "the golden fixtures" should {
    "exist" in {
      val expected = true

      val actual = cases.nonEmpty

      actual shouldBe expected
    }

    cases.foreach { dir =>
      val name = dir.getFileName.toString

      s"format $name as expected" in {
        val config = ConfigCli.fromFile(dir.resolve(ConfigCli.fileName)).getOrElse(fail(s"invalid $name config"))

        val expected = read(dir.resolve("expected.scala.txt"))

        read(dir.resolve("input.scala.txt")) should formatTo(expected, config)
      }

      s"be idempotent for $name" in {
        val config = ConfigCli.fromFile(dir.resolve(ConfigCli.fileName)).getOrElse(fail(s"invalid $name config"))

        val expected = read(dir.resolve("expected.scala.txt"))

        expected should formatTo(expected, config)
      }
    }
  }

}
