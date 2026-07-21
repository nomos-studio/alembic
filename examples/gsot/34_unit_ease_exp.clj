; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.34-unit-ease-exp
  "GSOT p.58 — go.unit.ease.exp: exponential ease curve on the unit interval.

  Maps [0,1) to [0,1) exactly via:

      ease_exp(x, k) = (exp(k·x) - 1) / (exp(k) - 1)

  k > 0: ease-in — slow start, fast finish (convex upward).
  k < 0: ease-out — fast start, slow finish (concave).
  k→0:   approaches linear (L'Hôpital limit = x).

  The formula is normalised: ease_exp(0) = 0 and ease_exp(1) = 1 exactly
  for all k ≠ 0.  Default k=3.0 gives a moderate ease-in curve.

  Param range [0.1, 10.0] avoids the k=0 singularity.  For ease-out, negate
  the input ramp before feeding: `(unit-ease-exp (sub (const 1.0) x))` and
  flip the output, or supply a negative k via a modified param range.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n2
        with {
          n1 = hslider(\"curvature\", 3.0, 0.1, 10.0, 0.00099);
          n2 = (exp(n1 * n0) - 1.0) / (exp(n1) - 1.0);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! unit-ease-exp
  {:params {:curvature {:range [0.1 10.0] :default 3.0}}}
  (let [x   (audio-in)
        k   (param :curvature)
        out (faust "(exp(%k * %x) - 1.0) / (exp(%k) - 1.0)" {:x x :k k})]
    (output out)))
