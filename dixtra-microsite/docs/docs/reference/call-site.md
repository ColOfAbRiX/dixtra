---
layout: docs
title: Call sites
---

# Call sites

The layout of the arguments where they are passed: calls, constructor calls and tuples. A trailing `{ ... }` argument (`foldLeft(z) { (acc, x) => ... }`) is not a group, and follows its own rules. Every setting is in [the settings table](#the-settings).

## `trailing-commas`

Whether the last argument, or the last element of a tuple, of a list laid out one per line is followed by a comma. A list on one line never has one. Default `always`.

`always`:

```scala
compute(
  firstArgument,
  secondArgument,
)
```

`never`:

```scala
compute(
  firstArgument,
  secondArgument
)
```

## `groups-separator`

Where `)(` goes between the groups of arguments of a broken call. Default `previous-group`.

`previous-group` — after the last argument of the group before, with the arguments of the next group below it:

```scala
Resource.make(
  IO.blocking(Files.createTempFile(dir, prefix, ".tmp")))(
  temp => IO.blocking(Files.delete(temp)).void,
)
```

`next-group` — at the start of the next group, on the line of the closing `)`:

```scala
Resource.make(
  IO.blocking(Files.createTempFile(dir, prefix, ".tmp")),
)(temp => IO.blocking(Files.delete(temp)).void,
)
```

## `groups-break`

Which groups of arguments break when the groups of a call do not fit on one line. Default `last-first`.

`break-all` — every group, each argument on its own line (see `groups-separator` above for the shape).

`last-first` — only the last group, while the groups before it fit on the line of the call; every group when they do not:

```scala
Resource.make(IO.blocking(Files.createTempFile(dir, prefix, ".tmp")))(
  temp => IO.blocking(Files.delete(temp)).void,
)
```

`independent` — each group by itself, as in a call with one group: a group breaks when the author broke it or it does not fit, and the others stay as they are:

```scala
Resource.make(
  acquire,
)(release)
```

## The settings

| Setting            | Default          | Values                                   |
| ------------------ | ---------------- | ---------------------------------------- |
| `trailing-commas`  | `always`         | `always`, `never`                        |
| `groups-separator` | `previous-group` | `previous-group`, `next-group`           |
| `groups-break`     | `last-first`     | `break-all`, `last-first`, `independent` |
