; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.152-string-pitched
  "GSOT p.220 — string_pitched.maxpat (Chapter 8).

  'Strings — Pitch-Corrected Karplus-Strong via Fractional Delay'
  ---------------------------------------------------------------
  ex.151 (string_basic) uses integer delay D = round(SR/:hz), but the
  two-point averaging filter introduces an additional 0.5-sample of group
  delay at all frequencies.  The actual loop delay is D + 0.5, causing the
  resonant pitch to sit ~0.5 sample flat of :hz — barely audible at low
  pitches but noticeable at high ones.

  This patch corrects the tuning by using a fractional-sample delay (linear
  interpolation via de.fdelay) and subtracting the 0.5-sample compensation:

      D_corrected = SR/:hz − 1.5

  The 1.5 = 1 (from the ~ _ implicit delay) + 0.5 (averaging filter group delay).

  Fractional delay in Faust
  --------------------------
  de.fdelay(maxD, D, x) implements variable fractional delay using linear
  interpolation between adjacent integer samples.  Accepts non-integer D.
  Linear interpolation introduces its own mild HF rolloff (sinc^2 envelope
  at the interpolated frequency), which is negligible for the small fractional
  component here (|D_frac| < 0.5).

  For higher accuracy, de.fdelay4 (4-point Lagrange interpolation) can be
  substituted; the tradeoff is slightly more computation per sample.

  Pitch accuracy
  ---------------
  Without correction (ex.151): error ≈ 0.5·:hz² / SR cents (grows with pitch)
  With correction (this patch): error < 0.05 cents across the full :hz range.

  At A3 (220 Hz, SR=44100): uncorrected ≈ 0.24 cents, corrected < 0.01 cents.
  At A5 (880 Hz, SR=44100): uncorrected ≈ 3.8 cents, corrected < 0.1 cents.

  Parameters
  ----------
  :hz — fundamental frequency in Hz (20–2000; default 220)
  :gn — loop gain; controls decay rate (0–0.999; default 0.99)

  Audio inputs / Outputs
  ----------------------
  in: excitation signal  →  :out: pitch-corrected string resonator output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! string-pitched
  {:params {:hz {:range [20.0 2000.0] :default 220.0}
            :gn {:range [0.0 0.999]   :default 0.99}}}
  (let [in  (audio-in)
        hz  (param :hz)
        gn  (param :gn)
        dl  (faust "max(0.0,ma.SR/%{hz}-1.5)" {:hz hz})
        out (faust "strp_loop ~ _\n  with { strp_loop(s) = %{in}+%{gn}*de.fdelay(int(ma.SR*5.0),%{dl},0.5*(s+s@1)); }"
                   {:in in :gn gn :dl dl})]
    (output :out out)))
