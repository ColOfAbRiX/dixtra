---
layout: docs
title: Definition sites
---

# Definition sites

The layout of the names where they are declared: the parameters and the type parameters of `def`s and of class constructors, and the selectors of an `import` or of an `export`. A signature that does not fit breaks one parameter per line, and every group of parameters breaks with it. Every setting is in [the settings table](#the-settings).

## `trailing-commas`

Whether the last parameter, type parameter or import selector of a list laid out one per line is followed by a comma. A list on one line never has one. Default `always`.

`always`:

```scala
def f(
  first: String,
  second: Boolean,
): Int = 1
```

`never`:

```scala
def f(
  first: String,
  second: Boolean
): Int = 1
```

## `groups-separator`

Where `)(` goes between the groups of parameters of a broken signature. Default `next-group`.

`next-group` — at the start of the next group, on the line of the closing `)`:

```scala
def f[A](
  first: String,
  second: Boolean,
)(numeric: Numeric[A],
)(using
  ord: Ordering[A],
): Int = 1
```

`previous-group` — after the last parameter of the group before, with the parameters of the next group below it:

```scala
def f[A](
  first: String,
  second: Boolean)(
  numeric: Numeric[A])(
  using
  ord: Ordering[A],
): Int = 1
```

A `using` or an `implicit` keyword is placed like a parameter: on a line of its own when its group opens at the end of a line, and on the `)(` line with `next-group`.

## The settings

| Setting | Default | Values |
| --- | --- | --- |
| `trailing-commas` | `always` | `always`, `never` |
| `groups-separator` | `next-group` | `next-group`, `previous-group` |
