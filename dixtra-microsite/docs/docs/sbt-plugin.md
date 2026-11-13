---
layout: docs
title: The sbt plugin
---

# The sbt plugin

The plugin requires sbt 2, and runs against Scala 3 code. It is enabled in every project of the build, and it does not depend on the build at compile time: it resolves Dixtra when it runs and loads it in a class loader of its own, so that Dixtra keeps its Scala version inside sbt.

```scala
// project/plugins.sbt
addSbtPlugin("com.colofabrix.scala" % "sbt-dixtra" % "$VERSION")
```

## Tasks

| Task                       | What it does                                                                            |
| -------------------------- | --------------------------------------------------------------------------------------- |
| `dixtraFormat`             | Formats the sources of a configuration (`Compile`; `Test / dixtraFormat` for the tests) |
| `dixtraCheck`              | Fails when a source of a configuration is not formatted                                 |
| `dixtraFormatAll`          | Formats the sources of `Compile` and `Test`, and the build files                        |
| `dixtraCheckAll`           | Fails when a source of `Compile` or `Test`, or a build file, is not formatted           |
| `dixtraFormatSbt`          | Formats the build: the `.sbt` files, and the `.sbt` / `.scala` files in `project/`      |
| `dixtraCheckSbt`           | Fails when a file of the build is not formatted                                         |
| `dixtraFormatOnly <files>` | Formats the given files, relative to the project directory                              |
| `dixtraClasspath`          | The jars of `dixtra-cli` and its dependencies, as resolved for `dixtraVersion`          |

The tasks cover the unmanaged source directories of a configuration, so the generated sources are left alone.

## Settings

| Setting              | Default            | What it does                                                |
| -------------------- | ------------------ | ----------------------------------------------------------- |
| `dixtraVersion`      | the plugin version | The version of Dixtra to format with                        |
| `dixtraConfig`       | `None`             | A configuration file, instead of the nearest `.dixtra.conf` |
| `dixtraChangedSince` | `None`             | A git ref: format or check only the files changed since it  |
| `dixtraOnCompile`    | `false`            | Format the sources of a configuration before compiling them |

```scala
// build.sbt
dixtraOnCompile    := true
dixtraChangedSince := Some("origin/main")
```

## In a CI job

The check tasks fail the build when a file is not formatted, which is what a CI job wants:

```shell
sbt dixtraCheckAll
```

To check only the files that changed, and to keep the job fast:

```shell
sbt "set dixtraChangedSince := Some(\"origin/main\")" dixtraCheckAll
```
