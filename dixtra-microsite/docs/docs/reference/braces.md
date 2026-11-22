---
layout: docs
title: Braces
---

# Braces

Whether each construct writes its body in braces, braceless with an `end` marker, or braceless, and where the braces go. The [configuration](../configuration.md) page explains how to write the settings.

`braces.<construct> = force-end` is rejected by the constructs that have no `end` marker: the blocks given as arguments, the case bodies, the lambdas and the tests. A `for`, a `while` or an `if` written on one line that fits stays as it is, and a case class or an object without a body never gets braces.

## `braces.style`

**Default:** `1tbs`.

Where the braces go relative to the code around them. It applies to every brace Dixtra writes; the braces of an argument (`xs.map { x => ... }`) and of a body after `=>` stay where they are. See [Indentation Style](https://en.wikipedia.org/wiki/Indentation_style).

Before:

```scala
def f =
  if c then {
    a()
  } else {
    b()
  }
```

### `braces.style = 1tbs`

The `{` at the end of the header line, and `} else {` on one line:

```scala
def f =
  if (c) {
    a()
  } else {
    b()
  }

// With settings:
//   max-column = 80
//   braces.if = force-braces
//   braces.style = 1tbs
```

### `braces.style = stroustrup`

As `1tbs`, with `else`, `catch`, `finally` and `yield` starting the line after the closing `}`:

```scala
def f =
  if (c) {
    a()
  }
  else {
    b()
  }

// With settings:
//   max-column = 80
//   braces.if = force-braces
//   braces.style = stroustrup
```

### `braces.style = allman`

As `stroustrup`, with every opening `{` of a body on its own line:

```scala
def f =
  if (c)
  {
    a()
  }
  else
  {
    b()
  }

// With settings:
//   max-column = 80
//   braces.if = force-braces
//   braces.style = allman
```

## `braces.imports`

**Default:** `keep`.

Whether the single selector of an `import` or of an `export` is in braces. The selectors keep their spelling (`_` / `*`, `=>` / `as`), and an import with several selectors always keeps its braces.

Before:

```scala
import a.B
import c.{ D }
import e.{ F => G }
```

### `braces.imports = keep`

As written:

```scala
import a.B
import c.{ D }
import e.{ F => G }

// With settings:
//   max-column = 80
//   braces.imports = keep
```

### `braces.imports = force-braces`

Every single selector in braces; a rename keeps its braces:

```scala
import a.{ B }
import c.{ D }
import e.{ F => G }

// With settings:
//   max-column = 80
//   braces.imports = force-braces
```

### `braces.imports = avoid-braces`

The braces of a single selector go; a rename keeps its braces:

```scala
import a.B
import c.D
import e.{ F => G }

// With settings:
//   max-column = 80
//   braces.imports = avoid-braces
```

## `braces.abstract-class`

**Default:** `force-braces`.

An abstract class. The first body is written in braces, the second one braceless.

Before:

```scala
abstract class One {
  def f: Int
  def g: Int
}

abstract class Two:
  def f: Int
  def g: Int
```

### `braces.abstract-class = force-braces`

In curly braces.

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
//   braces.abstract-class = force-braces
```

### `braces.abstract-class = force-end`

Braceless, closed by an `end` marker when it spans several lines.

```scala
abstract class One:
  def f: Int
  def g: Int
end One

abstract class Two:
  def f: Int
  def g: Int
end Two

// With settings:
//   max-column = 80
//   braces.abstract-class = force-end
```

### `braces.abstract-class = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
abstract class One:
  def f: Int
  def g: Int

abstract class Two:
  def f: Int
  def g: Int

// With settings:
//   max-column = 80
//   braces.abstract-class = force-indent
```

### `braces.abstract-class = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
abstract class One {
  def f: Int
  def g: Int
}

abstract class Two:
  def f: Int
  def g: Int

// With settings:
//   max-column = 80
//   braces.abstract-class = keep
```

## `braces.block-argument`

**Default:** `force-braces`.

The block of statements given as an argument. The first body is written in braces, the second one braceless. It has no `end` marker, so it rejects `force-end`.

Before:

```scala
def bkOne(): Unit =
  Resource.make {
    open()
    more()
  }

def blTwo(): Unit =
  Resource.make:
    open()
    more()
```

### `braces.block-argument = force-braces`

In curly braces.

```scala
def bkOne(): Unit =
  Resource.make {
    open()
    more()
  }

def blTwo(): Unit =
  Resource.make {
    open()
    more()
  }

// With settings:
//   max-column = 80
//   braces.block-argument = force-braces
```

### `braces.block-argument = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
def bkOne(): Unit =
  Resource.make:
    open()
    more()

def blTwo(): Unit =
  Resource.make:
    open()
    more()

// With settings:
//   max-column = 80
//   braces.block-argument = force-indent
```

### `braces.block-argument = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
def bkOne(): Unit =
  Resource.make {
    open()
    more()
  }

def blTwo(): Unit =
  Resource.make:
    open()
    more()

// With settings:
//   max-column = 80
//   braces.block-argument = keep
```

## `braces.case-body`

**Default:** `force-indent`.

The body of a case. The first body is written in braces, the second one braceless. It has no `end` marker, so it rejects `force-end`.

Before:

```scala
def cbOne(x: Int) =
  x match {
    case 1 => {
      a()
      b()
    }
  }

def ccTwo(x: Int) =
  x match {
    case 1 =>
      a()
      b()
  }
```

### `braces.case-body = force-braces`

In curly braces.

```scala
def cbOne(x: Int) =
  x match {
    case 1 => {
      a()
      b()
    }
  }

def ccTwo(x: Int) =
  x match {
    case 1 => {
      a()
      b()
    }
  }

// With settings:
//   max-column = 80
//   braces.case-body = force-braces
```

### `braces.case-body = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
def cbOne(x: Int) =
  x match {
    case 1 =>
      a()
      b()
  }

def ccTwo(x: Int) =
  x match {
    case 1 =>
      a()
      b()
  }

// With settings:
//   max-column = 80
//   braces.case-body = force-indent
```

### `braces.case-body = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
def cbOne(x: Int) =
  x match {
    case 1 => {
      a()
      b()
    }
  }

def ccTwo(x: Int) =
  x match {
    case 1 =>
      a()
      b()
  }

// With settings:
//   max-column = 80
//   braces.case-body = keep
```

## `braces.case-class`

**Default:** `force-braces`.

A case class with a body. The first body is written in braces, the second one braceless.

Before:

```scala
case class One(x: Int) {
  def f = x
  def g = x
}

case class Two(x: Int):
  def f = x
  def g = x
```

### `braces.case-class = force-braces`

In curly braces.

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
//   braces.case-class = force-braces
```

### `braces.case-class = force-end`

Braceless, closed by an `end` marker when it spans several lines.

```scala
case class One(x: Int):
  def f = x
  def g = x
end One

case class Two(x: Int):
  def f = x
  def g = x
end Two

// With settings:
//   max-column = 80
//   braces.case-class = force-end
```

### `braces.case-class = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
case class One(x: Int):
  def f = x
  def g = x

case class Two(x: Int):
  def f = x
  def g = x

// With settings:
//   max-column = 80
//   braces.case-class = force-indent
```

### `braces.case-class = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
case class One(x: Int) {
  def f = x
  def g = x
}

case class Two(x: Int):
  def f = x
  def g = x

// With settings:
//   max-column = 80
//   braces.case-class = keep
```

## `braces.class`

**Default:** `force-braces`.

A class. The first body is written in braces, the second one braceless.

Before:

```scala
class One {
  def f = 1
  def g = 2
}

class Two:
  def f = 1
  def g = 2
```

### `braces.class = force-braces`

In curly braces.

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
//   braces.class = force-braces
```

### `braces.class = force-end`

Braceless, closed by an `end` marker when it spans several lines.

```scala
class One:
  def f = 1
  def g = 2
end One

class Two:
  def f = 1
  def g = 2
end Two

// With settings:
//   max-column = 80
//   braces.class = force-end
```

### `braces.class = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
class One:
  def f = 1
  def g = 2

class Two:
  def f = 1
  def g = 2

// With settings:
//   max-column = 80
//   braces.class = force-indent
```

### `braces.class = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
class One {
  def f = 1
  def g = 2
}

class Two:
  def f = 1
  def g = 2

// With settings:
//   max-column = 80
//   braces.class = keep
```

## `braces.def`

**Default:** `force-indent`.

The body of a def. The first body is written in braces, the second one braceless.

Before:

```scala
def ddOne = {
  val a = 1
  a + 1
}

def deTwo =
  val a = 1
  a + 1
```

### `braces.def = force-braces`

In curly braces.

```scala
def ddOne = {
  val a = 1
  a + 1
}

def deTwo = {
  val a = 1
  a + 1
}

// With settings:
//   max-column = 80
//   braces.def = force-braces
```

### `braces.def = force-end`

Braceless, closed by an `end` marker when it spans several lines.

```scala
def ddOne =
  val a = 1
  a + 1
end ddOne

def deTwo =
  val a = 1
  a + 1
end deTwo

// With settings:
//   max-column = 80
//   braces.def = force-end
```

### `braces.def = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
def ddOne =
  val a = 1
  a + 1

def deTwo =
  val a = 1
  a + 1

// With settings:
//   max-column = 80
//   braces.def = force-indent
```

### `braces.def = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
def ddOne = {
  val a = 1
  a + 1
}

def deTwo =
  val a = 1
  a + 1

// With settings:
//   max-column = 80
//   braces.def = keep
```

## `braces.enum`

**Default:** `force-braces`.

An enum. The first body is written in braces, the second one braceless.

Before:

```scala
enum One {
  case Red
  case Green
}

enum Two:
  case Red
  case Green
```

### `braces.enum = force-braces`

In curly braces.

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
//   braces.enum = force-braces
```

### `braces.enum = force-end`

Braceless, closed by an `end` marker when it spans several lines.

```scala
enum One:
  case Red
  case Green
end One

enum Two:
  case Red
  case Green
end Two

// With settings:
//   max-column = 80
//   braces.enum = force-end
```

### `braces.enum = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
enum One:
  case Red
  case Green

enum Two:
  case Red
  case Green

// With settings:
//   max-column = 80
//   braces.enum = force-indent
```

### `braces.enum = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
enum One {
  case Red
  case Green
}

enum Two:
  case Red
  case Green

// With settings:
//   max-column = 80
//   braces.enum = keep
```

## `braces.extension`

**Default:** `force-braces`.

The body of an extension. The first body is written in braces, the second one braceless.

Before:

```scala
extension (sOne: String) {
  def a = sOne
  def b = sOne
}

extension (tTwo: String)
  def c = tTwo
  def d = tTwo
```

### `braces.extension = force-braces`

In curly braces.

```scala
extension (sOne: String) {
  def a = sOne
  def b = sOne
}

extension (tTwo: String) {
  def c = tTwo
  def d = tTwo
}

// With settings:
//   max-column = 80
//   braces.extension = force-braces
```

### `braces.extension = force-end`

Braceless, closed by an `end` marker when it spans several lines.

```scala
extension (sOne: String)
  def a = sOne
  def b = sOne
end extension

extension (tTwo: String)
  def c = tTwo
  def d = tTwo
end extension

// With settings:
//   max-column = 80
//   braces.extension = force-end
```

### `braces.extension = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
extension (sOne: String)
  def a = sOne
  def b = sOne

extension (tTwo: String)
  def c = tTwo
  def d = tTwo

// With settings:
//   max-column = 80
//   braces.extension = force-indent
```

### `braces.extension = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
extension (sOne: String) {
  def a = sOne
  def b = sOne
}

extension (tTwo: String)
  def c = tTwo
  def d = tTwo

// With settings:
//   max-column = 80
//   braces.extension = keep
```

## `braces.for`

**Default:** `force-indent`.

The body of a for loop. The first body is written in braces, the second one braceless.

Before:

```scala
def loopOne(): Unit =
  for x <- xs do {
    a(x)
    b(x)
  }

def loopTwo(): Unit =
  for x <- xs do
    a(x)
    b(x)
```

### `braces.for = force-braces`

In curly braces.

```scala
def loopOne(): Unit =
  for {
    x <- xs
  } {
    a(x)
    b(x)
  }

def loopTwo(): Unit =
  for {
    x <- xs
  } {
    a(x)
    b(x)
  }

// With settings:
//   max-column = 80
//   braces.for = force-braces
```

### `braces.for = force-end`

This construct has no `end` marker, so `force-end` is written like `force-indent`.

<!-- same-output-as: force-indent -->

Braceless, closed by an `end` marker when it spans several lines.

```scala
def loopOne(): Unit =
  for x <- xs do
    a(x)
    b(x)

def loopTwo(): Unit =
  for x <- xs do
    a(x)
    b(x)

// With settings:
//   max-column = 80
//   braces.for = force-end
```

### `braces.for = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
def loopOne(): Unit =
  for x <- xs do
    a(x)
    b(x)

def loopTwo(): Unit =
  for x <- xs do
    a(x)
    b(x)

// With settings:
//   max-column = 80
//   braces.for = force-indent
```

### `braces.for = keep`

A body written in braces is not kept as written here, so this value is currently written like `force-indent`.

<!-- same-output-as: force-indent -->

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
def loopOne(): Unit =
  for x <- xs do
    a(x)
    b(x)

def loopTwo(): Unit =
  for x <- xs do
    a(x)
    b(x)

// With settings:
//   max-column = 80
//   braces.for = keep
```

## `braces.for-yield`

**Default:** `force-indent`.

The body of a for-comprehension. The first body is written in braces, the second one braceless.

Before:

```scala
def fyOne =
  for x <- xs yield {
    a(x)
  }

def fzTwo =
  for x <- xs yield
    a(x)
```

### `braces.for-yield = force-braces`

In curly braces.

```scala
def fyOne =
  for {
    x <- xs
  } yield {
    a(x)
  }

def fzTwo =
  for {
    x <- xs
  } yield {
    a(x)
  }

// With settings:
//   max-column = 80
//   braces.for-yield = force-braces
```

### `braces.for-yield = force-end`

This construct has no `end` marker, so `force-end` is written like `force-indent`.

<!-- same-output-as: force-indent -->

Braceless, closed by an `end` marker when it spans several lines.

```scala
def fyOne =
  for
    x <- xs
  yield
    a(x)

def fzTwo =
  for
    x <- xs
  yield
    a(x)

// With settings:
//   max-column = 80
//   braces.for-yield = force-end
```

### `braces.for-yield = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
def fyOne =
  for
    x <- xs
  yield
    a(x)

def fzTwo =
  for
    x <- xs
  yield
    a(x)

// With settings:
//   max-column = 80
//   braces.for-yield = force-indent
```

### `braces.for-yield = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
def fyOne =
  for
    x <- xs
  yield {
    a(x)
  }

def fzTwo =
  for
    x <- xs
  yield
    a(x)

// With settings:
//   max-column = 80
//   braces.for-yield = keep
```

## `braces.given-alias`

**Default:** `keep`.

A given alias whose body is a block. The first body is written in braces, the second one braceless.

Before:

```scala
given orderOne: Ordering[Int] = {
  val b = 1
  ord(b)
}

given ordaTwo: Ordering[Int] =
  val c = 1
  ord(c)
```

### `braces.given-alias = force-braces`

In curly braces.

```scala
given orderOne: Ordering[Int] = {
  val b = 1
  ord(b)
}

given ordaTwo: Ordering[Int] = {
  val c = 1
  ord(c)
}

// With settings:
//   max-column = 80
//   braces.given-alias = force-braces
```

### `braces.given-alias = force-end`

Braceless, closed by an `end` marker when it spans several lines.

```scala
given orderOne: Ordering[Int] =
  val b = 1
  ord(b)
end orderOne

given ordaTwo: Ordering[Int] =
  val c = 1
  ord(c)
end ordaTwo

// With settings:
//   max-column = 80
//   braces.given-alias = force-end
```

### `braces.given-alias = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
given orderOne: Ordering[Int] =
  val b = 1
  ord(b)

given ordaTwo: Ordering[Int] =
  val c = 1
  ord(c)

// With settings:
//   max-column = 80
//   braces.given-alias = force-indent
```

### `braces.given-alias = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
given orderOne: Ordering[Int] = {
  val b = 1
  ord(b)
}

given ordaTwo: Ordering[Int] =
  val c = 1
  ord(c)

// With settings:
//   max-column = 80
//   braces.given-alias = keep
```

## `braces.given-instance`

**Default:** `force-braces`.

A given instance. The first body is written in braces, the second one braceless.

Before:

```scala
given ordOne: Ordering[Int] with {
  def compare(a: Int, b: Int) = a - b
  def other = 1
}

given ordeTwo: Ordering[Int] with
  def compare(a: Int, b: Int) = a - b
  def other = 1
```

### `braces.given-instance = force-braces`

In curly braces.

```scala
given ordOne: Ordering[Int] with {
  def compare(a: Int, b: Int) = a - b
  def other                   = 1
}

given ordeTwo: Ordering[Int] with {
  def compare(a: Int, b: Int) = a - b
  def other                   = 1
}

// With settings:
//   max-column = 80
//   braces.given-instance = force-braces
```

### `braces.given-instance = force-end`

Braceless, closed by an `end` marker when it spans several lines.

```scala
given ordOne: Ordering[Int] with
  def compare(a: Int, b: Int) = a - b
  def other                   = 1
end ordOne

given ordeTwo: Ordering[Int] with
  def compare(a: Int, b: Int) = a - b
  def other                   = 1
end ordeTwo

// With settings:
//   max-column = 80
//   braces.given-instance = force-end
```

### `braces.given-instance = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
given ordOne: Ordering[Int] with
  def compare(a: Int, b: Int) = a - b
  def other                   = 1

given ordeTwo: Ordering[Int] with
  def compare(a: Int, b: Int) = a - b
  def other                   = 1

// With settings:
//   max-column = 80
//   braces.given-instance = force-indent
```

### `braces.given-instance = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
given ordOne: Ordering[Int] with {
  def compare(a: Int, b: Int) = a - b
  def other                   = 1
}

given ordeTwo: Ordering[Int] with
  def compare(a: Int, b: Int) = a - b
  def other                   = 1

// With settings:
//   max-column = 80
//   braces.given-instance = keep
```

## `braces.if`

**Default:** `force-indent`.

The branches of an if/else. The first branch is written in braces, the second one braceless.

Before:

```scala
def branchOne =
  if c then {
    a()
  } else {
    b()
  }

def branchTwo =
  if c then
    a()
  else
    b()
```

### `braces.if = force-braces`

In curly braces.

```scala
def branchOne =
  if (c) {
    a()
  } else {
    b()
  }

def branchTwo =
  if (c) {
    a()
  } else {
    b()
  }

// With settings:
//   max-column = 80
//   braces.if = force-braces
```

### `braces.if = force-end`

This construct has no `end` marker, so `force-end` is written like `force-indent`.

<!-- same-output-as: force-indent -->

Braceless, closed by an `end` marker when it spans several lines.

```scala
def branchOne =
  if c then
    a()
  else
    b()

def branchTwo =
  if c then
    a()
  else
    b()

// With settings:
//   max-column = 80
//   braces.if = force-end
```

### `braces.if = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
def branchOne =
  if c then
    a()
  else
    b()

def branchTwo =
  if c then
    a()
  else
    b()

// With settings:
//   max-column = 80
//   braces.if = force-indent
```

### `braces.if = keep`

A body written in braces is not kept as written here, so this value is currently written like `force-indent`.

<!-- same-output-as: force-indent -->

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
def branchOne =
  if c then
    a()
  else
    b()

def branchTwo =
  if c then
    a()
  else
    b()

// With settings:
//   max-column = 80
//   braces.if = keep
```

## `braces.lambda-argument`

**Default:** `force-braces`.

The body of a lambda given in braces. The first body is written in braces, the second one braceless. It has no `end` marker, so it rejects `force-end`.

Before:

```scala
def laOne(): Unit =
  xs.foreach { x =>
    a(x)
    b(x)
  }

def lbTwo(): Unit =
  xs.map(x =>
    a(x) + b(x)
  )
```

### `braces.lambda-argument = force-braces`

In curly braces.

```scala
def laOne(): Unit =
  xs.foreach { x =>
    a(x)
    b(x)
  }

def lbTwo(): Unit =
  xs.map(
    x =>
      a(x) + b(x),
  )

// With settings:
//   max-column = 80
//   braces.lambda-argument = force-braces
```

### `braces.lambda-argument = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
def laOne(): Unit =
  xs.foreach: x =>
    a(x)
    b(x)

def lbTwo(): Unit =
  xs.map(
    x =>
      a(x) + b(x),
  )

// With settings:
//   max-column = 80
//   braces.lambda-argument = force-indent
```

### `braces.lambda-argument = keep`

A lambda over several lines is always written in braces, so this value is written like `force-braces`.

<!-- same-output-as: force-braces -->

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
def laOne(): Unit =
  xs.foreach { x =>
    a(x)
    b(x)
  }

def lbTwo(): Unit =
  xs.map(
    x =>
      a(x) + b(x),
  )

// With settings:
//   max-column = 80
//   braces.lambda-argument = keep
```

## `braces.lambda-body`

**Default:** `keep`.

The body of any other lambda. The first body is written in braces, the second one braceless. It has no `end` marker, so it rejects `force-end`.

Before:

```scala
val lbOne: Int => Unit = x => {
  a(x)
  b(x)
}

val lcTwo: Int => Unit = x =>
  a(x)
  b(x)
```

### `braces.lambda-body = force-braces`

In curly braces.

```scala
val lbOne: Int => Unit =
  x => {
    a(x)
    b(x)
  }

val lcTwo: Int => Unit =
  x => {
    a(x)
    b(x)
  }

// With settings:
//   max-column = 80
//   braces.lambda-body = force-braces
```

### `braces.lambda-body = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
val lbOne: Int => Unit =
  x =>
    a(x)
    b(x)

val lcTwo: Int => Unit =
  x =>
    a(x)
    b(x)

// With settings:
//   max-column = 80
//   braces.lambda-body = force-indent
```

### `braces.lambda-body = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
val lbOne: Int => Unit =
  x => {
    a(x)
    b(x)
  }

val lcTwo: Int => Unit =
  x =>
    a(x)
    b(x)

// With settings:
//   max-column = 80
//   braces.lambda-body = keep
```

## `braces.lazy-val`

**Default:** `force-indent`.

The body of a lazy val. The first body is written in braces, the second one braceless.

Before:

```scala
lazy val lvOne = {
  val a = 1
  a + 1
}

lazy val lwTwo =
  val a = 1
  a + 1
```

### `braces.lazy-val = force-braces`

In curly braces.

```scala
lazy val lvOne = {
  val a = 1
  a + 1
}

lazy val lwTwo = {
  val a = 1
  a + 1
}

// With settings:
//   max-column = 80
//   braces.lazy-val = force-braces
```

### `braces.lazy-val = force-end`

Braceless, closed by an `end` marker when it spans several lines.

```scala
lazy val lvOne =
  val a = 1
  a + 1
end lvOne

lazy val lwTwo =
  val a = 1
  a + 1
end lwTwo

// With settings:
//   max-column = 80
//   braces.lazy-val = force-end
```

### `braces.lazy-val = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
lazy val lvOne =
  val a = 1
  a + 1

lazy val lwTwo =
  val a = 1
  a + 1

// With settings:
//   max-column = 80
//   braces.lazy-val = force-indent
```

### `braces.lazy-val = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
lazy val lvOne = {
  val a = 1
  a + 1
}

lazy val lwTwo =
  val a = 1
  a + 1

// With settings:
//   max-column = 80
//   braces.lazy-val = keep
```

## `braces.match`

**Default:** `force-braces`.

The cases of a match. The first body is written in braces, the second one braceless.

Before:

```scala
def mOne(x: Int) = x match {
    case 1 => a
    case 2 => b
  }

def nTwo(x: Int) = x match
    case 1 => a
    case 2 => b
```

### `braces.match = force-braces`

In curly braces.

```scala
def mOne(x: Int) =
  x match {
    case 1 => a
    case 2 => b
  }

def nTwo(x: Int) =
  x match {
    case 1 => a
    case 2 => b
  }

// With settings:
//   max-column = 80
//   braces.match = force-braces
```

### `braces.match = force-end`

This construct has no `end` marker, so `force-end` is written like `force-indent`.

<!-- same-output-as: force-indent -->

Braceless, closed by an `end` marker when it spans several lines.

```scala
def mOne(x: Int) =
  x match
    case 1 => a
    case 2 => b

def nTwo(x: Int) =
  x match
    case 1 => a
    case 2 => b

// With settings:
//   max-column = 80
//   braces.match = force-end
```

### `braces.match = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
def mOne(x: Int) =
  x match
    case 1 => a
    case 2 => b

def nTwo(x: Int) =
  x match
    case 1 => a
    case 2 => b

// With settings:
//   max-column = 80
//   braces.match = force-indent
```

### `braces.match = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
def mOne(x: Int) =
  x match {
    case 1 => a
    case 2 => b
  }

def nTwo(x: Int) =
  x match
    case 1 => a
    case 2 => b

// With settings:
//   max-column = 80
//   braces.match = keep
```

## `braces.new-anonymous`

**Default:** `force-braces`.

A new anonymous class. The first body is written in braces, the second one braceless.

Before:

```scala
val rOne = new Runnable {
  def run(): Unit = ()
  def stop(): Unit = ()
}

val qTwo = new Runnable:
  def run(): Unit = ()
  def stop(): Unit = ()
```

### `braces.new-anonymous = force-braces`

In curly braces.

```scala
val rOne =
  new Runnable {
    def run(): Unit  = ()
    def stop(): Unit = ()
  }

val qTwo =
  new Runnable {
    def run(): Unit  = ()
    def stop(): Unit = ()
  }

// With settings:
//   max-column = 80
//   braces.new-anonymous = force-braces
```

### `braces.new-anonymous = force-end`

This construct has no `end` marker, so `force-end` is written like `force-indent`.

<!-- same-output-as: force-indent -->

Braceless, closed by an `end` marker when it spans several lines.

```scala
val rOne =
  new Runnable:
    def run(): Unit  = ()
    def stop(): Unit = ()

val qTwo =
  new Runnable:
    def run(): Unit  = ()
    def stop(): Unit = ()

// With settings:
//   max-column = 80
//   braces.new-anonymous = force-end
```

### `braces.new-anonymous = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
val rOne =
  new Runnable:
    def run(): Unit  = ()
    def stop(): Unit = ()

val qTwo =
  new Runnable:
    def run(): Unit  = ()
    def stop(): Unit = ()

// With settings:
//   max-column = 80
//   braces.new-anonymous = force-indent
```

### `braces.new-anonymous = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
val rOne =
  new Runnable {
    def run(): Unit  = ()
    def stop(): Unit = ()
  }

val qTwo =
  new Runnable:
    def run(): Unit  = ()
    def stop(): Unit = ()

// With settings:
//   max-column = 80
//   braces.new-anonymous = keep
```

## `braces.object`

**Default:** `force-braces`.

An object. The first body is written in braces, the second one braceless.

Before:

```scala
object One {
  val a = 1
  val b = 2
}

object Two:
  val a = 1
  val b = 2
```

### `braces.object = force-braces`

In curly braces.

```scala
object One {
  val a = 1
  val b = 2
}

object Two {
  val a = 1
  val b = 2
}

// With settings:
//   max-column = 80
//   braces.object = force-braces
```

### `braces.object = force-end`

Braceless, closed by an `end` marker when it spans several lines.

```scala
object One:
  val a = 1
  val b = 2
end One

object Two:
  val a = 1
  val b = 2
end Two

// With settings:
//   max-column = 80
//   braces.object = force-end
```

### `braces.object = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
object One:
  val a = 1
  val b = 2

object Two:
  val a = 1
  val b = 2

// With settings:
//   max-column = 80
//   braces.object = force-indent
```

### `braces.object = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
object One {
  val a = 1
  val b = 2
}

object Two:
  val a = 1
  val b = 2

// With settings:
//   max-column = 80
//   braces.object = keep
```

## `braces.package`

**Default:** `force-indent`.

A package clause. The first body is written in braces, the second one braceless.

Before:

```scala
package aOne {
  object C
}

package cTwo:
  object E
```

### `braces.package = force-braces`

A `package` clause is always written braceless, so this value is written like `force-indent`.

<!-- same-output-as: force-indent -->

In curly braces.

```scala
package aOne {
  object C
}

package cTwo:
  object E

// With settings:
//   max-column = 80
//   braces.package = force-braces
```

### `braces.package = force-end`

Braceless, closed by an `end` marker when it spans several lines.

```scala
package aOne {
  object C
}

package cTwo:
  object E
end cTwo

// With settings:
//   max-column = 80
//   braces.package = force-end
```

### `braces.package = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
package aOne {
  object C
}

package cTwo:
  object E

// With settings:
//   max-column = 80
//   braces.package = force-indent
```

### `braces.package = keep`

A `package` clause is always written braceless, so this value is written like `force-indent`.

<!-- same-output-as: force-indent -->

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
package aOne {
  object C
}

package cTwo:
  object E

// With settings:
//   max-column = 80
//   braces.package = keep
```

## `braces.package-object`

**Default:** `keep`.

A package object. The first body is written in braces, the second one braceless.

Before:

```scala
package object poOne {
  val x = 1
  val y = 2
}

package object ppTwo:
  val z = 3
  val w = 4
```

### `braces.package-object = force-braces`

In curly braces.

```scala
package object poOne {
  val x = 1
  val y = 2
}

package object ppTwo {
  val z = 3
  val w = 4
}

// With settings:
//   max-column = 80
//   braces.package-object = force-braces
```

### `braces.package-object = force-end`

Braceless, closed by an `end` marker when it spans several lines.

```scala
package object poOne:
  val x = 1
  val y = 2
end poOne

package object ppTwo:
  val z = 3
  val w = 4
end ppTwo

// With settings:
//   max-column = 80
//   braces.package-object = force-end
```

### `braces.package-object = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
package object poOne:
  val x = 1
  val y = 2

package object ppTwo:
  val z = 3
  val w = 4

// With settings:
//   max-column = 80
//   braces.package-object = force-indent
```

### `braces.package-object = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
package object poOne {
  val x = 1
  val y = 2
}

package object ppTwo:
  val z = 3
  val w = 4

// With settings:
//   max-column = 80
//   braces.package-object = keep
```

## `braces.test-case`

**Default:** `force-braces`.

The block of a test case. The first body is written in braces, the second one braceless. It has no `end` marker, so it rejects `force-end`.

Before:

```scala
def tcOne(): Unit =
  test("a parser") {
    parse("1")
    parse("2")
  }

def tdTwo(): Unit =
  test("a printer"):
    format(1)
    format(2)
```

### `braces.test-case = force-braces`

In curly braces.

```scala
def tcOne(): Unit =
  test("a parser") {
    parse("1")
    parse("2")
  }

def tdTwo(): Unit =
  test("a printer") {
    format(1)
    format(2)
  }

// With settings:
//   max-column = 80
//   braces.test-case = force-braces
```

### `braces.test-case = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
def tcOne(): Unit =
  test("a parser"):
    parse("1")
    parse("2")

def tdTwo(): Unit =
  test("a printer"):
    format(1)
    format(2)

// With settings:
//   max-column = 80
//   braces.test-case = force-indent
```

### `braces.test-case = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
def tcOne(): Unit =
  test("a parser") {
    parse("1")
    parse("2")
  }

def tdTwo(): Unit =
  test("a printer"):
    format(1)
    format(2)

// With settings:
//   max-column = 80
//   braces.test-case = keep
```

## `braces.test-group`

**Default:** `force-braces`.

The block of a group of tests. The first body is written in braces, the second one braceless. It has no `end` marker, so it rejects `force-end`.

Before:

```scala
def tgOne(): Unit =
  describe("a parser") {
    it("one") { parse("1") }
    it("two") { parse("2") }
  }

def thTwo(): Unit =
  describe("a printer"):
    it("one") { format(1) }
    it("two") { format(2) }
```

### `braces.test-group = force-braces`

In curly braces.

```scala
def tgOne(): Unit =
  describe("a parser") {
    it("one") { parse("1") }
    it("two") { parse("2") }
  }

def thTwo(): Unit =
  describe("a printer") {
    it("one") { format(1) }
    it("two") { format(2) }
  }

// With settings:
//   max-column = 80
//   braces.test-group = force-braces
```

### `braces.test-group = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
def tgOne(): Unit =
  describe("a parser"):
    it("one") { parse("1") }
    it("two") { parse("2") }

def thTwo(): Unit =
  describe("a printer"):
    it("one") { format(1) }
    it("two") { format(2) }

// With settings:
//   max-column = 80
//   braces.test-group = force-indent
```

### `braces.test-group = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
def tgOne(): Unit =
  describe("a parser") {
    it("one") { parse("1") }
    it("two") { parse("2") }
  }

def thTwo(): Unit =
  describe("a printer"):
    it("one") { format(1) }
    it("two") { format(2) }

// With settings:
//   max-column = 80
//   braces.test-group = keep
```

## `braces.trait`

**Default:** `force-braces`.

A trait. The first body is written in braces, the second one braceless.

Before:

```scala
trait One {
  def f: Int
  def g: Int
}

trait Two:
  def f: Int
  def g: Int
```

### `braces.trait = force-braces`

In curly braces.

```scala
trait One {
  def f: Int
  def g: Int
}

trait Two {
  def f: Int
  def g: Int
}

// With settings:
//   max-column = 80
//   braces.trait = force-braces
```

### `braces.trait = force-end`

Braceless, closed by an `end` marker when it spans several lines.

```scala
trait One:
  def f: Int
  def g: Int
end One

trait Two:
  def f: Int
  def g: Int
end Two

// With settings:
//   max-column = 80
//   braces.trait = force-end
```

### `braces.trait = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
trait One:
  def f: Int
  def g: Int

trait Two:
  def f: Int
  def g: Int

// With settings:
//   max-column = 80
//   braces.trait = force-indent
```

### `braces.trait = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
trait One {
  def f: Int
  def g: Int
}

trait Two:
  def f: Int
  def g: Int

// With settings:
//   max-column = 80
//   braces.trait = keep
```

## `braces.try`

**Default:** `force-braces`.

The bodies of a try. The first body is written in braces, the second one braceless.

Before:

```scala
def tOne(): Unit =
  try {
    a()
    b()
  } finally {
    c()
  }

def uTwo(): Unit =
  try
    a()
    b()
  finally
    c()
```

### `braces.try = force-braces`

In curly braces.

```scala
def tOne(): Unit =
  try {
    a()
    b()
  } finally {
    c()
  }

def uTwo(): Unit =
  try {
    a()
    b()
  } finally {
    c()
  }

// With settings:
//   max-column = 80
//   braces.try = force-braces
```

### `braces.try = force-end`

This construct has no `end` marker, so `force-end` is written like `force-indent`.

<!-- same-output-as: force-indent -->

Braceless, closed by an `end` marker when it spans several lines.

```scala
def tOne(): Unit =
  try
    a()
    b()
  finally
    c()

def uTwo(): Unit =
  try
    a()
    b()
  finally
    c()

// With settings:
//   max-column = 80
//   braces.try = force-end
```

### `braces.try = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
def tOne(): Unit =
  try
    a()
    b()
  finally
    c()

def uTwo(): Unit =
  try
    a()
    b()
  finally
    c()

// With settings:
//   max-column = 80
//   braces.try = force-indent
```

### `braces.try = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
def tOne(): Unit =
  try {
    a()
    b()
  } finally {
    c()
  }

def uTwo(): Unit =
  try
    a()
    b()
  finally
    c()

// With settings:
//   max-column = 80
//   braces.try = keep
```

## `braces.val`

**Default:** `force-indent`.

The body of a val. The first body is written in braces, the second one braceless.

Before:

```scala
val vvOne = {
  val a = 1
  a + 1
}

val vwTwo =
  val a = 1
  a + 1
```

### `braces.val = force-braces`

In curly braces.

```scala
val vvOne = {
  val a = 1
  a + 1
}

val vwTwo = {
  val a = 1
  a + 1
}

// With settings:
//   max-column = 80
//   braces.val = force-braces
```

### `braces.val = force-end`

Braceless, closed by an `end` marker when it spans several lines.

```scala
val vvOne =
  val a = 1
  a + 1
end vvOne

val vwTwo =
  val a = 1
  a + 1
end vwTwo

// With settings:
//   max-column = 80
//   braces.val = force-end
```

### `braces.val = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
val vvOne =
  val a = 1
  a + 1

val vwTwo =
  val a = 1
  a + 1

// With settings:
//   max-column = 80
//   braces.val = force-indent
```

### `braces.val = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
val vvOne = {
  val a = 1
  a + 1
}

val vwTwo =
  val a = 1
  a + 1

// With settings:
//   max-column = 80
//   braces.val = keep
```

## `braces.var`

**Default:** `force-indent`.

The body of a var. The first body is written in braces, the second one braceless.

Before:

```scala
var vrOne = {
  val a = 1
  a + 1
}

var vsTwo =
  val a = 1
  a + 1
```

### `braces.var = force-braces`

In curly braces.

```scala
var vrOne = {
  val a = 1
  a + 1
}

var vsTwo = {
  val a = 1
  a + 1
}

// With settings:
//   max-column = 80
//   braces.var = force-braces
```

### `braces.var = force-end`

Braceless, closed by an `end` marker when it spans several lines.

```scala
var vrOne =
  val a = 1
  a + 1
end vrOne

var vsTwo =
  val a = 1
  a + 1
end vsTwo

// With settings:
//   max-column = 80
//   braces.var = force-end
```

### `braces.var = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
var vrOne =
  val a = 1
  a + 1

var vsTwo =
  val a = 1
  a + 1

// With settings:
//   max-column = 80
//   braces.var = force-indent
```

### `braces.var = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
var vrOne = {
  val a = 1
  a + 1
}

var vsTwo =
  val a = 1
  a + 1

// With settings:
//   max-column = 80
//   braces.var = keep
```

## `braces.while`

**Default:** `force-braces`.

The body of a while loop. The first body is written in braces, the second one braceless.

Before:

```scala
def stepOne(): Unit =
  while c do {
    step()
    more()
  }

def stepTwo(): Unit =
  while c do
    step()
    more()
```

### `braces.while = force-braces`

In curly braces.

```scala
def stepOne(): Unit =
  while (c) {
    step()
    more()
  }

def stepTwo(): Unit =
  while (c) {
    step()
    more()
  }

// With settings:
//   max-column = 80
//   braces.while = force-braces
```

### `braces.while = force-end`

This construct has no `end` marker, so `force-end` is written like `force-indent`.

<!-- same-output-as: force-indent -->

Braceless, closed by an `end` marker when it spans several lines.

```scala
def stepOne(): Unit =
  while c do
    step()
    more()

def stepTwo(): Unit =
  while c do
    step()
    more()

// With settings:
//   max-column = 80
//   braces.while = force-end
```

### `braces.while = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
def stepOne(): Unit =
  while c do
    step()
    more()

def stepTwo(): Unit =
  while c do
    step()
    more()

// With settings:
//   max-column = 80
//   braces.while = force-indent
```

### `braces.while = keep`

A body written in braces is not kept as written here, so this value is currently written like `force-indent`.

<!-- same-output-as: force-indent -->

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
def stepOne(): Unit =
  while c do
    step()
    more()

def stepTwo(): Unit =
  while c do
    step()
    more()

// With settings:
//   max-column = 80
//   braces.while = keep
```

## `braces.single-expr.<construct>`

**Default:** `force-indent`.

The braces around a body that is a single expression, after the `=` of a definition or the `=>` of a lambda or of a case. A body of several statements follows the setting of its construct above: `braces.<construct> = force-braces` puts a body over several lines in braces (a body on one line never gets them), `force-indent` removes them, `keep` leaves them as written. The constructs are `case-body`, `def`, `given-alias`, `lambda-body`, `lazy-val`, `val` and `var`.

## `braces.single-expr.case-body`

**Default:** `force-indent`.

The braces around a single-expression body of a case.

Before:

```scala
def bodyOne(x: Int) = x match {
    case 1 => {
      someVeryLongFunctionCall(argumentOne, argumentTwo, argumentThree, argumentFour, argumentFive)
    }
  }

def bodyTwo(x: Int) = x match {
    case 1 =>
      someVeryLongFunctionCall(argumentOne, argumentTwo, argumentThree, argumentFour, argumentFive)
  }
```

### `braces.single-expr.case-body = force-braces`

In curly braces.

```scala
def bodyOne(x: Int) =
  x match {
    case 1 => {
      someVeryLongFunctionCall(
        argumentOne,
        argumentTwo,
        argumentThree,
        argumentFour,
        argumentFive,
      )
    }
  }

def bodyTwo(x: Int) =
  x match {
    case 1 => {
      someVeryLongFunctionCall(
        argumentOne,
        argumentTwo,
        argumentThree,
        argumentFour,
        argumentFive,
      )
    }
  }

// With settings:
//   max-column = 80
//   braces.single-expr.case-body = force-braces
```

### `braces.single-expr.case-body = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
def bodyOne(x: Int) =
  x match {
    case 1 =>
      someVeryLongFunctionCall(
        argumentOne,
        argumentTwo,
        argumentThree,
        argumentFour,
        argumentFive,
      )
  }

def bodyTwo(x: Int) =
  x match {
    case 1 =>
      someVeryLongFunctionCall(
        argumentOne,
        argumentTwo,
        argumentThree,
        argumentFour,
        argumentFive,
      )
  }

// With settings:
//   max-column = 80
//   braces.single-expr.case-body = force-indent
```

### `braces.single-expr.case-body = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
def bodyOne(x: Int) =
  x match {
    case 1 => {
      someVeryLongFunctionCall(
        argumentOne,
        argumentTwo,
        argumentThree,
        argumentFour,
        argumentFive,
      )
    }
  }

def bodyTwo(x: Int) =
  x match {
    case 1 =>
      someVeryLongFunctionCall(
        argumentOne,
        argumentTwo,
        argumentThree,
        argumentFour,
        argumentFive,
      )
  }

// With settings:
//   max-column = 80
//   braces.single-expr.case-body = keep
```

## `braces.single-expr.def`

**Default:** `force-indent`.

The braces around a single-expression body of def.

Before:

```scala
def sdOne = {
  someVeryLongFunctionCall(argumentOne, argumentTwo, argumentThree, argumentFour, argumentFive)
}

def seTwo =
  someVeryLongFunctionCall(argumentOne, argumentTwo, argumentThree, argumentFour, argumentFive)
```

### `braces.single-expr.def = force-braces`

In curly braces.

```scala
def sdOne = {
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )
}

