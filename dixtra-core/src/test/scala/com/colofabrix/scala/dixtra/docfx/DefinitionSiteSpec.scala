package com.colofabrix.scala.dixtra.docfx

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DefinitionSiteSpec extends AnyWordSpec with Matchers {

  "definition-site.md" should {

    "match the formatter" in {
      val problems = ReferencePageSuite.check("definition-site.md")

      problems shouldBe empty
    }

  }

}
