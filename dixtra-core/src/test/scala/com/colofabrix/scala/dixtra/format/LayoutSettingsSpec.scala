package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.BlankLinePolicy.*
import com.colofabrix.scala.dixtra.DefinitionKind.*
import com.colofabrix.scala.dixtra.MultiBodyPolicy.*
import com.colofabrix.scala.dixtra.testkit.LayoutRows
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The layout settings besides braces, one at a time: the chain count (`selector-chain`), the placement of bodies
 * (`body`), the alignment (`alignment`) and the blank lines at the edges of bodies (`blank-lines`).
 */
class LayoutSettingsSpec extends AnyWordSpec with Matchers with LayoutRows {

  "selector-chain.break-at-count" should {
    val twoSelections   = "def f =\n  fa.toVector.foldLeft(b)(f)\n"
    val twoExploded     = "def f =\n  fa\n    .toVector\n    .foldLeft(b)(f)\n"
    val threeSelections = "def f =\n  fa.toVector.sorted.foldLeft(b)(f)\n"

    "break a chain below = from that many selections" in {
      val input    = twoSelections
      val expected = twoExploded
      input should formatTo(expected, chainCount(2))
    }

    "keep a shorter chain below = on one line" in {
      val input    = threeSelections
      val expected = threeSelections
      input should formatTo(expected, chainCount(4))
    }

    "not break a chain that is an operand" in {
      val input    = "def f =\n  dataPoints(idx).point.speed.vertical >= config.minSwoopVerticalSpeed\n"
      val expected = input
      input should formatTo(expected)
    }

    "not break a chain that is an argument" in {
      val input    = "def f =\n  g(xs.map(f).filter(p).sum)\n"
      val expected = input
      input should formatTo(expected)
    }

    "not break a chain that is a condition" in {
      val input    = "def f =\n  if xs.map(f).filter(p).isEmpty then a else b\n"
      val expected = input
      input should formatTo(expected)
    }
  }

  "selector-chain.namespace-path" should {
    val oneLine  = "def f =\n  fs2.Stream.emits(xs).map(f).toList\n"
    val together = "def f =\n  fs2.Stream\n    .emits(xs)\n    .map(f)\n    .toList\n"
    val apart    = "def f =\n  fs2\n    .Stream\n    .emits(xs)\n    .map(f)\n    .toList\n"

    "place the path by the policy" in {

      val table =
        List(
          (ChainHeadPolicy.Keep, oneLine, together),
          (ChainHeadPolicy.Keep, apart, apart),
          (ChainHeadPolicy.AlwaysJoin, apart, together),
          (ChainHeadPolicy.AlwaysBreak, oneLine, apart),
        )

      table.foreach { (policy, input, expected) =>
        input should formatTo(expected, chainHead(namespacePath = policy))
      }

    }
  }

  "selector-chain.object-member" should {
    val oneLine  = "def f =\n  IO.pure(x).flatMap(f).map(g).void\n"
    val together = "def f =\n  IO.pure(x)\n    .flatMap(f)\n    .map(g)\n    .void\n"
    val apart    = "def f =\n  IO\n    .pure(x)\n    .flatMap(f)\n    .map(g)\n    .void\n"

    "place the first member by the policy" in {

      val table =
        List(
          (ChainHeadPolicy.AlwaysBreak, oneLine, apart),
          (ChainHeadPolicy.Keep, oneLine, together),
          (ChainHeadPolicy.Keep, apart, apart),
          (ChainHeadPolicy.AlwaysJoin, apart, together),
        )

      table.foreach { (policy, input, expected) =>
        input should formatTo(expected, chainHead(objectMember = policy))
      }

    }

    "join the member to a namespace path" in {
      val input    = "def f =\n  fs2.Stream.emits(xs).map(f).filter(g).toList\n"
      val expected = "def f =\n  fs2.Stream.emits(xs)\n    .map(f)\n    .filter(g)\n    .toList\n"
      input should formatTo(expected, chainHead(objectMember = ChainHeadPolicy.AlwaysJoin))
    }

    "not join a member to a lowercase receiver" in {
      val input    = "def f =\n  xs.map(f).filter(g).toList\n"
      val expected = "def f =\n  xs\n    .map(f)\n    .filter(g)\n    .toList\n"
      input should formatTo(expected, chainHead(objectMember = ChainHeadPolicy.AlwaysJoin))
    }
  }

  "selector-chain.receiver-min-length" should {
    val oneLine = "def f =\n  v.trim.toLowerCase.nonEmpty\n"

    "break after every receiver when 1" in {
      val input    = oneLine
      val expected = "def f =\n  v\n    .trim\n    .toLowerCase\n    .nonEmpty\n"
      input should formatTo(expected, receiverMinLength(1))
    }

    "keep the first selection with a shorter receiver" in {
      val input    = oneLine
      val expected = "def f =\n  v.trim\n    .toLowerCase\n    .nonEmpty\n"
      input should formatStablyTo(expected, receiverMinLength(3))
    }

    "count the kept selection towards break-at-count" in {
      val input = "def f =\n  v.trim.nonEmpty\n"
      val chain =
        Config
          .default
          .selectorChain
          .copy(breakAtCount = 2, receiverMinLength = 3)

      val config = Config.default.copy(selectorChain = chain)

      val expected = "def f =\n  v.trim\n    .nonEmpty\n"

      input should formatTo(expected, config)
    }

    "break after a receiver as long as the minimum" in {
      val input    = "def f =\n  abc.trim.toLowerCase.nonEmpty\n"
      val expected = "def f =\n  abc\n    .trim\n    .toLowerCase\n    .nonEmpty\n"
      input should formatTo(expected, receiverMinLength(3))
    }

    "keep a selection the author broke away" in {
      val input    = "def f =\n  v\n    .trim\n    .toLowerCase\n    .nonEmpty\n"
      val expected = input
      input should formatTo(expected, receiverMinLength(3))
    }
  }

  "bodies after =" should {
    val below = "def f =\n  xs.map(_ + 1)\n"

    "join a fitting body written below = to the = line when always-join" in {
      val table =
        List(
          below                                -> "def f = xs.map(_ + 1)\n",
          "val y =\n  xs.map(x => x + 1)\n"    -> "val y = xs.map(x => x + 1)\n",
          "val y =\n  xs.map { x => x + 1 }\n" -> "val y = xs.map { x => x + 1 }\n",
          "val f =\n  x => x + 1\n"            -> "val f = x => x + 1\n",
        )

      val join = bodies(_.withAfterEquals(SingleBodyPolicy.AlwaysJoin), 120)

      table.foreach { (input, expected) =>
        input should formatTo(expected, join)
      }
    }

    "keep a body on or below = where the author put it when keep" in {
      val table =
        List(
          below,
          "def f = xs.map(_ + 1)\n",
          "def concat(docs: Iterable[Doc]): Doc = docs.foldLeft(Doc.Empty: Doc)(_ ++ _)\n",
          "def concat(docs: Iterable[Doc]): Doc =\n  docs.foldLeft(Doc.Empty: Doc)(_ ++ _)\n",
          "val y =\n  xs.map(x => x + 1)\n",
          "val y =\n  xs.map { x => x + 1 }\n",
          "val f =\n  x => x + 1\n",
        )

      table.foreach { input =>
        input should formatTo(input, bodies(_.withAfterEquals(SingleBodyPolicy.Keep), 120))
      }
    }

    "put every body below = when always-break" in {
      val break = bodies(_.withAfterEquals(SingleBodyPolicy.AlwaysBreak), 120)

      val table =
        List(
          ("val a = 1\n", "val a =\n  1\n"),
          ("def f = xs.map(_ + 1)\n", "def f =\n  xs.map(_ + 1)\n"),
          ("type T = Int\n", "type T =\n  Int\n"),
        )

      table.foreach { (input, expected) =>
        input should formatTo(expected, break)
      }
    }

    "not join a body that would not fit on the = line when always-join" in {
      val long =
        "def f =\n" +
        "  someVeryLongFunctionName(argumentNumberOne, argumentNumberTwo, argumentNumberThree, argumentNumberFour," +
        " five, sixth)\n"

      val expected = long

      long should formatTo(expected, bodies(_.withAfterEquals(SingleBodyPolicy.AlwaysJoin), 120))
    }

    "not join a body that explodes when always-join" in {
      val chain = "def f =\n  xs\n    .map(_ + 1)\n    .filter(_ > 0)\n    .sum\n"

      val expected = chain

      chain should formatTo(expected, bodies(_.withAfterEquals(SingleBodyPolicy.AlwaysJoin), 120))
    }
  }

