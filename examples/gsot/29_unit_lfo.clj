; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.29-unit-lfo
  "GSOT p.58 — go.unit.lfo: raised cosine (smooth bell shape) from a unit ramp.

  Maps [0,1) to a smooth [0,1] arch via the raised cosine formula:

      lfo(x) = 0.5 * (1 - cos(2π·x))

  At x=0: 0.  At x=0.5: 1.  At x→1: 0.
  Derivative is zero at both endpoints — C¹ smooth, no corner at 0 or 1.
  Identical to a Hann window.  The 'lfo' name reflects its use as a smooth
  LFO shape when applied to a continuous phasor input.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n5
        with {
          n1 = cos(2.0*ma.PI*n0);
          n2 = 1.0;
          n3 = (n2 - n1);
          n4 = 0.5;
          n5 = (n3 * n4);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! unit-lfo
  {}
  (let [x   (audio-in)
        c   (faust "cos(2.0*ma.PI*%{x})" {:x x})
        out (mul (sub (const 1.0) c) (const 0.5))]
    (output out)))
