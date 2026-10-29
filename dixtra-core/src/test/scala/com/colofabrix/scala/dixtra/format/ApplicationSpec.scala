package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Function applications (D5): arguments break one per line only on an author break inside the call or on overflow. A
 * `{ ... }` argument (lambda, multi-statement block, or `{ case ... }`) always keeps its braces and is never rewritten
 * as `( ... )`; a `using` clause keeps its keyword. Exploded arguments end with a trailing comma. The argument groups
 * of a call that does not fit on one line all break, glued by `call-site.groups-separator`.
 */
class ApplicationSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "a using argument clause" should {

    "keep the using keyword" in {
      val input = "val u = f(using a)\n"

      val expected = input

      input should formatTo(expected)
    }

  }

  "a partial-function argument" should {

    "keep its braces rather than become parentheses" in {
      val input = "val p = f { case a => a }\n"

      val expected = input

      input should formatTo(expected)
    }

  }

  "a trailing multi-statement brace block" should {

    val expected =
      """val x =
        |  process {
        |    step1
        |    step2
        |  }
        |""".stripMargin

    "move below = keeping its braces (D5)" in {
      val input =
        """val x = process {
          |  step1
          |  step2
          |}
          |""".stripMargin

      input should formatStablyTo(expected)
    }

  }

  "a block used as an argument" should {

    "keep its braces when the argument list explodes" in {
      val input = "val x = foo(someArgument, { val a = 1; a }, anotherArgumentThatIsLong, yetAnotherOne, more)\n"

      val expected =
        """val x =
          |  foo(
          |    someArgument,
          |    { val a = 1; a },
          |    anotherArgumentThatIsLong,
          |    yetAnotherOne,
          |    more,
          |  )
          |""".stripMargin

      input should formatTo(expected, 80)
    }

  }

  "an exploded argument list" should {

    "end with a trailing comma, which stays" in {
      val input =
        """val x =
          |  f(
          |    a,
          |    b
          |  )
          |""".stripMargin

      val expected =
        """val x =
          |  f(
          |    a,
          |    b,
          |  )
          |""".stripMargin

      input should formatStablyTo(expected)
    }

    "put the trailing comma before a trailing comment" in {
      val input =
        """val x =
          |  f(
          |    a,
          |    b // last
          |  )
          |""".stripMargin

      val expected =
        """val x =
          |  f(
          |    a,
          |    b, // last
          |  )
          |""".stripMargin

      input should formatTo(expected)
    }

  }

  "the argument groups of a call" should {
    val input =
      "val r = Resource.make(IO.blocking(Files.createTempFile(target.getParent, \"tmp\", \".part\")))(temp => IO.blocking(temp).void)\n"

    "stay on one line when they fit" in {
      val oneLine = "val r = Resource.make(IO.blocking(create()))(temp => IO.blocking(temp).void)\n"

      val expected = oneLine

      oneLine should formatTo(expected)
    }

    "all break, with ) ( after the last argument of the group before by default" in {
      val expected =
        """val r =
          |  Resource.make(
          |    IO.blocking(Files.createTempFile(target.getParent, "tmp", ".part")))(
          |    temp => IO.blocking(temp).void,
          |  )
          |""".stripMargin

      input should formatStablyTo(expected, 80)
    }

    "open the next group with ) ( with call-site.groups-separator = next-group" in {
      val config    = Config.withMaxColumn(80)
      val nextGroup = withCallSite(_.copy(groupsSeparator = GroupsSeparator.NextGroup), config)

      val expected =
        """val r =
          |  Resource.make(
          |    IO.blocking(Files.createTempFile(target.getParent, "tmp", ".part")),
          |  )(temp => IO.blocking(temp).void,
          |  )
          |""".stripMargin

      input should formatStablyTo(expected, nextGroup)
    }

    "close with ) aligned with the selection in a broken chain, with call-site.groups-break = all" in {
      val chained =
        "val r = xs.filter(_.isValid).map(_.name).make(IO.blocking(Files.createTempFile(parent, \"tmp\", \".part\")))(temp => IO.blocking(temp).void)\n"
      val config = Config.withMaxColumn(80)
      val all    = withCallSite(_.copy(groupsBreak = GroupsBreak.BreakAll), config)

      val expected =
        """val r =
          |  xs
          |    .filter(_.isValid)
          |    .map(_.name)
          |    .make(
          |      IO.blocking(Files.createTempFile(parent, "tmp", ".part")))(
          |      temp => IO.blocking(temp).void,
          |    )
          |""".stripMargin

      chained should formatStablyTo(expected, all)
    }

    "leave a trailing brace block to its own rules" in {
      val braced =
        "val r = foldAll(initialValueWithALongName, anotherArgumentWithALongName)(using ordering) { (acc, x) => acc + x }\n"

      val expected =
        """val r =
          |  foldAll(initialValueWithALongName, anotherArgumentWithALongName)(using
          |    ordering,
          |  ) { (acc, x) => acc + x }
          |""".stripMargin

      braced should formatStablyTo(expected, 80)
    }
  }

  "call-site.groups-break" should {
    val input =
      "val e = Option.when(gap.isBlank && text.startsLine(brace.start))(Edit(brace.end, keyword.start, text.indentOf(brace.start)))\n"

    "break only the last group while the groups before it fit, with last-first" in {
      val expected =
        """val e =
          |  Option.when(gap.isBlank && text.startsLine(brace.start))(
          |    Edit(brace.end, keyword.start, text.indentOf(brace.start)),
          |  )
          |""".stripMargin

      input should formatStablyTo(expected, 80)
    }

    "break every group, with break-all" in {
      val config = Config.withMaxColumn(80)
      val all    = withCallSite(_.copy(groupsBreak = GroupsBreak.BreakAll), config)

      val expected =
        """val e =
          |  Option.when(
          |    gap.isBlank && text.startsLine(brace.start))(
          |    Edit(brace.end, keyword.start, text.indentOf(brace.start)),
          |  )
          |""".stripMargin

      input should formatStablyTo(expected, all)
    }

    "let each group decide by itself, with keep" in {
      val mixed =
        """val r =
          |  Resource.make(
          |    IO.blocking(Files.createTempFile(directory, prefix, ".tmp")),
          |  )(temp => IO.blocking(temp).void)
          |""".stripMargin
      val config = Config.withMaxColumn(80)
      val keep   = withCallSite(_.copy(groupsBreak = GroupsBreak.Independent), config)

      val expected = mixed

      mixed should formatTo(expected, keep)
    }
  }

  "a tuple" should {

    "stay on one line when it fits" in {
      val input = "val pair = (first, second)\n"

      val expected = input

      input should formatTo(expected)
    }

    "break one element per line when it does not fit" in {
      val input = "val tuple = (firstElement, secondElement, thirdElement)\n"

      val expected =
        """val tuple =
          |  (
          |    firstElement,
          |    secondElement,
          |    thirdElement,
          |  )
          |""".stripMargin

      input should formatTo(expected, 30)
    }

    "break as an argument that fits only without its comma" in {
      val input = "val xs =\n  List(\n    (alpha, beta),\n    (gamma, delta),\n  )\n"

      val expected = "val xs =\n  List(\n    (alpha, beta),\n    (\n      gamma,\n      delta,\n    ),\n  )\n"

      input should formatTo(expected, 18)
    }

  }

}
