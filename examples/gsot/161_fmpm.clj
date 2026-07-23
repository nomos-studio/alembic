; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.161-fmpm
  "GSOT pp.232-233 — FMPM.maxpat (Chapter 8: Frequent Modulations).

  'Frequency Modulation and Phase Modulation — Unified Synthesis'
  ---------------------------------------------------------------
  FM and PM are the two canonical ways to modulate a carrier oscillator with
  a second signal.  They differ only in *where* the modulator enters the
  carrier's phase computation:

      FM:  sin(2π × phasor(fc + ix×fm×mod(fm)))    — frequency is modulated
      PM:  sin(2π × phasor(fc)  + ix × mod(fm))    — phase is modulated

  Sideband structure (Bessel expansion)
  --------------------------------------
  For either FM or PM with sinusoidal modulator osc(fm) and index β = ix:

      y(t)  =  Σ_{n=-∞}^{∞}  J_n(β) × sin(2π(fc + n×fm)t)

  where J_n(β) is the nth-order Bessel function of the first kind.  Sidebands
  appear at fc ± n×fm for all integers n, with amplitudes J_n(β).

  J_0(β): carrier component; decreases then recovers as β increases
  J_1(β): first sidebands fc±fm; grows from 0 at β=0, peaks near β≈1.8
  J_n(β): nth sidebands; significant only when β ≳ n

  At β=0: only the carrier (dry). At β=1: carrier + two first sidebands.
  At β=2: five significant components.  At β=8+: dense harmonic spectrum.

  Modulation index parameterisation
  -----------------------------------
  Peak frequency deviation for FM: Δf = ix × fm  Hz
  FM modulation index:             β  = Δf / fm  = ix    (constant!)
  PM peak phase deviation:         β  = ix        radians

  With the parameterisation `fc + ix×fm×mod` the FM index β = ix regardless
  of :fm.  The number and distribution of sidebands depends only on :ix, not
  on the modulator frequency.  This is the Chowning convention.

  FM vs PM — the spectral identity
  ----------------------------------
  FM:  phase = 2π∫(fc + ix×fm×sin(2π×fm×t))dt
             = 2π×fc×t − ix×cos(2π×fm×t) + ix
             = 2π×fc×t + ix×sin(2π×fm×t − π/2) + ix

  PM:  phase = 2π×fc×t + ix×sin(2π×fm×t)

  The only difference is a −π/2 phase offset in the modulator term
  (cosine vs sine).  This produces identical *amplitude* spectra — the
  Bessel coefficients |J_n(ix)| are the same.  The phase relationships
  between sidebands differ, but that is perceptually inaudible.

  FM and PM are spectrally identical given the same ix and fm.

  The perceptual difference emerges when :fm changes:
    FM: Δf = ix×fm — frequency deviation grows with fm; same β = ix always.
        As fm doubles, sideband spacing doubles but amplitudes (Bessel) hold.
    PM: Δf = ix×fm — same formula! Indistinguishable in this parameterisation.
        (The distinction is only visible when ix is defined as a phase quantity
        independent of fm, which corresponds to the alternative PM convention
        Δf = ix — constant Hz deviation regardless of fm.)

  The :md morph
  -------------
  :md = 0.0 → pure FM:  modulator routes to frequency input
  :md = 1.0 → pure PM:  modulator routes to phase input

  Implementation:
      carrier_phase = os.phasor(1, fc + (1−md)×ix×fm×mod)
      phase_offset  = md × ix × mod
      out = sin(2π × carrier_phase + phase_offset)

  At md=0: FM — modulator perturbs the accumulated frequency.
  At md=1: PM — carrier accumulates at fc; modulator shifts phase directly.
  At intermediate md: a smooth blend (both routing paths partially active).

  Carrier-to-modulator ratio (C:M)
  ---------------------------------
  When fc/fm is a simple ratio, sidebands land on harmonics of the fm pitch:
    fc/fm = 1:1 — sidebands at 0, fc, 2fc, 3fc,… → sawtooth-like
    fc/fm = 2:1 — sidebands at fc/2, fc, 3fc/2, 2fc,… → missing odd harmonics
    fc/fm irrational — inharmonic sidebands → metallic, bell-like spectrum

  Parameters
  ----------
  :fc — carrier frequency in Hz (20–4000; default 440)
  :fm — modulator frequency in Hz (1–4000; default 220)
  :ix — modulation index β; 0=dry carrier, higher=more sidebands (0–10; default 1)
  :md — FM→PM morph; 0.0=FM, 1.0=PM (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained FM/PM synthesizer)
    :out — FM/PM output signal"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! fmpm
  {:params {:fc {:range [20.0 4000.0] :default 440.0}
            :fm {:range [1.0 4000.0]  :default 220.0}
            :ix {:range [0.0 10.0]    :default 1.0}
            :md {:range [0.0 1.0]     :default 0.0}}}
  (let [fc  (param :fc)
        fm  (param :fm)
        ix  (param :ix)
        md  (param :md)
        mo  (faust "os.osc(%fm)" {:fm fm})
        out (faust "sin(2.0*ma.PI*os.phasor(1,%fc+(1.0-%md)*%ix*%fm*%mo)+%md*%ix*%mo)"
                   {:fc fc :fm fm :ix ix :md md :mo mo})]
    (output :out out)))
