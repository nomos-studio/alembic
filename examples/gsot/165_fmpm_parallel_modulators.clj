; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.165-fmpm-parallel-modulators
  "GSOT p.238 — FMPM-parallel-modulators.maxpat (Chapter 8: Frequent Modulations).

  'FM Algorithms — Parallel Modulators into a Single Carrier'
  -----------------------------------------------------------
  The dual of ex.164: two modulators feed into one carrier.  Each modulator
  contributes its own sideband cluster centred on the carrier, and the clusters
  combine into a dense, complex spectrum that no single modulator can produce.

  Signal graph:

      modulator 1  osc(fm1)  × (i1 × fm1)  ──┐
                                               ├──→  Σ freq deviation  →  phasor(fc + Σ)  →  sin
      modulator 2  osc(fm2)  × (i2 × fm2)  ──┘

  FM:  out = sin(2π × phasor(fc + i1×fm1×osc(fm1) + i2×fm2×osc(fm2)))
  PM:  out = sin(2π × phasor(fc)  +  i1×osc(fm1) + i2×osc(fm2))

  where fm1 = fc × :r1  and  fm2 = fc × :r2.

  Parameterising with ratios
  --------------------------
  Both modulator frequencies are expressed as ratios of the carrier :fc,
  consistent with ex.163.  This makes the C:M structure of each modulator
  explicit:

    :r1=1.0 :r2=2.0 :fc=220 → fm1=220 (1:1 ratio), fm2=440 (1:2 ratio)
    :r1=1.0 :r2=1.5 :fc=220 → fm1=220 (1:1), fm2=330 (1:1.5 = 2:3)
    :r1=1.0 :r2=1.4 :fc=220 → fm1=220 (harmonic), fm2=308 (inharmonic)

  Spectral consequences
  ---------------------
  Modulator 1 alone would produce sidebands at: fc + n×fm1  (Bessel amplitudes J_n(i1))
  Modulator 2 alone would produce sidebands at: fc + n×fm2  (Bessel amplitudes J_n(i2))

  Together they produce intermodulation components at:
      fc + p×fm1 + q×fm2   for all integers p, q

  The spectrum is far denser than either modulator alone.  When fm1/fm2 is
  rational (e.g. 2:3), many intermodulation components land on shared harmonics,
  reinforcing or cancelling.  When the ratio is irrational, the intermodulation
  grid is inharmonic — complex, noisy, metallic.

  Independent index control
  -------------------------
  :i1 and :i2 independently control the depth of each modulator:
    i1=3, i2=0: equivalent to ex.163 with :rt=:r1
    i1=0, i2=3: equivalent to ex.163 with :rt=:r2
    i1=i2=1:    mild intermodulation, both modulators contribute equally
    i1=i2=4:    dense, complex spectrum; many intermodulation products audible

  This is the key creative advantage over parallel carriers (ex.164): you can
  balance two distinct timbral 'flavours' (e.g. harmonic + inharmonic modulation)
  without changing the output mix — the two modulators are baked into one carrier.

  FM/PM morph (:md)
  -----------------
  FM: both modulators enter the carrier's frequency input (accumulated phase)
      freq_deviation = i1×fm1×osc(fm1) + i2×fm2×osc(fm2)
      out = sin(2π × phasor(fc + freq_deviation))

  PM: both modulators enter the carrier's phase output (instantaneous phase)
      phase_offset  = i1×osc(fm1) + i2×osc(fm2)
      out = sin(2π × phasor(fc) + phase_offset)

  Morph: out = sin(2π × phasor(fc + (1−md)×freq_dev) + md×phase_off)

  DX7 / algorithm connection
  --------------------------
  This topology (two or more modulators summed into one carrier) appears as a
  sub-graph in many DX7 algorithms.  DX7 'algorithm 18' is two groups of one
  carrier / two parallel modulators — exactly this patch, doubled.  Stacking
  modulators with different C:M ratios and indices is the core mechanism behind
  the DX7's timbral range.

  Parameters
  ----------
  :fc — carrier frequency in Hz (20–2000; default 220)
  :r1 — modulator 1 ratio (fm1 = fc × :r1; 0.1–8.0; default 1.0)
  :r2 — modulator 2 ratio (fm2 = fc × :r2; 0.1–8.0; default 2.0)
  :i1 — modulation index for modulator 1 (0–10; default 2.0)
  :i2 — modulation index for modulator 2 (0–10; default 1.0)
  :md — FM→PM morph; 0.0=FM, 1.0=PM (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained FM/PM synthesizer)
    :out — parallel-modulator FM/PM output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! fmpm-parallel-modulators
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
        f1  (faust "%{fc}*%{r1}" {:fc fc :r1 r1})
        f2  (faust "%{fc}*%{r2}" {:fc fc :r2 r2})
        m1  (faust "os.osc(%{f1})" {:f1 f1})
        m2  (faust "os.osc(%{f2})" {:f2 f2})
        fd  (faust "%{i1}*%{f1}*%{m1}+%{i2}*%{f2}*%{m2}" {:i1 i1 :f1 f1 :m1 m1 :i2 i2 :f2 f2 :m2 m2})
        pd  (faust "%{i1}*%{m1}+%{i2}*%{m2}" {:i1 i1 :m1 m1 :i2 i2 :m2 m2})
        out (faust "sin(2.0*ma.PI*os.phasor(1,%{fc}+(1.0-%{md})*%{fd})+%{md}*%{pd})"
                   {:fc fc :md md :fd fd :pd pd})]
    (output :out out)))
