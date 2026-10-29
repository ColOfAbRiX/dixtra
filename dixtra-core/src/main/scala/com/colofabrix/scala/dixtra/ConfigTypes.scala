package com.colofabrix.scala.dixtra

/**
 * How a construct writes its body.
 */
enum BracePolicy(val key: String) {

  /**
   * In curly braces: `{ ... }` (for `if` / `while`: `if (c) { ... }`).
   */
  case ForceBraces extends BracePolicy("force-braces")

  /**
   * Braceless, closed by an `end` marker.
   */
  case ForceEnd extends BracePolicy("force-end")

  /**
   * Braceless, without an `end` marker: the body is only indented.
   */
  case ForceIndent extends BracePolicy("force-indent")

  /**
   * As the author wrote it.
   */
  case Keep extends BracePolicy("keep")

}

/**
 * A construct whose brace style and edge blank lines are configurable.
 *
 * @param key The name of the setting in the configuration.
 * @param allowsEnd Whether the construct can be closed by an `end` marker.
 */
enum Construct(val key: String, val allowsEnd: Boolean) {
  case Class          extends Construct("class", true)
  case AbstractClass  extends Construct("abstract-class", true)
  case CaseClass      extends Construct("case-class", true)
  case Object         extends Construct("object", true)
  case PackageObject  extends Construct("package-object", true)
  case Trait          extends Construct("trait", true)
  case Enum           extends Construct("enum", true)
  case Extension      extends Construct("extension", true)
  case GivenInstance  extends Construct("given-instance", true)
  case GivenAlias     extends Construct("given-alias", true)
  case NewAnonymous   extends Construct("new-anonymous", true)
  case Try            extends Construct("try", true)
  case Match          extends Construct("match", true)
  case While          extends Construct("while", true)
  case LambdaArgument extends Construct("lambda-argument", false)
  case Package        extends Construct("package", true)
  case If             extends Construct("if", true)
  case For            extends Construct("for", true)
  case ForYield       extends Construct("for-yield", true)
  case Def            extends Construct("def", true)
  case Val            extends Construct("val", true)
  case LazyVal        extends Construct("lazy-val", true)
  case Var            extends Construct("var", true)
  case CaseBody       extends Construct("case-body", false)
  case LambdaBody     extends Construct("lambda-body", false)

  /**
   * A block of statements given as an argument or an operand, without lambda parameters (`Resource.make { ... }`), and
   * not a test block.
   */
  case BlockArgument extends Construct("block-argument", false)

  /**
   * The block of a test case: a block of statements given to a call labelled by a string (`"a test" in { ... }`,
   * `test("a test") { ... }`).
   */
  case TestBlock extends Construct("test-block", false)
}

/**
 * Where the braces go relative to the code around them.
 */
enum BracesStyle(val key: String) {

  /**
   * `{` at the end of the header line, `} else {` on one line.
   */
  case OneTrueBrace extends BracesStyle("1tbs")

  /**
   * As 1TBS, with `else`, `catch`, `finally` and `yield` starting the line after the closing `}`.
   */
  case Stroustrup extends BracesStyle("stroustrup")

  /**
   * As Stroustrup, with every opening `{` of a body on its own line.
   */
  case Allman extends BracesStyle("allman")

}

/**
 * The braces: where they go, and the brace style of each construct; a construct without a setting is
 * [[BracePolicy.Keep]].
 *
 * @param style Where the braces go relative to the code around them.
 * @param policies The brace policy of each construct.
 */
final case class BraceConfig(style: BracesStyle, policies: Map[Construct, BracePolicy]) {

  /**
   * The brace style of `construct`.
   */
  def apply(construct: Construct): BracePolicy =
    policies.getOrElse(construct, BracePolicy.Keep)

}

object BraceConfig {
  import BracePolicy.*
  import Construct.*

