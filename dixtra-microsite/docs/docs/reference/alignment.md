---
layout: docs
title: Alignment
---

# Alignment

What lines up in columns.

Only adjacent one-line items align: a blank line, an item over several lines or any other statement ends a group of aligned items.

An item that would not fit once aligned breaks instead, and the items before and after it align separately.

## `alignment.assignment-groups`

**Default:** `[[val, lazy-val, var, def]]`.

The groups of keywords whose adjacent one-line definitions align their `=`. The keywords are `val`, `lazy-val`, `var`, `def`, `given-alias` and `type-def`. A keyword is in at most one group, and a keyword in no group never aligns and ends the groups around it.

Before:

```scala
val a = 1
var bb = 2
val c = 3
def dddd = 4
```

### `alignment.assignment-groups = [[val, lazy-val, var, def]]`

All four keywords are in one group, so the three definitions align their `=`:

```scala
val a    = 1
var bb   = 2
val c    = 3
def dddd = 4

// With settings:
//   max-column = 80
//   alignment.assignment-groups = [[val, lazy-val, var, def]]
```

### `alignment.assignment-groups = [[val, var], [def]]`

The `def` is in a group of its own, so it aligns with neither `val` nor `var`:

```scala
val a  = 1
var bb = 2
val c  = 3
def dddd = 4

// With settings:
//   max-column = 80
//   alignment.assignment-groups = [[val, var], [def]]
```

### `alignment.assignment-groups = [[val, def]]`

The `var` is in no group, so it never aligns and ends the groups around it:

```scala
val a = 1
var bb = 2
val c    = 3
def dddd = 4

// With settings:
//   max-column = 80
//   alignment.assignment-groups = [[val, def]]
```

### `alignment.assignment-groups = []`

No keyword is in a group, so no definition aligns:

```scala
val a = 1
var bb = 2
val c = 3

def dddd = 4

// With settings:
//   max-column = 80
//   alignment.assignment-groups = []
```

## `alignment.operators`

**Default:** `["=", "+=", "-=", "*=", "/=", ":=", "++=", "%", "%%", "%%%"]`.

