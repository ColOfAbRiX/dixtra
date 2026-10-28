package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The housekeeping of a whole file (`housekeeping`): the whitespace at the end of the lines, the line break at the end
 * of the file, and the line endings.
 */
class HousekeepingSpec extends AnyWordSpec with Matchers {

  private def housekeeping(trimTrailingWhitespace: Boolean, enforceEof: Boolean, lineEnding: LineEnding): Config =
    Config.default.copy(housekeeping = HousekeepingConfig(trimTrailingWhitespace, enforceEof, lineEnding))

  private val defaults = Config.default

  private val withBlanks = "object A { \n  val x = 1 \t\n  // note  \n}\n"

  private val multilineString = "object A {\n  val s = \"\"\"one  \n  two\t\n  \"\"\" \n}\n"

  "trim-trailing-whitespace" should {
    "remove the spaces and tabs at the end of each line by default" in {
      Dixtra.format(withBlanks, defaults) shouldBe Right("object A {\n  val x = 1\n  // note\n}\n")
    }

    "keep the spaces and tabs at the end of a line inside a string literal" in {
      Dixtra.format(multilineString, defaults) shouldBe
      Right("object A {\n  val s =\n    \"\"\"one  \n  two\t\n  \"\"\"\n}\n")
    }

    "be idempotent" in {
      val once = Dixtra.format(multilineString, defaults)
      once.flatMap(Dixtra.format(_, defaults)) shouldBe once
    }

    "keep the whitespace at the end of the lines with no" in {
      val input = "object A {\n  val x = 1 \n}\n"
      Dixtra.format(input, housekeeping(false, true, LineEnding.Lf)) shouldBe Right(input)
    }
  }

  "enforce-eof" should {
    "add a missing line break at the end of the file by default" in {
      Dixtra.format("object A", defaults) shouldBe Right("object A\n")
    }

    "remove the blank lines at the end of the file" in {
      Dixtra.format("object A\n\n\n", defaults) shouldBe Right("object A\n")
    }

    "keep an empty file empty" in {
      Dixtra.format("", defaults) shouldBe Right("")
    }

    "leave the end of the file as written with no" in {
      Dixtra.format("object A", housekeeping(true, false, LineEnding.Lf)) shouldBe Right("object A")
    }
  }

  "line-ending" should {
    val lf = "object A {\n  val x = 1\n}\n"

    "keep the line breaks of the file by default" in {
      List("\n", "\r\n", "\r").foreach { eol =>
        Dixtra.format(lf.replace("\n", eol), defaults) shouldBe Right(lf.replace("\n", eol))
      }
    }
    "write the line breaks it is set to, whatever the file has" in {
      val table =
        List(
          (LineEnding.Lf, "\r\n", "\n"),
          (LineEnding.Crlf, "\n", "\r\n"),
          (LineEnding.Cr, "\n", "\r"),
          (LineEnding.Lf, "\r", "\n"),
        )
      table.foreach { (ending, from, to) =>
        Dixtra.format(lf.replace("\n", from), housekeeping(true, true, ending)) shouldBe Right(lf.replace("\n", to))
      }
    }
  }

}
