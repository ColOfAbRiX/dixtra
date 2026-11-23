
# Rules and limitations

## Rules that have no setting

Some rules are fixed, on purpose: they are part of what makes the result consistent.

- **Indentation** is 2 spaces.
- **Spaces.** A run of spaces between two tokens on a line becomes one, except where Dixtra aligns code.
- **Blank lines.** A run of blank lines becomes one.
- **Braces.** Only few braces styles are allowed.

When Dixtra has no rule for a specific code snippet, the code is copied exactly as written.

## Known limitations

- Among the Scala 3 syntax it does not parse yet is `into` on a type (Scala 3.9).

## When Dixtra cannot format a file

A file is reported, and left as it is, when:

- it is not valid Scala 3, or it is Scala 2: Dixtra rejects it with the error of the parser;
- it holds a construct that Dixtra does not parse yet;
- the formatted result would not be equivalent to the input, which signals a bug in the formatter: the file is left untouched and the error is reported.