  "body.lambda" should {
    val below  = "def f =\n  xs.foreach { x =>\n    println(x)\n  }\n"
    val onLine = "def f =\n  xs.foreach { x => println(x) }\n"

    val table =
      List(
        (SingleBodyPolicy.Keep, below, below),
        (SingleBodyPolicy.Keep, onLine, onLine),
        (SingleBodyPolicy.AlwaysJoin, below, onLine),
        (SingleBodyPolicy.AlwaysJoin, onLine, onLine),
        (SingleBodyPolicy.AlwaysBreak, onLine, below),
        (SingleBodyPolicy.AlwaysBreak, below, below),
      )

    "place the body of a lambda by the policy" in {
      table.foreach { (policy, input, expected) =>
        input should formatTo(expected, bodies(_.withLambda(policy), 120))
      }
    }

    "put a body that does not fit on the => line below it when always-join" in {
      val input    = onLine
      val expected = below
      input should formatTo(expected, bodies(_.withLambda(SingleBodyPolicy.AlwaysJoin), 30))
    }
  }

  "body.if" should {
    val onEquals  = "def f = if c then a else b\n"
    val oneLine   = "def f =\n  if c then a else b\n"
    val elseLine  = "def f =\n  if c then a\n  else b\n"
    val thenBelow = "def f =\n  if c then\n    a\n  else b\n"
    val exploded  = "def f =\n  if c then\n    a\n  else\n    b\n"

    val table =
      List(
        (JoinMixed, onEquals, onEquals),
        (BreakMixed, onEquals, onEquals),
        (AlwaysJoin, onEquals, onEquals),
        (AlwaysBreak, onEquals, exploded),
        (JoinMixed, oneLine, oneLine),
        (BreakMixed, oneLine, oneLine),
        (AlwaysJoin, oneLine, oneLine),
        (AlwaysBreak, oneLine, exploded),
        (JoinMixed, elseLine, elseLine),
        (BreakMixed, elseLine, elseLine),
        (AlwaysJoin, elseLine, elseLine),
        (AlwaysBreak, elseLine, exploded),
        (JoinMixed, thenBelow, elseLine),
        (BreakMixed, thenBelow, exploded),
        (AlwaysJoin, thenBelow, elseLine),
        (AlwaysBreak, thenBelow, exploded),
        (JoinMixed, exploded, exploded),
        (BreakMixed, exploded, exploded),
        (AlwaysJoin, exploded, elseLine),
        (AlwaysBreak, exploded, exploded),
      )

    "join or break a mixed if/else by the policy, and keep a uniform one with the -mixed values" in {
      table.foreach { (policy, input, expected) =>
        input should formatTo(expected, bodies(_.copy(ifBody = policy), 120))
      }
    }

    "put every branch below when one does not fit on its line" in {
      val long     = "def f =\n  if c then a\n  else someVeryLongFunctionName(b)\n"
      val expected = "def f =\n  if c then\n    a\n  else\n    someVeryLongFunctionName(b)\n"
      MultiBodyPolicy.values.foreach(policy => long should formatTo(expected, bodies(_.copy(ifBody = policy), 32)))
    }

    "put every branch below when the if/else written on one line does not fit" in {
      val long     = "def f =\n  if cond then someFunction(a) else b\n"
      val expected = "def f =\n  if cond then\n    someFunction(a)\n  else\n    b\n"
      long should formatTo(expected, bodies(_.copy(ifBody = AlwaysJoin), 30))
    }

    "put every branch below when one holds a match" in {
      val input    = "def f =\n  if c then a\n  else x match { case 1 => b }\n"
      val expected = "def f =\n  if c then\n    a\n  else\n    x match {\n      case 1 => b\n    }\n"
      MultiBodyPolicy.values.foreach(policy => input should formatTo(expected, bodies(_.copy(ifBody = policy), 120)))
    }

    "keep each else of an else-if chain on its own line" in {
      val chain    = "def f =\n  if c1 then a()\n  else if c2 then b() else c()\n"
      val expected = "def f =\n  if c1 then a()\n  else if c2 then b()\n  else c()\n"
      chain should formatTo(expected, bodies(_.copy(ifBody = AlwaysJoin), 120))
    }

    "break a whole mixed else-if chain together" in {
      val chain    = "def f =\n  if c1 then a()\n  else if c2 then\n    b()\n  else c()\n"
      val expected = "def f =\n  if c1 then\n    a()\n  else if c2 then\n    b()\n  else\n    c()\n"
      chain should formatTo(expected, bodies(_.copy(ifBody = BreakMixed), 120))
    }

    "keep an if without else as written with the -mixed values" in {
      val onLine = "def f =\n  if c then a()\n"
      val below  = "def f =\n  if c then\n    a()\n"

      List(JoinMixed, BreakMixed).foreach { policy =>
        List(onLine, below).foreach(input => input should formatTo(input, bodies(_.copy(ifBody = policy), 120)))
      }
    }

    "write the branches on their lines in the syntax of the brace style" in {
      val braces = withBraces(Construct.If -> BracePolicy.ForceBraces)
      val indent = withBraces(Construct.If -> BracePolicy.ForceIndent)

      val table =
        List(
          (oneLine, "def f =\n  if (c) a else b\n", braces),
          ("def f =\n  if (c) a else b\n", oneLine, indent),
          (thenBelow, "def f =\n  if (c) {\n    a\n  } else {\n    b\n  }\n", braces),
        )

      table.foreach { (input, expected, config) => input should formatTo(expected, config) }
    }

    "write no end marker after an if/else on one line" in {
      val input    = oneLine
      val expected = input
      input should formatTo(expected, withBraces(Construct.If -> BracePolicy.ForceEnd))
    }
  }

