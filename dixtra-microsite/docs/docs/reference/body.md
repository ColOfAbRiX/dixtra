---
layout: docs
title: Bodies
---

# Bodies

Where each body goes relative to the separator before it: on its line, or below it one level in. The separators are the `=` of a definition, the `=>` of a lambda or of a case, the `then` and the `else` of an if/else, the `do` of a loop.

A construct with a single body (a definition, a lambda, a loop) takes `keep`, `always-join` or `always-break`. The bodies of a construct with several of them (the branches of an if/else, the cases of a match, the bodies of a try) are one group, never one by one: either every body is on the line of its separator, or every body is below it, and it takes `join-mixed`, `break-mixed`, `always-join` or `always-break`. A group where some bodies are inline and some others are on a new line is _mixed_.

Whatever the setting: a body goes below its separator when it cannot be joined on its line, and in a group such a body puts every body of the group below its separator. The settings move bodies only relative to their separators: an `else` written on its own line stays on its own line, and every `case` is on its own line. With braces (see [braces](braces.md)), a body below its separator is in braces, and a construct whose bodies are on their lines has none.

## `body.def`

**Default:** `keep`.

The `=` of a `def`.

Before:

```scala
def total = xs.sum
def average =
  xs.sum / xs.size
```

### `body.def = keep`

The body stays where the author wrote it.

```scala
def total = xs.sum

def average =
  xs.sum / xs.size

// With settings:
//   max-column = 80
//   body.def = keep
```

### `body.def = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
def total   = xs.sum
def average = xs.sum / xs.size

// With settings:
//   max-column = 80
//   body.def = always-join
```

### `body.def = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
def total =
  xs.sum

def average =
  xs.sum / xs.size

// With settings:
//   max-column = 80
//   body.def = always-break
```

## `body.val`

**Default:** `keep`.

The `=` of a `val`.

Before:

```scala
val total = xs.sum
val average =
  xs.sum / xs.size
```

### `body.val = keep`

The body stays where the author wrote it.

```scala
val total = xs.sum
val average =
  xs.sum / xs.size

// With settings:
//   max-column = 80
//   body.val = keep
```

### `body.val = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
val total   = xs.sum
val average = xs.sum / xs.size

// With settings:
//   max-column = 80
//   body.val = always-join
```

### `body.val = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
val total =
  xs.sum
val average =
  xs.sum / xs.size

// With settings:
//   max-column = 80
//   body.val = always-break
```

## `body.lazy-val`

**Default:** `keep`.

The `=` of a `lazy val`.

Before:

```scala
lazy val total = xs.sum
lazy val average =
  xs.sum / xs.size
```

### `body.lazy-val = keep`

The body stays where the author wrote it.

```scala
lazy val total = xs.sum
lazy val average =
  xs.sum / xs.size

// With settings:
//   max-column = 80
//   body.lazy-val = keep
```

### `body.lazy-val = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
lazy val total   = xs.sum
lazy val average = xs.sum / xs.size

// With settings:
//   max-column = 80
//   body.lazy-val = always-join
```

### `body.lazy-val = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
lazy val total =
  xs.sum
lazy val average =
  xs.sum / xs.size

// With settings:
//   max-column = 80
//   body.lazy-val = always-break
```

## `body.var`

**Default:** `keep`.

The `=` of a `var`.

Before:

```scala
var total = xs.sum
var average =
  xs.sum / xs.size
```

### `body.var = keep`

The body stays where the author wrote it.

```scala
var total = xs.sum
var average =
  xs.sum / xs.size

// With settings:
//   max-column = 80
//   body.var = keep
```

### `body.var = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
var total   = xs.sum
var average = xs.sum / xs.size

// With settings:
//   max-column = 80
//   body.var = always-join
```

### `body.var = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
var total =
  xs.sum
var average =
  xs.sum / xs.size

// With settings:
//   max-column = 80
//   body.var = always-break
```

## `body.given-alias`

**Default:** `keep`.

The `=` of a given alias.

Before:

```scala
given intOrder: Ordering[Int] = Ordering.Int
given stringOrder: Ordering[String] =
  Ordering.String