def seTwo = {
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )
}

// With settings:
//   max-column = 80
//   braces.single-expr.def = force-braces
```

### `braces.single-expr.def = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
def sdOne =
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )

def seTwo =
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )

// With settings:
//   max-column = 80
//   braces.single-expr.def = force-indent
```

### `braces.single-expr.def = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
def sdOne = {
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )
}

def seTwo =
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )

// With settings:
//   max-column = 80
//   braces.single-expr.def = keep
```

## `braces.single-expr.given-alias`

**Default:** `force-indent`.

The braces around a single-expression body of given-alias.

Before:

```scala
given sagOne: Ordering[Int] = {
  veryLongOrderingExpression(argumentOne, argumentTwo, argumentThree, argumentFour)
}

given sahTwo: Ordering[Int] =
  veryLongOrderingExpression(argumentOne, argumentTwo, argumentThree, argumentFour)
```

### `braces.single-expr.given-alias = force-braces`

In curly braces.

```scala
given sagOne: Ordering[Int] = {
  veryLongOrderingExpression(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
  )
}

given sahTwo: Ordering[Int] = {
  veryLongOrderingExpression(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
  )
}

// With settings:
//   max-column = 80
//   braces.single-expr.given-alias = force-braces
```

### `braces.single-expr.given-alias = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
given sagOne: Ordering[Int] =
  veryLongOrderingExpression(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
  )