  "body.case-body" should {
    val mixed =
      """def f(x: Int) =
        |  x match {
        |    case 1 =>
        |      "one"
        |    case 22 => "two"
        |  }
        |""".stripMargin

    val onLines =
      """def f(x: Int) =
        |  x match {
        |    case 1  => "one"
        |    case 22 => "two"
        |  }
        |""".stripMargin

    val below =
      """def f(x: Int) =
        |  x match {
        |    case 1 =>
        |      "one"
        |    case 22 =>
        |      "two"
        |  }
        |""".stripMargin

    val table =
      List(
        (JoinMixed, mixed, onLines),
        (BreakMixed, mixed, below),
        (AlwaysJoin, mixed, onLines),
        (AlwaysBreak, mixed, below),
        (JoinMixed, onLines, onLines),
        (BreakMixed, onLines, onLines),
        (AlwaysJoin, onLines, onLines),
        (AlwaysBreak, onLines, below),
        (JoinMixed, below, below),
        (BreakMixed, below, below),
        (AlwaysJoin, below, onLines),
        (AlwaysBreak, below, below),
      )

    "join or break mixed cases by the policy, and keep uniform ones with the -mixed values" in {
      table.foreach { (policy, input, expected) =>
        input should formatTo(expected, bodies(_.copy(caseBody = policy), 120))
      }
    }

    "put every body below its => when one does not fit on its => line" in {
      val input =
        """def f(x: Int) =
          |  x match {
          |    case 1 => "one"
          |    case 2 => "a much longer case body"
          |  }
          |""".stripMargin

      val expected =
        """def f(x: Int) =
          |  x match {
          |    case 1 =>
          |      "one"
          |    case 2 =>
          |      "a much longer case body"
          |  }
          |""".stripMargin

      MultiBodyPolicy.values.foreach(policy => input should formatTo(expected, bodies(_.copy(caseBody = policy), 30)))
    }

    "put every body below its => when one has several statements" in {
      val input =
        """def f(x: Int) =
          |  x match {
          |    case 1 => "one"
          |    case 2 =>
          |      val a = "two"
          |      a
          |  }
          |""".stripMargin

      val expected =
        """def f(x: Int) =
          |  x match {
          |    case 1 =>
          |      "one"
          |    case 2 =>
          |      val a = "two"
          |      a
          |  }
          |""".stripMargin

      List(JoinMixed, AlwaysJoin).foreach { policy =>
        input should formatTo(expected, bodies(_.copy(caseBody = policy), 120))
      }
    }

    "put every body below its => when a case does not fit once its arrow is aligned" in {
      val input    = "def f(x: Int) =\n  x match {\n    case 1 => \"abcdef\"\n    case 22 => \"b\"\n  }\n"
      val expected = "def f(x: Int) =\n  x match {\n    case 1 =>\n      \"abcdef\"\n    case 22 =>\n      \"b\"\n  }\n"
      input should formatTo(expected, bodies(_.copy(caseBody = AlwaysJoin), 22))
    }
  }

  "body.try" should {
    val config =
      (policy: MultiBodyPolicy) =>
        withBraces(Construct.Try -> BracePolicy.ForceIndent).copy(body = bodies(_.copy(tryBody = policy), 120).body)

    val onEquals = "def f = try a() finally b()\n"
    val oneLine  = "def f =\n  try a() finally b()\n"
    val onLines  = "def f =\n  try a()\n  finally b()\n"
    val mixed    = "def f =\n  try\n    a()\n  finally b()\n"
    val below    = "def f =\n  try\n    a()\n  finally\n    b()\n"

    val table =
      List(
        (JoinMixed, onEquals, onEquals),
        (BreakMixed, onEquals, onEquals),
        (AlwaysJoin, onEquals, onEquals),
        (BreakMixed, oneLine, oneLine),
        (AlwaysJoin, oneLine, oneLine),
        (AlwaysBreak, oneLine, below),
        (JoinMixed, onLines, onLines),
        (BreakMixed, onLines, onLines),
        (AlwaysBreak, onLines, below),
        (JoinMixed, mixed, onLines),
        (BreakMixed, mixed, below),
        (AlwaysJoin, mixed, onLines),
        (AlwaysBreak, mixed, below),
        (JoinMixed, below, below),
        (BreakMixed, below, below),
        (AlwaysJoin, below, onLines),
      )

    "join or break mixed try and finally bodies by the policy, and keep uniform ones with the -mixed values" in {
      table.foreach { (policy, input, expected) =>
        input should formatTo(expected, config(policy))
      }
    }
  }

  "body.for, body.for-yield and body.while" should {
    val forBelow    = "def f =\n  for x <- xs do\n    println(x)\n"
    val forOnLine   = "def f =\n  for x <- xs do println(x)\n"
    val whileBelow  = "def f =\n  while c do\n    step()\n"
    val whileOnLine = "def f =\n  while c do step()\n"
    val yieldBelow  = "def f =\n  for\n    x <- xs\n    y <- ys\n  yield\n    x + y\n"
    val yieldOnLine = "def f =\n  for\n    x <- xs\n    y <- ys\n  yield x + y\n"
    val yieldSimple = "def f =\n  for\n    x <- xs\n    y <- ys\n  yield x\n"

    val table =
      List(
        (SingleBodyPolicy.Keep, forBelow, forBelow),
        (SingleBodyPolicy.Keep, forOnLine, forOnLine),
        (SingleBodyPolicy.AlwaysJoin, forBelow, forOnLine),
        (SingleBodyPolicy.AlwaysBreak, forOnLine, forBelow),
      )

    val yieldTable =
      List(
        (YieldBodyPolicy.Keep, yieldBelow, yieldBelow),
        (YieldBodyPolicy.Keep, yieldOnLine, yieldOnLine),
        (YieldBodyPolicy.AlwaysJoin, yieldBelow, yieldOnLine),
        (YieldBodyPolicy.AlwaysBreak, yieldOnLine, yieldBelow),
        (YieldBodyPolicy.BreakComplex, yieldOnLine, yieldBelow),
        (YieldBodyPolicy.BreakComplex, yieldSimple, yieldSimple),
      )

    "place the body of a for loop by body.for" in {
      table.foreach { (policy, input, expected) =>
        input should formatTo(expected, bodies(_.copy(forBody = policy), 120))
      }
    }

    "place the yield expression by body.for-yield" in {
      yieldTable.foreach { (policy, input, expected) =>
        input should formatTo(expected, bodies(_.copy(forYield = policy), 120))
      }
    }

    "place each kind of loop by its own setting" in {
      val config = bodies(_.copy(forBody = SingleBodyPolicy.AlwaysJoin, forYield = YieldBodyPolicy.AlwaysBreak), 120)
      List(forBelow -> forOnLine, yieldOnLine -> yieldBelow).foreach(
        (input, expected) =>
          input should formatTo(expected, config),
      )
    }

    "place the body of a while loop by body.while" in {
      val config =
        (policy: SingleBodyPolicy) =>
          withBraces(Construct.While -> BracePolicy.ForceIndent).copy(
            body = bodies(_.copy(whileBody = policy), 120).body,
          )

      val table =
        List(
          (SingleBodyPolicy.Keep, whileBelow, whileBelow),
          (SingleBodyPolicy.Keep, whileOnLine, whileOnLine),
          (SingleBodyPolicy.AlwaysJoin, whileBelow, whileOnLine),
          (SingleBodyPolicy.AlwaysBreak, whileOnLine, whileBelow),
        )

      table.foreach { (policy, input, expected) =>
        input should formatTo(expected, config(policy))
      }
    }

    "write a loop with its body on its line in the syntax of the brace style" in {

      val table =
        List(
          (forOnLine, "def f =\n  for (x <- xs) println(x)\n", withBraces(Construct.For -> BracePolicy.ForceBraces)),
          (whileOnLine, "def f =\n  while (c) step()\n", withBraces(Construct.While -> BracePolicy.ForceBraces)),
          ("def f =\n  while (c) step()\n", whileOnLine, withBraces(Construct.While -> BracePolicy.ForceIndent)),
        )

      table.foreach { (input, expected, config) => input should formatTo(expected, config) }

    }
  }

