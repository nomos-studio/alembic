; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.35-unit-welch
  "GSOT p.58 — go.unit.welch: Welch (parabolic) window on the unit interval.

  Maps [0,1) to a parabolic arch [0,1]:

      welch(x) = 4x(1 - x) = 1 - (2x - 1)²

  At x=0: 0.  At x=0.5: 1.  At x→1: 0.
  A smooth bell shape, faster to compute than go.unit.lfo (no cosine).
  The Welch window is widely used in spectral analysis for its low sidelobes
  and simple closed form.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n5
        with {
          n1 = 4.0;
          n2 = (n1 * n0);
          n3 = 1.0;
          n4 = (n3 - n0);
          n5 = (n2 * n4);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! unit-welch
  {}
  (let [x   (audio-in)
        out (mul (mul (const 4.0) x) (sub (const 1.0) x))]
    (output out)))
