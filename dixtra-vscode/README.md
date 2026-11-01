Formats the document of the editor with [dixtra](https://github.com/ColOfAbRiX/dixtra), a formatter for Scala 3, when
you run **Format Document** (`Shift+Alt+F`).

## Setup

1. The extension bundles the dixtra executable for Windows x64: no `PATH` setup is needed on Windows, and
   `dixtra.executablePath` overrides the bundled executable.

2. Build it with `sbt dixtraVsCode/vscodePackage` from the repository root (or run `npm install && npm run package`
   inside the folder), then install the `.vsix` it produces. The version of the extension follows `version` in
   `build.sbt`:

   ```
   code --install-extension dixtra-vscode/dixtra-vscode-<version>.vsix
   ```

   For development, open `dixtra-vscode/` in VS Code and press `F5` to try it in an Extension Development Host.

3. When asked, pick the dixtra extension as the default formatter for Scala files.

## Settings

| Setting                 | Default  | What it does                                    |
| ----------------------- | -------- | ----------------------------------------------- |
| `dixtra.executablePath` | ``       | The dixtra executable to format with            |
| `dixtra.arguments`      | `[]`     | Extra arguments, e.g. `["--max-column", "100"]` |

The extension runs dixtra on the saved file, so the nearest `.dixtra.conf` and its `path-overrides` apply as in the
command line. A document with unsaved changes is saved first.
