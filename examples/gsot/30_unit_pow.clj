; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.30-unit-pow
  "GSOT p.58 — go.unit.pow: power-law curve on the unit interval.

  Maps [0,1) to [0,1) via x^p:

      pow(x, p) = x^p

  p=1: identity (linear).
  p>1: concave — slow start, fast finish (ease-in).
  p<1: convex — fast start, slow finish (ease-out).
  p=2: quadratic ease-in.  p=0.5: square-root ease-out.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n2
        with {
          n1 = hslider(\"power\", 2.0, 0.1, 8.0, 0.00079);
          n2 = pow(max(0.0, n0), n1);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! unit-pow
  {:params {:power {:range [0.1 8.0] :default 2.0}}}
  (let [x   (audio-in)
        p   (param :power)
        out (faust "pow(max(0.0, %x), %p)" {:x x :p p})]
    (output out)))
