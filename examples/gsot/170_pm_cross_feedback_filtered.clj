; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.170-pm-cross-feedback-filtered
  "GSOT p.247 — PM-cross-feedback-filtered.maxpat (Chapter 8: Frequent Modulations).

  'Filtered Cross-Coupled Feedback — LP in the Coupling Path'
  ------------------------------------------------------------
  ex.169 (cross-feedback) couples two oscillators by routing each output
  directly into the other's phase or frequency input.  At high coupling
  indices (:i1, :i2) the system enters a chaotic regime because the full
  bandwidth of each oscillator's output (including high harmonics produced
  by the nonlinearity) feeds back into the other.

  Inserting a one-pole lowpass filter on each feedback path — between the
  oscillator output and the modulation input — removes high-frequency content
  from the coupling signal before it reaches the other oscillator.  This:

    1. Delays the onset of chaos to higher :i1/:i2 values
    2. Smooths the coupling so the oscillators interact via their fundamental
       and lower harmonics rather than the full broadband signal
    3. Allows stable, rich coupling at index values that would be chaotic
       without the filter

  Filter structure — unity-DC-gain one-pole LP
  ---------------------------------------------
  The same filter used in ex.147 (feedback damping) and analogous to the
  DX7 2-point averaging on its feedback operator:

      y[n] = (1 − :dp) × x[n] + :dp × y[n−1]

  DC gain: (1−:dp)/(1−:dp) = 1 — the fundamental passes at full amplitude.
  At :dp=0: no filtering (y=x, identical to ex.169).
  At :dp=0.9: strong LP — only low frequencies of the coupling signal pass.
  At :dp→1: filter approaches a pure integrator; DC-only coupling.

  Signal graph:
                         LP filter (:dp)
      osc2[n−1] ──→  y2 = (1−dp)×osc2 + dp×y2@1  ──→  mod osc1
      osc1[n−1] ──→  y1 = (1−dp)×osc1 + dp×y1@1  ──→  mod osc2

  Faust implementation — nested with blocks
  ------------------------------------------
  The LP filter uses its own `~ _` state inside the `with` block of the
  `xfb` function definition, independent of the `si.bus(2)` cross-feedback
  state and each phasor's phase accumulator:

      (xfb ~ si.bus(2)) :> *(0.5)
      with {
          xfb(a, b) = sin(2π × phasor(fc + ...) + ...*lb),
                      sin(2π × phasor(f2 + ...) + ...*la)
          with {
              lb = (1−dp)×b : +~*(dp);    — LP applied to osc2's feedback
              la = (1−dp)×a : +~*(dp);    — LP applied to osc1's feedback
          };
      }

  Four independent state registers are created by the Faust compiler:
    - Two phasor phase accumulators (one per oscillator)
    - Two LP filter state registers (one per coupling path)
    - Two cross-feedback delay registers (from si.bus(2))
  Total: 6 state registers for 2 coupled filtered oscillators.

  Effect of :dp on the dynamics
  --------------------------------
  :dp=0.0:   no filtering — identical to ex.169
  :dp=0.5:   moderate LP; attenuates harmonics above fc by ~6 dB/octave
             from the coupling signal; chaos onset deferred
  :dp=0.9:   strong LP; coupling is dominated by the fundamental component;
             synchronisation at :rt=1.0 is more stable; cleaner spectra
  :dp=0.99:  very strong LP; only very slow modulation passes; coupling
             produces slow AM-like amplitude variation between oscillators

  Interaction with :rt (frequency ratio)
  ----------------------------------------
  The LP filter has a frequency-dependent effect on the coupling:
  When :rt ≈ 1.0, both oscillators are near fc and the filter attenuates
  their coupling signals equally.  When :rt = 2.0, osc2 at 2×fc has its
  signal filtered more aggressively (its fundamental is 1 oct higher →
  6 dB more attenuation at same :dp).  This breaks the symmetry:
  osc1 sees a more filtered signal from osc2 than vice versa, even when
  :i1=:i2.

  Comparing ex.169 and ex.170
  ----------------------------
  ex.169 (unfiltered):  raw, full-bandwidth coupling; chaotic at moderate :ix
  ex.170 (filtered):    smoother coupling; more musically stable at high :ix;
                        at :dp=0 the two are identical

  The filter is in the coupling path, not the output path — the output
  retains full bandwidth, only the feedback signal is band-limited.

  Parameters
  ----------
  :fc — base oscillator frequency in Hz; osc1 at :fc (20–2000; default 220)
  :rt — osc2 frequency ratio; osc2 at fc×:rt (0.5–4.0; default 1.0)
  :i1 — coupling index: osc2→osc1 modulation depth (0–4; default 1.5)
  :i2 — coupling index: osc1→osc2 modulation depth (0–4; default 1.5)
  :dp — LP pole coefficient on each coupling path; 0=flat, →1=strong LP (0–1; default 0.5)
  :md — FM→PM morph; 0.0=FM, 1.0=PM (0–1; default 1.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained cross-coupled filtered FM/PM oscillator pair)
    :out — 0.5 × (osc1 + osc2)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! pm-cross-feedback-filtered
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :rt {:range [0.5 4.0]     :default 1.0}
            :i1 {:range [0.0 4.0]     :default 1.5}
            :i2 {:range [0.0 4.0]     :default 1.5}
            :dp {:range [0.0 0.999]   :default 0.5}
            :md {:range [0.0 1.0]     :default 1.0}}}
  (let [fc  (param :fc)
        rt  (param :rt)
        i1  (param :i1)
        i2  (param :i2)
        dp  (param :dp)
        md  (param :md)
        f2  (faust "%{fc}*%{rt}" {:fc fc :rt rt})
        out (faust "(xfb ~ si.bus(2)) :> *(0.5)\n  with {\n    xfb(a,b) = sin(2.0*ma.PI*os.phasor(1,%{fc}+(1.0-%{md})*%{i1}*%{fc}*lb)+%{md}*%{i1}*lb),\n               sin(2.0*ma.PI*os.phasor(1,%{f2}+(1.0-%{md})*%{i2}*%{f2}*la)+%{md}*%{i2}*la)\n    with {\n      lb = (1.0-%{dp})*b : +~*(%{dp});\n      la = (1.0-%{dp})*a : +~*(%{dp});\n    };\n  }"
                   {:fc fc :f2 f2 :i1 i1 :i2 i2 :dp dp :md md})]
    (output :out out)))
