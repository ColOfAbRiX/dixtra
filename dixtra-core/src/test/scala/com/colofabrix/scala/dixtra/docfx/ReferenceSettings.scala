package com.colofabrix.scala.dixtra.docfx

import com.colofabrix.scala.dixtra.Config
import com.colofabrix.scala.dixtra.model.*

/**
 * Reads and writes the `group.setting = value` assignments of a documentation example.
 *
 * The two directions are kept together on purpose: the default of a setting and the way it is applied must never drift
 * apart, or a page could document a default it does not honour.
 */
trait SettingsResolver {

  def set(config: Config, key: String, value: String): Either[String, Config]

  def default(key: String): Option[String]

}

/**
 * The settings documented so far in the settings reference.
 *
 * A page cannot be checked until its group is listed here: an unknown setting fails the documentation check instead
 * of silently formatting with the default.
 */
object ReferenceSettings extends SettingsResolver {

  private val setters: Map[String, (Config, String) => Either[String, Config]] =
    Map(
      "max-column" -> ((config, value) =>
        value
          .toIntOption
          .map(column => config.copy(maxColumn = column))
          .toRight(s"'$value' is not a number")),
      "call-site.trailing-commas" -> ((config, value) =>
        enumValue(TrailingCommas.values)(_.key, value).map(
          commas => config.copy(callSite = config.callSite.copy(trailingCommas = commas)),
        )),
      "call-site.groups-separator" -> ((config, value) =>
        enumValue(GroupsSeparator.values)(_.key, value).map(
          separator => config.copy(callSite = config.callSite.copy(groupsSeparator = separator)),
        )),
      "call-site.groups-break" -> ((config, value) =>
        enumValue(GroupsBreak.values)(_.key, value).map(
          groupsBreak => config.copy(callSite = config.callSite.copy(groupsBreak = groupsBreak)),
        )),
      "definition-site.trailing-commas" -> ((config, value) =>
        enumValue(TrailingCommas.values)(_.key, value).map(
          commas => config.copy(definitionSite = config.definitionSite.copy(trailingCommas = commas)),
        )),
      "definition-site.groups-separator" -> ((config, value) =>
        enumValue(GroupsSeparator.values)(_.key, value).map(
          separator => config.copy(definitionSite = config.definitionSite.copy(groupsSeparator = separator)),
        )),
      "overflow.trailing-comment" -> ((config, value) =>
        enumValue(TrailingOverflow.values)(_.key, value).map(
          overflow => config.copy(overflow = config.overflow.copy(trailingComment = overflow)),
        )),
    )

  private val getters: Map[String, Config => String] =
    Map(
      "max-column"                       -> (_.maxColumn.toString),
      "call-site.trailing-commas"        -> (_.callSite.trailingCommas.key),
      "call-site.groups-separator"       -> (_.callSite.groupsSeparator.key),
      "call-site.groups-break"           -> (_.callSite.groupsBreak.key),
      "definition-site.trailing-commas"  -> (_.definitionSite.trailingCommas.key),
      "definition-site.groups-separator" -> (_.definitionSite.groupsSeparator.key),
      "overflow.trailing-comment"        -> (_.overflow.trailingComment.key),
    )

  def set(config: Config, key: String, value: String): Either[String, Config] =
    setters
      .get(key)
      .toRight(s"unknown setting '$key'")
      .flatMap(setter => setter(config, value))

  def default(key: String): Option[String] =
    getters.get(key).map(getter => getter(Config.default))

  private def enumValue[A](values: Array[A])(keyOf: A => String, key: String): Either[String, A] =
    values.find(value => keyOf(value) == key).toRight(s"'$key' is not one of ${values.map(keyOf).mkString(", ")}")

}
