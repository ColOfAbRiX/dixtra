---
layout: docs
title: Rules and limitations
---

# Rules and limitations

## Rules that have no setting

Some rules are fixed, on purpose: they are what makes the result consistent.

- **Indentation** is 2 spaces.
- **Spaces.** A run of spaces between two tokens on a line becomes one, except where Dixtra aligns code.
- **Blank lines.** A run of blank lines becomes one.
- **Comments** on lines of their own are reflowed to `max-column`.
- **Scaladoc** gets the javadoc layout.

When Dixtra has no rule for a specific code snippet, the code is copied exactly as written.

## Known limitations

- Among the Scala 3 syntax it does not parse yet is `into` on a type (Scala 3.9).

## Ignoring the formatting

The code between a `dixtra: off` comment and a `dixtra: on` comment stays exactly as written:

```scala
object Matrices {
  val a = 1

  // dixtra: off
  val identity = Array(
    1, 0,
    0, 1,
  )
  // dixtra: on
}
```

- **Unclosed regions.** Without a `dixtra: on`, the region runs to the end of the file. A `// dixtra: off` at the top of a file leaves the whole file as written.
- **Nesting.** An `off` inside a region, or an `on` outside one, does nothing.
- **The markers themselves** are laid out like any other comment. Only the code between them is protected.
