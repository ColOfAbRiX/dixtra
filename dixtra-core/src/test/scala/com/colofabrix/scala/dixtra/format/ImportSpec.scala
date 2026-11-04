package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The `import` and `export` clauses: one space inside the braces of the selectors, the selectors packed onto as few lines
 * as fit when the clause does not fit, and the braces around a single selector by `braces.imports`. The spelling of the
 * selectors (`_` / `*`, `=>` / `as`) never changes.
 */
class ImportSpec extends AnyWordSpec with Matchers with FormatMatchers {

  private def withImportBraces(policy: ImportBraces): Config =
    Config.default.copy(braces = Config.default.braces.copy(imports = policy))

  "the selectors of an import" should {

    "have one space inside the braces and after each comma" in {
      val table =
        List(
          "import a.{B,C}\n"         -> "import a.{ B, C }\n",
          "import a.{ B ,  C }\n"    -> "import a.{ B, C }\n",
          "import a.{  B, C  }\n"    -> "import a.{ B, C }\n",
          "import a.{ B, C }\n"      -> "import a.{ B, C }\n",
          "import a.B\n"             -> "import a.B\n",
          "import a.{B => C, _}\n"   -> "import a.{ B => C, _ }\n",
          "import a.{B as C, *}\n"   -> "import a.{ B as C, * }\n",
          "import a.{given, *}\n"    -> "import a.{ given, * }\n",
          "import a.{given Ord[?]}\n" -> "import a.{ given Ord[?] }\n",
        )

      table.foreach { (input, expected) =>
        input should formatStablyTo(expected)
      }
    }

    "keep each importer of a clause with several importers on one line" in {
      val input    = "import a.{B,C}, d.E\n"
      val expected = "import a.{ B, C }, d.E\n"
      input should formatStablyTo(expected)
    }

    "follow the same rules in an export" in {
      val input    = "object A {\n  export b.{c,d}\n}\n"
      val expected = "object A {\n  export b.{ c, d }\n}\n"
      input should formatStablyTo(expected)
    }

    "follow the same rules inside a body" in {
      val input    = "def f = {\n  import a.{B,C}\n  B\n}\n"
      val expected = "def f =\n  import a.{ B, C }\n  B\n"
      input should formatStablyTo(expected)
    }

    "keep an import with a comment among its selectors as written" in {
      val input = "import a.{ B, /* c */ C}\n"
      input should formatStablyTo(input)
    }

  }

  "an import that does not fit" should {

    "pack its selectors onto as few lines as fit, with } on its own line" in {
      val input = "import com.example.render.context.{ RenderContext, SourceDocs, SourceTokens, SomethingElse }\n"

      val expected =
        """import com.example.render.context.{
          |  RenderContext, SourceDocs, SourceTokens,
          |  SomethingElse,
          |}
          |""".stripMargin

      input should formatStablyTo(expected, 50)
    }

    "end without a comma with definition-site.trailing-commas = never" in {
      val input  = "import com.example.render.context.{ RenderContext, SourceDocs, SourceTokens, SomethingElse }\n"
      val config = withDefinitionSite(_.copy(trailingCommas = TrailingCommas.Never), Config.withMaxColumn(50))

      val expected =
        """import com.example.render.context.{
          |  RenderContext, SourceDocs, SourceTokens,
          |  SomethingElse
          |}
          |""".stripMargin

      input should formatStablyTo(expected, config)
    }

    "be joined back when it was broken but fits" in {
      val input    = "import a.{\n  B,\n  C,\n}\n"
      val expected = "import a.{ B, C }\n"
      input should formatStablyTo(expected)
    }

    "stay as it is without braces" in {
      val input = "import com.example.very.long.path.to.something.Name\n"
      input should formatStablyTo(input, 30)
    }

    "stay on one line with several importers" in {
      val input = "import com.example.a.{ B, C }, com.example.d.{ E, F }\n"
      input should formatStablyTo(input, 30)
    }

  }

  "braces.imports" should {

    "keep the braces as written with keep" in {
      val table =
        List(
          "import a.{B}\n" -> "import a.{ B }\n",
          "import a.B\n"   -> "import a.B\n",
        )

      table.foreach { (input, expected) =>
        input should formatStablyTo(expected, withImportBraces(ImportBraces.Keep))
      }
    }

    "put a single selector in braces with force-braces" in {
      val table =
        List(
          "import a.B\n"        -> "import a.{ B }\n",
          "import a._\n"        -> "import a.{ _ }\n",
          "import a.*\n"        -> "import a.{ * }\n",
          "import a.given\n"    -> "import a.{ given }\n",
          "import a.B as C\n"   -> "import a.{ B as C }\n",
          "import a.{ B, C }\n" -> "import a.{ B, C }\n",
          "export a.b\n"        -> "export a.{ b }\n",
        )

      table.foreach { (input, expected) =>
        input should formatStablyTo(expected, withImportBraces(ImportBraces.ForceBraces))
      }
    }

    "drop the braces of a single selector that stands alone with avoid-braces" in {
      val table =
        List(
          "import a.{ B }\n"      -> "import a.B\n",
          "import a.{ _ }\n"      -> "import a._\n",
          "import a.{ * }\n"      -> "import a.*\n",
          "import a.{ given }\n"  -> "import a.given\n",
          "import a.{ B => C }\n" -> "import a.{ B => C }\n",
          "import a.{ B => _ }\n" -> "import a.{ B => _ }\n",
          "import a.{ B as C }\n" -> "import a.{ B as C }\n",
          "import a.{ B, C }\n"   -> "import a.{ B, C }\n",
          "import a.B as C\n"     -> "import a.B as C\n",
        )

      table.foreach { (input, expected) =>
        input should formatStablyTo(expected, withImportBraces(ImportBraces.AvoidBraces))
      }
    }

  }

}
