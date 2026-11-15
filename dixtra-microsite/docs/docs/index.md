---
layout: docs
title: Intro
---

# Introduction

Dixtra is an opinionated formatter for Scala 3. Its primary goal is visual consistency and clarity across constructs.

Other Scala formatters let you tune typographic details: where the parentheses go, how the indentation works, when a comma is written.
Dixtra treats the code as having _visual features_ that must be formatted as a unit, and gives you fewer settings, so that the result is consistent by construction.

**The settings.** Dixtra is configured by a `.dixtra.conf` file. Every setting, with its default and an example for every value it takes, is in the [settings reference](configuration.md), one page per group.

## The three ways to use it

- The sbt plugin, for a project that builds with sbt 2.
- The command line, `dixtra`, for any project, and for the files that are not part of a build.
- The VS Code extension, to format the file that is open in the editor.

All three run the same formatter, with the same configuration.

## Principles

1. **Format visual features.** A construct is laid out as a whole: the braces of a `def`, the alignment of a group of definitions, the blank lines around a body.
2. **Consistency across formattings.** The same code, formatted twice, gives the same result. The same code, formatted in two projects, gives the same result.
3. **One operation per line.** A statement that does one thing gets one line.

## Where to go next

- [Setup](setup.md): install Dixtra and format something.
- [Using Dixtra](usage.md): format, check, and format only what changed.
- [Configuration](configuration.md): the `.dixtra.conf` file, and the [settings reference](configuration.md) with an example for every value.
- [Rules and limitations](rules.md): the rules that have no setting, and what Dixtra cannot parse yet.
- [Building from source](building.md): build the formatter, the command line, the sbt plugin and the VS Code extension.
