---
layout: docs
title: Using Dixtra as a library
---

# Using Dixtra as a library

`dixtra-core` is the formatter alone, with no dependency other than scalameta:

```scala
libraryDependencies += "com.colofabrix.scala" %% "dixtra-core" % "@VERSION@"
```

```scala
import com.colofabrix.scala.dixtra.{ Config, Dixtra, SourceDialect }

Dixtra.format(source)                                       // the default configuration
Dixtra.format(source, Config.default.copy(maxColumn = 100))
Dixtra.format(source, Config.default, SourceDialect.sbt)    // an sbt 2 build file
```

`Dixtra.format` is a pure function from text to text. It returns `Right(formatted)`, or `Left` of a `FormatError`:

- `ParseError`: the source is not valid Scala 3;
- `Scala2Error`: the source is Scala 2;
- `UnsafeOutputError`: the output would not be equivalent to the input. This signals a formatter bug, not a problem with the source.

`Embedded.run` in `dixtra-cli` runs the command line without exiting the JVM, with Java types only: it hands each report line to a callback, one for the standard output and one for the standard error, and returns the exit code, 2 for invalid options. The sbt plugin calls it from its own class loader.

The API is documented in the [API documentation](api.md).