  val default: BraceConfig =
    BraceConfig(
      BracesStyle.OneTrueBrace,
      Map(
        Class          -> ForceBraces,
        AbstractClass  -> ForceBraces,
        CaseClass      -> ForceBraces,
        Object         -> ForceBraces,
        Trait          -> ForceBraces,
        Enum           -> ForceBraces,
        Extension      -> ForceBraces,
        GivenInstance  -> ForceBraces,
        NewAnonymous   -> ForceBraces,
        Try            -> ForceBraces,
        Match          -> ForceBraces,
        While          -> ForceBraces,
        LambdaArgument -> ForceBraces,
        BlockArgument  -> ForceBraces,
        TestBlock      -> ForceBraces,
        Package        -> ForceIndent,
        If             -> ForceIndent,
        For            -> ForceIndent,
        ForYield       -> ForceIndent,
        Def            -> ForceIndent,
        Val            -> ForceIndent,
        LazyVal        -> ForceIndent,
        Var            -> ForceIndent,
        CaseBody       -> ForceIndent,
        LambdaBody     -> Keep,
        GivenAlias     -> Keep,
        PackageObject  -> Keep,
      ),
    )
}

/**
 * Where blank lines go.
 *
 * @param bodyEdges The blank lines at the edges of the body of each construct: after its opening `{` (or its header,
 *                  braceless) and before its closing `}`. `keep` (the default for a construct not listed) makes the two
 *                  edges agree: a blank line at either edge means a blank line at both.
 * @param betweenAlignmentGroups The blank line between two adjacent one-line definitions of different alignment groups.
 * @param beforeScaladoc The blank line before a scaladoc. At the start of a body, a scaladoc that wants one counts as a
 *                       blank line written at its opening edge, and the body edges decide.
 */
final case class BlankLinesConfig(
  bodyEdges: Map[Construct, BlankLinePolicy],
  betweenAlignmentGroups: BlankLinePolicy,
  beforeScaladoc: ScaladocSpacing,
) {

  /**
   * The edge blank-line policy of `construct`.
   */
  def apply(construct: Construct): BlankLinePolicy =
    bodyEdges.getOrElse(construct, BlankLinePolicy.Keep)

}

object BlankLinesConfig {

  /**
   * The default edge blank lines: kept as the author wrote them (made symmetric) for containers and test blocks, always
   * for a package, never for the other bodies.
   */
  val default: BlankLinesConfig =
    import Construct.*
    import BlankLinePolicy.*
    BlankLinesConfig(
      Map(
        Class          -> Keep,
        AbstractClass  -> Keep,
        CaseClass      -> Keep,
        Object         -> Keep,
        PackageObject  -> Keep,
        Trait          -> Keep,
        Enum           -> Keep,
        Extension      -> Keep,
        GivenInstance  -> Keep,
        GivenAlias     -> Never,
        NewAnonymous   -> Never,
        Try            -> Never,
        Match          -> Never,
        While          -> Never,
        LambdaArgument -> Never,
        BlockArgument  -> Never,
        TestBlock      -> Keep,
        If             -> Never,
        For            -> Never,
        ForYield       -> Never,
        Def            -> Never,
        Val            -> Never,
        LazyVal        -> Never,
        Var            -> Never,
        CaseBody       -> Never,
        LambdaBody     -> Never,
        Package        -> Always,
      ),
      betweenAlignmentGroups = Keep,
      beforeScaladoc = ScaladocSpacing.Always,
    )

}

/**
 * Whether the first selections of a chain stay together with its receiver.
 */
enum ChainHeadPolicy(val key: String) {

  /**
   * Always together, even when the author breaks the line between them.
   */
  case AlwaysJoin extends ChainHeadPolicy("always-join")

  /**
   * Together unless the author breaks the line between them.
   */
  case Keep extends ChainHeadPolicy("keep")

  /**
   * Never together: they break like any other selection.
   */
  case AlwaysBreak extends ChainHeadPolicy("always-break")

}

