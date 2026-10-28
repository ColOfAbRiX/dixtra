package com.colofabrix.scala.dixtra.cli

import com.typesafe.config.{Config as HoconConfig, *}
import scala.jdk.CollectionConverters.*
import scala.util.Try

/**
 * The aliases of the settings: a name that sets one or more settings at once.
 *
 * The aliases are defined in the internal resource `aliases.conf`, the one place to add, change or remove them. The
 * tests check that every target is a setting of `reference.conf`, that no alias is a setting itself, and that the
 * targets of an alias accept the same values.
 *
 * Aliases are resolved when a configuration is loaded, on the settings the user wrote and before the defaults are
 * merged in, so the rest of dixtra never sees them. An alias sets each of its targets to its value. When a setting is
 * given more than once, directly or through aliases, every value must agree, else the configuration is rejected.
 */
object ConfigAliases {

  /**
   * An alias: its path, and the paths of the settings it sets.
   */
  final case class Alias(name: String, targets: List[String])

  /**
   * Every alias, read from `aliases.conf`.
   */
  lazy val all: List[Alias] =
    val aliases = ConfigFactory.parseResources(getClass.getClassLoader, "aliases.conf").getObject("aliases")
    aliases
      .keySet()
      .asScala
      .toList
      .sorted
      .map(name => Alias(name, aliases.toConfig.getStringList(ConfigUtil.quoteString(name)).asScala.toList))

 /**
  * The settings of `overrides` with its aliases replaced by their targets, or the errors of the settings given more
  * than once with different values. `defaults` resolves the substitutions of `overrides`.
  */
  def resolve(overrides: HoconConfig, defaults: HoconConfig): Either[String, HoconConfig] =
    val written = all.filter(alias => present(overrides, alias.name))

    if written.isEmpty then
      Right(overrides)
    else
      val resolved = overrides.withFallback(defaults).resolve()

      // Every value given for each setting: (the key it was written as, its value)
      val claims =
        written
          .flatMap(alias => alias.targets.map(target => target -> (alias.name, resolved.getValue(alias.name))))
          .groupMap(_._1)(_._2)
          .map { (target, viaAliases) =>
            val direct = Option.when(present(overrides, target))((target, resolved.getValue(target))).toList
            target -> (direct ++ viaAliases)
          }

      val conflicts =
        claims
          .toList
          .sortBy(_._1)
          .collect {
            case (target, values) if values.map(_._2.unwrapped).distinct.sizeIs > 1 =>
              val settings = values.map((key, value) => s"$key = ${value.render()}${line(value)}").mkString(", ")
              s"$target is set to different values: $settings"
          }

      if conflicts.nonEmpty then
        Left(conflicts.mkString("; "))
      else
        val withoutAliases = written.foldLeft(overrides)((acc, alias) => without(acc, alias.name))
        Right(
          claims.foldLeft(withoutAliases) { case (acc, (target, values)) =>
            values.headOption.fold(acc)((_, value) => acc.withValue(target, value))
          },
        )

  // `config` without `path`, and without the sections it leaves empty (`scaladoc {}` once `scaladoc.align-tags` is
  // gone).
  @annotation.tailrec
  private[cli] def without(config: HoconConfig, path: String): HoconConfig =
    val removed = config.withoutPath(path)
    val parent =
      path
        .split('.')
        .dropRight(1)
        .mkString(".")
    if parent.isEmpty || !Try(removed.getObject(parent).isEmpty).getOrElse(false) then removed
    else without(removed, parent)

  // Whether the user wrote a setting at `path`, null included.
  private def present(config: HoconConfig, path: String): Boolean =
    Try(config.hasPathOrNull(path)).getOrElse(false)

  // ` (line n)` for a value read from a known line.
  private[cli] def line(value: ConfigValue): String =
    Option(value.origin())
      .map(_.lineNumber())
      .filter(_ > 0)
      .fold("")(n => s" (line ${n.toString})")

}
