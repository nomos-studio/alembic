; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.55-random-range
  "GSOT p.93 — random_range.maxpat.

  'Feel the noise' (Chapter 4) — scaling noise to a target range
  ---------------------------------------------------------------
  White noise from `no.noise` is uniformly distributed in [−1, 1].
  `random_range` maps this to an arbitrary [lo, hi] interval using a
  linear affine transform:

      out = lo + (hi − lo) · 0.5 · (noise + 1)

  Derivation:
    (noise + 1) ∈ [0, 2]      — shift to non-negative
    × 0.5       ∈ [0, 1]      — normalise to unit interval
    × (hi−lo)   ∈ [0, hi−lo]  — scale to target width
    + lo        ∈ [lo, hi]    — shift to target base

  When hi > lo: normal upward-mapped range.
  When lo = hi: output is constant (degenerate case, no randomness).
  When lo > hi: range is inverted (output still uniform, just reversed).

  Parameters :lo and :hi both default to [−1, 1] range, giving default
  output identical to raw noise.  Set :lo = 0 and :hi = 1 for unipolar
  random values; :lo = 440 and :hi = 880 for random Hz in one octave, etc.

  Note on placeholder names: %lo and %hi are 3-char slugs; neither is a
  prefix of the other or of %nn, so replacement order is unconstrained.

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp = n2
        with {
          n0 = hslider(\"lo\", -1.0, -1.0, 1.0, 0.0002);
          n1 = hslider(\"hi\",  1.0, -1.0, 1.0, 0.0002);
          n2 = n0+(n1-n0)*0.5*(no.noise+1.0);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! random-range
  {:params {:lo {:range [-1.0 1.0] :default -1.0}
            :hi {:range [-1.0 1.0] :default  1.0}}}
  (let [n   (faust "no.noise" {})
        lo  (param :lo)
        hi  (param :hi)
        out (faust "%{lo}+(%{hi}-%{lo})*0.5*(%{nn}+1.0)" {:lo lo :hi hi :nn n})]
    (output out)))
