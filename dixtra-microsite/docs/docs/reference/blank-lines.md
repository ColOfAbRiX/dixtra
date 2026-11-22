---
layout: docs
title: Blank lines
---

# Blank lines

Where blank lines go, beyond the ones the author wrote between statements.

Some rules have no setting: a run of blank lines always becomes one; a method after another statement always gets a blank line before it, and before its annotations and comments; the blank lines between the imports are always removed, so the groups of imports are not supported.

## The edges of the bodies

The edges are after the opening `{` (or after the header, without braces) and before the closing `}`; a body without braces has no closing edge. For `package`, the edge is the blank line after the package clause.

By default the containers and the test blocks keep the blank lines the author wrote, the package clause is followed by one, and the other bodies never have any. `blank-lines.body-edges.test-case` and `blank-lines.body-edges.test-group` tell the tests from the groups of tests, so a configuration can give a blank line after a group's `{` and none after a case's `{`.

A test case is a test block that holds no other test block; a group is one that holds them. See [bodies](body.md) for the difference.

## `blank-lines.body-edges.abstract-class`

**Default:** `keep`.

The blank lines at the edges of the body of An abstract class. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
abstract class One {
  def f: Int
  def g: Int
}

abstract class Two {

  def f: Int
  def g: Int

}
```

### `blank-lines.body-edges.abstract-class = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
abstract class One {
  def f: Int
  def g: Int
}

abstract class Two {

  def f: Int
  def g: Int

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.abstract-class = keep
```

### `blank-lines.body-edges.abstract-class = always`

A blank line at both edges.

```scala
abstract class One {

  def f: Int
  def g: Int

}

abstract class Two {

