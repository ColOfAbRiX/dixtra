package com.colofabrix.scala.dixtra.cli.config

import com.colofabrix.scala.dixtra.Config
import com.colofabrix.scala.dixtra.model.*
import com.typesafe.config.ConfigObject
import pureconfig.*
import pureconfig.error.*
import pureconfig.generic.ProductHint
import pureconfig.generic.scala3.HintsAwareConfigReaderDerivation.deriveReader
import pureconfig.generic.scala3.HintsAwareConfigWriterDerivation.deriveWriter

/**
 * The pureconfig hints, readers and writers for [[Config]].
 */
object ConfigInstances {

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

  given ConfigReader[SemicolonPolicy] =
    keyed(SemicolonPolicy.values.toList)(_.key)

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

  given ProductHint[OverflowConfig] =
    ProductHint[OverflowConfig](allowUnknownKeys = false)

  given ConfigReader[OverflowConfig] =
    deriveReader[OverflowConfig]

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

  given ConfigReader[ImportBraces] =
    keyed(ImportBraces.values.toList)(_.key)

  given ConfigReader[BraceConfig] =
    ConfigReader.fromCursor { cursor =>
      for
        settings   <- cursor.asObjectCursor
        style      <- settings.atKey("style").flatMap(ConfigReader[BracesStyle].from)
        imports    <- settings.atKey("imports").flatMap(ConfigReader[ImportBraces].from)
        singleExpr <- settings.atKey(singleExprKey).flatMap(singleExprPolicies.from)
        constructs  = settings.withoutKey("style").withoutKey("imports").withoutKey(singleExprKey)
        policies   <- bracePolicies.from(constructs)
      yield
        BraceConfig(style, imports, policies, singleExpr)
    }

  // The section of `braces` with the braces around a single-expression body of each construct
  private val singleExprKey =
    "single-expr"

  private val singleExprPolicies: ConfigReader[Map[Construct, BracePolicy]] =
    bracePolicies.emap { policies =>
      val unsupported = policies.keys.filterNot(BraceConfig.singleExprConstructs.contains)
      val endMarkers  = policies.collect { case (construct, BracePolicy.ForceEnd) => construct.key }

      if unsupported.nonEmpty then
        val expected =
          BraceConfig
            .singleExprConstructs
            .map(_.key)
            .mkString(", ")

        Left(
          cannotConvert(
            unsupported.map(_.key).mkString(", "),
            s"unknown construct, expected one of $expected",
          ),
        )
      else if endMarkers.nonEmpty then
        Left(
          cannotConvert(
            endMarkers.mkString(", "),
            "a single expression has no end marker, so it cannot be force-end",
          ),
        )
      else
        Right(policies)
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

  //  ConfigWriter Instances  //

  // The settings written as the key of their case, like those read

  given ConfigWriter[SingleBodyPolicy] =
    keyedWriter(_.key)

  given ConfigWriter[YieldBodyPolicy] =
    keyedWriter(_.key)

  given ConfigWriter[SemicolonPolicy] =
    keyedWriter(_.key)

  given ConfigWriter[MultiBodyPolicy] =
    keyedWriter(_.key)

  given ConfigWriter[GroupsSeparator] =
    keyedWriter(_.key)

  given ConfigWriter[GroupsBreak] =
    keyedWriter(_.key)

  given ConfigWriter[ChainHeadPolicy] =
    keyedWriter(_.key)

  given ConfigWriter[InlineChainPolicy] =
    keyedWriter(_.key)

  given ConfigWriter[NestedChainPolicy] =
    keyedWriter(_.key)

  given ConfigWriter[BracePolicy] =
    keyedWriter(_.key)

  given ConfigWriter[DefinitionKind] =
    keyedWriter(_.key)

  given ConfigWriter[TrailingOverflow] =
    keyedWriter(_.key)

  given ConfigWriter[TrailingCommas] =
    keyedWriter(_.key)

  given ConfigWriter[LineEnding] =
    keyedWriter(_.key)

  given ConfigWriter[ScaladocSpacing] =
    keyedWriter(_.key)

  given ConfigWriter[BlankLinePolicy] =
    keyedWriter(_.key)

  given ConfigWriter[BracesStyle] =
    keyedWriter(_.key)

  given ConfigWriter[ImportBraces] =
    keyedWriter(_.key)

  given ConfigWriter[Map[Construct, BlankLinePolicy]] =
    ConfigWriter[Map[String, BlankLinePolicy]].contramap(
      edges => edges.map((construct, policy) => construct.key -> policy),
    )

  // The brace settings as one section, the style, the import braces and the single-expression braces beside the policy
  // of each construct: the shape the reader expects
  given ConfigWriter[BraceConfig] =
    ConfigWriter.fromFunction { braces =>
      def section(policies: Map[Construct, BracePolicy]) =
        ConfigWriter[Map[String, BracePolicy]].to(policies.map((construct, policy) => construct.key -> policy))

      val style      = ConfigWriter[BracesStyle].to(braces.style)
      val imports    = ConfigWriter[ImportBraces].to(braces.imports)
      val singleExpr = section(braces.singleExpressions)

      section(braces.policies) match {
        case written: ConfigObject =>
          written
            .withValue("style", style)
            .withValue("imports", imports)
            .withValue(singleExprKey, singleExpr)
        case other =>
          other
      }
    }

  given ConfigWriter[SelectorChainConfig] =
    deriveWriter[SelectorChainConfig]

  given ConfigWriter[BodyConfig] =
    deriveWriter[BodyConfig]

  given ConfigWriter[DefinitionSiteConfig] =
    deriveWriter[DefinitionSiteConfig]

  given ConfigWriter[CallSiteConfig] =
    deriveWriter[CallSiteConfig]

  given ConfigWriter[AlignmentConfig] =
    deriveWriter[AlignmentConfig]

  given ConfigWriter[PathsConfig] =
    deriveWriter[PathsConfig]

  given ConfigWriter[ArrowsConfig] =
    deriveWriter[ArrowsConfig]

  given ConfigWriter[ScaladocAlignmentConfig] =
    deriveWriter[ScaladocAlignmentConfig]

  given ConfigWriter[HousekeepingConfig] =
    deriveWriter[HousekeepingConfig]

  given ConfigWriter[OverflowConfig] =
    deriveWriter[OverflowConfig]

  given ConfigWriter[BlankLinesConfig] =
    deriveWriter[BlankLinesConfig]

  given ConfigWriter[Config] =
    deriveWriter[Config]

  private def keyedWriter[A](key: A => String): ConfigWriter[A] =
    ConfigWriter[String].contramap(key)

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
