package com.colofabrix.scala.dixtra.normalize

import com.colofabrix.scala.dixtra.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Blank-line collapsing: runs of blank lines outside strings and comments collapse to one, while the interior of
 * multi-line strings is untouched.
 */
class BlankLineNormalizerSpec extends AnyWordSpec with Matchers {

  private val q = "\"\"\""

  "blank lines inside a multi-line string" should {

    "be preserved by blank-line collapsing" in {
      val input = s"val s = ${q}a\n\n\n\nb$q\n"
      BlankLineNormalizer.collapseBlankRuns(input) shouldBe input
    }

    "be preserved by the full pipeline" in {
      val input = s"val s = ${q}a\n\n\n\nb$q\n"
      Normalize(input, Config.default, SourceDialect.scala) shouldBe input
    }

    "be preserved inside an interpolated string" in {
      val input = s"val s = s${q}a\n\n\n$${x}\n\n\nb$q\n"
      Normalize(input, Config.default, SourceDialect.scala) shouldBe input
    }

  }

  "blank-line runs outside strings" should {

    "still collapse to one" in {
      val input = s"val s = ${q}a\n\nb$q\n\n\n\nval t = 1\n"
      BlankLineNormalizer.collapseBlankRuns(input) shouldBe s"val s = ${q}a\n\nb$q\n\nval t = 1\n"
    }

  }

}
