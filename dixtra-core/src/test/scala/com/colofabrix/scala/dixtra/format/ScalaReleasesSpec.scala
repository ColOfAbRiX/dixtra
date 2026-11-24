package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.Config
import com.colofabrix.scala.dixtra.Dixtra
import com.colofabrix.scala.dixtra.SourceDialect
import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * dixtra is built with Scala 3.9.0 and formats the sources of every Scala 3 release: each fixture uses the syntax a
 * minor release made standard (3.1, 3.2 and 3.3 added none) and is already formatted, so it must parse, survive the
 * equivalence check and stay as it is.
 */
class ScalaReleasesSpec extends AnyWordSpec with Matchers with FormatMatchers {

  private val releases =
    List(
      "3.0" ->
      """enum Color {
        |  case Red, Green
        |}
        |
        |opaque type Id = Int
        |
        |extension (x: Int) {
        |  def double: Int = x * 2
        |}
        |
        |given Ordering[Id] = Ordering.Int
        |
        |def first(using o: Ordering[Id]): Int = 1
        |
        |type Pair = [A] =>> (A, A)
        |
        |def describe(x: Int | String): String =
        |  if x == 1 then
        |    "one"
        |  else
        |    "other"
        |""".stripMargin,
      "3.4" ->
      """def count(xs: List[?]): Int = sum(xs*)
        |
        |def head(xs: Seq[Int]): Int =
        |  xs match {
        |    case Seq(first, rest*) => first
        |    case _                 => 0
        |  }
        |""".stripMargin,
      "3.5" ->
      """val mask = 0b1010_1010
        |""".stripMargin,
      "3.6" ->
      """def max[A: Ordering as ord](x: A, y: A): A = ord.max(x, y)
        |
        |def show[A: {Ordering, Numeric}](x: A): String = x.toString
        |
        |def pair[A](a: A)[B](b: B): (A, B) = (a, b)
        |
        |given listOrdering: [A: Ordering] => Ordering[List[A]] = ???
        |""".stripMargin,
      "3.7" ->
      """val person = (name = "Bob", age = 33)
        |
        |type Person = (name: String, age: Int)
        |
        |def name(p: Person): String =
        |  p match {
        |    case (name = n, age = _) => n
        |  }
        |""".stripMargin,
      "3.8" ->
      """def total(xs: Option[Int]): Option[Int] =
        |  for
        |    a  = 1
        |    b <- xs
        |  yield
        |    a + b
        |""".stripMargin,
      "3.9" ->
      """into trait Shape
        |
        |into class Circle(radius: Double)
        |""".stripMargin,
    )

  "the syntax of a Scala 3 release" should {

    releases.foreach { (release, source) =>
      s"be formatted and stable for Scala $release" in {
        val expected = source
        source should formatTo(expected)
      }
    }

    // A known limitation: Scalameta (4.17.4) does not parse `into` on a type yet, so such a file fails and is left as
    // it is. This test fails once it does: move the source to the 3.9 fixture then.
    "fail on the into modifier of a type (Scala 3.9), until Scalameta parses it" in {
      val expected = true
      val actual   = Dixtra.format("def concat(xs: into Seq[Int]): Seq[Int] = xs\n").isLeft
      actual shouldBe expected
    }

    "skip the constructs that Scala 3 dropped" in {
      val dropped =
        List(
          "object A {\n  def f() { println(1) }\n}\n",
          "object A {\n  def f = do { g() } while (true)\n}\n",
          "object A {\n  type T = List[X] forSome { type X }\n}\n",
          "object A {\n  def f: Int = macro impl\n}\n",
        )

      val expected = dropped.map(_ => Left("Scala2Error"))

      val actual = dropped.map(Dixtra.format(_).left.map(_.getClass.getSimpleName))

      actual shouldBe expected
    }

    "skip a source that parses only as Scala 2, as a source file and as a build file" in {
      val source    = "object A {\n  val then = 1\n}\n"
      val buildFile = "val then = 1\n"

      val skipped  = Left(("Scala2Error", "the file is Scala 2: it parses as Scala 2 but not as Scala 3"))
      val expected = (skipped, skipped)

      val asSource    = Dixtra.format(source, Config.default, SourceDialect.scala)
      val asBuildFile = Dixtra.format(buildFile, Config.default, SourceDialect.sbt)

      val actual =
        (
          asSource.left.map(error => (error.getClass.getSimpleName, error.getMessage)),
          asBuildFile.left.map(error => (error.getClass.getSimpleName, error.getMessage)),
        )

      actual shouldBe expected
    }

    "skip a construct that Scala 3 dropped with compat213 too, naming it" in {
      val source = "object A {\n  def f() { println(1) }\n}\n"
      val config = Config.default.copy(compat213 = true)

      val expected = Left(("Scala2Error", "the file is Scala 2: it uses procedure syntax, which Scala 3 dropped"))

      val actual =
        Dixtra
          .format(source, config)
          .left
          .map(error => (error.getClass.getSimpleName, error.getMessage))

      actual shouldBe expected
    }

  }

}
