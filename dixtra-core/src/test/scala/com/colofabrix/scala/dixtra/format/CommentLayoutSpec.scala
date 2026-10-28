package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.testkit.LayoutRows
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The layout of comments: reflowing to the maximum width, the space after `//`, the scaladoc layout and its tags. Each
 * row formats an input at a width and checks the output, then that the output is stable.
 */
class CommentLayoutSpec extends AnyWordSpec with Matchers with LayoutRows {

  private def at(width: Int): Config =
    Config.withMaxColumn(width)

  private def alignedTags(width: Int): Config =
    val config = at(width)
    config.copy(alignment = config.alignment.copy(scaladoc = ScaladocAlignmentConfig(tags = true)))

  private val rows: List[(String, Config, String, String)] =
    List(
      (
        "a // paragraph reflowed to the width",
        at(40),
        "// The quick brown fox\n// jumps over the lazy dog and keeps running far away\nval a = 1\n",
        "// The quick brown fox jumps over the\n// lazy dog and keeps running far away\nval a = 1\n",
      ),
      (
        "a blank // line kept between paragraphs",
        at(40),
        "// first para\n//\n// second para\nval a = 1\n",
        "// first para\n//\n// second para\nval a = 1\n",
      ),
      ("a space after //", at(40), "//foo\nval a = 1 //bar\n", "// foo\nval a = 1 // bar\n"),
      ("several spaces after // kept", at(40), "//   foo\nval a = 1\n", "//   foo\nval a = 1\n"),
      (
        "an overlong // line broken in several",
        at(30),
        "// aaa bbb ccc ddd eee fff ggg hhh iii\nval a = 1\n",
        "// aaa bbb ccc ddd eee fff ggg\n// hhh iii\nval a = 1\n",
      ),
      (
        "commented-out code kept",
        at(40),
        "// val x = f(a)\n// if (x) {\n// g()\n// }\nval a = 1\n",
        "// val x = f(a)\n// if (x) {\n// g()\n// }\nval a = 1\n",
      ),
      (
        "facts on their own // lines kept",
        at(80),
        "// prev = point(2)\n// curr = point(3)\nval a = 1\n",
        "// prev = point(2)\n// curr = point(3)\nval a = 1\n",
      ),
      (
        "an aligned table kept",
        at(40),
        "// name   value\n// foo    1\nval a = 1\n",
        "// name   value\n// foo    1\nval a = 1\n",
      ),
      (
        "list items with a hanging indent",
        at(40),
        "// - first item that is long enough to wrap around\n// - second\nval a = 1\n",
        "// - first item that is long enough to\n//   wrap around\n// - second\nval a = 1\n",
      ),
      (
        "an overlong one-line block comment broken in lines",
        at(30),
        "/* aaa bbb ccc ddd eee fff ggg hhh */\nval a = 1\n",
        "/* aaa bbb ccc ddd eee fff ggg\n   hhh */\nval a = 1\n",
      ),
      (
        "a block comment reflowed with its gutter",
        at(40),
        "/*\n * aaa bbb\n * ccc\n */\nval a = 1\n",
        "/*\n * aaa bbb ccc\n */\nval a = 1\n",
      ),
      (
        "a Scala-style scaladoc in the javadoc layout",
        at(80),
        "/** Summary line.\n  * More text.\n  */\ndef f = 1\n",
        "/**\n * Summary line. More text.\n */\ndef f = 1\n",
      ),
      (
        "a scaladoc without edge blank lines, with a blank line before its tags",
        at(80),
        "/**\n *\n * Summary.\n * @param x The input.\n *\n */\ndef f(x: Int) = x\n",
        "/**\n * Summary.\n *\n * @param x The input.\n */\ndef f(x: Int) = x\n",
      ),
      (
        "a wrapped tag continuing under its description",
        at(40),
        "/**\n * Summary.\n *\n * @param config The configuration read from the file.\n */\n" +
        "def f(config: Int) = config\n",
        "/**\n * Summary.\n *\n * @param config The configuration read\n *               from the file.\n */\n" +
        "def f(config: Int) = config\n",
      ),
      (
        "tag descriptions each after their own name",
        at(80),
        "/**\n * Summary.\n *\n * @param source The source.\n * @return     The result.\n */\n" +
        "def f(source: Int) = source\n",
        "/**\n * Summary.\n *\n * @param source The source.\n * @return The result.\n */\n" +
        "def f(source: Int) = source\n",
      ),
      (
        "tag descriptions aligned on one column",
        alignedTags(80),
        "/**\n * Summary.\n *\n * @param source The source.\n * @return The result.\n */\n" +
        "def f(source: Int) = source\n",
        "/**\n * Summary.\n *\n * @param source The source.\n * @return       The result.\n */\n" +
        "def f(source: Int) = source\n",
      ),
      (
        "a fitting one-line scaladoc kept",
        at(80),
        "/** The answer. */\nval a = 42\n",
        "/** The answer. */\nval a = 42\n",
      ),
      (
        "an overflowing one-line scaladoc expanded",
        at(30),
        "/** The answer to everything here. */\nval a = 42\n",
        "/**\n * The answer to everything\n * here.\n */\nval a = 42\n",
      ),
      (
        "no blank line between a scaladoc and its definition",
        at(80),
        "/** Doc. */\n\ndef f = 1\n",
        "/** Doc. */\ndef f = 1\n",
      ),
      (
        "a code block in a scaladoc kept",
        at(80),
        "/**\n * Example:\n * {{{\n *   val x = f(a)  // ok\n * }}}\n */\ndef f = 1\n",
        "/**\n * Example:\n * {{{\n *   val x = f(a)  // ok\n * }}}\n */\ndef f = 1\n",
      ),
    )

  "a comment" should {
    layoutRows(rows)
  }

}
