# Changelog

## Unreleased

## v1.0.0-RC1 - 2026/10/28

Initial release.

The features of the first release are:

* Whole-construct layout: every construct is laid out either entirely on one line or entirely broken, one element per line
* Two triggers to break a construct: the one-line form past `max-column`, or a line break written by the author
* Whole-file safety check: the output is parsed again and compared with the input, and a file that fails is left
  unchanged and reported
* Fallback for each construct: a construct that cannot be laid out safely is copied exactly as written
* Atomic writes, per-file isolation, and Scala 2 files skipped, never rewritten
* Scala 3 sources (`.scala`) and sbt 2 build files (`.sbt`), with configurable `paths` and `path-overrides`
* Configuration in HOCON, with the nearest `.dixtra.conf` per path, aliases by construct, unknown settings ignored
  with a warning, and `config-version`
* Settings for braces and end markers, selector chains, bodies, parameter and argument clauses, alignment, blank
  lines, trailing commas, trailing comments, scaladoc, and housekeeping
* A command line, `dixtra-cli`, with `--check`, `--changed-since`, `--config-path`, `--config-string`,
  `--max-column` and `--quiet`, also embeddable in another JVM
* A GraalVM native executable that starts instantly
* An sbt 2 plugin, `sbt-dixtra`, with format and check tasks, format-on-compile, and `dixtraChangedSince`, which
  loads dixtra in a class loader of its own so that it keeps its own Scala version
