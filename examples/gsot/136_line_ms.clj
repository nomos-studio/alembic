; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.136-line-ms
  "GSOT p.196 — go.line.ms (Chapter 6).

  'Lag Generator — Millisecond-Parameterized One-Pole Lag'
  ---------------------------------------------------------
  Usability wrapper over go.line.samples (ex.135) that converts a millisecond
  time constant to a sample count before applying the one-pole IIR recurrence:

      N   = max(1, ms × SR / 1000)      (guard prevents division by zero)
      out[n] = out[n−1] + (in[n] − out[n−1]) / N

  With `:ms 1000` at SR=44100: N = 44100, a = 1 − 1/44100 ≈ 0.99998.
  The output reaches ~63% of a step target in exactly 1000 ms (1/e time constant).

  Relationship to go.line.samples (ex.135)
  ------------------------------------------
      go.line.samples — N given directly as a sample count
      go.line.ms      — N derived as ms × SR / 1000

  These are the same filter at any fixed sample rate; go.line.ms is sample-rate
  independent (10 ms sounds the same at 44.1 kHz or 96 kHz).

  Relationship to go.slewlimit.ms (ex.134)
  ------------------------------------------
  Both are parameterised in ms.  The difference is the SHAPE of the response:

      go.slewlimit.ms  (ex.134)  linear ramp — constant speed, reaches target
      go.line.ms       (ex.136)  exponential lag — fast initially, slows at target

  go.slewlimit.ms traverses the full signal range in exactly `:ms` milliseconds.
  go.line.ms reaches 63% of a target step in `:ms` milliseconds; never finishes.

  Common use cases
  -----------------
  Pitch portamento where the ear expects an exponential approach (not a linear slide):
      player releases one note, next note target; output glides to it logarithmically.

  Automation smoothing:
      DAW parameter jumps at block boundaries; lag at ~5–20 ms removes zipper noise.

  CV portamento from MIDI:
      MIDI note number (0–127) → VCO v/oct; 10–50 ms lag softens retriggering.

  Faust computation
  ------------------
  Step 1 — compute N, guarded:
      nn = max(1.0, %ms × ma.SR / 1000.0)

  Step 2 — one-pole recurrence:
      out = (_ + (%in − _) / %nn) ~ _

  The max(1.0, ...) guard clips N to at least 1 sample, preventing division by zero
  when ms → 0, and also caps per-sample step to ≤ 1.0 (instant tracking at ms=0).

  At ms → ∞: N → ∞, a = 1 − 1/N → 1, output frozen — never reaches input.
  At ms → 0: N = 1, a = 0 — output equals input each sample (instant).

  Parameters
  ----------
  :ms — lag time constant in milliseconds (0.1–5000; default 10)

  Audio inputs / Outputs
  ----------------------
  in: signal to lag  →  :out: lag-smoothed output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! line-ms
  {:params {:ms {:range [0.1 5000.0] :default 10.0}}}
  (let [in  (audio-in)
        ms  (param :ms)
        nn  (faust "max(1.0,%ms*ma.SR/1000.0)" {:ms ms})
        out (faust "(_+(%in-_)/%nn)~_" {:in in :nn nn})]
    (output :out out)))
