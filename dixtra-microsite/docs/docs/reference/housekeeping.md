---
layout: docs
title: Housekeeping
---

# Housekeeping

The housekeeping of a whole file, applied after the other formatting.

These settings change bytes that a code block cannot show: the spaces at the end of a line, the line break at the end of the file, the line break between the lines. They are described here rather than shown.

## `housekeeping.trim-trailing-whitespace`

**Default:** `true`.

Whether the spaces and the tabs at the end of each line are removed. The spaces and tabs inside a string literal always stay.

- `housekeeping.trim-trailing-whitespace = true` — removed.
- `housekeeping.trim-trailing-whitespace = false` — kept, though the layout of comments still trims the end of a comment.

## `housekeeping.enforce-eof`

**Default:** `true`.

Whether the file ends with exactly one line break (standard Unix EOF).

- `housekeeping.enforce-eof = true` — a missing line break at the end of the file is added, and the blank lines at its end are removed.
- `housekeeping.enforce-eof = false` — the end of the file stays as written.

## `housekeeping.line-ending`

**Default:** `keep`.

The line break written at the end of each line, string literals over several lines included.

- `housekeeping.line-ending = lf` — `\n`, as on Linux and macOS.
- `housekeeping.line-ending = crlf` — `\r\n`, as on Windows.
- `housekeeping.line-ending = cr` — `\r`, as on classic macOS (up to Mac OS 9).
- `housekeeping.line-ending = keep` — the line break of the file as it is written.