/**
 * When select chains break.
 *
 * @param breakAtCount A chain with at least this many selections breaks one selection per line when the author writes
 *                     it below `=` / `=>`.
 * @param namespacePath The lowercase names of a chain up to and including the first capitalised one
 *                      (`java.nio.file.Files`).
 * @param objectMember The first member selected on a capitalised name (`Files.exists`).
 * @param receiverMinLength A chain breaks after a receiver name at least this long; a shorter one keeps its first
 *                          selection on its line (`v.trim`), and that selection still counts towards `breakAtCount`.
 * @param inlineChains Whether a chain on the line of its `=` / `=>` breaks at `breakAtCount` selections too.
 * @param inFor Whether a chain in the enumerators of a `for` keeps the line breaks of its author.
 * @param inCall Whether a chain in the arguments of a call keeps the line breaks of its author.
 */
final case class SelectorChainConfig(
  breakAtCount: Int,
  namespacePath: ChainHeadPolicy,
  objectMember: ChainHeadPolicy,
  receiverMinLength: Int,
  inlineChains: InlineChainPolicy,
  inFor: NestedChainPolicy,
  inCall: NestedChainPolicy,
)

/**
 * How a chain nested where [[SelectorChainConfig.breakAtCount]] does not apply breaks: in the enumerators of a `for`
 * (its generators, guards and value definitions, and what they hold, but not the `yield` expression or the `do` body),
 * and in the arguments of a call, positional or named (but not the bodies of the lambdas and blocks passed to it). A
 * chain there never breaks at `breakAtCount` selections, and always breaks when it does not fit.
 */
enum NestedChainPolicy(val key: String) {

  /**
   * On one line while it fits, even when the author broke it.
   */
  case NeverBreak extends NestedChainPolicy("never-break")

  /**
   * Broken where the author broke it, like any other chain.
   */
  case Keep extends NestedChainPolicy("keep")

}

/**
 * Whether a chain on the line of its `=` / `=>` breaks at [[SelectorChainConfig.breakAtCount]] selections.
 */
enum InlineChainPolicy(val key: String) {

  /**
   * On one line while it fits.
   */
  case Keep extends InlineChainPolicy("keep")

  /**
   * One selection per line from `break-at-count` selections, like a chain below the `=` / `=>`.
   */
  case BreakAtCount extends InlineChainPolicy("break-at-count")

}

/**
 * Where the bodies go relative to their separators, by construct. The bodies of a construct with several of them (the
 * branches of an if/else, the cases of a match, the bodies of a try) are a group, and a group is never mixed: either
 * all its bodies are on the lines of their separators, or all are below them.
 *
 * @param defBody The body after the `=` of a `def`.
 * @param valBody The body after the `=` of a `val`.
 * @param lazyVal The body after the `=` of a `lazy val`.
 * @param varBody The body after the `=` of a `var`.
 * @param givenAlias The body after the `=` of a `given` alias.
 * @param typeBody The type after the `=` of a type alias.
 * @param assignment The value after the `=` of an assignment (`x = 1`).
 * @param namedArgument The value after the `=` of a named argument.
 * @param lambdaArgument The body after the `=>` of a lambda passed in braces or after a colon (`f { x => ... }`, `f:
 *                       x => ...`).
 * @param lambdaBody The body after the `=>` of any other lambda.
 * @param blockArgument The statements of a block given as an argument, after its `{` or its colon (`f { ... }`, `f:
 *                      ...`).
 * @param testBlock The statements of a test block, after its `{` or its colon (`"a test" in { ... }`, `test("a test"):
 *                  ...`).
 * @param ifBody The bodies after the `then` and `else` of an if/else, an `else if` chain being one group.
 * @param caseBody The bodies after the `=>` of the cases of a match, a `{ case ... }` or a `catch`, one group.
 * @param tryBody The bodies after the `try` and the `finally` of a try.
 * @param forBody The body after the `do` of a `for` loop.
 * @param forYield The expression after the `yield` of a for-comprehension.
 * @param whileBody The body after the `do` of a `while` loop.
 */
