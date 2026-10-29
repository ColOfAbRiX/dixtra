package com.colofabrix.scala.dixtra.normalize

import com.colofabrix.scala.dixtra.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Blank-line collapsing: runs of blank lines outside strings and comments collapse to one, while the interior of
 * multi-line strings is untouched.
 */
class BlankLineNormalizerSpec extends AnyWordSpec with Matchers {

  private val tripleQuote = "\"\"\""

  "blank lines inside a multi-line string" should {

    "be preserved by blank-line collapsing" in {
      val input = s"val s = ${tripleQuote}a\n\n\n\nb$tripleQuote\n"

      val expected = input

      val actual = BlankLineNormalizer.collapseBlankRuns(input)

      actual shouldBe expected
    }

    "be preserved by the full pipeline" in {
      val input = s"val s = ${tripleQuote}a\n\n\n\nb$tripleQuote\n"

      val expected = input

      val actual = Normalize(input, Config.default, SourceDialect.scala)

      actual shouldBe expected
    }

    "be preserved inside an interpolated string" in {
      val input = s"val s = s${tripleQuote}a\n\n\n$${x}\n\n\nb$tripleQuote\n"

      val expected = input

      val actual = Normalize(input, Config.default, SourceDialect.scala)

      actual shouldBe expected
    }

  }

  "blank-line runs outside strings" should {

    "still collapse to one" in {
      val input = s"val s = ${tripleQuote}a\n\nb$tripleQuote\n\n\n\nval t = 1\n"

      val expected = s"val s = ${tripleQuote}a\n\nb$tripleQuote\n\nval t = 1\n"

      val actual = BlankLineNormalizer.collapseBlankRuns(input)

      actual shouldBe expected
    }

  }

}
