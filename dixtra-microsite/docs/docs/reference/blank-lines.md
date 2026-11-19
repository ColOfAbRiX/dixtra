---
layout: docs
title: Blank lines
---

# Blank lines

Where blank lines go, beyond the ones the author wrote between statements. Every setting is in [the settings table](#the-settings).

Some rules have no setting:

- A run of blank lines always becomes one.
- A method after another statement always gets a blank line before it, and before its annotations and comments.
- The blank lines between the imports are always removed, so the groups of imports are not supported.

## The edges of the bodies

The edges are after the opening `{` (or after the header, without braces) and before the closing `}`; a body without braces has no closing edge. For `package`, the edge is the blank line after the package clause.

`blank-lines.body-edges.<construct> = always` — a blank line at both edges:

```scala
class A {

  def f = 1

}
```

`blank-lines.body-edges.<construct> = never` — no blank line at the edges:

```scala
class A {
  def f = 1
}
```

`blank-lines.body-edges.<construct> = keep` — as written, made to agree: a blank line at either edge means one at both. Written with a blank line only after the `{`, the class gets one before the `}` too:

```scala
class A {

  def f = 1

}
```

By default the containers and the test blocks keep the blank lines the author wrote, the package clause is followed by one, and the other bodies never have any.

`blank-lines.body-edges.test-case` and `blank-lines.body-edges.test-group` tell the tests from the groups of tests, so a configuration can give a blank line after `"a parser" should {` and none after `"parses a number" in {`:

```scala
"a parser" should {

  "parse a number" in {
    parse("1")
  }

}
```

A test case is a test block that holds no other test block; a group is one that holds them. See [bodies](body.md) for the difference.

## The blank line between two one-line definitions

`blank-lines.between-alignment-groups` holds the blank line between two adjacent one-line definitions of different alignment groups (see [`alignment.assignment-groups`](alignment.md)). The examples are with `alignment.assignment-groups = [[val], [def]]`.

`blank-lines.between-alignment-groups = keep` — as written. `blank-lines.between-alignment-groups = always`:

```scala
val a = 1

def b = 2
```

`blank-lines.between-alignment-groups = never`:

```scala
val a = 1
def b = 2
```

## The blank line before a scaladoc

`blank-lines.before-scaladoc` holds the blank line before a scaladoc that follows a line of code.

`blank-lines.before-scaladoc = always` — before every scaladoc, except a one-line scaladoc right after a line that opens a block. With `blank-lines.body-edges.class = keep`, a scaladoc at the start of a class gives both edges a blank line:

```scala
class Repository {

  /** Description here */
  def read(): Int = 1

}
```

`blank-lines.before-scaladoc = always-multiline` — as `blank-lines.before-scaladoc = always`, for a multi-line scaladoc only; a one-line scaladoc stays as written, and does not count as a blank line at the start of a body:

```scala
def a = 1
/** One line */
def b = 2
```

`blank-lines.before-scaladoc = keep` — as written. `blank-lines.before-scaladoc = never` — no blank line, a method's included; after a line that opens a block, `blank-lines.body-edges` decides:

```scala
def a = 1
/** Description here */
def b = 2
```

At the start of a body (a class, an object, a trait, an enum, an extension, a package), a scaladoc that gets a blank line counts as a blank line written at the opening edge, and `blank-lines.body-edges` decides.

## The settings

| Setting                                | Default  |
| -------------------------------------- | -------- |
| `blank-lines.body-edges.abstract-class`  | `keep`   |
| `blank-lines.body-edges.block-argument`  | `never`  |
| `blank-lines.body-edges.case-body`       | `never`  |
| `blank-lines.body-edges.case-class`      | `keep`   |
| `blank-lines.body-edges.class`           | `keep`   |
| `blank-lines.body-edges.def`             | `never`  |
| `blank-lines.body-edges.enum`            | `keep`   |
| `blank-lines.body-edges.extension`       | `keep`   |
| `blank-lines.body-edges.for`             | `never`  |
| `blank-lines.body-edges.for-yield`       | `never`  |
| `blank-lines.body-edges.given-alias`     | `never`  |
| `blank-lines.body-edges.given-instance`  | `keep`   |
| `blank-lines.body-edges.if`              | `never`  |
| `blank-lines.body-edges.lambda-argument` | `never`  |
| `blank-lines.body-edges.lambda-body`     | `never`  |
| `blank-lines.body-edges.lazy-val`        | `never`  |
| `blank-lines.body-edges.match`           | `never`  |
| `blank-lines.body-edges.new-anonymous`   | `never`  |
| `blank-lines.body-edges.object`          | `keep`   |
| `blank-lines.body-edges.package`         | `always` |
| `blank-lines.body-edges.package-object`  | `keep`   |
| `blank-lines.body-edges.test-case`       | `keep`   |
| `blank-lines.body-edges.test-group`      | `keep`   |
| `blank-lines.body-edges.trait`           | `keep`   |
| `blank-lines.body-edges.try`             | `never`  |
| `blank-lines.body-edges.val`             | `never`  |
| `blank-lines.body-edges.var`             | `never`  |
| `blank-lines.body-edges.while`           | `never`  |
| `blank-lines.between-alignment-groups`   | `keep`   |
| `blank-lines.before-scaladoc`            | `always` |
