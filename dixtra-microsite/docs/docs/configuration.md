---
layout: docs
title: Configuration
---

# Configuration

Dixtra gets its configuration from `.dixtra.conf` files, written in [HOCON](https://github.com/lightbend/config/blob/master/HOCON.md), the format of the reference configuration of the command line.

```conf
config-version = 1.0.0

max-column = 100

braces { style = stroustrup, if = force-braces }
paths.excluded = ["**/generated/**"]
```

A project overrides only the settings it changes. Every setting that is not written keeps its default, listed with an example for every value in the [settings reference](#the-settings-reference).

## The top-level settings

| Setting          | Default | What it does                                                                                                                                                                                                           |
| ---------------- | ------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `config-version` | `1.0.0` | The version of the schema of the configuration. A configuration without it is read as the current version; one written for a later version, or for another major version, is rejected instead of being half understood |
| `max-column`     | `120`   | The longest a line can be. A construct whose one-line form does not fit within this width breaks                                                                                                                       |

## The settings reference

The settings are split into one page per group. Each page lists the settings, their defaults and an example for every value they take.

| Group                                           | What it holds                                                                                |
| ----------------------------------------------- | -------------------------------------------------------------------------------------------- |
| [braces](reference/braces.md)                   | The braces, the `end` markers or the indentation of every construct, and where the braces go |
| [body](reference/body.md)                       | Whether a body stays on the line of its separator, or goes below it                          |
| [selector-chain](reference/selector-chain.md)   | When a chain of selections breaks, and how                                                   |
| [alignment](reference/alignment.md)             | What lines up in columns                                                                     |
| [blank-lines](reference/blank-lines.md)         | The blank lines at the edges of the bodies, between groups and before the scaladocs          |
| [call-site](reference/call-site.md)             | The arguments of a call and the elements of a tuple: groups and commas                       |
| [definition-site](reference/definition-site.md) | The parameters of a `def` or of a class, and the selectors of an import: groups and commas   |
| [overflow](reference/overflow.md)               | What happens to what goes past `max-column`                                                  |
| [housekeeping](reference/housekeeping.md)       | The trailing whitespace, the line break at the end of the file, the line endings             |
| [paths](reference/paths.md)                     | The files of a directory to format                                                           |
| [path-overrides](reference/path-overrides.md)   | Other settings for some of the files                                                         |

## Which file applies

For each file it formats, Dixtra uses the nearest `.dixtra.conf` in the directory of the file, or in a directory above it. Without one, it uses the defaults.

The lookup stops at the first configuration found: a `.dixtra.conf` in a subdirectory does not inherit the one of the directory above it. Put the settings that apply to a whole project in the configuration of its root, and the ones that apply to a subtree in the configuration of that subtree.

## Replacing the configuration

A file given with `--config-path`, or text given with `--config-string`, replaces the lookup: no `.dixtra.conf` is read at all.

```shell
dixtra --config-string "max-column = 80, braces.style = allman" src
dixtra --config-path build/.dixtra.conf src
```

The same options exist in the sbt plugin, as `dixtraConfig`.

## Settings for some files

`path-overrides` gives some files other settings. Each key is a glob, with the syntax of `paths` (`*`, `**`, `?`), and its section holds the settings of the files it matches:

```conf
path-overrides {
  "src/main/scala/legacy/**" { trailing-commas = never }
  "build.sbt" { housekeeping.enforce-eof = false }
}
```

- **Globs** are relative to the directory of the configuration file that declares them, or to the working directory for `--config-string`. A file outside that directory never matches. A glob holding a `.` must be quoted, or HOCON reads the `.` as a section separator.
- **Settings.** A section can set any setting or alias but `paths`, `config-version` and `path-overrides`: those are ignored with a warning, as unknown settings are.
- **Precedence.** A file gets the settings of every glob its path matches, over the rest of the configuration: a setting in an override beats the same setting written outside `path-overrides`, directly or through an alias.
- **Conflicts.** Two matching globs that give a setting different values fail that file only, with the error `<setting> is set to different values by path-overrides."<glob>" and path-overrides."<glob>"`. An invalid value in an override fails the whole configuration.

The details of the globs are in [path overrides](reference/path-overrides.md).

## Aliases

An alias is a name that sets one or more settings at once. Most of them are named after the construct they apply to, and the aspect they change:

```conf
path-overrides {
  "src/main/scala/legacy/**" {
    "def.braces" = force-braces
    "def.body" = always-break
  }
}
```

Here, `"def.braces"` sets `braces.def`, and `"def.body"` sets `body.def`. The aliases that are not about a single construct are `"trailing-commas"`, which sets the trailing commas of the call sites and of the definition sites, and `"scaladoc.align-tags"`, which sets `alignment.scaladoc.tags`. The aliases `"test-block.braces"`, `"test-block.body"` and `"test-block.body-edges"` set both the `test-case` and the `test-group` setting: they are the names Dixtra used before it told the tests from the groups of tests.

An alias is resolved when the configuration is loaded, on the settings that were written. A setting given more than once, directly or through aliases, must get the same value every time, otherwise the configuration is rejected.

## When the configuration is wrong

- An unknown key is ignored, with a warning naming it.
- An invalid value is rejected: the file is not formatted.
- A configuration written for another version of the schema, or for a later major version, is rejected instead of being half understood. The version is the `config-version` setting, currently `1.0.0`.
