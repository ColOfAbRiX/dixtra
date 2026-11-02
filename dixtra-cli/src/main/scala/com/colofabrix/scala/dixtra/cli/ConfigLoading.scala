package com.colofabrix.scala.dixtra.cli

import cats.data.*
import cats.syntax.all.*
import com.colofabrix.scala.dixtra.Config
import com.colofabrix.scala.dixtra.FormatError
import com.typesafe.config.{Config as HoconConfig, ConfigFactory, ConfigObject, ConfigUtil}
import java.nio.file.*
import pureconfig.ConfigSource
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.*
import ConfigInstances.given

/**
 * Loading a configuration: HOCON parsing, alias resolution, unknown-key warnings, `path-overrides` and the version
 * check, over the defaults of [[ConfigSchema]].
 */
object ConfigLoading {

  /**
   * Parse a HOCON file of overrides over the defaults: the configuration of a file no override matches.
   */
  def fromFile(path: Path): ValidatedNel[String, Config] =
    loadFile(path).map(_.base)

  /**
   * Parse a HOCON string of overrides over the defaults: the configuration of a file no override matches.
   */
  def fromString(configString: String): ValidatedNel[String, Config] =
    load(configString, workingDirectory).map(_.base)

  /**
   * Load a HOCON file of overrides over the defaults, the globs of its `path-overrides` relative to its directory. Its
   * errors and warnings name the file.
   */
  def loadFile(path: Path): ValidatedNel[String, LoadedConfig] =
    val directory =
      path
        .toAbsolutePath
        .normalize
        .getParent

    Try(Using.resource(Source.fromFile(path.toFile, "UTF-8"))(_.mkString))
      .fold(
        e => Validated.invalidNel(s"Cannot read ${path.toString}: ${FormatError.describe(e)}"),
        load(_, Option(directory).getOrElse(workingDirectory)),
      )
      .bimap(
        _.map(error => s"${path.toString}: $error"),
        loaded => loaded.copy(warnings = loaded.warnings.map(warning => s"${path.toString}: $warning")),
      )

  /**
   * Load a HOCON string of overrides over the defaults, its aliases resolved (see [[ConfigAliases]]) and the globs of
   * its `path-overrides` relative to `anchor`. An unknown key is not an error: it is dropped, with a warning.
   */
  def load(configString: String, anchor: Path): ValidatedNel[String, LoadedConfig] =
    load(configString, anchor, ConfigSchema.references)

  // Load a HOCON string over `refs` in place of `reference.conf`: the tests pass a partial one, to check that a key it
  // doesn't have neither fails nor warns.
  private[cli] def load(configString: String, anchor: Path, refs: HoconConfig): ValidatedNel[String, LoadedConfig] =
    Try(ConfigFactory.parseString(configString))
      .fold(
        e => Validated.invalidNel(FormatError.describe(e)),
        parsed => {
          val defaults = ConfigSchema.declared(refs)

          val loaded =
            for
              (written, entries) <- splitOverrides(parsed)
              resolved           <- ConfigAliases.resolve(written, defaults)
              (settings, unknown) = knownOnly(resolved, defaults, "")
              merged             <- supported(settings.withFallback(defaults).resolve())
              base               <- read(merged)
              overrides          <- entries.traverse((glob, written) => pathOverride(glob, written, settings, defaults))
            yield
              val overrideWarnings = overrides.flatMap(_._2)

              LoadedConfig(
                base,
                settings.withoutPath(ConfigSchema.versionKey),
                overrides.map(_._1),
                unknown ++ overrideWarnings,
                anchor,
              )

          Validated.fromEither(loaded).toValidatedNel
        },
      )