  "selector-chain.in-for" should {
    val broken = "def f =\n  for\n    x <- xs\n           .filter(p)\n           .map(g)\n  yield x\n"
    val joined = "def f =\n  for\n    x <- xs.filter(p).map(g)\n  yield x\n"

    "join a chain the author broke in an enumerator when never-break" in {
      val input    = broken
      val expected = joined
      input should formatTo(expected, inFor(NestedChainPolicy.NeverBreak, 120))
    }

    "keep a chain the author broke in an enumerator when keep" in {
      val input    = broken
      val expected = input
      input should formatTo(expected, inFor(NestedChainPolicy.Keep, 120))
    }

    "not break an enumerator chain at break-at-count selections" in {
      val input =
        "def f =\n  for\n    x <- xs.filter(p).map(g).take(3)\n    y  = ys.map(h).filter(q).size\n  yield x\n"
      val expected = input
      NestedChainPolicy.values.foreach(policy => input should formatTo(expected, inFor(policy, 120)))
    }

    "join a chain nested in an enumerator when never-break" in {
      val input    = "def f =\n  for\n    x <- a ++ ys\n                 .map(g)\n  yield x\n"
      val expected = "def f =\n  for\n    x <- a ++ ys.map(g)\n  yield x\n"
      input should formatTo(expected, inFor(NestedChainPolicy.NeverBreak, 120))
    }

    "break an enumerator chain that does not fit" in {
      val input    = joined
      val expected = broken
      input should formatTo(expected, inFor(NestedChainPolicy.NeverBreak, 26))
    }

    "keep an enumerator chain with a comment between its selections" in {
      val input    = "def f =\n  for\n    x <- xs\n           // keep\n           .filter(p)\n  yield x\n"
      val expected = input
      input should formatTo(expected, inFor(NestedChainPolicy.NeverBreak, 120))
    }

    "not join a chain after yield" in {
      val input    = "def f =\n  for\n    x <- xs\n  yield\n    x\n      .filter(p)\n"
      val expected = input
      input should formatTo(expected, inFor(NestedChainPolicy.NeverBreak, 120))
    }
  }

  "selector-chain.in-call" should {
    val broken = "def f =\n  g(\n    xs\n      .filter(p)\n      .map(h),\n  )\n"
    val joined = "def f =\n  g(\n    xs.filter(p).map(h),\n  )\n"

    "not break a named argument chain at break-at-count selections" in {
      val input    = "def f =\n  g(a = d.name.pos.end, b = 1)\n"
      val expected = input
      NestedChainPolicy.values.foreach(policy => input should formatTo(expected, inCall(policy)))
    }

    "not break a constructor argument chain at break-at-count selections" in {
      val input    = "val a = new A(d.name.pos.end)\n"
      val expected = input
      NestedChainPolicy.values.foreach(policy => input should formatTo(expected, inCall(policy)))
    }

    "join a chain the author broke in an argument when never-break" in {
      val input    = broken
      val expected = joined
      input should formatTo(expected, inCall(NestedChainPolicy.NeverBreak))
    }

    "keep a chain the author broke in an argument when keep" in {
      val input    = broken
      val expected = input
      input should formatTo(expected, inCall(NestedChainPolicy.Keep))
    }

    "treat the elements of a tuple and the arguments of an infix with several of them as arguments, joining the chain when it never breaks" in {
      val brokenChain = "    xs\n      .filter(p)\n      .map(h),\n"
      val table =
        List(
          "def f =\n  (\n    a,\n"            -> "  )\n",
          "def f =\n  x shouldBe (\n    a,\n" -> "  )\n",
        )

      table.foreach { (open, close) =>
        val written  = open + brokenChain + close
        val expected = open + "    xs.filter(p).map(h),\n" + close
        written should formatTo(expected, inCall(NestedChainPolicy.NeverBreak))
      }
    }

    "treat the elements of a tuple and the arguments of an infix with several of them as arguments, keeping the author's breaks when keep" in {
      val brokenChain = "    xs\n      .filter(p)\n      .map(h),\n"
      val table =
        List(
          "def f =\n  (\n    a,\n"            -> "  )\n",
          "def f =\n  x shouldBe (\n    a,\n" -> "  )\n",
        )

      table.foreach { (open, close) =>
        val written  = open + brokenChain + close
        val expected = written
        written should formatTo(expected, inCall(NestedChainPolicy.Keep))
      }
    }

    "still break a chain in the body of a lambda argument at break-at-count selections" in {
      val input    = "def f =\n  g { x =>\n    x.a.b.c\n  }\n"
      val expected = "def f =\n  g { x =>\n    x\n      .a\n      .b\n      .c\n  }\n"
      input should formatTo(expected, inCall(NestedChainPolicy.NeverBreak))
    }
  }

  "a test block" should {
    val config =
      withBraces(Construct.TestBlock -> BracePolicy.Keep).copy(
        blankLines =
          Config
            .default
            .blankLines
            .copy(bodyEdges = Config.default.blankLines.bodyEdges + (Construct.TestBlock -> Always)),
      )

    "be the block of a call labelled by a string" in {
      val table =
        List(
          "class S {\n  \"a\" in {\n    f()\n  }\n}\n" -> "class S {\n  \"a\" in {\n\n    f()\n\n  }\n}\n",
          "class S {\n  it should \"a\" in {\n    f()\n  }\n}\n" ->
          "class S {\n  it should \"a\" in {\n\n    f()\n\n  }\n}\n",
          "class S {\n  test(\"a\") {\n    f()\n  }\n}\n" -> "class S {\n  test(\"a\") {\n\n    f()\n\n  }\n}\n",
          "class S {\n  describe(s\"a $b\") {\n    f()\n  }\n}\n" ->
          "class S {\n  describe(s\"a $b\") {\n\n    f()\n\n  }\n}\n",
        )

      table.foreach { (input, expected) =>
        input should formatTo(expected, config)
      }
    }

    "not be any other block argument" in {
      val table =
        List(
          "class S {\n  make {\n    f()\n  }\n}\n",
          "class S {\n  run(a) {\n    f()\n  }\n}\n",
          "class S {\n  a in {\n    f()\n  }\n}\n",
        )

      table.foreach { input =>
        input should formatTo(input, config)
      }
    }
  }

  "braces.test-block and braces.block-argument" should {
    val braced    = "class S {\n  test(\"a\") {\n\n    f()\n    g()\n\n  }\n}\n"
    val braceless = "class S {\n  test(\"a\"):\n\n    f()\n    g()\n}\n"

    "write a test block of a call without braces when force-indent, and with braces when force-braces" in {
      val table =
        List(
          (braced, braceless, BracePolicy.ForceIndent),
          (braceless, braced, BracePolicy.ForceBraces),
          (braced, braced, BracePolicy.Keep),
          (braceless, braceless, BracePolicy.Keep),
        )

      table.foreach { (input, expected, policy) =>
        input should formatTo(expected, withBraces(Construct.TestBlock -> policy))
      }
    }

    "keep the braces of a test block after an operator" in {
      val input = "class S {\n  \"a\" should {\n    f()\n    g()\n  }\n}\n"

      val expected = input

      input should formatTo(expected, withBraces(Construct.TestBlock -> BracePolicy.ForceIndent))
    }

    "write a block argument without braces when force-indent" in {
      val input    = "class S {\n  make {\n    f()\n    g()\n  }\n}\n"
      val expected = "class S {\n  make:\n    f()\n    g()\n}\n"
      input should formatTo(expected, withBraces(Construct.BlockArgument -> BracePolicy.ForceIndent))
    }
  }

