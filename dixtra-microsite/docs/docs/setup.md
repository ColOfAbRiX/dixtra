---
layout: docs
title: Setup
---
# Setup

## The sbt plugin

Dixtra runs against Scala 3 code, and the plugin requires sbt 2.

Add the plugin to `project/plugins.sbt`:

```scala
addSbtPlugin("com.colofabrix.scala" % "sbt-dixtra" % "@VERSION@")
```

The plugin is enabled in every project of the build. Its tasks are listed in [the sbt plugin](sbt-plugin.md).

## The command line

The command line is a single executable, `dixtra`. It formats the files you give it, whatever they are, and it is the same formatter the plugin and the extension use.

Download the executable for your platform from the [releases](https://github.com/ColOfAbRiX/dixtra/releases) page, and put it on your path:

```text
linux-x64    dixtra-linux-x64
macos-arm64  dixtra-darwin-arm64
windows-x64  dixtra-win32-x64.exe
```

Then format a directory:

```shell
dixtra src
```

The options are listed in [the command line](command-line.md).

## The VS Code extension

The extension formats the document of the editor with Dixtra when you run **Format Document** (`Shift+Alt+F`).

Install it with `code --install-extension dixtra-vscode-<platform>-<version>.vsix`, from the [releases](https://github.com/ColOfAbRiX/dixtra/releases) page, or from the VS Code marketplace when it is published there. It is described in [the VS Code extension](vscode.md).

## The library

The formatter alone, with no dependency other than scalameta:

```scala
libraryDependencies += "com.colofabrix.scala" %% "dixtra-core" % "@VERSION@"
```

Its API is described in [using Dixtra as a library](library.md).

## The first run

With the plugin installed, format the sources of a project:

```shell
sbt dixtraFormatAll
```

and check that they are formatted:

```shell
sbt dixtraCheckAll
```

Dixtra formats the code it understands and copies the rest, so the first run rarely fails: it reports the files it could not format, and the ones it could not parse.
