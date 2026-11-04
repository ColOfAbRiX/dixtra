[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.colofabrix.scala/dixtra-core_3.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.colofabrix.scala/dixtra-core_3)
[![Tests Status](https://github.com/ColOfAbRiX/dixtra/actions/workflows/tests.yml/badge.svg)](https://github.com/ColOfAbRiX/dixtra/actions/workflows/tests.yml)

# Dixtra

Dixtra is an opinionated formatter for Scala 3. The primary goal is visual consistency.

Unlike other Scala formatters, that allow you to tune typographic formatting like parentheses,
indentation, commas, etc., Dixtra treats the code as having visual features that can be formatted as
a unit. On top of this, Dixtra doesn't give the users full control to limit potential for unwanted
results.

For example this function definition:

```scala
def total[A](xs: List[Int], limit: Int, multiplier: Double)(using ord: Ordering[Int]): Double = xs.filter(_ > limit).map(_ * multiplier).sum
```

has a definition with a single parameter list and a body containing a selector chain and each of
these parts are formatted indipendently and as a unit:

```scala
def total(
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

This page describes how to install and run dixtra, and how it behaves. The formatting options
themselves are documented, with examples, in
[`reference.conf`](dixtra-cli/src/main/resources/reference.conf), which also holds the default of
every setting.

## Principles - TODO, rewrite section

- **Whole constructs.** A construct (an argument list, a chain of calls, a signature, an if/else) is laid out either
  fully inline or fully exploded. Nothing is ever partly broken.
- **Two triggers.** A construct breaks when its one-line form goes past `max-column`, or when the author wrote a line
  break inside it. Otherwise it is joined on one line.
- **The author decides where the layout leaves a choice.** A line break written on purpose is a signal: Dixtra keeps
  it, and it doesn't add breaks the width doesn't call for.
- **A multi-line value goes below its `=`.** When the right-hand side of a definition doesn't fit on one line, it
  starts on the line after the `=`, one level in.
- **No lines of lone delimiters.** No line holds only `)(` or `](`. The next element is glued on instead.
- **Stable.** Formatting a formatted file changes nothing, and the output doesn't depend on the order of the files.

## What gets formatted

- **File types.** Scala 3 sources (`.scala`) and sbt 2 build files (`.sbt`, with top-level statements). Scripts
  (`.sc`) are not picked up from directories.
- **Dialect.** A `.sbt` file is parsed as an sbt 2 build file, which allows statements at the top level. Any other file
  is parsed as Scala 3.
- **Directories.** Dixtra walks them recursively, and formats their files in path order. Below the walked directory,
  it skips every hidden directory (`.git`, `.bsp`, `.metals`, ...) and every directory named `target`.
- **The `paths` setting** narrows down what a directory walk picks up, with globs relative to the walked directory (see
  `paths` in `reference.conf`). A file is picked up when it matches an `included` glob, and neither it nor a directory
  above it matches an `excluded` one:

  ```conf
  paths {
    included = ["src/**"]
    excluded = ["**/generated/**", "legacy/*.scala"]
  }
  ```

  `**/` matches any number of directories, `*` anything within one path segment, and `?` one character.

## The sbt plugin

The plugin needs sbt 2. Add it to `project/plugins.sbt`:

```scala
addSbtPlugin("com.colofabrix.scala" % "sbt-dixtra" % "1.0.0-RC1")
```

It is enabled automatically in every project. The plugin itself doesn't depend on dixtra: when a task first runs, it
downloads `dixtra-cli` and runs it in a class loader of its own. Dixtra therefore keeps its own Scala version
whatever sbt runs on.

### Tasks

| Task                       | What it does                                                                            |
|----------------------------|-----------------------------------------------------------------------------------------|
| `dixtraFormat`             | Formats the sources of a configuration (`Compile`; `Test / dixtraFormat` for the tests) |
| `dixtraCheck`              | Fails when a source of a configuration is not formatted                                 |
| `dixtraFormatAll`          | Formats the sources of `Compile` and `Test`, and the build files                        |
| `dixtraCheckAll`           | Fails when a source of `Compile` or `Test`, or a build file, is not formatted           |
| `dixtraFormatSbt`          | Formats the build: the `.sbt` files, and the `.sbt` / `.scala` files in `project/`      |
| `dixtraCheckSbt`           | Fails when a file of the build is not formatted                                         |
| `dixtraFormatOnly <files>` | Formats the given files, relative to the project directory                              |
| `dixtraClasspath`          | The jars of `dixtra-cli` and its dependencies, as resolved for `dixtraVersion`          |

The tasks cover the unmanaged source directories of a configuration, so generated sources are left alone. To get them
in a custom configuration, add `inConfig(MyConfig)(DixtraPlugin.configSettings)`. The build tasks take the `.sbt`
files of the project directory and the `.sbt` and `.scala` files directly in `project/`, not those of its
subdirectories. `dixtraFormatOnly` formats the files it is given even when `dixtraChangedSince` is set.

### Settings

| Setting              | Default              | What it does                                                |
|----------------------|----------------------|-------------------------------------------------------------|
| `dixtraVersion`      | the plugin's version | The version of Dixtra to format with                        |
| `dixtraConfig`       | `None`               | A configuration file, instead of the nearest `.dixtra.conf` |
| `dixtraChangedSince` | `None`               | A git ref: format or check only the files changed since it  |
| `dixtraOnCompile`    | `false`              | Format the sources of a configuration before compiling them |

```scala
// build.sbt
dixtraOnCompile    := true
dixtraChangedSince := Some("origin/main")
```

With `dixtraOnCompile`, a file that can't be formatted only logs a warning and is compiled as it is.

The plugin logs the report of the command line (see [The command line](#the-command-line)): the formatted files and
the summary at info level, the unchanged files at debug level, the skipped files and the configuration warnings at
warn level, and the failures at error level. A check task that finds a file not formatted, or a task where a file
failed, then fails with one message: the files at fault are already in the log.

## The command line

The command line is `dixtra-cli`. From a clone of this repository, run it with `sbt "run <options> <paths>"`, or
build it as a native executable that starts instantly (see [Building from source](#building-from-source)).

```text
Dixtra [--config-string <hocon> | --config-path <file>] [--max-column <n>] [--check] [--changed-since <ref>]
      [--quiet] <path>...
```

| Option                       | What it does                                                            |
|------------------------------|-------------------------------------------------------------------------|
| `<path>...`                  | Files and directories to format. At least one is required               |
| `--check`, `-c`              | Report the files that are not formatted, without writing them           |
| `--max-column <n>`, `-w <n>` | The longest a line can be, over every `max-column` of the configuration |
| `--config-string <hocon>`    | The configuration, as HOCON text, instead of the `.dixtra.conf` files   |
| `--config-path <file>`       | The configuration file to use, instead of the `.dixtra.conf` files      |
| `--changed-since <ref>`      | Only the files changed since a git ref (see below)                      |
| `--quiet`, `-q`              | List only the files that change or fail, not the unchanged ones         |
| `--version`, `-v`            | Print the version                                                       |
| `--help`                     | Print the usage                                                         |

`--max-column` must be a positive number. It applies over the `max-column` of the configuration and of its
`path-overrides` alike.

With `--changed-since`, a file counts as changed when it was committed since the ref, has uncommitted changes, or is new
and not ignored by git. With `HEAD`, only the uncommitted changes count. A deleted file is not formatted. When git
fails, for example outside a repository or with an unknown ref, the path fails with the error of git.

Each file gets one line in the report:

```text
formatted: src/main/scala/A.scala
unchanged: src/main/scala/B.scala
would format: src/main/scala/C.scala        (with --check)
skipped (scala 2): src/main/scala/Old.scala
failed: src/main/scala/D.scala: <reason>
```

A path under the working directory is printed relative to it, any other path in full. The `skipped` and `failed`
lines, and the configuration warnings (see [Configuration](#configuration)), go to the standard error; the others to
the standard output. The warnings come first, each once. A last line sums the run up, e.g. `12 files: 3 formatted, 9
unchanged`, and is printed with `--quiet` too.

The exit code is 1 when a file failed, or with `--check` when a file would change. Otherwise it is 0. A skipped Scala 2
file doesn't change the exit code. A file that fails doesn't stop the others, and neither does an invalid
configuration: it fails the paths that use it, with one `failed` line for each path given on the command line. Invalid
options print the usage on the standard error, and exit with 1.

## The VS Code extension

The `dixtra-vscode/` folder holds an extension that formats the document of the editor with Dixtra when you run
**Format Document** (`Shift+Alt+F`). A document with unsaved changes is
saved first, and Dixtra runs on the file on disk, so the nearest `.dixtra.conf` and its `path-overrides` apply as in
the command line. An untitled document has no file to run on and is declined, and a file that fails to format is
left unchanged, with the error of Dixtra reported.

To build it, run `sbt dixtraVsCode/vscodePackage` from the repository root. It writes
`dist/dixtra-vscode-<version>.vsix`, installed with `code --install-extension dist/dixtra-vscode-<version>.vsix`;
[Building the VS Code extension](#building-the-vs-code-extension) describes the task, its requirements and every
output it produces in `dist/`.

## Configuration

A project overrides only the settings it changes, in a HOCON file named `.dixtra.conf`:

```conf
config-version = 1.0.0

max-column = 100
braces { style = stroustrup, if = force-braces }
paths.excluded = ["**/generated/**"]
```

- **Lookup.** For each path it formats, Dixtra uses the nearest `.dixtra.conf` in the path's directory or above it.
  Without one, it uses the defaults. A file given with `--config-path` (or `dixtraConfig`), or text given with
  `--config-string`, replaces the lookup. A configuration overrides the defaults of `reference.conf` only: it is never
  merged with another `.dixtra.conf`.
- **Unknown settings** are ignored, with a warning on the standard error, so a typo never goes unnoticed and never
  stops a run:

  ```text
  warning: /project/.dixtra.conf: unknown setting max-colum (line 3), ignored
  ```

  The warning names the shortest part of the key that is unknown, `foo` for `foo { bar = 1 }`, but under the name of
  a construct, `comments` or `scaladoc` it names the whole key, `if.brace` for a misspelt alias. A configuration used
  by several paths warns once. The sbt plugin logs the warnings at warn level.

- **Invalid values** of known settings are rejected: a configuration with one fails the paths that use it, and so does
  a file that can't be read or isn't valid HOCON.
- **`config-version`.** It states the version of the configuration schema. Without it, the file is read as the current
  version. A file written for another major version, or for a later dixtra, is rejected rather than half understood.

### Settings

The settings are grouped in sections. [`reference.conf`](dixtra-cli/src/main/resources/reference.conf) describes each
setting and value with examples, and holds the defaults:

| Section           | What it decides                                                                           |
|-------------------|-------------------------------------------------------------------------------------------|
| `max-column`      | The width that makes a construct break (default 120)                                      |
| `overflow`        | What happens to what goes past `max-column`: a trailing comment kept, or moved above      |
| `braces`          | Braces, `end` markers or indentation for each construct, and where braces go              |
| `selector-chain`  | When a chain of calls `a.b(c).d` breaks, one `.member` per line                           |
| `body`            | Whether a body stays after its `=` / `=>` / `then` / `do`, or goes below it               |
| `definition-site` | Parameters of a `def` or a class, and import selectors: how groups break, trailing commas |
| `call-site`       | Arguments of a call, and tuple elements: how groups break, trailing commas                |
| `alignment`       | What lines up in columns: `=`, operators, arrows, `extends`, comments, scaladoc tags      |
| `blank-lines`     | Blank lines at the edges of bodies, between alignment groups, and before scaladoc         |
| `housekeeping`    | Trailing whitespace, the line break at the end of the file, and line endings              |
| `paths`           | The files of a directory to format (see [What gets formatted](#what-gets-formatted))      |
| `path-overrides`  | Other settings for some of the files (see below)                                          |

With `trailing-commas = always`, the default of both sites, the last element of a list laid out one element per line
is followed by a comma. `definition-site.trailing-commas` covers parameters, type parameters (higher-kinded ones
included), the parameters of a class and the selectors of an import; `call-site.trailing-commas` covers arguments
(`List(...)` included) and the elements of a tuple. With `never` there is none. A list on one line never has one, and
neither does a group glued as `b)(` by `groups-separator = previous-group`. The alias `trailing-commas` sets both.

The `housekeeping` section applies to the whole file, after the formatting. By default, the spaces and tabs at the end
of each line are removed (`trim-trailing-whitespace`), but not those inside a string literal; the file ends with
exactly one line break (`enforce-eof`); and each file keeps its own line endings (`line-ending = keep`, or `lf`,
`crlf`, `cr`).

### Aliases

Aliases are second names for settings. Setting one setting twice with different values, directly or through an alias,
is an error:

- **By construct.** Each construct gathers its settings under its own name: `<construct>.braces` sets
  `braces.<construct>`, `<construct>.body` sets `body.<construct>`, and `<construct>.body-edges` sets
  `blank-lines.body-edges.<construct>`, wherever the setting exists. A few constructs also reach a shared setting,
  which changes the other constructs too:

  | Alias                                           | Sets                               |
  |-------------------------------------------------|------------------------------------|
  | `class.align-extends` (and the other templates) | `alignment.extends-clauses`        |
  | `match.align-arrows`, `case-body.align-arrows`  | `alignment.arrows.cases`           |
  | `for.align-arrows`, `for-yield.align-arrows`    | `alignment.arrows.for-enumerators` |
  | `for.chains`, `for-yield.chains`                | `selector-chain.in-for`            |
  | `named-argument.align-equals`                   | `alignment.named-arguments`        |
  | `def.groups-separator`                          | `definition-site.groups-separator` |

- **Comments and scaladoc:**

  | Alias                        | Sets                          |
  |------------------------------|-------------------------------|
  | `comments.align-trailing`    | `alignment.trailing-comments` |
  | `comments.align-across`      | `alignment.across-comments`   |
  | `comments.trailing-overflow` | `overflow.trailing-comment`   |
  | `scaladoc.blank-line-before` | `blank-lines.before-scaladoc` |
  | `scaladoc.align-tags`        | `alignment.scaladoc.tags`     |

- **Both sites:** `trailing-commas` sets `definition-site.trailing-commas` and `call-site.trailing-commas`.

The constructs are those of `braces`, `body` and `blank-lines.body-edges` in `reference.conf`, as `class`, `def`, `if`,
`match`, `for-yield` or `lambda-body`. The templates are `abstract-class`, `case-class`, `class`, `enum`, `object` and
`trait`. The full list is in [`aliases.conf`](dixtra-cli/src/main/resources/aliases.conf), and
[`references.aliases.conf`](dixtra-cli/src/main/resources/references.aliases.conf) writes all the defaults grouped by
construct through the aliases.

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

Some rules have no setting:

- **Indentation** is 2 spaces.
- **Spaces.** A run of spaces between two tokens on a line becomes one, except where Dixtra aligns code; the
  indentation and the insides of strings and comments are left alone. `=>` has one space on each side.
- **Blank lines.** A run of blank lines becomes one. A method (`def`, `given`, `extension`) after another statement
  gets a blank line before it and before its annotations and comments, unless it follows the line that opens its body,
  or it is a one-line definition aligned with the one before it (see `alignment.assignment-groups`).
- **Comments** on lines of their own are reflowed to `max-column`: consecutive `//` lines at one column are one
  comment, re-wrapped by paragraph, with at least one space after `//`. Directives (`///`, `//>`, `//noinspection`)
  and code inside comments are left alone. A trailing comment after code is left as written, but for the space after
  `//`, and never counts in the width of its line.
- **Scaladoc** gets the javadoc layout: `/**` and `*/` on lines of their own, a `*` gutter, no blank lines at its
  edges, a blank line before its first tag, and no blank line between it and its definition. A one-line scaladoc that
  fits stays on one line.
- **Imports and exports** get one space inside their braces (`import a.{ B, C }`) and no blank lines between them. One
  that does not fit packs its selectors onto as few lines as fit, below the `{`. The selectors keep their spelling
  (`_` / `*`, `=>` / `as`) and their order; the braces around a single selector follow `braces.imports`.

### Imports and Scalafix

Dixtra never changes which imports a file has, only their layout. Removing unused imports, sorting, merging, grouping,
wildcards and the Scala 3 spelling (`*`, `as`) are left to Scalafix's `OrganizeImports`. Run Scalafix first and Dixtra
after it, since Scalafix writes every import it touches on one line. Groups are not supported: Dixtra removes the blank
lines between them, so set `groups = ["*"]` in `OrganizeImports`.

Some code is copied exactly as written, because Dixtra has no rule for it yet:

- a secondary constructor (`def this(...)`), and a class whose constructor has modifiers (`class A private (...)`);
- a `def` or a `given` with several parameter-clause groups (`def f[A](a: A)[B](b: B)`), an anonymous `given` with
  parameters, and a `given` in the syntax of Scala 3.6 (`given a: [A] => B = ...`);
- a type alias written over several lines, and a match type;
- a construct holding a comment Dixtra cannot re-attach.
- Among the Scala 3 syntax it does not parse yet is `into` on a type (Scala 3.9).

## Leaving code as written

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

- **Marker syntax.** A marker is a line or a block comment that holds only the marker. Letter case and spaces don't
  matter, so `//DIXTRA:OFF` and `/* Dixtra : on */` are markers too.
- **Unclosed regions.** Without `dixtra: on`, the region runs to the end of the file. A `// dixtra: off` at the top of
  a file leaves the whole file as written.
- **Nesting.** An `off` inside a region, or an `on` outside one, does nothing.
- **The markers themselves** are laid out like any other comment. Only the code between them is protected.
- **Changes around a region.** The code around a region is still formatted. If a change around it can't keep the
  region as written, for example re-indenting a whole package, the file is left unchanged and reported as failed.

## Using Dixtra as a library

`dixtra-core` is the formatter alone, with no dependency other than scalameta:

```scala
libraryDependencies += "com.colofabrix.scala" %% "dixtra-core" % "1.0.0-RC1"
```

```scala
import com.colofabrix.scala.dixtra.{ Config, Dixtra, SourceDialect }

Dixtra.format(source)                                         // the default configuration
Dixtra.format(source, Config.default.copy(maxColumn = 100))
Dixtra.format(source, Config.default, SourceDialect.sbt)  // an sbt 2 build file
```

`Dixtra.format` is a pure function from text to text. It returns `Right(formatted)`, or `Left` of a `FormatError`:

- `ParseError`: the source is not valid Scala 3;
- `Scala2Error`: the source is Scala 2;
- `UnsafeOutputError`: the output would not be equivalent to the input. This signals a formatter bug, not a problem
  with the source.

Reading `.dixtra.conf` files is part of `dixtra-cli` (`ConfigCli`), not of the core, and so are the aliases, the
warnings about unknown settings and `path-overrides`: `ConfigCli.fromFile` and `ConfigCli.fromString` return the
configuration of the files no `path-overrides` glob matches. `Embedded.run` in `dixtra-cli` runs the command line
without exiting the JVM, with Java types only: it hands each report line to a callback, one for the standard output
and one for the standard error, and returns the exit code, 2 for invalid options. The sbt plugin calls it from its
own class loader.

## Building from source

The build needs sbt 2.0 and a JDK that sbt 2 supports (17 or later). It has three projects:

| Project        | Artifact      | Contents                                                    |
|----------------|---------------|-------------------------------------------------------------|
| `dixtra-core/` | `dixtra-core` | the formatter (Scala 3.9)                                   |
| `dixtra-cli/`  | `dixtra-cli`  | the command line, the configuration files, `reference.conf` |
| `dixtra-sbt/`  | `sbt-dixtra`  | the sbt plugin                                              |

```shell
sbt publishLocal                    # publish the three artifacts to the local Ivy repository
sbt test                            # the tests of the formatter and the command line
sbt sbtDixtra/scripted              # the tests of the plugin
sbt "run --check dixtra-core/src"   # the command line from the build
```

### The native executable

`sbt dixtraCli/nativeImage` builds the command line with GraalVM `native-image`, into
`target/out/jvm/scala-3.9.0/dixtra-cli/native/dixtra` (`dixtra.exe` on Windows). It needs:

- `GRAALVM_HOME` pointing to a GraalVM installation (GraalVM Community 25 or later);
- on Windows, the Visual Studio C++ build tools (the "Desktop development with C++" workload). If `native-image`
  doesn't find them, start sbt from an "x64 Native Tools Command Prompt", or after running `vcvars64.bat`.

The reflection and resource metadata of `native-image` is in
`dixtra-cli/src/main/resources/META-INF/native-image/`. It is generated by `sbt dixtraCli/nativeImageMetadata`, which
traces the command line on the JVM of GraalVM over the runs of `nativeImageRuns` in `build.sbt`. Run it again after
changing the dependencies, or the way the command line reads its configuration.

### Building the VS Code extension

`sbt dixtraVsCode/vscodePackage` builds the extension of `dixtra-vscode/` (see
[The VS Code extension](#the-vs-code-extension) for what it does). It needs `GRAALVM_HOME` and, on Windows, the
Visual Studio build tools of [the native executable](#the-native-executable), because it builds it first. The task:

1. writes the version of the build into the `version` of `dixtra-vscode/package.json`, and copies `LICENSE` into
   the folder of the extension;
2. builds the native executable of the current platform, and **moves** every executable it finds into `dist/`
   (`win32-x64/dixtra.exe` and `linux-x64/dixtra`, whichever exist), then renders the icon of the extension from
   `dixtra-vscode/icon.svg` into `dist/icon.png`, with rsvg-convert, ImageMagick or resvg, whichever is installed;
3. stages what `vsce` packages into `dixtra-vscode/bin/`: the Windows executable and the icon; the Linux one is
   left out of the bundle for now;
4. runs `npm install`, `npm run compile` and `vsce package`, and copies the `.vsix` into `dist/` as well.

Everything it produces lands in `dist/`, ignored by git:

```text
dist/
├── dixtra-vscode-1.0.0-RC1.vsix   the extension, installable with code --install-extension
├── win32-x64/dixtra.exe            the executable bundled with the extension
├── linux-x64/dixtra                built, but not bundled for now
└── icon.png                        rendered from dixtra-vscode/icon.svg
```

Install it with `code --install-extension dist/dixtra-vscode-<version>.vsix`. The packaging runs again whenever a
source of the extension, the icon or an executable changes; `npm install && npm run package` in the folder rebuilds
the `.vsix` on its own, packaging what `bin/` already holds, without sbt and without building anything.

### Bootstrapping

The build formats itself with its own plugin (`project/plugins.sbt`), so in a fresh clone the plugin must already be
resolvable. Comment out the `addSbtPlugin` line for the first `sbt publishLocal`.

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
