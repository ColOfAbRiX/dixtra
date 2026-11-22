package com.colofabrix.scala.dixtra.docfx

import com.colofabrix.scala.dixtra.{ Config, Dixtra }
import com.colofabrix.scala.dixtra.docfx.ReferenceDocs.{ Page, Section, Subsection }
import java.nio.file.Files

/**
 * Compares a settings-reference page with what the formatter actually produces, and regenerates it.
 *
 * Every check reads the page as written, rebuilds the configuration from each code block's `// With settings:` trailer,
 * and formats the section's `Before` snippet with it.
 */
object ReferenceDocsChecker {

  def validate(page: Page, settings: SettingsResolver = ReferenceSettings): List[String] =
    page.sections.flatMap(validateSection(settings, _)) ++ distinctnessProblems(page)

  def rewrite(page: Page, settings: SettingsResolver = ReferenceSettings): List[String] =
    val replacements =
      page
        .sections
        .flatMap { section =>
          section.subsections.map(subsection => (subsection, section.before))
        }

    val formatted =
      replacements.map { case (subsection, before) =>
        val label  = s"${subsection.key} = ${subsection.value}"
        val result = before.map(format(settings, label, _, subsection.trailer))
        (subsection, result)
      }

    val failures = formatted.collect { case (_, Some(Left(error))) => error }

    if failures.nonEmpty then
      failures
    else
      val bottomUp = formatted.sortBy { case (subsection, _) => -subsection.outputFrom }

      val patched =
        bottomUp.foldLeft(page.lines) {
          case (lines, (subsection, Some(Right(output)))) if subsection.outputFrom >= 0 =>
            lines.take(subsection.outputFrom) ++ output ++ List("") ++ lines.drop(subsection.outputTo)
          case (lines, _) =>
            lines
        }

      Files.write(page.path, (patched.mkString("\n") + "\n").getBytes("UTF-8"))

      Nil

  private def validateSection(settings: SettingsResolver, section: Section): List[String] =
    if section.subsections.isEmpty then
      Nil
    else
      val expectedDefault = settings.default(section.key)

      val defaultProblems =
        (section.default, expectedDefault) match {
          case (Some(actual), Some(expected)) if actual == expected =>
            Nil
          case (actual, expected) =>
            List(
              s"${section.key}: default line is '${actual.getOrElse("missing")}', expected '${expected.getOrElse("?")}'",
            )
        }

      val beforeProblems = if section.before.nonEmpty then Nil else List(s"${section.key}: no Before snippet")

      defaultProblems ++ beforeProblems ++ section.subsections.flatMap(validateSubsection(settings, section, _))

  private def validateSubsection(settings: SettingsResolver, section: Section, subsection: Subsection): List[String] =
    val label           = s"${subsection.key} = ${subsection.value}"
    val fenceProblems   = if subsection.outputFrom < 0 then List(s"$label: no scala code block") else Nil
    val trailerProblems = validateTrailer(settings, label, section.key, subsection)

    val outputProblems =
      section.before match {
        case None =>
          Nil
        case Some(before) =>
          format(settings, label, before, subsection.trailer) match {
            case Left(error) =>
              List(error)
            case Right(expected) if expected != subsection.output =>
              List(
                s"$label: output differs\n--- docs ---\n${subsection.output.mkString("\n")}\n--- formatter ---\n${expected.mkString("\n")}",
              )
            case Right(_) =>
              Nil
          }
      }

    fenceProblems ++ trailerProblems ++ outputProblems

  private def validateTrailer(
    settings: SettingsResolver,
    label: String,
    sectionKey: String,
    subsection: Subsection,
  ): List[String] =
    val keys     = subsection.trailer.map(_._1)
    val required = List("max-column", sectionKey)

    val missingProblems =
      required
        .filterNot(keys.contains)
        .map(key => s"$label: settings block is missing '$key', it holds ${keys.mkString(", ")}")

    val orderProblems =
      keys.headOption.filter(_ != "max-column").map(_ => s"$label: settings block must start with 'max-column'").toList

    val extraProblems =
      keys
        .filterNot(required.contains)
        .flatMap { key =>
          settings.default(key) match {
            case None => List(s"$label: unknown setting '$key' in the settings block")
            case Some(default) if subsection.trailer.toMap.get(key).contains(default) =>
              List(s"$label: settings block lists '$key' at its default value '$default'")
            case Some(_) => Nil
          }
        }

    val valueProblems =
      subsection
        .trailer
        .collect {
          case (key, value) if key == sectionKey && value != subsection.value =>
            s"$label: settings block says $key = $value, heading says ${subsection.value}"
        }

    missingProblems ++ orderProblems ++ extraProblems ++ valueProblems

  private def distinctnessProblems(page: Page): List[String] =
    page
      .sections
      .flatMap { section =>
        section
          .subsections
          .groupBy(_.key)
          .toList
          .flatMap { case (key, subsections) =>
            val (declared, independent) = subsections.partition(_.sameAs.isDefined)

            val declarationProblems =
              declared.flatMap { subsection =>
                subsections.find(_.value == subsection.sameAs.getOrElse("")) match {
                  case None =>
                    List(s"$key = ${subsection.value}: declares the output of '${subsection.sameAs.getOrElse("")}', which is not a value of the setting")
                  case Some(other) if other.output != subsection.output =>
                    List(s"$key = ${subsection.value}: should render as '${other.value}' but does not")
                  case Some(_) => Nil
                }
              }

            val duplicateProblems =
              independent
                .groupBy(_.output)
                .collect {
                  case (_, group) if group.size > 1 =>
                    s"$key: values ${group.map(_.value).sorted.mkString(", ")} render identically"
                }

            declarationProblems ++ duplicateProblems
          }
      }

  private def format(
    settings: SettingsResolver,
    label: String,
    source: String,
    trailer: List[(String, String)],
  ): Either[String, List[String]] =
    for
      config <- configOf(settings, trailer).left.map(error => s"$label: $error")
      output <- Dixtra.format(source, config).left.map(error => s"$label: ${error.getMessage}")
    yield
      output
        .stripSuffix("\n")
        .linesIterator
        .toList

  private def configOf(settings: SettingsResolver, trailer: List[(String, String)]): Either[String, Config] =
    trailer.foldLeft[Either[String, Config]](Right(Config.default)) { case (accumulated, (key, value)) =>
      for
        config  <- accumulated
        updated <- settings.set(config, key, value)
      yield updated
    }

}