  "body.test-block and body.block-argument" should {
    val below  = "class S {\n  \"a\" in {\n    f()\n  }\n}\n"
    val onLine = "class S {\n  \"a\" in { f() }\n}\n"

    "place the statements of a test block by the policy" in {
      val table =
        List(
          (SingleBodyPolicy.Keep, below, below),
          (SingleBodyPolicy.Keep, onLine, onLine),
          (SingleBodyPolicy.AlwaysJoin, below, onLine),
          (SingleBodyPolicy.AlwaysBreak, onLine, below),
        )

      table.foreach { (policy, input, expected) =>
        input should formatTo(expected, bodies(_.copy(testBlock = policy), 120))
      }
    }

    "place the statements of a block argument by its own policy" in {
      val config = bodies(_.copy(blockArgument = SingleBodyPolicy.AlwaysJoin), 120)
      val table  = List("class S {\n  make {\n    f()\n  }\n}\n" -> "class S {\n  make { f() }\n}\n", below -> below)

      table.foreach { (input, expected) =>
        input should formatTo(expected, config)
      }
    }
  }

  "selector-chain.inline-chains" should {
    val oneLine = "val y = xs.map(_ + 1).filter(_ > 0).sum\n"

    "keep a chain on the = line on one line when keep" in {
      val input    = oneLine
      val expected = input
      input should formatTo(expected, inlineChains(InlineChainPolicy.Keep, 3))
    }

    "break a chain on the = line from break-at-count selections when break-at-count" in {
      val input    = oneLine
      val expected = "val y =\n  xs\n    .map(_ + 1)\n    .filter(_ > 0)\n    .sum\n"
      input should formatStablyTo(expected, inlineChains(InlineChainPolicy.BreakAtCount, 3))
    }

    "keep a chain on the = line with fewer selections than break-at-count when break-at-count" in {
      val input    = oneLine
      val expected = input
      input should formatTo(expected, inlineChains(InlineChainPolicy.BreakAtCount, 4))
    }
  }

  "alignment.across-comments" should {
    "align definitions, arrows and operators across a comment line when on" in {
      val table =
        List(
          "val a = 1\n// note\nval bbb = 2\n" -> "val a   = 1\n// note\nval bbb = 2\n",
          "def f(x: Int) =\n  x match {\n    case 1 => a\n    // big\n    case 100 => b\n  }\n" ->
          "def f(x: Int) =\n  x match {\n    case 1   => a\n    // big\n    case 100 => b\n  }\n",
          "val m =\n  Map(\n    a -> 1,\n    // the rest\n    bbb -> 2,\n  )\n" ->
          "val m =\n  Map(\n    a   -> 1,\n    // the rest\n    bbb -> 2,\n  )\n",
          "def f =\n  total += x\n  // reset\n  n = 0\n  g()\n" ->
          "def f =\n  total += x\n  // reset\n  n     = 0\n  g()\n",
        )

      table.foreach { (input, expected) =>
        input should formatTo(expected, acrossComments(true))
      }
    }

    "not align definitions across a comment line when off" in {
      val input    = "val a = 1\n// note\nval bbb = 2\n"
      val expected = input
      input should formatTo(expected, acrossComments(false))
    }

    "still not align across a blank line when on" in {
      val input    = "val a = 1\n// note\n\nval bbb = 2\n"
      val expected = input
      input should formatTo(expected, acrossComments(true))
    }
  }

  "alignment.operators" should {
    "align the operators of adjacent one-line assignments" in {
      val input    = "def f =\n  total += x\n  count = 0\n  longerName -= 2\n  g()\n"
      val expected = "def f =\n  total      += x\n  count      = 0\n  longerName -= 2\n  g()\n"
      input should formatTo(expected)
    }

    "not align across a blank line" in {
      val input    = "def f =\n  a = 1\n\n  longer = 2\n"
      val expected = input
      input should formatTo(expected)
    }

    "not align an operator that is not listed" in {
      val input    = "def f =\n  a = 1\n  longer &= 2\n"
      val expected = input
      input should formatTo(expected)
    }

    "align an operator added to the list" in {
      val input    = "def f =\n  a = 1\n  longer &= 2\n"
      val expected = "def f =\n  a      = 1\n  longer &= 2\n"
      input should formatTo(expected, operators("=", "&="))
    }

    "not align when the list is empty" in {
      val input    = "def f =\n  a = 1\n  longer = 2\n"
      val expected = input
      input should formatTo(expected, operators())
    }

    "align the sbt setting operators of statements" in {
      val input = "def f =\n  name := \"demo\"\n  scalacOptions += \"-preview\"\n  deps ++= Seq(a)\n"
      val expected =
        "def f =\n  name          := \"demo\"\n  scalacOptions += \"-preview\"\n  deps          ++= Seq(a)\n"
      input should formatTo(expected)
    }

    "align the operators of the elements of an exploded list" in {
      val input =
        """val common =
          |  Seq(
          |    organization := "org",
          |    scalacOptions += "-preview",
          |    Test / fork := true,
          |  )
          |""".stripMargin

      val expected =
        """val common =
          |  Seq(
          |    organization  := "org",
          |    scalacOptions += "-preview",
          |    Test / fork   := true,
          |  )
          |""".stripMargin

      input should formatTo(expected)
    }

    "align operator chains column by column" in {
      val input =
        """val deps =
          |  List(
          |    "co.fs2" %% "fs2-core" % "3.13.0",
          |    "org.typelevel" %% "cats" % "1.8.0" % Test,
          |    "org.scalatest" %% "scalatest" % "3.2.20" % Test,
          |    "org" % "java-lib" % "1.0",
          |  )
          |""".stripMargin

      val expected =
        """val deps =
          |  List(
          |    "co.fs2"        %% "fs2-core"  % "3.13.0",
          |    "org.typelevel" %% "cats"      % "1.8.0"  % Test,
          |    "org.scalatest" %% "scalatest" % "3.2.20" % Test,
          |    "org"           % "java-lib"   % "1.0",
          |  )
          |""".stripMargin

      input should formatTo(expected)
    }

    "align the operators inside the value of a statement" in {
      val input = "def f =\n  deps += \"org\" %% \"lib\" % \"1.0\"\n  deps += \"org.long\" %% \"1\" % \"2.0\"\n"

      val expected =
        "def f =\n  deps += \"org\"      %% \"lib\" % \"1.0\"\n  deps += \"org.long\" %% \"1\"   % \"2.0\"\n"

      input should formatTo(expected)
    }

    "not align an element over several lines" in {
      val input =
        """val common =
          |  Seq(
          |    name := "demo",
          |    libraryDependencies ++= List(
      a,
    ),
  )
          |""".stripMargin

      val expected = input

      input should formatTo(expected)
    }

    "not align an assignment with a definition" in {
      val input    = "def f =\n  val a = 1\n  longer = 2\n  b = 3\n"
      val expected = "def f =\n  val a = 1\n  longer = 2\n  b      = 3\n"
      input should formatTo(expected)
    }

    "not align a multi-line assignment" in {
      val input    = "def f =\n  a = 1\n  longer =\n    xs\n      .map(f)\n      .filter(g)\n      .sum\n"
      val expected = input
      input should formatTo(expected)
    }
  }