given sahTwo: Ordering[Int] =
  veryLongOrderingExpression(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
  )

// With settings:
//   max-column = 80
//   braces.single-expr.given-alias = force-indent
```

### `braces.single-expr.given-alias = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
given sagOne: Ordering[Int] = {
  veryLongOrderingExpression(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
  )
}

given sahTwo: Ordering[Int] =
  veryLongOrderingExpression(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
  )

// With settings:
//   max-column = 80
//   braces.single-expr.given-alias = keep
```

## `braces.single-expr.lambda-body`

**Default:** `force-indent`.

The braces around a single-expression body of lambda-body.

Before:

```scala
val slbOne: Int => Int = x => {
  someVeryLongFunctionCall(argumentOne, argumentTwo, argumentThree, argumentFour, argumentFive)
}

val slcTwo: Int => Int = x =>
  someVeryLongFunctionCall(argumentOne, argumentTwo, argumentThree, argumentFour, argumentFive)
```

### `braces.single-expr.lambda-body = force-braces`

In curly braces.

```scala
val slbOne: Int => Int =
  x => {
    someVeryLongFunctionCall(
      argumentOne,
      argumentTwo,
      argumentThree,
      argumentFour,
      argumentFive,
    )
  }

val slcTwo: Int => Int =
  x => {
    someVeryLongFunctionCall(
      argumentOne,
      argumentTwo,
      argumentThree,
      argumentFour,
      argumentFive,
    )
  }

