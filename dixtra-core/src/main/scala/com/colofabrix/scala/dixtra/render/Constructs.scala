package com.colofabrix.scala.dixtra.render

import com.colofabrix.scala.dixtra.model.Construct
import scala.meta.*

/**
 * The configurable construct a tree is (see [[Construct]]), and the name its `end` marker carries.
 */
private[render] object Constructs {

  /**
   * The construct of a statement whose brace style is configurable.
   */
  def of(tree: Tree): Option[Construct] =
    tree match {
      case defn: Defn.Class if defn.mods.exists(_.is[Mod.Case])     => Some(Construct.CaseClass)
      case defn: Defn.Class if defn.mods.exists(_.is[Mod.Abstract]) => Some(Construct.AbstractClass)
      case _: Defn.Class                                            => Some(Construct.Class)
      case _: Defn.Object                                           => Some(Construct.Object)
      case _: Pkg.Object                                            => Some(Construct.PackageObject)
      case _: Defn.Trait                                            => Some(Construct.Trait)
      case _: Defn.Enum                                             => Some(Construct.Enum)
      case _: Defn.ExtensionGroup                                   => Some(Construct.Extension)
      case _: Defn.Given                                            => Some(Construct.GivenInstance)
      case _: Defn.GivenAlias                                       => Some(Construct.GivenAlias)
      case _: Term.NewAnonymous                                     => Some(Construct.NewAnonymous)
      case _: Term.Try                                              => Some(Construct.Try)
      case _: Term.Match                                            => Some(Construct.Match)
      case _: Term.While                                            => Some(Construct.While)
      case _: Pkg                                                   => Some(Construct.Package)
      case _: Term.If                                               => Some(Construct.If)
      case _: Term.For                                              => Some(Construct.For)
      case _: Term.ForYield                                         => Some(Construct.ForYield)
      case _: Defn.Def                                              => Some(Construct.Def)
      case defn: Defn.Val if defn.mods.exists(_.is[Mod.Lazy])       => Some(Construct.LazyVal)
      case _: Defn.Val                                              => Some(Construct.Val)
      case _: Defn.Var                                              => Some(Construct.Var)
      case _                                                        => None
    }

  /**
   * The name an `end` marker closing `tree` carries, when it can have one.
   */
  def endName(tree: Tree): Option[String] =
    tree match {
      case defn: Defn.Class               => Some(defn.name.value)
      case defn: Defn.Object              => Some(defn.name.value)
      case defn: Pkg.Object               => Some(defn.name.value)
      case defn: Defn.Trait               => Some(defn.name.value)
      case defn: Defn.Enum                => Some(defn.name.value)
      case _: Defn.ExtensionGroup         => Some("extension")
      case defn: Defn.Given               => Some(defn.name.value).filter(_.nonEmpty).orElse(Some("given"))
      case defn: Defn.GivenAlias          => Some(defn.name.value).filter(_.nonEmpty).orElse(Some("given"))
      case defn: Defn.Def                 => Some(defn.name.value)
      case defn: Defn.Val                 => singleName(defn.pats)
      case defn: Defn.Var                 => singleName(defn.pats)
      case pkg: Pkg                       => lastName(pkg.ref)
      case _: Term.NewAnonymous           => Some("new")
      case _: Term.Try                    => Some("try")
      case _: Term.Match                  => Some("match")
      case _: Term.While                  => Some("while")
      case _: Term.If                     => Some("if")
      case _: Term.For | _: Term.ForYield => Some("for")
      case _                              => None
    }

  private def singleName(pats: List[Pat]): Option[String] =
    pats match {
      case (v: Pat.Var) :: Nil => Some(v.name.value)
      case _                   => None
    }

  private def lastName(ref: Term.Ref): Option[String] =
    ref match {
      case n: Term.Name   => Some(n.value)
      case s: Term.Select => Some(s.name.value)
      case _              => None
    }

  /**
   * True when `marker` is the `end` marker of `tree`.
   */
  def closes(tree: Tree, marker: Term.EndMarker): Boolean =
    endName(tree).contains(marker.name.value)

}