final case class BodyConfig(
  defBody: SingleBodyPolicy,
  valBody: SingleBodyPolicy,
  lazyVal: SingleBodyPolicy,
  varBody: SingleBodyPolicy,
  givenAlias: SingleBodyPolicy,
  typeBody: SingleBodyPolicy,
  assignment: SingleBodyPolicy,
  namedArgument: SingleBodyPolicy,
  lambdaArgument: SingleBodyPolicy,
  lambdaBody: SingleBodyPolicy,
  blockArgument: SingleBodyPolicy,
  testBlock: SingleBodyPolicy,
  ifBody: MultiBodyPolicy,
  caseBody: MultiBodyPolicy,
  tryBody: MultiBodyPolicy,
  forBody: SingleBodyPolicy,
  forYield: YieldBodyPolicy,
  whileBody: SingleBodyPolicy,
) {

  /**
   * The policy of the body after the `=` of a definition of `kind`.
   */
  def afterEquals(kind: DefinitionKind): SingleBodyPolicy =
    kind match {
      case DefinitionKind.Val        => valBody
      case DefinitionKind.LazyVal    => lazyVal
      case DefinitionKind.Var        => varBody
      case DefinitionKind.Def        => defBody
      case DefinitionKind.GivenAlias => givenAlias
      case DefinitionKind.Type       => typeBody
    }

}

/**
 * Where the body of a construct with a single body goes relative to its separator. Whatever the policy, the body goes
 * below its separator when it cannot be on the separator line: it does not fit there on one line, or a comment follows
 * the separator.
 */
enum SingleBodyPolicy(val key: String) {

  /**
   * Where the author wrote it.
   */
  case Keep extends SingleBodyPolicy("keep")

  /**
   * On the line of its separator.
   */
  case AlwaysJoin extends SingleBodyPolicy("always-join")

  /**
   * Below its separator.
   */
  case AlwaysBreak extends SingleBodyPolicy("always-break")

}

/**
 * Where the expression after the `yield` of a for-comprehension goes: the values of [[SingleBodyPolicy]], and
 * `break-complex`.
 */
enum YieldBodyPolicy(val key: String) {

  /**
   * Where the author wrote it.
   */
  case Keep extends YieldBodyPolicy("keep")

  /**
   * On the line of the `yield`.
   */
  case AlwaysJoin extends YieldBodyPolicy("always-join")

  /**
   * Below the `yield`.
   */
  case AlwaysBreak extends YieldBodyPolicy("always-break")

  /**
   * On the line of the `yield` when it is simple, a single name, path (`ExitCode.Success`) or literal; below it
   * otherwise.
   */
  case BreakComplex extends YieldBodyPolicy("break-complex")

}

/**
 * Where the bodies of a construct with several of them go relative to their separators. A group is never mixed: when
 * the bodies are all on the lines of
 * their separators, or all below them, the policy decides whether they stay;
 * when they are mixed, it decides which way they all go. A group that should be on the lines of its separators goes
 * below them when one body cannot be joined: it does not fit there on one line (once the arrows of the cases are
 * aligned), a comment follows its separator, or it holds an if, a match or a try. A group of one body is never mixed,
 * so both `-mixed` policies leave it as written.
 */
enum MultiBodyPolicy(val key: String) {

  /**
   * A mixed group is joined on the lines of the separators; a group that is all on them, or all below them, stays as it
   * is.
   */
  case JoinMixed extends MultiBodyPolicy("join-mixed")

  /**
   * On the lines of their separators.
   */
  case AlwaysJoin extends MultiBodyPolicy("always-join")

  /**
   * A mixed group goes below the separators; a group that is all on them, or all below them, stays as it is.
   */
  case BreakMixed extends MultiBodyPolicy("break-mixed")

  /**
   * Below their separators.
   */
  case AlwaysBreak extends MultiBodyPolicy("always-break")

}

/**
 * Where `)(` goes between the groups of parameters or arguments of a broken definition or call.
 */
