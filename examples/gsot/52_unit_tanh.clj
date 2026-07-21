; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.52-unit-tanh
  "GSOT p.87 — go.unit.tanh.gendsp.

  'Normalized sigmoids as unit shapers' (Chapter 3)
  --------------------------------------------------
  The sigmoid functions in examples 49-51 map ℝ → (−1,1) and operate on
  bipolar signals.  To use them as UNIT SHAPERS — dropping into the go.unit.*
  vocabulary that takes x ∈ [0,1] and returns y ∈ [0,1] — requires two steps:

    1. Bipolarize:  map [0,1] → [−1,1]  via  x' = 2x − 1
    2. Normalize:   map output back to [0,1] using the sigmoid's own value at
                    the boundary to establish the [min,max] range.

  For tanh, the normalization exploits odd symmetry (tanh(−k) = −tanh(k)):

      u   = k · (2x − 1)              bipolarized, driven input
      out = (tanh(u) + tanh(k)) / (2 · tanh(k))

  Verification:
    x=0 → u=−k: (tanh(−k)+tanh(k))/(2·tanh(k)) = 0/(2·tanh(k)) = 0  ✓
    x=1 → u=+k: (tanh(k)+tanh(k))/(2·tanh(k))  = 1                   ✓
    x=½ → u=0:  (0+tanh(k))/(2·tanh(k))          = ½                  ✓

  Limiting behaviour
  ------------------
  k → 0 (identity limit): tanh(u) ≈ u = k(2x−1), tanh(k) ≈ k
      → (k(2x−1)+k)/(2k) = 2kx/(2k) = x   (linear, no shaping)

  k → ∞ (step limit): tanh(±k) → ±1
      → x>½: (1+1)/2 = 1;  x<½: (−1+1)/2 = 0   (hard step at x=½)

  Drive parameter controls the S-curve tightness between these limits.
  Default 2.0 gives a musically useful gentle S that adds soft saturation
  when applied to ramps, LFO shapes, or envelopes.

  The '.gendsp' suffix
  --------------------
  In GSOT naming, '.gendsp' marks abstractions implemented at the gen~ DSP
  level (direct signal arithmetic) rather than as higher-level Max patchers.
  The normalized-tanh abstraction is conceptually simple but requires careful
  handling of the exp-form division, hence the explicit gen~ primitive form.

  Implementation note
  -------------------
  Both tanh(u) and tanh(k) use the identity 1−2/(1+exp(2v)) since Faust
  exposes no tanh primitive.  tanh(k) (the normalization denominator) is
  computed from the param node directly — it is a block-rate scalar in
  practice (changes only when :drive changes) but appears as a sample-rate
  node in Faust; the compiler treats it correctly.

  Denominator guard: max(tanhk, 0.0001) prevents division-by-zero if drive
  is ever 0; not reachable in the declared range [0.5, 8.0] but defensive.

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      n0 = hslider(\"drive\", 2.0, 0.5, 8.0, 0.00075);

      alembic_dsp(n1) = n7
        with {
          n2 = 2.0*n1-1.0;
          n3 = n0*n2;
          n4 = exp(2.0*n3);
          n5 = 1.0-2.0/(1.0+n4);
          n6 = 1.0-2.0/(1.0+exp(2.0*n0));
          n7 = (n5+n6)/(2.0*max(n6,0.0001));
        };
      process = alembic_dsp;

  n0 = drive param (k)
  n1 = x (unit audio-in)
  n2 = 2x−1       (bipolarize)
  n3 = u = k(2x−1) (driven bipolar)
  n4 = exp(2u)
  n5 = tanh(u) = 1−2/(1+exp(2u))
  n6 = tanh(k) = 1−2/(1+exp(2k))  (normalization scalar)
  n7 = (tanh(u)+tanh(k)) / (2·tanh(k))"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! unit-tanh
  {:params {:drive {:range [0.5 8.0] :default 2.0}}}
  (let [x     (audio-in)
        k     (param :drive)
        bx    (faust "2.0*%xx-1.0"                    {:xx x})
        u     (faust "%kk*%bx"                         {:kk k :bx bx})
        e2u   (faust "exp(2.0*%uu)"                    {:uu u})
        tanhu (faust "1.0-2.0/(1.0+%eu)"              {:eu e2u})
        ; tanh(k): normalization denominator — uses k directly via exp form
        tanhk (faust "1.0-2.0/(1.0+exp(2.0*%kk))"    {:kk k})
        out   (faust "(%tu+%tk)/(2.0*max(%tk,0.0001))" {:tu tanhu :tk tanhk})]
    (output out)))
