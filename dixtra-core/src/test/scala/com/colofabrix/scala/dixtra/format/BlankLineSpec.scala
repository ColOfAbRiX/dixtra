package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Blank lines: the author's blank lines are preserved everywhere (between statements, cases and comments, at any
 * nesting), with runs capped at one. A scaladoc always gets a blank line before it; other comments neither add nor
 * remove blank lines around them.
 */
class BlankLineSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "multiple blank lines between statements" should {

    "collapse to a single blank line" in {
      val input =
        """val a = 1
          |
          |
          |
          |val b = 2
          |""".stripMargin

      val expected =
        """val a = 1
          |
          |val b = 2
          |""".stripMargin

      input should formatTo(expected)
    }

  }

  "a single blank line between statements" should {

    "be preserved" in {
      val input =
        """val a = 1
          |
          |val b = 2
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

  }

  "no blank line between statements" should {

    "be preserved" in {
      val input =
        """val a = 1
          |val b = 2
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

  }

  "multiple blank lines inside a container body" should {

    "collapse to a single blank line" in {
      val input =
        """object A {
          |  val a = 1
          |
          |
          |  val b = 2
          |}""".stripMargin

      val expected =
        """object A {
          |  val a = 1
          |
          |  val b = 2
          |}""".stripMargin

      input should formatTo(expected)
    }

  }

  "blank lines in a def body" should {

    "be preserved, at most one, when the block loses its braces" in {
      val input =
        """def f = {
          |  val a = 1
          |
          |
          |  val b = 2
          |
          |  a + b
          |}""".stripMargin

      val expected =
        """def f =
          |  val a = 1
          |
          |  val b = 2
          |
          |  a + b""".stripMargin

      input should formatTo(expected)
    }

    "be preserved in a braceless body" in {
      val input =
        """def f =
          |  val a = 1
          |
          |  a
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "be dropped at the edges of a block that loses its braces" in {
      val input =
        """def f = {
          |
          |  val a = 1
          |  a
          |
          |}""".stripMargin

      val expected =
        """def f =
          |  val a = 1
          |  a""".stripMargin

      input should formatTo(expected)
    }

  }

  "blank lines in nested constructs" should {

    "be preserved in a lambda body" in {
      val input =
        """def f =
          |  xs.foreach { x =>
          |    val y = x
          |
          |    println(y)
          |  }
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "be removed at the edges of a lambda argument by default" in {
      val input =
        """def f =
          |  xs.foreach { x =>
          |
          |    val y = x
          |    println(y)
          |  }
          |""".stripMargin

      val expected =
        """def f =
          |  xs.foreach { x =>
          |    val y = x
          |    println(y)
          |  }
          |""".stripMargin

      input should formatTo(expected)
    }

    "be preserved in an if/else branch" in {
      val input =
        """def f =
          |  if c then
          |    val a = 1
          |
          |    a
          |  else
          |    2
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "be preserved between and inside cases" in {
      val input =
        """def f(x: Int) =
          |  x match {
          |    case 1 =>
          |      val a = "a"
          |
          |      a
          |
          |    case _ =>
          |      "b"
          |  }
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

  }

  "blank lines around a comment in a block" should {

    "be kept where present and not added where absent" in {
      val input =
        """def f =
          |  val a = 1
          |
          |  // note
          |  val b = 2
          |  // other
          |
          |  a + b
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

  }

  "a line or block comment" should {

    "not get blank lines added around it" in {
      val input =
        """val x = 1
          |// c
          |val y = 2
          |/* d */
          |val z = 3
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

  }

  "a scaladoc comment" should {

    "get a blank line before it" in {
      val input =
        """val x = 1
          |/** Doc. */
          |val y = 2
          |""".stripMargin

      val expected =
        """val x = 1
          |
          |/** Doc. */
          |val y = 2
          |""".stripMargin

      input should formatTo(expected)
    }

    "get a blank line before it when multiline (javadoc style)" in {
      val input =
        """object A {
          |  val x = 1
          |  /**
          |   * Doc.
          |   */
          |  val y = 2
          |}""".stripMargin

      val expected =
        """object A {
          |  val x = 1
          |
          |  /**
          |   * Doc.
          |   */
          |  val y = 2
          |}""".stripMargin

      input should formatTo(expected)
    }

    "get a blank line before it and after it on a one-line definition" in {
      val table =
        List(
          "val a = 1\n/** doc */\ndef f = 2\nval bbb = 3\n" -> "val a = 1\n\n/** doc */\ndef f = 2\n\nval bbb = 3\n",
          "def f =\n  val a = 1\n  /** Doc. */\n  val b = 2\n  b\n" ->
          "def f =\n  val a = 1\n\n  /** Doc. */\n  val b = 2\n\n  b\n",
          // The definitions after it align as a group of their own
          "class Temp {\n  val a = 1\n  /** Doc. */\n  val bb = 2\n  val ccc = 3\n  val d = 4\n}\n" ->
          "class Temp {\n  val a = 1\n\n  /** Doc. */\n  val bb = 2\n\n  val ccc = 3\n  val d   = 4\n}\n",
        )

      table.foreach((input, expected) => input should formatStablyTo(expected))
    }

    "keep a single existing blank line before it" in {
      val input =
        """val x = 1
          |
          |
          |/** Doc. */
          |val y = 2
          |""".stripMargin

      val expected =
        """val x = 1
          |
          |/** Doc. */
          |val y = 2
          |""".stripMargin

      input should formatTo(expected)
    }

    "get no blank line at the start of the file" in {
      val input =
        """/** Doc. */
          |val y = 2
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

  }

  "blank lines between the cases of a match" should {

    "keep a single blank line at the end of a case followed by another case" in {
      val input =
        """def f =
          |  x match {
          |    case 1 =>
          |      a()
          |      b()
          |
          |
          |    case 2 => c
          |  }
          |""".stripMargin

      val expected =
        """def f =
          |  x match {
          |    case 1 =>
          |      a()
          |      b()
          |
          |    case 2 =>
          |      c
          |  }
          |""".stripMargin

      input should formatTo(expected)
    }

    "drop the blank lines after the last case" in {
      val input =
        """def f =
          |  x match {
          |    case 1 => a
          |    case 2 => c
          |
          |  }
          |""".stripMargin

      val expected =
        """def f =
          |  x match {
          |    case 1 => a
          |    case 2 => c
          |  }
          |""".stripMargin

      input should formatTo(expected)
    }

    "drop the blank lines before the first case" in {
      val input =
        """def f =
          |  x match {
          |
          |    case 1 => a
          |    case 2 => c
          |  }
          |""".stripMargin

      val expected =
        """def f =
          |  x match {
          |    case 1 => a
          |    case 2 => c
          |  }
          |""".stripMargin

      input should formatTo(expected)
    }

    "drop the blank lines at the edges of a partial function and of a catch" in {
      val input =
        """def f =
          |  try {
          |    g()
          |  } catch {
          |    case e: A => 1
          |
          |    case e: B => 2
          |
          |  }
          |""".stripMargin

      val expected =
        """def f =
          |  try {
          |    g()
          |  } catch {
          |    case e: A => 1
          |
          |    case e: B => 2
          |  }
          |""".stripMargin

      input should formatTo(expected)
    }

  }

  "blank lines between imports" should {

    "be removed" in {
      val input    = "package a\n\nimport b.C\n\nimport d.E\n\n\nimport f.G\n\nobject H\n"
      val expected = "package a\n\nimport b.C\nimport d.E\nimport f.G\n\nobject H\n"
      input should formatStablyTo(expected)
    }

    "be removed around a comment, keeping the comment" in {
      val input    = "import b.C\n\n// Effects\n\nimport d.E // trailing\n\nimport f.G\n"
      val expected = "import b.C\n// Effects\nimport d.E // trailing\nimport f.G\n"
      input should formatTo(expected)
    }

    "be removed after an import over several lines" in {
      val input    = "import b.{\n  C,\n  D,\n}\n\nimport e.F\n"
      val expected = "import b.{\n  C, D,\n}\nimport e.F\n"
      input should formatTo(expected, 12)
    }

    "be removed inside a body" in {
      val input    = "object A {\n  import b.C\n\n  import d.E\n\n  val x = 1\n}\n"
      val expected = "object A {\n  import b.C\n  import d.E\n\n  val x = 1\n}\n"
      input should formatTo(expected)
    }

    "keep a block comment over several lines between imports as it is" in {
      val input    = "import b.C\n\n/* one\n\n    two */\nimport d.E\n"
      val expected = input
      input should formatTo(expected)
    }

  }

}
