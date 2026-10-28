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
      Main.run(List.empty).unsafeRunSync() shouldBe cats.effect.ExitCode.Error
    }

    "print the version and succeed with --version" in {
      Main.run(List("--version")).unsafeRunSync() shouldBe cats.effect.ExitCode.Success
    }

    "exit with an error when --max-column is not positive" in {
      Main.run(List("--max-column", "0", "x.scala")).unsafeRunSync() shouldBe cats.effect.ExitCode.Error
    }
  }

}
