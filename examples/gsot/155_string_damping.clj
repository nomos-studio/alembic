; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.155-string-damping
  "GSOT pp.222-223 — string_damping.maxpat (Chapter 8).

  'Damping — RT60 Decay Time Parameterisation for Karplus-Strong'
  ---------------------------------------------------------------
  In ex.151-154 the loop gain :gn directly sets the per-loop energy loss,
  but its relationship to perceptible decay time depends on both :gn and :hz.
  At :hz=110 the loop runs 110 times per second; at :hz=880 it runs 880 times.
  The same :gn=0.99 decays in ~0.9 s at 110 Hz but only ~0.1 s at 880 Hz.

  This patch replaces :gn with :dc (decay time to −60 dB in milliseconds),
  deriving the loop gain internally using the RT60 formula:

      fb = pow(0.001, period_ms / :dc)
         = pow(0.001, 1000 / (:hz × :dc))

  The factor 0.001 = 10^{-3} corresponds to −60 dB.  After :dc milliseconds
  of loop traversals, the accumulated gain is:
      fb^{:dc / period_ms} = 0.001^{(period_ms/:dc)×(:dc/period_ms)} = 0.001

  The result is a string whose decay time is independent of pitch — the same
  :dc value gives the same perceptual sustain at any :hz.

  Connection to Chapter 7
  ------------------------
  This is the same reparameterisation introduced in ex.139
  (delay_feedback_decaytime.maxpat) for echo effects.  The formula is
  identical; only the context changes from ms-scale echo trails to
  period-scale string resonator loops.

  Frequency-dependent vs. frequency-independent damping
  -------------------------------------------------------
  :dc controls uniform (frequency-independent) damping — all harmonics
  decay at the same rate.  The averaging LP still applies its inherent
  HF damping on top of this, so high harmonics always decay somewhat
  faster than low ones.

  For independent control of tonal character and decay time, combine:
    - :dc (this patch) → sets when the string decays
    - :dp (ex.153)     → sets how bright or dark the decay sounds

  Parameters
  ----------
  :hz — fundamental frequency in Hz (20–2000; default 220)
  :dc — RT60 decay time in milliseconds; time for string to reach −60 dB
        (1–10000; default 1000)

  Audio inputs / Outputs
  ----------------------
  in: excitation signal  →  :out: RT60-damped string resonator output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! string-damping
  {:params {:hz {:range [20.0 2000.0]  :default 220.0}
            :dc {:range [1.0 10000.0]  :default 1000.0}}}
  (let [in  (audio-in)
        hz  (param :hz)
        dc  (param :dc)
        fb  (faust "pow(0.001,1000.0/(%{hz}*max(1.0,%{dc})))" {:hz hz :dc dc})
        dl  (faust "int(max(1.0,ma.SR/%{hz}))" {:hz hz})
        dl1 (faust "max(0,%{dl}-1)" {:dl dl})
        out (faust "strd_loop ~ _\n  with { strd_loop(s) = %{in}+%{fb}*de.delay(int(ma.SR*5.0),%{d1},0.5*(s+s@1)); }"
                   {:in in :fb fb :d1 dl1})]
    (output :out out)))
