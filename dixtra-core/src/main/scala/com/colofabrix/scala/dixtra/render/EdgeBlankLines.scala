package com.colofabrix.scala.dixtra.render

import com.colofabrix.scala.dixtra.*
import com.colofabrix.scala.dixtra.normalize.SourceEdits
import com.colofabrix.scala.dixtra.render.context.RenderContext

/**
 * The blank lines at the edges of a rendered body (`blank-lines.body-edges`): after the line that opens it and, when
 * braced, before its closing `}`.
 *
 * `keep` makes the two edges agree: a blank line at either edge means a blank line at both, and none at either means
 * none. A scaladoc right after the opening line that wants a blank line before it (`blank-lines.before-scaladoc`)
 * counts as a blank line there, so it gets one unless the policy is `never`.
 */
private[render] object EdgeBlankLines {

  /**
   * The body of `construct` opened by line `openIdx` of `rendered`: braced when that line ends with `{`, braceless
   * otherwise, by the policy of `construct`.
   */
  def apply(rendered: String, openIdx: Int, construct: Construct, ctx: RenderContext): String =
    val policy = ctx.config.blankLines(construct)
    val doc =
      ctx
        .config
        .blankLines
        .beforeScaladoc
    if rendered.split("\n", -1).lift(openIdx).exists(_.stripTrailing.endsWith("{")) then
      braced(rendered, openIdx, policy, doc)
    else
      leading(rendered, openIdx, policy, doc)

  /**
   * A braced body: line `openIdx` of `rendered` opens it and its last line is the closing `}`. Anything else is
   * returned unchanged. `beforeScaladoc` is the policy of the blank line before a scaladoc.
   */
  def braced(rendered: String, openIdx: Int, policy: BlankLinePolicy, beforeScaladoc: ScaladocSpacing): String =
    val lines    = rendered.split("\n", -1).toVector
    val closeIdx = lines.length - 1
    val body     = lines.slice(openIdx + 1, closeIdx)
    val content  = trimBlanks(body)
    val noBody   = closeIdx <= openIdx || lines(closeIdx).trim != "}" || content.isEmpty

    if noBody then
      rendered
    else
      val written =
        body.headOption.exists(_.isBlank) || body.lastOption.exists(_.isBlank) || opensWithDoc(content, beforeScaladoc)
      
      val edge = 
        if wanted(policy, written) then 
          Vector("") 
        else 
          Vector.empty

      (lines.take(openIdx + 1) ++ edge ++ content ++ edge :+ lines(closeIdx)).mkString("\n")

  /**
   * A braceless body: everything after line `openIdx` of `rendered`, which has no closing edge.
   */
  def leading(rendered: String, openIdx: Int, policy: BlankLinePolicy, beforeScaladoc: ScaladocSpacing): String =
    val lines = rendered.split("\n", -1).toVector
    val body  = lines.drop(openIdx + 1)
    val rest  = body.dropWhile(_.isBlank)

    if rest.isEmpty then
      rendered
    else
      val written = body.headOption.exists(_.isBlank) || opensWithDoc(rest, beforeScaladoc)
      val edge    = if wanted(policy, written) then Vector("") else Vector.empty
      (lines.take(openIdx + 1) ++ edge ++ rest).mkString("\n")

  /**
   * Whether `policy` wants blank lines at the edges of a body, `written` telling whether the author left one.
   */
  def wanted(policy: BlankLinePolicy, written: Boolean): Boolean =
    policy match {
      case BlankLinePolicy.Always => true
      case BlankLinePolicy.Never  => false
      case BlankLinePolicy.Keep   => written
    }

  private def trimBlanks(lines: Vector[String]): Vector[String] =
    lines
      .dropWhile(_.isBlank)
      .reverse
      .dropWhile(_.isBlank)
      .reverse

  // Whether the body opens with a scaladoc that wants a blank line before it.
  private def opensWithDoc(lines: Vector[String], beforeScaladoc: ScaladocSpacing): Boolean =
    lines
      .headOption
      .map(_.trim)
      .exists { first => 
        SourceEdits.isScaladoc(first) && beforeScaladoc.wantsBlank(first)
      }

}
