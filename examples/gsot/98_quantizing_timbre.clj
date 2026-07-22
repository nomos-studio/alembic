; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.98-quantizing-timbre
  "GSOT pp.157-158 — quantizing-timbre.maxpat (Chapter 5).

  'Quantization as a timbral shape'
  -----------------------------------
  Applies the Euclidean inverse digitized ratio as a WAVEFORM WAVESHAPER rather
  than a pitch converter.  The same formula as quantizing-pitch (example 96) now
  takes a continuous phasor [0,1) as input and produces an audio signal.

  The key substitution
  --------------------
  In examples 96-97 the input is a discrete integer degree index.  Here:

      degree = phasor × N

  The phasor [0,1) unfolds N scale degrees continuously per cycle.  Applying the
  inverse digitized ratio gives:

      ratio    = 12 × degree / N = 12 × phasor      (independent of N!)
      semi_q   = floor(ratio)                        (Euclidean staircase)
      semi_out = semi_q + smooth × (ratio − semi_q)  (same lerp as ex.97)

  The ratio is always 12 × phasor — only the STEP POSITIONS (the floor values)
  depend on N.  This is the timbral shape: which of the 12 possible staircase
  levels are visited, and when, in each cycle.

  Output normalisation to [-1, 1]:

      out = semi_out / 6 − 1

  Euclidean waveform shapes by N
  -------------------------------
  N=1  → DC (-1.0) — no timbre
  N=2  → 2-step: −1.0, 0.0              Square-wave approximation
  N=3  → 3 steps: −1.0, −0.33, 0.33     Augmented / major 3rds spacing
  N=4  → 4 steps: −1.0, −0.5, 0.0, 0.5  Diminished (equal minor 3rds)
  N=5  → 5 non-uniform steps             Pentatonic timbral character
  N=6  → 6 equal steps                   Whole-tone (uniform quantisation)
  N=7  → 7 non-uniform steps             Diatonic timbral character
  N=8  → 8 non-uniform steps             Octatonic character

  For N=6 (whole tone) the 6 steps are equally spaced, so this produces the
  same waveform as 6-level uniform quantisation of a sawtooth.  For all other
  N the steps are non-uniformly spaced (maximally even, not equal), giving each
  value its own harmonic signature.

  Smooth parameter
  ----------------
  smooth=0.0 → hard Euclidean staircase; each step is flat, harmonic content
               determined purely by the step level positions.
  smooth=1.0 → linear sawtooth; all N values collapse to the same waveform
               (ratio = 12 × phasor, N cancels out).  N has no effect.
  smooth ∈ (0,1) → blend: retains step character while rounding the edges.

  This is the timbral dual of quantizing-pitch-smoothed (example 97): where
  smooth=0 in ex.97 gives the discrete pitch set and smooth=1 gives continuous
  pitch interpolation, here smooth=0 gives the stepped waveform and smooth=1
  gives a linear sawtooth.

  Connection to pitch quantization
  ---------------------------------
  Feeding the phasor from an oscillator through this patch and into the
  oscillator's FM or waveshaping input couples the pitch-space structure directly
  to timbre.  Same :beats parameter controls both the pitch set and the waveform
  shape — the harmonic content of the output reflects the scale's interval
  structure.

  Parameters
  ----------
  :beats  — N: scale family / staircase steps per cycle (1–8; default 6)
  :smooth — 0.0=hard Euclidean steps → 1.0=linear sawtooth (default 0.0)

  Audio inputs
  ------------
  audio-in 0: phasor — normalised ramp [0,1) at audio rate

  Outputs
  -------
  :out — audio waveform [-1, 1); Euclidean staircase at smooth=0, sawtooth at smooth=1

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_out
        with {
          n_bt  = hslider(\"beats\",  6.0, 1.0, 8.0,  1.0);
          n_sm  = hslider(\"smooth\", 0.0, 0.0, 1.0, 0.01);
          n_dg  = n0*n_bt;
          n_qr  = 12.0*n_dg/max(1.0,n_bt);
          n_sq  = floor(n_qr);
          n_out = (n_sq+n_sm*(n_qr-n_sq))/6.0-1.0;
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! quantizing-timbre
  {:params {:beats  {:range [1.0 8.0] :default 6.0}
            :smooth {:range [0.0 1.0] :default 0.0}}}
  (let [phasor (audio-in)
        beats  (param :beats)
        smooth (param :smooth)
        ; Map phasor to continuous degree [0,N); ratio cancels N → 12*phasor
        degree (faust "%ph*%bt" {:ph phasor :bt beats})
        ratio  (faust "12.0*%dg/max(1.0,%bt)" {:dg degree :bt beats})
        semi-q (faust "floor(%qr)" {:qr ratio})
        ; Smooth-step blend: lerp from Euclidean staircase to linear ramp
        ; Normalize [0,12) → [-1,1): (value/6) - 1
        out    (faust "(%sq+%sm*(%qr-%sq))/6.0-1.0"
                      {:sq semi-q :sm smooth :qr ratio})]
    (output :out out)))
