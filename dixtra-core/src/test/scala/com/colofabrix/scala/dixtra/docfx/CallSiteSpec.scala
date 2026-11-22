package com.colofabrix.scala.dixtra.docfx

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CallSiteSpec extends AnyWordSpec with Matchers {

  "call-site.md" should {

    "match the formatter" in {
      val problems = ReferencePageSuite.check("call-site.md")

      problems shouldBe empty
    }

  }

}
