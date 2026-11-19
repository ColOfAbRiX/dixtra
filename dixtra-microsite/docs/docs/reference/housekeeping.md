---
layout: docs
title: Housekeeping
---

# Housekeeping

The housekeeping of a whole file, applied after the other formatting. Every setting is in [the settings table](#the-settings).

## `housekeeping.trim-trailing-whitespace`

Whether the spaces and the tabs at the end of each line are removed. Default `housekeeping.trim-trailing-whitespace = true`.

`housekeeping.trim-trailing-whitespace = true` — removed:

```scala
val a = 1
```

`housekeeping.trim-trailing-whitespace = false` — kept, though the layout of comments still trims the end of a comment.

## `housekeeping.enforce-eof`

Whether the file ends with exactly one line break (standard Unix EOF). Default `housekeeping.enforce-eof = true`.

`housekeeping.enforce-eof = true` — a missing line break at the end of the file is added, and the blank lines at its end are removed.

`housekeeping.enforce-eof = false` — the end of the file stays as written.

## `housekeeping.line-ending`

The line break written at the end of each line, string literals over several lines included. Default `housekeeping.line-ending = keep`.

- `housekeeping.line-ending = lf` — `\n`, as on Linux and macOS.
- `housekeeping.line-ending = crlf` — `\r\n`, as on Windows.
- `housekeeping.line-ending = cr` — `\r`, as on classic macOS (up to Mac OS 9).
- `housekeeping.line-ending = keep` — the line break of the file as it is written.

## The settings

| Setting                              | Default | Values                     |
| ------------------------------------ | ------- | -------------------------- |
| `housekeeping.trim-trailing-whitespace` | `true`  | `true`, `false`            |
| `housekeeping.enforce-eof`           | `true`  | `true`, `false`            |
| `housekeeping.line-ending`           | `keep`  | `lf`, `crlf`, `cr`, `keep` |
