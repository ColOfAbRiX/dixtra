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

## The top-level settings

| Setting          | Default | What it does                                                                                                                                                                                                           |
| ---------------- | ------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `config-version` | `1.0.0` | The version of the schema of the configuration. A configuration without it is read as the current version; one written for a later version, or for another major version, is rejected instead of being half understood |
| `max-column`     | `120`   | The longest a line can be. A construct whose one-line form does not fit within this width breaks                                                                                                                       |
| `compat213`      | `false` | Whether the output must also be valid Scala 2.13, for the sources of a project cross-compiled for Scala 2.13 and Scala 3                                                                                              |

### Configuration version

Dixtra follows [Semantic versioning](https://semver.org/) where given a version number `MAJOR.MINOR.PATCH` the:

* `MAJOR` version indicates incompatible API changes
* `MINOR` version indicates new functionalities in a backward compatible manner
* `PATCH` version indicates backward compatible bug fixes

A configuration written for a another minor or path version of the same major version is guaranteed to be backward compatible.

### Column width

This settings ensure that no line will exceed this width.

### Scala 2.13 compatibility

`compat213` keeps the output valid Scala 2.13 too, for the sources of a project cross-compiled for Scala 2.13 and Scala 3. When it is `true`, Dixtra enforces the settings that would otherwise write Scala 3 syntax, whatever the rest of the configuration says:

| Setting                          | Value                                              |
| -------------------------------- | -------------------------------------------------- |
| `braces.<construct>`             | `keep`, except `package` and `try`: `force-braces` |
| `braces.single-expr.<construct>` | `keep`                                             |
| `body.semicolon-statements`      | `keep`                                             |

`braces.style`, `braces.imports` and the other body settings keep their values. A source that is valid Scala 2.13 too keeps the braces of the Scala 2 form, and a `package` clause and a `try` written without braces get them:

```scala
def f = {
  a()
  b()
}
```

A source that is valid only in Scala 3 is still formatted, but the setting does not make it valid Scala 2.13. A source that is Scala 2 is still rejected. The setting can be set in a [`path-overrides`](reference/path-overrides.md) entry, so only some files are formatted in compatibility mode.

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

# Ignoring the formatting

The code between a `dixtra: off` comment and a `dixtra: on` comment stays exactly as written:

```scala
object Matrices {
  val a = 1

  // dixtra: off
  val identity = Array(
    1, 0,
    0, 1,
  )
  // dixtra: on
}
```

- **Unclosed regions.** Without a `dixtra: on`, the region runs to the end of the file. A `// dixtra: off` at the top of a file leaves the whole file as written.
- **Nesting.** An `off` inside a region, or an `on` outside one, does nothing.
- **The markers themselves** are laid out like any other comment. Only the code between them is protected.

## Disabling Dixtra for a subproject

The `dixtra: off` markers protect regions of one file. To leave the sources and build files of a whole sbt subproject as written, turn the plugin off for that subproject:

```scala
lazy val myProject = 
  project
    .in(file("myProject"))
    .disablePlugins(DixtraPlugin)
```

The plugin then adds nothing to the subproject: its sources are not formatted on compile, and none of the `dixtraFormat*` or `dixtraCheck*` tasks run for it.
