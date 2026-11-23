
# Call sites

The layout of the arguments where they are passed: calls, constructor calls and tuples. 

A trailing `{ ... }` argument (`foldLeft(z) { (acc, x) => ... }`) is not a group, and follows its own rules.

## `call-site.trailing-commas`

**Default:** `always`.

Whether the last argument, or the last element of a tuple, of a list laid out one per line is followed by a comma. A list on one line never has one.

Before:

```scala
val r = compute(
  firstArgument,
  secondArgument
)
```

### `call-site.trailing-commas = always`

The last argument gets its comma:

```scala
val r =
  compute(
    firstArgument,
    secondArgument,
  )

// With settings:
//   max-column = 80
//   call-site.trailing-commas = always
```

### `call-site.trailing-commas = never`

The last argument stays without its comma, as written:

```scala
val r =
  compute(
    firstArgument,
    secondArgument
  )

// With settings:
//   max-column = 80
//   call-site.trailing-commas = never
```

## `call-site.groups-separator`

**Default:** `previous-group`.

Where `)(` goes between the groups of arguments of a broken call.

Before:

```scala
val r = Resource.make(
  acquireResource(config),
  releaseResource
)(release)
```

### `call-site.groups-separator = previous-group`

`)(` goes at the end of the last element of the group before, with the elements of the next group below it:

```scala
val r =
  Resource.make(
    acquireResource(config),
    releaseResource)(
    release,
  )

// With settings:
//   max-column = 80
//   call-site.groups-separator = previous-group
```

### `call-site.groups-separator = next-group`

`)(` goes at the start of the next group, on the line of the closing `)`:

```scala
val r =
  Resource.make(
    acquireResource(config),
    releaseResource,
  )(release,
  )

// With settings:
//   max-column = 80
//   call-site.groups-separator = next-group
```

## `call-site.groups-break`

**Default:** `last-first`.

Which groups of arguments break when the groups of a call do not fit on one line.

Before:

```scala
val e = Option.when(condition)(
  computeValue(argument)
)

val r = Resource.make(
  acquireResource(config),
  releaseResource
)(release)
```

### `call-site.groups-break = break-all`

Every group breaks, each argument on its own line. Even a first group that would fit breaks with the last one:

```scala
val e =
  Option.when(
    condition)(
    computeValue(argument),
  )

val r =
  Resource.make(
    acquireResource(config),
    releaseResource)(
    release,
  )

// With settings:
//   max-column = 80
//   call-site.groups-break = break-all
```

### `call-site.groups-break = last-first`

Only the last group breaks while the groups before it fit on the line of the call. When the author broke a leading group, every group breaks:

```scala
val e =
  Option.when(condition)(
    computeValue(argument),
  )

val r =
  Resource.make(
    acquireResource(config),
    releaseResource)(
    release,
  )

// With settings:
//   max-column = 80
//   call-site.groups-break = last-first
```

### `call-site.groups-break = independent`

Each group decides for itself, as in a call with one group: it breaks when the author broke it or it does not fit from where it starts. A leading group that fits stays inline, and a last group that fits stays inline even after a broken one:

```scala
val e =
  Option.when(condition)(
    computeValue(argument),
  )

val r =
  Resource.make(
    acquireResource(config),
    releaseResource,
  )(release)

// With settings:
//   max-column = 80
//   call-site.groups-break = independent
```
