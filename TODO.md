# TODO

[<PRIORITY>][<TYPE>] <description>

## Must for Release 1.0.0-RC1

* [HIGH][TAKS]If it's only the cli that reads .dixtra.conf, how does the sbt plugin resolve the
  hierarchy of nested .dixtra.conf?

## Next

* [LOW][TASK] Test the project OS properties (atomic move, work on links, permissions, UTF, ...)
  across multiple OSes
* [MED][BUG] Remove formatter exception in comments where it tries to detect a code in the comment.
  The only two rules to not format a part of a comment are 1.Example) if it is indented more than
  the first and 2.Paragraphs) keep multi-blank lines intact. Both of these rules are already present
  but there are more that shouldn't be there.
* [MED][FEAT] Add a command to recursively validate all dixtra configurations in the project

## Scala 2.13 Compat:

This file is an example of values should become part of a `compat213` setting. When `compat213` is set, all
these settings are enforced to the values below, regardless of the configuration in .dixtra.conf.
I think some of these values should enforce braces, though, rather than keep, to make sure compatibility with
Scala 2.13 works

```conf
braces {
  def       = keep
  val       = keep
  var       = keep
  lazy-val  = keep
  if        = keep
  for       = keep
  for-yield = keep

  # A braced single-expression body (`= { x => stats }`) keeps its braces, which Scala 2 needs
  single-expr {
    def         = keep
    val         = keep
    var         = keep
    lazy-val    = keep
    given-alias = keep
    lambda-body = keep
    case-body   = keep
  }
}

# `always-break` puts the statements of `{ a; b }` braceless below the `=`
body.semicolon-statements = keep
```
