; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.32-unit-cubic
  "GSOT p.58 — go.unit.cubic: cubic smoothstep (Hermite S-curve).

  Maps [0,1) to [0,1) via the cubic smoothstep formula:

      cubic(x) = 3x² - 2x³

  Derivative is zero at both x=0 and x=1 — C¹ smooth endpoints, no corner.
  The curve is symmetric about (0.5, 0.5).  Slower at both ends, fastest at centre.
  Identical to the Ken Perlin smoothstep function.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n7
        with {
          n1 = (n0 * n0);
          n2 = (n1 * n0);
          n3 = 3.0;
          n4 = (n3 * n1);
          n5 = 2.0;
          n6 = (n5 * n2);
          n7 = (n4 - n6);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! unit-cubic
  {}
  (let [x   (audio-in)
        x2  (mul x x)
        x3  (mul x2 x)
        out (sub (mul (const 3.0) x2) (mul (const 2.0) x3))]
    (output out)))
