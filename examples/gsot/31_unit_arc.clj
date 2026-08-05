; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.31-unit-arc
  "GSOT p.58 — go.unit.arc: circular arc on the unit interval.

  Maps [0,1) to [0,1) along a quarter-circle arc:

      arc(x) = sqrt(x · (2 - x)) = sqrt(1 - (1 - x)²)

  Geometrically: traces the upper quarter of a unit circle centred at (1,0).
  The curve is convex — it rises quickly then levels off (ease-out character).
  Derivative at x=0 is √2 ≈ 1.41 (steep start); derivative at x→1 is 0 (flat finish).

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n1
        with {
          n1 = sqrt(max(0.0, n0 * (2.0 - n0)));
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! unit-arc
  {}
  (let [x   (audio-in)
        out (faust "sqrt(max(0.0, %{x} * (2.0 - %{x})))" {:x x})]
    (output out)))