// With settings:
//   max-column = 80
//   braces.single-expr.lambda-body = force-braces
```

### `braces.single-expr.lambda-body = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
val slbOne: Int => Int =
  x =>
    someVeryLongFunctionCall(
      argumentOne,
      argumentTwo,
      argumentThree,
      argumentFour,
      argumentFive,
    )

val slcTwo: Int => Int =
  x =>
    someVeryLongFunctionCall(
      argumentOne,
      argumentTwo,
      argumentThree,
      argumentFour,
      argumentFive,
    )

// With settings:
//   max-column = 80
//   braces.single-expr.lambda-body = force-indent
```

### `braces.single-expr.lambda-body = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
val slbOne: Int => Int =
  x => {
    someVeryLongFunctionCall(
      argumentOne,
      argumentTwo,
      argumentThree,
      argumentFour,
      argumentFive,
    )
  }

val slcTwo: Int => Int =
  x =>
    someVeryLongFunctionCall(
      argumentOne,
      argumentTwo,
      argumentThree,
      argumentFour,
      argumentFive,
    )

// With settings:
//   max-column = 80
//   braces.single-expr.lambda-body = keep
```

## `braces.single-expr.lazy-val`

**Default:** `force-indent`.

The braces around a single-expression body of lazy-val.

Before:

```scala
lazy val slvOne = {
  someVeryLongFunctionCall(argumentOne, argumentTwo, argumentThree, argumentFour, argumentFive)
}

