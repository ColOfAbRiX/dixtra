package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Function types: exactly one space around `=>` in a `Type.Function`, e.g. `A => B` and `(A, B) => C`.
 *
 * Spacing is canonicalised by the normalisation passes: the whitespace pass collapses redundant spaces and the
 * arrow-spacing pass inserts a single space where one is missing.
 *
 * The type of a type definition is an operation when it is a run of `|`, `&` or `=>`: on one line, or one operand per
 * line with the operator at the end of the line before.
 */
class FunctionTypeSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "a function type" should {

    "have exactly one space around =>" in {
      val table =
        List(
          "val f: Int => String = _.toString\n"     -> "val f: Int => String = _.toString\n",
          "val f: (Int, Int) => Int = _ + _\n"      -> "val f: (Int, Int) => Int = _ + _\n",
          "val f: Int   =>   String = _.toString\n" -> "val f: Int => String = _.toString\n",
          "val f: Int=>String = _.toString\n"       -> "val f: Int => String = _.toString\n",
        )

      table.foreach { (input, expected) =>
        input should formatTo(expected)
      }
    }

  }

  "the type of a type definition" should {

    "keep the type written below the = there, with its spacing fixed" in {
      val input    = "type Handler =\n       Request   =>   IO[Response]\n"
      val expected = "type Handler =\n  Request => IO[Response]\n"
      input should formatStablyTo(expected)
    }

    "go below the = when it fits there only" in {
      val input    = "type Event = UserCreated | UserDeleted | OrderPlaced | OrderShipped\n"
      val expected = "type Event =\n  UserCreated | UserDeleted | OrderPlaced | OrderShipped\n"
      input should formatStablyTo(expected, 60)
    }

    "break a union that does not fit below the =, one operand per line with | at the end of the line" in {
      val input = "type Event = UserCreated | UserDeleted | OrderPlaced | OrderShipped | OrderCancelled | Refunded\n"

      val expected =
        """type Event =
          |  UserCreated |
          |  UserDeleted |
          |  OrderPlaced |
          |  OrderShipped |
          |  OrderCancelled |
          |  Refunded
          |""".stripMargin

      input should formatStablyTo(expected, 60)
    }

    "break a function type the same way, at its =>" in {
      val input = "type Pipeline = Request => Validated[Request] => IO[Response] => Logged[IO[Response]]\n"

      val expected =
        """type Pipeline =
          |  Request =>
          |  Validated[Request] =>
          |  IO[Response] =>
          |  Logged[IO[Response]]
          |""".stripMargin

      input should formatStablyTo(expected, 40)
    }

    "go below the = as a match type, its cases one per line in braces, the arrows aligned" in {
      val input =
        """type Elem[X] = X match {
          |  case String => Char
          |  case Array[t] =>   t
          |  case Iterable[t] => t
          |}
          |""".stripMargin

      val expected =
        """type Elem[X] =
          |  X match {
          |    case String      => Char
          |    case Array[t]    => t
          |    case Iterable[t] => t
          |  }
          |""".stripMargin

      input should formatStablyTo(expected)
    }

    "add braces to a braceless match type with the default braces.match" in {
      val input =
        """type Head[T <: Tuple] = T match
          |  case h *: _ =>   h
          |  case EmptyTuple => Nothing
          |""".stripMargin

      val expected =
        """type Head[T <: Tuple] =
          |  T match {
          |    case h *: _     => h
          |    case EmptyTuple => Nothing
          |  }
          |""".stripMargin

      input should formatStablyTo(expected)
    }

    "keep an operand in parentheses whole" in {
      val input = "type Mixed = (Alpha & Beta) | Gamma | Delta | Epsilon | Zeta | Eta | Theta\n"

      val expected =
        """type Mixed =
          |  (Alpha & Beta) |
          |  Gamma |
          |  Delta |
          |  Epsilon |
          |  Zeta |
          |  Eta |
          |  Theta
          |""".stripMargin

      input should formatStablyTo(expected, 40)
    }

  }

}
