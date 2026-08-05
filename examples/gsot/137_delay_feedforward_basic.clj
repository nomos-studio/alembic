; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.137-delay-feedforward-basic
  "GSOT pp.197-199 — delay_feedforward_basic.maxpat (Chapter 7).

  'Tape Delay — Feedforward, No Feedback'
  -----------------------------------------
  A circular-buffer delay line with no feedback path.  The output mixes
  the undelayed dry signal with a single delayed copy (the 'wet' tap):

      y[n] = (1 − mix) · x[n]  +  mix · x[n − D]

  where D = ms × SR / 1000 (converted to integer samples).

  'Feedforward' in the FIR / filter-theory sense means the signal path
  flows only forward through the delay: there is no path from the delay
  output back to its input.  Compare to the feedback variants (Chapter 7)
  where the delayed output is recirculated, creating echo trails.

  Comb filter interpretation
  ---------------------------
  Adding a delayed copy of a signal to itself creates a COMB FILTER —
  frequency response is:

      |H(e^jω)| = |(1−mix) + mix·e^{−jωD}|

  At mix = 0.5, D samples:
    - Peaks (constructive) at frequencies k/D Hz (multiples of 1/D)
    - Notches (destructive) at (k + 0.5)/D Hz

  This is the central example of Chapter 7: delay introduces phase, and
  phase differences between a signal and its delayed copy create the
  characteristic 'comb' teeth in the magnitude spectrum.

  The delay time in terms of musical pitch
  -----------------------------------------
  A D-sample delay at sample rate SR gives a fundamental comb period of
  SR/D Hz.  For D ≈ 2ms at 44.1 kHz: D = 88 samples, period = 500 Hz
  (B4 area).  Longer delays push the comb fundamental lower.

  Short delay (0–10 ms):    chorus / flanging zone; comb audible in timbre
  Medium delay (10–100 ms): slapback / room-tail zone
  Long delay (100 ms+):     rhythmic echo territory

  Implementation
  ---------------
  Uses Faust's `de.delay(maxDel, del, x)` from delays.lib:
    - maxDel = int(ma.SR × 5.0)  — 5-second buffer, exact at any sample rate
    - del    = int(max(0, ms × SR / 1000)) — guard prevents negative index
    - x      = %in

  The mix is applied post-delay:
      (1.0 − mix) × dry  +  mix × wet

  Parameters
  ----------
  :ms — delay time in milliseconds (0–5000; default 250)
  :mx — wet/dry mix; 0.0 = dry only, 1.0 = wet only (default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: dry/wet mixed delayed output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! delay-feedforward-basic
  {:params {:ms {:range [0.0 5000.0] :default 250.0}
            :mx {:range [0.0 1.0]    :default 0.5}}}
  (let [in  (audio-in)
        ms  (param :ms)
        mx  (param :mx)
        dly (faust "de.delay(int(ma.SR*5.0),int(max(0.0,%{ms}*ma.SR/1000.0)),%{in})"
                   {:ms ms :in in})
        out (faust "(1.0-%{mx})*%{in}+%{mx}*%{dl}" {:mx mx :in in :dl dly})]
    (output :out out)))
