package com.colofabrix.scala.dixtra

import com.colofabrix.scala.dixtra.normalize.*
import com.colofabrix.scala.dixtra.render.TreeRenderer
import com.colofabrix.scala.dixtra.verify.Equivalence
import scala.meta.*
import scala.meta.parsers.Parsed
import scala.util.control.NonFatal

// dixtra: off
//    :::-----------------------------------------------------:::::::::::::::::::::::::::::::.:.....................    
//  /--------------------------------------------------------------:-::::::::::::::::::::::::::.......................  
// -------------------------------------------------------..----------:-::::::::::::::::::::::::::.:................... 
// -------------------------------------------------==-=++%**+*#+*++**+=-:---:::::::::::::::::::::::................... 
// ------------------------=---=--------=-=====+*#+**++-%/..\**+:==#-=+*.\=-:-::-:::::::::::::::::::::................. 
// ------------------------===-==========-=-**==*+==#+=++....-#+*++..*+*#//.+=----:-:::::::::::::::::::..:............. 
// ------------------==================-=%::---+-+*+*+--:+/...+%/./-`%-\/// \=*--------:::::::::::::::::............... 
// ---==------=-=-===================--*#/-+-+==+=-%+-==+:\\../.-:..\..\/ :  \:----------:::::::::::::::::............. 
// -=-============================+==-:-=-=+-*-#-*%-*-:+**+...:-::=-::::.:. . +:--==--------:::::::::::::::::::........ 
// =============================+++%+++++.*:+*#*-*=+---==-:.-*-#-::---:-:.....:-=-===---------::::::::::::::::::....... 
// ====================+=++=++===+:+#*+#+@\:=*=*#*==**==:-++---=-...*+-=..........%+=----------:::::::::::::::::....... 
// =================++++++++==+*+*==-+-=+.##+-:+===+*+*:+==*+==**+%***++*...+##%%%*\=++----------:::::::::::::::.:..... 
// ================+++++++++++++*/.=-=:++--:-:==+++***==*#*******#*******#####%%%%%\.+=+-----------:::::::::::::::..... 
// ===========+==+++++++++++*++=::-=::=..=--:+===*+*++=**=###**#####%%%%%#####%%@@%%%\\=-----------::::::::::::::::.... 
// ===========+++++++++++++*+-+:+-=::-:.:..:.+-==+***+*#+*##**#######%%%%%#%######%%%#\\-=------------::::::::::::::... 
// ====+==+++++++++++*++*=+=--::-=-.-..: ..:-=--++***++*****+**%###**###%##%%%%%%%%%%%%\\==-=-----------::::::::::::::. 
// ===+++++++++++++**+***-=-=::::...... :::-=+-==#%##**+++***######%%######*****#%@@@%#%\\=-=-----------::::::::::::::: 
// =++++++++++++++******+*=--.:::... .:-.:-==--*########*****###########*%@@%######@@@%%%\====-------------:::::::::::: 
// +++++++++++++++*******//.-=-.:- .:/.-:=++=+++%%%#%############*##%%%%@%%%%%@%%%%@@%%@%\\====------------:::::::::::: 
// +++++++++++++********//  `==---=../+===+*+**#%%###########*%####%%%#@%@%%%%%@###%%%%%#%%\====------------::::::::::: 
// +++++++++************``  .-+:.:=+=++--+*=*=#****##*############%%%%%%%%*#....#*#*##***.##*======----------:::::::::: 
// ++++++***************``  .--=.:=---=--##++++=#++******##**####%%%@@#..*#/..-=++***+++...+#*=====-----------::::::::: 
// +++++*+**************\\ . ::---:+-=-=-+-*=*====**+++**#####%%%%%%.+++=+./:---=++**=---:-=*++*+====---------::::::::: 
// ++++**************#**#\\ : -::-+====+*+*-=-+-==-=+++****#######./=+++-=:-::::-=+*+/-::==========-===---------::::::: 
// +++**************#**###\\:.-:---==-=-*=-=:-::====+++***####*#..==+%/...::....:-=*+\.-  ::==========-----------:::::: 
// +****************######\\.......:-.\...=+-:=-====*+****#**++==-=+.-//. .  ..---:=*\..../+==++=-=====----------:::::: 
// +************#*#########\.%%%%%%\\    .. .==*==========+*#%%@`:=.:=\...../=+=:=-+*#..\+=#=+++=======-=---------::::: 
// ************#############*......#.....-:/.=*+.--**#..#%#**--.-==..++....=-=-+ :-=+*%%%\*#%==+========-=---------:::: 
// **********###############`------=+.=*:---#+--#++==#%%###*+...++*****++*==+++---==+*%%@@*#%%++=+======-=---------:::: 
// *********################``-:-=--=.-===--:=-=+++***=**###%%%.***++****+++*+==-==++`@@@@@@%%*+==========----------::: 
// ********##################\\:===:..:--=+-=:--==+*****==+\%%%%#%=**++++..#**%=+++=`%%%%@%%%+===========-----------::: 
// ******##*##################\=*\\:.::`/*++----+-===+++*+==.....#%%#+%#*@@*=++-=+*+#%%...%###+=============--------::: 
// *****#######################**%\....-#**=--=--==++++++++=--+-==++*%%%#*###+:=+**......+++*+============---------:::: 
// *****#######################%+=*%@.\-++-===:---===+++=+==+====+=++**%##**+=--=./...:\=+++#+++===========--------:::: 
// *****###################@@@#+*#=*..##%*-----=:+=--+===++=++==---=+*##***+=+++-:../...:=#%*#+==+=========---------::: 
// ****###################@@%=+\-++=+=====-=--:-=-===++=++==+++==+=+*#****+=++*+*+..=+---=*#**+++++========--------:::: 
// ***####################@%@..\\*#+-======---:-:-=+++=+++++++=*==++=++++===#*+*=+#+==+-+++#*+++===========---------::: 
// ***##################%#@@@@@@@#*#.+====-..---====++*==-*=*++*+==+++++-=++*+++-:+---=+++=+#+++===========--------:-:: 
// ***#################*++\\@@@@%%####*+-:---:-:+-===+++===++===-++====-+=:..........--=-.-`%**#+=========---------::-: 
// ***#######%####%###*+*==\\@@@@@=#+#%/--::-::-+==:+=+=+=+++=====++=-=-..:.:         .-+.=++##+*#+=======------------: 
// ***###**#**+***++++*+**+*+\.@@*@*#+//-:---:--:-=-+======++=++==+=+=-:. .:::.........++++=+#+#-##======-------------: 
// ***####***+*+**+***#*+**+*=\\@@@@+%`-:::----=:::--+--===========+==-.:-=---====++===*+**++=*#-`#*=====-----------::: 
// **#**+++**********+*#***+*+*+\\@@@\\---:-:-:---.::-:---=-=-====----::=:-=------==+-++***#%#+=:`#*======------------: 
// *#+++*+*+*+*****+***#++***++=#=\@@@\\.:-:::-----::::.:-=----==-:----::-------=-=+++++*+*###+-+#*+.\.==-------------: 
// ++=+==*+****#*+********+*+****+\\%#@..\.\--:-----:- :  .:----------=:-::+:===--=++=+++**+**+=-=+\`@\\\------------:: 
// -===++=**+***+##*++***+++++++*+=*+#\@@..\.\-----=--:....:::----::-::=-::=:-====-=-===++==**+-=+=\`@@\\\-----------:- 
// ++==+++===++=-++**=++##+*++===+*==*+#@.@@\\\\--:--+=-::-::::--:.=:::-:-:::---=-====--=--====-:::``@@@@\\----------:- 
// -===+++*==+*-=+=#+++-==++**++++-*+=+@#=\@*@\\`::-------=-:--:::::::--::::-=--==-:::.:=====-::+-=``@@@@@\\----------- 
// :.....--=*++=*===:+=++=:-++*+=+=+*++*%+#*@@//=...-=--=--=-===--:--:.:=:=.-::--:::.:::=:::-=:::--``@#@@@@\\-------:-- 
// @.....\\..--=+:+#==:+++=:-=+===-=+=*/@\:%../:-/.\\::---======-=====---=:-=-=.:.:    `=  ---.....``@@@%@%@\\--------- 
// @#@@#@@...\\.-=-==+:=:+==::-===--=*=`@@\\-++#:+##\\:::------===----=--=-==::.       .-- :-:..:::``@@@@@@#@\\-------- 
// @@@@@@@@@@@..\\--=+-=:+:+=::--++--:-\\@@\..=+#+.*#\\:::------=---:---+=-:...     ....:- .:.:-:::``@@@%%@@%@\\------- 
// @@@@@@@@@@@@@\\..-*-+-*:+==::--=-:=:`\\./...//.\=.+\\::-:::-:-::.:-==+/.: .:     ..  =: :=......``@%@@%%%%%\\------- 
// @@@@@@@@@@@@@@@..\\-+:+-=--=.:-+--:-==../....  --==+*\:::::---..::=:-/. : ...     . ..:..- :--::``%%@@#%@%#@\\------ 
// @@@@@@@...@@@@%@@@\\-:+-**-=- -:=--:-=-/. :  *-: -++++\.::---:.---=+/   . .:.     ....:: -:::..//##%@@%*@#%%@\\----- 
// @@@@@@%+==\@@@@@#@@\\\--=+-=-+.:-=-::::=..... .=-. =:+=-:----:-====// ... : .    .....::.:  .://#*%%%#%%@%##@@\\---- 
// @@@@@@@...=#%@@@@@@@@\\===-+-:: -:=:::.:-:::.:. =*/\\:-=+--.::-=--//   .  .::    .....::.:. ../`###*%%%######@\\---- 
// @@@*%@@@@@.@**#.@@@%@@\\\=-+-:- :===:::---:-:.=- \\*#.+-  -.  -++//     : ..:  .  .:...:-...://*##%%#%*%@+###%%\\--- 
// @+@@@@@@@@@%@@++*%@@@@%@\\-=--:-::--:..:------:=-::\\**\\.-\.--+//     . ..::     ....::-.: //#*#####%*%%#*#*%@@\\-- 
// @@%@@@@@@@@@@@\.###@@%@#@\\:==-- ::--:::--=-:----=::\**+=+++=:+//   :  ... ::   ....::::::.//**+###*%#%*@%#+#*@@\\-- 
// @@@@@@@@@@@=%@@@@@%%%%*@#@\\--:--:=--:...:-:..-+.==::\+=**=*=++``    :. ....-    ...::-:-- /**#**#*#%+###@##*##%@\\` 
// %%@@@*@@@@@@%@@@@@@*##%%@@\\\:--: --:::::::=-.::-.-::.--+*#%=-*\`   :  ::.:.:. .:...:::::-=+*+***#*##*##@@#***%%@@\\ 
// @@%@@@@%%@@@@@@@@@@%@@%%%%@\\:-:: ----:.::::-:.:-=.-=-::\\*#+**`` .   :.....:  .:.::..::.:=:+++++##+*####%***+\@%@@` 
// %%%@@@@@@*@@@@@@%@@@@@@%%%#@\\:-- `--::..:::.- .---:---::\..=##``  :: -:..::-   ..:...::::=+==+*****#*#%%%****+`@%%` 
// %@@@@#@@@@@@@@@@#@@@@@%%%%%@%\\-: .--::...::::: .::--::-::::\\#``..- . :- .--    ..::::..:-=-=-**=*+*####%*+**#+@@@` 
// =@@@@@+@@@@@@*@@@@@@@@%%%%%%%#\\`  :::::. .::--  :-::-.---:=:\./`::.-..::-.-=    ..:::::.:-:-=+++=****####+*++#%#@%` 
// #@@=%##%@@@@@@@%@@@%%%@%%%%#%#@\\\.::::....::.-:  .:.:-.:----=:-=::+:=-:::-=-. .   ....::.--=-==+++*+**###=**+#%%@@` 
// ##%%%##*%@%@%%%%@@@@#@%*#%%%%%%%\\:.::.... ...::   ::.=--:=:-::::-+-+:::::::-....:  ..:.::::-:=-++=*++**#*=+*+#*%#@` 
// %%%%:%%%###%%@@@@#@/*@@*%%%###%%`\.:::..... ::::   ..:-::-.::-::-::==:--::::.....:.....::::::-=++=*=**++*=++++#%%%%` 
// #+@@@@@@%###+#%@@@@#+*###%%%*####`\.::....   ::--    :::.-:-..:::::.::::-::-............::::-==+++++-**+++-++*##*##` 
// %%@@+@@@@@@%##%=@@@@@@@%%#%%+##*##\:::...   ..-::     ..:..:--...::::::::::-..:........:.:::--=++++*+=*+-+++=**###*` 
// #%%%@%@@@@@`:%####%=@#@**#*####**#``::..     ..:-    ..::....:::..::.::::::-...... . :.::..:--==+++++*=++==+=#+*###` 
// %%#%%%@@@@@@@@%%##%%%@%@%%+#*#*#**=\\:.       .:-:    ...::..:-::::::.::::::............::. :-====+****-++++*####**` 
// %%#@@@@@@@@#%@@@=%#%%%%#@@#*+#+****#\`..       .::      ......::::.:.:::::::.............. .::--===++=++===**+**+##` 
//  \=%@@@@@#@@@@@@@@@@@@%%%%@%####*++=*`...      .::.     ......::::::::..:::.............:.. ..-=-=+=++-=++++**+**+./ 
//   %%%@@@@@@@@@@@@@@@+@%%###%%#*#+****-.       ...::      ......:::..:..:.::......   ......:-----=====+=-++**++++++   
// dixtra: on

