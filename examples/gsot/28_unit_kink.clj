; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.28-unit-kink
  "GSOT p.58 — go.unit.kink: piecewise linear ramp with a slope change.

  Maps [0,1) to [0,1) monotonically, with a slope change at x=kink.
  The kink point always maps to y=0.5; the two segments have different slopes.

      x < kink:   (x / kink) * 0.5
      x ≥ kink:   0.5 + ((x - kink) / (1 - kink)) * 0.5

  kink=0.5 (default): both slopes equal 1.0 → identity (straight line).
  kink=0.25: steep first half (slope 2), gentle second half (slope 2/3).
  kink=0.75: gentle first half (slope 2/3), steep second half (slope 2).

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n2
        with {
          n1 = hslider(\"kink\", 0.5, 0.01, 0.99, 0.0001);
          n2 = select2(n0 >= n1, n0 / max(n1, 0.0001) * 0.5, 0.5 + (n0 - n1) / max(1.0 - n1, 0.0001) * 0.5);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! unit-kink
  {:params {:kink {:range [0.01 0.99] :default 0.5}}}
  (let [x   (audio-in)
        k   (param :kink)
        out (faust "select2(%x >= %k, %x / max(%k, 0.0001) * 0.5, 0.5 + (%x - %k) / max(1.0 - %k, 0.0001) * 0.5)"
                   {:x x :k k})]
    (output out)))
