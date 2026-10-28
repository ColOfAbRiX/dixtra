package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * `given` and `extension` definitions are formatted and separated exactly like `def`s: a given alias body like a `val`,
 * extension methods like methods.
 */
class GivenExtensionSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "given and extension definitions" should {
    "be separated from their neighbours exactly like defs" in {
      val input =
        """object A {
          |  val x = 1
          |  given Ordering[Int] = ???
          |  def f = 1
          |  extension (i: Int)
          |    def double = i * 2
          |    def triple = i * 3
          |}
          |""".stripMargin
      val expected =
        """object A {
          |  val x = 1
          |
          |  given Ordering[Int] = ???
          |
          |  def f = 1
          |
          |  extension (i: Int) {
          |    def double = i * 2
          |    def triple = i * 3
          |  }
          |}
          |""".stripMargin
      input should formatTo(expected)
    }

    "have a given alias body formatted like a val" in {
      val input = "given xs: List[Int] = List(a,\n  b)\n"
      val expected =
        """given xs: List[Int] =
          |  List(
          |    a,
          |    b,
          |  )
          |""".stripMargin
      input should formatTo(expected)
    }

    "have extension methods formatted like defs" in {
      val input = "extension (i: Int)\n  def xs = List(a,\n    b)\n"
      val expected =
        """extension (i: Int)
          |  def xs =
          |    List(
          |      a,
          |      b,
          |    )
          |""".stripMargin
      input should formatTo(expected)
    }
  }

}
