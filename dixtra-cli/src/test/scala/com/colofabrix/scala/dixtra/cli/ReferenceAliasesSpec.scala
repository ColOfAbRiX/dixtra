package com.colofabrix.scala.dixtra.cli

import cats.data.Validated
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.Using

/**
 * references.aliases.conf, the defaults of reference.conf grouped by construct through the aliases.
 */
class ReferenceAliasesSpec extends AnyWordSpec with Matchers {

  private val text =
    Using.resource(Source.fromResource("references.aliases.conf", getClass.getClassLoader))(_.mkString)

  private val file = ConfigFactory.parseString(text)
  private val references =
    ConfigFactory
      .parseResources(getClass.getClassLoader, "reference.conf")
      .resolve()
      .withoutPath("config-version")

  "references.aliases.conf" should {
    "load to the defaults" in {
      ConfigCli.fromString(text) shouldBe Validated.valid(ConfigCli.default)
    }

    "hold the settings of reference.conf with their default values, once its aliases are resolved" in {
      ConfigAliases.resolve(file, ConfigFactory.empty()).map(_.root.unwrapped) shouldBe Right(references.root.unwrapped)
    }

    "write each setting once, directly or through the only alias that reaches it" in {
      val aliases =
        ConfigAliases
          .all
          .map(alias => alias.name -> alias.targets)
          .toMap
      // How many aliases reach each setting
      val reach =
        ConfigAliases
          .all
          .flatMap(_.targets)
          .groupMapReduce(identity)(_ => 1)(_ + _)
      val keys =
        file
          .entrySet()
          .asScala
          .toList
          .map(_.getKey)
          .sorted
      val (throughAliases, direct) = keys.partition(aliases.contains)
      val written                  = direct ++ throughAliases.flatMap(aliases)
      (
        written.groupBy(identity).collect { case (setting, ways) if ways.sizeIs > 1 => setting }.toList.sorted,
        direct.filter(setting => reach.get(setting).contains(1)),
        throughAliases.filter(alias => aliases(alias).exists(target => reach.getOrElse(target, 0) > 1)),
      ) shouldBe (Nil, Nil, Nil)
    }
  }

}
