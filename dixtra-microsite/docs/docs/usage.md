---
layout: docs
title: Using Dixtra
---

# Using Dixtra

The settings that change how the code is formatted are in the [settings reference](configuration.md): a page per group, with an example for every value a setting takes.

## With the sbt plugin

The plugin adds the tasks listed in [the sbt plugin](sbt-plugin.md). The ones you use most:

```shell
sbt dixtraFormatAll       # format the sources of Compile and Test, and the build files
sbt dixtraCheckAll        # fail when a source is not formatted
sbt "dixtraFormatOnly src/main/scala/Main.scala"
```

`dixtraFormatAll` formats the sources of the `Compile` and `Test` configurations, and the files of the build. The generated sources are left alone: the tasks cover the unmanaged source directories only.

## Only the files that changed

Formatting a whole project on every save is wasteful when only a few files changed. Both the plugin and the command line can restrict the work to the files that changed since a git ref:

```scala
// build.sbt
dixtraChangedSince := Some("origin/main")
```

```shell
dixtra --changed-since origin/main src
```

A file counts as changed when it was committed since the ref, has uncommitted changes, or is new and not ignored by git. With `HEAD`, only the uncommitted changes count.

## From the command line

The command line formats the files and the directories you give it, and writes the result back:

```shell
dixtra src
```

It reports every file it formatted, and every file it could not format. With `--check` it does not write anything, and it fails when a file would change: that is what a CI job wants.

```shell
dixtra --check src
```

The options are listed in [the command line](command-line.md).

## In the editor

The VS Code extension formats the open document with **Format Document**. A document with unsaved changes is saved first, and Dixtra runs on the file on disk, so the nearest `.dixtra.conf` and its `path-overrides` apply as in the command line.

## When Dixtra cannot format a file

A file is reported, and left as it is, when:

- it is not valid Scala 3, or it is Scala 2: Dixtra rejects it with the error of the parser;
- it holds a construct that Dixtra does not parse yet;
- the formatted result would not be equivalent to the input, which signals a bug in the formatter: the file is left untouched and the error is reported.

See [rules and limitations](rules.md) for the known limitations.
