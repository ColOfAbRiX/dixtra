
# Selector chains

When a chain of selections (`a.b(x).c.d`) breaks, one selection per line.

A chain always breaks when it does not fit, or when the author broke it before a dot.

A broken chain has its receiver on the first line and each selection below it, one level in.

## `selector-chain.inline-chains`

**Default:** `break-at-count`.

Whether a chain on the line of its `=` or `=>` breaks at `selector-chain.break-at-count` selections too.

Before:

```scala
val y = xs.map(_ + 1).filter(_ > 0).sum
val averageDiscountedPriceOfInStockItems = inventory.filter(_.inStock).map(_.discountedPrice).sum
```

### `selector-chain.inline-chains = break-at-count`

It breaks like a chain written below the `=`, and goes below it:

```scala
val y =
  xs
    .map(_ + 1)
    .filter(_ > 0)
    .sum
val averageDiscountedPriceOfInStockItems =
  inventory
    .filter(_.inStock)
    .map(_.discountedPrice)
    .sum

// With settings:
//   max-column = 80
//   selector-chain.inline-chains = break-at-count
```

### `selector-chain.inline-chains = keep`

It stays on one line while it fits:

```scala
val y = xs.map(_ + 1).filter(_ > 0).sum
val averageDiscountedPriceOfInStockItems =
  inventory
    .filter(_.inStock)
    .map(_.discountedPrice)
    .sum

// With settings:
//   max-column = 80
//   selector-chain.inline-chains = keep
```

## `selector-chain.break-at-count`

**Default:** `3`.

How many selections make a chain that is a whole body break, even when it fits. The receiver does not count, and the value is at least 2.

Before:

```scala
def total = xs.filter(_.paid).map(_.amount).sum
```

### `selector-chain.break-at-count = 3`

Three selections break the chain:

```scala
def total =
  xs
    .filter(_.paid)
    .map(_.amount)
    .sum

// With settings:
//   max-column = 80
//   selector-chain.break-at-count = 3
```

### `selector-chain.break-at-count = 4`

Three selections stay on one line, and only four break it:

```scala
def total = xs.filter(_.paid).map(_.amount).sum

// With settings:
//   max-column = 80
//   selector-chain.break-at-count = 4
```

A chain inside an operation, an argument or a condition breaks only when it does not fit, or when the author broke it. A chain on the line of its `=` follows `selector-chain.inline-chains`.

## `selector-chain.in-for`

**Default:** `never-break`.

How a chain breaks in the enumerators of a `for`: the generators (`x <- xs`), the guards (`if ...`) and the value definitions (`y = ...`), and everything they hold. The expression after `yield` and the body after `do` are not enumerators.

A chain there never breaks at `selector-chain.break-at-count` selections, and it always breaks when it does not fit.

Before:

```scala
def names =
  for
    user <- users
      .filter(_.active)
  yield user.name
```

### `selector-chain.in-for = never-break`

A chain the author broke is joined while it fits:

```scala
def names =
  for
    user <- users.filter(_.active)
  yield user.name

// With settings:
//   max-column = 80
//   selector-chain.in-for = never-break
```

### `selector-chain.in-for = keep`

A chain stays broken where the author broke it:

```scala
def names =
  for
    user <- users
              .filter(_.active)
  yield user.name

// With settings:
//   max-column = 80
//   selector-chain.in-for = keep
```

## `selector-chain.in-call`

**Default:** `never-break`.

How a chain breaks in the arguments of a call: the arguments of a call or of a constructor, positional or named, the elements of a tuple, the arguments of an infix application with several of them (`x shouldBe (a, b)`), and everything they hold. The bodies of the lambdas and of the blocks given as arguments are not arguments: a chain there breaks like in any other body.

A chain in an argument never breaks at `selector-chain.break-at-count` selections, and it always breaks when it does not fit.

Before:

```scala
val s = SignatureParts(
  nameEnd = d
    .name
    .pos
    .end,
)
```

### `selector-chain.in-call = never-break`

A chain the author broke is joined while it fits:

```scala
val s =
  SignatureParts(
    nameEnd = d.name.pos.end,
  )

// With settings:
//   max-column = 80
//   selector-chain.in-call = never-break
```

