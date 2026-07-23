; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.167-fmpm-cascade-modulation
  "GSOT pp.241-242 — FMPM-cascade-modulation.maxpat (Chapter 8: Frequent Modulations).

  'Modulating Modulators — Cascade FM/PM'
  ----------------------------------------
  Instead of summing two modulators into a carrier (parallel, ex.165), chain
  them: the top operator modulates the middle operator, which then modulates
  the carrier.

  Signal graph:

      top     osc(fm2)          — pure sine, no modulation
        │
        ▼  ix2
      middle  FM(fm1, fm2, i2)  — itself an FM oscillator
        │
        ▼  ix1
      carrier FM(fc, fm1, i1)   — carrier sees an FM signal as its modulator

  where fm1 = fc × :r1  and  fm2 = fc × :r2.

  Middle operator as FM signal
  ----------------------------
  The middle operator is an FM oscillator whose instantaneous frequency is:
      fm1  +  i2 × fm2 × osc(fm2)

  Its Bessel expansion:
      m1(t)  =  Σ_{n=-∞}^{∞}  J_n(i2) × sin(2π × (fm1 + n×fm2) × t)

  The carrier therefore sees a *sum* of sinusoidal modulators at frequencies
  fm1, fm1±fm2, fm1±2×fm2, … each with amplitude J_n(i2).

  Spectral consequences
  ---------------------
  Each component of m1 generates its own Bessel cluster in the output.
  The dominant cluster is centred on fc, driven by the fm1 component of m1
  (amplitude J_0(i2)).  Side clusters appear at fc ± k×fm2 for each cascade
  harmonic k, each with a Bessel expansion around its own centre.

  The output spectrum has components near:
      fc  +  n1 × fm1  +  n1 × n2 × fm2        (qualitative)

  Key contrast with parallel modulators (ex.165):
      parallel:  fc + p×fm1 + q×fm2        — additive, independent grid
      cascade:   fc + n1×fm1 + n1×n2×fm2   — multiplicative; n2 scaled by n1

  In the cascade, the top modulator's influence on the output grows with n1 —
  the first modulator's harmonic index.  High-order sidebands of fm1 get more
  frequency smearing from fm2.  This creates a characteristic 'spreading'
  of spectral energy that sounds denser and less analytically clean than
  parallel modulation.

  Effective modulation index scaling
  -----------------------------------
  For the nth component of m1 driving the carrier (at frequency fm1 + n×fm2):

      effective β_n  ≈  i1 × J_n(i2) × fm1 / (fm1 + n×fm2)

  At n=0 (dominant): β_0 = i1 × J_0(i2)  — reduced by J_0(i2) < 1 for i2 > 0
  As i2 grows, J_0(i2) decreases (carrier component of m1 diminishes) and
  energy spreads to side clusters.  The cascade spectrum 'opens out' as i2
  increases, spreading spectral energy across a wider range of components.

  FM/PM morph (:md) applied at each stage
  ----------------------------------------
  :md=0 (FM): frequency-input routing at both middle and carrier stages
      m1  = sin(2π × phasor(f1 + (1−md)×i2×f2×m2) + md×i2×m2)
      out = sin(2π × phasor(fc + (1−md)×i1×f1×m1) + md×i1×m1)

  :md=1 (PM): phase-input routing at both stages
      m1  = sin(2π × phasor(f1) + i2×m2)
      out = sin(2π × phasor(fc) + i1×m1)

  Both stages receive the same :md, so the morph is global across the cascade.

  DX7 algorithm connection
  ------------------------
  This two-operator cascade is the building block of all DX7 'series' algorithms.

    DX7 Algorithm 5:  op3 → op2 → op1(carrier)    — three-level cascade
    DX7 Algorithm 1:  op6 → op5 → op4 → op3 → op2 → op1  — six-level cascade

  More levels of cascade multiply the spectral complexity further.  Alembic
  can express deeper cascades by chaining additional `sin(2π×phasor(...))` nodes
  — each new level adds one more layer of FM expansion to the signal.

  Interesting settings
  ---------------------
  i2=0:  top mod has no effect; reduces to ex.163 (single modulator)
  i2=1:  mild cascade — slight spectral smearing
  i2=3:  strong cascade — middle mod spectrum dense, carrier sees wide spread
  r1=1, r2=1 (1:1:1 ratio): cascade of same-frequency operators; complex
  r1=1, r2=√2: inharmonic cascade; metallic, bell-like spreading
  r1=2, r2=3:  harmonic cascade; rich but structured

  Parameters
  ----------
  :fc — carrier frequency in Hz (20–2000; default 220)
  :r1 — middle modulator ratio (fm1 = fc × :r1; 0.1–8.0; default 1.0)
  :r2 — top modulator ratio (fm2 = fc × :r2; 0.1–8.0; default 2.0)
  :i1 — carrier modulation index (carrier ← middle mod; 0–10; default 2.0)
  :i2 — middle mod modulation index (middle mod ← top mod; 0–10; default 1.0)
  :md — FM→PM morph, applied at both cascade stages (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained FM/PM synthesizer)
    :out — cascade FM/PM output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! fmpm-cascade-modulation
  {:params {:fc {:range [20.0 2000.0]  :default 220.0}
            :r1 {:range [0.1 8.0]      :default 1.0}
            :r2 {:range [0.1 8.0]      :default 2.0}
            :i1 {:range [0.0 10.0]     :default 2.0}
            :i2 {:range [0.0 10.0]     :default 1.0}
            :md {:range [0.0 1.0]      :default 0.0}}}
  (let [fc  (param :fc)
        r1  (param :r1)
        r2  (param :r2)
        i1  (param :i1)
        i2  (param :i2)
        md  (param :md)
        f1  (faust "%fc*%r1" {:fc fc :r1 r1})
        f2  (faust "%fc*%r2" {:fc fc :r2 r2})
        m2  (faust "os.osc(%f2)" {:f2 f2})
        m1  (faust "sin(2.0*ma.PI*os.phasor(1,%f1+(1.0-%md)*%i2*%f2*%m2)+%md*%i2*%m2)"
                   {:f1 f1 :f2 f2 :i2 i2 :md md :m2 m2})
        out (faust "sin(2.0*ma.PI*os.phasor(1,%fc+(1.0-%md)*%i1*%f1*%m1)+%md*%i1*%m1)"
                   {:fc fc :f1 f1 :i1 i1 :md md :m1 m1})]
    (output :out out)))