lazy val slwTwo =
  someVeryLongFunctionCall(argumentOne, argumentTwo, argumentThree, argumentFour, argumentFive)
```

### `braces.single-expr.lazy-val = force-braces`

In curly braces.

```scala
lazy val slvOne = {
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )
}

lazy val slwTwo = {
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )
}

// With settings:
//   max-column = 80
//   braces.single-expr.lazy-val = force-braces
```

### `braces.single-expr.lazy-val = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
lazy val slvOne =
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )

lazy val slwTwo =
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )

// With settings:
//   max-column = 80
//   braces.single-expr.lazy-val = force-indent
```

### `braces.single-expr.lazy-val = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
lazy val slvOne = {
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )
}

lazy val slwTwo =
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )

// With settings:
//   max-column = 80
//   braces.single-expr.lazy-val = keep
```

## `braces.single-expr.val`

**Default:** `force-indent`.

The braces around a single-expression body of val.

Before:

```scala
val svOne = {
  someVeryLongFunctionCall(argumentOne, argumentTwo, argumentThree, argumentFour, argumentFive)
}

val swTwo =
  someVeryLongFunctionCall(argumentOne, argumentTwo, argumentThree, argumentFour, argumentFive)
```

### `braces.single-expr.val = force-braces`

In curly braces.

```scala
val svOne = {
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )
}

