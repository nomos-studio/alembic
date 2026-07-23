; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.168-fmpm-feedback
  "GSOT pp.242-243 — FMPM-feedback.maxpat (Chapter 8: Frequent Modulations).

  'Feedback Modulation — Self-Modulating FM/PM'
  -----------------------------------------------
  All previous FM examples used an *external* modulator — a separate oscillator
  driving the carrier's frequency or phase.  Feedback FM routes the carrier's
  own output (delayed by 1 sample) back into its frequency or phase input.
  The oscillator modulates itself.

  Signal graph:

      out[n−1] ──┐
                  │  × :ix (× :fc if FM)
                  ▼
      phasor(fc + deviation)  →  sin  →  out[n]  ──┐
                                                      │ (1-sample delay: ~ _)
                                                      └──────────────────────┘

  PM feedback (md=1):
      out[n] = sin(2π × phasor(fc) + ix × out[n−1])

  FM feedback (md=0):
      out[n] = sin(2π × phasor(fc + ix × fc × out[n−1]))

  Morph (0 ≤ md ≤ 1):
      out[n] = sin(2π × phasor(fc + (1−md) × ix × fc × out[n−1])
                             + md × ix × out[n−1])

  Spectral evolution with :ix
  ---------------------------
  :ix=0.0:    pure sine at fc — no feedback, identical to os.osc(fc)
  :ix≈0.2:    slight harmonic enrichment; second harmonic appears faintly
  :ix≈0.5:    noticeable harmonic content; waveform visibly non-sinusoidal
  :ix≈1.0:    rich spectrum; resembles a band-limited sawtooth in character
  :ix≈1.5:    very dense; many harmonics of comparable amplitude
  :ix > 2.0:  chaotic regime; spectrum broadens, approaches noise-like
  :ix >> 2.0: fully chaotic; periodic structure collapses entirely

  The transition from periodic to chaotic behaviour is an intrinsic property
  of feedback nonlinearity — unlike external FM (where any :ix produces a
  periodic signal), feedback FM is a nonlinear dynamical system that can
  exhibit period-doubling and chaos.

  Why the spectrum looks sawtooth-like at moderate :ix
  -----------------------------------------------------
  The feedback loop produces waveforms with all harmonics present (like a
  sawtooth), but with Bessel-like amplitude rolloff.  As :ix increases, the
  rolloff flattens and more energy accumulates in higher harmonics before the
  chaotic regime is reached.  The DX7 exploits this to produce fat, harmonically
  rich tones from a single operator — no external modulator required.

  1-sample delay and digital artefacts
  --------------------------------------
  The `~ _` path introduces exactly one sample of delay.  For moderate :ix
  values at audio sample rates (44.1–48 kHz) this delay is inaudible (≈20–23 μs).
  At very high feedback levels it can cause aliasing-like artefacts as the
  spectral content extends above Nyquist.

  The phasor inside the feedback loop has its own independent `~ _` state
  (its phase accumulator).  Two separate state registers are created by the
  Faust compiler: one for the phasor accumulation, one for the output feedback.
  This is the same nested-state pattern used in the string and comb patches.

  DX7 feedback
  -------------
  The DX7's operator self-feedback is PM feedback (:md=1 in this patch).
  It is limited to 7 discrete levels (0–7 in the DX7 UI), corresponding
  roughly to :ix ≈ 0, 0.08, 0.16, 0.25, 0.4, 0.6, 0.9, in radians.
  All DX7 algorithms that show a feedback arrow use this mechanism on
  operator 1.  At level 7 (maximum), the DX7 produces its characteristic
  bright, buzzy sawtooth-approaching waveform.

  The DX7 also averages the current and previous feedback sample
  (y[n] = sin(phase + 0.5*(x[n-1]+x[n-2]))) to reduce aliasing — a
  simple 2-point average LP on the feedback path, identical to one step
  of Karplus-Strong damping (ex.151–155) applied to a feedback signal.

  Parameters
  ----------
  :fc — carrier frequency in Hz (20–2000; default 220)
  :ix — feedback index; 0=no feedback, ≈1=sawtooth-like, >2=chaotic (0–4; default 0.5)
  :md — FM→PM morph; 0.0=FM feedback, 1.0=PM feedback (0–1; default 1.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained self-modulating FM/PM oscillator)
    :out — feedback FM/PM output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! fmpm-feedback
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :ix {:range [0.0 4.0]     :default 0.5}
            :md {:range [0.0 1.0]     :default 1.0}}}
  (let [fc  (param :fc)
        ix  (param :ix)
        md  (param :md)
        out (faust "fmfb ~ _\n  with { fmfb(x) = sin(2.0*ma.PI*os.phasor(1,%fc+(1.0-%md)*%ix*%fc*x)+%md*%ix*x); }"
                   {:fc fc :ix ix :md md})]
    (output :out out)))
