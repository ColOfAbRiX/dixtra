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

`always` — a blank line at both edges:

```scala
class A {

  def f = 1

}
```

`never` — no blank line at the edges:

```scala
class A {
  def f = 1
}
```

`keep` — as written, made to agree: a blank line at either edge means one at both. Written with a blank line only after the `{`, the class gets one before the `}` too:

```scala
class A {

  def f = 1

}
```

By default the containers and the test blocks keep the blank lines the author wrote, the package clause is followed by one, and the other bodies never have any.

`test-case` and `test-group` tell the tests from the groups of tests, so a configuration can give a blank line after `"a parser" should {` and none after `"parses a number" in {`:

```scala
"a parser" should {

  "parse a number" in {
    parse("1")
  }

}
```

A test case is a test block that holds no other test block; a group is one that holds them. See [bodies](body.md) for the difference.

## The blank line between two one-line definitions

`between-alignment-groups` holds the blank line between two adjacent one-line definitions of different alignment groups (see [`alignment.assignment-groups`](alignment.md)). The examples are with `[[val], [def]]`.

`keep` — as written. `always`:

```scala
val a = 1

def b = 2
```

`never`:

```scala
val a = 1
def b = 2
```

## The blank line before a scaladoc

`before-scaladoc` holds the blank line before a scaladoc that follows a line of code.

`always` — before every scaladoc, except a one-line scaladoc right after a line that opens a block. With `body-edges.class = keep`, a scaladoc at the start of a class gives both edges a blank line:

```scala
class Repository {

  /** Description here */
  def read(): Int = 1

}
```

`always-multiline` — as `always`, for a multi-line scaladoc only; a one-line scaladoc stays as written, and does not count as a blank line at the start of a body:

```scala
def a = 1
/** One line */
def b = 2
```

`keep` — as written. `never` — no blank line, a method's included; after a line that opens a block, `body-edges` decides:

```scala
def a = 1
/** Description here */
def b = 2
```

At the start of a body (a class, an object, a trait, an enum, an extension, a package), a scaladoc that gets a blank line counts as a blank line written at the opening edge, and `body-edges` decides.

## The settings

| Setting | Default |
| --- | --- |
| `body-edges.abstract-class` | `keep` |
| `body-edges.block-argument` | `never` |
| `body-edges.case-body` | `never` |
| `body-edges.case-class` | `keep` |
| `body-edges.class` | `keep` |
| `body-edges.def` | `never` |
| `body-edges.enum` | `keep` |
| `body-edges.extension` | `keep` |
| `body-edges.for` | `never` |
| `body-edges.for-yield` | `never` |
| `body-edges.given-alias` | `never` |
| `body-edges.given-instance` | `keep` |
| `body-edges.if` | `never` |
| `body-edges.lambda-argument` | `never` |
| `body-edges.lambda-body` | `never` |
| `body-edges.lazy-val` | `never` |
| `body-edges.match` | `never` |
| `body-edges.new-anonymous` | `never` |
| `body-edges.object` | `keep` |
| `body-edges.package` | `always` |
| `body-edges.package-object` | `keep` |
| `body-edges.test-case` | `keep` |
| `body-edges.test-group` | `keep` |
| `body-edges.trait` | `keep` |
| `body-edges.try` | `never` |
| `body-edges.val` | `never` |
| `body-edges.var` | `never` |
| `body-edges.while` | `never` |
| `between-alignment-groups` | `keep` |
| `before-scaladoc` | `always` |
