package com.colofabrix.scala.dixtra.model

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
 * @param typeDef The type after the `=` of a type alias.
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
  typeDef: SingleBodyPolicy,
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
      case DefinitionKind.TypeDef    => typeDef
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
 * the bodies are all on the lines of their separators, or all below them, the policy decides whether they stay; when
 * they are mixed, it decides which way they all go. A group that should be on the lines of its separators goes below
 * them when one body cannot be joined: it does not fit there on one line (once the arrows of the cases are aligned), a
 * comment follows its separator, or it holds an if, a match or a try. A group of one body is never mixed, so both
 * `-mixed` policies leave it as written.
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