/**
 * Public entry point for the Dixtra Scala 3 formatter.
 *
 * The formatter is a pure function from source text to formatted source text. It first collapses redundant whitespace,
 * parses the result with Scalameta and renders the tree according to the style rules of Dixtra. Any construct not yet
 * supported falls back to the safety net, which reproduces the source verbatim, so the tool never corrupts code.
 *
 * As a final guarantee the output is re-parsed and compared with the original input: if the program structure or the
 * comments differ, formatting fails with an [[UnsafeOutputError]] instead of returning corrupted code.
 */
object Dixtra {

  /**
   * Format the given source text using the default configuration.
   */
  def format(source: String): Either[FormatError, String] =
    format(source, Config.default)

  /**
   * Format the given source text with the given configuration.
   *
   * @param source The source text to format.
   * @param config The formatting configuration.
   * @return The formatted source on success, or a FormatError on parse failure or when the output would not be
   *         equivalent to the input.
   */
  def format(source: String, config: Config): Either[FormatError, String] =
    format(source, config, SourceDialect.scala)

  /**
   * Format the given source text, parsed as a whole with `dialect`.
   *
   * @param source The source text to format.
   * @param config The formatting configuration; with `compat213`, the settings that keep the output valid Scala 2.13
   *               too.
   * @param dialect The dialect of the whole file (see [[SourceDialect]]).
   * @return The formatted source on success, or a FormatError on parse failure or when the output would not be
   *         equivalent to the input.
   */
  def format(source: String, config: Config, dialect: Dialect): Either[FormatError, String] =
    val effective = config.effective
    val lf        = Housekeeping.withLf(source)
    val eol       = Housekeeping.separator(effective.housekeeping.lineEnding, source)

