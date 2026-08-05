; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.181-harmonic
  "GSOT pp.259-260 — Harmonic.maxpat / go.harmonic.gendsp (Chapter 8: Frequent Modulations).

  'Blending Harmonic Oscillators'
  --------------------------------
  The `go.harmonic` gen~ operator is a reusable DSP abstraction (`.gendsp`
  patcher) that generates a bank of phase-locked harmonics from a single
  fundamental phasor, with a continuous blend parameter that sweeps the
  spectral weighting from a pure sine (one partial) to a sawtooth-like
  harmonic series (multiple partials with 1/n rolloff).

  Structure
  ----------
  1. A single phasor drives all harmonics:

         ph = phasor(fc)   — ramp from 0 to 1 at :fc Hz

  2. The nth harmonic (n = 1, 2, ..., N) is generated as:

         h_n = sin(2π × n × ph)   — nth partial, phase-locked to ph

  3. Each harmonic is amplitude-weighted by the blend function:

         a_n(:bl) = :bl^(n−1) / n

  4. The output sums all weighted harmonics:

         out = Σ_{n=1}^{N}  a_n(:bl) × h_n

  Blend function analysis
  -----------------------
  :bl controls how many harmonics contribute and how steeply they roll off:

  a_n(:bl) = :bl^(n−1) / n

  At :bl = 0.0:
      a_1 = 1/1 = 1.0,  a_n = 0  for n ≥ 2.
      Pure sine at fc.  Only the fundamental survives.

  At :bl = 1.0:
      a_n = 1/n  for all n.
      Sawtooth spectrum: amplitudes 1, 1/2, 1/3, ..., 1/8.
      The partial sum Σ 1/n ≈ 2.718 (N=8); peak amplitude is lower due
      to partial phase cancellation.

  At :bl = 0.5:
      a_1 = 1,  a_2 = 0.25,  a_3 = 0.083,  a_4 = 0.016, …
      The harmonics decay rapidly: each successive partial is attenuated
      by 0.5 relative to the previous one.  Sounds warm, flute-like.

  At :bl = 0.8:
      a_1 = 1,  a_2 = 0.4,  a_3 = 0.213,  a_4 = 0.128, a_5 = 0.082, …
      Brighter, clarinet-like — multiple harmonics present with gentle rolloff.

  At :bl = 0.95:
      Very close to sawtooth; all 8 harmonics audible, nearly 1/n weighting.
      Bright, reedy, bowed-string character.

  The blend parameter provides a single dial that traverses the full range
  from pure sine (hollow) to bright sawtooth, passing through musically
  meaningful timbral regions at each position.

  Shared phasor and phase coherence
  -----------------------------------
  All harmonics derive from the same `ph = phasor(1, :fc)`.  The nth harmonic
  is `sin(2π × n × ph)` where `ph ∈ [0, 1)` is the fundamental phase.

  This guarantees phase coherence: all partials are in-phase at the moment
  `ph` crosses zero (the 'cycle start'), producing the characteristic sharp
  attack of a sawtooth wave at :bl=1.  Using independent oscillators
  `os.osc(n × fc)` would lose this coherence — each oscillator initialises
  at an arbitrary phase, and the combined waveform would drift over time.

  The shared phasor also ensures that FM or PM applied to `:fc` (e.g.,
  from the FM patches in this chapter) modulates all harmonics in sync:
  since all harmonics track `ph`, any phase deviation on `ph` propagates
  coherently through the harmonic bank.  This is the foundation for using
  `go.harmonic` as an FM carrier: FM the phasor, not individual oscillators.

  Connection to previous additive examples
  ----------------------------------------
  ex.166 (blending sine/saw modulator): used a sine→sawtooth blend on the
  FM MODULATOR.  The sawtooth modulator was interpreted as a harmonic stack
  of sinusoidal modulators; each harmonic of the modulator creates a Bessel
  cluster around the carrier.

  ex.181 (this): applies a similar blend to the OUTPUT waveform directly —
  additive synthesis rather than modulated synthesis.  The spectrum is built
  by explicit summation, not by FM sideband generation.

  Fourier series relationship
  ----------------------------
  The sawtooth wave (:bl=1) has the Fourier series:

      x(t) = (2/π) × Σ_{n=1}^∞ (−1)^{n+1} × sin(n × 2π × fc × t) / n

  This patch omits the (2/π) normalisation factor and the alternating sign
  (−1)^{n+1} — giving a 'positive-phase' sawtooth (all harmonics in phase).
  The 0.5 output gain compensates for the unnormalised partial sum.

  Normalisation
  -------------
  The partial sum at :bl=1 is Σ 1/n ≈ 2.718 for N=8.  However, since the
  partials are at different frequencies, the actual peak amplitude is less
  than this (partial phase cancellation at the output).  A fixed 0.5 gain
  keeps the output within ≈ ±1 for all :bl values at N=8.

  N = 8 in Faust
  ---------------
  Faust's `par(i, N, expr)` expands `expr` N times at compile time with
  `i` substituted as a literal integer (0, 1, ..., N−1).  Here N=8 is
  fixed.  For more harmonics, increase the literal; for fewer, decrease it.

  The Faust compiler can CSE the shared `%ph` phasor across all 8 sin()
  calls, producing a single phasor register in the compiled C++.

  Parameters
  ----------
  :fc — fundamental frequency in Hz (20–4000; default 220)
  :bl — harmonic blend; 0.0=pure sine, 1.0=sawtooth (0–1; default 0.5)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained additive synthesizer)
    :out — weighted sum of 8 harmonics: Σ bl^(n-1)/n × sin(2π×n×ph); ×0.5 gain"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! harmonic
  {:params {:fc {:range [20.0 4000.0] :default 220.0}
            :bl {:range [0.0 1.0]     :default 0.5}}}
  (let [fc  (param :fc)
        bl  (param :bl)
        ph  (faust "os.phasor(1,%{fc})" {:fc fc})
        out (faust "par(i,8,sin(2.0*ma.PI*(i+1)*%{ph})*pow(%{bl},i)/(i+1.0)):>*(0.5)"
                   {:ph ph :bl bl})]
    (output :out out)))