  def f: Int
  def g: Int

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.abstract-class = always
```

### `blank-lines.body-edges.abstract-class = never`

No blank line at either edge.

```scala
abstract class One {
  def f: Int
  def g: Int
}

abstract class Two {
  def f: Int
  def g: Int
}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.abstract-class = never
```

## `blank-lines.body-edges.block-argument`

**Default:** `never`.

The blank lines at the edges of the body of The body of a block given as an argument. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
def fOne(): Unit =
  Resource.make {
    open()
    more()
  }

def fTwo(): Unit =
  Resource.make {

    open()
    more()

  }
```

### `blank-lines.body-edges.block-argument = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
def fOne(): Unit =
  Resource.make {
    open()
    more()
  }

def fTwo(): Unit =
  Resource.make {

    open()
    more()

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.block-argument = keep
```

### `blank-lines.body-edges.block-argument = always`

A blank line at both edges.

```scala
def fOne(): Unit =
  Resource.make {

    open()
    more()

  }

def fTwo(): Unit =
  Resource.make {

    open()
    more()

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.block-argument = always
```

### `blank-lines.body-edges.block-argument = never`

No blank line at either edge.

```scala
def fOne(): Unit =
  Resource.make {
    open()
    more()
  }

def fTwo(): Unit =
  Resource.make {
    open()
    more()
  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.block-argument = never
```

## `blank-lines.body-edges.case-body`

**Default:** `never`.

The blank lines at the edges of the body of The body of a case. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
def fOne(x: Int) = x match {
    case 1 => {
      a()
      b()
    }
  }

def fTwo(x: Int) = x match {

    case 1 => {

      a()
      b()

    }

  }
```

### `blank-lines.body-edges.case-body = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
def fOne(x: Int) =
  x match {
    case 1 => {
      a()
      b()
    }
  }

def fTwo(x: Int) =
  x match {
    case 1 => {

      a()
      b()

    }
  }

// With settings:
//   max-column = 80
//   braces.case-body = force-braces
//   blank-lines.body-edges.case-body = keep
```

### `blank-lines.body-edges.case-body = always`

A blank line at both edges.

```scala
def fOne(x: Int) =
  x match {
    case 1 => {

      a()
      b()

    }
  }

def fTwo(x: Int) =
  x match {
    case 1 => {

      a()
      b()

    }
  }

// With settings:
//   max-column = 80
//   braces.case-body = force-braces
//   blank-lines.body-edges.case-body = always
```

### `blank-lines.body-edges.case-body = never`

No blank line at either edge.

```scala
def fOne(x: Int) =
  x match {
    case 1 => {
      a()
      b()
    }
  }

def fTwo(x: Int) =
  x match {
    case 1 => {
      a()
      b()
    }
  }

// With settings:
//   max-column = 80
//   braces.case-body = force-braces
//   blank-lines.body-edges.case-body = never
```

## `blank-lines.body-edges.case-class`

**Default:** `keep`.

The blank lines at the edges of the body of A case class. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
case class One(x: Int) {
  def f = x
  def g = x
}

case class Two(x: Int) {

  def f = x
  def g = x

}
```

### `blank-lines.body-edges.case-class = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
case class One(x: Int) {
  def f = x
  def g = x
}

case class Two(x: Int) {

  def f = x
  def g = x

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.case-class = keep
```

### `blank-lines.body-edges.case-class = always`

A blank line at both edges.

```scala
case class One(x: Int) {

  def f = x
  def g = x

}

case class Two(x: Int) {

  def f = x
  def g = x

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.case-class = always
```

### `blank-lines.body-edges.case-class = never`

No blank line at either edge.

```scala
case class One(x: Int) {
  def f = x
  def g = x
}

case class Two(x: Int) {
  def f = x
  def g = x
}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.case-class = never
```

## `blank-lines.body-edges.class`

**Default:** `keep`.

The blank lines at the edges of the body of A class. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
class One {
  def f = 1
  def g = 2
}

class Two {

  def f = 1
  def g = 2

}
```

### `blank-lines.body-edges.class = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
class One {
  def f = 1
  def g = 2
}

class Two {

  def f = 1
  def g = 2

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.class = keep
```

### `blank-lines.body-edges.class = always`

A blank line at both edges.

```scala
class One {

  def f = 1
  def g = 2

}

class Two {

  def f = 1
  def g = 2

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.class = always
```

### `blank-lines.body-edges.class = never`

No blank line at either edge.

```scala
class One {
  def f = 1
  def g = 2
}

class Two {
  def f = 1
  def g = 2
}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.class = never
```

## `blank-lines.body-edges.def`

**Default:** `never`.

The blank lines at the edges of the body of A `def`. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
def fOne = {
  val a = 1
  a + 1
}

def fTwo = {

  val a = 1
  a + 1

}
```

### `blank-lines.body-edges.def = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
def fOne = {
  val a = 1
  a + 1
}

def fTwo = {

  val a = 1
  a + 1

}

// With settings:
//   max-column = 80
//   braces.def = force-braces
//   blank-lines.body-edges.def = keep
```

### `blank-lines.body-edges.def = always`

A blank line at both edges.

```scala
def fOne = {

  val a = 1
  a + 1

}

def fTwo = {

  val a = 1
  a + 1

}

// With settings:
//   max-column = 80
//   braces.def = force-braces
//   blank-lines.body-edges.def = always
```

### `blank-lines.body-edges.def = never`

No blank line at either edge.

```scala
def fOne = {
  val a = 1
  a + 1
}

def fTwo = {
  val a = 1
  a + 1
}

// With settings:
//   max-column = 80
//   braces.def = force-braces
//   blank-lines.body-edges.def = never
```

## `blank-lines.body-edges.enum`

**Default:** `keep`.

The blank lines at the edges of the body of An enum. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
enum One {
  case Red
  case Green
}

enum Two {

  case Red
  case Green

}
```

### `blank-lines.body-edges.enum = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
enum One {
  case Red
  case Green
}

enum Two {

  case Red
  case Green

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.enum = keep
```

### `blank-lines.body-edges.enum = always`

A blank line at both edges.

```scala
enum One {

  case Red
  case Green

}

enum Two {

  case Red
  case Green

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.enum = always
```

### `blank-lines.body-edges.enum = never`

No blank line at either edge.

```scala
enum One {
  case Red
  case Green
}

enum Two {
  case Red
  case Green
}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.enum = never
```

## `blank-lines.body-edges.extension`

**Default:** `keep`.

The blank lines at the edges of the body of The body of an `extension`. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
extension (x: Int) {
  def twiceOne = x * 2
  def thriceOne = x * 3
}

extension (y: Int) {

  def twiceTwo = y * 2
  def thriceTwo = y * 3

}
```

### `blank-lines.body-edges.extension = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
extension (x: Int) {
  def twiceOne  = x * 2
  def thriceOne = x * 3
}

extension (y: Int) {

  def twiceTwo  = y * 2
  def thriceTwo = y * 3

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.extension = keep
```

### `blank-lines.body-edges.extension = always`

A blank line at both edges.

```scala
extension (x: Int) {

  def twiceOne  = x * 2
  def thriceOne = x * 3

}

extension (y: Int) {

  def twiceTwo  = y * 2
  def thriceTwo = y * 3

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.extension = always
```

### `blank-lines.body-edges.extension = never`

No blank line at either edge.

```scala
extension (x: Int) {
  def twiceOne  = x * 2
  def thriceOne = x * 3
}

extension (y: Int) {
  def twiceTwo  = y * 2
  def thriceTwo = y * 3
}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.extension = never
```

## `blank-lines.body-edges.for`

**Default:** `never`.

The blank lines at the edges of the body of The body of a `for` loop. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
def fOne(): Unit =
  for x <- xs do {
    a(x)
    b(x)
  }

def fTwo(): Unit =
  for x <- xs do {

    a(x)
    b(x)

  }
```

### `blank-lines.body-edges.for = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
def fOne(): Unit =
  for {
    x <- xs
  } {
    a(x)
    b(x)
  }

def fTwo(): Unit =
  for {
    x <- xs
  } {

    a(x)
    b(x)

  }

// With settings:
//   max-column = 80
//   braces.for = force-braces
//   blank-lines.body-edges.for = keep
```

### `blank-lines.body-edges.for = always`

A blank line at both edges.

```scala
def fOne(): Unit =
  for {
    x <- xs
  } {

    a(x)
    b(x)

  }

def fTwo(): Unit =
  for {
    x <- xs
  } {

    a(x)
    b(x)

  }

// With settings:
//   max-column = 80
//   braces.for = force-braces
//   blank-lines.body-edges.for = always
```

### `blank-lines.body-edges.for = never`

No blank line at either edge.

```scala
def fOne(): Unit =
  for {
    x <- xs
  } {
    a(x)
    b(x)
  }

def fTwo(): Unit =
  for {
    x <- xs
  } {
    a(x)
    b(x)
  }

// With settings:
//   max-column = 80
//   braces.for = force-braces
//   blank-lines.body-edges.for = never
```

## `blank-lines.body-edges.for-yield`

**Default:** `never`.

The blank lines at the edges of the body of The body of a for-comprehension. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
def fOne =
  for x <- xs yield {
    a(x)
    b(x)
  }

def fTwo =
  for x <- xs yield {

    a(x)
    b(x)

  }
```

### `blank-lines.body-edges.for-yield = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
def fOne =
  for {
    x <- xs
  } yield {
    a(x)
    b(x)
  }

def fTwo =
  for {
    x <- xs
  } yield {

    a(x)
    b(x)

  }

// With settings:
//   max-column = 80
//   braces.for-yield = force-braces
//   blank-lines.body-edges.for-yield = keep
```

### `blank-lines.body-edges.for-yield = always`

A blank line at both edges.

```scala
def fOne =
  for {
    x <- xs
  } yield {

    a(x)
    b(x)

  }

def fTwo =
  for {
    x <- xs
  } yield {

    a(x)
    b(x)

  }

// With settings:
//   max-column = 80
//   braces.for-yield = force-braces
//   blank-lines.body-edges.for-yield = always
```

### `blank-lines.body-edges.for-yield = never`

No blank line at either edge.

```scala
def fOne =
  for {
    x <- xs
  } yield {
    a(x)
    b(x)
  }

def fTwo =
  for {
    x <- xs
  } yield {
    a(x)
    b(x)
  }

// With settings:
//   max-column = 80
//   braces.for-yield = force-braces
//   blank-lines.body-edges.for-yield = never
```

## `blank-lines.body-edges.given-alias`

**Default:** `never`.

The blank lines at the edges of the body of A given alias. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
given intOrderOne: Ordering[Int] = {
  val a = 1
  Ordering.Int
}

given intOrderTwo: Ordering[Int] = {

  val a = 1
  Ordering.Int

}
```

### `blank-lines.body-edges.given-alias = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
given intOrderOne: Ordering[Int] = {
  val a = 1
  Ordering.Int
}

given intOrderTwo: Ordering[Int] = {

  val a = 1
  Ordering.Int

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.given-alias = keep
```

### `blank-lines.body-edges.given-alias = always`

A blank line at both edges.

```scala
given intOrderOne: Ordering[Int] = {

  val a = 1
  Ordering.Int

}

given intOrderTwo: Ordering[Int] = {

  val a = 1
  Ordering.Int

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.given-alias = always
```

### `blank-lines.body-edges.given-alias = never`

No blank line at either edge.

```scala
given intOrderOne: Ordering[Int] = {
  val a = 1
  Ordering.Int
}

given intOrderTwo: Ordering[Int] = {
  val a = 1
  Ordering.Int
}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.given-alias = never
```

## `blank-lines.body-edges.given-instance`

**Default:** `keep`.

The blank lines at the edges of the body of A given instance. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
given intOrdOne: Ordering[Int] with {
  def compare(x: Int, y: Int) = 0
  def other = 1
}

given intOrdTwo: Ordering[Int] with {

  def compare(x: Int, y: Int) = 0
  def other = 1

}
```

### `blank-lines.body-edges.given-instance = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
given intOrdOne: Ordering[Int] with {
  def compare(x: Int, y: Int) = 0
  def other                   = 1
}

given intOrdTwo: Ordering[Int] with {

  def compare(x: Int, y: Int) = 0
  def other                   = 1

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.given-instance = keep
```

### `blank-lines.body-edges.given-instance = always`

A blank line at both edges.

```scala
given intOrdOne: Ordering[Int] with {

  def compare(x: Int, y: Int) = 0
  def other                   = 1

}

given intOrdTwo: Ordering[Int] with {

  def compare(x: Int, y: Int) = 0
  def other                   = 1

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.given-instance = always
```

### `blank-lines.body-edges.given-instance = never`

No blank line at either edge.

```scala
given intOrdOne: Ordering[Int] with {
  def compare(x: Int, y: Int) = 0
  def other                   = 1
}

given intOrdTwo: Ordering[Int] with {
  def compare(x: Int, y: Int) = 0
  def other                   = 1
}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.given-instance = never
```

## `blank-lines.body-edges.if`

**Default:** `never`.

The blank lines at the edges of the body of The branches of an if/else. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
def fOne =
  if c then {
    a()
    b()
  } else {
    c()
    d()
  }

def fTwo =
  if c then {

    a()
    b()

  } else {

    c()
    d()

  }
```

### `blank-lines.body-edges.if = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
def fOne =
  if (c) {
    a()
    b()
  } else {
    c()
    d()
  }

def fTwo =
  if (c) {

    a()
    b()

  } else {

    c()
    d()

  }

// With settings:
//   max-column = 80
//   braces.if = force-braces
//   blank-lines.body-edges.if = keep
```

### `blank-lines.body-edges.if = always`

A blank line at both edges.

```scala
def fOne =
  if (c) {

    a()
    b()

  } else {

    c()
    d()

  }

def fTwo =
  if (c) {

    a()
    b()

  } else {

    c()
    d()

  }

// With settings:
//   max-column = 80
//   braces.if = force-braces
//   blank-lines.body-edges.if = always
```

### `blank-lines.body-edges.if = never`

No blank line at either edge.

```scala
def fOne =
  if (c) {
    a()
    b()
  } else {
    c()
    d()
  }

def fTwo =
  if (c) {
    a()
    b()
  } else {
    c()
    d()
  }

// With settings:
//   max-column = 80
//   braces.if = force-braces
//   blank-lines.body-edges.if = never
```

## `blank-lines.body-edges.lambda-argument`

**Default:** `never`.

The blank lines at the edges of the body of The body of a lambda given in braces. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
def fOne(): Unit =
  xs.foreach { x =>
    a(x)
    b(x)
  }

def fTwo(): Unit =
  xs.foreach { x =>
    a(x)
    b(x)

  }
```

### `blank-lines.body-edges.lambda-argument = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
def fOne(): Unit =
  xs.foreach { x =>
    a(x)
    b(x)
  }

def fTwo(): Unit =
  xs.foreach { x =>

    a(x)
    b(x)

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.lambda-argument = keep
```

### `blank-lines.body-edges.lambda-argument = always`

A blank line at both edges.

```scala
def fOne(): Unit =
  xs.foreach { x =>

    a(x)
    b(x)

  }

def fTwo(): Unit =
  xs.foreach { x =>

    a(x)
    b(x)

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.lambda-argument = always
```

### `blank-lines.body-edges.lambda-argument = never`

No blank line at either edge.

```scala
def fOne(): Unit =
  xs.foreach { x =>
    a(x)
    b(x)
  }

def fTwo(): Unit =
  xs.foreach { x =>
    a(x)
    b(x)
  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.lambda-argument = never
```

## `blank-lines.body-edges.lambda-body`

**Default:** `never`.

The blank lines at the edges of the body of The body of any other lambda. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
val gOne: Int => Unit = x => {
  a(x)
  b(x)
}

val gTwo: Int => Unit = x => {

  a(x)
  b(x)

}
```

### `blank-lines.body-edges.lambda-body = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
val gOne: Int => Unit =
  x => {
    a(x)
    b(x)
  }

val gTwo: Int => Unit =
  x => {

    a(x)
    b(x)

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.lambda-body = keep
```

### `blank-lines.body-edges.lambda-body = always`

A blank line at both edges.

```scala
val gOne: Int => Unit =
  x => {

    a(x)
    b(x)

  }

val gTwo: Int => Unit =
  x => {

    a(x)
    b(x)

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.lambda-body = always
```

### `blank-lines.body-edges.lambda-body = never`

No blank line at either edge.

```scala
val gOne: Int => Unit =
  x => {
    a(x)
    b(x)
  }

val gTwo: Int => Unit =
  x => {
    a(x)
    b(x)
  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.lambda-body = never
```

## `blank-lines.body-edges.lazy-val`

**Default:** `never`.

The blank lines at the edges of the body of A `lazy val`. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
lazy val vOne = {
  val a = 1
  a + 1
}

lazy val vTwo = {

  val a = 1
  a + 1

}
```

### `blank-lines.body-edges.lazy-val = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
lazy val vOne = {
  val a = 1
  a + 1
}

lazy val vTwo = {

  val a = 1
  a + 1

}

// With settings:
//   max-column = 80
//   braces.lazy-val = force-braces
//   blank-lines.body-edges.lazy-val = keep
```

### `blank-lines.body-edges.lazy-val = always`

A blank line at both edges.

```scala
lazy val vOne = {

  val a = 1
  a + 1

}

lazy val vTwo = {

  val a = 1
  a + 1

}

// With settings:
//   max-column = 80
//   braces.lazy-val = force-braces
//   blank-lines.body-edges.lazy-val = always
```

### `blank-lines.body-edges.lazy-val = never`

No blank line at either edge.

```scala
lazy val vOne = {
  val a = 1
  a + 1
}

lazy val vTwo = {
  val a = 1
  a + 1
}

// With settings:
//   max-column = 80
//   braces.lazy-val = force-braces
//   blank-lines.body-edges.lazy-val = never
```

## `blank-lines.body-edges.match`

**Default:** `never`.

The blank lines at the edges of the body of The cases of a `match`. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
def fOne(x: Int) = x match {
    case 1 =>
      a()
      b()
    case 2 => c()
  }

def fTwo(x: Int) = x match {

    case 1 =>
      a()
      b()
    case 2 => c()

  }
```

### `blank-lines.body-edges.match = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
def fOne(x: Int) =
  x match {
    case 1 =>
      a()
      b()
    case 2 =>
      c()
  }

def fTwo(x: Int) =
  x match {

    case 1 =>
      a()
      b()
    case 2 =>
      c()

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.match = keep
```

### `blank-lines.body-edges.match = always`

A blank line at both edges.

```scala
def fOne(x: Int) =
  x match {

    case 1 =>
      a()
      b()
    case 2 =>
      c()

  }

def fTwo(x: Int) =
  x match {

    case 1 =>
      a()
      b()
    case 2 =>
      c()

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.match = always
```

### `blank-lines.body-edges.match = never`

No blank line at either edge.

```scala
def fOne(x: Int) =
  x match {
    case 1 =>
      a()
      b()
    case 2 =>
      c()
  }

def fTwo(x: Int) =
  x match {
    case 1 =>
      a()
      b()
    case 2 =>
      c()
  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.match = never
```

## `blank-lines.body-edges.new-anonymous`

**Default:** `never`.

The blank lines at the edges of the body of The body of a new anonymous class. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
val rOne = new Runnable {
  def run(): Unit = ()
  def stop(): Unit = ()
}

val rTwo = new Runnable {

  def run(): Unit = ()
  def stop(): Unit = ()

}
```

### `blank-lines.body-edges.new-anonymous = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
val rOne =
  new Runnable {
    def run(): Unit  = ()
    def stop(): Unit = ()
  }

val rTwo =
  new Runnable {

    def run(): Unit  = ()
    def stop(): Unit = ()

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.new-anonymous = keep
```

### `blank-lines.body-edges.new-anonymous = always`

A blank line at both edges.

```scala
val rOne =
  new Runnable {

    def run(): Unit  = ()
    def stop(): Unit = ()

  }

val rTwo =
  new Runnable {

    def run(): Unit  = ()
    def stop(): Unit = ()

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.new-anonymous = always
```

### `blank-lines.body-edges.new-anonymous = never`

No blank line at either edge.

```scala
val rOne =
  new Runnable {
    def run(): Unit  = ()
    def stop(): Unit = ()
  }

val rTwo =
  new Runnable {
    def run(): Unit  = ()
    def stop(): Unit = ()
  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.new-anonymous = never
```

## `blank-lines.body-edges.object`

**Default:** `keep`.

The blank lines at the edges of the body of An object. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
object One {
  def f = 1
  def g = 2
}

object Two {

  def f = 1
  def g = 2

}
```

### `blank-lines.body-edges.object = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
object One {
  def f = 1
  def g = 2
}

object Two {

  def f = 1
  def g = 2

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.object = keep
```

### `blank-lines.body-edges.object = always`

A blank line at both edges.

```scala
object One {

  def f = 1
  def g = 2

}

object Two {

  def f = 1
  def g = 2

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.object = always
```

### `blank-lines.body-edges.object = never`

No blank line at either edge.

```scala
object One {
  def f = 1
  def g = 2
}

object Two {
  def f = 1
  def g = 2
}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.object = never
```

## `blank-lines.body-edges.test-case`

**Default:** `keep`.

The blank lines at the edges of the body of The body of a test case. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
def sOne(): Unit =
  test("a parser") {
    parse("1")
    parse("2")
  }

def sTwo(): Unit =
  test("a parser") {

    parse("1")
    parse("2")

  }
```

### `blank-lines.body-edges.test-case = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
def sOne(): Unit =
  test("a parser") {
    parse("1")
    parse("2")
  }

def sTwo(): Unit =
  test("a parser") {

    parse("1")
    parse("2")

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.test-case = keep
```

### `blank-lines.body-edges.test-case = always`

A blank line at both edges.

```scala
def sOne(): Unit =
  test("a parser") {

    parse("1")
    parse("2")

  }

def sTwo(): Unit =
  test("a parser") {

    parse("1")
    parse("2")

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.test-case = always
```

### `blank-lines.body-edges.test-case = never`

No blank line at either edge.

```scala
def sOne(): Unit =
  test("a parser") {
    parse("1")
    parse("2")
  }

def sTwo(): Unit =
  test("a parser") {
    parse("1")
    parse("2")
  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.test-case = never
```

## `blank-lines.body-edges.test-group`

**Default:** `keep`.

The blank lines at the edges of the body of The body of a group of tests. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
def sOne(): Unit =
  test("group") {
    test("a") { parse("1") }
    test("b") { parse("2") }
  }

def sTwo(): Unit =
  test("group") {

    test("a") { parse("1") }
    test("b") { parse("2") }

  }
```

### `blank-lines.body-edges.test-group = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
def sOne(): Unit =
  test("group") {
    test("a") { parse("1") }
    test("b") { parse("2") }
  }

def sTwo(): Unit =
  test("group") {

    test("a") { parse("1") }
    test("b") { parse("2") }

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.test-group = keep
```

### `blank-lines.body-edges.test-group = always`

A blank line at both edges.

```scala
def sOne(): Unit =
  test("group") {

    test("a") { parse("1") }
    test("b") { parse("2") }

  }

def sTwo(): Unit =
  test("group") {

    test("a") { parse("1") }
    test("b") { parse("2") }

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.test-group = always
```

### `blank-lines.body-edges.test-group = never`

No blank line at either edge.

```scala
def sOne(): Unit =
  test("group") {
    test("a") { parse("1") }
    test("b") { parse("2") }
  }

def sTwo(): Unit =
  test("group") {
    test("a") { parse("1") }
    test("b") { parse("2") }
  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.test-group = never
```

## `blank-lines.body-edges.trait`

**Default:** `keep`.

The blank lines at the edges of the body of A trait. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
trait One {
  def f = 1
  def g = 2
}

trait Two {

  def f = 1
  def g = 2

}
```

### `blank-lines.body-edges.trait = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
trait One {
  def f = 1
  def g = 2
}

trait Two {

  def f = 1
  def g = 2

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.trait = keep
```

### `blank-lines.body-edges.trait = always`

A blank line at both edges.

```scala
trait One {

  def f = 1
  def g = 2

}

trait Two {

  def f = 1
  def g = 2

}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.trait = always
```

### `blank-lines.body-edges.trait = never`

No blank line at either edge.

```scala
trait One {
  def f = 1
  def g = 2
}

trait Two {
  def f = 1
  def g = 2
}

// With settings:
//   max-column = 80
//   blank-lines.body-edges.trait = never
```

## `blank-lines.body-edges.try`

**Default:** `never`.

The blank lines at the edges of the body of The bodies of a try. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
def fOne(): Unit =
  try {
    a()
    b()
  } finally {
    c()
    d()
  }

def fTwo(): Unit =
  try {

    a()
    b()

  } finally {

    c()
    d()

  }
```

### `blank-lines.body-edges.try = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
def fOne(): Unit =
  try {
    a()
    b()
  } finally {
    c()
    d()
  }

def fTwo(): Unit =
  try {

    a()
    b()

  } finally {

    c()
    d()

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.try = keep
```

### `blank-lines.body-edges.try = always`

A blank line at both edges.

```scala
def fOne(): Unit =
  try {

    a()
    b()

  } finally {

    c()
    d()

  }

def fTwo(): Unit =
  try {

    a()
    b()

  } finally {

    c()
    d()

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.try = always
```

### `blank-lines.body-edges.try = never`

No blank line at either edge.

```scala
def fOne(): Unit =
  try {
    a()
    b()
  } finally {
    c()
    d()
  }

def fTwo(): Unit =
  try {
    a()
    b()
  } finally {
    c()
    d()
  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.try = never
```

## `blank-lines.body-edges.val`

**Default:** `never`.

The blank lines at the edges of the body of A `val`. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
val vOne = {
  val a = 1
  a + 1
}

val vTwo = {

  val a = 1
  a + 1

}
```

### `blank-lines.body-edges.val = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
val vOne = {
  val a = 1
  a + 1
}

val vTwo = {

  val a = 1
  a + 1

}

// With settings:
//   max-column = 80
//   braces.val = force-braces
//   blank-lines.body-edges.val = keep
```

### `blank-lines.body-edges.val = always`

A blank line at both edges.

```scala
val vOne = {

  val a = 1
  a + 1

}

val vTwo = {

  val a = 1
  a + 1

}

// With settings:
//   max-column = 80
//   braces.val = force-braces
//   blank-lines.body-edges.val = always
```

### `blank-lines.body-edges.val = never`

No blank line at either edge.

```scala
val vOne = {
  val a = 1
  a + 1
}

val vTwo = {
  val a = 1
  a + 1
}

// With settings:
//   max-column = 80
//   braces.val = force-braces
//   blank-lines.body-edges.val = never
```

## `blank-lines.body-edges.var`

**Default:** `never`.

The blank lines at the edges of the body of A `var`. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
var vOne = {
  val a = 1
  a + 1
}

var vTwo = {

  val a = 1
  a + 1

}
```

### `blank-lines.body-edges.var = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
var vOne = {
  val a = 1
  a + 1
}

var vTwo = {

  val a = 1
  a + 1

}

// With settings:
//   max-column = 80
//   braces.var = force-braces
//   blank-lines.body-edges.var = keep
```

### `blank-lines.body-edges.var = always`

A blank line at both edges.

```scala
var vOne = {

  val a = 1
  a + 1

}

var vTwo = {

  val a = 1
  a + 1

}

// With settings:
//   max-column = 80
//   braces.var = force-braces
//   blank-lines.body-edges.var = always
```

### `blank-lines.body-edges.var = never`

No blank line at either edge.

```scala
var vOne = {
  val a = 1
  a + 1
}

var vTwo = {
  val a = 1
  a + 1
}

// With settings:
//   max-column = 80
//   braces.var = force-braces
//   blank-lines.body-edges.var = never
```

## `blank-lines.body-edges.while`

**Default:** `never`.

The blank lines at the edges of the body of The body of a `while` loop. The first body has no blank line at its edges, and the second one has both.

Before:

```scala
def fOne(): Unit =
  while c do {
    a()
    b()
  }

def fTwo(): Unit =
  while c do {

    a()
    b()

  }
```

### `blank-lines.body-edges.while = keep`

A blank line at one edge agrees with the other: a blank line written at either edge stays at both, and a body with neither keeps none.

```scala
def fOne(): Unit =
  while (c) {
    a()
    b()
  }

def fTwo(): Unit =
  while (c) {

    a()
    b()

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.while = keep
```

### `blank-lines.body-edges.while = always`

A blank line at both edges.

```scala
def fOne(): Unit =
  while (c) {

    a()
    b()

  }

def fTwo(): Unit =
  while (c) {

    a()
    b()

  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.while = always
```

### `blank-lines.body-edges.while = never`

No blank line at either edge.

```scala
def fOne(): Unit =
  while (c) {
    a()
    b()
  }

def fTwo(): Unit =
  while (c) {
    a()
    b()
  }

// With settings:
//   max-column = 80
//   blank-lines.body-edges.while = never
```

## `blank-lines.between-alignment-groups`

**Default:** `keep`.

The blank line between two adjacent one-line definitions of different alignment groups (see [`alignment.assignment-groups`](alignment.md)). The examples are with `alignment.assignment-groups = [[val], [def]]`, so a `val` and a `def` are two groups.

Before:

```scala
val a = 1
def b = 2

val c = 3

def d = 4
```

### `blank-lines.between-alignment-groups = keep`

As written: no blank line between the first two definitions, one between the last two:

```scala
val a = 1
def b = 2

val c = 3

def d = 4

// With settings:
//   max-column = 80
//   alignment.assignment-groups = [[val], [def]]
//   blank-lines.between-alignment-groups = keep
```

### `blank-lines.between-alignment-groups = always`

A blank line between both pairs:

```scala
val a = 1

def b = 2

val c = 3

def d = 4

// With settings:
//   max-column = 80
//   alignment.assignment-groups = [[val], [def]]
//   blank-lines.between-alignment-groups = always
```

### `blank-lines.between-alignment-groups = never`

No blank line between either pair:

```scala
val a = 1
def b = 2
val c = 3
def d = 4

// With settings:
//   max-column = 80
//   alignment.assignment-groups = [[val], [def]]
//   blank-lines.between-alignment-groups = never
```

## `blank-lines.before-scaladoc`

**Default:** `always`.

The blank line before a scaladoc that follows a line of code. At the start of a body (a class, an object, a trait, an enum, an extension, a package), a scaladoc that gets a blank line counts as a blank line written at the opening edge, and `blank-lines.body-edges` decides.

Before:

```scala
def a = 1

/** One line */
def b = 2

def c = 3
/** Two lines */
def d = 4

def e = 5
/**
 * Three lines
 */
def f = 6
```

### `blank-lines.before-scaladoc = always`

A blank line before every scaladoc, except a one-line scaladoc right after a line that opens a block:

```scala
def a = 1

/** One line */
def b = 2

def c = 3

/** Two lines */
def d = 4

def e = 5

/**
 * Three lines
 */
def f = 6

// With settings:
//   max-column = 80
//   blank-lines.before-scaladoc = always
```

### `blank-lines.before-scaladoc = always-multiline`

As `always`, for a multi-line scaladoc only; a one-line scaladoc stays as written, and does not count as a blank line at the start of a body:

```scala
def a = 1

/** One line */
def b = 2

def c = 3
/** Two lines */
def d = 4

def e = 5

/**
 * Three lines
 */
def f = 6

// With settings:
//   max-column = 80
//   blank-lines.before-scaladoc = always-multiline
```

### `blank-lines.before-scaladoc = keep`

As written:

```scala
def a = 1

/** One line */
def b = 2

def c = 3
/** Two lines */
def d = 4

def e = 5
/**
 * Three lines
 */
def f = 6

// With settings:
//   max-column = 80
//   blank-lines.before-scaladoc = keep
```

### `blank-lines.before-scaladoc = never`

No blank line, a method's included; after a line that opens a block, `blank-lines.body-edges` decides:

```scala
def a = 1
/** One line */
def b = 2

def c = 3
/** Two lines */
def d = 4

def e = 5
/**
 * Three lines
 */
def f = 6

// With settings:
//   max-column = 80
//   blank-lines.before-scaladoc = never
```