    // The blank lines between imports, and with `blank-lines.before-scaladoc = never` before scaladocs, are removed
    // before rendering, so the layout sees the result
    val spaced    = ImportSpacingNormalizer.normalize(WhitespaceNormalizer.normalize(lf))
    val cleaned   = DocCommentSpacingNormalizer.glued(spaced, effective.blankLines.beforeScaladoc)
    val unchanged = cleaned == lf

    for
      tree      <- parseOriginal(lf, dialect)
      cleanTree <- if unchanged then Right(tree) else parse(cleaned, dialect)
      formatted <- render(cleaned, cleanTree, effective, dialect)
      restored  <- FormatterOff
                     .restore(lf, Housekeeping(formatted, effective.housekeeping), dialect)
                     .left
                     .map(UnsafeOutputError(_))
      verified  <- verify(lf, tree, restored, dialect)
    yield
      if eol == "\n" then
        verified
      else
        verified.replace("\n", eol)

  // Parse the source as written: a Scala 2 source is reported as such, so that it is skipped. It either parses only as
  // Scala 2, or it parses as Scala 3 too but uses a construct that Scala 3 dropped (see [[Scala2Syntax]]).
  private def parseOriginal(source: String, dialect: Dialect): Either[FormatError, Source] =
    parse(source, dialect) match {
      case Right(tree) =>
        Scala2Syntax
          .dropped(tree, source)
          .map(construct => Scala2Error(s"the file is Scala 2: it uses $construct, which Scala 3 dropped"))
          .toLeft(tree)
      case Left(error) =>
        if parse(source, SourceDialect.scala2(dialect)).isRight then
          Left(Scala2Error("the file is Scala 2: it parses as Scala 2 but not as Scala 3"))
        else
          Left(error)
    }