val swTwo = {
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )
}

// With settings:
//   max-column = 80
//   braces.single-expr.val = force-braces
```

### `braces.single-expr.val = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
val svOne =
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )

val swTwo =
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )

// With settings:
//   max-column = 80
//   braces.single-expr.val = force-indent
```

### `braces.single-expr.val = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
val svOne = {
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )
}

val swTwo =
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )

// With settings:
//   max-column = 80
//   braces.single-expr.val = keep
```

## `braces.single-expr.var`

**Default:** `force-indent`.

The braces around a single-expression body of var.

Before:

```scala
var srOne = {
  someVeryLongFunctionCall(argumentOne, argumentTwo, argumentThree, argumentFour, argumentFive)
}

var ssTwo =
  someVeryLongFunctionCall(argumentOne, argumentTwo, argumentThree, argumentFour, argumentFive)
```

### `braces.single-expr.var = force-braces`

In curly braces.

```scala
var srOne = {
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )
}

var ssTwo = {
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )
}

// With settings:
//   max-column = 80
//   braces.single-expr.var = force-braces
```

### `braces.single-expr.var = force-indent`

Braceless and without an `end` marker, the body only indented.

```scala
var srOne =
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )

var ssTwo =
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )

// With settings:
//   max-column = 80
//   braces.single-expr.var = force-indent
```

### `braces.single-expr.var = keep`

As the author wrote it: the first body keeps its braces, the second one stays braceless.

```scala
var srOne = {
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )
}

var ssTwo =
  someVeryLongFunctionCall(
    argumentOne,
    argumentTwo,
    argumentThree,
    argumentFour,
    argumentFive,
  )

// With settings:
//   max-column = 80
//   braces.single-expr.var = keep
```
