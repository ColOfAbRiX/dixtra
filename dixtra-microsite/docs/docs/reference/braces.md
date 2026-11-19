---
layout: docs
title: Braces
---

# Braces

Whether each construct writes its body in braces, braceless with an `end` marker, or braceless, and where the braces go. Every setting is in [the settings table](#the-settings); the [configuration](../configuration.md) page explains how to write them.

## The values

- `braces.<construct> = force-braces` — in curly braces.
- `braces.<construct> = force-end` — braceless, closed by an `end` marker when it spans several lines.
- `braces.<construct> = force-indent` — braceless and without an `end` marker, the body only indented.
- `braces.<construct> = keep` — as the author wrote it.

`braces.<construct> = force-braces`:

```scala
class A {
  def f = 1
}
```

`braces.<construct> = force-end`:

```scala
class A:
  def f = 1
end A
```

`braces.<construct> = force-indent`:

```scala
class A:
  def f = 1
```

`braces.<construct> = keep`: the construct stays as it is written.

`block-argument`, `case-body`, `lambda-argument`, `lambda-body`, `test-case` and `test-group` have no `end` marker, so they reject `braces.<construct> = force-end`. A `for`, a `while` or an `if` written on one line that fits stays as it is, and a case class or an object without a body never gets braces.

## `braces.style`

Where the braces go relative to the code around them. Default `braces.style = 1tbs`. It applies to every brace Dixtra writes; the braces of an argument (`xs.map { x => ... }`) and of a body after `=>` stay where they are.

See [Indentation Style](https://en.wikipedia.org/wiki/Indentation_style)

`braces.style = 1tbs` — the `{` at the end of the header line, and `} else {` on one line:

```scala
if (c) {
  a()
} else {
  b()
}
```

`braces.style = stroustrup` — as `braces.style = 1tbs`, with `else`, `catch`, `finally` and `yield` starting the line after the closing `}`:

```scala
if (c) {
  a()
}
else {
  b()
}
```

`braces.style = allman` — as `braces.style = stroustrup`, with every opening `{` of a body on its own line:

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

## `braces.imports`

Whether the single selector of an `import` or of an `export` is in braces. Default `braces.imports = keep`. The selectors keep their spelling (`_` / `*`, `=>` / `as`), and an import with several selectors always keeps its braces.

`braces.imports = force-braces`:

```scala
import a.{ B }
import a.{ * }
```

`braces.imports = avoid-braces` — a rename keeps its braces:

```scala
import a.B
import a.*
import a.{ B => C }
```

`braces.imports = keep`: as written.

## The settings

| Setting                          | Default                       |
| -------------------------------- | ----------------------------- |
| `braces.style`                   | `1tbs`                        |
| `braces.imports`                 | `keep`                        |
| `braces.abstract-class`          | `force-braces`                |
| `braces.block-argument`          | `force-braces`                |
| `braces.case-body`               | `force-indent`                |
| `braces.case-class`              | `force-braces`                |
| `braces.class`                   | `force-braces`                |
| `braces.def`                     | `force-indent`                |
| `braces.enum`                    | `force-braces`                |
| `braces.extension`               | `force-braces`                |
| `braces.for`                     | `force-indent`                |
| `braces.for-yield`               | `force-indent`                |
| `braces.given-alias`             | `keep`                        |
| `braces.given-instance`          | `force-braces`                |
| `braces.if`                      | `force-indent`                |
| `braces.lambda-argument`         | `force-braces`                |
| `braces.lambda-body`             | `keep`                        |
| `braces.lazy-val`                | `force-indent`                |
| `braces.match`                   | `force-braces`                |
| `braces.new-anonymous`           | `force-braces`                |
| `braces.object`                  | `force-braces`                |
| `braces.package`                 | `force-indent`                |
| `braces.package-object`          | `keep`                        |
| `braces.test-case`               | `force-braces`                |
| `braces.test-group`              | `force-braces`                |
| `braces.trait`                   | `force-braces`                |
| `braces.try`                     | `force-braces`                |
| `braces.val`                     | `force-indent`                |
| `braces.var`                     | `force-indent`                |
| `braces.while`                   | `force-braces`                |
| `braces.single-expr.case-body`   | `force-indent`                |
| `braces.single-expr.def`         | `force-indent`                |
| `braces.single-expr.given-alias` | `force-indent`                |
| `braces.single-expr.lambda-body` | `force-indent`                |
| `braces.single-expr.lazy-val`    | `force-indent`                |
| `braces.single-expr.val`         | `force-indent`                |
| `braces.single-expr.var`         | `force-indent`                |

`braces.single-expr` holds the braces around a body that is a single expression, after the `=` of a definition or the `=>` of a lambda or of a case; a body of several statements follows the setting of its construct above. `braces.<construct> = force-braces` puts a body over several lines in braces (a body on one line never gets them), `braces.<construct> = force-indent` removes them, `braces.<construct> = keep` leaves them as written; `braces.<construct> = force-end` is rejected.

## Each construct in the form its default does not write

```scala
// braces.abstract-class = force-indent
abstract class A:
  def f: Int

// braces.block-argument = force-indent
Resource.make:
  open()

// braces.case-body = force-braces
case p => {
  a()
  b()
}

// braces.case-class = force-indent
case class A(x: Int):
  def f = x

// braces.class = force-indent
class A:
  def f = 1

// braces.def = force-braces
def f = {
  val a = 1
  a + 1
}

// braces.enum = force-indent
enum Color:
  case Red
  case Green

// braces.extension = force-indent
extension (s: String)
  def a = s
  def b = s

// braces.for = force-braces
for {
  x <- xs
} {
  a(x)
}

// braces.for-yield = force-braces
for {
  x <- xs
  y <- ys
} yield {
  x + y
}

// braces.given-alias = force-braces
given Ord[Int] = {
  val b = 1
  ord(b)
}

// braces.given-instance = force-indent
given Ord[Int] with
  def compare(a: Int, b: Int) = a - b

// braces.if = force-braces
if (c) {
  a()
} else {
  b()
}

// braces.lambda-argument = force-indent
xs.map: x =>
  x + 1

// braces.lambda-body = force-braces
val f =
  (x: Int) => {
    val y = x
    y + 1
  }

// braces.lazy-val = force-braces
lazy val a = {
  val b = 1
  b + 1
}

// braces.match = force-indent
x match
  case 1 => a
  case 2 => b

// braces.new-anonymous = force-indent
new Runnable:
  def run() = ()

// braces.object = force-indent
object A:
  val a = 1

// braces.package = force-braces
package a.b {

  object C

}

// braces.package-object = force-braces
package object a {
  val x = 1
}

// braces.test-case = force-indent
test("a parser"):
  parse("1")

// braces.test-group = force-indent
describe("a parser"):
  it("parses a number") {
    parse("1")
  }

// braces.trait = force-indent
trait A:
  def f: Int

// braces.try = force-indent
try
  a()
finally
  b()

// braces.val = force-braces
val a = {
  val b = 1
  b + 1
}

// braces.var = force-braces
var a = {
  val b = 1
  b + 1
}

// braces.while = force-indent
while c do
  step()
  more()

// braces.single-expr.def = keep
def f: Int => Int = {
  x =>
    val y = x
    y + 1
}
```
