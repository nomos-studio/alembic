; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.26-unit-triangle
  "GSOT p.58 — go.unit.triangle: symmetric triangle from a unit ramp.

  Maps [0,1) to a triangle wave [0,1]: rises linearly 0→1 over the first
  half-cycle, falls linearly 1→0 over the second.

      triangle(x) = 1 - |2x - 1|

  At x=0: 0.  At x=0.5: 1.  At x→1: 0.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n7
        with {
          n1 = 1.0;
          n2 = 2.0;
          n3 = (n0 * n2);
          n4 = 1.0;
          n5 = (n3 - n4);
          n6 = abs(n5);
          n7 = (n1 - n6);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! unit-triangle
  {}
  (let [x   (audio-in)
        out (sub (const 1.0) (abs (sub (mul x (const 2.0)) (const 1.0))))]
    (output out)))
