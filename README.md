[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.colofabrix.scala/dixtra-core_3.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.colofabrix.scala/dixtra-core_3)
[![Tests Status](https://github.com/ColOfAbRiX/dixtra/actions/workflows/tests.yml/badge.svg)](https://github.com/ColOfAbRiX/dixtra/actions/workflows/tests.yml)

# Dixtra

Dixtra is an opinionated formatter for Scala 3. Its primary goal is visual consistency across constructs.

Unlike other Scala formatters, that allow you to tune typographic formatting like parentheses,
indentation, commas, etc., Dixtra treats the code as having visual features that can be formatted as
a unit. On top of this, Dixtra doesn't give the users full control of everything to limit the potential
for inconsistent results.

## Example 

This function definition:

```scala
def total[A](xs: List[Int], limit: Int, multiplier: Double)(using ord: Ordering[Int]): Double = xs.filter(_ > limit).map(_ * multiplier).sum
```

is composed of:

* a function definition
* one type parameter list
* one parameter list
* a body containing a selector chain

each of these parts are formatted indipendently and as a unit:

```scala
def total[A](
  xs: List[Int],
  limit: Int,
  multiplier: Double
)(using
  ord: Ordering[Int]
): Double =
  xs
    .filter(_ > limit)
    .map(_ * multiplier)
    .sum
```

## Installation and configuration

The plugin requires sbt 2. Add this line to `project/plugins.sbt`:

```scala
addSbtPlugin("com.colofabrix.scala" % "sbt-dixtra" % "1.0.0-RC1")
```

Dixtra gets its configuration from `.dixtra.conf` files. The formatting options are documented, with examples,
in [`reference.conf`](dixtra-cli/src/main/resources/reference.conf), which also holds the default of every setting.

## Principles (WIP)

### Format visual features

alignment
blank-lines
body
braces
call-site
definition-site
selector-chain
housekeeping
paths
path-overrides
overflow

### Consistency across formattings

### One operation per line

## The sbt plugin

Once installed (see above) it is enabled automatically in every project.

### Tasks

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

The tasks cover the unmanaged source directories of a configuration, so generated sources are left alone.

### Settings

| Setting              | Default              | What it does                                                |
| -------------------- | -------------------- | ----------------------------------------------------------- |
| `dixtraVersion`      | the plugin's version | The version of Dixtra to format with                        |
| `dixtraConfig`       | `None`               | A configuration file, instead of the nearest `.dixtra.conf` |
| `dixtraChangedSince` | `None`               | A git ref: format or check only the files changed since it  |
| `dixtraOnCompile`    | `false`              | Format the sources of a configuration before compiling them |

```scala
// build.sbt
dixtraOnCompile    := true
dixtraChangedSince := Some("origin/main")
```

## The command line

The command line is `dixtra-cli`. From a clone of this repository, run it with `sbt "run <options> <paths>"`, or
build it as a native executable that starts instantly (see [Building from source](#building-from-source)).

```text
Dixtra [--config-string <hocon> | --config-path <file>] [--max-column <n>] [--check] [--changed-since <ref>]
      [--quiet] <path>...
```

| Option                       | What it does                                                            |
| ---------------------------- | ----------------------------------------------------------------------- |
| `<path>...`                  | Files and directories to format. At least one is required               |
| `--check`, `-c`              | Report the files that are not formatted, without writing them           |
| `--max-column <n>`, `-w <n>` | The longest a line can be, over every `max-column` of the configuration |
| `--config-string <hocon>`    | The configuration, as HOCON text, instead of the `.dixtra.conf` files   |
| `--config-path <file>`       | The configuration file to use, instead of the `.dixtra.conf` files      |
| `--changed-since <ref>`      | Only the files changed since a git ref (see below)                      |
| `--quiet`, `-q`              | List only the files that change or fail, not the unchanged ones         |
| `--version`, `-v`            | Print the version                                                       |
| `--help`                     | Print the usage                                                         |

With `--changed-since`, a file counts as changed when it was committed since the ref, has uncommitted changes, or is new
and not ignored by git. With `HEAD`, only the uncommitted changes count.

## The VS Code extension

The `dixtra-vscode/` folder holds an extension that formats the document of the editor with Dixtra when you run
**Format Document** (`Shift+Alt+F`). 

A document with unsaved changes is saved first, and Dixtra runs on the file on disk, so the nearest `.dixtra.conf` and its `path-overrides` apply as in
the command line.

To build it, run `sbt dixtraVsCode/vscodePackage` from the repository root. It writes
`dist/dixtra-vscode-<version>.vsix`, installed with `code --install-extension dist/dixtra-vscode-<version>.vsix`;
[Building the VS Code extension](#building-the-vs-code-extension) describes the task, its requirements and every
output it produces in `dist/`.

## Configuration

A project overrides the defaults of the settings it changes, in a HOCON file named `.dixtra.conf`:

```conf
config-version = 1.0.0

max-column = 100
braces { style = stroustrup, if = force-braces }
paths.excluded = ["**/generated/**"]
```

For each path it formats, Dixtra uses the nearest `.dixtra.conf` in the path's directory or above it. Without one, it
uses the defaults. A file given with `--config-path` (or `dixtraConfig`), or text given with `--config-string`, replaces the lookup.

### Settings for some files

`path-overrides` gives some files other settings. Each key is a glob, with the syntax of `paths` (`*`, `**`, `?`), and
its section holds the settings of the files it matches:

```conf
path-overrides {
  "src/main/scala/legacy/**" { trailing-commas = never }
  "build.sbt" { housekeeping.enforce-eof = false }
}
```

- **Globs** are relative to the directory of the configuration file that declares them, or to the working directory
  for `--config-string`. A file outside that directory never matches. A glob holding a `.` must be quoted, or HOCON
  reads the `.` as a section separator.
- **Settings.** A section can set any setting or alias but `paths`, `config-version` and `path-overrides`: those are
  ignored with a warning, as unknown settings are.
- **Precedence.** A file gets the settings of every glob its path matches, over the rest of the configuration: a
  setting in an override beats the same setting written outside `path-overrides`, directly or through an alias.
- **Conflicts.** Two matching globs that give a setting different values fail that file only, with the error
  `<setting> is set to different values by path-overrides."<glob>" and path-overrides."<glob>"`. An invalid value in
  an override fails the whole configuration.

The default is `path-overrides {}`.

## Fixed rules and limitations

Some rules have no setting on purpose:

- **Indentation** is 2 spaces.
- **Spaces.** A run of spaces between two tokens on a line becomes one, except where Dixtra aligns code.
- **Blank lines.** A run of blank lines becomes one.
- **Comments** on lines of their own are reflowed to `max-column.
- **Scaladoc** gets the javadoc layout.

Some code is copied exactly as written, because Dixtra has no rule for it yet:

- Among the Scala 3 syntax it does not parse yet is `into` on a type (Scala 3.9).

## Ignoring formatting for snippets of code

The code between a `dixtra: off` comment and a `dixtra: on` comment stays exactly as written:

```scala
object Matrices {
  val a = 1

  // dixtra: off
  val identity = Array(
    1, 0, 0,
    0, 1, 0,
    0, 0, 1,
  )
  // dixtra: on
}
```

- **Unclosed regions.** Without `dixtra: on`, the region runs to the end of the file. A `// dixtra: off` at the top of
  a file leaves the whole file as written.
- **Nesting.** An `off` inside a region, or an `on` outside one, does nothing.
- **The markers themselves** are laid out like any other comment. Only the code between them is protected.

## Using Dixtra as a library

`dixtra-core` is the formatter alone, with no dependency other than scalameta:

```scala
libraryDependencies += "com.colofabrix.scala" %% "dixtra-core" % "1.0.0-RC1"
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
- `UnsafeOutputError`: the output would not be equivalent to the input. This signals a formatter bug, not a problem
  with the source.

Reading `.dixtra.conf` files is part of `dixtra-cli` (`ConfigCli`), not of the core, and so are the aliases.

`Embedded.run` in `dixtra-cli` runs the command line without exiting the JVM, with Java types only: it hands each
report line to a callback, one for the standard output and one for the standard error, and returns the exit code,
2 for invalid options. The sbt plugin calls it from its own class loader.

## Building from source

The build needs sbt 2.0 and a JDK that sbt 2 supports (17 or later). It has four projects:

| Project          | Artifact        | Contents                                                    |
| ---------------- | --------------- | ----------------------------------------------------------- |
| `dixtra-core/`   | `dixtra-core`   | the formatter (Scala 3.9)                                   |
| `dixtra-cli/`    | `dixtra-cli`    | the command line, the configuration files, `reference.conf` |
| `dixtra-sbt/`    | `sbt-dixtra`    | the sbt plugin                                              |
| `dixtra-vscode/` | `dixtra-vscode` | the sbt plugin                                              |

```shell
sbt publishLocal                    # publish the three artifacts to the local Ivy repository
sbt test                            # the tests of the formatter and the command line
sbt sbtDixtra/scripted              # the tests of the plugin
sbt "run --check dixtra-core/src"   # the command line from the build
```

### The SBT Plugin

To build and deploy the sbt plugin locally, run:

```shell
sbt publishLocal
```

and the artifacts will be published under `<local_ivy2>/local/com.colofabrix.scala`

### The native CLI executable

`sbt dixtraCli/nativeImage` builds the command line with GraalVM `native-image`, into
`target/out/jvm/scala-3.9.0/dixtra-cli/native/dixtra` (`dixtra.exe` on Windows).

It needs:

- `GRAALVM_HOME` pointing to a GraalVM installation (GraalVM Community 25 or later);
- on Windows, the Visual Studio C++ build tools (the "Desktop development with C++" workload). If `native-image`
  doesn't find them, start sbt from an "x64 Native Tools Command Prompt", or after running `vcvars64.bat`.

### Building the VS Code extension

`sbt dixtraVsCode/vscodePackage` builds the extension of `dixtra-vscode/` (see
[The VS Code extension](#the-vs-code-extension) for what it does).

It needs:

- `GRAALVM_HOME` pointing to a GraalVM installation (GraalVM Community 25 or later);
- on Windows, the Visual Studio C++ build tools (the "Desktop development with C++" workload). If `native-image`
  doesn't find them, start sbt from an "x64 Native Tools Command Prompt", or after running `vcvars64.bat`.

Everything it produces lands in `dist/`:

```text
dist/
├── dixtra-vscode-1.0.0-RC1.vsix   the extension, installable with code --install-extension
├── win32-x64/dixtra.exe            the executable bundled with the extension
└── linux-x64/dixtra                built, but not bundled for now
```

Install it with `code --install-extension dist/dixtra-vscode-<version>.vsix` or through the UI interface.


## Contributing

Contributions are welcome! See [CONTRIBUTING](.github/CONTRIBUTING.md) for how to build the project, run its tests and
submit a pull request, and the [Code of conduct](.github/CODE_OF_CONDUCT.md) that everyone participating in it is
expected to follow.

## Changelog

You can check [the changelog of Dixtra here](CHANGELOG.md)

## License

Dixtra is released under a "MIT" license. See [LICENSE](LICENSE) for specifics and copyright
declaration.

## Author Information

[Fabrizio Colonna](mailto:colofabrix@tin.it)
