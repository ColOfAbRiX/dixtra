
# Definition sites

The layout of the names where they are declared: the parameters and the type parameters of `def`s and of class constructors, and the selectors of an `import` or of an `export`.

A signature that does not fit breaks one parameter per line, and every group of parameters breaks with it.

## `definition-site.trailing-commas`

**Default:** `always`.

Whether the last parameter, type parameter or import selector of a list laid out one per line is followed by a comma. A list on one line never has one.

Before:

```scala
def f(firstArgument: String, secondArgument: Boolean, thirdArgument: Option[Int]): Int = 1
```

### `definition-site.trailing-commas = always`

The last parameter gets its comma:

```scala
def f(
  firstArgument: String,
  secondArgument: Boolean,
  thirdArgument: Option[Int],
): Int = 1

// With settings:
//   max-column = 80
//   definition-site.trailing-commas = always
```

### `definition-site.trailing-commas = never`

The last parameter stays without its comma, as written:

```scala
def f(
  firstArgument: String,
  secondArgument: Boolean,
  thirdArgument: Option[Int]
): Int = 1

// With settings:
//   max-column = 80
//   definition-site.trailing-commas = never
```

## `definition-site.groups-separator`

**Default:** `next-group`.

Where `)(` goes between the groups of parameters of a broken signature.

Before:

```scala
def f[A](firstArgument: String, secondArgument: Boolean)(numeric: Numeric[A])(using ord: Ordering[A]): Int = 1
```

### `definition-site.groups-separator = next-group`

`)(` goes at the start of the next group, on the line of the closing `)`:

```scala
def f[A](
  firstArgument: String,
  secondArgument: Boolean,
)(numeric: Numeric[A],
)(using
  ord: Ordering[A],
): Int = 1

// With settings:
//   max-column = 80
//   definition-site.groups-separator = next-group
```

### `definition-site.groups-separator = previous-group`

`)(` goes at the end of the last parameter of the group before, with the parameters of the next group below it:

```scala
def f[A](
  firstArgument: String,
  secondArgument: Boolean)(
  numeric: Numeric[A])(
  using
  ord: Ordering[A],
): Int = 1

// With settings:
//   max-column = 80
//   definition-site.groups-separator = previous-group
```

A `using` or an `implicit` keyword is placed like a parameter: on a line of its own when its group opens at the end of a line, and on the `)(` line with `definition-site.groups-separator = next-group`.
