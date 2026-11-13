---
layout: docs
title: Glossary
---

# Glossary

**Alignment group.** A group of adjacent one-line items that Dixtra aligns together, for example the `=` of a run of `val` definitions. A blank line, an item over several lines, or any other statement ends the group.

**Body edge.** The blank line at the beginning or at the end of the body of a construct, after the opening `{` and before the closing `}`.

**Call site.** The place where the arguments are passed: a call, a constructor call, a tuple.

**Definition site.** The place where the names are declared: the parameters of a `def` or of a class, and the selectors of an `import` or of an `export`.

**Visual feature.** A part of the code that Dixtra formats as a unit, like the braces of a `def`, or the alignment of a group of definitions. The settings change visual features, not individual lines.

**Selector chain.** A chain of selections, like `a.b(x).c.d`. When it breaks, the receiver stays on the first line and each selection goes below it.

**Separator.** The token that introduces a body: the `=` of a definition, the `=>` of a lambda or of a case, the `then` and the `else` of an if/else, the `do` of a loop.

**Group of bodies.** The bodies of a construct that has several of them, like the branches of an if/else or the cases of a match. They are treated as a group, never one by one.
