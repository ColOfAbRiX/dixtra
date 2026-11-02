package com.colofabrix.scala.dixtra.model

/**
 * A definition keyword that can align its `=` with others.
 */
enum DefinitionKind(val key: String) {
  case Val        extends DefinitionKind("val")
  case LazyVal    extends DefinitionKind("lazy-val")
  case Var        extends DefinitionKind("var")
  case Def        extends DefinitionKind("def")
  case GivenAlias extends DefinitionKind("given-alias")
  case TypeDef    extends DefinitionKind("type-def")
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
