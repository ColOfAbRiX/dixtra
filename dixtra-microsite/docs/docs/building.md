---
layout: docs
title: Building from source
---

# Building from source

The build needs sbt 2.0 and a JDK that sbt 2 supports (17 or later). It has five projects:

| Project          | Artifact        | Contents                                                    |
| ---------------- | --------------- | ----------------------------------------------------------- |
| `dixtra-core/`   | `dixtra-core`   | the formatter (Scala 3.9)                                   |
| `dixtra-cli/`    | `dixtra-cli`    | the command line, the configuration files, `reference.conf` |
| `dixtra-sbt/`    | `sbt-dixtra`    | the sbt plugin                                              |
| `dixtra-vscode/` | `dixtra-vscode` | the VS Code extension                                       |
| `dixtra-bench/`  | not published   | the JMH benchmarks of the formatter                         |

```shell
sbt publishLocal                    # publish the three artifacts to the local Ivy repository
sbt test                            # the tests of the formatter and the command line
sbt sbtDixtra/scripted              # the tests of the plugin
sbt "run --check dixtra-core/src"   # the command line from the build
```

## The sbt plugin

To build and deploy the sbt plugin locally, run:

```shell
sbt publishLocal
```

and the artifacts will be published under `<local_ivy2>/local/com.colofabrix.scala`.

## The command line tool

`sbt dixtraCli/nativeImage` builds the command line with GraalVM `native-image`, into
`target/out/jvm/scala-3.9.0/dixtra-cli/native/dixtra` (`dixtra.exe` on Windows).

Requirements:

- `GRAALVM_HOME` pointing to a GraalVM installation (GraalVM Community 25 or later);
- on Windows, the Visual Studio C++ build tools (the "Desktop development with C++" workload). If `native-image`
  doesn't find them, start sbt from an "x64 Native Tools Command Prompt", or after running `vcvars64.bat`.

## The VS Code extension

`sbt dixtraVsCode/vscodePackage` builds the extension of `dixtra-vscode/` (see
[The VS Code extension](vscode.md) for what it does).

Requirements:

- `GRAALVM_HOME` pointing to a GraalVM installation (GraalVM Community 25 or later);
- on Windows, the Visual Studio C++ build tools (the "Desktop development with C++" workload). If `native-image`
  doesn't find them, start sbt from an "x64 Native Tools Command Prompt", or after running `vcvars64.bat`.

Everything it produces lands in `dist/`:

```text
dist/
├── dixtra-vscode-<version>.vsix   the extension, installable with code --install-extension
├── win32-x64/dixtra.exe            the executable bundled with the extension
└── linux-x64/dixtra                built, but not bundled for now
```

Install it with `code --install-extension dist/dixtra-vscode-<version>.vsix` or through the UI interface.
