package com.colofabrix.scala.dixtra.properties

import com.colofabrix.scala.dixtra.Dixtra
import java.nio.charset.StandardCharsets
import java.nio.file.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.jdk.CollectionConverters.*
import scala.util.Using

/**
 * Dogfooding: the formatter runs over this project's own sources, a corpus of real-world code with packages, comments,
 * scaladoc, and string literals.
 *
 * Every file must format without error (which includes passing the output safety guard) and the result must be
 * idempotent.
 */
class SelfFormatSpec extends AnyWordSpec with Matchers {

  // The sources of the command line and of the formatter, from `dixtra-cli`, where the tests of the command line run.
  private val sources: List[(Path, String)] =
    List(Paths.get("src"), Paths.get("..", "dixtra-core", "src")).flatMap { root =>
      Using.resource(Files.walk(root)) { stream =>
        stream
          .iterator()
          .asScala
          .filter(path => path.toString.endsWith(".scala") && Files.isRegularFile(path))
          .toList
          .sorted
          .map(path => (path, new String(Files.readAllBytes(path), StandardCharsets.UTF_8)))
      }
    }

  "formatting this project's sources" should {
    "find a corpus to format" in {
      val expected = true

      val actual = sources.size > 20

      actual shouldBe expected
    }

    "succeed for every file" in {
      val expected = true

      val actual =
        sources.collect { case (path, text) if Dixtra.format(text).isLeft => path.toString }.isEmpty

      actual shouldBe expected
    }

    "be idempotent for every file" in {
      val expected = true

      val actual =
        sources
          .collect {
            case (path, text) if Dixtra.format(text).flatMap(Dixtra.format) != Dixtra.format(text) =>
              path.toString
          }
          .isEmpty

      actual shouldBe expected
    }
  }

}
