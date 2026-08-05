; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.135-line-samples
  "GSOT p.196 — go.line.samples (Chapter 6).

  'Lag Generator — Sample-Parameterized One-Pole Lag'
  ------------------------------------------------------
  The lag generator follows the input with a one-pole IIR whose time constant
  is expressed directly in samples rather than as a frequency or exp coefficient:

      out[n] = out[n−1] + (in[n] − out[n−1]) / N

  This is equivalent to a one-pole lowpass with pole coefficient a = 1 − 1/N:

      out[n] = (1 − 1/N) · out[n−1] + (1/N) · in[n]

  At large N, the initial response to a step looks like a straight line — hence
  'drawing a line.'  The actual trajectory is exponential: it reaches ~63% of the
  target after N samples (the 1/e time constant), not 100%.

  Relation to other one-pole parameterisations
  ----------------------------------------------
  example 100  go.onepole.basic     coefficient a ∈ [0,1] directly
  example 103  go.onepole.basic_hz  a = exp(−2π·fc/SR); frequency domain
  example 135  go.line.samples      a = 1 − 1/N; time domain, N in samples
  example 136  go.line.ms           a = 1 − 1000/(ms·SR); time domain, ms

  At large N: 1 − 1/N ≈ exp(−1/N), so the sample-count and exp parameterisations
  agree closely.  At small N (fast lag, high cutoff) they diverge.

  The 'line' interpretation
  -------------------------
  For small signals or large N, the first portion of the exponential rise visually
  approximates a straight line — the eye reads it as a ramp even though the
  underlying recurrence is geometric.  GSOT calls this 'drawing a line.'

  At N = 1: a = 0 — output equals input every sample (no lag, instant tracking).
  At N → ∞: a → 1 — output never reaches input (fully smoothed / frozen).
  At N = 44100 at 44.1 kHz: 1-second lag (63% response time).

  Faust recursion
  ----------------
      out = (_ + (%in − _) / %sa) ~ _

  The guard max(1, N) in the ms variant is not needed here because the parameter
  range floor (1.0) already prevents division by zero.

  Compare to go.slewlimit (example 132)
  ----------------------------------------
  go.slewlimit constrains the absolute per-sample RATE of change — it moves at
  constant speed and REACHES the target exactly in finite time.

  go.line.samples constrains the per-sample FRACTION of remaining distance — it
  moves faster at first and slows as it nears the target, never quite arriving.

  Parameters
  ----------
  :samp — lag time constant in samples (1–192000; default 100)

  Audio inputs / Outputs
  ----------------------
  in: signal to lag  →  :out: lag-smoothed output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! line-samples
  {:params {:samp {:range [1.0 192000.0] :default 100.0}}}
  (let [in   (audio-in)
        samp (param :samp)
        out  (faust "(_+(%{in}-_)/%{sa})~_" {:in in :sa samp})]
    (output :out out)))
