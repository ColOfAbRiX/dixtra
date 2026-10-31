package com.colofabrix.scala.dixtra.testkit

import com.colofabrix.scala.dixtra.*
import org.scalatest.matchers.*

/**
 * Custom matchers for dixtra formatter specs.
 *
 * Provides the `formatTo` matcher, which formats an input source and compares it to the expected output, printing a
 * readable line-by-line diff when they differ.
 */
trait FormatMatchers {

  extension (body: BodyConfig) {

    /**
     * The same with `policy` for every body after a `=`.
     */
    def withAfterEquals(policy: SingleBodyPolicy): BodyConfig =
      body.copy(
        defBody = policy,
        valBody = policy,
        lazyVal = policy,
        varBody = policy,
        givenAlias = policy,
        typeBody = policy,
        assignment = policy,
        namedArgument = policy,
      )

    /**
     * The same with `policy` for the body of every lambda, as the alias `body.lambda`.
     */
    def withLambda(policy: SingleBodyPolicy): BodyConfig =
      body.copy(lambdaArgument = policy, lambdaBody = policy)

  }

  /**
   * Matcher that formats the input string and asserts it equals `expected`.
   */
  def formatTo(expected: String): Matcher[String] =
    formatTo(expected, Config.default)

  /**
   * Convenience overload: format at the given max column width.
   */
  def formatTo(expected: String, maxColumn: Int): Matcher[String] =
    formatTo(expected, Config.withMaxColumn(maxColumn))

  /**
   * The default configuration with some constructs set to another brace style.
   */
  def withBraces(settings: (Construct, BracePolicy)*): Config =
    Config.default.copy(braces = Config.default.braces.copy(policies = Config.default.braces.policies ++ settings))

  /**
   * `base` with its `selector-chain` settings changed by `f`.
   */
  def withSelectorChain(f: SelectorChainConfig => SelectorChainConfig, base: Config = Config.default): Config =
    base.copy(selectorChain = f(base.selectorChain))

  /**
   * `base` with its `alignment` settings changed by `f`.
   */
  def withAlignment(f: AlignmentConfig => AlignmentConfig, base: Config = Config.default): Config =
    base.copy(alignment = f(base.alignment))

  /**
   * `base` with its `alignment.arrows` settings changed by `f`.
   */
  def withArrows(f: ArrowsConfig => ArrowsConfig, base: Config = Config.default): Config =
    withAlignment(alignment => alignment.copy(arrows = f(alignment.arrows)), base)

  /**
   * `base` with its `body` settings changed by `f`.
   */
  def withBody(f: BodyConfig => BodyConfig, base: Config = Config.default): Config =
    base.copy(body = f(base.body))

  /**
   * `base` with its `blank-lines` settings changed by `f`.
   */
  def withBlankLines(f: BlankLinesConfig => BlankLinesConfig, base: Config = Config.default): Config =
    base.copy(blankLines = f(base.blankLines))

  /**
   * `base` with its `call-site` settings changed by `f`.
   */
  def withCallSite(f: CallSiteConfig => CallSiteConfig, base: Config = Config.default): Config =
    base.copy(callSite = f(base.callSite))

  /**
   * The default configuration with `always-join` for every body after a `=`: a body the author wrote below the `=`
   * moves up to it when it fits.
   */
  def joinAfterEquals: Config =
    Config.default.copy(body = Config.default.body.withAfterEquals(SingleBodyPolicy.AlwaysJoin))

  /**
   * `config` with `selector-chain.inline-chains = keep`: a chain on the line of
   * its `=` / `=>` stays on one line while it fits.
   */
  def keepInlineChains(config: Config): Config =
    config.copy(selectorChain = config.selectorChain.copy(inlineChains = InlineChainPolicy.Keep))

  /**
   * Matcher that formats the input string with the given config and asserts it equals `expected`. With
   * `housekeeping.enforce-eof`, an `expected` text without a final line break gets the one the formatter adds, so that
   * an example can end at its last line.
   */
  def formatTo(expected: String, config: Config): Matcher[String] =
    Matcher { (input: String) =>
      val wanted =
        if config.housekeeping.enforceEof && expected.nonEmpty && !expected.endsWith("\n") then
          expected + "\n"
        else
          expected

      Dixtra.format(input, config) match {
        case Left(error) =>
          MatchResult(
            matches = false,
            rawFailureMessage = s"Formatting failed with error: ${error.getMessage}",
            rawNegatedFailureMessage = "Formatting unexpectedly succeeded",
          )
        case Right(actual) =>
          MatchResult(
            matches = actual == wanted,
            rawFailureMessage = renderDiff(wanted, actual),
            rawNegatedFailureMessage = "Formatted output matched expected but should not have",
          )
      }
    }

  /**
   * Matcher that formats the input string and asserts it equals `expected`, and that `expected` stays as it is.
   */
  def formatStablyTo(expected: String): Matcher[String] =
    formatStablyTo(expected, Config.default)

  /**
   * Convenience overload: format stably at the given max column width.
   */
  def formatStablyTo(expected: String, maxColumn: Int): Matcher[String] =
    formatStablyTo(expected, Config.withMaxColumn(maxColumn))

  /**
   * Matcher that formats the input string with the given config and asserts it equals `expected`, and that `expected`
   * formats to itself with the same config.
   */
  def formatStablyTo(expected: String, config: Config): Matcher[String] =
    Matcher { (input: String) =>
      val first = formatTo(expected, config)(input)
      if first.matches then
        val second = formatTo(expected, config)(expected)
        MatchResult(
          matches = second.matches,
          rawFailureMessage = s"The expected output is not stable. ${second.rawFailureMessage}",
          rawNegatedFailureMessage = second.rawNegatedFailureMessage,
        )
      else
        first
    }

  private def renderDiff(expected: String, actual: String): String =
    val expectedLines = expected.split("\n", -1).toVector
    val actualLines   = actual.split("\n", -1).toVector
    val maxLines      = math.max(expectedLines.length, actualLines.length)

    val diffLines =
      (0 until maxLines)
        .map { i =>
          val e      = expectedLines.lift(i).getOrElse("<missing>")
          val a      = actualLines.lift(i).getOrElse("<missing>")
          val marker = if e == a then "   " else " ! "
          s"$marker line ${i + 1}:\n$marker   expected: ${quote(e)}\n$marker   actual:   ${quote(a)}"
        }
        .mkString("\n")

    s"Formatted output did not match expected.\n$diffLines"

  private def quote(line: String): String =
    "\"" + line.replace("\t", "\\t") + "\""

}
