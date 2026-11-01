package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * try / catch: always keeps curly braces (opening/closing on the keyword line, `try {` ... `} catch {` ... `}`) and
 * follows the same break-together-or-neither policy as if/else: the try and catch blocks break together or neither
 * does.
 */
class TryCatchSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "a single-line try/catch that overflows" should {

    "break both blocks together, keeping braces on the keyword lines" in {
      val input =
        "def f = try { risky() } catch { case e => recover(e) }\n"

      val expected =
        """def f =
          |  try {
          |    risky()
          |  } catch {
          |    case e => recover(e)
          |  }
          |""".stripMargin

      input should formatTo(expected, 40)
    }

  }

  "a braceless try/catch" should {

    "add braces on the keyword lines" in {
      val input =
        """def f =
          |  try risky()
          |  catch case e => recover(e)
          |""".stripMargin

      val expected =
        """def f =
          |  try {
          |    risky()
          |  } catch {
          |    case e => recover(e)
          |  }
          |""".stripMargin

      input should formatStablyTo(expected)
    }

  }

}
