[Dixtra](https://github.com/ColOfAbRiX/dixtra) is an opinionated formatter for Scala 3. The primary
goal is visual consistency.

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

## Setup

1. The extension bundles the dixtra executable for Windows x64: no `PATH` setup is needed on Windows,
   and `dixtra.executablePath` overrides the bundled executable.

2. Build it with `sbt dixtraVsCode/vscodePackage` from the repository root (or run 
   `npm install && npm run package` inside the folder), then install the `.vsix` it produces in the
   `dist/` folder of the repository, next to the executables. The version of the
   extension follows `version` in `build.sbt`:

   ```
   code --install-extension dist/dixtra-vscode-<version>.vsix
   ```

   For development, open `dixtra-vscode/` in VS Code and press `F5` to try it in an Extension
   Development Host.

3. When asked, pick the dixtra extension as the default formatter for Scala files.

## Settings

| Setting                 | Default  | What it does                                    |
| ----------------------- | -------- | ----------------------------------------------- |
| `dixtra.executablePath` | ``       | The dixtra executable to format with            |
| `dixtra.arguments`      | `[]`     | Extra arguments, e.g. `["--max-column", "100"]` |

The extension runs dixtra on the saved file, so the nearest `.dixtra.conf` and its `path-overrides`
apply as in the command line. A document with unsaved changes is saved first.
