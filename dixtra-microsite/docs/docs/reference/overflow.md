---
layout: docs
title: Overflow
---

# Overflow

What happens to what goes past `max-column`, when breaking the construct is not the answer.

## `trailing-comment`

What happens to a line that goes past `max-column` only because of the comment at its end. Default `keep`.

`keep` — the line stays longer than `max-column`:

```scala
val total = prices.sum // a comment that does not fit
```

`move-above` — the comment moves to a line of its own above the code, at its indent, wrapped if it is too long:

```scala
// a comment that does not fit
val total = prices.sum
```

A trailing comment never counts in the width, so the code before it never breaks because of it.

## The settings

| Setting | Default | Values |
| --- | --- | --- |
| `trailing-comment` | `keep` | `keep`, `move-above` |