  "alignment.named-arguments" should {
    val plain =
      """val x =
        |  MyCaseClass(
        |    aaa = "",
        |    name = 123,
        |  )
        |""".stripMargin

    val aligned =
      """val x =
        |  MyCaseClass(
        |    aaa = "",
        |    name = 123,
        |  )
        |""".stripMargin

    "align the = of one-line named arguments when on" in {
      List(plain, aligned).foreach(_ should formatTo(aligned, namedArguments(true)))
    }

    "not align them when off" in {
      List(plain, aligned).foreach(_ should formatTo(plain, namedArguments(false)))
    }

    "not align across a named argument over several lines" in {
      val input =
        """val x =
          |  f(
          |    a = 1,
          |    nested = g(
          |      b = 2,
          |    ),
          |    longer = 3,
          |  )
          |""".stripMargin

      val expected = input

      input should formatTo(expected, namedArguments(true))
    }
  }

  "alignment.trailing-comments" should {
    val plain   = "def f =\n  a(1) // one\n  bbb(2) // two\n"
    val aligned = "def f =\n  a(1)   // one\n  bbb(2) // two\n"

    "align the comments at the end of consecutive lines when on" in {
      List(plain, aligned).foreach(_ should formatTo(aligned, trailingComments(true, 120)))
    }

    "not align them when off" in {
      List(plain, aligned).foreach(_ should formatTo(plain, trailingComments(false, 120)))
    }

    "not align across a blank line or a line without a comment" in {
      val input    = "def f =\n  a() // one\n  bb()\n  ccc() // three\n\n  dddd() // four\n  e() // five\n"
      val expected = "def f =\n  a() // one\n  bb()\n  ccc() // three\n\n  dddd() // four\n  e()    // five\n"
      input should formatTo(expected, trailingComments(true, 120))
    }

    "align the comments of the elements of a list" in {
      val input    = "val y =\n  List(\n    1, // one\n    22, // twenty-two\n  )\n"
      val expected = "val y =\n  List(\n    1,  // one\n    22, // twenty-two\n  )\n"
      input should formatTo(expected, trailingComments(true, 120))
    }

    "leave as written a line that would not fit once aligned, the others aligning around it" in {
      val input    = "def f =\n  a() // one\n  bbbbbbbb() // two\n  c() // a longer comment\n  d() // four\n"
      val expected = "def f =\n  a()        // one\n  bbbbbbbb() // two\n  c() // a longer comment\n  d() // four\n"
      input should formatStablyTo(expected, trailingComments(true, 30))
    }
  }

  "trailing-commas" should {
    // Each list laid out one element per line, with and without the comma after its last element
    // format: off
    val lists =
      List(
        "def f =\n  compute(\n    firstArgument,\n    secondArgument,\n  )\n" ->
        "def f =\n  compute(\n    firstArgument,\n    secondArgument\n  )\n",
        "def f(\n  first: Int,\n  second: Int,\n): Int = 1\n" ->
        "def f(\n  first: Int,\n  second: Int\n): Int = 1\n",
        "val t =\n  (\n    first,\n    second,\n  )\n" ->
        "val t =\n  (\n    first,\n    second\n  )\n",
        "case class A(\n  first: Int,\n  second: Int,\n)\n" ->
        "case class A(\n  first: Int,\n  second: Int\n)\n",
        "def f =\n  compute(\n    firstArgument,\n    secondArgument, // note\n  )\n" ->
        "def f =\n  compute(\n    firstArgument,\n    secondArgument // note\n  )\n",
        "val xs =\n  List(\n    firstElement,\n    secondElement,\n  )\n" ->
        "val xs =\n  List(\n    firstElement,\n    secondElement\n  )\n",
        "def f =\n  run(\n    firstArgument,\n    x => x.compute,\n  )\n" ->
        "def f =\n  run(\n    firstArgument,\n    x => x.compute\n  )\n",
        "def f =\n  run(first)(\n    firstArgument,\n    secondArgument,\n  )\n" ->
        "def f =\n  run(first)(\n    firstArgument,\n    secondArgument\n  )\n",
        "class A(\n  first: Int,\n  second: Int,\n) extends TraitA\n" ->
        "class A(\n  first: Int,\n  second: Int\n) extends TraitA\n",
        "class A[\n  FirstType,\n  SecondType,\n](first: Int,\n)\n" ->
        "class A[\n  FirstType,\n  SecondType\n](first: Int\n)\n",
        "def f[\n  F[\n    FirstType,\n    SecondType,\n    ThirdType,\n  ],\n](x: Int,\n): Int = 1\n" ->
        "def f[\n  F[\n    FirstType,\n    SecondType,\n    ThirdType\n  ]\n](x: Int\n): Int = 1\n",
      )
    // format: on

    def commas(policy: TrailingCommas): Config =
      Config.withMaxColumn(30).copy(trailingCommas = policy)

    "add a comma after the last element with always" in {
      lists.foreach((always, never) => never should formatTo(always, commas(TrailingCommas.Always)))
    }

    "remove the comma after the last element with never" in {
      lists.foreach((always, never) => always should formatTo(never, commas(TrailingCommas.Never)))
    }

    "never add one to a list on one line" in {
      val input    = "def f = compute(first, second)\n"
      val expected = input
      input should formatTo(expected, commas(TrailingCommas.Always))
    }
  }

  "trailing-comment-overflow" should {
    val long = "def f =\n  g(1) // a comment that goes past the maximum width\n"

    "leave a line longer than the maximum width with allow" in {
      val input    = long
      val expected = input
      input should formatTo(expected, trailingOverflow(TrailingOverflow.Keep, 30))
    }

    "never break the code because of a trailing comment" in {
      val input    = "val x = foo(aaaa, bbbb) // a comment\n"
      val expected = input
      input should formatTo(expected, trailingOverflow(TrailingOverflow.Keep, 28))
    }

    "move the comment above its line with move-above, wrapped to the width" in {
      val input    = long
      val expected = "def f =\n  // a comment that goes past\n  // the maximum width\n  g(1)\n"
      input should formatStablyTo(expected, trailingOverflow(TrailingOverflow.MoveAbove, 30))
    }

    "keep a moved comment apart from a comment line above it" in {
      val input    = "def f =\n  // Note\n  g(1) // a long trailing comment\n"
      val expected = "def f =\n  // Note\n  //\n  // a long trailing comment\n  g(1)\n"
      input should formatStablyTo(expected, trailingOverflow(TrailingOverflow.MoveAbove, 30))
    }

    "leave a comment that fits at the end of its line with move-above" in {
      val input    = "def f =\n  g(1) // short\n"
      val expected = input
      input should formatTo(expected, trailingOverflow(TrailingOverflow.MoveAbove, 30))
    }

    "leave a comment after the end of a multi-line string" in {
      val input    = "val s =\n  \"\"\"a\nb\"\"\" // a comment that goes past the width\n"
      val expected = input
      input should formatTo(expected, trailingOverflow(TrailingOverflow.MoveAbove, 30))
    }
  }

  "alignment.extends-clauses" should {
    val enumInput =
      "enum Construct(val key: String) {\n  case Class extends Construct(\"class\")\n" +
      "  case AbstractClass extends Construct(\"abstract-class\")\n" +
      "  case CaseClass extends Construct(\"case-class\")\n}\n"

    val enumAligned =
      "enum Construct(val key: String) {\n  case Class         extends Construct(\"class\")\n" +
      "  case AbstractClass extends Construct(\"abstract-class\")\n" +
      "  case CaseClass     extends Construct(\"case-class\")\n}\n"

    "align the extends of consecutive enum cases" in {
      val input    = enumInput
      val expected = enumAligned
      input should formatTo(expected, extendsClauses(true))
    }

    "not keep the author's alignment when off" in {
      val input    = enumAligned
      val expected = enumInput
      input should formatTo(expected, extendsClauses(false))
    }

    "align the extends of consecutive one-line classes and objects" in {
      val input = "object A {\n  case class Small(x: Int) extends Base\n  case object Larger extends Base\n}\n"

      val expected =
        "object A {\n  case class Small(x: Int) extends Base\n  case object Larger       extends Base\n}\n"

      input should formatTo(expected, extendsClauses(true))
    }

    "not align across a blank line" in {
      val input    = "enum E {\n  case A extends E\n\n  case Longer extends E\n}\n"
      val expected = input
      input should formatTo(expected, extendsClauses(true))
    }

    "not align a definition with a body" in {
      val input =
        "object A {\n  case object B extends Base\n  class Longer extends Base {\n    def x = 1\n  }\n}\n"
      val expected = input
      input should formatTo(expected, extendsClauses(true))
    }
  }

