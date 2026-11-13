---
layout: docs
title: Bodies
---

# Bodies

Where each body goes relative to the separator before it: on its line, or below it one level in. The separators are the `=` of a definition, the `=>` of a lambda or of a case, the `then` and the `else` of an if/else, the `do` of a loop. Every setting is in [the settings table](#the-settings).

## The values

A construct with a single body (a definition, a lambda, a loop) takes:

- `keep` — the body stays where the author wrote it.
- `always-join` — the body on the line of its separator, even when the author wrote it below.
- `always-break` — the body below its separator, even when the author wrote it on the line.

`keep`:

```scala
def total =
  xs.sum
```

`always-join`:

```scala
def total = xs.sum
```

`always-break`:

```scala
def total =
  xs.sum
```

The bodies of a construct with several of them (the branches of an if/else, the cases of a match, the bodies of a try) are treated as a group, never one by one: either every body is on the line of its separator, or every body is below it. A group where some bodies are inline and some others are on a new line is *mixed*. These constructs take:

- `join-mixed` — a mixed group is joined on the lines of the separators; a group all on its lines, or all below, stays as it is.
- `break-mixed` — a mixed group goes below the separators; a group all on its lines, or all below, stays as it is.
- `always-join` — every body on the line of its separator.
- `always-break` — every body below its separator.

The examples are for this mixed input:

```scala
if c then
  a
else b
```

`join-mixed` — the first branch moves up:

```scala
if c then a
else b
```

`break-mixed` — the second branch goes below too:

```scala
if c then
  a
else
  b
```

`always-join` — as `join-mixed`, and a group all below its separators moves up too. `always-break` — as `break-mixed`, and `if c then a else b` goes below too.

A group of one body (an `if` without `else`, a match with one case) is never mixed, so both `-mixed` values leave it as written.

## The rules that hold whatever the setting

- A body goes below its separator when it cannot be joined on its line: it does not fit there on one line, or a comment follows the separator.
- In a group, such a body puts every body of the group below its separator, and so does a body that holds an `if`, a `match` or a `try`.
- The settings move bodies only relative to their separators: an `else` written on its own line stays on its own line, and every `case` is on its own line.
- With braces (see [braces](braces.md)), a body below its separator is in braces, and a construct whose bodies are on their lines has none: `if (c) a else b`, `for (x <- xs) f(x)`, `while (c) step()`.

## The settings

| Setting | Default | The body after |
| --- | --- | --- |
| `def` | `keep` | The `=` of a `def` |
| `val` | `keep` | The `=` of a `val` |
| `lazy-val` | `keep` | The `=` of a `lazy val` |
| `var` | `keep` | The `=` of a `var` |
| `given-alias` | `keep` | The `=` of a given alias |
| `type-def` | `keep` | The `=` of a type definition |
| `assignment` | `keep` | The `=` of an assignment |
| `named-argument` | `keep` | The `=` of a named argument |
| `lambda-argument` | `keep` | The `=>` of a lambda given in braces or after a colon |
| `lambda-body` | `keep` | The `=>` of any other lambda |
| `block-argument` | `keep` | The `{` or the colon of a block given as an argument |
| `test-block` | `keep` | The `{` or the colon of a test block |
| `if` | `break-mixed` | The `then` and the `else` of an if/else |
| `case-body` | `break-mixed` | The `=>` of the cases of a match, of a `{ case ... }` or of a `catch` |
| `try` | `break-mixed` | The `try` and the `finally` of a try |
| `for` | `keep` | The `do` of a `for` loop |
| `for-yield` | `break-complex` | The `yield` of a for-comprehension |
| `while` | `keep` | The `do` of a `while` loop |
| `semicolon-statements` | `always-break` | The `;` between the statements of a body written on one line |

## The body after `=`

The eight settings of the `=` body — `def`, `val`, `lazy-val`, `var`, `given-alias`, `type-def`, `assignment`, `named-argument` — move the body the same way. With `body.def = always-break`:

```scala
def total =
  xs.sum
```

A `def` whose signature is over several lines keeps its body where it is, even with `always-join`.

## The bodies of a lambda

`lambda-argument` is the `=>` of a lambda given in braces or after a colon; `lambda-body` is the `=>` of any other lambda.

`always-join`:

```scala
xs.foreach { x => println(x) }
```

`always-break`:

```scala
xs.foreach { x =>
  println(x)
}
```

## The blocks given as arguments

`block-argument` is a block of statements given as an argument, without lambda parameters, that is not a test block; `test-block` is the block of a test case, a block of statements given to a call labelled by a string. Both move their statements the same way.

`always-join` — the statements on the line of the `{`, with a `;` between several of them:

```scala
Resource.make { open() }
```

`always-break`:

```scala
Resource.make {
  open()
}
```

## The branches of an if/else

`if` moves the branches after the `then` and the `else`. The branches of an `else if` chain are one group with those of the outer if/else. On the lines of their separators, the if/else stays on one line when the author wrote each `else` on the line of the branch before it; otherwise every `else` starts a line of its own. Below their separators, an `else if` stays on the line of its `else`.

`always-join`, for the input above:

```scala
if c then a
else b
```

`always-break`:

```scala
if c then
  a
else
  b
```

## The bodies of the cases

`case-body` moves the bodies after the `=>` of the cases of a `match`, of a `{ case ... }` or of a `catch`, which are one group. When a case would not fit once its arrow is aligned (see [`alignment.arrows.cases`](alignment.md)), every body goes below its `=>`.

The examples are for this mixed input:

```scala
x match {
  case 1 =>
    "one"
  case 22 => "two"
}
```

`join-mixed`:

```scala
x match {
  case 1  => "one"
  case 22 => "two"
}
```

`break-mixed`:

```scala
x match {
  case 1 =>
    "one"
  case 22 =>
    "two"
}
```

`always-join` — as `join-mixed`, also when every body is below its `=>`. `always-break` — as `break-mixed`, also when every body is on its `=>` line.

## The bodies of a try

`try` moves the bodies after the `try` and the `finally`, which are one group; the cases after `catch` follow `case-body`. A try written on one line that fits stays as it is, unless its bodies go below their keywords. With `braces.try = force-braces`, the bodies of a try over several lines are always below their `{`.

The examples, with `braces.try = force-indent`, are for this mixed input:

```scala
try
  a()
finally b()
```

`join-mixed`:

```scala
try a()
finally b()
```

`break-mixed`:

```scala
try
  a()
finally
  b()
```

`always-join` — as `join-mixed`, also when both bodies are below their keywords. `always-break` — as `break-mixed`, also when both bodies are on the lines of their keywords.

## The bodies of the loops

`for` is the body after the `do` of a `for` loop; `while` is the body after the `do` of a `while` loop. A loop written on one line that fits stays as it is, unless `always-break`.

`always-join`:

```scala
for x <- xs do println(x)
```

`always-break`:

```scala
for x <- xs do
  println(x)
```

With `braces.for = force-braces`, a body on its line gives `for (x <- xs) println(x)`.

## The expression after `yield`

`for-yield` moves the expression after the `yield` of a for-comprehension. A for-comprehension written on one line that fits stays as it is, unless `always-break`.

`always-join`:

```scala
yield x + y
```

`always-break`:

```scala
yield
  x + y
```

`break-complex` — as `always-break`, but a simple expression, a single name, a path (`ExitCode.Success`) or a literal stays on the line of the `yield`:

```scala
yield
  x + y

yield name
```

## The statements on one line

`semicolon-statements` holds the statements of a body written on one line and separated by `;`, whatever the construct.

`keep` — they stay on one line, with their braces, while it fits:

```scala
def this(s: String) = { this(s.toInt, 0); println(s) }
```

`always-break` — one statement per line, below the `=`:

```scala
def this(s: String) =
  this(s.toInt, 0)
  println(s)
```
