package com.colofabrix.scala.dixtra.format

import com.colofabrix.scala.dixtra.*
import java.nio.charset.StandardCharsets
import java.nio.file.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * sbt build definitions: Scala 3 with settings as top-level statements, formatted like the statements of a block.
 */
class SbtFileSpec extends AnyWordSpec with Matchers {

  private def format(source: String): Either[String, String] =
    Dixtra
      .format(source, Config.default, SourceDialect.sbt)
      .left
      .map(_.toString)

  private def read(path: String*): String =
    new String(Files.readAllBytes(Paths.get(path.head, path.tail*)), StandardCharsets.UTF_8)

  "a build definition" should {
    "keep fitting top-level settings as written" in {
      val input = "ThisBuild / scalaVersion := \"3.8.4\"\nThisBuild / organization := \"com.example\"\n"
      format(input) shouldBe Right(input)
    }

    "format the definitions among its settings" in {
      val input    = "name := \"demo\"\n\nval x =\n  list.filter(_ > 0).map(_ * 2).headOption\n"
      val expected = "name := \"demo\"\n\nval x =\n  list\n    .filter(_ > 0)\n    .map(_ * 2)\n    .headOption\n"
      format(input) shouldBe Right(expected)
    }

    "align the operators of adjacent settings" in {
      val input    = "name := \"demo\"\nscalaVersion := \"3.8.4\"\nlibraryDependencies += \"org\" %% \"lib\" % \"1.0\"\n"
      val expected =
        "name                := \"demo\"\nscalaVersion        := \"3.8.4\"\n" +
        "libraryDependencies += \"org\" %% \"lib\" % \"1.0\"\n"
      format(input) shouldBe Right(expected)
    }

    "align the operators of the argument of adjacent calls" in {
      val input =
        "addSbtPlugin(\"ch.epfl.scala\" % \"sbt-scalafix\" % \"0.14.7\")\n" +
        "addSbtPlugin(\"org.typelevel\" % \"sbt-tpolecat\" % \"0.5.7\")\n" +
        "addSbtPlugin(\"pl.project13.scala\" % \"sbt-jmh\" % \"0.4.8\")\n"
      val expected =
        "addSbtPlugin(\"ch.epfl.scala\"      % \"sbt-scalafix\" % \"0.14.7\")\n" +
        "addSbtPlugin(\"org.typelevel\"      % \"sbt-tpolecat\" % \"0.5.7\")\n" +
        "addSbtPlugin(\"pl.project13.scala\" % \"sbt-jmh\"      % \"0.4.8\")\n"
      format(input) shouldBe Right(expected)
    }

    "not parse as a Scala source" in {
      Dixtra.format("name := \"demo\"\n", Config.default).isLeft shouldBe true
    }
  }

  "the build definitions of this project" should {
    // From `dixtra-cli`, where the tests of the command line run
    List(List("..", "build.sbt"), List("..", "project", "plugins.sbt")).foreach { path =>
      s"format ${path.drop(1).mkString("/")} and be stable" in {
          val once = format(read(path*))
          (once.isRight, once.flatMap(format)) shouldBe (true, once)
        }
    }
  }

}
