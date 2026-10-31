package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.testkit.FormatMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Alignment, decided after whitespace clean-up and before line breaking, so the padding counts towards the max column.
 *
 * Consecutive one-line `val` / `lazy val` / `def` definitions align their `=`; anything else between them (a blank
 * line, a comment line, a multi-line definition, an expression) starts a new group, and a definition that would not fit
 * once aligned leaves the group. A one-line `def` in such a block is not separated by a blank line, so it aligns like a
 * `val`. The enumerators of a for-comprehension align the start of their right-hand side, across multi-line right-hand
 * sides. Consecutive one-line `case` clauses align their arrows. The lines of a multi-line operator expression all
 * start at the column of its first operand.
 */
class AlignmentSpec extends AnyWordSpec with Matchers with FormatMatchers {

  "consecutive definitions" should {

    "align their = signs, per group" in {
      val input =
        """class A {
          |  private val ctx = new RenderContext(original, config)
          |  private val terms = new TermRenderer(ctx, renderDefinition)
          |  private val asss = new AssignmentRenderer(ctx, terms)
          |
          |  private val defs = new DefRenderer(ctx, terms, assignments)
          |  private val containers = new ContainerRenderer(ctx, renderNode)
          |}""".stripMargin

      val expected =
        """class A {
          |  private val ctx   = new RenderContext(original, config)
          |  private val terms = new TermRenderer(ctx, renderDefinition)
          |  private val asss  = new AssignmentRenderer(ctx, terms)
          |
          |  private val defs       = new DefRenderer(ctx, terms, assignments)
          |  private val containers = new ContainerRenderer(ctx, renderNode)
          |}""".stripMargin

      input should formatTo(expected)
    }

    "be idempotent" in {
      val input =
        """val ctx   = 1
          |val terms = 2
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "align lazy vals with vals" in {
      val input =
        """val a = 1
          |lazy val bb = 2
          |""".stripMargin

      val expected =
        """val a       = 1
          |lazy val bb = 2
          |""".stripMargin

      input should formatTo(expected)
    }

    "align on the = after a type ascription" in {
      val input =
        """val a: Int = 1
          |val bb = 2
          |""".stripMargin

      val expected =
        """val a: Int = 1
          |val bb     = 2
          |""".stripMargin

      input should formatTo(expected)
    }

    "align vars with vals, lazy vals and defs" in {
      val input =
        """def foldNewest[B](n: Int)(z: B)(f: (B, A) => B): B =
          |  val count = Math.max(1, Math.min(n, length))
          |  val start = length - count
          |  var acc = z
          |  var i = start
          |  while i < length do
          |    acc = f(acc, readAt(i))
          |    i += 1
          |  acc
          |""".stripMargin

      val expected =
        """def foldNewest[B](n: Int)(z: B)(f: (B, A) => B): B =
          |  val count = Math.max(1, Math.min(n, length))
          |  val start = length - count
          |  var acc   = z
          |  var i     = start
          |  while i < length do
          |    acc = f(acc, readAt(i))
          |    i   += 1
          |  acc
          |""".stripMargin

      input should formatTo(expected, withBraces(Construct.While -> BracePolicy.Keep))
    }

    "keep consecutive one-line declarations together" in {
      val input =
        """trait PointReader[A] {
          |  def time(point: A): Instant
          |  def altitude(point: A): Double
          |  def speed(point: A): GeoVector
          |}
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "align a def with the vals that follow it" in {
      val input =
        """def a = 1
          |val bbb = 2
          |""".stripMargin

      val expected =
        """def a   = 1
          |val bbb = 2
          |""".stripMargin

      input should formatTo(expected)
    }

    "align a def with the vals before it, in the same block" in {
      val input =
        """lazy val prevEnd = ctx.span(prev)._2
          |val nextStart = ctx.span(next)._1
          |def gap = ctx.slice(prevEnd, nextStart)
          |""".stripMargin

      val expected =
        """lazy val prevEnd = ctx.span(prev)._2
          |val nextStart    = ctx.span(next)._1
          |def gap          = ctx.slice(prevEnd, nextStart)
          |""".stripMargin

      input should formatTo(expected)
    }

    "align consecutive one-line defs" in {
      val input =
        """object A {
          |  def a = 1
          |  def bbb = 2
          |}""".stripMargin

      val expected =
        """object A {
          |  def a   = 1
          |  def bbb = 2
          |}""".stripMargin

      input should formatTo(expected)
    }

    "still separate a multi-line def from the definitions before it" in {
      val input =
        """val a = 1
          |def f =
          |  a + 1
          |""".stripMargin

      val expected =
        """val a = 1
          |
          |def f =
          |  a + 1
          |""".stripMargin

      input should formatTo(expected)
    }

    "still separate a one-line def from a multi-line definition before it" in {
      val input =
        """val a =
          |  1
          |def f = 2
          |""".stripMargin

      val expected =
        """val a =
          |  1
          |
          |def f = 2
          |""".stripMargin

      input should formatTo(expected)
    }

    "align a def that starts a new block with the definitions that follow it" in {
      val input =
        """val a =
          |  1
          |def f = 2
          |val bbb = 3
          |""".stripMargin

      val expected =
        """val a =
          |  1
          |
          |def f   = 2
          |val bbb = 3
          |""".stripMargin

      input should formatTo(expected)
    }

    "keep aligning past a trailing comment" in {
      val input =
        """val a = 1 // note
          |val bbb = 2
          |""".stripMargin

      val expected =
        """val a   = 1 // note
          |val bbb = 2
          |""".stripMargin

      input should formatTo(expected)
    }

    "not align the definitions on either side of a multi-line definition" in {
      val input =
        """val a = 1
          |val bb =
          |  2
          |val ccc = 3
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "not align across an expression" in {
      val input =
        """def f =
          |  val a = 1
          |  println(a)
          |  val bbb = 2
          |  bbb
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "align inside a block" in {
      val input =
        """def f =
          |  val a = 1
          |  val bbb = 2
          |  a + bbb
          |""".stripMargin

      val expected =
        """def f =
          |  val a   = 1
          |  val bbb = 2
          |  a + bbb
          |""".stripMargin

      input should formatTo(expected)
    }

    "break a definition that would not fit once aligned, and align the others" in {
      val input =
        """val a = abcdefghijklmnopqrst
          |val bb = 1
          |val cccc = 2
          |""".stripMargin

      val expected =
        """val a =
          |  abcdefghijklmnopqrst
          |val bb   = 1
          |val cccc = 2
          |""".stripMargin

      input should formatStablyTo(expected, 30)
    }

    "align the definitions before and after a broken one separately" in {
      val input =
        """val a = 1
          |val bbbbbb = 2
          |val c = abcdefghijklmnopqrstu
          |val dd = 3
          |def e = 4
          |""".stripMargin

      val expected =
        """val a      = 1
          |val bbbbbb = 2
          |val c =
          |  abcdefghijklmnopqrstu
          |val dd = 3
          |def e  = 4
          |""".stripMargin

      input should formatStablyTo(expected, 30)
    }

  }

  "a for-comprehension" should {

    "align the start of every right-hand side" in {
      val input =
        """def f =
          |  for {
          |    tree <- parse(1f)
          |    formatted = render(1f, tree, config)
          |    verified <- verify(1f, tree, formatted)
          |  } yield verified
          |""".stripMargin

      val expected =
        """def f =
          |  for {
          |    tree     <- parse(1f)
          |    formatted = render(1f, tree, config)
          |    verified <- verify(1f, tree, formatted)
          |  } yield verified
          |""".stripMargin

      input should formatTo(expected, withBraces(Construct.ForYield -> BracePolicy.Keep))
    }

    "carry the alignment past a multi-line right-hand side" in {
      val input =
        """def f =
          |  for {
          |    documents <- streams.currentDocs
          |    data <- IO.pure(
          |      docs
          |        .collect { case (doc: Doc[TestDoc], e: Deleted) => doc -> e }
          |          .runWith(List.seq)
          |    )
          |    result <- process(data)
          |  } yield data
          |""".stripMargin
      val keepChains =
        Config
          .default
          .selectorChain
          .copy(inFor = NestedChainPolicy.Keep, inCall = NestedChainPolicy.Keep)

      val expected =
        """def f =
          |  for {
          |    documents <- streams.currentDocs
          |    data      <- IO.pure(
          |                   docs
          |                     .collect { case (doc: Doc[TestDoc], e: Deleted) => doc -> e }
          |                     .runWith(List.seq),
          |                 )
          |    result    <- process(data)
          |  } yield data
          |""".stripMargin

      input should
      formatTo(expected, withBraces(Construct.ForYield -> BracePolicy.Keep).copy(selectorChain = keepChains))
    }

    "start a guard on its own line at the column of the expressions" in {
      val input =
        """def f =
          |  for {
          |    a <- xs
          |    if a > 1
          |    bbb <- ys
          |  } yield a
          |""".stripMargin

      val expected =
        """def f =
          |  for {
          |    a   <- xs
          |           if a > 1
          |    bbb <- ys
          |  } yield a
          |""".stripMargin

      input should formatTo(expected, withBraces(Construct.ForYield -> BracePolicy.Keep))
    }

    "keep a guard on the line of its generator while it fits" in {
      val input =
        """def f =
          |  for {
          |    rolloutStartIdx <- findRolloutStart(dataPoints) if isValidRolloutStart(dataPoints, rolloutStartIdx)
          |    turnInitIdx <- findTurnInitiation(dataPoints, rolloutStartIdx)
          |  } yield turnInitIdx
          |""".stripMargin

      val expected =
        """def f =
          |  for {
          |    rolloutStartIdx <- findRolloutStart(dataPoints) if isValidRolloutStart(dataPoints, rolloutStartIdx)
          |    turnInitIdx     <- findTurnInitiation(dataPoints, rolloutStartIdx)
          |  } yield turnInitIdx
          |""".stripMargin

      input should formatTo(expected, withBraces(Construct.ForYield -> BracePolicy.Keep))
    }

    "move a guard that does not fit on the line of its generator to its own line" in {
      val input =
        ("""def f =
         |  for {
         |    rolloutStartIdx <- findRolloutStart(dataPoints) """ +
        """if isValidRolloutStart(dataPoints, rolloutStartIdx, minimumThreshold)
         |    turnInitIdx <- findTurnInitiation(dataPoints, rolloutStartIdx)
         |  } yield turnInitIdx
         |""").stripMargin

      val expected =
        """def f =
          |  for {
          |    rolloutStartIdx <- findRolloutStart(dataPoints)
          |                       if isValidRolloutStart(dataPoints, rolloutStartIdx, minimumThreshold)
          |    turnInitIdx     <- findTurnInitiation(dataPoints, rolloutStartIdx)
          |  } yield turnInitIdx
          |""".stripMargin

      input should formatTo(expected, withBraces(Construct.ForYield -> BracePolicy.Keep))
    }

    "go below = when it is the body of a val" in {
      val input =
        """val result = for {
          |  docs <- streams.currentDocs
          |  data <- myData
          |} yield data
          |""".stripMargin

      val expected =
        """val result =
          |  for {
          |    docs <- streams.currentDocs
          |    data <- myData
          |  } yield data
          |""".stripMargin

      input should formatTo(expected, withBraces(Construct.ForYield -> BracePolicy.Keep))
    }
    
  }

  "the cases of a match" should {

    "align the arrows of consecutive one-line cases" in {
      val input =
        """def f =
          |  term match {
          |    case block: Term.Block => blocks.render(block, indent, forceExplode)
          |    case ifTerm: Term.If => ifElses.render(ifTerm, indent, forceExplode)
          |      case interpolate: Term.Interpolate => interpolations.render(interpolate)
          |    }
          |""".stripMargin

      val expected =
        """def f =
          |  term match {
          |    case block: Term.Block             => blocks.render(block, indent, forceExplode)
          |    case ifTerm: Term.If               => ifElses.render(ifTerm, indent, forceExplode)
          |    case interpolate: Term.Interpolate => interpolations.render(interpolate)
          |  }
          |""".stripMargin

      input should formatTo(expected)
    }

    "put every body below its => when a case would not fit once aligned" in {
      val input =
        """def f =
          |  x match {
          |    case 1 => a
          |    case 22 => abcdefghijklmnopqrst
          |    case 333 => b
          |    case 4444 => c
          |  }
          |""".stripMargin

      val expected =
        """def f =
          |  x match {
          |    case 1 =>
          |      a
          |    case 22 =>
          |      abcdefghijklmnopqrst
          |    case 333 =>
          |      b
          |    case 4444 =>
          |      c
          |  }
          |""".stripMargin

      input should formatStablyTo(expected, 36)
    }

    "align the arrows of a partial function given as a named argument" in {
      val input =
        """def f =
          |  g(
          |    eligible = {
          |      case s: Defn.Val => true
          |      case _ => false
          |    },
          |  )
          |""".stripMargin

      val expected =
        """def f =
          |  g(
          |    eligible = {
          |      case s: Defn.Val => true
          |      case _           => false
          |    },
          |  )
          |""".stripMargin

      input should formatTo(expected)
    }

    "keep the braces of a val partial function on the = line" in {
      val input =
        """private val detectors: DetectingState => List[EventType] = {
          |  case DetectingState(_, FlightPhase.BeforeTakeoff, _) =>
          |    EventType.Takeoff :: Nil
          |  case _ =>
          |    Nil
          |}
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "move the braces of a val partial function written below = up to the = line" in {
      val input =
        """val f: Int => String =
          |  {
          |    case 1 => "a"
          |    case 22 => "b"
          |  }
          |""".stripMargin

      val expected =
        """val f: Int => String = {
          |  case 1  => "a"
          |  case 22 => "b"
          |}
          |""".stripMargin

      input should formatTo(expected)
    }

    "keep the braces of a def partial function on the = line" in {
      val input =
        """def handler(x: Int): PartialFunction[Int, String] = {
          |  case 1 => "a"
          |  case 22 => "b"
          |}
          |""".stripMargin

      val expected =
        """def handler(x: Int): PartialFunction[Int, String] = {
          |  case 1  => "a"
          |  case 22 => "b"
          |}
          |""".stripMargin

      input should formatTo(expected)
    }

    "keep the braces of an assigned partial function on the = line" in {
      val input =
        """def f =
          |  handler = {
          |    case 1 => "a"
          |    case 22 => "b"
          |  }
          |""".stripMargin

      val expected =
        """def f =
          |  handler = {
          |    case 1  => "a"
          |    case 22 => "b"
          |  }
          |""".stripMargin

      input should formatTo(expected)
    }

    "keep the braces of an assigned block on the = line" in {
      val input =
        """def f =
          |  x = {
          |    val a = 1
          |    a + 1
          |  }
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "not align across a multi-line case" in {
      val input =
        """def f =
          |  x match {
          |    case 1 => "a"
          |    case 22 =>
          |      "b"
          |    case 333 => "c"
          |  }
          |""".stripMargin

      val expected =
        """def f =
          |  x match {
          |    case 1   => "a"
          |    case 22  => "b"
          |    case 333 => "c"
          |  }
          |""".stripMargin

      input should formatTo(expected, withBody(_.copy(caseBody = MultiBodyPolicy.AlwaysJoin)))
    }
    
  }

  "the pairs of an exploded argument list" should {

    "align their arrows" in {
      val input =
        """val colorMap =
          |  Map(
          |    FlightPhase.Climbing -> "rgb(44, 160, 44)",
          |    FlightPhase.UnderCanopy -> "rgb(148, 103, 189)",
          |    FlightPhase.Landed -> "rgb(140, 86, 75)",
          |  )
          |""".stripMargin

      val expected =
        """val colorMap =
          |  Map(
          |    FlightPhase.Climbing    -> "rgb(44, 160, 44)",
          |    FlightPhase.UnderCanopy -> "rgb(148, 103, 189)",
          |    FlightPhase.Landed      -> "rgb(140, 86, 75)",
          |  )
          |""".stripMargin

      input should formatTo(expected)
    }

    "align them also when the list was written on one line" in {
      val input =
        """val colorMap = Map(FlightPhase.Climbing -> "rgb(44, 160, 44)", """ +
        """FlightPhase.UnderCanopy -> "rgb(148, 103, 189)", FlightPhase.Landed -> "rgb(140, 86, 75)")""" + "\n"

      val expected =
        """val colorMap =
          |  Map(
          |    FlightPhase.Climbing    -> "rgb(44, 160, 44)",
          |    FlightPhase.UnderCanopy -> "rgb(148, 103, 189)",
          |    FlightPhase.Landed      -> "rgb(140, 86, 75)",
          |  )
          |""".stripMargin

      input should formatTo(expected)
    }

    "not align across a comment line (by default) or with an element that is not a pair" in {
      val input =
        """val m =
          |  Map(
          |    a -> 1,
          |    bbb -> 2,
          |    // the rest
          |    cc -> 3,
          |    d -> 4,
          |    other,
          |    eeeee -> 5,
          |  )
          |""".stripMargin

      val expected =
        """val m =
          |  Map(
          |    a   -> 1,
          |    bbb -> 2,
          |    // the rest
          |    cc -> 3,
          |    d  -> 4,
          |    other,
          |    eeeee -> 5,
          |  )
          |""".stripMargin

      input should formatTo(expected)
    }

  }

  "a multi-line operator expression" should {

    "start every line at the column of the first operand" in {
      val input =
        """def f =
          |  Doc.text(open.filterNot(_.isWhitespace)) +
          |    renderUnwrapped(term, indent, forceExplode) +
          |    Doc.text(close.filterNot(_.isWhitespace))
          |""".stripMargin

      val expected =
        """def f =
          |  Doc.text(open.filterNot(_.isWhitespace)) +
          |  renderUnwrapped(term, indent, forceExplode) +
          |  Doc.text(close.filterNot(_.isWhitespace))
          |""".stripMargin

      input should formatTo(expected)
    }

    "do the same with leading operators" in {
      val input =
        """def f =
          |  first
          |    && second
          |    && third
          |""".stripMargin

      val expected =
        """def f =
          |  first
          |  && second
          |  && third
          |""".stripMargin

      input should formatTo(expected)
    }

  }

  "an operator expression that does not fit" should {

    val assertion =
      """Render.render(Doc.intercalate(Doc.text(", "), List(Doc.text("a"), Doc.text("b"), Doc.text("c")))) shouldBe"""

    "break after its operator" in {
      val input = s"""def f =\n  $assertion "a, b, c, d, e"\n"""

      val expected =
        s"""def f =
           |  $assertion
           |  "a, b, c, d, e"
           |""".stripMargin

      input should formatTo(expected)
    }

    "keep the break on a second run" in {
      val input = s"""def f =\n  $assertion\n  "a, b, c, d, e"\n"""

      val expected = input

      input should formatTo(expected)
    }

    "break after every operator of a run of the same operator" in {
      val input =
        "val all = withLeading.take(close).reverse.dropWhile(_.trim.isEmpty).reverse ++ separatorLines ++ withLeading.drop(close) ++ trailer\n"

      val expected =
        """val all =
          |  withLeading.take(close).reverse.dropWhile(_.trim.isEmpty).reverse ++
          |  separatorLines ++
          |  withLeading.drop(close) ++
          |  trailer
          |""".stripMargin

      input should formatTo(expected, keepInlineChains(Config.default))
    }

    "leave an inner operator of another kind on one line" in {
      val input =
        "val ok = withLeading.take(close).reverse.dropWhile(_.trim.isEmpty).nonEmpty && separator.isEmpty || fallbackValueOption.isDefined\n"

      val expected =
        """val ok =
          |  withLeading.take(close).reverse.dropWhile(_.trim.isEmpty).nonEmpty && separator.isEmpty ||
          |  fallbackValueOption.isDefined
          |""".stripMargin

      input should formatTo(expected, keepInlineChains(Config.default))
    }

  }

}
