[Dixtra](https://github.com/ColOfAbRiX/dixtra) is an opinionated formatter for Scala 3.

Unlike other Scala formatters, that allow you to tune typografic formatting like parentheses,
indentation, commas, etc., Dixtra treats the code as having visual features that can be formatted as
a unit.

## Setup

1. The extension bundles the dixtra executable for Windows x64: no `PATH` setup is needed on Windows,
   and `dixtra.executablePath` overrides the bundled executable.

2. Build it with `sbt dixtraVsCode/vscodePackage` from the repository root (or run 
   `npm install && npm run package` inside the folder), then install the `.vsix` it produces in the
   `dist/` folder of the repository, next to the executables and the icon. The version of the
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
