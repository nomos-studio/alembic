; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.39-smooth-stepped
  "GSOT p.70 — smooth-stepped-template / linear-stepped-noise.

  'Shaping smooth stepped interpolation for LFOs and smooth glides'
  -----------------------------------------------------------------
  The core idea: at each phasor wrap, sample a new target value (from any
  source) and remember the previous output.  Use the phasor itself — [0,1)
  — as the blend factor for mix(from, to, phasor).  The output glides
  linearly from the previous output to the new target over one cycle.

  gen~ topology (smooth-stepped-template):

      source ─→ latch(to) ──────────────────────────┐
                                                     ↓
      phasor ─→ go.ramp2trig ─→ (trigger both latches)
                                                     ↓
                                                    mix ─→ out
                                                     ↑
      history(out) ─→ latch(from) ──────────────────┘
               ↑_____________________________|

  Comments on the gen~ patch:
    'sample from/to values at start of each phasor ramp'
    'new input'     → source  → to-latch
    'previous output' → history → from-latch
    'add unit shaper here' (above phasor into mix, for shaped variants)

  linear-stepped-noise (this example):
    source = noise (abs(no.noise) = unipolar)
    The phasor is internal (Hz param).

  Feedback in Faust
  -----------------
  The gen~ `history(out)` one-sample delay avoids a circular dependency
  because it captures `out[n-1]`, not `out[n]`.  In Alembic this is
  expressed using `to'` (previous sample of `to`) as the `from` input:

      to   = (select2(trig > 0.5, _, noise) ~ _)   ; S&H noise on trigger
      from = (select2(trig > 0.5, _, to')   ~ _)   ; S&H prev-to on trigger
      out  = from + ramp * (to - from)              ; linear lerp

  At trigger sample T:
    to[T]   = noise[T]       (new target)
    from[T] = to[T-1]        (previous target, via to')
    out[T]  = from[T] + 0    (ramp=0 at wrap → output = from exactly)

  After trigger, ramp climbs 0→1 while from and to hold:
    out[n]  = to[T-1] + ramp[n] * (to[T] - to[T-1])

  No circular dependency: to → from (via to') → out.  All Alembic nodes
  are a strict DAG.

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
      n10 = n9+n1*(n8-n9);

      process = n10, n1;

  n8 = to   (S&H of noise on trigger)
  n9 = from (S&H of n8' = previous to, on trigger)
  n10 = out  (from + ramp*(to-from))"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! smooth-stepped
  {:params {:hz {:range [0.01 20.0] :default 2.0}}}
  (let [ramp  (phasor (param :hz))
        cmp   (comparator (abs (delta ramp)) (const 0.5))
        trig  (:out cmp)
        noise (faust "abs(no.noise)")
        to    (track-hold noise trig)
        from  (faust "(select2(%trig>0.5,_,%tgt')~_)" {:trig trig :tgt to})
        out   (faust "%from+%ramp*(%tgt-%from)" {:from from :ramp ramp :tgt to})]
    (output out)
    (output :ramp ramp)))
