package com.colofabrix.scala.dixtra.cli.config

import cats.data.*
import com.colofabrix.scala.dixtra.Config
import com.colofabrix.scala.dixtra.cli.files.SourceFiles
import com.monovore.decline.Opts
import com.typesafe.config.Config as HoconConfig
import java.nio.file.*
import scala.annotation.tailrec

/**
 * CLI option definitions and the lookup of the configuration file for [[Config]]. The loading itself is
 * [[ConfigLoading]], the defaults it loads over [[ConfigSchema]], and the pureconfig instances [[ConfigInstances]].
 *
 * The configuration is the defaults in `reference.conf` overridden by, in order of precedence: `--config-string`,
 * `--config-path`, or the nearest `.dixtra.conf` found walking up from each formatted path.
 *
 * A configuration states the version of its schema in `config-version`, a semantic version (`1.0.0`). One without it is
 * read as the current version; one with another major version, or written for a later version, is rejected.
 */
object ConfigCli {

  /**
   * The name of the configuration file looked up from the formatted paths.
   */
  val fileName: String = ".dixtra.conf"

  /**
   * The version of the configuration schema this dixtra reads.
   */
  val configVersion: String =
    "1.0.0"

  /**
   * Default configuration loaded from `reference.conf`.
   */
  lazy val default: Config =
    ConfigSchema.default

  // The defaults: `reference.conf` alone, as [[ConfigSchema]] reads them.
  private[cli] lazy val references: HoconConfig =
    ConfigSchema.references

  /**
   * A configuration given explicitly on the command line, if any. It is loaded but not validated here: an invalid one
   * fails the paths that use it, as an invalid `.dixtra.conf` does, rather than the parsing of the command line.
   */
  lazy val declineOpts: Opts[Option[ValidatedNel[String, LoadedConfig]]] =
    (configString orElse configPath).orNone

  private lazy val configPath: Opts[ValidatedNel[String, LoadedConfig]] =
    Opts
      .option[String](
        long = "config-path",
        help = s"Path to a HOCON file with override configuration (default: the nearest $fileName)",
      )
      .map(path => loadFile(Paths.get(path)))

  private lazy val configString: Opts[ValidatedNel[String, LoadedConfig]] =
    Opts
      .option[String](
        long = "config-string",
        help = "Override configuration, in HOCON format",
      )
      .map(load(_, workingDirectory))

  private def workingDirectory: Path =
    ConfigLoading.workingDirectory

  /**
   * The configuration for `path`: the nearest `.dixtra.conf` in its directory or any parent directory, or the defaults
   * when there is none.
   */
  def discover(path: Path): ValidatedNel[String, LoadedConfig] =
    nearestFile(SourceFiles.directoryOf(path))
      .fold(Validated.validNel[String, LoadedConfig](LoadedConfig.defaults(workingDirectory)))(loadFile)

  @tailrec
  private def nearestFile(dir: Option[Path]): Option[Path] =
    dir match {
      case None                                                => None
      case Some(d) if Files.isRegularFile(d.resolve(fileName)) => Some(d.resolve(fileName))
      case Some(d)                                             => nearestFile(Option(d.getParent))
    }

  /**
   * Parse a HOCON file of overrides over the defaults: the configuration of a file no override matches.
   */
  def fromFile(path: Path): ValidatedNel[String, Config] =
    ConfigLoading.fromFile(path)

  /**
   * Parse a HOCON string of overrides over the defaults: the configuration of a file no override matches.
   */
  def fromString(configString: String): ValidatedNel[String, Config] =
    ConfigLoading.fromString(configString)

  /**
   * Load a HOCON file of overrides over the defaults, the globs of its `path-overrides` relative to its directory. Its
   * errors and warnings name the file.
   */
  def loadFile(path: Path): ValidatedNel[String, LoadedConfig] =
    ConfigLoading.loadFile(path)

  /**
   * Load a HOCON string of overrides over the defaults, its aliases resolved (see [[ConfigAliases]]) and the globs of
   * its `path-overrides` relative to `anchor`. An unknown key is not an error: it is dropped, with a warning.
   */
  def load(configString: String, anchor: Path): ValidatedNel[String, LoadedConfig] =
    ConfigLoading.load(configString, anchor)

  // Load a HOCON string over `refs` in place of `reference.conf`: the tests pass a partial one, to check that a key it
  // doesn't have neither fails nor warns.
  private[cli] def load(configString: String, anchor: Path, refs: HoconConfig): ValidatedNel[String, LoadedConfig] =
    ConfigLoading.load(configString, anchor, refs)

  /**
   * The configuration of the files that `matching` overrides match, over `settings`. A setting given different values
   * by two of them is an error.
   */
  private[cli] def overridden(settings: HoconConfig, matching: List[PathOverride]): ValidatedNel[String, Config] =
    ConfigLoading.overridden(settings, matching)

}
