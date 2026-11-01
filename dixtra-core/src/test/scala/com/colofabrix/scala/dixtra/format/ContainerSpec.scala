package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Containers: object / class / trait bodies and packages have their members formatted in place while the header,
 * braces, and surrounding whitespace are preserved verbatim. Padding inside braces follows the top: a blank line after
 * `{` (or a multiline scaladoc right after it) means a blank line before `}`, and no blank line after `{` means none
 * before `}`. The padding of a body with one member is covered by the `blank-lines.body-edges` rows of
 * [[LayoutSettingsSpec]], and a scaladoc right after the brace by its `blank-lines.before-scaladoc` tests.
 */
class ContainerSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "an object, a class or a trait body" should {

    "format its members while preserving the header and braces" in {
      List("object", "class", "trait").foreach { keyword =>
        val input =
          s"""$keyword A {
             |  val xs = List(
             |    elementOne,
             |    elementTwo
             |  )
             |}""".stripMargin

        val expected =
          s"""$keyword A {
             |  val xs =
             |    List(
             |      elementOne,
             |      elementTwo,
             |    )
             |}""".stripMargin

        input should formatTo(expected)
      }
    }

  }

  "container padding" should {

    "count a multiline scaladoc right after the brace as a leading blank" in {
      val input =
        """class Temp {
          |  /**
          |   * Doc.
          |   */
          |  def myMethod() = ???
          |}""".stripMargin

      val expected =
        """class Temp {
          |
          |  /**
          |   * Doc.
          |   */
          |  def myMethod() = ???

          |}""".stripMargin

      input should formatTo(expected)
    }

    "separate one-line methods that have a scaladoc" in {
      val input =
        """class Temp {
          |   /** Doc. */
          |   def myMethod() = ???
          |   /** Doc. */
          |   def myMethod1() = ???
          |   /** Doc. */
          |   def myMethod2() = ???
          |}""".stripMargin

      val expected =
        """class Temp {
          |
          |   /** Doc. */
          |   def myMethod() = ???
          |
          |   /** Doc. */
          |   def myMethod1() = ???
          |
          |   /** Doc. */
          |   def myMethod2() = ???
          |
          |}""".stripMargin

      input should formatStablyTo(expected)
    }

    "not separate one-line methods that have a comment or an annotation" in {
      val input =
        """class Temp {
          |   def a() = 1
          |   // Note
          |   def b() = 2
          |   @inline
          |   def c() = 3
          |}""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

  }

  "blank line between methods" should {

    "keep a single blank line between two methods" in {
      val input =
        """class A {
          |  def f() = 1
          |
          |  def g() = 2
          |}""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "add a blank line between two adjacent methods" in {
      val input =
        """class A {
          |  def f() =
          |    1
          |  def g() =
          |    2
          |}""".stripMargin

      val expected =
        """class A {
          |  def f() =
          |    1
          |
          |  def g() =
          |    2
          |}""".stripMargin

      input should formatTo(expected)
    }

  }

  "container padding with multiple members" should {

    "keep both blanks when both are present" in {
      val input =
        """class A {
          |
          |  def f() = 1
          |
          |  def g() = 2
          |
          |}""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "add a trailing blank when only the leading blank is present" in {
      val input =
        """class A {
          |
          |  def f() = 1
          |
          |  def g() = 2
          |}""".stripMargin

      val expected =
        """class A {
          |
          |  def f() = 1
          |
          |  def g() = 2
          |
          |}""".stripMargin

      input should formatTo(expected)
    }

    "add the leading blank when only the trailing blank is written" in {
      val input =
        """class A {
          |  def f() = 1
          |
          |  def g() = 2
          |
          |}""".stripMargin

      val expected =
        """class A {
          |
          |  def f() = 1
          |
          |  def g() = 2
          |
          |}""".stripMargin

      input should formatTo(expected)
    }

  }

  "a braced container with a self type" should {

    "balance its padding" in {
      val input =
        """trait T { self =>
          |
          |  val x = 1
          |}
          |""".stripMargin

      val expected =
        """trait T { self =>
          |
          |  val x = 1
          |
          |}
          |""".stripMargin

      input should formatTo(expected)
    }

  }

  "a braceless container" should {

    "not have padding inserted around nested braces" in {
      val input =
        """object A:
          |
          |  val x = 1
          |""".stripMargin

      val expected = input

      input should formatTo(expected, withBraces(Construct.Object -> BracePolicy.Keep))
    }

    "get braces, with the padding balanced, when the brace style uses them" in {
      val input =
        """object A:
          |
          |  val x = 1
          |""".stripMargin

      val expected =
        """object A {
          |
          |  val x = 1
          |
          |}
          |""".stripMargin

      input should formatTo(expected)
    }

  }

  "package clauses" should {

    "format the members of a file with a package clause" in {
      val input =
        """package a
          |
          |object A {
          |  val xs = List(a,
          |      b)
          |}
          |""".stripMargin

      val expected =
        """package a
          |
          |object A {
          |  val xs =
          |    List(
          |      a,
          |      b,
          |    )
          |}
          |""".stripMargin

      input should formatTo(expected)
    }

    "format the members of nested package clauses" in {
      val input =
        """package a
          |package b
          |
          |val xs = List(a,
          |  b)
          |""".stripMargin

      val expected =
        """package a
          |package b
          |
          |val xs =
          |  List(
          |    a,
          |    b,
          |  )
          |""".stripMargin

      input should formatTo(expected)
    }

    "format the members of a package object" in {
      val input =
        """package object a {
          |  val xs = List(a,
          |    b)
          |}
          |""".stripMargin

      val expected =
        """package object a {
          |  val xs =
          |    List(
          |      a,
          |      b,
          |    )
          |}
          |""".stripMargin

      input should formatTo(expected)
    }

  }

}
