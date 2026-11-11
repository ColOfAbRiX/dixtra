package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.model.*
import com.colofabrix.scala.dixtra.model.BracePolicy.*
import com.colofabrix.scala.dixtra.testkit.LayoutRows
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * The brace style of each construct (`braces` in the configuration), one setting at a time: each row sets one construct
 * to one style and formats an input written in another style. A new construct or style costs rows, not specs.
 */
class BraceStyleSpec extends AnyWordSpec with Matchers with LayoutRows {

  private val withBlock = "def f = {\n  val a = 1\n  a + 1\n}\n"
  private val noBlock   = "def f =\n  val a = 1\n  a + 1\n"

  // (construct, style, input, expected)
  private val rows: List[(Construct, BracePolicy, String, String)] =
    List(
      // Containers
      (
        Construct.Class,
        ForceBraces,
        "class A:\n  val x = 1\n",
        "class A {\n  val x = 1\n}\n",
      ),
      (
        Construct.Class,
        ForceIndent,
        "class A {\n  val x = 1\n}\n",
        "class A:\n  val x = 1\n",
      ),
      (
        Construct.Class,
        ForceEnd,
        "class A {\n  val x = 1\n}\n",
        "class A:\n  val x = 1\nend A\n",
      ),
      (
        Construct.Class,
        Keep,
        "class A:\n  val x = 1\n",
        "class A:\n  val x = 1\n",
      ),
      (
        Construct.Class,
        Keep,
        "class A:\n  val x = 1\nend A\n",
        "class A:\n  val x = 1\nend A\n",
      ),
      (
        Construct.Class,
        ForceBraces,
        "class A:\n  val x = 1\nend A\n",
        "class A {\n  val x = 1\n}\n",
      ),
      (
        Construct.AbstractClass,
        ForceIndent,
        "abstract class A {\n  val x = 1\n}\n",
        "abstract class A:\n  val x = 1\n",
      ),
      (
        Construct.CaseClass,
        ForceBraces,
        "case class A(x: Int)\n",
        "case class A(x: Int)\n",
      ),
      (
        Construct.CaseClass,
        ForceBraces,
        "case class A(x: Int):\n  val y = x\n",
        "case class A(x: Int) {\n  val y = x\n}\n",
      ),
      (
        Construct.Object,
        ForceBraces,
        "object A:\n  val x = 1\n",
        "object A {\n  val x = 1\n}\n",
      ),
      (
        Construct.Object,
        ForceBraces,
        "object A\n",
        "object A\n",
      ),
      (
        Construct.Object,
        ForceIndent,
        "object A {\n  val x = 1\n}\n",
        "object A:\n  val x = 1\n",
      ),
      (
        Construct.Trait,
        ForceBraces,
        "trait A:\n  def x: Int\n",
        "trait A {\n  def x: Int\n}\n",
      ),
      (
        Construct.Enum,
        ForceBraces,
        "enum Color:\n  case Red, Green\n",
        "enum Color {\n  case Red, Green\n}\n",
      ),
      (
        Construct.Enum,
        ForceIndent,
        "enum Color {\n  case Red, Green\n}\n",
        "enum Color:\n  case Red, Green\n",
      ),
      (
        Construct.GivenInstance,
        ForceBraces,
        "given Ordering[Int] with\n  def compare(a: Int, b: Int) = a - b\n",
        "given Ordering[Int] with {\n  def compare(a: Int, b: Int) = a - b\n}\n",
      ),
      (
        Construct.GivenInstance,
        ForceIndent,
        "given Ordering[Int] with {\n  def compare(a: Int, b: Int) = a - b\n}\n",
        "given Ordering[Int] with\n  def compare(a: Int, b: Int) = a - b\n",
      ),
      (
        Construct.Extension,
        ForceBraces,
        "extension (x: Int)\n  def a = x\n  def b = x\n",
        "extension (x: Int) {\n  def a = x\n  def b = x\n}\n",
      ),
      (
        Construct.Extension,
        ForceIndent,
        "extension (x: Int) {\n  def a = x\n  def b = x\n}\n",
        "extension (x: Int)\n  def a = x\n  def b = x\n",
      ),
      (
        Construct.Package,
        ForceIndent,
        "package a {\n  object B\n}\n",
        "package a\n\nobject B\n",
      ),
      (
        Construct.Package,
        ForceBraces,
        "package a\n\nobject B\n",
        "package a {\n\n  object B\n\n}\n",
      ),
      (
        Construct.Package,
        ForceBraces,
        "package a\n\n/**\n * Doc\n */\nobject B\n",
        "package a {\n\n  /**\n   * Doc\n   */\n  object B\n\n}\n",
      ),
      (
        Construct.Package,
        ForceIndent,
        "package a {\n\n  /**\n   * Doc\n   */\n  object B\n}\n",
        "package a\n\n/**\n * Doc\n */\nobject B\n",
      ),
      (
        Construct.Package,
        ForceBraces,
        "package a\n\nobject B {\n  val s =\n    \"\"\"x\ny\"\"\"\n}\n",
        "package a {\n\n  object B {\n    val s =\n      \"\"\"x\ny\"\"\"\n  }\n\n}\n",
      ),
      // Terms
      (
        Construct.NewAnonymous,
        ForceIndent,
        "val r =\n  new Runnable {\n    def run() = println(1)\n  }\n",
        "val r =\n  new Runnable:\n    def run() = println(1)\n",
      ),
      (
        Construct.NewAnonymous,
        ForceBraces,
        "val r =\n  new Runnable:\n    def run() = println(1)\n",
        "val r =\n  new Runnable {\n    def run() = println(1)\n  }\n",
      ),
      (
        Construct.Try,
        ForceIndent,
        "def f =\n  try {\n    a()\n  } catch {\n    case e: Exception => b()\n  } finally {\n    c()\n  }\n",
        "def f =\n  try\n    a()\n  catch\n    case e: Exception => b()\n  finally\n    c()\n",
      ),
      (
        Construct.Try,
        ForceBraces,
        "def f =\n  try\n    a()\n  catch\n    case e: Exception => b()\n  finally\n    c()\n",
        "def f =\n  try {\n    a()\n  } catch {\n    case e: Exception => b()\n  } finally {\n    c()\n  }\n",
      ),
      (
        Construct.Match,
        ForceBraces,
        "def f(x: Int) =\n  x match\n    case 1 => a\n    case _ => b\n",
        "def f(x: Int) =\n  x match {\n    case 1 => a\n    case _ => b\n  }\n",
      ),
      (
        Construct.Match,
        ForceIndent,
        "def f(x: Int) =\n  x match {\n    case 1 => a\n    case _ => b\n  }\n",
        "def f(x: Int) =\n  x match\n    case 1 => a\n    case _ => b\n",
      ),
      (
        Construct.While,
        ForceBraces,
        "def f =\n  while i < 10 do\n    i += 1\n",
        "def f =\n  while (i < 10) {\n    i += 1\n  }\n",
      ),
      (
        Construct.While,
        ForceIndent,
        "def f =\n  while (i < 10) {\n    i += 1\n  }\n",
        "def f =\n  while i < 10 do\n    i += 1\n",
      ),
      (
        Construct.While,
        Keep,
        "def f =\n  while (i < 10) i += 1\n",
        "def f =\n  while (i < 10) i += 1\n",
      ),
      (
        Construct.If,
        ForceBraces,
        "def f =\n  if a then\n    b\n  else\n    c\n",
        "def f =\n  if (a) {\n    b\n  } else {\n    c\n  }\n",
      ),
      (
        Construct.If,
        ForceIndent,
        "def f =\n  if (a) {\n    b\n  } else {\n    c\n  }\n",
        "def f =\n  if a then\n    b\n  else\n    c\n",
      ),
      (
        Construct.If,
        ForceBraces,
        "def f = if (a) b else c\n",
        "def f = if (a) b else c\n",
      ),
      (
        Construct.For,
        ForceIndent,
        "def f =\n  for {\n    a <- xs\n    b <- ys\n  } {\n    println(a)\n  }\n",
        "def f =\n  for\n    a <- xs\n    b <- ys\n  do\n    println(a)\n",
      ),
      (
        Construct.For,
        ForceBraces,
        "def f =\n  for\n    a <- xs\n    b <- ys\n  do\n    println(a)\n",
        "def f =\n  for {\n    a <- xs\n    b <- ys\n  } {\n    println(a)\n  }\n",
      ),
      (
        Construct.For,
        ForceIndent,
        "def f =\n  for (x <- xs) {\n    println(x)\n  }\n",
        "def f =\n  for x <- xs do\n    println(x)\n",
      ),
      (
        Construct.ForYield,
        ForceIndent,
        "def f =\n  for {\n    a <- xs\n    b <- ys\n  } yield a + b\n",
        "def f =\n  for\n    a <- xs\n    b <- ys\n  yield\n    a + b\n",
      ),
      (
        Construct.ForYield,
        ForceBraces,
        "def f =\n  for\n    a <- xs\n    b <- ys\n  yield\n    a + b\n",
        "def f =\n  for {\n    a <- xs\n    b <- ys\n  } yield {\n    a + b\n  }\n",
      ),
      (
        Construct.ForYield,
        ForceBraces,
        "def f =\n  for\n    a <- xs\n    b <- ys\n  yield a\n",
        "def f =\n  for {\n    a <- xs\n    b <- ys\n  } yield a\n",
      ),
      // Bodies
      (
        Construct.Def,
        ForceBraces,
        noBlock,
        withBlock,
      ),
      (
        Construct.Def,
        ForceIndent,
        withBlock,
        noBlock,
      ),
      (
        Construct.Def,
        Keep,
        withBlock,
        withBlock,
      ),
      (
        Construct.Def,
        ForceEnd,
        withBlock,
        noBlock + "end f\n",
      ),
      (
        Construct.Def,
        ForceBraces,
        "def f = 1\n",
        "def f = 1\n",
      ),
      (
        Construct.Val,
        ForceBraces,
        "val f =\n  val a = 1\n  a + 1\n",
        "val f = {\n  val a = 1\n  a + 1\n}\n",
      ),
      (
        Construct.Val,
        ForceEnd,
        "val f = {\n  val a = 1\n  a + 1\n}\n",
        "val f =\n  val a = 1\n  a + 1\nend f\n",
      ),
      (
        Construct.LazyVal,
        ForceBraces,
        "lazy val f =\n  val a = 1\n  a + 1\n",
        "lazy val f = {\n  val a = 1\n  a + 1\n}\n",
      ),
      (
        Construct.Var,
        ForceBraces,
        "var f =\n  val a = 1\n  a + 1\n",
        "var f = {\n  val a = 1\n  a + 1\n}\n",
      ),
      (
        Construct.GivenAlias,
        ForceBraces,
        "given f: Int =\n  val a = 1\n  a + 1\n",
        "given f: Int = {\n  val a = 1\n  a + 1\n}\n",
      ),
      (
        Construct.PackageObject,
        ForceIndent,
        "package object a {\n  def x = 1\n}\n",
        "package object a:\n  def x = 1\n",
      ),
      (
        Construct.PackageObject,
        ForceBraces,
        "package object a:\n  def x = 1\n",
        "package object a {\n  def x = 1\n}\n",
      ),
      (
        Construct.CaseBody,
        ForceBraces,
        "def f(x: Int) =\n  x match {\n    case 1 =>\n      a()\n      b()\n    }\n",
        "def f(x: Int) =\n  x match {\n    case 1 => {\n      a()\n      b()\n    }\n  }\n",
      ),
      (
        Construct.CaseBody,
        ForceIndent,
        "def f(x: Int) =\n  x match {\n    case 1 => {\n      a()\n      b()\n    }\n  }\n",
        "def f(x: Int) =\n  x match {\n    case 1 =>\n      a()\n      b()\n  }\n",
      ),
      (
        Construct.LambdaBody,
        ForceIndent,
        "val g =\n  (x: Int) => {\n    val y = x\n    y\n  }\n",
        "val g =\n  (x: Int) =>\n    val y = x\n    y\n",
      ),
      (
        Construct.LambdaBody,
        Keep,
        "val g =\n  (x: Int) => {\n    val y = x\n    y\n  }\n",
        "val g =\n  (x: Int) => {\n    val y = x\n    y\n  }\n",
      ),
      (
        Construct.LambdaArgument,
        ForceIndent,
        "def f =\n  xs.map { x =>\n    val y = x\n    y\n  }\n",
        "def f =\n  xs.map: x =>\n    val y = x\n    y\n",
      ),
      (
        Construct.LambdaArgument,
        ForceBraces,
        "def f =\n  xs.map: x =>\n    val y = x\n    y\n",
        "def f =\n  xs.map { x =>\n    val y = x\n    y\n  }\n",
      ),
      (
        Construct.LambdaArgument,
        ForceIndent,
        "def f = xs.map { x => x + 1 }\n",
        "def f = xs.map { x => x + 1 }\n",
      ),
    )

