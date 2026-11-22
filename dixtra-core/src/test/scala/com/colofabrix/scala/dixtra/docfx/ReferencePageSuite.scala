package com.colofabrix.scala.dixtra.docfx

import com.colofabrix.scala.dixtra.docfx.ReferenceDocs.{ loadPages, Page }

import java.nio.file.{ Files, Path }

/**
 * Resolves the settings-reference directory and checks one page at a time.
 *
 * Each page has its own spec, so a failure names the page it is about and the pages stay independent of one another.
 * The `dixtra.docs.write` property turns the check into a regeneration: the page is rewritten instead of asserted.
 */
object ReferencePageSuite {

  private val relativeDocs = Path.of("dixtra-microsite", "docs", "docs", "reference")

  val docsDir: Option[Path] =
    val explicit = sys.props.get("dixtra.docsDir").toList.map(Path.of(_))
    explicit
      .concat(ancestors(Path.of(sys.props.getOrElse("user.dir", ".")).toAbsolutePath).map(_.resolve(relativeDocs)))
      .find(Files.isDirectory(_))

  val writeMode: Boolean =
    sys.props.get("dixtra.docs.write").contains("true")

  def page(name: String): Either[String, Page] =
    docsDir match {
      case None =>
        Left(s"no settings-reference directory found, looked in ${describeCandidates}")
      case Some(dir) =>
        loadPages(dir).find(_.name == name).toRight(s"'$name' not found in ${dir.toString}")
    }

  def check(name: String): List[String] =
    page(name) match {
      case Left(error) =>
        List(error)
      case Right(found) =>
        if writeMode then ReferenceDocsChecker.rewrite(found) else ReferenceDocsChecker.validate(found)
    }

  private def describeCandidates: String =
    (sys.props.get("dixtra.docsDir").toList :+ relativeDocs.toString).mkString(", ")

  private def ancestors(path: Path): List[Path] =
    if path == null then Nil else path :: ancestors(path.getParent)

}
