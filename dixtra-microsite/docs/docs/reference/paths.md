
# Paths

The files to format inside the directories given to Dixtra.

The patterns are globs relative to the directory being formatted, with `/` between directories: `*` matches within a directory name, `**` across directories, `?` matches one character.

A file is formatted when it matches a `paths.included` pattern and neither it nor a directory above it matches a `paths.excluded` pattern. 

The hidden directories (`.git`, `.metals`, ...) and the `target` directories are always skipped, and a file given explicitly is always formatted.

## `paths.included`

**Default:** `["**"]`.

The files to format.

## `paths.excluded`

**Default:** `[]`.

The files and the directories to skip among the included ones.

Both hold lists of globs:

```conf
paths {
  included = ["src/**"]
  excluded = ["**/generated/**", "legacy/*.scala"]
}
```

With that configuration, only the files below `src` are formatted, and the directories named `generated` and the Scala files directly in `legacy` are skipped.
