---
layout: docs
title: Path overrides
---

# Path overrides

Other settings for some of the files. The default is `path-overrides {}`: no file gets other settings.

Each key is a glob, as those of [`paths`](paths.md), relative to the directory of the configuration file that declares it, or to the working directory for `--config-string`. Its section sets any setting or alias, except `paths`, `config-version` and `path-overrides`: those are ignored with a warning, as unknown settings are.

```conf
path-overrides {
  "path/to/file_1" {
    call-site.trailing-commas = never
  }
  "path/to/other/file_2.scala" {
    housekeeping.enforce-eof = false
  }
  "path/that/uses/**/globs/*" {
    housekeeping.line-ending = lf
  }
}
```

## The rules

- **Globs** are relative to the directory of the configuration file that declares them. A file outside that directory never matches. A glob holding a `.` must be quoted, or HOCON reads the `.` as a section separator.
- **Precedence.** A file gets the settings of every glob its path matches, over the rest of the configuration: a setting in an override beats the same setting written outside `path-overrides`, directly or through an alias.
- **Conflicts.** Two matching globs that give a setting different values fail that file only, with the error `<setting> is set to different values by path-overrides."<glob>" and path-overrides."<glob>"`. An invalid value in an override fails the whole configuration.

## An example

A project formats the legacy sources without trailing commas and with the braces of a `def` forced, while the rest of the project keeps the defaults:

```conf
path-overrides {
  "src/main/scala/legacy/**" {
    "trailing-commas" = never
    "def.braces"      = force-braces
  }
}
```
