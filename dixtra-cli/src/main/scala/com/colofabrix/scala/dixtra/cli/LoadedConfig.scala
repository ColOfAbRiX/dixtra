package com.colofabrix.scala.dixtra.cli

import cats.data.*
import com.colofabrix.scala.dixtra.Config
import com.typesafe.config.Config as HoconConfig
import java.nio.file.Path
import scala.collection.concurrent.TrieMap
import scala.jdk.CollectionConverters.*
import scala.util.matching.Regex

/**
 * The settings of a `path-overrides` entry: the files its glob matches get them over the rest of the configuration.
 *
 * @param glob The glob as written, relative to the directory of the configuration.
 * @param pattern The glob as a regular expression.
 * @param settings The settings it sets, its aliases resolved and its unknown keys dropped.
 */
final case class PathOverride(glob: String, pattern: Regex, settings: HoconConfig)

/**
 * A configuration as loaded: the one of every file, and the `path-overrides` that change it for some of them.
 *
 * @param base The configuration of a file that no override matches.
 * @param settings The settings written outside `path-overrides`, which the overrides are applied over.
 * @param overrides The `path-overrides` entries.
 * @param warnings What was ignored while loading, as the unknown keys.
 * @param anchor The directory the globs of the overrides are relative to: the one of the configuration file, or the
 *               working directory for a configuration given as a string.
 */
final case class LoadedConfig(
  base: Config,
  settings: HoconConfig,
  overrides: List[PathOverride],
  warnings: List[String],
  anchor: Path,
) {

  // The configuration of each set of matching overrides, by their globs
  private val combined = TrieMap.empty[List[String], ValidatedNel[String, Config]]

  /**
   * The configuration of `file`: the base one with the settings of every override whose glob matches the path of `file`
   * relative to the anchor. Two matching overrides that give a setting different values fail the file.
   */
  def configFor(file: Path): ValidatedNel[String, Config] =
    val matching = 
      relative(file).fold(List.empty[PathOverride]) { path => 
        overrides.filter(_.pattern.matches(path))
      }
      
    if matching.isEmpty then 
      Validated.valid(base)
    else 
      combined.getOrElseUpdate(matching.map(_.glob), ConfigCli.overridden(settings, matching))

  // The path of `file` relative to the anchor, with `/` between directories, when `file` is inside it.
  private def relative(file: Path): Option[String] =
    val path = anchor.relativize(file.toAbsolutePath.normalize)

    val segments =
      path
        .iterator()
        .asScala
        .map(_.toString)
        .toList

    Option.when(!segments.headOption.contains("..")) {
      segments.mkString("/")
    }

}

object LoadedConfig {

  /**
   * The defaults, with no override.
   */
  def defaults(anchor: Path): LoadedConfig =
    LoadedConfig(ConfigCli.default, com.typesafe.config.ConfigFactory.empty(), Nil, Nil, anchor)

}
