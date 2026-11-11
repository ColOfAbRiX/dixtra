# Dixtra

[Dixtra](https://github.com/ColOfAbRiX/dixtra) is an opinionated formatter for Scala 3. Its primary
goal is visual consistency and clarity across constructs.

Unlike other Scala formatters that allow you to fine tune typographic formatting like parentheses,
indentation, commas, etc., Dixtra treats the code as having visual features that must be formatted as
a unit. On top of this, Dixtra doesn't give the users full control of everything to limit the potential
for inconsistent results.

## Setup

1. Install the extension using:

   ```
   code --install-extension dist/dixtra-vscode-<version>.vsix
   ```

2. The extension bundles the dixtra executable for Windows, Linux and MacOS. No _PATH_ setup is needed
   but `dixtra.executablePath` can be used to override the bundled executable.

3. When asked, pick the dixtra extension as the default formatter for Scala files.

## Settings

| Setting                 | Default  | What it does                                    |
| ----------------------- | -------- | ----------------------------------------------- |
| `dixtra.executablePath` | ``       | The dixtra executable to format with            |
| `dixtra.arguments`      | `[]`     | Extra arguments, e.g. `["--max-column", "100"]` |

The extension runs dixtra on the saved file, so the nearest `.dixtra.conf` and its `path-overrides`
apply as in the command line. A document with unsaved changes is saved first.
