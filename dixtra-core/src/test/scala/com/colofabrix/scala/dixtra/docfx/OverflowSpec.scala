package com.colofabrix.scala.dixtra.docfx

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OverflowSpec extends AnyWordSpec with Matchers {

  "overflow.md" should {

    "match the formatter" in {
      val problems = ReferencePageSuite.check("overflow.md")

      problems shouldBe empty
    }

  }

}
