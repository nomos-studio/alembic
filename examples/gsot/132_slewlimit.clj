; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.132-slewlimit
  "GSOT pp.193-194 — go.slewlimit (Chapter 6).

  'Slew Limiting — Symmetric Rate Limiting'
  -------------------------------------------
  A slew limiter constrains the maximum rate of change of a signal.
  Instead of tracking the input instantly, the output moves toward the target
  at most `:slew` units per sample in either direction:

      delta[n] = in[n] − out[n−1]
      out[n]   = out[n−1] + clamp(delta[n], −slew, +slew)

  This is a LINEAR rate limiter — the output moves at a CONSTANT absolute speed
  toward the target, unlike exponential smoothing (RC filter) which slows as it
  approaches the target.

  Physical analogy
  -----------------
  A slew limiter behaves like a car with a limited acceleration and braking
  force.  No matter how suddenly the destination changes, the output can only
  move at the bounded rate.  Once it reaches the target it stops immediately
  (unlike an RC filter, which asymptotically approaches forever).

  Contrast with exponential smoothing
  -------------------------------------
  Exponential (RC) slew — Alembic :slew op:
      out[n] = in[n]·(1−k) + out[n−1]·k     where k = exp(−1/(τ·SR))

  The RC slew slows down as it nears the target: the last bit of the ramp
  takes infinitely long.  It never reaches the target exactly.

  Linear slew — this patch:
      out[n] = out[n−1] + clamp(in[n] − out[n−1], −slew, +slew)

  The linear slew moves at constant speed and REACHES the target exactly.
  Once it arrives, output == input and the clamp has no effect.

  The :slew parameter
  ---------------------
  :slew is the maximum change per SAMPLE (not per second).  To convert from
  seconds-to-traverse-full-scale to per-sample rate:

      slew_per_sample = full_scale_range / (seconds × SR)

  Examples at SR=44100 for a [−1, 1] signal (range = 2.0):
      0.001  → 2 / (0.001 × 44100) ≈ 45 ms to full range
      0.0001 → ≈ 450 ms to full range
      1.0    → instant (no limiting; follows input sample by sample)

  Faust recursion
  ----------------
  The one-sample feedback is expressed with ~_:

      out = (_ + max(−slew, min(slew, in − _))) ~ _

  _ = out[n−1]; the expression in parens computes out[n].
  Equivalent to clamp(delta, −slew, slew) + prev.

  Compare to go.slewlimit2 (example 133)
  -----------------------------------------
  example 132 (this patch)  symmetric (:slew same for rise and fall)
  example 133  go.slewlimit2  asymmetric (separate :up and :dn rates)

  Parameters
  ----------
  :slew — maximum change per sample (0.0–1.0; default 0.01)

  Audio inputs / Outputs
  ----------------------
  in: signal to rate-limit  →  :out: slew-limited output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! slewlimit
  {:params {:slew {:range [0.0 1.0] :default 0.01}}}
  (let [in   (audio-in)
        slew (param :slew)
        out  (faust "(_+max(-%sl,min(%sl,%in-_)))~_" {:sl slew :in in})]
    (output :out out)))
