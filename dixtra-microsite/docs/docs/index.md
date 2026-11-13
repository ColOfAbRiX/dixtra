---
layout: docs
title: Intro
---
# Introduction

Dixtra is an opinionated formatter for Scala 3. Its primary goal is visual consistency and clarity across constructs.

Other Scala formatters let you tune typographic details: where the parentheses go, how the indentation works, when a comma is written. Dixtra treats the code as having *visual features* that must be formatted as a unit, and gives you only a few settings, so that the result is consistent by construction.

> **The settings.** Dixtra is configured by a `.dixtra.conf` file. Every setting, with its default and an example for every value it takes, is in the [settings reference](configuration.md), one page per group.

## The three ways to use it

- The sbt plugin, for a project that builds with sbt 2.
- The command line, `dixtra`, for any project, and for the files that are not part of a build.
- The VS Code extension, to format the file that is open in the editor.

All three run the same formatter, with the same configuration.

## Principles

1. **Format visual features.** A construct is laid out as a whole: the braces of a `def`, the alignment of a group of definitions, the blank lines around a body.
2. **Consistency across formattings.** The same code, formatted twice, gives the same result. The same code, formatted in two projects, gives the same result.
3. **One operation per line.** A statement that does one thing gets one line.

## What Dixtra does not do

- It does not format Scala 2 code: a source that is Scala 2 is rejected with an error.
- It does not rewrite code: it only moves things around. When it has no rule for a snippet, the code is copied exactly as written.
- It is not a style guide you can bend line by line: most of the layout is fixed, and the settings that exist change whole groups of constructs.

## Where to go next

- [Setup](setup.md): install Dixtra and format something.
- [Using Dixtra](usage.md): format, check, and format only what changed.
- [Configuration](configuration.md): the `.dixtra.conf` file, and the [settings reference](configuration.md) with an example for every value.
- [Rules and limitations](rules.md): the rules that have no setting, and what Dixtra cannot parse yet.
