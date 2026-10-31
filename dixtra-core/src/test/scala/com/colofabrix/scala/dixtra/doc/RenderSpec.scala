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
      val expected = "ab"
      val actual = Render.render(Doc.text("a") ++ Doc.text("b"))
      actual shouldBe expected
    }

    "start each line break at the current nesting" in {
      val doc = 
        Doc.text("a") ++ 
        Doc.Nest(2, Doc.Line ++ Doc.text("b")) ++
        Doc.Line ++ 
        Doc.text("c")

      val expected = "a\n  b\nc"
      val actual = Render.render(doc)

      actual shouldBe expected
    }

    "start continuation lines at the base indent" in {
      val expected = "a\n    b"
      val actual = Render.render(Doc.text("a") ++ Doc.Line ++ Doc.text("b"), baseIndent = 4)
      actual shouldBe expected
    }

    "separate documents with intercalate, not after the last one" in {
      val expected = "a, b, c"
      val actual = Render.render(Doc.intercalate(Doc.text(", "), List(Doc.text("a"), Doc.text("b"), Doc.text("c"))))
      actual shouldBe expected
    }

  }

  "a verbatim slice" should {

    "keep the indentation of its lines relative to its source line" in {
      val slice = Doc.Verbatim("f {\n      x\n    }", sourceIndent = 4, frozenLines = Set.empty)
      val expected = "f {\n    x\n  }"
      val actual = Render.render(Doc.Nest(2, slice))
      actual shouldBe expected
    }

    "emit its frozen lines untouched" in {
      val slice = Doc.Verbatim("s = \"\"\"a\n  b\"\"\"", sourceIndent = 0, frozenLines = Set(1))
      val expected = "s = \"\"\"a\n  b\"\"\""
      val actual = Render.render(Doc.Nest(4, slice))
      actual shouldBe expected
    }

  }

  "a standalone document" should {

    "render as its content" in {
      val expected = "val x = 1"
      val actual = Render.render(Doc.Standalone(Doc.text("val x = 1")))
      actual shouldBe expected
    }

    "never count as single-line" in {
      val expected = None
      val actual = Render.flatWidth(Doc.Standalone(Doc.text("x")))
      actual shouldBe expected
    }

  }

  "the flat width of a document" should {

    "be the width of a single-line document" in {
      val expected = Some(3)
      val actual = Render.flatWidth(Doc.text("ab") ++ Doc.Nest(2, Doc.text("c")))
      actual shouldBe expected
    }

    "be undefined with a line break" in {
      val expected = None
      val actual = Render.flatWidth(Doc.text("a") ++ Doc.Line)
      actual shouldBe expected
    }

    "be undefined for a multiline verbatim slice" in {
      val expected = None
      val actual = Render.flatWidth(Doc.Verbatim("a\nb", sourceIndent = 0, frozenLines = Set.empty))
      actual shouldBe expected
    }

  }

}
