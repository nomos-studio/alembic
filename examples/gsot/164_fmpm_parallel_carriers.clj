; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.164-fmpm-parallel-carriers
  "GSOT p.238 — FMPM-parallel-carriers.maxpat (Chapter 8: Frequent Modulations).

  'FM Algorithms — Parallel Carriers from a Shared Modulator'
  ------------------------------------------------------------
  The simplest FM algorithm extension: one modulator drives two or more
  carriers simultaneously.  Each carrier has its own C:M ratio (and therefore
  its own sideband set), but the modulation index and envelope are shared.

  Signal graph:

      modulator  osc(fm)
          │
          ├──→  carrier 1:  sin(2π × phasor(fc1 ± ix×fm×mod))
          │
          └──→  carrier 2:  sin(2π × phasor(fc2 ± ix×fm×mod))
                                       │
                                       ▼
                              :mx × c1  +  (1−:mx) × c2

  where fc1 = fm × :r1 and fc2 = fm × :r2.

  Parameterising with ratios
  --------------------------
  Both carrier frequencies are expressed as multiples of the shared modulator
  frequency :fm, following the ex.163 convention.  This keeps the C:M ratios
  explicit and makes harmonic relationships easy to reason about:

    :r1=1.0 :r2=2.0 :fm=220 → fc1=220, fc2=440 (octave carrier pair)
    :r1=1.0 :r2=1.5 :fm=220 → fc1=220, fc2=330 (fifth carrier pair)
    :r1=1.0 :r2=1.4 :fm=220 → fc1=220, fc2=308 (inharmonic carrier pair)

  Spectral consequences
  ---------------------
  Carrier 1 produces sidebands at: fc1 + n×fm  =  fm×(r1 + n)
  Carrier 2 produces sidebands at: fc2 + n×fm  =  fm×(r2 + n)

  The combined spectrum is the sum of both sideband sets.  When r2 − r1 is an
  integer, some sidebands coincide (reinforcement or cancellation depending on
  phase).  When r2 − r1 is irrational, the two sets are interleaved — denser
  and more complex than either alone.

  The :mx parameter blends between the two carriers:
    :mx=0:   only carrier 1 (behaviour of ex.163 with :rt=:r1)
    :mx=0.5: equal mix — full parallel carrier timbre
    :mx=1:   only carrier 2 (behaviour of ex.163 with :rt=:r2)

  DX7 / algorithm connection
  --------------------------
  This is the building block of multi-operator FM algorithms.  The Yamaha DX7
  'algorithm 5' is four parallel carriers sharing one modulator — exactly this
  pattern extended to four voices.  'Algorithm 1' chains operators (modulator
  of a modulator), which is a different topology; see ex.165 and beyond.

  FM/PM morph (:md)
  -----------------
  :md=0 → FM: modulator enters at frequency input of each carrier's phasor
  :md=1 → PM: modulator enters at phase output of each carrier's phasor
  Same routing as ex.161–163; both carriers receive the same :md.

  Parameters
  ----------
  :fm — shared modulator frequency in Hz (1–2000; default 220)
  :r1 — carrier 1 ratio (fc1 = fm × :r1; 0.1–8.0; default 1.0)
  :r2 — carrier 2 ratio (fc2 = fm × :r2; 0.1–8.0; default 2.0)
  :ix — shared modulation index β (0–10; default 2.0)
  :mx — carrier mix; 0=only c1, 1=only c2 (0–1; default 0.5)
  :md — FM→PM morph; 0.0=FM, 1.0=PM (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained FM/PM synthesizer)
    :out — mixed parallel-carrier FM/PM output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! fmpm-parallel-carriers
  {:params {:fm {:range [1.0 2000.0]  :default 220.0}
            :r1 {:range [0.1 8.0]     :default 1.0}
            :r2 {:range [0.1 8.0]     :default 2.0}
            :ix {:range [0.0 10.0]    :default 2.0}
            :mx {:range [0.0 1.0]     :default 0.5}
            :md {:range [0.0 1.0]     :default 0.0}}}
  (let [fm  (param :fm)
        r1  (param :r1)
        r2  (param :r2)
        ix  (param :ix)
        mx  (param :mx)
        md  (param :md)
        mo  (faust "os.osc(%{fm})" {:fm fm})
        c1  (faust "sin(2.0*ma.PI*os.phasor(1,%{fm}*%{r1}+(1.0-%{md})*%{ix}*%{fm}*%{mo})+%{md}*%{ix}*%{mo})"
                   {:fm fm :r1 r1 :md md :ix ix :mo mo})
        c2  (faust "sin(2.0*ma.PI*os.phasor(1,%{fm}*%{r2}+(1.0-%{md})*%{ix}*%{fm}*%{mo})+%{md}*%{ix}*%{mo})"
                   {:fm fm :r2 r2 :md md :ix ix :mo mo})
        out (faust "(1.0-%{mx})*%{c1}+%{mx}*%{c2}" {:mx mx :c1 c1 :c2 c2})]
    (output :out out)))
