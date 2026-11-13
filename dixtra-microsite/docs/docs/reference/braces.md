---
layout: docs
title: Braces
---

# Braces

Whether each construct writes its body in braces, braceless with an `end` marker, or braceless, and where the braces go. Every setting is in [the settings table](#the-settings); the [configuration](../configuration.md) page explains how to write them.

## The values

- `force-braces` — in curly braces.
- `force-end` — braceless, closed by an `end` marker when it spans several lines.
- `force-indent` — braceless and without an `end` marker, the body only indented.
- `keep` — as the author wrote it.

`force-braces`:

```scala
class A {
  def f = 1
}
```

`force-end`:

```scala
class A:
  def f = 1
end A
```

`force-indent`:

```scala
class A:
  def f = 1
```

`keep`: the construct stays as it is written.

`block-argument`, `case-body`, `lambda-argument`, `lambda-body`, `test-case` and `test-group` have no `end` marker, so they reject `force-end`. A `for`, a `while` or an `if` written on one line that fits stays as it is, and a case class or an object without a body never gets braces.

## `style`

Where the braces go relative to the code around them. Default `1tbs`. It applies to every brace Dixtra writes; the braces of an argument (`xs.map { x => ... }`) and of a body after `=>` stay where they are.

`1tbs` — the `{` at the end of the header line, and `} else {` on one line:

```scala
if (c) {
  a()
} else {
  b()
}
```

`stroustrup` — as `1tbs`, with `else`, `catch`, `finally` and `yield` starting the line after the closing `}`:

```scala
if (c) {
  a()
}
else {
  b()
}
```

`allman` — as `stroustrup`, with every opening `{` of a body on its own line:

```scala
if (c)
{
  a()
}
else
{
  b()
}
```

## `imports`

Whether the single selector of an `import` or of an `export` is in braces. Default `keep`. The selectors keep their spelling (`_` / `*`, `=>` / `as`), and an import with several selectors always keeps its braces.

`force-braces`:

```scala
import a.{ B }
import a.{ * }
```

`avoid-braces` — a rename keeps its braces:

```scala
import a.B
import a.*
import a.{ B => C }
```

`keep`: as written.

## The settings

| Setting | Default |
| --- | --- |
| `style` | `1tbs` |
| `imports` | `keep` |
| `abstract-class` | `force-braces` |
| `block-argument` | `force-braces` |
| `case-body` | `force-indent` |
| `case-class` | `force-braces` |
| `class` | `force-braces` |
| `def` | `force-indent` |
| `enum` | `force-braces` |
| `extension` | `force-braces` |
| `for` | `force-indent` |
| `for-yield` | `force-indent` |
| `given-alias` | `keep` |
| `given-instance` | `force-braces` |
| `if` | `force-indent` |
| `lambda-argument` | `force-braces` |
| `lambda-body` | `keep` |
| `lazy-val` | `force-indent` |
| `match` | `force-braces` |
| `new-anonymous` | `force-braces` |
| `object` | `force-braces` |
| `package` | `force-indent` |
| `package-object` | `keep` |
| `test-case` | `force-braces` |
| `test-group` | `force-braces` |
| `trait` | `force-braces` |
| `try` | `force-braces` |
| `val` | `force-indent` |
| `var` | `force-indent` |
| `while` | `force-braces` |
| `single-expr.case-body` | `force-indent` |
| `single-expr.def` | `force-indent` |
| `single-expr.given-alias` | `force-indent` |
| `single-expr.lambda-body` | `force-indent` |
| `single-expr.lazy-val` | `force-indent` |
| `single-expr.val` | `force-indent` |
| `single-expr.var` | `force-indent` |

`single-expr` holds the braces around a body that is a single expression, after the `=` of a definition or the `=>` of a lambda or of a case; a body of several statements follows the setting of its construct above. `force-braces` puts a body over several lines in braces (a body on one line never gets them), `force-indent` removes them, `keep` leaves them as written; `force-end` is rejected.

## Each construct in the form its default does not write

```scala
// abstract-class = force-indent
abstract class A:
  def f: Int

// block-argument = force-indent
Resource.make:
  open()

// case-body = force-braces
case p => {
  a()
  b()
}

// case-class = force-indent
case class A(x: Int):
  def f = x

// class = force-indent
class A:
  def f = 1

// def = force-braces
def f = {
  val a = 1
  a + 1
}

// enum = force-indent
enum Color:
  case Red
  case Green

// extension = force-indent
extension (s: String)
  def a = s
  def b = s

// for = force-braces
for {
  x <- xs
} {
  a(x)
}

// for-yield = force-braces
for {
  x <- xs
  y <- ys
} yield {
  x + y
}

// given-alias = force-braces
given Ord[Int] = {
  val b = 1
  ord(b)
}

// given-instance = force-indent
given Ord[Int] with
  def compare(a: Int, b: Int) = a - b

// if = force-braces
if (c) {
  a()
} else {
  b()
}

// lambda-argument = force-indent
xs.map: x =>
  x + 1

// lambda-body = force-braces
val f =
  (x: Int) => {
    val y = x
    y + 1
  }

// lazy-val = force-braces
lazy val a = {
  val b = 1
  b + 1
}

// match = force-indent
x match
  case 1 => a
  case 2 => b

// new-anonymous = force-indent
new Runnable:
  def run() = ()

// object = force-indent
object A:
  val a = 1

// package = force-braces
package a.b {

  object C

}

// package-object = force-braces
package object a {
  val x = 1
}

// test-case = force-indent
test("a parser"):
  parse("1")

// test-group = force-indent
describe("a parser"):
  it("parses a number") {
    parse("1")
  }

// trait = force-indent
trait A:
  def f: Int

// try = force-indent
try
  a()
finally
  b()

// val = force-braces
val a = {
  val b = 1
  b + 1
}

// var = force-braces
var a = {
  val b = 1
  b + 1
}

// while = force-indent
while c do
  step()
  more()

// single-expr.def = keep
def f: Int => Int = {
  x =>
    val y = x
    y + 1
}
```
