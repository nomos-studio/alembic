; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.43-portamento-shaped
  "GSOT pp.76-77 — smooth-stepped-noise / portamento with ease-exp shaping.

  Identical topology to portamento (example 42) with one addition: the linear
  clipped ramp is shaped by go.unit.ease.exp before being used as the blend
  factor.  The result is an ease-in portamento — the glide accelerates at the
  start and decelerates at the end.

  go.unit.ease.exp shaping (blend form)
  --------------------------------------
  Like go.unit.arc in the interpolating-LFO (example 41), the GSOT gen~
  abstraction takes a SHAPE FACTOR (sf) rather than using the full exponential
  directly.  sf blends between linear (sf=0) and full ease-exp (sf=1):

      ease_exp(x) = (exp(k·x) - 1) / (exp(k) - 1)    k = 3.0 (fixed)
      shaped(x, sf) = x + sf · (ease_exp(x) - x)

  The smooth-stepped-noise maxpat hardcodes sf=0.5 (balanced blend).
  This example exposes :shape as a parameter for exploration.

  Signal flow (additions over example 42):

      clipped ──→ shaped_ramp ──→ mix → out
                    ↑
                  0.5*(linear + ease_exp)

  All other nodes (trig, slope, accum, clip, to, from) are identical.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = hslider(\"glide-ms\", 300.0, 1.0, 5000.0, 0.4999);
      n1 = hslider(\"shape\", 0.5, 0.0, 1.0, 0.0001);

      alembic_dsp(n2) = n10, n6
        with {
          n3  = float(abs(n2-n2')>0.5);
          n4  = 1000.0/(ma.SR*n0);
          n5  = (select2(n3>0.5,_+n4,n4))~_;
          n6  = max(0.0,min(1.0,n5));
          n7  = n6+n1*((exp(3.0*n6)-1.0)/(exp(3.0)-1.0)-n6);
          n8  = (select2(n3>0.5,_,n2)~_);
          n9  = (select2(n3>0.5,_,n8')~_);
          n10 = n9+n7*(n8-n9);
        };
      process = alembic_dsp;

  n3  = trig    (change detection)
  n4  = slope   (per-sample increment)
  n5  = ramp    (accumulator)
  n6  = clipped (ramp clamped [0,1])
  n7  = shaped  (ease-exp blend: linear + sf·(ease_exp − linear))
  n8  = to      (S&H of input on trigger)
  n9  = from    (S&H of to' on trigger)
  n10 = out     (from + shaped × (to − from))"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! portamento-shaped
  {:params {:glide-ms {:range [1.0 5000.0] :default 300.0 :unit :ms}
            :shape    {:range [0.0 1.0]    :default 0.5}}}
  (let [x       (audio-in)
        trig    (faust "float(abs(%{inp}-%{inp}')>0.5)" {:inp x})
        slope   (faust "1000.0/(ma.SR*%{gms})" {:gms (param :glide-ms)})
        ramp    (faust "(select2(%{trig}>0.5,_+%{slope},%{slope}))~_"
                       {:trig trig :slope slope})
        clipped (faust "max(0.0,min(1.0,%{r}))" {:r ramp})
        ; ease-exp blend: x + sf*((exp(3x)-1)/(exp(3)-1) - x)
        ; sf=0 → linear; sf=1 → full exponential ease-in; sf=0.5 matches maxpat
        shaped  (faust "%{r}+%{sf}*((exp(3.0*%{r})-1.0)/(exp(3.0)-1.0)-%{r})"
                       {:r clipped :sf (param :shape)})
        to      (track-hold x trig)
        from    (faust "(select2(%{trig}>0.5,_,%{tgt}')~_)" {:trig trig :tgt to})
        out     (faust "%{from}+%{s}*(%{tgt}-%{from})" {:from from :s shaped :tgt to})]
    (output out)
    (output :ramp clipped)))
