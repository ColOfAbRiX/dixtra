---
layout: docs
title: Housekeeping
---

# Housekeeping

The housekeeping of a whole file, applied after the other formatting. Every setting is in [the settings table](#the-settings).

## `trim-trailing-whitespace`

Whether the spaces and the tabs at the end of each line are removed. Default `true`.

`true` — removed:

```scala
val a = 1
```

`false` — kept, though the layout of comments still trims the end of a comment.

## `enforce-eof`

Whether the file ends with exactly one line break (standard Unix EOF). Default `true`.

`true` — a missing line break at the end of the file is added, and the blank lines at its end are removed.

`false` — the end of the file stays as written.

## `line-ending`

The line break written at the end of each line, string literals over several lines included. Default `keep`.

- `lf` — `\n`, as on Linux and macOS.
- `crlf` — `\r\n`, as on Windows.
- `cr` — `\r`, as on classic macOS (up to Mac OS 9).
- `keep` — the line break of the file as it is written.

## The settings

| Setting                    | Default | Values                     |
| -------------------------- | ------- | -------------------------- |
| `trim-trailing-whitespace` | `true`  | `true`, `false`            |
| `enforce-eof`              | `true`  | `true`, `false`            |
| `line-ending`              | `keep`  | `lf`, `crlf`, `cr`, `keep` |
