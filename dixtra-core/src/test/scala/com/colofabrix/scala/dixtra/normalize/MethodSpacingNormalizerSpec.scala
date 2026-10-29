package com.colofabrix.scala.dixtra.normalize

import com.colofabrix.scala.dixtra.Config
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Method separation: a blank line before every method that follows another statement, keeping decorations (annotations,
 * scaladoc, comments) attached to their method and never inserting a line after an opener or inside a string.
 */
class MethodSpacingNormalizerSpec extends AnyWordSpec with Matchers {

  // The pass with the default configuration.
  private def normalize(source: String): String =
    MethodSpacingNormalizer.normalize(
      source,
      Config.default.alignment,
      Config.default.blankLines.betweenAlignmentGroups,
      Config.default.blankLines.beforeScaladoc,
    )

  private val tripleQuote = "\"\"\""

  "a def line inside a multi-line string" should {

    "not be treated as a method start" in {
      val input = s"val s = ${tripleQuote}x\ndef y$tripleQuote\n"

      val expected = input

      val actual = normalize(input)

      actual shouldBe expected
    }

  }

  "an annotated def" should {

    "not get a blank between the annotation and the def" in {
      val input = "object A {\n  @inline\n  def f = 1\n}"

      val expected = input

      val actual = normalize(input)

      actual shouldBe expected
    }

    "get no blank before the annotation of a one-liner after a one-liner" in {
      val input = "val x = 1\n@inline\ndef f = 1"

      val expected = input

      val actual = normalize(input)

      actual shouldBe expected
    }

  }

  "a def with a single-line scaladoc" should {

    "not get a blank between the scaladoc and the def" in {
      val input = "object A {\n  /** doc */\n  def f = 1\n}"

      val expected = input

      val actual = normalize(input)

      actual shouldBe expected
    }

    "get the blank before the scaladoc" in {
      val input = "val x = 1\n/** doc */\ndef f = 1"

      val expected = "val x = 1\n\n/** doc */\ndef f = 1"

      val actual = normalize(input)

      actual shouldBe expected
    }

  }

  "a def with a multi-line scaladoc and annotation" should {

    "get the blank before the whole decoration group" in {
      val input = "val x = 1\n/**\n * doc\n */\n@inline\ndef f = 1"

      val expected = "val x = 1\n\n/**\n * doc\n */\n@inline\ndef f = 1"

      val actual = normalize(input)

      actual shouldBe expected
    }

  }

  "a def preceded by a line comment" should {

    "get no blank before the comment of a one-liner after a one-liner" in {
      val input = "def a = 1\n// about b\ndef b = 2"

      val expected = input

      val actual = normalize(input)

      actual shouldBe expected
    }

  }

  "a def directly after a container opener" should {

    "get no blank after a brace" in {
      val input = "object A {\n  def f = 1\n}"

      val expected = input

      val actual = normalize(input)

      actual shouldBe expected
    }

    "get no blank after a braceless `object A:`" in {
      val input = "object A:\n  def f = 1\n\n  def g = 2"

      val expected = input

      val actual = normalize(input)

      actual shouldBe expected
    }

    "get no blank after an extension header" in {
      val input = "extension (x: Int)\n  def double = x * 2\n\n  def triple = x * 3"

      val expected = input

      val actual = normalize(input)

      actual shouldBe expected
    }

    "get no blank after a `given ... with` header" in {
      val input = "given liftIo: LiftIO[F] with\n  override def liftIO[A](ioa: IO[A]): F[A] =\n    lift(ioa)"

      val expected = input

      val actual = normalize(input)

      actual shouldBe expected
    }

    "get no blank after a braceless opener followed by a scaladoc" in {
      val input = "object A:\n  /** doc */\n  def f = 1"

      val expected = input

      val actual = normalize(input)

      actual shouldBe expected
    }

    "get no blank for a local given at the top of a braceless method body" in {
      val input = "object A {\n  def f: Int =\n    given Dialect = dialects.Scala3\n    1\n}"

      val expected = input

      val actual = normalize(input)

      actual shouldBe expected
    }

    "get no blank for a local def at the top of a lambda body" in {
      val input = "val f = (x: Int) =>\n  def g = x\n  g"

      val expected = input

      val actual = normalize(input)

      actual shouldBe expected
    }

  }

  "defs with qualified or extra modifiers" should {

    "be separated" in {
      List("private[pkg] def", "protected[this] final def", "abstract override def").foreach { modifiers =>
        val input = s"val x = 1\n$modifiers f =\n  1"

        val expected = s"val x = 1\n\n$modifiers f =\n  1"

        val actual = normalize(input)

        actual shouldBe expected
      }
    }

  }

  "one-line declarations and vars" should {

    "not be separated from each other" in {
      val input = "trait A {\n  def f(x: Int): Int\n  def g: String\n}"

      val expected = input

      val actual = normalize(input)

      actual shouldBe expected
    }

    "not separate a one-line def from a var before it" in {
      val input = "var i = 0\ndef f = i"

      val expected = input

      val actual = normalize(input)

      actual shouldBe expected
    }

    "still separate a multi-line declaration" in {
      val input = "def f(x: Int): Int\ndef g(\n  x: Int\n): Int"

      val expected = "def f(x: Int): Int\n\ndef g(\n  x: Int\n): Int"

      val actual = normalize(input)

      actual shouldBe expected
    }

  }

  "a def annotated on the same line" should {

    "be separated from the previous def, which is not a decoration" in {
      val input = "@inline def f = 1\n@tailrec private def g = 2"

      val expected = "@inline def f = 1\n\n@tailrec private def g = 2"

      val actual = normalize(input)

      actual shouldBe expected
    }

  }

  "untokenisable text" should {

    "be returned unchanged by method spacing" in {
      val input = s"val s = ${tripleQuote}unterminated\ndef f = 1"

      val expected = input

      val actual = normalize(input)

      actual shouldBe expected
    }

  }

}