### `selector-chain.in-call = keep`

A chain stays broken where the author broke it:

```scala
val s =
  SignatureParts(
    nameEnd = d
      .name
      .pos
      .end,
  )

// With settings:
//   max-column = 80
//   selector-chain.in-call = keep
```

## `selector-chain.namespace-path`

**Default:** `keep`.

Whether a package path at the start of a chain stays together as its receiver. A package path is the run of lowercase names up to the first capitalised one: `java.nio.file.Files`.

Before:

```scala
val a = java.nio.file.Files
  .exists(path)
  .toString
  
val b = java.nio
  .file.Files
  .exists(path)
  .toString
```

### `selector-chain.namespace-path = always-join`

Always, even when the author broke the line inside the path:

```scala
val a =
  java.nio.file.Files
    .exists(path)
    .toString

val b =
  java.nio.file.Files
    .exists(path)
    .toString

// With settings:
//   max-column = 80
//   selector-chain.namespace-path = always-join
```

### `selector-chain.namespace-path = keep`

As `always-join`, unless the author broke the line inside the path. The first chain joins its path, and the second, broken inside the path, breaks like any other selection:

```scala
val a =
  java.nio.file.Files
    .exists(path)
    .toString

val b =
  java
    .nio
    .file
    .Files
    .exists(path)
    .toString

// With settings:
//   max-column = 80
//   selector-chain.namespace-path = keep
```

### `selector-chain.namespace-path = always-break`

The path breaks like any other selection:

```scala
val a =
  java
    .nio
    .file
    .Files
    .exists(path)
    .toString

val b =
  java
    .nio
    .file
    .Files
    .exists(path)
    .toString

// With settings:
//   max-column = 80
//   selector-chain.namespace-path = always-break
```

## `selector-chain.object-member`

**Default:** `always-break`.

Whether the first member selected on a capitalised name stays with it as the receiver.

Before:

```scala
val a = IO.pure(x)
  .map(f)
  .flatMap(g)
  
val b = IO
  .pure(x)
  .map(f)
```

### `selector-chain.object-member = always-join`

Always, even when the author broke the line before its dot:

```scala
val a =
  IO.pure(x)
    .map(f)
    .flatMap(g)

val b =
  IO.pure(x)
    .map(f)

// With settings:
//   max-column = 80
//   selector-chain.object-member = always-join
```

### `selector-chain.object-member = keep`

As `always-join`, unless the author broke the line before its dot. The first chain joins its member, and the second, broken before it, breaks like any other selection:

```scala
val a =
  IO.pure(x)
    .map(f)
    .flatMap(g)

val b =
  IO
    .pure(x)
    .map(f)

// With settings:
//   max-column = 80
//   selector-chain.object-member = keep
```

### `selector-chain.object-member = always-break`

The member breaks like any other selection:

```scala
val a =
  IO
    .pure(x)
    .map(f)
    .flatMap(g)

val b =
  IO
    .pure(x)
    .map(f)

// With settings:
//   max-column = 80
//   selector-chain.object-member = always-break
```

## `selector-chain.receiver-min-length`

**Default:** `1`.

The shortest receiver name a chain breaks after. The value is at least 1. A shorter receiver keeps its first selection on its line, and that selection still counts towards `selector-chain.break-at-count`.

Before:

```scala
val a = v.trim.toLowerCase.nonEmpty
val b = values.map(_.trim).filter(_.nonEmpty).size
```

### `selector-chain.receiver-min-length = 1`

Every receiver breaks after its name:

```scala
val a =
  v
    .trim
    .toLowerCase
    .nonEmpty
val b =
  values
    .map(_.trim)
    .filter(_.nonEmpty)
    .size

// With settings:
//   max-column = 80
//   selector-chain.receiver-min-length = 1
```

### `selector-chain.receiver-min-length = 3`

`v` keeps its first selection on its line, and `values` breaks:

```scala
val a =
  v.trim
    .toLowerCase
    .nonEmpty
val b =
  values
    .map(_.trim)
    .filter(_.nonEmpty)
    .size

// With settings:
//   max-column = 80
//   selector-chain.receiver-min-length = 3
```
