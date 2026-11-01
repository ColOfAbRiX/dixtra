package com.colofabrix.scala.dixtra.normalize

import com.colofabrix.scala.dixtra.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The whole normalisation pipeline: it leaves comments and strings untouched, separates decorated methods, and is
 * idempotent.
 */
class NormalizeSpec extends AnyWordSpec with Matchers {

  private val tripleQuote =
    "\"\"\""

  // The pipeline with the default configuration.
  private def normalize(rendered: String): String =
    Normalize(rendered, Config.default, SourceDialect.scala)

  "a block comment containing blank lines and a def line" should {

    "be left untouched" in {
      val input  = "val a = 1\n/* note\n\n\nx;\ndef y = {\n*/\nval b = 2\n"
      val actual = normalize(input)
      actual shouldBe input
    }

  }

  "Normalize" should {

    "be idempotent" in {
      val input =
        s"""object A {
           |  val x = 1
           |/**
           |  * doc
           |  */
           |@inline
           |private[pkg] def f = 1
           |val s = ${tripleQuote}a
           |
           |
           |def y$tripleQuote
           |
           |
           |  def g = 2
           |}
           |""".stripMargin

      val once     = normalize(input)
      val expected = once

      val actual = normalize(once)

      actual shouldBe expected
    }

    "separate a method with a scaladoc from its neighbours" in {
      val input    = "object A {\n  val x = 1\n  /** doc */\n  def f = 1\n  def g = 2\n}\n"
      val expected = "object A {\n  val x = 1\n\n  /** doc */\n  def f = 1\n\n  def g = 2\n}\n"

      val actual = normalize(input)

      actual shouldBe expected
    }

  }

}
