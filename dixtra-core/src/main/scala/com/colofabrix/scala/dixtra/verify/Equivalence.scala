package com.colofabrix.scala.dixtra.verify

import com.colofabrix.scala.dixtra.SourceDialect
import com.colofabrix.scala.dixtra.normalize.SourceEdits
import scala.annotation.tailrec
import scala.meta.*
import scala.meta.tokens.Token
import scala.util.control.NonFatal

/**
 * Output verification: the check behind the formatter's non-corruption guarantee.
 *
 * A rendering is only accepted when it parses back to a tree equivalent to the original and keeps every comment.
 * Equivalence ignores the layout rewrites the formatter makes on purpose (a single-expression `{ x }` block unwrapped
 * to `x`) and the reindentation of multiline comments.
 */
object Equivalence {

  /**
   * True when `rendered` is a faithful rendering of the statement `original`: it parses as a statement with an
   * equivalent structure and preserves the comments of the original source slice.
   */
  def isEquivalentStat(original: Tree, rendered: String): Boolean =
    parse[Stat](rendered).exists { after =>
      val sameStructure = equivalent(original, after)
      val sameComments  = comments(original.pos.text) == comments(rendered)

      sameStructure && sameComments
    }

  /**
   * True when `rendered` is a faithful rendering of the package clause `original` as a whole compilation unit (a
   * package clause does not parse as a statement on its own).
   */
  def isEquivalentPackage(original: Pkg, rendered: String): Boolean =
    parse[Source](rendered).exists { after =>
      val singleEquivalentStat = after.stats.sizeIs == 1 && after.stats.forall(equivalent(original, _))
      singleEquivalentStat && comments(original.pos.text) == comments(rendered)
    }

  /**
   * True when the formatted text of a whole compilation unit is equivalent to the parsed original: same structure and
   * same comments.
   */
  def isEquivalentSource(original: Source, originalText: String, formatted: String): Boolean =
    isEquivalentSource(original, originalText, formatted, SourceDialect.scala)

  /**
   * True when the formatted text of a whole compilation unit, parsed with `dialect`, is equivalent to the parsed
   * original: same structure and same comments. An unchanged text is not parsed again.
   */
  def isEquivalentSource(original: Source, originalText: String, formatted: String, dialect: Dialect): Boolean =
    val unchanged = formatted == originalText
    val reparses =
      parse[Source](formatted, dialect).exists { after =>
        val sameStructure = equivalent(original, after)
        val sameComments  = comments(originalText) == comments(formatted)

        sameStructure && sameComments
      }

    unchanged || reparses

  private def parse[T <: Tree](text: String)(using parser: scala.meta.parsers.Parse[T]): Option[T] =
    parse[T](text, SourceDialect.scala)

  private def parse[T <: Tree](text: String, dialect: Dialect)(using parser: scala.meta.parsers.Parse[T]): Option[T] =
    try {
      parser(Input.String(text), dialect).toOption
    } catch {
      case NonFatal(_) => None
    }

  // Two trees with the same structure once the intentional rewrites are normalised away: `end` markers are dropped (the
  // brace style adds and removes them), and a single-expression block `{ x }` is equivalent to `x`. Leaves compare
  // their Scalameta structure; inner nodes their kind and their children. The trees are walked together, stopping at
  // the first difference.
  private def equivalent(a: Tree, b: Tree): Boolean =
    (unwrapped(a), unwrapped(b)) match {
      case (x, y) if x.children.isEmpty && y.children.isEmpty =>
        sameLeaf(x, y)
      case (x, y) if x.children.nonEmpty && y.children.nonEmpty =>
        val xs       = x.children.filterNot(_.is[Term.EndMarker])
        val ys       = y.children.filterNot(_.is[Term.EndMarker])
        val sameKind = x.productPrefix == y.productPrefix
        val sameSize = xs.sizeCompare(ys) == 0
        sameKind && sameSize && xs.lazyZip(ys).forall(equivalent)
      case (x, y) =>
        canonical(x) == canonical(y)
    }

  // Two leaves with the same structure. A leaf holds only values (a name, a literal), so equal values mean an equal
  // structure, which is printed only for leaves that differ.
  private def sameLeaf(x: Tree, y: Tree): Boolean =
    val sameValues    = x.productPrefix == y.productPrefix && x.productIterator.sameElements[Any](y.productIterator)
    val sameStructure = x.structure == y.structure

    sameValues || sameStructure

  // A tree without the rewrites of `equivalent` at its root.
  @tailrec
  private def unwrapped(tree: Tree): Tree =
    tree match {
      case Term.Block(stats) if stats.exists(_.is[Term.EndMarker]) =>
        unwrapped(Term.Block(stats.filterNot(_.is[Term.EndMarker])))
      case Term.Block((single: Term) :: Nil) =>
        unwrapped(single)
      case other =>
        other
    }

  // The structure of `equivalent` as text, for a leaf compared with a node.
  private def canonical(tree: Tree): String =
    tree match {
      case Term.Block(stats) if stats.exists(_.is[Term.EndMarker]) =>
        canonical(Term.Block(stats.filterNot(_.is[Term.EndMarker])))
      case Term.Block((single: Term) :: Nil) =>
        canonical(single)
      case leaf if leaf.children.isEmpty =>
        leaf.structure
      case node =>
        node.productPrefix + node.children.filterNot(_.is[Term.EndMarker]).map(canonical).mkString("(", ", ", ")")
    }

  // The words of the comments in source order, without delimiters and gutters, so that reindenting, reflowing and
  // re-laying out comments (the scaladoc gutter, the space after `//`) is not seen as a change.
  private def comments(text: String): List[String] =
    SourceEdits
      .tokens(text)
      .map(_.collect { case c: Token.Comment => commentWords(c.text) }.toList.flatten)
      .getOrElse(Nil)

  private def commentWords(comment: String): List[String] =
    val content =
      if comment.startsWith("//") then
        comment.drop(2)
      else
        comment
          .stripPrefix("/**")
          .stripPrefix("/*")
          .stripSuffix("*/")

    content
      .split("\n")
      .toList
      .map(line => line.trim.stripPrefix("*"))
      .flatMap(_.split("\\s+").toList)
      .filter(word => word.nonEmpty && !word.forall(_ == '*'))

}
