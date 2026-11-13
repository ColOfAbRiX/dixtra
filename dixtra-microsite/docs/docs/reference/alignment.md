---
layout: docs
title: Alignment
---

# Alignment

What lines up in columns. Every setting is in [the settings table](#the-settings).

Only adjacent one-line items align: a blank line, an item over several lines or any other statement ends a group of aligned items (see also [`across-comments`](#across-comments)). An item that would not fit once aligned breaks instead, and the items before and after it align separately.

## `assignment-groups`

The groups of keywords whose adjacent one-line definitions align their `=`. The keywords are `val`, `lazy-val`, `var`, `def`, `given-alias` and `type-def`. A keyword is in at most one group, and a keyword in no group never aligns and ends the groups around it. Default `[[val, lazy-val, var, def]]`.

With the default:

```scala
val name       = "dixtra"
var count      = 0
def totalCount = count * 2
```

With `[[val, var], [def]]`, the `def` aligns with neither:

```scala
val name  = "dixtra"
var count = 0
def totalCount = count * 2
```

With `[[val, def]]`, the `var` aligns with nothing, and the definitions before and after it align separately:

```scala
val name = "dixtra"
var count = 0
val totalCount = count * 2
def f          = 1
```

With `[]`, no definition aligns.

## `operators`

The operators that align column by column. Default `["=", "+=", "-=", "*=", "/=", ":=", "++=", "%", "%%", "%%%"]`.

They align in adjacent one-line statements, in adjacent one-line elements of a list laid out one per line, and in an operation that is the only argument of a call statement. The named arguments follow [`named-arguments`](#named-arguments) instead.

```scala
total += x
n     = 0
```

```scala
Seq(
  "org.typelevel" %% "cats-core" % "2.13.0",
  "co.fs2"        %% "fs2-io"    % "3.13.0",
)
```

With `[]`, no operator aligns.

## `arrows.cases`

Whether the `=>` of adjacent one-line cases align. Default `true`.

```scala
case Some(value) => value
case None        => default
```

With `false`:

```scala
case Some(value) => value
case None => default
```

## `arrows.pairs`

Whether the `->` of the `a -> b` elements of a list laid out one per line align. Default `true`.

```scala
Map(
  "one"   -> 1,
  "three" -> 3,
)
```

With `false`:

```scala
Map(
  "one" -> 1,
  "three" -> 3,
)
```

## `arrows.for-enumerators`

Whether the `<-` and the `=` of the enumerators of a `for` align. Default `true`.

```scala
for
  user     <- users
  userCount = users.size
  account  <- accounts(user)
yield account
```

With `false`:

```scala
for
  user <- users
  userCount = users.size
  account <- accounts(user)
yield account
```

## `extends-clauses`

Whether the `extends` of adjacent one-line definitions (classes, objects, traits, enum cases) align. Default `true`.

```scala
case Red   extends Color("red")
case Green extends Color("green")
```

With `false`:

```scala
case Red extends Color("red")
case Green extends Color("green")
```

## `named-arguments`

Whether the `=` of adjacent one-line named arguments align, in a call laid out one argument per line. Default `false`.

With `true`:

```scala
MyCaseClass(
  aaa  = "",
  name = 123,
)
```

With `false`:

```scala
MyCaseClass(
  aaa = "",
  name = 123,
)
```

## `trailing-comments`

Whether the comments at the end of consecutive lines of code start on one column, one space after the longest code. A line without a trailing comment ends the group. Default `false`.

With `true`:

```scala
run(1)     // one
compute(2) // two
```

With `false`:

```scala
run(1) // one
compute(2) // two
```

## `across-comments`

Whether a comment on its own line between two items keeps them in one group. The items are definitions, statements, cases and list elements. A blank line still ends a group, and the enumerators of a `for` always align across comments. Default `false`.

With `true`, one group:

```scala
val a   = 1
// note
val bbb = 2
```

With `false`, two groups:

```scala
val a = 1
// note
val bbb = 2
```

## `scaladoc.tags`

Whether the descriptions of the tags of a scaladoc start on one column. Wrapped description lines continue under the start of their description. Default `false`.

With `true`:

```scala
/**
 * @param source The source text.
 * @param width  The maximum width.
 * @return       The formatted text.
 */
```

With `false`:

```scala
/**
 * @param source The source text.
 * @param width The maximum width.
 * @return The formatted text.
 */
```

## The settings

| Setting                  | Default                                                        | Values                      |
| ------------------------ | -------------------------------------------------------------- | --------------------------- |
| `assignment-groups`      | `[[val, lazy-val, var, def]]`                                  | a list of lists of keywords |
| `operators`              | `["=", "+=", "-=", "*=", "/=", ":=", "++=", "%", "%%", "%%%"]` | a list of operators         |
| `arrows.cases`           | `true`                                                         | `true`, `false`             |
| `arrows.pairs`           | `true`                                                         | `true`, `false`             |
| `arrows.for-enumerators` | `true`                                                         | `true`, `false`             |
| `extends-clauses`        | `true`                                                         | `true`, `false`             |
| `named-arguments`        | `false`                                                        | `true`, `false`             |
| `trailing-comments`      | `false`                                                        | `true`, `false`             |
| `across-comments`        | `false`                                                        | `true`, `false`             |
| `scaladoc.tags`          | `false`                                                        | `true`, `false`             |
