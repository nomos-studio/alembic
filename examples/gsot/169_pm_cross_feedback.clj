; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.169-pm-cross-feedback
  "GSOT p.246 — PM-cross-feedback.maxpat (Chapter 8: Frequent Modulations).

  'Cross-Coupled Feedback Modulation — Mutual PM/FM'
  ---------------------------------------------------
  Two oscillators each modulate the other's phase (or frequency) via a
  1-sample delay feedback path:

      osc1[n] = sin(2π × phasor(fc)   + ix1 × osc2[n−1])
      osc2[n] = sin(2π × phasor(fc×rt) + ix2 × osc1[n−1])
      out[n]  = 0.5 × (osc1[n] + osc2[n])

  Each oscillator simultaneously acts as both carrier and modulator to the
  other.  The result is a coupled nonlinear system whose behaviour depends
  on the frequency ratio (:rt), the coupling strengths (:i1, :i2), and the
  initial conditions.

  Faust implementation — si.bus(2) cross-feedback
  -------------------------------------------------
  Two-channel feedback is expressed with Faust's `si.bus(2)` combinator:

      xfb(a, b) = osc1_modulated_by_b,
                  osc2_modulated_by_a;
      out = (xfb ~ si.bus(2)) :> *(0.5)

  `xfb ~ si.bus(2)` routes each output back to the *other* function's input
  after one sample.  `:> *(0.5)` merges the two outputs (sums and halves)
  into the single audio output.  Each phasor has its own independent phase
  state; the feedback adds a second pair of state registers for the coupling.

  FM/PM routing (:md)
  --------------------
  As in ex.161–168, :md morphs between FM (mod→frequency) and PM (mod→phase):

    PM (:md=1): osc1[n] = sin(2π × phasor(fc)   + i1 × osc2[n−1])
    FM (:md=0): osc1[n] = sin(2π × phasor(fc   + (1−md)×i1×fc×osc2[n−1]))
    morph:      osc1[n] = sin(2π × phasor(fc   + (1−md)×i1×fc×osc2[n−1])
                                      + md×i1×osc2[n−1])

  The patch name `PM-cross-feedback` suggests PM (:md=1.0) as the primary
  mode; :md defaults to 1.0 accordingly.

  Coupling regimes — :rt=1.0 (unison)
  -------------------------------------
  When both oscillators share the same frequency (:rt=1.0):

  :i1=:i2=0:     two independent sines; output is their average — pure sine
  :i1=:i2 small: mutual perturbation; slight harmonic enrichment
  :i1=:i2 ≈1:    synchronisation onset; oscillators entrain to each other
  :i1=:i2 ≈2:    strong synchronisation; one of two modes emerges:
      In-phase lock:    both oscillators track the same phase → output
                        sounds like a single oscillator at full amplitude
                        (then attenuated by the ×0.5 mix)
      Anti-phase lock:  oscillators settle 180° apart → outputs cancel →
                        near-silence (try detuning with :rt≈1.001 to break)
      Quadrature:       90° phase separation → complex modulation; the
                        two oscillators trade energy rhythmically
  :i1=:i2 > 2:   potentially chaotic; coupling amplifies each oscillator's
                  nonlinearity; earlier chaos onset than solo feedback (ex.168)

  Coupling regimes — detuning (:rt near 1.0)
  --------------------------------------------
  Small detuning (:rt=1.005, :rt=1.01) with moderate coupling produces
  frequency *pulling*: the oscillators partially synchronise, beating at a
  rate lower than the free-running frequency difference.  This is the FM
  analogue of injection locking in electronic oscillators.

  At stronger coupling the oscillators may fully lock (one-to-one), at weaker
  coupling the beating persists but with FM-like sidebands around fc and fc×rt.

  Coupling regimes — harmonic ratios
  ------------------------------------
  :rt=2.0 (octave):  osc2 can lock to the subharmonic of osc1; output has
                      both fc and 2×fc components, richly intermodulated
  :rt=1.5 (fifth):   harmonic locking; partial tones appear at GCD(fc, fc×rt)
  :rt=φ (golden):    inharmonic; coupling produces irregular sideband patterns;
                      chaotic at lower :i1/:i2 than harmonic ratios

  Asymmetric coupling (:i1≠:i2)
  --------------------------------
  When the two coupling indices differ, one oscillator is more influenced than
  the other.  The more strongly-driven oscillator loses its 'own' frequency
  more readily and tracks the other.  At extreme asymmetry (:i1=2, :i2=0)
  this degenerates to single-oscillator feedback (ex.168) plus an unmodulated
  sine — the coupling collapses to one-way modulation.

  Relationship to prior FM patches
  ----------------------------------
  ex.168 (feedback):  one oscillator modulates itself → solo nonlinear dynamics
  ex.169 (cross-feedback): two oscillators modulate each other → coupled
                            nonlinear system with synchronisation phenomena

  Cascade (ex.167) and cross-feedback are topologically distinct: cascade
  is a directed acyclic graph (top→middle→carrier), cross-feedback is a
  directed cyclic graph (osc1↔osc2).  Cycles introduce the possibility of
  synchronisation, which acyclic topologies cannot produce.

  Parameters
  ----------
  :fc — base oscillator frequency in Hz; osc1 at :fc (20–2000; default 220)
  :rt — osc2 frequency ratio; osc2 at fc×:rt (0.5–4.0; default 1.0)
  :i1 — coupling index: osc2→osc1 modulation depth (0–4; default 1.0)
  :i2 — coupling index: osc1→osc2 modulation depth (0–4; default 1.0)
  :md — FM→PM morph; 0.0=FM, 1.0=PM (0–1; default 1.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained cross-coupled FM/PM oscillator pair)
    :out — 0.5 × (osc1 + osc2) — equal mix of both oscillators"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! pm-cross-feedback
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :rt {:range [0.5 4.0]     :default 1.0}
            :i1 {:range [0.0 4.0]     :default 1.0}
            :i2 {:range [0.0 4.0]     :default 1.0}
            :md {:range [0.0 1.0]     :default 1.0}}}
  (let [fc  (param :fc)
        rt  (param :rt)
        i1  (param :i1)
        i2  (param :i2)
        md  (param :md)
        f2  (faust "%fc*%rt" {:fc fc :rt rt})
        out (faust "(xfb ~ si.bus(2)) :> *(0.5)\n  with {\n    xfb(a,b) = sin(2.0*ma.PI*os.phasor(1,%fc+(1.0-%md)*%i1*%fc*b)+%md*%i1*b),\n               sin(2.0*ma.PI*os.phasor(1,%f2+(1.0-%md)*%i2*%f2*a)+%md*%i2*a);\n  }"
                   {:fc fc :f2 f2 :i1 i1 :i2 i2 :md md})]
    (output :out out)))
