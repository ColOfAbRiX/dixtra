---
layout: docs
title: Intro
---

# Introduction

Dixtra is an opinionated formatter for Scala 3. Its primary goal is visual consistency and clarity across constructs.

Other Scala formatters let you tune typographic details: where the parentheses go, how the indentation works, when a comma is written.
Dixtra treats the code as having _visual features_ that must be formatted as a unit, and gives you fewer settings, so that the result is consistent by construction.

## Design Principles

These are the principles used to develop and design Dixtra:

### Format visual features

Dixtra targers visual features for formattings:

* alignment blocks
* blank lines
* bodies
* braces
* call site
* definition site
* selector chain

and as such they are formatted as a unit. For example, a definition site with multiple parameter groups
will uniformly format the parameters.

### Consistency across formattings

The same visual feature must format the same, everywhere in the code. For example, if a selector chain
breaks after 3 selectors, it always does so.

### One concept per line

Strongly favour having one "concept" per line. This can be a selector chain, the name of a function, a
member, a parameter and more. Scala allows so much flexibility that the code can become easily unreadable.

### Optinionated

Ultimately, this is a personal project aimed at solving long standing frustrations with the existing formatters
and, as such, it strongly favours my views on stlyling.

## Three ways to use it

- The sbt plugin, for a project that builds with sbt 2.
- The command line, `dixtra`, for any project, and for the files that are not part of a build.
- The VS Code extension, to format the file that is open in the editor.

All three run the same formatter, with the same configuration.

## Where to go next

- [Setup](setup.md): install Dixtra and format something.
- [Using Dixtra](usage.md): format, check, and format only what changed.
- [Configuration](configuration.md): the `.dixtra.conf` file, and the [settings reference](configuration.md) with an example for every value.
- [Rules and limitations](rules.md): the rules that have no setting, and what Dixtra cannot parse yet.
- [Building from source](building.md): build the formatter, the command line, the sbt plugin and the VS Code extension.