```

### `body.given-alias = keep`

The body stays where the author wrote it.

```scala
given intOrder: Ordering[Int] = Ordering.Int

given stringOrder: Ordering[String] =
  Ordering.String

// With settings:
//   max-column = 80
//   body.given-alias = keep
```

### `body.given-alias = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
given intOrder: Ordering[Int] = Ordering.Int

given stringOrder: Ordering[String] = Ordering.String

// With settings:
//   max-column = 80
//   body.given-alias = always-join
```

### `body.given-alias = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
given intOrder: Ordering[Int] =
  Ordering.Int

given stringOrder: Ordering[String] =
  Ordering.String

// With settings:
//   max-column = 80
//   body.given-alias = always-break
```

## `body.type-def`

**Default:** `keep`.

The `=` of a type definition.

Before:

```scala
type A = Int
type B =
  List[Int]
```

### `body.type-def = keep`

The body stays where the author wrote it.

```scala
type A = Int
type B =
  List[Int]

// With settings:
//   max-column = 80
//   body.type-def = keep
```

### `body.type-def = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
type A = Int
type B = List[Int]

// With settings:
//   max-column = 80
//   body.type-def = always-join
```

### `body.type-def = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
type A =
  Int
type B =
  List[Int]

// With settings:
//   max-column = 80
//   body.type-def = always-break
```

## `body.assignment`

**Default:** `keep`.

The `=` of an assignment.

Before:

```scala
def reset(): Unit =
  total = 0
  count =
    1
```

### `body.assignment = keep`

The body stays where the author wrote it.

```scala
def reset(): Unit =
  total = 0
  count =
    1

// With settings:
//   max-column = 80
//   body.assignment = keep
```

### `body.assignment = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
def reset(): Unit =
  total = 0
  count = 1

// With settings:
//   max-column = 80
//   body.assignment = always-join
```

### `body.assignment = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
def reset(): Unit =
  total =
    0
  count =
    1

// With settings:
//   max-column = 80
//   body.assignment = always-break
```

## `body.named-argument`

**Default:** `keep`.

The `=` of a named argument.

Before:

```scala
val c = Config(
  name = "dixtra",
  width =
    80,
)
```

### `body.named-argument = keep`

The body stays where the author wrote it.

```scala
val c =
  Config(
    name = "dixtra",
    width =
      80,
  )

// With settings:
//   max-column = 80
//   body.named-argument = keep
```

### `body.named-argument = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
val c =
  Config(
    name = "dixtra",
    width = 80,
  )

// With settings:
//   max-column = 80
//   body.named-argument = always-join
```

### `body.named-argument = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
val c =
  Config(
    name =
      "dixtra",
    width =
      80,
  )

// With settings:
//   max-column = 80
//   body.named-argument = always-break
```

## `body.lambda-argument`

**Default:** `keep`.

The `=>` of a lambda given in braces or after a colon.

Before:

```scala
def run(): Unit =
  xs.foreach { x => println(x) }
  xs.foreach { x =>
    println(x)
  }
```

### `body.lambda-argument = keep`

The body stays where the author wrote it.

```scala
def run(): Unit =
  xs.foreach { x => println(x) }
  xs.foreach { x =>
    println(x)
  }

// With settings:
//   max-column = 80
//   body.lambda-argument = keep
```

### `body.lambda-argument = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
def run(): Unit =
  xs.foreach { x => println(x) }
  xs.foreach { x => println(x) }

// With settings:
//   max-column = 80
//   body.lambda-argument = always-join
```

### `body.lambda-argument = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
def run(): Unit =
  xs.foreach { x =>
    println(x)
  }
  xs.foreach { x =>
    println(x)
  }

// With settings:
//   max-column = 80
//   body.lambda-argument = always-break
```

## `body.lambda-body`

**Default:** `keep`.

The `=>` of any other lambda.

Before:

```scala
val f = (x: Int) => x + 1
val g = (x: Int) =>
  x + 2
```

### `body.lambda-body = keep`

The body stays where the author wrote it.

```scala
val f = (x: Int) => x + 1
val g =
  (x: Int) =>
    x + 2

// With settings:
//   max-column = 80
//   body.lambda-body = keep
```

### `body.lambda-body = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
val f = (x: Int) => x + 1
val g = (x: Int) => x + 2

