; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.33-unit-logistic
  "GSOT p.58 — go.unit.logistic: logistic sigmoid on the unit interval.

  Maps [0,1) to approximately [0,1) via the logistic function:

      logistic(x, k) = 1 / (1 + exp(-k · (x - 0.5)))

  k controls the steepness:
  k=0:  linear (flat slope = 0.25 at centre)
  k=5:  gentle S-curve
  k=10: steep S-curve (default)
  k=20: near-threshold / near-step function

  The output is not exactly 0 at x=0 or exactly 1 at x=1; for k=10 the
  error is < 0.01.  For precise normalisation the caller can chain go.unit.kink
  or rescale, but for most modulation uses the approximation is sufficient.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n2
        with {
          n1 = hslider(\"steepness\", 10.0, 0.0, 20.0, 0.002);
          n2 = 1.0 / (1.0 + exp(-n1 * (n0 - 0.5)));
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! unit-logistic
  {:params {:steepness {:range [0.0 20.0] :default 10.0}}}
  (let [x   (audio-in)
        k   (param :steepness)
        out (faust "1.0 / (1.0 + exp(-%k * (%x - 0.5)))" {:x x :k k})]
    (output out)))