  /**
   * The configuration of the files that `matching` overrides match, over `settings`. A setting given different values
   * by two of them is an error.
   */
  private[cli] def overridden(settings: HoconConfig, matching: List[PathOverride]): ValidatedNel[String, Config] =
    val conflicts =
      matching
        .flatMap { pathOverride =>
          pathOverride
            .settings
            .entrySet()
            .asScala
            .toList
            .map { entry =>
              (entry.getKey, pathOverride.glob, entry.getValue.unwrapped)
            }
        }
        .groupBy(_._1)
        .toList
        .sortBy(_._1)
        .collect {
          case (key, values) if values.map(_._3).distinct.sizeIs > 1 =>
            s"$key is set to different values by ${values.map(v => s"${ConfigSchema.overridesKey}.\"${v._2}\"").mkString(" and ")}"
        }

    NonEmptyList.fromList(conflicts) match {
      case Some(errors) =>
        Validated.invalid(errors)
      case None =>
        val merged =
          matching.foldLeft(settings) { (acc, pathOverride) =>
            pathOverride.settings.withFallback(acc)
          }

        Validated.fromEither(read(merged.withFallback(ConfigSchema.references).resolve())).toValidatedNel
    }

  // The settings outside `path-overrides`, and its entries: each glob with its settings.
  private def splitOverrides(parsed: HoconConfig): Either[String, (HoconConfig, List[(String, HoconConfig)])] =
    if !parsed.hasPathOrNull(ConfigSchema.overridesKey) then
      Right((parsed, Nil))
    else
      parsed.getValue(ConfigSchema.overridesKey) match {
        case entries: ConfigObject =>
          entries
            .asScala
            .toList
            .sortBy(_._1)
            .traverse {
              case (glob, settings: ConfigObject) =>
                Right(glob -> settings.toConfig)
              case (glob, _) =>
                Left(s"${ConfigSchema.overridesKey}.\"$glob\" must be a section of settings")
            }
            .map(parsed.withoutPath(ConfigSchema.overridesKey) -> _)
        case _ =>
          Left(s"${ConfigSchema.overridesKey} must be a section of globs, each with its settings")
      }

  // An entry of `path-overrides`, its aliases resolved, checked over `settings`; and the warnings about it.
  private def pathOverride(
    glob: String,
    written: HoconConfig,
    settings: HoconConfig,
    defaults: HoconConfig,
  ): Either[String, (PathOverride, List[String])] =
    val name = s"${ConfigSchema.overridesKey}.\"$glob\""

    // The settings an override can set: all but the ones that choose the files and the version
    val eligible =
      defaults
        .withoutPath(ConfigSchema.versionKey)
        .withoutPath(ConfigSchema.overridesKey)
        .withoutPath("paths")

    val loaded =
      for
        resolved        <- ConfigAliases.resolve(written, eligible)
        (known, unknown) = knownOnly(resolved, eligible, s"$name.")
        _               <- read(known.withFallback(settings).withFallback(defaults).resolve())
      yield
        (PathOverride(glob, SourceFiles.globRegex(glob), known), unknown)

    loaded.left.map(error => s"$name: $error")

  // The sections of the aliases (`if` of `if.braces`): a key under them is a misspelt alias, named in full.
  private lazy val aliasPrefixes: Set[String] =
    ConfigAliases
      .all
      .flatMap { alias =>
        val segments =
          ConfigUtil
            .splitPath(alias.name)
            .asScala
            .toList

        segments
          .indices
          .drop(1)
          .map(i => ConfigUtil.joinPath(segments.take(i).asJava))
      }
      .toSet

  // `settings` without the keys `known` doesn't have, and a warning for each of them, its name after `prefix`.
  private def knownOnly(settings: HoconConfig, known: HoconConfig, prefix: String): (HoconConfig, List[String]) =
    val unknown =
      settings
        .entrySet()
        .asScala
        .toList
        .map(_.getKey)
        .sorted
        .flatMap { key =>
          val segments =
            ConfigUtil
              .splitPath(key)
              .asScala
              .toList

          segments
            .indices
            .map(i => ConfigUtil.joinPath(segments.take(i + 1).asJava))
            .find { path =>
              val isKnown       = Try(known.hasPathOrNull(path)).getOrElse(false)
              val aliasPrefixed = aliasPrefixes.contains(path)
              !isKnown && !aliasPrefixed
            }
        }
        .distinct

    (
      unknown.foldLeft(settings)(ConfigAliases.without),
      unknown.map(key => s"unknown setting $prefix$key${ConfigAliases.line(settings.getValue(key))}, ignored"),
    )

  // The configuration `merged` holds: its settings with their values, a key without one read as the default of the
  // schema.
  private def read(merged: HoconConfig): Either[String, Config] =
    ConfigSource
      .fromConfig(
        merged
          .withoutPath(ConfigSchema.overridesKey)
          .withoutPath(ConfigSchema.versionKey)
          .withFallback(ConfigSchema.schemaDefaults),
      )
      .load[Config]
      .left
      .map(_.prettyPrint())

  // The settings of `merged` without their version, when this dixtra reads that version. Reading an older major version
  // would translate its settings here. One without a version is read as the current one.
  private def supported(merged: com.typesafe.config.Config): Either[String, com.typesafe.config.Config] =
    val written =
      if merged.hasPath(ConfigSchema.versionKey) then Try(merged.getString(ConfigSchema.versionKey)).getOrElse("")
      else ConfigCli.configVersion
    (semver(written), semver(ConfigCli.configVersion)) match {
      case (None, _) =>
        Left(s"""${ConfigSchema.versionKey} must be a version like "${ConfigCli.configVersion}", not "$written"""")
      case (Some((writtenMajor, _, _)), Some((currentMajor, _, _))) if writtenMajor != currentMajor =>
        Left(
          s"${ConfigSchema.versionKey} = $written is not read by this dixtra, which reads ${ConfigCli.configVersion}",
        )
      case (Some(version), Some(current)) if Ordering[(Int, Int, Int)].gt(version, current) =>
        Left(
          s"${ConfigSchema.versionKey} = $written is for a later dixtra, this one reads up to ${ConfigCli.configVersion}",
        )
      case _ =>
        Right(merged.withoutPath(ConfigSchema.versionKey))
    }

  // The major, minor and patch numbers of a version like `1.2.3`.
  private def semver(version: String): Option[(Int, Int, Int)] =
    """(\d+)\.(\d+)\.(\d+)"""
      .r
      .unapplySeq(version)
      .flatMap {
        case List(major, minor, patch) =>
          for
            majorNumber <- major.toIntOption
            minorNumber <- minor.toIntOption
            patchNumber <- patch.toIntOption
          yield
            (majorNumber, minorNumber, patchNumber)
        case _ =>
          None
      }

  private[cli] def workingDirectory: Path =
    Paths
      .get("")
      .toAbsolutePath
      .normalize

}