  "each brace style" should {
    layoutRows(
      rows.map {
        case (construct, policy, input, expected) =>
          (s"${construct.key} as ${policy.key}", withBraces(construct -> policy), input, expected)
      },
    )
  }

  "the end markers of statements in a block" should {

    "be written for a braceless construct set to force-end" in {
      val input = "def f(x: Int) =\n  println(x)\n  x match {\n    case 1 => a\n    case _ => b\n  }\n"

      val expected = "def f(x: Int) =\n  println(x)\n  x match\n    case 1 => a\n    case _ => b\n  end match\n"

      input should formatTo(expected, withBraces(Construct.Match -> ForceEnd))
    }

    "be removed for a construct set to force-indent" in {
      val input = "def f(x: Int) =\n  println(x)\n  x match\n    case 1 => a\n    case _ => b\n  end match\n"

      val expected = "def f(x: Int) =\n  println(x)\n  x match\n    case 1 => a\n    case _ => b\n"

      input should formatTo(expected, withBraces(Construct.Match -> ForceIndent))
    }

    "not count as a member of a single-member extension group" in {
      val input = "extension (x: Int) {\n  def a =\n    val y = x\n    y\n  end a\n}\n"

      val expected = input

      input should formatTo(expected, withBraces(Construct.Extension -> ForceIndent, Construct.Def -> ForceEnd))
    }

    "be removed when the construct gets braces" in {
      val input = "def f(x: Int) =\n  println(x)\n  x match\n    case 1 => a\n    case _ => b\n  end match\n"

      val expected = "def f(x: Int) =\n  println(x)\n  x match {\n    case 1 => a\n    case _ => b\n  }\n"

      input should formatTo(expected, withBraces(Construct.Match -> ForceBraces))
    }

  }

