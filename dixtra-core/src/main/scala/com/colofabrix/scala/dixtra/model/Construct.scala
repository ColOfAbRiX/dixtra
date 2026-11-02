package com.colofabrix.scala.dixtra.model

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
