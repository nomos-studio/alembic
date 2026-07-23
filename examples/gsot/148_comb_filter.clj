; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.148-comb-filter
  "GSOT p.215 — comb_filter.maxpat (Chapter 8).

  'Filtering with Delay: The Feedforward Comb Filter'
  ----------------------------------------------------
  A delay line mixed back with the dry signal creates a comb filter — so named
  because its frequency response looks like the teeth of a comb: evenly spaced
  peaks and notches across the spectrum.

  Transfer function: H(z) = 1 + g·z^{-D}

  Frequency response
  -------------------
  The magnitude is | 1 + g·exp(-j·ω·D) |.  Constructive and destructive
  interference between the direct and delayed copies creates:

    g > 0 (positive gain):
      Notches at f = (2k+1)/(2D) Hz  — odd multiples of 1/(2D)
      Peaks  at f = k/D Hz           — multiples of 1/D

    g < 0 (negative gain):
      Peaks  at f = (2k+1)/(2D) Hz  — odd multiples of 1/(2D)
      Notches at f = k/D Hz

    |g| = 1: complete cancellation at notch frequencies (−∞ dB).

  The spacing between adjacent peaks is always 1/D Hz regardless of g.
  At D = 5 ms: peaks every 200 Hz.  At D = 1 ms: peaks every 1000 Hz.

  Comb as delay-based EQ
  -----------------------
  The comb filter is a delay-based equalizer: the delay time sets the
  spacing of the teeth, and the gain sets their depth and polarity.
  Positive gain brightens even harmonics; negative gain brightens odd.

  This is the FIR (feedforward) variant — finite impulse response, always
  stable for any g.  See ex.150 for the IIR (feedback) variant whose teeth
  are sharper but requires |g| < 1 for stability.

  Parameters
  ----------
  :ms — delay time in milliseconds; sets comb tooth spacing = 1000/:ms Hz (0.1–100; default 5)
  :gn — comb gain; positive = notches at odd multiples, negative = notches at even (−1–1; default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: comb-filtered output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! comb-filter
  {:params {:ms {:range [0.1 100.0] :default 5.0}
            :gn {:range [-1.0 1.0]  :default 0.5}}}
  (let [in  (audio-in)
        ms  (param :ms)
        gn  (param :gn)
        dly (faust "de.delay(int(ma.SR*5.0),int(max(0.0,%ms*ma.SR/1000.0)),%in)"
                   {:ms ms :in in})
        out (faust "%in+%gn*%dl" {:in in :gn gn :dl dly})]
    (output :out out)))
