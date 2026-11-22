package com.colofabrix.scala.dixtra.docfx

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ReferencePagesSpec extends AnyWordSpec with Matchers {

  "reference pages" should {

    "be found" in {
      ReferencePageSuite.docsDir should not be empty
    }

    ReferencePageSuite.referencePages.foreach { page =>
      s"${page.name} match the formatter" in {
        val problems = ReferencePageSuite.check(page.name)

        problems shouldBe empty
      }
    }

  }

}