  "alignment.assignment-groups" should {
    val input = "object A {\n  val a = 1\n  val bb = 2\n  def ccc = 3\n  def d = 4\n}\n"

    "align only the definitions of the same group" in {
      val expected = "object A {\n  val a  = 1\n  val bb = 2\n  def ccc = 3\n  def d   = 4\n}\n"
      input should formatTo(expected, groups(Set(Val), Set(Def)))
    }

    // A `def` in no group is a method like any other: separated by a blank line
    "not align a keyword that is in no group" in {
      val expected = "object A {\n  val a  = 1\n  val bb = 2\n\n  def ccc = 3\n\n  def d = 4\n}\n"
      input should formatTo(expected, groups(Set(Val)))
    }

    "align type definitions when grouped" in {
      val source   = "object A {\n  type T = Int\n  type LongName = String\n}\n"
      val expected = "object A {\n  type T        = Int\n  type LongName = String\n}\n"
      source should formatTo(expected, groups(Set(TypeDef)))
    }

    "align given aliases when grouped" in {
      val source   = "object A {\n  given a: Int = 1\n  given ord: Ordering[Int] = o\n}\n"
      val expected = "object A {\n  given a: Int             = 1\n  given ord: Ordering[Int] = o\n}\n"
      source should formatTo(expected, groups(Set(GivenAlias)))
    }

    "not align type definitions when ungrouped" in {
      val source   = "object A {\n  type T = Int\n  type LongName = String\n}\n"
      val expected = source
      source should formatTo(expected, groups(Set(Val)))
    }
  }

  "blank-lines.before-scaladoc" should {
    val config =
      (policy: ScaladocSpacing) =>
        withBlankLines(_.copy(beforeScaladoc = policy))
    val glued     = "object A {\n  def a = 1\n  /** Doc */\n  def b = 2\n}\n"
    val separated = "object A {\n  def a = 1\n\n  /** Doc */\n  def b = 2\n}\n"
    val opening   = "class Repository {\n  /** Description here */\n  def read(): Int = 1\n}\n"
    val edges     = "class Repository {\n\n  /** Description here */\n  def read(): Int = 1\n\n}\n"

    "put a blank line before a scaladoc after a line of code with always" in {
      val input    = glued
      val expected = separated
      input should formatTo(expected, config(ScaladocSpacing.Always))
    }

    "count a scaladoc at the start of a body as a blank edge with always" in {
      val input    = opening
      val expected = edges
      input should formatTo(expected, config(ScaladocSpacing.Always))
    }

    "put a blank line only before a multi-line scaladoc with always-multiline" in {
      val multiline          = "object A {\n  def a = 1\n  /**\n   * Doc\n   */\n  def b = 2\n}\n"
      val multilineSeparated = "object A {\n  def a = 1\n\n  /**\n   * Doc\n   */\n  def b = 2\n}\n"
      val table =
        List(
          glued     -> glued,
          separated -> separated,
          opening   -> opening,
          multiline -> multilineSeparated,
        )

      table.foreach { (input, expected) =>
        input should formatTo(expected, config(ScaladocSpacing.AlwaysMultiline))
      }
    }

    "keep the blank lines as written with keep" in {
      List(glued, separated, opening).foreach(input => input should formatTo(input, config(ScaladocSpacing.Keep)))
    }

    "remove the blank line before a scaladoc with never" in {
      List(glued, separated).foreach(_ should formatTo(glued, config(ScaladocSpacing.Never)))
    }

    "leave the start of a body to body-edges with never" in {
      val input    = edges
      val expected = input
      input should formatTo(expected, config(ScaladocSpacing.Never))
    }
  }

  "blank-lines.between-alignment-groups" should {
    val adjacent  = "object A {\n  val a = 1\n  def b = 2\n}\n"
    val separated = "object A {\n  val a = 1\n\n  def b = 2\n}\n"

    "separate the definitions of different groups by the policy, never those of the same group" in {
      val table =
        List(
          (blankLines(BlankLinePolicy.Keep, Set(Val), Set(Def)), adjacent, adjacent),
          (blankLines(BlankLinePolicy.Keep, Set(Val), Set(Def)), separated, separated),
          (blankLines(BlankLinePolicy.Always, Set(Val), Set(Def)), adjacent, separated),
          (blankLines(BlankLinePolicy.Never, Set(Val), Set(Def)), separated, adjacent),
          (blankLines(BlankLinePolicy.Always, Set(Val, Def)), adjacent, adjacent),
        )

      table.foreach { (config, input, expected) =>
        input should formatTo(expected, config)
      }
    }
  }

  "the arrow alignments" should {

    "not align the arrows a switch turns off" in {
      val table =
        List(
          (
            withArrows(_.copy(cases = false)),
            "def f(x: Int) =\n  x match {\n    case 1 => a\n    case 100 => b\n  }\n",
          ),
          (withArrows(_.copy(pairs = false)), "val m =\n  Map(\n    1 -> a,\n    100 -> b,\n  )\n"),
          (withArrows(_.copy(forEnumerators = false)), "def f =\n  for\n    a <- xs\n    bbb <- ys\n  yield a\n"),
        )

      table.foreach { (config, input) =>
        input should formatTo(input, config)
      }
    }
    
  }

  "the leading and closing blank lines" should {
    layoutRows(
      edgeRows.map { case (construct, policy, input, expected) =>
        (s"${construct.key} as ${policy.toString.toLowerCase}", edges(construct, policy), input, expected)
      },
    )
  }

  private def chainCount(count: Int): Config =
    withSelectorChain(_.copy(breakAtCount = count))

  private def groups(gs: Set[DefinitionKind]*): Config =
    withAlignment(_.copy(assignmentGroups = gs.toList))

  private def blankLines(policy: BlankLinePolicy, gs: Set[DefinitionKind]*): Config =
    withBlankLines(_.copy(betweenAlignmentGroups = policy), groups(gs*))

  private def operators(operators: String*): Config =
    withAlignment(_.copy(operators = operators.toList))

  private def extendsClauses(on: Boolean): Config =
    withAlignment(_.copy(extendsClauses = on))

  private def namedArguments(on: Boolean): Config =
    withAlignment(_.copy(namedArguments = on))

  private def trailingComments(on: Boolean, maxColumn: Int): Config =
    withAlignment(_.copy(trailingComments = on), Config.withMaxColumn(maxColumn))

  private def trailingOverflow(policy: TrailingOverflow, maxColumn: Int): Config =
    Config.withMaxColumn(maxColumn).copy(trailingCommentOverflow = policy)

  private def chainHead(
    namespacePath: ChainHeadPolicy = ChainHeadPolicy.Keep,
    objectMember: ChainHeadPolicy = ChainHeadPolicy.AlwaysBreak,
  ): Config =
    withSelectorChain(_.copy(namespacePath = namespacePath, objectMember = objectMember))

