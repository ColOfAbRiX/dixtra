package com.colofabrix.scala.dixtra.cli

import cats.data.*
import cats.syntax.all.*
import com.colofabrix.scala.dixtra.*
import com.monovore.decline.Opts
import com.typesafe.config.{Config as HoconConfig, ConfigFactory, ConfigObject, ConfigUtil}
import java.nio.file.*
import pureconfig.*
import pureconfig.error.*
import pureconfig.generic.ProductHint
import pureconfig.generic.scala3.HintsAwareConfigReaderDerivation.deriveReader
import scala.annotation.tailrec
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.*

/**
 * CLI option definitions, HOCON parsing, and pureconfig loading for [[Config]].
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

  private val versionKey =
    "config-version"

  private val overridesKey =
    "path-overrides"

  /**
   * Default configuration loaded from `reference.conf`.
   */
  lazy val default: Config =
    ConfigSource
      .fromConfig(references.withoutPath(versionKey).withoutPath(overridesKey))
      .loadOrThrow[Config]

  // The defaults: `reference.conf` alone. `ConfigFactory.defaultReference` also merges the JVM system properties, which
  // would be read as unknown keys.
  private lazy val references: HoconConfig =
    ConfigFactory.parseResources(getClass.getClassLoader, "reference.conf").resolve()

  // The settings an override can set: all but the ones that choose the files and the version
  private lazy val overridable: HoconConfig =
    references
      .withoutPath(versionKey)
      .withoutPath(overridesKey)
      .withoutPath("paths")

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
    Paths
      .get("")
      .toAbsolutePath
      .normalize

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
    Try(ConfigFactory.parseString(configString))
      .fold(
        e => Validated.invalidNel(FormatError.describe(e)),
        parsed => {
          val loaded =
            for
              (written, entries) <- splitOverrides(parsed)
              resolved           <- ConfigAliases.resolve(written, references)
              (settings, unknown) = knownOnly(resolved, references, "")
              merged             <- supported(settings.withFallback(references).resolve())
              base               <- read(merged)
              overrides          <- entries.traverse((glob, written) => pathOverride(glob, written, settings))
            yield
              val overrideWarnings = overrides.flatMap(_._2)

              LoadedConfig(
                base,
                settings.withoutPath(versionKey),
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
            s"$key is set to different values by ${values.map(v => s"$overridesKey.\"${v._2}\"").mkString(" and ")}"
        }

    NonEmptyList.fromList(conflicts) match {
      case Some(errors) =>
        Validated.invalid(errors)
      case None =>
        val merged =
          matching.foldLeft(settings) { (acc, pathOverride) =>
            pathOverride.settings.withFallback(acc)
          }

        Validated.fromEither(read(merged.withFallback(references).resolve())).toValidatedNel
    }

  // The settings outside `path-overrides`, and its entries: each glob with its settings.
  private def splitOverrides(parsed: HoconConfig): Either[String, (HoconConfig, List[(String, HoconConfig)])] =
    if !parsed.hasPathOrNull(overridesKey) then
      Right((parsed, Nil))
    else
      parsed.getValue(overridesKey) match {
        case entries: ConfigObject =>
          entries
            .asScala
            .toList
            .sortBy(_._1)
            .traverse {
              case (glob, settings: ConfigObject) => Right(glob -> settings.toConfig)
              case (glob, _)                      => Left(s"$overridesKey.\"$glob\" must be a section of settings")
            }
            .map(parsed.withoutPath(overridesKey) -> _)
        case _ =>
          Left(s"$overridesKey must be a section of globs, each with its settings")
      }

  // An entry of `path-overrides`, its aliases resolved, checked over `settings`; and the warnings about it.
  private def pathOverride(
    glob: String,
    written: HoconConfig,
    settings: HoconConfig,
  ): Either[String, (PathOverride, List[String])] =
    val name = s"$overridesKey.\"$glob\""

    val loaded =
      for
        resolved        <- ConfigAliases.resolve(written, overridable)
        (known, unknown) = knownOnly(resolved, overridable, s"$name.")
        _               <- read(known.withFallback(settings).withFallback(references).resolve())
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

        segments.indices.drop(1).map(i => ConfigUtil.joinPath(segments.take(i).asJava))
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

  // The configuration `merged` holds: the settings of `reference.conf` with their values.
  private def read(merged: HoconConfig): Either[String, Config] =
    ConfigSource
      .fromConfig(merged.withoutPath(overridesKey).withoutPath(versionKey))
      .load[Config]
      .left
      .map(_.prettyPrint())

  // The settings of `merged` without their version, when this dixtra reads that version. Reading an older major version
  // would translate its settings here.
  private def supported(merged: com.typesafe.config.Config): Either[String, com.typesafe.config.Config] =
    val written = Try(merged.getString(versionKey)).getOrElse("")
    (semver(written), semver(configVersion)) match {
      case (None, _) =>
        Left(s"""$versionKey must be a version like "$configVersion", not "$written"""")
      case (Some((writtenMajor, _, _)), Some((currentMajor, _, _))) if writtenMajor != currentMajor =>
        Left(s"$versionKey = $written is not read by this dixtra, which reads $configVersion")
      case (Some(version), Some(current)) if Ordering[(Int, Int, Int)].gt(version, current) =>
        Left(s"$versionKey = $written is for a later dixtra, this one reads up to $configVersion")
      case _ =>
        Right(merged.withoutPath(versionKey))
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

  //  ConfigReader Instances  //

  given ProductHint[Config] =
    ProductHint[Config](allowUnknownKeys = false)

  given ProductHint[SelectorChainConfig] =
    ProductHint[SelectorChainConfig](allowUnknownKeys = false)

  // The keys of `body` are Scala keywords, so their fields have another name
  given ProductHint[BodyConfig] =
    ProductHint[BodyConfig](
      ConfigFieldMapping(CamelCase, KebabCase).withOverrides(
        "defBody"   -> "def",
        "valBody"   -> "val",
        "varBody"   -> "var",
        "typeBody"  -> "type",
        "ifBody"    -> "if",
        "tryBody"   -> "try",
        "forBody"   -> "for",
        "whileBody" -> "while",
      ),
      allowUnknownKeys = false,
    )

  given ConfigReader[BodyConfig] =
    deriveReader[BodyConfig]

  given ProductHint[DefinitionSiteConfig] =
    ProductHint[DefinitionSiteConfig](allowUnknownKeys = false)

  given ConfigReader[DefinitionSiteConfig] =
    deriveReader[DefinitionSiteConfig]

  given ProductHint[CallSiteConfig] =
    ProductHint[CallSiteConfig](allowUnknownKeys = false)

  given ConfigReader[CallSiteConfig] =
    deriveReader[CallSiteConfig]

  given ConfigReader[SingleBodyPolicy] =
    keyed(SingleBodyPolicy.values.toList)(_.key)

  given ConfigReader[YieldBodyPolicy] =
    keyed(YieldBodyPolicy.values.toList)(_.key)

  given ConfigReader[MultiBodyPolicy] =
    keyed(MultiBodyPolicy.values.toList)(_.key)

  given ConfigReader[GroupsSeparator] =
    keyed(GroupsSeparator.values.toList)(_.key)

  given ConfigReader[GroupsBreak] =
    keyed(GroupsBreak.values.toList)(_.key)

  given ProductHint[AlignmentConfig] =
    ProductHint[AlignmentConfig](allowUnknownKeys = false)

  given ProductHint[PathsConfig] =
    ProductHint[PathsConfig](allowUnknownKeys = false)

  given ProductHint[ArrowsConfig] =
    ProductHint[ArrowsConfig](allowUnknownKeys = false)

  given ConfigReader[PathsConfig] =
    deriveReader[PathsConfig]

  given ConfigReader[ArrowsConfig] =
    deriveReader[ArrowsConfig]

  given ProductHint[ScaladocAlignmentConfig] =
    ProductHint[ScaladocAlignmentConfig](allowUnknownKeys = false)

  given ConfigReader[ScaladocAlignmentConfig] =
    deriveReader[ScaladocAlignmentConfig]

  given ConfigReader[ChainHeadPolicy] =
    keyed(ChainHeadPolicy.values.toList)(_.key)

  given ConfigReader[InlineChainPolicy] =
    keyed(InlineChainPolicy.values.toList)(_.key)

  given ConfigReader[NestedChainPolicy] =
    keyed(NestedChainPolicy.values.toList)(_.key)

  given ConfigReader[BracePolicy] =
    keyed(BracePolicy.values.toList)(_.key)

  given ConfigReader[DefinitionKind] =
    keyed(DefinitionKind.values.toList)(_.key)

  given ConfigReader[TrailingOverflow] =
    keyed(TrailingOverflow.values.toList)(_.key)

  given ConfigReader[TrailingCommas] =
    keyed(TrailingCommas.values.toList)(_.key)

  given ProductHint[HousekeepingConfig] =
    ProductHint[HousekeepingConfig](allowUnknownKeys = false)

  given ConfigReader[HousekeepingConfig] =
    deriveReader[HousekeepingConfig]

  given ConfigReader[LineEnding] =
    keyed(LineEnding.values.toList)(_.key)

  given ConfigReader[ScaladocSpacing] =
    keyed(ScaladocSpacing.values.toList)(_.key)

  given ConfigReader[BlankLinePolicy] =
    keyed(BlankLinePolicy.values.toList)(_.key)

  given ConfigReader[BracesStyle] =
    keyed(BracesStyle.values.toList)(_.key)

  given ConfigReader[BraceConfig] =
    ConfigReader.fromCursor { cursor =>
      for
        settings <- cursor.asObjectCursor
        style    <- settings.atKey("style").flatMap(ConfigReader[BracesStyle].from)
        policies <- bracePolicies.from(settings.withoutKey("style"))
      yield
        BraceConfig(style, policies)
    }

  given bracePolicies: ConfigReader[Map[Construct, BracePolicy]] =
    ConfigReader[Map[String, BracePolicy]].emap { settings =>
      settings
        .toList
        .foldLeft[Either[FailureReason, Map[Construct, BracePolicy]]](Right(Map.empty)) {
          case (acc, (key, policy)) =>
            val construct = Construct.values.find(_.key == key)

            acc.flatMap { policies =>
              construct match {
                case None =>
                  Left(unknownConstruct(key))
                case Some(construct) if policy == BracePolicy.ForceEnd && !construct.allowsEnd =>
                  Left(cannotConvert(key, s"$key has no end marker, so it cannot be ${policy.key}"))
                case Some(construct) =>
                  Right(policies + (construct -> policy))
              }
            }
        }
    }

  given ProductHint[BlankLinesConfig] =
    ProductHint[BlankLinesConfig](allowUnknownKeys = false)

  given ConfigReader[Map[Construct, BlankLinePolicy]] =
    ConfigReader[Map[String, BlankLinePolicy]].emap { settings =>
      settings
        .toList
        .foldLeft[Either[FailureReason, Map[Construct, BlankLinePolicy]]](Right(Map.empty)) {
          case (acc, (key, policy)) =>
            acc.flatMap { policies =>
              Construct
                .values
                .find(_.key == key)
                .toRight(unknownConstruct(key))
                .map(construct => policies + (construct -> policy))
            }
        }
    }

  given ConfigReader[BlankLinesConfig] =
    deriveReader[BlankLinesConfig]

  given ConfigReader[SelectorChainConfig] =
    deriveReader[SelectorChainConfig].emap { chain =>
      if chain.breakAtCount < 2 then
        Left(cannotConvert(chain.breakAtCount.toString, "break-at-count must be at least 2"))
      else if chain.receiverMinLength < 1 then
        Left(cannotConvert(chain.receiverMinLength.toString, "receiver-min-length must be at least 1"))
      else
        Right(chain)
    }

  given ConfigReader[AlignmentConfig] =
    deriveReader[AlignmentConfig].emap { alignment =>
      val all        = alignment.assignmentGroups.flatMap(_.toList)
      val duplicated = all.diff(all.distinct).distinct
      if duplicated.nonEmpty then
        Left(cannotConvert(duplicated.map(_.key).mkString(", "), "a keyword can be in only one alignment group"))
      else if alignment.operators.exists(_.isBlank) then
        Left(cannotConvert("\"\"", "an operator cannot be empty"))
      else
        Right(alignment)
    }

  given ConfigReader[Config] =
    deriveReader[Config]

  private def keyed[A](values: List[A])(key: A => String): ConfigReader[A] =
    ConfigReader[String].emap { written =>
      values
        .find(key(_) == written)
        .toRight(cannotConvert(written, s"expected one of ${values.map(key).mkString(", ")}"))
    }

  private def unknownConstruct(key: String): FailureReason =
    cannotConvert(key, s"unknown construct, expected one of ${Construct.values.map(_.key).mkString(", ")}")

  private def cannotConvert(value: String, because: String): FailureReason =
    CannotConvert(value, "configuration", because)

}
