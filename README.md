[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.colofabrix.scala/dixtra-core_3.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.colofabrix.scala/dixtra-core_3)
[![Tests Status](https://github.com/ColOfAbRiX/dixtra/actions/workflows/tests.yml/badge.svg)](https://github.com/ColOfAbRiX/dixtra/actions/workflows/tests.yml)

# Dixtra

Dixtra is an opinionated formatter for Scala 3. Its primary goal is visual consistency and clarity across constructs.

Unlike other Scala formatters that allow you to fine tune typographic formatting like parentheses,
indentation, commas, etc., Dixtra treats the code as having visual features that must be formatted as
a unit. On top of this, Dixtra doesn't give the users full control of everything to limit the potential
for inconsistent results.

The full documentation is published at [colofabrix.github.io/dixtra](https://colofabrix.github.io/dixtra/): start with
[the setup](https://colofabrix.github.io/dixtra/docs/setup.html) and the [settings reference](https://colofabrix.github.io/dixtra/docs/configuration.html).

## Quickstart in your projects

Add this line to `project/plugins.sbt`:

```scala
addSbtPlugin("com.colofabrix.scala" % "sbt-dixtra" % "1.0.0-RC1")
```

Then run `sbt dixtraFormatAll`

The rest of the documentation is at [colofabrix.github.io/dixtra](https://colofabrix.github.io/dixtra/).

## Contributing

Contributions are welcome! See [CONTRIBUTING](.github/CONTRIBUTING.md) for how to build the project, run its tests and
submit a pull request, and the [Code of conduct](.github/CODE_OF_CONDUCT.md) that everyone participating in it is
expected to follow.

## Changelog

You can check [the changelog of Dixtra here](CHANGELOG.md)

## License

Dixtra is released under a "MIT" license. See [LICENSE](LICENSE) for specifics and copyright
declaration.

## Author Information

[Fabrizio Colonna](mailto:colofabrix@tin.it)
