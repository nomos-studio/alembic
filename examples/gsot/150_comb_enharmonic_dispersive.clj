; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.150-comb-enharmonic-dispersive
  "Alembic extension — not in GSOT.

  'Dispersive Feedback Comb — Allpass Inharmonicity'
  ---------------------------------------------------
  A second approach to enharmonic comb resonances: instead of two feedforward
  taps at inharmonic ratios (ex.149), put a first-order allpass filter inside
  a single feedback comb loop.  The allpass adds frequency-dependent phase delay
  to the feedback signal on every pass, displacing the resonant peaks away from
  harmonic positions.  The degree of inharmonicity is controlled by a single
  coefficient :bp.

  Physical model connection
  --------------------------
  A perfectly flexible string has harmonic overtones.  A stiff string (piano,
  harpsichord, struck metal) has inharmonic overtones because wave speed depends
  on frequency — higher modes travel faster and arrive earlier, stretching the
  harmonic series upward.  This dispersion is modelled exactly by allpass filters
  in a Karplus-Strong feedback loop: the allpass adds group delay that varies
  with frequency, simulating the frequency-dependent propagation speed.

  Signal chain
  -------------
  1. Feedback loop: y[n] = de.delay(D, x[n] + g · ap(y[n-1]))
  2. Allpass of feedback: ap_y[n] = b·s[n] − b·ap_y[n-1] + s[n-1]
     where s[n] = y[n-1] (the outer ~ _ implicit delay)
     H_ap(z) = (b + z^{-1}) / (1 + b·z^{-1})

  The resonant frequencies shift by: Δf_k ≈ (k·b) / (π·D·SR)  (first-order approx).
  At b=0: harmonic (standard feedback comb).
  At b→±1: maximum dispersion; resonances spread nonlinearly across spectrum.

  Contrast with ex.149
  ----------------------
  ex.149 (two feedforward taps): dense inharmonic spectrum, always stable, no
  sustained resonances — the sound decays as fast as the input.
  ex.150 (dispersive feedback): sustained inharmonic resonances that ring after
  the input stops, with decay controlled by :gn.  Richer for pitched sounds;
  requires |:gn| < 1 for stability.

  Nested ~ _ structure
  ---------------------
  The outer `enh_loop ~ _` closes the delay feedback; `s` is enh_loop[n-1].
  The inner `apl ~ _` runs the allpass on `s`; `p` is apl[n-1].
  Both feedback registers are independent and compile to separate state variables
  in the generated C++.

  Parameters
  ----------
  :ms — delay time in milliseconds; sets base pitch = 1000/:ms Hz (0.1–100; default 10.0)
  :gn — feedback gain; controls decay rate (0–0.99; default 0.7)
  :bp — allpass dispersion coefficient; 0 = harmonic, ±0.99 = maximum inharmonicity
        (−0.99–0.99; default 0.3)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: dispersive inharmonic comb resonator output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! comb-enharmonic-dispersive
  {:params {:ms {:range [0.1 100.0]  :default 10.0}
            :gn {:range [0.0 0.99]   :default 0.7}
            :bp {:range [-0.99 0.99] :default 0.3}}}
  (let [in  (audio-in)
        ms  (param :ms)
        gn  (param :gn)
        bp  (param :bp)
        dl  (faust "int(max(0.0,%{ms}*ma.SR/1000.0))" {:ms ms})
        fdl (faust "enh_loop ~ _\n  with {\n    enh_loop(s) = de.delay(int(ma.SR*5.0),%{dl},%{in}+%{gn}*ap)\n      with {\n        ap = apl ~ _\n          with { apl(p) = %{bp}*s-%{bp}*p+s@1; };\n      };\n  }"
                   {:dl dl :in in :gn gn :bp bp})]
    (output :out fdl)))
