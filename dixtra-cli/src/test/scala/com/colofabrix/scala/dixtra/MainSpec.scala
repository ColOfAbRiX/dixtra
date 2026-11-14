package com.colofabrix.scala.dixtra

import cats.effect.unsafe.implicits.global
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Smoke tests for the CLI entry point wiring.
 */
class MainSpec extends AnyWordSpec with Matchers {

  "Main.run" should {

    "exit with an error when no paths are supplied" in {
      val expected = cats.effect.ExitCode.Error
      val actual   = Main.run(List.empty).unsafeRunSync()
      actual shouldBe expected
    }

    "print the version and succeed with --version" in {
      val expected = cats.effect.ExitCode.Success
      val actual   = Main.run(List("--version")).unsafeRunSync()
      actual shouldBe expected
    }

    "exit with an error when --max-column is not positive" in {
      val expected = cats.effect.ExitCode.Error
      val actual   = Main.run(List("--max-column", "0", "x.scala")).unsafeRunSync()
      actual shouldBe expected
    }

  }

}
