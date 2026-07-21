; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.40-smooth-stepped-shaped
  "GSOT p.70 — shaped-stepped-noise: smooth-stepped with raised-cosine blend.

  Identical topology to smooth-stepped (example 39) except the ramp is
  passed through go.unit.sine (raised cosine) before being used as the
  mix factor.  The linear blend becomes an ease-in/ease-out S-curve:

      linear:         out = from + ramp * (to - from)
      shaped (this):  out = from + 0.5*(1-cos(2π·ramp)) * (to - from)

  go.unit.sine maps [0,1) to [0,1) with:
    - slow start  (ramp near 0: cosine slope ≈ 0)
    - fast middle (ramp near 0.5: cosine slope = max)
    - slow finish (ramp near 1: cosine slope ≈ 0)

  The shaped blend softens the transitions: at the start and end of each
  cycle the output moves slowly, accelerating through the middle.  The
  result sounds smoother than linear stepping — no sudden velocity
  changes at cycle boundaries.

  Signal flow:

      phasor(Hz) ──→ ramp ──→ go.unit.sine ──→ shaped_ramp
                       │                            │
                       └──→ go.ramp2trig → S&H  ──→ mix → out
                                          S&H ───────┘

  The S&H pair (to / from) is identical to example 39:
    to   = S&H of noise on trigger          (new target each cycle)
    from = S&H of to' on trigger            (previous target as start)
    out  = from + shaped_ramp * (to - from) (ease-in/ease-out blend)

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = hslider(\"hz\", 2.0, 0.01, 20.0, 0.001999);
      n1 = os.phasor(1.0, n0);
      n2 = (n1 - n1');
      n3 = abs(n2);
      n4 = 0.5;
      n5 = (float(n3 > n4));
      n7 = abs(no.noise);
      n8 = (select2(n5 > 0.5, _, n7) ~ _);
      n9 = (select2(n5>0.5,_,n8')~_);
      n10 = 0.5*(1.0-cos(2.0*ma.PI*n1));
      n11 = n9+n10*(n8-n9);

      process = n11, n1;

  n8 = to     (S&H noise on trigger)
  n9 = from   (S&H n8' = prev target on trigger)
  n10 = shaped (raised cosine of ramp)
  n11 = out    (from + shaped*(to-from))"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! smooth-stepped-shaped
  {:params {:hz {:range [0.01 20.0] :default 2.0}}}
  (let [ramp   (phasor (param :hz))
        cmp    (comparator (abs (delta ramp)) (const 0.5))
        trig   (:out cmp)
        noise  (faust "abs(no.noise)")
        to     (track-hold noise trig)
        from   (faust "(select2(%trig>0.5,_,%tgt')~_)" {:trig trig :tgt to})
        shaped (faust "0.5*(1.0-cos(2.0*ma.PI*%r))" {:r ramp})
        out    (faust "%from+%s*(%tgt-%from)" {:from from :s shaped :tgt to})]
    (output out)
    (output :ramp ramp)))
