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

  private val defaults =
    Config.default

  private val withBlanks =
    "object A { \n  val x = 1 \t\n  // note  \n}\n"

  private val multilineString =
    "object A {\n  val s = \"\"\"one  \n  two\t\n  \"\"\" \n}\n"

  "trim-trailing-whitespace" should {

    "remove the spaces and tabs at the end of each line by default" in {
      val expected = Right("object A {\n  val x = 1\n  // note\n}\n")
      val actual   = Dixtra.format(withBlanks, defaults)
      actual shouldBe expected
    }

    "keep the spaces and tabs at the end of a line inside a string literal" in {
      val expected = Right("object A {\n  val s =\n    \"\"\"one  \n  two\t\n  \"\"\"\n}\n")
      val actual   = Dixtra.format(multilineString, defaults)
      actual shouldBe expected
    }

    "be idempotent" in {
      val once     = Dixtra.format(multilineString, defaults)
      val expected = once
      val actual   = once.flatMap(Dixtra.format(_, defaults))
      actual shouldBe expected
    }

    "keep the whitespace at the end of the lines with no" in {
      val input  = "object A {\n  val x = 1 \n}\n"
      val config = housekeeping(false, true, LineEnding.Lf)

      val expected = Right(input)

      val actual = Dixtra.format(input, config)

      actual shouldBe expected
    }

  }

  "enforce-eof" should {

    "add a missing line break at the end of the file by default" in {
      val expected = Right("object A\n")
      val actual   = Dixtra.format("object A", defaults)
      actual shouldBe expected
    }

    "remove the blank lines at the end of the file" in {
      val expected = Right("object A\n")
      val actual   = Dixtra.format("object A\n\n\n", defaults)
      actual shouldBe expected
    }

    "keep an empty file empty" in {
      val expected = Right("")
      val actual   = Dixtra.format("", defaults)
      actual shouldBe expected
    }

    "leave the end of the file as written with no" in {
      val config = housekeeping(true, false, LineEnding.Lf)

      val expected = Right("object A")
      val actual   = Dixtra.format("object A", config)
      actual shouldBe expected
    }

  }

  "line-ending" should {
    val lf = "object A {\n  val x = 1\n}\n"

    "keep the line breaks of the file by default" in {
      List("\n", "\r\n", "\r").foreach { eol =>
        val expected = Right(lf.replace("\n", eol))
        val actual   = Dixtra.format(lf.replace("\n", eol), defaults)
        actual shouldBe expected
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

      table.foreach { (ending, fromEol, toEol) =>
        val expected = Right(lf.replace("\n", toEol))
        val actual   = Dixtra.format(lf.replace("\n", fromEol), housekeeping(true, true, ending))
        actual shouldBe expected
      }
    }
  }

}
