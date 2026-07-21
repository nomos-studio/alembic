; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.27-unit-trapezoid
  "GSOT p.58 — go.unit.trapezoid: piecewise linear rise-hold-fall envelope.

  Maps [0,1) to a trapezoid shape: linear rise, hold at 1, linear fall, hold at 0.

      x < rise:        x / rise
      rise ≤ x < fall-start:  1
      fall-start ≤ x < fall:  (fall - x) / (fall - fall-start)
      x ≥ fall:        0

  Params define the four breakpoints.  Default: rise=0.2, fall-start=0.5, fall=0.8.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n4
        with {
          n1 = hslider(\"rise\", 0.2, 0.0, 1.0, 0.0001);
          n2 = hslider(\"fall-start\", 0.5, 0.0, 1.0, 0.0001);
          n3 = hslider(\"fall\", 0.8, 0.0, 1.0, 0.0001);
          n4 = select2(n0 >= n3, select2(n0 >= n2, select2(n0 >= n1, 1.0, n0 / n1), (n3 - n0) / (n3 - n2)), 0.0);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! unit-trapezoid
  {:params {:rise       {:range [0.0 1.0] :default 0.2}
            :fall-start {:range [0.0 1.0] :default 0.5}
            :fall       {:range [0.0 1.0] :default 0.8}}}
  (let [x   (audio-in)
        r   (param :rise)
        s   (param :fall-start)
        f   (param :fall)
        out (faust "select2(%x >= %f, select2(%x >= %s, select2(%x >= %r, 1.0, %x / %r), (%f - %x) / (%f - %s)), 0.0)"
                   {:x x :r r :s s :f f})]
    (output out)))
