package com.colofabrix.scala.dixtra.cli

import cats.data.Validated
import com.colofabrix.scala.dixtra.*
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.jdk.CollectionConverters.*

/**
 * The aliases of the settings (see [[ConfigAliases]]): their table, and how a configuration that uses them is read.
 */
class ConfigAliasesSpec extends AnyWordSpec with Matchers {

  private val references            = ConfigFactory.parseResources(getClass.getClassLoader, "reference.conf").resolve()
  private val settings: Set[String] =
    references
      .entrySet()
      .asScala
      .map(_.getKey)
      .toSet

  // Values to probe what a setting accepts: every key of the enumerations of the configuration, booleans and numbers.
  private val probes: List[String] =
    (BlankLinePolicy.values.map(_.key) ++ SingleBodyPolicy.values.map(_.key) ++ YieldBodyPolicy.values.map(_.key) ++
    MultiBodyPolicy.values.map(_.key) ++ BracePolicy.values.map(_.key) ++ BracesStyle.values.map(_.key) ++
    ChainHeadPolicy.values.map(_.key) ++ GroupsBreak.values.map(_.key) ++ GroupsSeparator.values.map(_.key) ++
    InlineChainPolicy.values.map(_.key) ++ NestedChainPolicy.values.map(_.key) ++ ScaladocSpacing.values.map(_.key) ++
    TrailingOverflow.values.map(_.key) ++ TrailingCommas.values.map(_.key))
      .toList
      .distinct ++
      List("true", "false", "1", "3")

  "the table of aliases" should {
    "point every alias to settings of reference.conf" in {
      ConfigAliases.all.flatMap(alias => alias.targets.filterNot(settings).map(t => s"${alias.name} -> $t")) shouldBe
      empty
    }

    "never use a setting, or a section holding settings, as an alias" in {
      val clashes =
        ConfigAliases
          .all
          .map(_.name)
          .filter(name => settings.exists(s => s == name || s.startsWith(name + ".")))
      clashes shouldBe empty
    }

    "give each alias at least one target, without duplicates" in {
      ConfigAliases.all.filter(a => a.targets.isEmpty || a.targets.distinct.sizeIs != a.targets.size) shouldBe empty
    }

    "only group settings that accept the same values" in {
      val mismatched =
        ConfigAliases
          .all
          .filter(_.targets.sizeIs > 1)
          .flatMap { alias =>
            val accepted = alias.targets.map(t => t -> probes.filter(v => ConfigCli.fromString(s"$t = $v").isValid))
            accepted
              .headOption
              .toList
              .flatMap { (first, values) =>
                accepted.collect { case (t, other) if other != values => s"${alias.name}: $first and $t" }
              }
          }
      mismatched shouldBe empty
    }
  }

  "a configuration with an alias" should {
    "set the target of the alias, leaving the other settings as they are" in {
      ConfigCli
        .fromString("scaladoc.align-tags = true")
        .map(c => (c.alignment.scaladoc.tags, c.blankLines.beforeScaladoc)) shouldBe
      Validated.valid((true, ScaladocSpacing.Always))
    }

    "accept a setting given again with the same value" in {
      val config = ConfigCli.fromString("scaladoc.align-tags = true, alignment.scaladoc.tags = true")
      config.map(_.alignment.scaladoc.tags) shouldBe Validated.valid(true)
    }

    "reject a setting given again with another value, naming both" in {
      val result = ConfigCli.fromString("scaladoc.align-tags = true\nalignment.scaladoc.tags = false")
      result.fold(_.toList.mkString, _ => "valid") should
      (include("scaladoc.align-tags") and include("alignment.scaladoc.tags"))
    }

    "reject a value its targets do not accept" in {
      ConfigCli.fromString("scaladoc.align-tags = always").isInvalid shouldBe true
    }

    "set its target when it lives in a section of settings, leaving that section's settings as they are" in {
      ConfigCli
        .fromString("comments { align-trailing = true, trailing-overflow = keep }")
        .map(c => (c.alignment.trailingComments, c.trailingCommentOverflow)) shouldBe
      Validated.valid((true, TrailingOverflow.Keep))
    }

    "reject two construct aliases that give a shared setting different values" in {
      ConfigCli.fromString("class.align-extends = true\ntrait.align-extends = false").isInvalid shouldBe true
    }

    "resolve a substitution in its value" in {
      ConfigCli.fromString("scaladoc.align-tags = ${alignment.extends-clauses}").map(_.alignment.scaladoc.tags) shouldBe
      Validated.valid(true)
    }
  }

}
