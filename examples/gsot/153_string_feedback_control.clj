; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.153-string-feedback-control
  "GSOT p.221 — string_feedback_control.maxpat (Chapter 8).

  'Strings — Variable Feedback Filter for Independent Brightness Control'
  -----------------------------------------------------------------------
  ex.151/152 fix the averaging filter at the K-S canonical 0.5 coefficient,
  coupling brightness to the number of loop traversals (and thus to pitch and
  sustain).  This patch replaces the fixed average with a variable first-order
  FIR whose coefficient :dp separates brightness from decay rate:

      lp[n] = (1 − :dp)·y[n−1]  +  :dp·y[n−2]

  This is a two-point weighted sum; its frequency response is:
      H(ω) = (1−dp) + dp·e^{-jω}

  Frequency response of the feedback filter
  -------------------------------------------
  :dp = 0.0  →  H = 1 (flat; no HF rolloff — bright, like a harpsichord)
  :dp = 0.5  →  H = 0.5·(1+z^{-1})  (standard K-S averaging; -∞ dB at Nyquist)
  :dp = 0.99 →  H ≈ z^{-1} (near-pure delay; very dark, almost no decay)

  The Nyquist gain is 1 − 2·:dp, which goes to zero at dp=0.5 and negative
  beyond — negative Nyquist gain flips the sign of odd harmonics, producing
  a clarinet-like spectral shape (odd harmonics only) when dp > 0.5.

  DC gain is always 1.0 regardless of :dp; the fundamental decays at the rate
  set by :gn alone.

  Practical use
  --------------
  Plucked string (bright attack, dark decay): start :dp at 0, sweep to 0.5.
  Bowed string (sustained): :gn ≈ 1, :dp ≈ 0.5.
  Struck metal (inharmonic with ex.150 or harmonic with this): :dp < 0.3.
  Bass string (very dark): :dp 0.6–0.8.

  Pitch correction
  -----------------
  Uses fractional delay from ex.152 (de.fdelay, D = SR/:hz − 1.5), but the
  group delay of the variable LP is now (dp) samples rather than the fixed 0.5.
  The corrected total: D_corrected = SR/:hz − 1 − dp.

      dl = max(0.0, ma.SR/:hz − 1.0 − :dp)

  This keeps pitch accurate across the full :dp range.

  Parameters
  ----------
  :hz — fundamental frequency in Hz (20–2000; default 220)
  :gn — loop gain; controls overall decay rate (0–0.999; default 0.99)
  :dp — feedback filter coefficient; 0=bright/flat, 0.5=K-S canonical, 0.99=dark
        (0–0.99; default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: excitation signal  →  :out: string resonator with variable tone output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! string-feedback-control
  {:params {:hz {:range [20.0 2000.0] :default 220.0}
            :gn {:range [0.0 0.999]   :default 0.99}
            :dp {:range [0.0 0.99]    :default 0.5}}}
  (let [in  (audio-in)
        hz  (param :hz)
        gn  (param :gn)
        dp  (param :dp)
        dl  (faust "max(0.0,ma.SR/%{hz}-1.0-%{dp})" {:hz hz :dp dp})
        out (faust "strfc_loop ~ _\n  with { strfc_loop(s) = %{in}+%{gn}*de.fdelay(int(ma.SR*5.0),%{dl},(1.0-%{dp})*s+%{dp}*s@1); }"
                   {:in in :gn gn :dp dp :dl dl})]
    (output :out out)))