  "the braces around a single-expression body (braces.single-expr)" should {

    // A lambda with statements as the only expression of a braced body: Scala 2 needs the braces (tapir
    // core/internal/package.scala, recoverErrors2)
    val bracedLambda =
      """def f: Int => Int = {
        |  x =>
        |    val y = x
        |    y + 1
        |}
        |""".stripMargin

    val bracelessLambda =
      """def f: Int => Int =
        |  x =>
        |    val y = x
        |    y + 1
        |""".stripMargin

    "be removed by default, as before the setting" in {
      bracedLambda should formatTo(bracelessLambda)
    }

    "be kept as written with keep" in {
      bracedLambda should formatStablyTo(bracedLambda, withSingleExpr(Construct.Def -> Keep))
    }

    "not be added with keep" in {
      bracelessLambda should formatTo(bracelessLambda, withSingleExpr(Construct.Def -> Keep))
    }

    "be added to a body over several lines with force-braces" in {
      bracelessLambda should formatStablyTo(bracedLambda, withSingleExpr(Construct.Def -> ForceBraces))
    }

    "not be added to a body on one line with force-braces" in {
      val input = "def f = compute(x)\n"
      input should formatTo(input, withSingleExpr(Construct.Def -> ForceBraces))
    }

    "be removed with force-indent" in {
      bracedLambda should formatTo(bracelessLambda, withSingleExpr(Construct.Def -> ForceIndent))
    }

    "keep the curried header of the tapir case, and its braces, with keep" in {
      val input =
        """def recover(f: Int => Int): Int => Int => Int = {
          |  implicit monad => t =>
          |    val y = f(t)
          |    y + monad
          |}
          |""".stripMargin

      val expected = Right((Some("def recover(f: Int => Int): Int => Int => Int = {"), Some("}")))

      val actual =
        Dixtra
          .format(input, withSingleExpr(Construct.Def -> Keep))
          .map(_.linesIterator.toList)
          .map(lines => (lines.headOption, lines.lastOption))

      actual shouldBe expected
    }

    "follow the setting of a val" in {
      val input =
        """val f: Int => Int = {
          |  x =>
          |    val y = x
          |    y + 1
          |}
          |""".stripMargin

      input should formatStablyTo(input, withSingleExpr(Construct.Val -> Keep))
    }

    "not change a body of several statements, which follows its construct" in {
      val input    = "def f = {\n  val a = 1\n  a + 1\n}\n"
      val expected = "def f =\n  val a = 1\n  a + 1\n"
      input should formatTo(expected, withSingleExpr(Construct.Def -> Keep))
    }

  }

}
