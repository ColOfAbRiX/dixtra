---
layout: docs
title: The VS Code extension
---

# The VS Code extension

The extension formats the document of the editor with Dixtra when you run **Format Document** (`Shift+Alt+F`).

A document with unsaved changes is saved first, and Dixtra runs on the file on disk, so the nearest `.dixtra.conf` and its `path-overrides` apply as in the command line.

## Install

```shell
code --install-extension dixtra-vscode-<platform>-<version>.vsix
```

The [releases](https://github.com/ColOfAbRiX/dixtra/releases) page holds one package per platform: `dixtra-vscode-linux-x64-<version>.vsix`, `dixtra-vscode-win32-x64-<version>.vsix` and `dixtra-vscode-darwin-arm64-<version>.vsix`.

The extension bundles the Dixtra executable, so no `PATH` setup is needed. When VS Code asks, pick Dixtra as the default formatter for the Scala files.

## Settings

| Setting                 | Default | What it does                                                                                  |
| ----------------------- | ------- | --------------------------------------------------------------------------------------------- |
| `dixtra.executablePath` | empty   | The Dixtra executable to format with. Empty uses the bundled one, or `dixtra` from the `PATH` |
| `dixtra.arguments`      | `[]`    | Extra arguments, e.g. `["--max-column", "100"]`                                               |

```json
{
  "dixtra.arguments": ["--max-column", "100"]
}
```
