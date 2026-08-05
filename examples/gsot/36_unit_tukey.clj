; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.36-unit-tukey
  "GSOT p.58 — go.unit.tukey: cosine-tapered (Tukey) window on the unit interval.

  Maps [0,1) to a flat-top window with cosine-shaped roll-off at each edge:

      x < α/2:          0.5·(1 - cos(2π·x / α))        rise
      α/2 ≤ x < 1-α/2:  1                               hold
      x ≥ 1-α/2:        0.5·(1 - cos(2π·(1-x) / α))    fall

  α=0: rectangular window (no taper, hold=1 everywhere).
  α=1: Hann window (full cosine taper, no flat top = go.unit.lfo).
  α=0.5 (default): half-tapered — equal parts rise, hold, fall.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n2
        with {
          n1 = hslider(\"alpha\", 0.5, 0.0, 1.0, 0.0001);
          n2 = select2(n0 >= (1.0 - n1/2.0), select2(n0 >= (n1/2.0), 1.0, 0.5*(1.0-cos(2.0*ma.PI*n0/max(n1,0.0001)))), 0.5*(1.0-cos(2.0*ma.PI*(1.0-n0)/max(n1,0.0001))));
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! unit-tukey
  {:params {:alpha {:range [0.0 1.0] :default 0.5}}}
  (let [x   (audio-in)
        a   (param :alpha)
        out (faust "select2(%{x} >= (1.0 - %{a}/2.0), select2(%{x} >= (%{a}/2.0), 1.0, 0.5*(1.0-cos(2.0*ma.PI*%{x}/max(%{a},0.0001)))), 0.5*(1.0-cos(2.0*ma.PI*(1.0-%{x})/max(%{a},0.0001))))"
                   {:x x :a a})]
    (output out)))
