; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.166-fmpm-blending
  "GSOT pp.239-241 — FMPM-blending.maxpat (Chapter 8: Frequent Modulations).

  'Beyond Sinusoidal Modulators — Waveform Blending in FM/PM'
  -----------------------------------------------------------
  All previous FM/PM examples used a sinusoidal modulator `os.osc(fm)`.
  A sinusoidal modulator produces exactly the Bessel sideband picture:
  components at fc ± n×fm with amplitudes J_n(β).

  Using a *non-sinusoidal* modulator is equivalent to using multiple
  simultaneous sinusoidal modulators at the harmonic frequencies of that
  waveform.  The resulting spectrum is a superposition of Bessel clusters,
  one per harmonic of the modulator:

  Sawtooth modulator (all harmonics, 1/n amplitudes):
      mod(t)  =  Σ_{k=1}^{∞}  (1/k) × sin(2π×k×fm×t)

  → FM output = superposition of Bessel clusters at fc ± n×(k×fm)
                for all integers n and all harmonics k=1,2,3,…
  → Very dense, harmonically rich spectrum.  At large β the clusters
    overlap and the output approaches noise.

  Triangle modulator (odd harmonics, 1/k² amplitudes):
      mod(t)  =  Σ_{k=1,3,5,…}  (8/(π²k²)) × sin(2π×k×fm×t)

  → Bessel clusters at fc ± n×(k×fm) for odd k only, but rapidly
    diminishing.  Softer than sawtooth; only odd-harmonic clusters
    contribute significantly.

  The :bl blend parameter
  -----------------------
  :bl morphs the modulator waveform shape from pure sine to pure sawtooth:

      mo(t)  =  (1 − :bl) × sin(2π×fm×t)  +  :bl × sawtooth(fm×t)

  :bl=0.0:  sinusoidal modulator — clean Bessel sidebands (ex.161–165)
  :bl=0.5:  equal mix — second and higher harmonics of fm partially present
  :bl=1.0:  sawtooth modulator — full harmonic series of fm in modulator

  The effective modulation index for each harmonic k of the sawtooth is
  β_k = β × (1/k), so the fundamental cluster (k=1) dominates and higher
  clusters fade out with 1/k.  Even so, the spectral density grows quickly:
  at :bl=1 and β=2 there are already dozens of audible components.

  Why this matters
  ----------------
  With sinusoidal modulators, the only way to add more spectral complexity
  was to add more operators (ex.164–165).  With non-sinusoidal modulators,
  a single operator implicitly carries an infinite stack of sinusoidal
  modulators, each at a harmonic of fm.  The :bl knob is a compact path
  from the clean, analysable sine-only spectrum to a much richer one.

  This also reframes the sawtooth oscillator as a *spectral tool*: choosing
  the modulator waveform selects which harmonic stack drives the FM clusters.

  Spectral comparison at :bl=0 vs :bl=1 (β=2, :rt=1)
  -------------------------------------------------------
  :bl=0 (sine):   fc, fc±fm, fc±2fm, fc±3fm          (Bessel amplitudes)
  :bl=1 (saw):    fc±fm cluster  (β=2, as above)
                + fc±2fm cluster (β=2/2=1, smaller)
                + fc±3fm cluster (β=2/3, smaller still)
                + …
  The :bl=1 spectrum has the :bl=0 spectrum as its dominant layer, plus
  progressively weaker cluster copies interleaved at every harmonic of fm.

  FM/PM routing (:md)
  --------------------
  The blended modulator replaces `os.osc(fm)` in the standard FM/PM formula:

    FM: sin(2π × phasor(fc + ix×fm×mo_blended))
    PM: sin(2π × phasor(fc) + ix×mo_blended)
    morph: sin(2π × phasor(fc + (1−md)×ix×fm×mo) + md×ix×mo)

  Parameters
  ----------
  :fc — carrier frequency in Hz (20–2000; default 220)
  :rt — C:M ratio (fm = fc × :rt; 0.1–8.0; default 1.0)
  :ix — modulation index β (0–10; default 2.0)
  :bl — modulator waveform blend; 0=sine, 1=sawtooth (0–1; default 0.0)
  :md — FM→PM morph; 0.0=FM, 1.0=PM (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained FM/PM synthesizer)
    :out — FM/PM output with blended modulator waveform"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! fmpm-blending
  {:params {:fc {:range [20.0 2000.0]  :default 220.0}
            :rt {:range [0.1 8.0]      :default 1.0}
            :ix {:range [0.0 10.0]     :default 2.0}
            :bl {:range [0.0 1.0]      :default 0.0}
            :md {:range [0.0 1.0]      :default 0.0}}}
  (let [fc  (param :fc)
        rt  (param :rt)
        ix  (param :ix)
        bl  (param :bl)
        md  (param :md)
        fm  (faust "%fc*%rt" {:fc fc :rt rt})
        mo  (faust "(1.0-%bl)*os.osc(%fm)+%bl*os.sawtooth(%fm)" {:bl bl :fm fm})
        out (faust "sin(2.0*ma.PI*os.phasor(1,%fc+(1.0-%md)*%ix*%fm*%mo)+%md*%ix*%mo)"
                   {:fc fc :fm fm :ix ix :md md :mo mo})]
    (output :out out)))
