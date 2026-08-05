; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.138-delay-basic-feedback
  "GSOT p.200 — delay_basic_feedback.maxpat (Chapter 7).

  'Tape Delay — Feedback (IIR Echo Loop)'
  -----------------------------------------
  Adds a feedback path to the feedforward delay (ex.137): the delayed output
  is multiplied by a feedback coefficient and mixed back into the delay input.

      w[n] = x[n] + fb · y[n−1]        (write signal into buffer)
      y[n] = w[n − D]                  (read D samples later)
           = x[n−D] + fb · y[n−D−1]   (substituting)

  This produces a geometrically decaying series of echoes:

      y[n] ≈ x[n−D] + fb·x[n−2D] + fb²·x[n−3D] + …

  The extra −1 sample in the exponent comes from Faust's `~` operator which
  inserts an implicit 1-sample delay in any feedback loop.  At audio delay
  times (D ≫ 1) this offset is inaudible and is the conventional Faust idiom.

  Stability condition
  --------------------
  The IIR recurrence is stable only when |fb| < 1.  At |fb| = 1 the echoes
  sustain indefinitely (infinite reverb / freeze).  At |fb| > 1 the output
  grows without bound (avoid in practice — clip the input or guard fb).

  The parameter range [0, 0.99] keeps the loop stable with a wide practical range.
  A value of 0 reduces to the feedforward delay (ex.137 with fb=0).

  Frequency response
  -------------------
  The IIR comb filter transfer function (with mix = 1, no dry blend):

      H(z) = z^{−D} / (1 − fb · z^{−D})

  Magnitude response has peaks at f = k·SR/D (same comb positions as the FIR
  version) but the peaks are SHARPENED — height = 1/(1 − fb) rather than flat.
  Higher feedback → narrower, taller resonant peaks.  This is also the
  building block for Schroeder reverb (Chapter 7, later examples).

  Faust recursion
  ----------------
  Uses the named-function `~ _` idiom (same as SVF, ex.129):

      fdl_loop(fbs) = de.delay(maxD, D, in + fb·fbs)
      fdl = fdl_loop ~ _

  `fbs` is the feedback signal (previous output of fdl_loop, delayed 1 sample
  by Faust's `~`).  The result `fdl` is the D-sample-delayed wet signal.

  Output mix:
      out = (1 − mx)·in  +  mx·fdl

  Parameters
  ----------
  :ms — delay time in milliseconds (0–5000; default 250)
  :fb — feedback coefficient (0–0.99; default 0.5)
  :mx — wet/dry mix; 0.0 = dry only, 1.0 = wet only (default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: dry/wet mixed IIR delay output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! delay-basic-feedback
  {:params {:ms {:range [0.0 5000.0] :default 250.0}
            :fb {:range [0.0 0.99]   :default 0.5}
            :mx {:range [0.0 1.0]    :default 0.5}}}
  (let [in  (audio-in)
        ms  (param :ms)
        fb  (param :fb)
        mx  (param :mx)
        fdl (faust "fdl_loop ~ _\n  with { fdl_loop(fbs) = de.delay(int(ma.SR*5.0),int(max(0.0,%{ms}*ma.SR/1000.0)),%{in}+%{fb}*fbs); }"
                   {:ms ms :in in :fb fb})
        out (faust "(1.0-%{mx})*%{in}+%{mx}*%{fd}" {:mx mx :in in :fd fdl})]
    (output :out out)))
