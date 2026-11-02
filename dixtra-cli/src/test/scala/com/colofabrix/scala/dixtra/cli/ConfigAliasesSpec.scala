package com.colofabrix.scala.dixtra.cli

import cats.data.Validated
import com.colofabrix.scala.dixtra.model.*
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.jdk.CollectionConverters.*

/**
 * The aliases of the settings (see [[ConfigAliases]]): their table, and how a configuration that uses them is read.
 */
class ConfigAliasesSpec extends AnyWordSpec with Matchers {

  private val references = ConfigFactory.parseResources(getClass.getClassLoader, "reference.conf").resolve()
  private val settings: Set[String] =
    references
      .entrySet()
      .asScala
      .map(_.getKey)
      .toSet

  // Values to probe what a setting accepts: every key of the enumerations of the configuration, booleans and numbers.
  private val bodyPolicies =
    BlankLinePolicy.values.map(_.key) ++ SingleBodyPolicy.values.map(_.key) ++ YieldBodyPolicy.values.map(_.key) ++
    MultiBodyPolicy.values.map(_.key)
  private val bracePolicies =
    BracePolicy.values.map(_.key) ++ BracesStyle.values.map(_.key)
  private val chainPolicies =
    ChainHeadPolicy.values.map(_.key) ++ GroupsBreak.values.map(_.key) ++ GroupsSeparator.values.map(_.key) ++
    InlineChainPolicy.values.map(_.key) ++ NestedChainPolicy.values.map(_.key)
  private val settingValues =
    ScaladocSpacing.values.map(_.key) ++ TrailingOverflow.values.map(_.key) ++ TrailingCommas.values.map(_.key)

  private val probes: List[String] =
    val enumerated = (bodyPolicies ++ bracePolicies ++ chainPolicies ++ settingValues).toList.distinct

    enumerated ++ List("true", "false", "1", "3")

  "the table of aliases" should {
    "point every alias to settings of reference.conf" in {
      val expected = true

      val actual =
        ConfigAliases
          .all
          .flatMap(alias => alias.targets.filterNot(settings).map(target => s"${alias.name} -> $target"))
          .isEmpty

      actual shouldBe expected
    }

    "never use a setting, or a section holding settings, as an alias" in {
      val expected = true

      val actual =
        ConfigAliases
          .all
          .map(_.name)
          .filter(name => settings.exists(s => s == name || s.startsWith(name + ".")))
          .isEmpty

      actual shouldBe expected
    }

    "give each alias at least one target, without duplicates" in {
      val expected = true

      val actual =
        ConfigAliases
          .all
          .filter(a => a.targets.isEmpty || a.targets.distinct.sizeIs != a.targets.size)
          .isEmpty

      actual shouldBe expected
    }

    "only group settings that accept the same values" in {
      val expected = true

      val actual =
        ConfigAliases
          .all
          .filter(_.targets.sizeIs > 1)
          .flatMap { alias =>
            val accepted = alias.targets.map(t => t -> probes.filter(v => ConfigCli.fromString(s"$t = $v").isValid))
            accepted
              .headOption
              .toList
              .flatMap { (first, values) =>
                accepted.collect { case (target, other) if other != values => s"${alias.name}: $first and $target" }
              }
          }
          .isEmpty

      actual shouldBe expected
    }
  }

  "a configuration with an alias" should {
    "set the target of the alias, leaving the other settings as they are" in {
      val expected = Validated.valid((true, ScaladocSpacing.Always))

      val actual =
        ConfigCli
          .fromString("scaladoc.align-tags = true")
          .map(c => (c.alignment.scaladoc.tags, c.blankLines.beforeScaladoc))

      actual shouldBe expected
    }

    "accept a setting given again with the same value" in {
      val expected = Validated.valid(true)

      val actual =
        ConfigCli
          .fromString("scaladoc.align-tags = true, alignment.scaladoc.tags = true")
          .map(_.alignment.scaladoc.tags)

      actual shouldBe expected
    }

    "reject a setting given again with another value, naming both" in {
      val message =
        ConfigCli
          .fromString("scaladoc.align-tags = true\nalignment.scaladoc.tags = false")
          .fold(
            _.toList.mkString,
            _ => "valid",
          )

      val expected = true

      val actual = message.contains("scaladoc.align-tags") && message.contains("alignment.scaladoc.tags")

      actual shouldBe expected
    }

    "reject a value its targets do not accept" in {
      val expected = true

      val actual = ConfigCli.fromString("scaladoc.align-tags = always").isInvalid

      actual shouldBe expected
    }

    "set its target when it lives in a section of settings, leaving that section's settings as they are" in {
      val expected = Validated.valid((true, TrailingOverflow.Keep))

      val actual =
        ConfigCli
          .fromString("comments { align-trailing = true, trailing-overflow = keep }")
          .map(c => (c.alignment.trailingComments, c.trailingCommentOverflow))

      actual shouldBe expected
    }

    "reject two construct aliases that give a shared setting different values" in {
      val expected = true

      val actual = ConfigCli.fromString("class.align-extends = true\ntrait.align-extends = false").isInvalid

      actual shouldBe expected
    }

    "resolve a substitution in its value" in {
      val expected = Validated.valid(true)

      val actual =
        ConfigCli.fromString("scaladoc.align-tags = ${alignment.extends-clauses}").map(_.alignment.scaladoc.tags)

      actual shouldBe expected
    }
  }

}
