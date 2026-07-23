; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.133-slewlimit2
  "GSOT pp.193-194 — go.slewlimit2 (Chapter 6).

  'Slew Limiting — Asymmetric Rise/Fall Rates'
  -----------------------------------------------
  The asymmetric slew limiter allows independent rate control for upward
  (rising) and downward (falling) transitions:

      delta[n] = in[n] − out[n−1]
      out[n]   = out[n−1] + (delta[n] > 0 ? min(delta[n], up)
                                           : max(delta[n], −dn))

  When the input rises, the output ramps up at most `:up` units per sample.
  When the input falls, the output ramps down at most `:dn` units per sample.
  When delta = 0, the output holds exactly.

  Applications of asymmetric slew
  ---------------------------------
  Attack/release envelope following:
      fast attack (high :up) + slow release (low :dn) → peak follower
      slow attack (low :up) + fast release (high :dn) → valley follower

  Portamento with directional control:
      slide up slowly (low :up) + drop immediately (high :dn)
      → simulates guitar bends that snap back quickly

  CV pre-conditioning:
      Slew gate pulses (fast gates → CV ramp triggers) differently in each
      direction — rising edge is slewed, falling edge is not (high :dn = 1.0).

  Faust implementation
  ---------------------
      out = (_ + select2(%in>_, max(−%dn, %in−_), min(%up, %in−_))) ~ _

  select2(condition, on_false, on_true):
      in > _  (rising):  on_true  = min(%up, delta) — cap at rise rate
      in ≤ _  (falling): on_false = max(−%dn, delta) — floor at fall rate

  The result is added to _ (previous output) and fed back via ~_.

  Parameters
  ----------
  :up — maximum rise per sample (0.0–1.0; default 0.01)
  :dn — maximum fall per sample (0.0–1.0; default 0.01)

  Audio inputs / Outputs
  ----------------------
  in: signal to rate-limit  →  :out: asymmetrically slew-limited output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! slewlimit2
  {:params {:up {:range [0.0 1.0] :default 0.01}
            :dn {:range [0.0 1.0] :default 0.01}}}
  (let [in  (audio-in)
        up  (param :up)
        dn  (param :dn)
        out (faust "(_+select2(%in>_,max(-%dn,%in-_),min(%up,%in-_)))~_"
                   {:in in :up up :dn dn})]
    (output :out out)))