  private def parse(source: String, dialect: Dialect): Either[FormatError, Source] =
    given Dialect = dialect
    val input = Input.String(source)
    try {
      input.parse[Source] match {
        case Parsed.Success(tree) =>
          Right(tree)
        case Parsed.Error(pos, msg, _) =>
          Left(ParseError(s"line ${pos.startLine + 1}, column ${pos.startColumn + 1}: $msg"))
        case _ =>
          Left(ParseError("Unknown parse failure"))
      }
    } catch {
      case NonFatal(e) => Left(ParseError(FormatError.describe(e)))
    }

  // Render the tree (splicing formatted node slices back into the original source, with unsupported nodes falling back
  // to the safety net) and then apply the whole-file text-normalisation passes. When the package clause changes between
  // braces and braceless, its members moved by two columns after their layout, so the result is rendered once more to
  // lay them out where they are.
  private def render(source: String, tree: Source, config: Config, dialect: Dialect): Either[FormatError, String] =
    try {
      val renderer = new TreeRenderer(source, config)
      val rendered = renderer.renderSource(tree)

      // A rendering that left the source as it was (a formatted file) has the tree already parsed
      val once = Normalize(rendered, config, dialect, Option.when(rendered == source)(tree))

      if !renderer.restylesPackage(tree) then
        Right(once)
      else
        val relaid =
          parse(once, dialect).map { reparsed =>
            val renderedAgain = new TreeRenderer(once, config).renderSource(reparsed)
            Normalize(renderedAgain, config, dialect)
          }

        Right(relaid.getOrElse(once))
    } catch {
      case NonFatal(e) =>
        Left(UnsafeOutputError(s"Internal formatter error: ${FormatError.describe(e)}"))
    }

  private def verify(source: String, tree: Source, formatted: String, dialect: Dialect): Either[FormatError, String] =
    if Equivalence.isEquivalentSource(tree, source, formatted, dialect) then
      Right(formatted)
    else
      Left(UnsafeOutputError("Formatted output is not equivalent to the input; file left unchanged"))

}
