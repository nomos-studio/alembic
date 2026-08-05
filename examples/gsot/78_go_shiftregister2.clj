; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.78-go-shiftregister2
  "GSOT pp.127 — go.shiftregister2.gendsp.

  'A shift register canon' (Chapter 5)
  -------------------------------------
  A 2-stage shift register.  On each trigger, the new cv-in value enters
  at stage 0, and the previous stage-0 value shifts to stage 1.

  Signal flow
  -----------
      s0[n] = if trig[n]: cv-in[n]      else s0[n-1]   (track-hold)
      s1[n] = if trig[n]: s0[n-1]       else s1[n-1]   (track-hold on s0@1)

  The `@1` delay on s0 gives s0's value from the immediately preceding
  sample — which, because s0 only changes on trigger events, is exactly
  the value s0 held BEFORE this trigger fired.  So s1 always lags s0 by
  exactly one trigger event.

  After three consecutive triggers with inputs a, b, c (c most recent):
      s0 = c   (current input)
      s1 = b   (previous input)

  Canon use: run both outputs into separate voices (oscillators, filters,
  etc.) at the same clock.  Voice 1 (s0) plays the current note; voice 2
  (s1) plays the note from one step ago.  At tempo T, voice 2 is delayed
  by one beat — a 2-voice canon in time.

  This is the same shift-register pattern used internally in example 58
  (spline-smoothed) to maintain the 6-point Catmull-Rom sliding window.
  See go.shiftregister8 (example 79) for the 8-stage version.

  Audio inputs
  ------------
  audio-in 0: trig  — clock; each rising edge advances the register
  audio-in 1: cv-in — value to shift in at stage 0 on each trigger

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1) = n_s0, n_s1
        with {
          n_s0 = (select2(n0>0.5,_,n1)~_);
          n_s1 = (select2(n0>0.5,_,n_s0@1)~_);
        };
      process = alembic_dsp;

  n0 = trig, n1 = cv-in"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! go-shiftregister2
  {}
  (let [trig  (audio-in)
        cv-in (audio-in)
        s0    (track-hold cv-in trig)
        s1    (track-hold (faust "%{pv}@1" {:pv s0}) trig)]
    (output :s0 s0)
    (output :s1 s1)))
