package com.colofabrix.scala.dixtra.cli

import com.colofabrix.scala.dixtra.Config
import com.typesafe.config.{Config as HoconConfig, ConfigFactory, ConfigRenderOptions}
import pureconfig.{ConfigSource, ConfigWriter}
import ConfigInstances.given

/**
 * The settings dixtra declares: `reference.conf` completed with the defaults of the schema, the defaults of a
 * configuration that omits keys, and the reserved keys of the configuration.
 */
object ConfigSchema {

  // The reserved keys: the version of a configuration, and the settings that choose the files.
  private[cli] val versionKey =
    "config-version"

  private[cli] val overridesKey =
    "path-overrides"

  /**
   * Default configuration loaded from `reference.conf`.
   */
  private[cli] lazy val default: Config =
    ConfigSource
      .fromConfig(declared(references).withoutPath(versionKey).withoutPath(overridesKey))
      .loadOrThrow[Config]

  // The defaults: `reference.conf` alone. `ConfigFactory.defaultReference` also merges the JVM system properties, which
  // would be read as unknown keys.
  private[cli] lazy val references: HoconConfig =
    ConfigFactory.parseResources(getClass.getClassLoader, "reference.conf").resolve()

  // The defaults of the schema: `Config.default` as the settings they name. The key that neither a configuration nor
  // `reference.conf` has is read as its value, and a setting that only the schema declares is still a known one.
  private[cli] lazy val schemaDefaults: HoconConfig =
    ConfigFactory
      .parseString(ConfigWriter[Config].to(Config.default).render(ConfigRenderOptions.concise()))

  // The settings dixtra declares: the ones of `refs` (normally `reference.conf`) and the ones of the schema.
  private[cli] def declared(refs: HoconConfig): HoconConfig =
    refs.withFallback(schemaDefaults).resolve()

}
