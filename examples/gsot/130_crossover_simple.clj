; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.130-crossover-simple
  "GSOT pp.191-192 — crossover_simple.maxpat (Chapter 6).

  'Simple Crossover — One-Pole Complement Pair'
  -----------------------------------------------
  The simplest possible crossover divides a signal into two perfectly
  complementary bands that sum exactly back to the original:

      lp[n]   = (1−a)·x[n] + a·lp[n−1]    (one-pole lowpass; ex.103)
      hp[n]   = x[n] − lp[n]               (exact complement)
      lp + hp = x[n]  ∀n                   (perfect reconstruction)

  The coefficient a = exp(−2π·fc/SR) is the same backward-Euler formula
  used in go.onepole.basic_hz (example 103).

  Complement identity
  --------------------
  Because lp[n] = (1−a)·x[n] + a·lp[n−1] is a linear operation, and
  hp = x − lp, we have:

      hp[n] = x[n] − (1−a)·x[n] − a·lp[n−1]
            = a·x[n] − a·lp[n−1]
            = a·(x[n] − lp[n−1])

  This is itself a one-pole highpass: hp passes the high-frequency change
  (x[n] − lp[n−1]) attenuated by a.  The exact LP + HP = x identity holds
  at every sample, making the simple crossover lossless — ideal for splits
  where the two bands are later recombined (multiband processing).

  Slope and phase
  ----------------
  First-order filters roll off at ±6 dB/oct.  The slope is gentle but
  sufficient for woofer/tweeter splits in studio use.  The LP and HP outputs
  share a common allpass phase response — their sum is always in phase with
  the original, which is why LP + HP = x exactly.

  Compare to crossover.maxpat (example 131)
  -------------------------------------------
  example 130 (this patch)   1st-order; LP + HP = x; gentle slope
  example 131  crossover.maxpat   2nd-order SVF; steeper slope; LP + HP ≈ flat

  Outputs
  -------
  :out0  (ch 4) — lp: low-frequency band (6 dB/oct roll-off above fc)
  :out1  (ch 5) — hp: high-frequency band (6 dB/oct roll-off below fc)

  Parameters
  ----------
  :hz — crossover frequency in Hz (20–20000; default 1000)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! crossover-simple
  {:params {:hz {:range [20.0 20000.0] :default 1000.0}}}
  (let [in  (audio-in)
        hz  (param :hz)
        ; a = exp(-2π·fc/SR) — backward Euler pole (same as go.onepole.basic_hz)
        a   (faust "exp(-2.0*ma.PI*%hz/ma.SR)" {:hz hz})
        ; LP: one-pole lowpass feedback
        lp  (faust "((1.0-%al)*%in+%al*_)~_" {:al a :in in})
        ; HP: exact complement — sums to dry at every sample
        hp  (faust "%in-%lp" {:in in :lp lp})]
    (output :out0 lp)
    (output :out1 hp)))
