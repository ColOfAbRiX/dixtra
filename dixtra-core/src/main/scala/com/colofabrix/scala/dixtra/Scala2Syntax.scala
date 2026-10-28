package com.colofabrix.scala.dixtra

import scala.meta.*

/**
 * Recognises the Scala 2 sources that Scalameta also parses as Scala 3 (its Scala 3 dialect accepts most Scala 2
 * syntax, for migration): a source that uses a construct Scala 3 dropped is Scala 2.
 */
private[dixtra] object Scala2Syntax {

  /**
   * A description of the first construct in `tree` that Scala 3 dropped, if any.
   *
   * @param tree The parsed source.
   * @param source The text `tree` was parsed from.
   */
  def dropped(tree: Tree, source: String): Option[String] =
    tree match {
      case _: Term.Do                            => Some("a do-while loop")
      case _: Type.Existential                   => Some("an existential type")
      case _: Defn.Macro                         => Some("a macro definition")
      case d: Defn.Def if isProcedure(d, source) => Some("procedure syntax")
      case _                                     => droppedInChildren(tree, source)
    }

  private def droppedInChildren(tree: Tree, source: String): Option[String] =
    tree
      .children
      .iterator
      .flatMap(dropped(_, source))
      .nextOption()

  // `def f() { ... }`: no `=` before the body (Scalameta gives it a `Unit` result type that is not in the source).
  private def isProcedure(d: Defn.Def, source: String): Boolean =
    val clauseEnds   = d.paramClauseGroups.map(_.pos.end)
    val signatureEnd = clauseEnds.foldLeft(d.name.pos.end)(math.max)
    val beforeBody   = source.slice(signatureEnd, d.body.pos.start)
    !beforeBody.contains('=')

}
