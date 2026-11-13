---
layout: docs
title: The command line
---

# The command line

The command line is `dixtra`. It formats the files and the directories you give it, with the same formatter and the same configuration as the sbt plugin.

```text
Dixtra [--config-string <hocon> | --config-path <file>] [--max-column <n>] [--check] [--changed-since <ref>]
      [--quiet] <path>...
```

## Options

| Option                       | What it does                                                            |
| ---------------------------- | ----------------------------------------------------------------------- |
| `<path>...`                  | Files and directories to format. At least one is required               |
| `--check`, `-c`              | Report the files that are not formatted, without writing them           |
| `--max-column <n>`, `-w <n>` | The longest a line can be, over every `max-column` of the configuration |
| `--config-string <hocon>`    | The configuration, as HOCON text, instead of the `.dixtra.conf` files   |
| `--config-path <file>`       | The configuration file to use, instead of the `.dixtra.conf` files      |
| `--changed-since <ref>`      | Only the files changed since a git ref                                  |
| `--quiet`, `-q`              | List only the files that change or fail, not the unchanged ones         |
| `--version`, `-v`            | Print the version                                                       |
| `--help`                     | Print the usage                                                         |

## Exit codes

| Code | When                                                          |
| ---- | ------------------------------------------------------------- |
| 0    | Everything was formatted, or everything was already formatted |
| 1    | A file failed, or, in `--check` mode, a file would change     |
| 2    | The options are invalid                                       |

## Examples

Format the sources of a project:

```shell
dixtra src
```

Check that the sources are formatted, and fail when they are not:

```shell
dixtra --check src
```

Format only the files that changed since a branch:

```shell
dixtra --changed-since origin/main src
```

Format with a configuration that is not on the disk:

```shell
dixtra --config-string "max-column = 80, braces.style = allman" src
```

Format the build files of an sbt project:

```shell
dixtra build.sbt project
```

The build files of an sbt 2 project are Scala 3 with statements at the top level, and Dixtra parses them with that dialect.
