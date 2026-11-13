---
layout: docs
title: Selector chains
---

# Selector chains

When a chain of selections (`a.b(x).c.d`) breaks, one selection per line. A chain always breaks when it does not fit, or when the author broke it before a dot.

A broken chain has its receiver on the first line and each selection below it, one level in:

```scala
xs
  .filter(_.paid)
  .map(_.amount)
  .sum
```

Every setting is in [the settings table](#the-settings).

## `inline-chains`

Whether a chain on the line of its `=` or `=>` breaks at `break-at-count` selections too. Default `break-at-count`.

`break-at-count` — it breaks like a chain written below the `=`, and goes below it:

```scala
val y =
  xs
    .map(_ + 1)
    .filter(_ > 0)
    .sum
```

`keep` — it stays on one line while it fits:

```scala
val y = xs.map(_ + 1).filter(_ > 0).sum
```

## `break-at-count`

How many selections make a chain that is a whole body break, even when it fits. Default `3`. The receiver does not count, and the value is at least 2.

With `3`, two selections stay on one line and three break:

```scala
def total =
  xs.map(_.amount).sum

def total =
  xs
    .filter(_.paid)
    .map(_.amount)
    .sum
```

A chain inside an operation, an argument or a condition breaks only when it does not fit, or when the author broke it: `p.speed.vertical >= config.minSpeed` stays on one line. A chain on the line of its `=` follows `inline-chains`.

## `in-for`

How a chain breaks in the enumerators of a `for`: the generators (`x <- xs`), the guards (`if ...`) and the value definitions (`y = ...`), and everything they hold. The expression after `yield` and the body after `do` are not enumerators.

A chain there never breaks at `break-at-count` selections, and it always breaks when it does not fit. Default `never-break`.

`never-break` — a chain the author broke is joined while it fits:

```scala
for
  user <- users.filter(_.active)
yield user.name
```

`keep` — a chain stays broken where the author broke it:

```scala
for
  user <- users
            .filter(_.active)
yield user.name
```

## `in-call`

How a chain breaks in the arguments of a call: the arguments of a call or of a constructor, positional or named, the elements of a tuple, the arguments of an infix application with several of them (`x shouldBe (a, b)`), and everything they hold. The bodies of the lambdas and of the blocks given as arguments are not arguments: a chain there breaks like in any other body.

A chain in an argument never breaks at `break-at-count` selections, and it always breaks when it does not fit. Default `never-break`.

`never-break` — a chain the author broke is joined while it fits:

```scala
SignatureParts(
  nameEnd = d.name.pos.end,
)
```

`keep` — a chain stays broken where the author broke it:

```scala
SignatureParts(
  nameEnd = d
    .name
    .pos
    .end,
)
```

## `namespace-path`

Whether a package path at the start of a chain stays together as its receiver. A package path is the run of lowercase names up to the first capitalised one: `java.nio.file.Files`. Default `keep`.

`always-join` — always, even when the author broke the line inside the path:

```scala
java.nio.file.Files
  .exists(path)
  .toString
```

`keep` — as `always-join`, unless the author broke the line inside the path.

`always-break` — the path breaks like any other selection:

```scala
java
  .nio
  .file
  .Files
  .exists(path)
  .toString
```

## `object-member`

Whether the first member selected on a capitalised name stays with it as the receiver. Default `always-break`.

`always-join` — always, even when the author broke the line before its dot:

```scala
IO.pure(x)
  .map(f)
  .flatMap(g)
```

`keep` — as `always-join`, unless the author broke the line before its dot.

`always-break` — the member breaks like any other selection:

```scala
IO
  .pure(x)
  .map(f)
  .flatMap(g)
```

## `receiver-min-length`

The shortest receiver name a chain breaks after. Default `1`, and the value is at least 1. A shorter receiver keeps its first selection on its line, and that selection still counts towards `break-at-count`.

With `3`, `v` keeps `.trim` and `values` breaks:

```scala
v.trim
  .toLowerCase
  .nonEmpty

values
  .map(_.trim)
  .filter(_.nonEmpty)
  .size
```

## The settings

| Setting | Default | Values |
| --- | --- | --- |
| `inline-chains` | `break-at-count` | `break-at-count`, `keep` |
| `break-at-count` | `3` | a number, at least 2 |
| `in-for` | `never-break` | `never-break`, `keep` |
| `in-call` | `never-break` | `never-break`, `keep` |
| `namespace-path` | `keep` | `always-join`, `keep`, `always-break` |
| `object-member` | `always-break` | `always-join`, `keep`, `always-break` |
| `receiver-min-length` | `1` | a number, at least 1 |