  private def receiverMinLength(n: Int): Config =
    withSelectorChain(_.copy(receiverMinLength = n))

  private def acrossComments(on: Boolean): Config =
    withAlignment(_.copy(acrossComments = on))

  // The configuration at `maxColumn` with its `body` settings changed by `change`, starting from `keep` for every body
  // after `=` so the body of the `def` around an example stays where it is written.
  private def bodies(change: BodyConfig => BodyConfig, maxColumn: Int): Config =
    withBody(body => change(body.withAfterEquals(SingleBodyPolicy.Keep)), Config.withMaxColumn(maxColumn))

  private def inlineChains(policy: InlineChainPolicy, count: Int): Config =
    withSelectorChain(_.copy(inlineChains = policy, breakAtCount = count))

  private def inCall(policy: NestedChainPolicy): Config =
    withSelectorChain(_.copy(inCall = policy))

  private def inFor(policy: NestedChainPolicy, maxColumn: Int): Config =
    withSelectorChain(_.copy(inFor = policy), Config.withMaxColumn(maxColumn))

  // The edge blank lines of `construct` set to `policy`, its braces kept as written.
  private def edges(construct: Construct, policy: BlankLinePolicy): Config =
    withBlankLines(
      b => b.copy(bodyEdges = b.bodyEdges + (construct -> policy)),
      withBraces(construct -> BracePolicy.Keep),
    )

  // Each row sets the edge blank lines of one construct and formats an input.
  private lazy val edgeRows: List[(Construct, BlankLinePolicy, String, String)] =
    List(
      (
        Construct.Class,
        Keep,
        "class A {\n  def f = 1\n}\n",
        "class A {\n  def f = 1\n}\n",
      ),
      (
        Construct.Class,
        Keep,
        "class A {\n\n  def f = 1\n}\n",
        "class A {\n\n  def f = 1\n\n}\n",
      ),
      (
        Construct.Class,
        Keep,
        "class A {\n  def f = 1\n\n}\n",
        "class A {\n\n  def f = 1\n\n}\n",
      ),
      (
        Construct.Class,
        Always,
        "class A {\n  def f = 1\n}\n",
        "class A {\n\n  def f = 1\n\n}\n",
      ),
      (
        Construct.Class,
        Never,
        "class A {\n\n  def f = 1\n\n}\n",
        "class A {\n  def f = 1\n}\n",
      ),
      (
        Construct.Trait,
        Keep,
        "trait A {\n  self: B =>\n\n  def f = 1\n}\n",
        "trait A {\n  self: B =>\n\n  def f = 1\n\n}\n",
      ),
      (
        Construct.Trait,
        Keep,
        "trait A {\n  self: B =>\n  def f = 1\n\n}\n",
        "trait A {\n  self: B =>\n\n  def f = 1\n\n}\n",
      ),
      (
        Construct.Object,
        Always,
        "object A:\n  def f = 1\n",
        "object A:\n\n  def f = 1\n",
      ),
      (
        Construct.Object,
        Never,
        "object A:\n\n  def f = 1\n",
        "object A:\n  def f = 1\n",
      ),
      (
        Construct.Package,
        Never,
        "package a\n\nobject B\n",
        "package a\nobject B\n",
      ),
      (
        Construct.Package,
        Keep,
        "package a\nobject B\n",
        "package a\nobject B\n",
      ),
      (
        Construct.Extension,
        Always,
        "extension (x: Int) {\n  def f = 1\n  def g = 2\n}\n",
        "extension (x: Int) {\n\n  def f = 1\n  def g = 2\n\n}\n",
      ),
      (
        Construct.LambdaArgument,
        Keep,
        "def f =\n  xs.foreach { x =>\n\n    g(x)\n    h(x)\n  }\n",
        "def f =\n  xs.foreach { x =>\n\n    g(x)\n    h(x)\n\n  }\n",
      ),
      (
        Construct.LambdaArgument,
        Always,
        "def f =\n  xs.foreach { x =>\n    g(x)\n    h(x)\n  }\n",
        "def f =\n  xs.foreach { x =>\n\n    g(x)\n    h(x)\n\n  }\n",
      ),
      (
        Construct.TestBlock,
        Keep,
        "class S {\n  \"a\" should {\n\n    f()\n    g()\n  }\n}\n",
        "class S {\n  \"a\" should {\n\n    f()\n    g()\n\n  }\n}\n",
      ),
      (
        Construct.TestBlock,
        Keep,
        "class S {\n  \"a\" should {\n    f()\n    g()\n  }\n}\n",
        "class S {\n  \"a\" should {\n    f()\n    g()\n  }\n}\n",
      ),
      (
        Construct.TestBlock,
        Keep,
        "class S {\n  \"a\" should {\n\n    xs.foreach { x =>\n\n      g(x)\n      h(x)\n    }\n    make {\n\n" +
        "        f()\n        g()\n      }\n\n  }\n}\n",
        "class S {\n  \"a\" should {\n\n    xs.foreach { x =>\n      g(x)\n      h(x)\n    }\n    make {\n" +
        "      f()\n      g()\n    }\n\n  }\n}\n",
      ),
      (
        Construct.TestBlock,
        Never,
        "class S {\n  \"a\" should {\n\n    f()\n    g()\n\n  }\n}\n",
        "class S {\n  \"a\" should {\n    f()\n    g()\n  }\n}\n",
      ),
      (
        Construct.TestBlock,
        Always,
        "class S {\n  test(\"a\") {\n    f()\n    g()\n  }\n}\n",
        "class S {\n  test(\"a\") {\n\n    f()\n    g()\n\n  }\n}\n",
      ),
      (
        Construct.BlockArgument,
        Keep,
        "class S {\n  make {\n\n    f()\n     g()\n  }\n}\n",
        "class S {\n  make {\n\n    f()\n    g()\n\n  }\n}\n",
      ),
      (
        Construct.BlockArgument,
        Never,
        "class S {\n  make {\n\n    f()\n     g()\n\n  }\n}\n",
        "class S {\n  make {\n    f()\n    g()\n  }\n}\n",
      ),
      (
        Construct.Match,
        Always,
        "def f(x: Int) =\n  x match {\n    case 1 => a\n    case _ => b\n  }\n",
        "def f(x: Int) =\n  x match {\n\n    case 1 => a\n    case _ => b\n\n  }\n",
      ),
      (
        Construct.Match,
        Always,
        "def f(x: Int) =\n  x match\n    case 1 => a\n    case _ => b\n",
        "def f(x: Int) =\n  x match\n    case 1 => a\n    case _ => b\n",
      ),
      (
        Construct.NewAnonymous,
        Keep,
        "val a =\n  new A {\n\n    def f = 1\n  }\n",
        "val a =\n  new A {\n\n    def f = 1\n\n  }\n",
      ),
      (
        Construct.If,
        Always,
        "def f =\n  if (c) {\n    g()\n    h()\n  } else {\n    k()\n  }\n",
        "def f =\n  if (c) {\n\n    g()\n    h()\n\n  } else {\n\n    k()\n\n  }\n",
      ),
      (
        Construct.While,
        Keep,
        "def f =\n  while (c) {\n\n    g()\n    h()\n  }\n",
        "def f =\n  while (c) {\n\n    g()\n    h()\n\n  }\n",
      ),
      (
        Construct.Try,
        Always,
        "def f =\n  try {\n    g()\n  } catch {\n    case e => h()\n  }\n",
        "def f =\n  try {\n\n    g()\n\n  } catch {\n\n    case e => h()\n\n  }\n",
      ),
    )

}