enum GroupsSeparator(val key: String) {

  /**
   * At the start of the next group, on the line of the closing `)`: `)(numeric: Numeric[A],`.
   */
  case NextGroup extends GroupsSeparator("next-group")

  /**
   * At the end of the last element of the group before: `second: Boolean)(`, the elements of the next group below it.
   */
  case PreviousGroup extends GroupsSeparator("previous-group")

}

/**
 * The layout of signatures (`def`, class constructors).
 *
 * @param groupsSeparator Where `)(` goes between broken parameter groups.
 */
final case class DefinitionSiteConfig(groupsSeparator: GroupsSeparator)

/**
 * Which argument groups of a call (`f(a)(b)`) break when they do not fit on one line.
 */
enum GroupsBreak(val key: String) {

  /**
   * Every group, each argument on its own line.
   */
  case BreakAll extends GroupsBreak("break-all")

  /**
   * Only the last group while the groups before it fit on the line of the call (`Option.when(cond)(` and the body
   * below); every group when they do not.
   */
  case LastFirst extends GroupsBreak("last-first")

  /**
   * Each group by itself, as for a call with one group: a group breaks when the author broke it or it does not fit from
   * where it starts.
   */
  case Independent extends GroupsBreak("independent")

}

/**
 * The layout of calls.
 *
 * @param groupsSeparator Where `)(` goes between broken argument groups.
 * @param groupsBreak Which argument groups break.
 */
final case class CallSiteConfig(groupsSeparator: GroupsSeparator, groupsBreak: GroupsBreak)

/**
 * The files and directories to format.
 *
 * Patterns are globs matched against paths relative to the directory being formatted, with `/` as separator: `*`
 * matches within a path segment, `**` across segments, `?` one character.
 *
 * @param included A file is formatted only if its path matches one of these.
 * @param excluded A file is not formatted if its path, or the path of a directory above it, matches one of these.
 */
final case class PathsConfig(included: List[String], excluded: List[String])

object PathsConfig {

  /**
   * Every file.
   */
  val everything: PathsConfig =
    PathsConfig(included = List("**"), excluded = Nil)

}

/**
 * A definition keyword that can align its `=` with others.
 */
enum DefinitionKind(val key: String) {
  case Val        extends DefinitionKind("val")
  case LazyVal    extends DefinitionKind("lazy-val")
  case Var        extends DefinitionKind("var")
  case Def        extends DefinitionKind("def")
  case GivenAlias extends DefinitionKind("given-alias")
  case Type       extends DefinitionKind("type")
}

/**
 * Whether a blank line goes before a scaladoc that follows a line of code.
 */
enum ScaladocSpacing(val key: String) {

  /**
   * Before every scaladoc.
   */
  case Always extends ScaladocSpacing("always")

  /**
   * Before every multi-line scaladoc; a one-line scaladoc as the author wrote it.
   */
  case AlwaysMultiline extends ScaladocSpacing("always-multiline")

  /**
   * As the author wrote it.
   */
  case Keep extends ScaladocSpacing("keep")

  /**
   * Never.
   */
  case Never extends ScaladocSpacing("never")

  /**
   * Whether a scaladoc whose first line is `firstLine` wants a blank line before it.
   */
  def wantsBlank(firstLine: String): Boolean =
    this match {
      case Always          => true
      case AlwaysMultiline => !firstLine.contains("*/")
      case Keep | Never    => false
    }

}

/**
 * Whether a blank line separates two things.
 */
enum BlankLinePolicy(val key: String) {

  /**
   * As the author wrote it.
   */
  case Keep extends BlankLinePolicy("keep")

  /**
   * Always a blank line.
   */
  case Always extends BlankLinePolicy("always")

  /**
   * Never a blank line.
   */
  case Never extends BlankLinePolicy("never")

}

/**
 * Which arrows align.
 *
 * @param cases Whether consecutive one-line `case` clauses align `=>`.
 * @param pairs Whether the `a -> b` elements of an exploded argument list align `->`.
 * @param forEnumerators Whether the enumerators of a for-comprehension align their expressions.
 */
