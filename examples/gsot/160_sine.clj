; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.160-sine
  "GSOT p.233 — sine.maxpat (Chapter 8: Frequent Modulations).

  'Phase-Explicit Sine Oscillator — the FM/PM Building Block'
  ------------------------------------------------------------
  gen~'s built-in `cycle~` operator hides its phasor: you supply a frequency
  and get a sine, but the accumulated phase is not accessible.  For FM and PM
  synthesis you need that phase explicitly in the signal graph.

  This patch implements the same result using an exposed phasor:

      phasor(fc)  →  × 2π  →  + pm  →  sin  →  output

  In Faust:
      sin(2π × os.phasor(1, fc) + pm)

  The phasor `os.phasor(1, fc)` produces a 0–1 sawtooth ramp at frequency fc.
  Multiplying by 2π converts it to radians (0–2π).  The `pm` term shifts that
  phase by an arbitrary signal before the sin is taken.

  When pm = 0: identical to a plain sine oscillator.
  When pm carries a signal: phase modulation (PM) synthesis.

  The FM variant (see ex.161) feeds the modulator to the *frequency* input
  of the phasor instead of to the phase:
      sin(2π × os.phasor(1, fc + modulator))

  Phase modulation input
  -----------------------
  The audio input to this patch is the raw PM signal in radians.  To drive
  it as a standalone oscillator with no modulation, feed silence or zero.
  To perform PM synthesis, feed the output of another oscillator (scaled by
  a modulation index) into :in.

  Relationship to os.osc(f)
  --------------------------
  `os.osc(f)` in stdfaust.lib is exactly `sin(2π × os.phasor(1, f))` — the
  same as this patch with pm=0.  The distinction is purely structural: here
  the phase input is a named audio port, making modulation explicit in the
  signal graph.

  Parameters
  ----------
  :fc — carrier frequency in Hz (20–8000; default 440)

  Audio inputs / Outputs
  ----------------------
  in: phase modulation signal in radians (0 = no modulation)
    :out — sin(2π × phasor(:fc) + in)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! sine
  {:params {:fc {:range [20.0 8000.0] :default 440.0}}}
  (let [pm  (audio-in)
        fc  (param :fc)
        out (faust "sin(2.0*ma.PI*os.phasor(1,%{fc})+%{pm})" {:fc fc :pm pm})]
    (output :out out)))
