# Contributing

We are very happy to welcome new contributors!

## How can I contribute?

### Report a bug

If you think you have found a bug, please search our issue tracker to see if anyone has already
reported it.

If you are the first to have noticed it, please create an issue and make sure to provide any
information that might help us resolve it. The "how to reproduce" is particularly important as it is
the only way we have to see and catch the bug: a source file dixtra gets wrong, the `.dixtra.conf`
it is formatted with, and the output you expected.

You are welcome to try and fix your bug by submitting a pull request, if you would like to (see Pull
Requests section for more information).

### Feature requests and enhancements

We are open to feature requests, be sure to search our issue tracker to see if anyone has already
asked for it.

If not, please create an issue describing what you want, what your use case is, and an example of
code.

You are welcome to try and do it yourself by submitting a pull request if you would like to (see
Pull requests section for more information).

If you think of anything that might help us improve the project, please create an issue and we will
be happy to discuss it with you.

## Project structure

dixtra is a multi-module sbt project. The root project aggregates the other modules.

| Module         | Directory      | Description                                                                                                                                          |
|----------------|----------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| `dixtra` (root)| `.`            | Aggregates `dixtraCore`, `dixtraCli` and `sbtDixtra`; `run` runs the command line.                                                                   |
| `dixtraCore`   | `dixtra-core/` | The formatter alone: the parsing, the rendering and the safety checks that format a source text by a configuration.                                   |
| `dixtraCli`    | `dixtra-cli/`  | The command line, `dixtra-cli`: the configuration files, the files to format and the reports, on top of the formatter. It builds as a GraalVM native executable. |
| `sbtDixtra`    | `dixtra-sbt/`  | The sbt 2 plugin, `sbt-dixtra`. It does not depend on dixtra at build time: it resolves `dixtra-cli` when a task runs and loads it in a class loader of its own. |

Each module follows the standard sbt layout:

* `src/main` — library code
* `src/test` — unit tests

The behaviour of the formatter is pinned by the golden fixtures under
`dixtra-cli/src/test/resources/golden`, each holding a `.dixtra.conf`, an `input.scala.txt` and the
`expected.scala.txt` it formats to, and by the dogfooding spec that formats this project's own
sources.

## Requirements

* JDK 21 — the version used by CI.
* sbt — the version used is pinned in [`project/build.properties`](../project/build.properties).
* GraalVM — needed only to build the native executable (`sbt dixtraCli/nativeImage`); `GRAALVM_HOME`
  must point to the installation. Everything else runs on a plain JDK.

## Building and testing

All commands are run from the repository root. The project convention is:

```
sbt --client --color=false --supershell=false --batch "<command>"
```

Plain `sbt <command>` behaves the same way.

| Command                       | What it does                                                                     |
|-------------------------------|----------------------------------------------------------------------------------|
| `sbt test`                    | All tests, for every module                                                      |
| `sbt dixtraCheckAll`          | Fails when a source of the project is not formatted — the project formats itself  |
| `sbt dixtraFormatAll`         | Formats the sources of the project in place                                       |
| `sbt dixtraCli/nativeImage`   | Builds the native executable, in `dixtra-cli/target/native`                       |
| `sbt publishLocal`            | Publishes the modules locally, to try the sbt plugin against them                 |

### Running a single suite

```
sbt "dixtraCore/testOnly com.colofabrix.scala.dixtra.format.IfElseSpec"
```

## Pull requests

### First run

To start contributing you need to:

* have [sbt installed](https://www.scala-sbt.org/) on your machine;
* clone this repository with `git clone`;
* use your favourite editor (we love [VSCode + Metals](https://scalameta.org/metals/docs/editors/vscode.html));
* use GIT to manage your changes.

### Issues and Branches

We like to keep things tidy and we ask you to create an Issue for each change you want to submit and
to put your changes on a GIT branch.

One issue, one branch.

### Tests and Code style

Each Pull Request is validated by an automatic tool that checks several aspects of the code:

* All tests must pass (`sbt test`)
* The sources must be formatted by dixtra itself (`sbt dixtraCheckAll`)
* Wartremover and the scalac options of the build must report nothing

All these checks must pass for the PR to be accepted.

If you write any code, be sure to write the tests that go with it and, before pushing anything, be
sure to check that the tests are OK by running `sbt test`. The sources of the project are formatted
by dixtra itself: run `sbt dixtraFormatAll` before pushing, and `sbt dixtraCheckAll` to check them.

### Documentation

The better the documentation, the fewer things users will have to wonder about.

Feel free do add more documentation, to make corrections and to clarify parts that you think are not
clear enough. The formatting options are documented, with examples, in
[`reference.conf`](../dixtra-cli/src/main/resources/reference.conf), and the user-facing behaviour in
the [README](../README.md).

## Code of conduct

In order to keep a happy and respectful atmosphere around the project, we expect everyone
participating in it to follow our [Code of conduct](CODE_OF_CONDUCT.md).
