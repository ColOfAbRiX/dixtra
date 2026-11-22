package com.colofabrix.scala.dixtra.docfx

import com.colofabrix.scala.dixtra.docfx.ReferenceDocs.{ parsePage, Page }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.Files

object ReferenceDocsCheckerSpec {

  val sample: String =
    """## `call-site.groups-break`
      |
      |**Default:** `last-first`.
      |
      |Which groups break.
      |
      |Before:
      |
      |```scala
      |val e = Option.when(condition)(
      |  computeValue(argument)
      |)
      |```
      |
      |### `call-site.groups-break = break-all`
      |
      |Everything breaks.
      |
      |```scala
      |val e =
      |  Option.when(
      |    condition)(
      |    computeValue(argument),
      |  )
      |
      |// With settings:
      |//   max-column = 80
      |//   call-site.groups-break = break-all
      |```
      |
      |### `call-site.groups-break = last-first`
      |
      |Only the last breaks.
      |
      |```scala
      |val e =
      |  Option.when(condition)(
      |    computeValue(argument),
      |  )
      |
      |// With settings:
      |//   max-column = 80
      |//   call-site.groups-break = last-first
      |```
      |""".stripMargin

  val identicalSample: String =
    """## `call-site.groups-break`
      |
      |**Default:** `last-first`.
      |
      |Which groups break.
      |
      |Before:
      |
      |```scala
      |val r = Resource.make(
      |  acquireResource(config),
      |  releaseResource
      |)(release)
      |```
      |
      |### `call-site.groups-break = break-all`
      |
      |Everything breaks.
      |
      |```scala
      |val r =
      |  Resource.make(
      |    acquireResource(config),
      |    releaseResource)(
      |    release,
      |  )
      |
      |// With settings:
      |//   max-column = 80
      |//   call-site.groups-break = break-all
      |```
      |
      |### `call-site.groups-break = last-first`
      |
      |Only the last breaks.
      |
      |```scala
      |val r =
      |  Resource.make(
      |    acquireResource(config),
      |    releaseResource)(
      |    release,
      |  )
      |
      |// With settings:
      |//   max-column = 80
      |//   call-site.groups-break = last-first
      |```
      |""".stripMargin

  def withPage[A](markdown: String)(check: Page => A): A =
    val file = Files.createTempFile("reference-docs", ".md")
    try {
      Files.write(file, markdown.getBytes("UTF-8"))
      check(parsePage(file))
    } finally {
      Files.deleteIfExists(file): Unit
    }

  def blankOutputs(markdown: String): String =
    val pattern = "(?ms)(```scala\\n)((?:(?!```).)*?)(\\n\\n// With settings:)".r
    pattern.replaceAllIn(markdown, matched => matched.group(1) + matched.group(3))

}

class ReferenceDocsCheckerSpec extends AnyWordSpec with Matchers {

  import ReferenceDocsCheckerSpec.*

  "the checker" should {

    "accept a page that matches the formatter" in {
      withPage(sample) { page =>
        ReferenceDocsChecker.validate(page) shouldBe empty
      }
    }

    "reject an output that differs from the formatter" in {
      withPage(sample.replace("  Option.when(condition)(", "  Option.when(condition)()")) { page =>
        ReferenceDocsChecker.validate(page).exists(_.contains("output differs")) shouldBe true
      }
    }

    "reject a settings block that does not match the heading" in {
      withPage(sample.replace("//   call-site.groups-break = last-first", "//   call-site.groups-break = break-all")) { page =>
        ReferenceDocsChecker.validate(page).exists(_.contains("settings block says")) shouldBe true
      }
    }

    "reject an unknown setting" in {
      withPage(sample.replace("//   call-site.groups-break = last-first", "//   call-site.groups-berak = last-first")) { page =>
        ReferenceDocsChecker.validate(page).exists(_.contains("unknown setting")) shouldBe true
      }
    }

    "reject a missing default line" in {
      withPage(sample.replace("**Default:** `last-first`.", "")) { page =>
        ReferenceDocsChecker.validate(page).exists(_.contains("default line")) shouldBe true
      }
    }

    "regenerate a block whose output was blanked" in {
      withPage(blankOutputs(sample)) { page =>
        val problems    = ReferenceDocsChecker.rewrite(page)
        val regenerated = Files.readString(page.path)
        if problems.nonEmpty then fail(problems.mkString("\n"))
        regenerated shouldBe sample
      }
    }

    "reject two values that render identically" in {
      withPage(identicalSample) { page =>
        ReferenceDocsChecker.validate(page).exists(_.contains("render identically")) shouldBe true
      }
    }

  }

}
