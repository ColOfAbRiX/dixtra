
# Overflow

What happens to what goes past `max-column`, when breaking the construct is not the answer.

## `overflow.trailing-comment`

**Default:** `keep`.

What happens to a line that goes past `max-column` only because of the comment at its end.

A trailing comment never counts in the width, so the code before it never breaks because of it.

Before:

```scala
val total = pricesAndDiscounts.sum // the total of every price after the discount
```

### `overflow.trailing-comment = keep`

The line stays longer than `max-column`:

```scala
val total = pricesAndDiscounts.sum // the total of every price after the discount

// With settings:
//   max-column = 80
//   overflow.trailing-comment = keep
```

### `overflow.trailing-comment = move-above`

The comment moves to a line of its own above the code, at its indent, wrapped if it is too long:

```scala
// the total of every price after the discount
val total = pricesAndDiscounts.sum

// With settings:
//   max-column = 80
//   overflow.trailing-comment = move-above
```
