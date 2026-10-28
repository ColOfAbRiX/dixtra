package com.colofabrix.scala.dixtra.doc

import com.colofabrix.scala.dixtra.doc.Doc.++
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The document IR and its renderer: hard line breaks at the current indent, verbatim slices re-based onto that indent
 * with string content frozen, and the single-line width measurement the layout decisions rely on.
 */
class RenderSpec extends AnyWordSpec with Matchers {

  "rendering a document" should {

    "concatenate text" in {
      Render.render(Doc.text("a") ++ Doc.text("b")) shouldBe "ab"
    }

    "start each line break at the current nesting" in {
      val doc = Doc.text("a") ++ Doc.Nest(2, Doc.Line ++ Doc.text("b")) ++ Doc.Line ++ Doc.text("c")
      Render.render(doc) shouldBe "a\n  b\nc"
    }

    "start continuation lines at the base indent" in {
      Render.render(Doc.text("a") ++ Doc.Line ++ Doc.text("b"), baseIndent = 4) shouldBe "a\n    b"
    }

    "separate documents with intercalate, not after the last one" in {
      Render.render(Doc.intercalate(Doc.text(", "), List(Doc.text("a"), Doc.text("b"), Doc.text("c")))) shouldBe
      "a, b, c"
    }

  }

  "a verbatim slice" should {

    "keep the indentation of its lines relative to its source line" in {
      val slice = Doc.Verbatim("f {\n      x\n    }", sourceIndent = 4, frozenLines = Set.empty)
      Render.render(Doc.Nest(2, slice)) shouldBe "f {\n    x\n  }"
    }

    "emit its frozen lines untouched" in {
      val slice = Doc.Verbatim("s = \"\"\"a\n  b\"\"\"", sourceIndent = 0, frozenLines = Set(1))
      Render.render(Doc.Nest(4, slice)) shouldBe "s = \"\"\"a\n  b\"\"\""
    }

  }

  "a standalone document" should {

    "render as its content" in {
      Render.render(Doc.Standalone(Doc.text("val x = 1"))) shouldBe "val x = 1"
    }

    "never count as single-line" in {
      Render.flatWidth(Doc.Standalone(Doc.text("x"))) shouldBe None
    }

  }

  "the flat width of a document" should {

    "be the width of a single-line document" in {
      Render.flatWidth(Doc.text("ab") ++ Doc.Nest(2, Doc.text("c"))) shouldBe Some(3)
    }

    "be undefined with a line break" in {
      Render.flatWidth(Doc.text("a") ++ Doc.Line) shouldBe None
    }

    "be undefined for a multiline verbatim slice" in {
      Render.flatWidth(Doc.Verbatim("a\nb", sourceIndent = 0, frozenLines = Set.empty)) shouldBe None
    }

  }

}