final case class ArrowsConfig(cases: Boolean, pairs: Boolean, forEnumerators: Boolean)

/**
 * What aligns in scaladoc comments.
 *
 * @param tags Whether the descriptions of a scaladoc's tag list align on one column; otherwise each tag's description
 *             starts after its own name.
 */
final case class ScaladocAlignmentConfig(tags: Boolean)

/**
 * Whether the last element of a list laid out one element per line (arguments, parameters, type parameters, tuple
 * elements) is followed by a comma.
 */
enum TrailingCommas(val key: String) {

  /**
   * A comma after the last element too.
   */
  case Always extends TrailingCommas("always")

  /**
   * No comma after the last element.
   */
  case Never extends TrailingCommas("never")

}

/**
 * What happens to a line of code that goes past the maximum width only because of the comment at its end.
 */
enum TrailingOverflow(val key: String) {

  /**
   * The line stays as it is: a trailing comment does not count in the width.
   */
  case Keep extends TrailingOverflow("keep")

  /**
   * The comment moves to a line of its own above the code, at its indent.
   */
  case MoveAbove extends TrailingOverflow("move-above")

}

/**
 * The line break written at the end of each line.
 */
enum LineEnding(val key: String) {

  /**
   * `\n`, as on Linux and macOS.
   */
  case Lf extends LineEnding("lf")

  /**
   * `\r\n`, as on Windows.
   */
  case Crlf extends LineEnding("crlf")

  /**
   * `\r`, as on classic macOS (up to Mac OS 9).
   */
  case Cr extends LineEnding("cr")

  /**
   * The line break of the file as it is written.
   */
  case Keep extends LineEnding("keep")

}

/**
 * The housekeeping of a whole file.
 *
 * @param trimTrailingWhitespace Whether the spaces and tabs at the end of each line are removed; those inside a string
 *                               literal always stay.
 * @param enforceEof Whether the file ends with exactly one line break.
 * @param lineEnding The line break written at the end of each line.
 */
final case class HousekeepingConfig(trimTrailingWhitespace: Boolean, enforceEof: Boolean, lineEnding: LineEnding)

/**
 * What aligns with what.
 *
 * @param assignmentGroups Adjacent one-line definitions align their `=` when their keywords are in the same group; a
 *                         keyword in no group never aligns. A keyword is in at most one group.
 * @param operators The operators (`=`, `+=`, `:=`, `%`, ...) that adjacent one-line statements, and adjacent one-line
 *                  elements of a list laid out one per line, align column by column.
 * @param arrows Which arrows align.
 * @param extendsClauses Whether adjacent one-line definitions align their `extends` keyword.
 * @param namedArguments Whether the `=` of adjacent one-line named arguments of a call laid out one per line align.
 * @param trailingComments Whether the comments at the end of consecutive lines of code start on one column.
 * @param scaladoc What aligns in scaladoc comments.
 * @param acrossComments Whether a comment on its own line between two items (definitions, statements, cases, list
 *                       elements, enumerators) keeps them in one alignment group.
 */
final case class AlignmentConfig(
  assignmentGroups: List[Set[DefinitionKind]],
  operators: List[String],
  arrows: ArrowsConfig,
  extendsClauses: Boolean,
  namedArguments: Boolean,
  trailingComments: Boolean,
  scaladoc: ScaladocAlignmentConfig,
  acrossComments: Boolean,
) {

  /**
   * The index of the group of `kind`, if it is in one.
   */
  def groupOf(kind: DefinitionKind): Option[Int] =
    assignmentGroups.indexWhere(_.contains(kind)) match {
      case -1 => None
      case i  => Some(i)
    }

  /**
   * Whether `a` and `b` align with each other.
   */
  def sameGroup(a: DefinitionKind, b: DefinitionKind): Boolean =
    groupOf(a).exists(groupOf(b).contains)

}
