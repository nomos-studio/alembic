; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.149-comb-enharmonic
  "GSOT pp.217-218 — comb_enharmonic.maxpat (Chapter 8).

  'Enharmonic Combs — Two Feedforward Taps at Inharmonic Ratios'
  ---------------------------------------------------------------
  A standard comb filter (ex.148) has resonances at integer multiples of
  1/D Hz — a perfectly harmonic series.  An enharmonic comb breaks this by
  combining two independent feedforward taps whose delay times are in an
  irrational ratio.  The resulting resonance pattern is not a harmonic series:
  it has no single fundamental, and the peaks never repeat at a fixed interval
  across the spectrum.

  Transfer function: H(z) = 1 + g₁·z^{-D₁} + g₂·z^{-D₂}

  Why inharmonic?
  ----------------
  Tap A alone would give peaks at k/D₁ Hz (harmonic).
  Tap B alone would give peaks at k/D₂ Hz (a different harmonic series).
  Combined, the interference pattern has peaks where both constructive
  interferences coincide — which only happens at the LCM of D₁ and D₂ samples.
  When D₂/D₁ is irrational (e.g. φ ≈ 1.618, √2 ≈ 1.414), the LCM is infinite:
  no two peaks ever exactly coincide, and the spectrum is dense and inharmonic.

  When D₂/D₁ is a simple rational fraction (2/1, 3/2, 4/3…), the peaks realign
  periodically and the result is more harmonic — so the ratio controls the degree
  of inharmonicity.

  Default :t2 = :t1 × φ (golden ratio) — maximally inharmonic by LCM argument;
  φ has the worst rational approximations of any irrational number.

  Practical sound
  ----------------
  Short tap times (1–10 ms): metallic colouration, gong-like, inharmonic ring.
  Longer tap times (10–50 ms): comb-like filtering with complex, shifting texture.
  Independent gain signs (g₁ > 0, g₂ < 0): emphasises the interference pattern.

  Contrast with ex.150 (dispersive feedback comb): that approach achieves
  inharmonic resonances through allpass phase dispersion in the feedback path,
  modelling the inharmonicity of stiff strings and struck metal.

  Parameters
  ----------
  :t1 — tap A delay in milliseconds (0.1–100; default 10.0)
  :t2 — tap B delay in milliseconds (0.1–100; default 16.18  ≈ t1 × φ)
  :g1 — tap A gain (−1–1; default 0.5)
  :g2 — tap B gain (−1–1; default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: two-tap enharmonic comb output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! comb-enharmonic
  {:params {:t1 {:range [0.1 100.0] :default 10.0}
            :t2 {:range [0.1 100.0] :default 16.18}
            :g1 {:range [-1.0 1.0]  :default 0.5}
            :g2 {:range [-1.0 1.0]  :default 0.5}}}
  (let [in  (audio-in)
        t1  (param :t1)
        t2  (param :t2)
        g1  (param :g1)
        g2  (param :g2)
        d1  (faust "de.delay(int(ma.SR*5.0),int(max(0.0,%t1*ma.SR/1000.0)),%in)"
                   {:t1 t1 :in in})
        d2  (faust "de.delay(int(ma.SR*5.0),int(max(0.0,%t2*ma.SR/1000.0)),%in)"
                   {:t2 t2 :in in})
        out (faust "%in+%g1*%d1+%g2*%d2" {:in in :g1 g1 :d1 d1 :g2 g2 :d2 d2})]
    (output :out out)))