The operators that align column by column in adjacent one-line statements, and in adjacent one-line elements of a list laid out one per line. The named arguments follow [`alignment.named-arguments`](#alignmentnamed-arguments) instead.

Before:

```scala
def run(): Unit =
  total += x
  n = 0
```

### `alignment.operators = ["=", "+=", "-=", "*=", "/=", ":=", "++=", "%", "%%", "%%%"]`

The `=` of the two statements aligns with the `+=`:

```scala
def run(): Unit =
  total += x
  n     = 0

// With settings:
//   max-column = 80
//   alignment.operators = ["=", "+=", "-=", "*=", "/=", ":=", "++=", "%", "%%", "%%%"]
```

### `alignment.operators = []`

No operator aligns:

```scala
def run(): Unit =
  total += x
  n = 0

// With settings:
//   max-column = 80
//   alignment.operators = []
```

## `alignment.arrows.cases`

**Default:** `true`.

Whether the `=>` of adjacent one-line cases align.

Before:

```scala
def f(value: Option[Int]) = value match
  case Some(value) => value
  case None => default
```

### `alignment.arrows.cases = true`

The `=>` of the two cases align:

```scala
def f(value: Option[Int]) =
  value match {
    case Some(value) => value
    case None        => default
  }

// With settings:
//   max-column = 80
//   alignment.arrows.cases = true
```

### `alignment.arrows.cases = false`

Each `=>` stays after its own pattern:

```scala
def f(value: Option[Int]) =
  value match {
    case Some(value) => value
    case None => default
  }

// With settings:
//   max-column = 80
//   alignment.arrows.cases = false
```

## `alignment.arrows.pairs`

**Default:** `true`.

Whether the `->` of the `a -> b` elements of a list laid out one per line align.

Before:

```scala
val m = Map(
  "one" -> 1,
  "three" -> 3,
)
```

### `alignment.arrows.pairs = true`

The `->` of the two elements align:

```scala
val m =
  Map(
    "one"   -> 1,
    "three" -> 3,
  )

// With settings:
//   max-column = 80
//   alignment.arrows.pairs = true
```

### `alignment.arrows.pairs = false`

Each `->` stays after its own key:

```scala
val m =
  Map(
    "one" -> 1,
    "three" -> 3,
  )

// With settings:
//   max-column = 80
//   alignment.arrows.pairs = false
```

## `alignment.arrows.for-enumerators`

**Default:** `true`.

Whether the `<-` and the `=` of the enumerators of a `for` align.

Before:

```scala
def f =
  for
    user <- users
    userCount = users.size
    account <- accounts(user)
  yield account
```

### `alignment.arrows.for-enumerators = true`

The `<-` and the `=` of the three enumerators align:

```scala
def f =
  for
    user     <- users
    userCount = users.size
    account  <- accounts(user)
  yield account

// With settings:
//   max-column = 80
//   alignment.arrows.for-enumerators = true
```

### `alignment.arrows.for-enumerators = false`

Each enumerator keeps its own spacing:

```scala
def f =
  for
    user <- users
    userCount = users.size
    account <- accounts(user)
  yield account

// With settings:
//   max-column = 80
//   alignment.arrows.for-enumerators = false
```

## `alignment.extends-clauses`

**Default:** `true`.

Whether the `extends` of adjacent one-line definitions (classes, objects, traits, enum cases) align.

Before:

```scala
enum Color:
  case Red extends Color("red")
  case Green extends Color("green")
```

### `alignment.extends-clauses = true`

The `extends` of the two cases align:

```scala
enum Color {
  case Red   extends Color("red")
  case Green extends Color("green")
}

// With settings:
//   max-column = 80
//   alignment.extends-clauses = true
```

### `alignment.extends-clauses = false`

Each `extends` follows its own name:

```scala
enum Color {
  case Red extends Color("red")
  case Green extends Color("green")
}

// With settings:
//   max-column = 80
//   alignment.extends-clauses = false
```

## `alignment.named-arguments`

**Default:** `false`.

Whether the `=` of adjacent one-line named arguments align, in a call laid out one argument per line.

Before:

```scala
val c = MyCaseClass(
  aaa = "",
  name = 123,
)
```

### `alignment.named-arguments = true`

Currently written like `false`.

<!-- same-output-as: false -->

The `=` of the two named arguments align:

```scala
val c =
  MyCaseClass(
    aaa = "",
    name = 123,
  )

// With settings:
//   max-column = 80
//   alignment.named-arguments = true
```

### `alignment.named-arguments = false`

Each `=` follows its own name:

```scala
val c =
  MyCaseClass(
    aaa = "",
    name = 123,
  )

// With settings:
//   max-column = 80
//   alignment.named-arguments = false
```

## `alignment.trailing-comments`

**Default:** `false`.

Whether the comments at the end of consecutive lines of code start on one column, one space after the longest code. A line without a trailing comment ends the group.

Before:

```scala
def run(): Unit =
  run(1) // one
  compute(2) // two
```

### `alignment.trailing-comments = true`

The two comments start on one column:

```scala
def run(): Unit =
  run(1)     // one
  compute(2) // two

// With settings:
//   max-column = 80
//   alignment.trailing-comments = true
```

### `alignment.trailing-comments = false`

Each comment follows its own code:

```scala
def run(): Unit =
  run(1) // one
  compute(2) // two

// With settings:
//   max-column = 80
//   alignment.trailing-comments = false
```

## `alignment.across-comments`

**Default:** `false`.

Whether a comment on its own line between two items keeps them in one group. The items are definitions, statements, cases and list elements. A blank line still ends a group, and the enumerators of a `for` always align across comments.

Before:

```scala
val a = 1
// note
val bbb = 2
```

### `alignment.across-comments = true`

The comment does not end the group, so the two definitions align:

```scala
val a   = 1
// note
val bbb = 2

// With settings:
//   max-column = 80
//   alignment.across-comments = true
```

### `alignment.across-comments = false`

The comment ends the group, so the definitions align on their own:

```scala
val a = 1
// note
val bbb = 2

// With settings:
//   max-column = 80
//   alignment.across-comments = false
```

## `alignment.scaladoc.tags`

**Default:** `false`.

Whether the descriptions of the tags of a scaladoc start on one column. Wrapped description lines continue under the start of their description.

Before:

```scala
/**
 * @param source The source text.
 * @param width The maximum width.
 * @return The formatted text.
 */
def f(source: String, width: Int): String = ???
```

### `alignment.scaladoc.tags = true`

The descriptions start on one column:

```scala
/**
 * @param source The source text.
 * @param width  The maximum width.
 * @return       The formatted text.
 */
def f(source: String, width: Int): String = ???

// With settings:
//   max-column = 80
//   alignment.scaladoc.tags = true
```

### `alignment.scaladoc.tags = false`

Each description follows its own tag:

```scala
/**
 * @param source The source text.
 * @param width The maximum width.
 * @return The formatted text.
 */
def f(source: String, width: Int): String = ???

// With settings:
//   max-column = 80
//   alignment.scaladoc.tags = false
```
