; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.183-modfm
  "GSOT pp.262-263 — ModFM.maxpat (Chapter 8: Frequent Modulations).

  'Modified FM'
  -------------
  Modified FM (Lazzarini & Timoney, 2010) replaces the sinusoidal modulator
  in standard PM/FM with a triangle wave derived via the arcsin function:

      standard PM:  sin(2π × phasor(fc) + ix × sin(2π × fm × t))
      Modified FM:  sin(2π × phasor(fc) + ix × tri(2π × fm × t))

  where tri(x) = asin(sin(x)) / (π/2) — a triangle wave normalised to ±1.

  The arcsin transformation
  --------------------------
  For any angle x, `asin(sin(x))` maps the sinusoid through its inverse,
  producing a triangle wave:

      asin(sin(x)) = x          for x ∈ [−π/2, π/2]   (rising slope)
                    = π − x      for x ∈ [π/2,  3π/2]  (falling slope)
                    = x − 2π    for x ∈ [3π/2, 5π/2]  (rising slope, next cycle)

  The amplitude of asin(sin(x)) is ±π/2 (not ±1).  Dividing by π/2
  normalises it to ±1, making the PM index :ix behave consistently with
  all other FM/PM examples in this chapter:

      mo = asin(os.osc(:fm)) / (π/2)    — triangle wave, ±1 amplitude

  The Fourier series of the normalised triangle wave:

      tri(t) = (8/π²) × Σ_{k=0}^∞ (−1)^k / (2k+1)²  ×  sin((2k+1) × ωm × t)

  Odd harmonics only (n = 1, 3, 5, 7, …), with 1/n² amplitude rolloff.

  Comparison of modulator spectra
  --------------------------------
  The modulator type determines which sidebands appear and at what amplitudes:

  sine modulator (ex.161):       n=1 only; 1 sideband pair at ±fm
  triangle modulator (ModFM):    n=1,3,5,… ; 1/n² rolloff; odd pairs only
  sawtooth modulator (ex.166):   n=1,2,3,… ; 1/n  rolloff; all pairs

  The triangle sits between sine and sawtooth:
    — More spectrally rich than sine (multiple Bessel clusters)
    — Fewer, weaker sideband clusters than sawtooth (1/n² vs 1/n)
    — Odd-harmonics-only symmetry; even pairs suppressed

  Bessel structure
  -----------------
  The nth harmonic of the triangle modulator contributes a Bessel cluster
  with index β_n = ix × a_n where a_n = (8/π²) × (−1)^k / (2k+1)² for n=2k+1.

      n=1 (fundamental):   β_1 = ix × 8/π² ≈ ix × 0.811     dominant cluster
      n=3 (first harmonic): β_3 = ix × 8/(9π²) ≈ ix × 0.090  weaker cluster
      n=5 (second harmonic): β_5 = ix × 8/(25π²) ≈ ix × 0.032 very weak

  In practice, only the n=1 cluster (centred on fc, sidebands at fc ± fm)
  is significant.  The n=3 cluster (sidebands at fc ± 3fm) is about 9× weaker
  in index and barely audible at moderate :ix.

  This is why Modified FM sounds 'between' single-modulator PM and multi-harmonic
  blended PM: the dominant contribution is from a slightly-compressed-index
  single-cluster, with faint odd-multiple sidebands above.

  Relationship to other examples
  --------------------------------
  ex.173 (waveshaping modulator):  shaped the modulator with ma.tanh(dr×osc) / ma.tanh(dr)
      — only odd harmonics (tanh is odd); amplitude-dependent rolloff.
      tanh shaping: harmonic rolloff grows with drive.

  ex.183 (ModFM / arcsin modulator): asin(osc) / (π/2)
      — only odd harmonics (asin(sin) is odd); fixed 1/n² rolloff.
      arcsin shaping: fixed triangle-wave harmonic profile regardless of :ix.

  ex.166 (blending modulator):  (1−bl)×osc + bl×sawtooth
      — all harmonics at :bl=1; continuous transition from sine to sawtooth.

  ModFM occupies a specific, musically useful point in modulator-space:
  the 1/n² rolloff is gentle enough to be harmonically rich but fast enough
  to avoid aliasing issues that plague the sawtooth modulator at high :ix.

  FM / PM routing (:md)
  ----------------------
  The triangle modulator drops into the standard FM/PM morph formula from
  ex.161, replacing os.osc(fm) with the normalised triangle:

      FM (md=0): sin(2π × phasor(fc + ix×fm×mo))
      PM (md=1): sin(2π × phasor(fc) + ix×mo)
      morph: sin(2π × phasor(fc + (1−md)×ix×fm×mo) + md×ix×mo)

  where mo = asin(os.osc(fm)) / (π/2).

  In FM mode: the frequency deviation at the carrier is ±ix×fm×mo, with mo
  peak ±1 → peak deviation ±ix×fm Hz.  Same convention as ex.161.

  In PM mode: the phase deviation is ±ix radians (same as ex.161 PM convention).

  Carson's rule for ModFM
  -------------------------
  The dominant modulator harmonic has index β_1 ≈ 0.811 × :ix.  Applying
  Carson's rule to the dominant term:

      BW ≈ 2 × (β_1 + 1) × fm  ≈  2 × (0.811 × :ix + 1) × :fc × :rt

  This is slightly narrower than standard FM at the same :ix, reflecting the
  lower effective index of the triangle modulator's fundamental harmonic.

  Parameters
  ----------
  :fc — carrier frequency in Hz (20–2000; default 220)
  :fm — modulator frequency in Hz (1–4000; default 220)
  :ix — modulation index (0–10; default 2.0)
  :md — FM→PM morph; 0.0=FM, 1.0=PM (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained ModFM synthesizer)
    :out — FM/PM output with triangle (arcsin) modulator"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! modfm
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :fm {:range [1.0 4000.0]  :default 220.0}
            :ix {:range [0.0 10.0]    :default 2.0}
            :md {:range [0.0 1.0]     :default 0.0}}}
  (let [fc  (param :fc)
        fm  (param :fm)
        ix  (param :ix)
        md  (param :md)
        mo  (faust "asin(os.osc(%{fm}))/(ma.PI/2.0)" {:fm fm})
        out (faust "sin(2.0*ma.PI*os.phasor(1,%{fc}+(1.0-%{md})*%{ix}*%{fm}*%{mo})+%{md}*%{ix}*%{mo})"
                   {:fc fc :fm fm :ix ix :md md :mo mo})]
    (output :out out)))
