# Alembic GSOT Examples

Translations of the gen~ patches from:

> **Graham Wakefield and Gregory Taylor**
> *Generating Sound and Organizing Time*
> Cycling '74, 2022
> ISBN 978-0-9833585-3-1
> <https://cycling74.com/books/generating-sound-and-organizing-time>

Everyone working in DSP, computer music, or live coding should own this book.
It is the clearest treatment of sample-rate signal programming in print —
working from first principles through oscillators, envelopes, delays, filters,
physical models, and beyond, with every concept grounded in a runnable gen~
patch and an honest explanation of the mathematics behind it.

## What these files are

Each `.clj` file in this directory translates one GSOT patch into the
[Alembic DSL](../../README.md): a Clojure DSL that compiles to verified Faust DSP
via `emit-faust` + `validate`.  The translation preserves the signal-chain logic
of the original while expressing it in Alembic's `defpatch!` / `faust` vocabulary.

Where GSOT's gen~ representation and the Alembic/Faust representation diverge
(operator naming, feedback syntax, fractional delay API, library functions),
the docstring explains the mapping.  A small number of files (prefixed `ex.150`
and above in some cases) are Alembic extensions — ideas prompted by GSOT but
not literally in the book; these are clearly marked.

## File naming

Files follow the convention `NNN_patch_name.clj` where `NNN` is the sequential
Alembic example number.  These do not correspond to GSOT's own numbering
(the book numbers by chapter section, not globally); the page reference in
each file's docstring is the authoritative GSOT location.

## Coverage

| Range | Topic | GSOT pages |
|-------|-------|------------|
| 01–99 | Chapters 1–6: operators, counters, buffers, oscillators, envelopes | pp.1–193 |
| 135–136 | Drawing a line (lag generator) | pp.195–196 |
| 137–147 | Chapter 7: delay effects — feedforward, feedback, filtering, multi-effect | pp.197–213 |
| 148–150 | Chapter 8: comb filters and enharmonic variants (+ Alembic dispersive extension) | pp.215–218 |
| 151–155 | Chapter 8: Karplus-Strong string synthesis | pp.218–223 |

## Running the examples

```clojure
(require '[alembic.patch :refer [emit-faust validate]])
(require '[examples.gsot.151-string-basic :refer [string-basic]])

;; Inspect the generated Faust DSP
(println (emit-faust string-basic))

;; Verify it compiles through faust -lang cpp
(validate string-basic)  ; => nil on success
```

## License

These translation files are EPL-2.0, the same license as the Alembic project.
The underlying concepts, patch designs, and pedagogical structure are the work
of Graham Wakefield and Gregory Taylor and remain their intellectual property.
Buy the book.