// With settings:
//   max-column = 80
//   body.lambda-body = always-join
```

### `body.lambda-body = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
val f =
  (x: Int) =>
    x + 1
val g =
  (x: Int) =>
    x + 2

// With settings:
//   max-column = 80
//   body.lambda-body = always-break
```

## `body.block-argument`

**Default:** `keep`.

The `{` or the colon of a block of statements given as an argument, without lambda parameters and not a test block.

Before:

```scala
def run(): Unit =
  Resource.make { open() }
  Resource.make {
    close()
  }
```

### `body.block-argument = keep`

The body stays where the author wrote it.

```scala
def run(): Unit =
  Resource.make { open() }
  Resource.make {
    close()
  }

// With settings:
//   max-column = 80
//   body.block-argument = keep
```

### `body.block-argument = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
def run(): Unit =
  Resource.make { open() }
  Resource.make { close() }

// With settings:
//   max-column = 80
//   body.block-argument = always-join
```

### `body.block-argument = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
def run(): Unit =
  Resource.make {
    open()
  }
  Resource.make {
    close()
  }

// With settings:
//   max-column = 80
//   body.block-argument = always-break
```

## `body.test-case`

**Default:** `keep`.

The `{` or the colon of a test case: a block of statements given to a call labelled by a string, holding no other test block.

Before:

```scala
def suite(): Unit =
  test("a parser") { parse("x") }
  test("a printer") {
    print("x")
  }
```

### `body.test-case = keep`

The body stays where the author wrote it.

```scala
def suite(): Unit =
  test("a parser") { parse("x") }
  test("a printer") {
    print("x")
  }

// With settings:
//   max-column = 80
//   body.test-case = keep
```

### `body.test-case = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
def suite(): Unit =
  test("a parser") { parse("x") }
  test("a printer") { print("x") }

// With settings:
//   max-column = 80
//   body.test-case = always-join
```

### `body.test-case = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
def suite(): Unit =
  test("a parser") {
    parse("x")
  }
  test("a printer") {
    print("x")
  }

// With settings:
//   max-column = 80
//   body.test-case = always-break
```

## `body.test-group`

**Default:** `keep`.

The `{` or the colon of a group of tests: a block of statements given to a call labelled by a string, holding other test blocks.

Before:

```scala
def suite(): Unit =
  test("group one") { test("a") { parse("x") } }
  test("group two") {
    test("b") { parse("x") }
  }
```

### `body.test-group = keep`

The body stays where the author wrote it.

```scala
def suite(): Unit =
  test("group one") { test("a") { parse("x") } }
  test("group two") {
    test("b") { parse("x") }
  }

// With settings:
//   max-column = 80
//   body.test-group = keep
```

### `body.test-group = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
def suite(): Unit =
  test("group one") { test("a") { parse("x") } }
  test("group two") { test("b") { parse("x") } }

// With settings:
//   max-column = 80
//   body.test-group = always-join
```

### `body.test-group = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
def suite(): Unit =
  test("group one") {
    test("a") { parse("x") }
  }
  test("group two") {
    test("b") { parse("x") }
  }

// With settings:
//   max-column = 80
//   body.test-group = always-break
```

## `body.if`

**Default:** `break-mixed`.

The `then` and the `else` of an if/else. The branches of an `else if` chain are one group with those of the outer if/else.

Before:

```scala
def f: Int =
  if c then
    a
  else b

def g: Int =
  if d then
    e
  else
    f

def h: Int = if i then j else k
```

### `body.if = join-mixed`

A mixed group is joined on the lines of the separators; a group all on its lines, or all below, stays as it is.

```scala
def f: Int =
  if c then a
  else b

def g: Int =
  if d then
    e
  else
    f

def h: Int = if i then j else k

// With settings:
//   max-column = 80
//   body.if = join-mixed
```

### `body.if = always-join`

Every body goes on the line of its separator.

```scala
def f: Int =
  if c then a
  else b

def g: Int =
  if d then e
  else f

def h: Int = if i then j else k

// With settings:
//   max-column = 80
//   body.if = always-join
```

### `body.if = break-mixed`

A mixed group goes below the separators; a group all on its lines, or all below, stays as it is.

```scala
def f: Int =
  if c then
    a
  else
    b

def g: Int =
  if d then
    e
  else
    f

def h: Int = if i then j else k

// With settings:
//   max-column = 80
//   body.if = break-mixed
```

### `body.if = always-break`

Every body goes below its separator.

```scala
def f: Int =
  if c then
    a
  else
    b

def g: Int =
  if d then
    e
  else
    f

def h: Int =
  if i then
    j
  else
    k

// With settings:
//   max-column = 80
//   body.if = always-break
```

## `body.case-body`

**Default:** `break-mixed`.

The `=>` of the cases of a `match`, of a `{ case ... }` or of a `catch`, which are one group.

Before:

```scala
def f(x: Int): String = x match {
    case 1 =>
      "one"
    case 22 => "two"
  }

def g(y: Int): String = y match {
    case 1 =>
      "one"
    case 2 =>
      "two"
  }

def h(z: Int): String = z match {
    case 1 => "one"
    case 2 => "two"
  }
```

### `body.case-body = join-mixed`

A mixed group is joined on the lines of the separators; a group all on its lines, or all below, stays as it is.

```scala
def f(x: Int): String =
  x match {
    case 1  => "one"
    case 22 => "two"
  }

def g(y: Int): String =
  y match {
    case 1 =>
      "one"
    case 2 =>
      "two"
  }

def h(z: Int): String =
  z match {
    case 1 => "one"
    case 2 => "two"
  }

// With settings:
//   max-column = 80
//   body.case-body = join-mixed
```

### `body.case-body = always-join`

Every body goes on the line of its separator.

```scala
def f(x: Int): String =
  x match {
    case 1  => "one"
    case 22 => "two"
  }

def g(y: Int): String =
  y match {
    case 1 => "one"
    case 2 => "two"
  }

def h(z: Int): String =
  z match {
    case 1 => "one"
    case 2 => "two"
  }

// With settings:
//   max-column = 80
//   body.case-body = always-join
```

### `body.case-body = break-mixed`

A mixed group goes below the separators; a group all on its lines, or all below, stays as it is.

```scala
def f(x: Int): String =
  x match {
    case 1 =>
      "one"
    case 22 =>
      "two"
  }

def g(y: Int): String =
  y match {
    case 1 =>
      "one"
    case 2 =>
      "two"
  }

def h(z: Int): String =
  z match {
    case 1 => "one"
    case 2 => "two"
  }

// With settings:
//   max-column = 80
//   body.case-body = break-mixed
```

### `body.case-body = always-break`

Every body goes below its separator.

```scala
def f(x: Int): String =
  x match {
    case 1 =>
      "one"
    case 22 =>
      "two"
  }

def g(y: Int): String =
  y match {
    case 1 =>
      "one"
    case 2 =>
      "two"
  }

def h(z: Int): String =
  z match {
    case 1 =>
      "one"
    case 2 =>
      "two"
  }

// With settings:
//   max-column = 80
//   body.case-body = always-break
```

## `body.try`

**Default:** `break-mixed`.

The bodies after the `try` and the `finally`, which are one group; the cases after `catch` follow `body.case-body`.

Before:

```scala
def f(): Unit =
  try
    a()
  finally b()

def g(): Unit =
  try
    c()
  finally
    d()

def h(): Unit =
  try a()
  finally b()
```

### `body.try = join-mixed`

A mixed group is joined on the lines of the separators; a group all on its lines, or all below, stays as it is.

```scala
def f(): Unit =
  try a()
  finally b()

def g(): Unit =
  try
    c()
  finally
    d()

def h(): Unit =
  try a()
  finally b()

// With settings:
//   max-column = 80
//   braces.try = force-indent
//   body.try = join-mixed
```

### `body.try = always-join`

Every body goes on the line of its separator.

```scala
def f(): Unit =
  try a()
  finally b()

def g(): Unit =
  try c()
  finally d()

def h(): Unit =
  try a()
  finally b()

// With settings:
//   max-column = 80
//   braces.try = force-indent
//   body.try = always-join
```

### `body.try = break-mixed`

A mixed group goes below the separators; a group all on its lines, or all below, stays as it is.

```scala
def f(): Unit =
  try
    a()
  finally
    b()

def g(): Unit =
  try
    c()
  finally
    d()

def h(): Unit =
  try a()
  finally b()

// With settings:
//   max-column = 80
//   braces.try = force-indent
//   body.try = break-mixed
```

### `body.try = always-break`

Every body goes below its separator.

```scala
def f(): Unit =
  try
    a()
  finally
    b()

def g(): Unit =
  try
    c()
  finally
    d()

def h(): Unit =
  try
    a()
  finally
    b()

// With settings:
//   max-column = 80
//   braces.try = force-indent
//   body.try = always-break
```

## `body.for`

**Default:** `keep`.

The `do` of a `for` loop.

Before:

```scala
def f(): Unit =
  for x <- xs do println(x)
  for y <- ys do
    println(y)
```

### `body.for = keep`

The body stays where the author wrote it.

```scala
def f(): Unit =
  for x <- xs do println(x)
  for y <- ys do
    println(y)

// With settings:
//   max-column = 80
//   body.for = keep
```

### `body.for = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
def f(): Unit =
  for x <- xs do println(x)
  for y <- ys do println(y)

// With settings:
//   max-column = 80
//   body.for = always-join
```

### `body.for = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
def f(): Unit =
  for x <- xs do
    println(x)
  for y <- ys do
    println(y)

// With settings:
//   max-column = 80
//   body.for = always-break
```

## `body.for-yield`

**Default:** `break-complex`.

The expression after the `yield` of a for-comprehension.

Before:

```scala
def a =
  for x <- xs
  yield
    name

def b =
  for x <- xs
  yield x + 1
```

### `body.for-yield = keep`

The expression stays where the author wrote it.

```scala
def a =
  for
    x <- xs
  yield
    name

def b =
  for
    x <- xs
  yield x + 1

// With settings:
//   max-column = 80
//   body.for-yield = keep
```

### `body.for-yield = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
def a =
  for
    x <- xs
  yield name

def b =
  for
    x <- xs
  yield x + 1

// With settings:
//   max-column = 80
//   body.for-yield = always-join
```

### `body.for-yield = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
def a =
  for
    x <- xs
  yield
    name

def b =
  for
    x <- xs
  yield
    x + 1

// With settings:
//   max-column = 80
//   body.for-yield = always-break
```

### `body.for-yield = break-complex`

As `always-break`, but a simple expression, a single name, a path or a literal stays on the line of the `yield`.

```scala
def a =
  for
    x <- xs
  yield name

def b =
  for
    x <- xs
  yield
    x + 1

// With settings:
//   max-column = 80
//   body.for-yield = break-complex
```

## `body.while`

**Default:** `keep`.

The `do` of a `while` loop.

Before:

```scala
def f(): Unit =
  while c do step()
  while d do
    other()
```

### `body.while = keep`

The body stays where the author wrote it.

```scala
def f(): Unit =
  while (c) step()
  while (d) {
    other()
  }

// With settings:
//   max-column = 80
//   body.while = keep
```

### `body.while = always-join`

The body goes on the line of its separator, even when the author wrote it below.

```scala
def f(): Unit =
  while (c) step()
  while (d) other()

// With settings:
//   max-column = 80
//   body.while = always-join
```

### `body.while = always-break`

The body goes below its separator, even when the author wrote it on the line.

```scala
def f(): Unit =
  while (c) {
    step()
  }
  while (d) {
    other()
  }

// With settings:
//   max-column = 80
//   body.while = always-break
```

## `body.semicolon-statements`

**Default:** `always-break`.

The `;` between the statements of a body written on one line, whatever the construct.

Before:

```scala
def run(): Unit = { println(1); println(2) }
```

### `body.semicolon-statements = keep`

The statements stay on one line, with their braces, while it fits.

```scala
def run(): Unit = { println(1); println(2) }

// With settings:
//   max-column = 80
//   body.semicolon-statements = keep
```

### `body.semicolon-statements = always-break`

One statement per line, below the `=`.

```scala
def run(): Unit =
  println(1)
  println(2)

// With settings:
//   max-column = 80
//   body.semicolon-statements = always-break
```
