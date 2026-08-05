; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.57-random-smoothed
  "GSOT p.94 — random_smoothed.maxpat.

  'Smooth stepped random' (Chapter 4) — linear interpolation between random steps
  ---------------------------------------------------------------------------------
  Extends random_steps (example 56) with phasor-phase-driven linear interpolation.
  Instead of snapping to a new random value at each trigger, the output smoothly
  glides from the previous target to the new one over the step duration.

  Two audio inputs:
    trig  — trigger pulse (e.g. from go.ramp2trig, example 14)
    phase — phasor phase [0, 1] within the current step (same phasor as trig source)

  Signal flow
  -----------
  1. Scale white noise to [lo, hi] (same as random_range, example 55).
  2. new-val = track-hold(scaled, trig)   — latch a new target on each trigger.
  3. prev-val = track-hold(new-val@1, trig)
        new-val@1 is new-val delayed by 1 sample.  Between triggers new-val is
        constant, so new-val@1 = old new-val.  When trig fires, prev-val latches
        the value new-val held BEFORE this trigger = previous target.
  4. out = prev-val + phase × (new-val − prev-val)
        Linear interpolation from prev to new over [0,1] phase.

  At trigger moment: phase resets to 0  →  out = prev (no jump).
  End of step:       phase reaches 1    →  out = new  (arrives at target).
  Next trigger:      new prev-val = old new-val — seamless continuation.

  This differs from portamento (example 42), which has a fixed exponential time
  constant.  Here the interpolation duration matches the step duration exactly,
  so the output always arrives at the target value by the next trigger regardless
  of tempo.

  Emitted Faust DSP (abbreviated):
      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1) = n7
        with {
          n2  = hslider(\"lo\", -1.0, -1.0, 1.0, 0.0002);
          n3  = hslider(\"hi\",  1.0, -1.0, 1.0, 0.0002);
          n4  = n2+(n3-n2)*0.5*(no.noise+1.0);
          n5  = (select2(n0>0.5,_,n4)~_);             // new-val
          n6  = n5@1;                                  // new-val delayed 1 sample
          n7  = (select2(n0>0.5,_,n6)~_);             // prev-val
          n8  = n7+n1*(n5-n7);                        // linear interp
        };
      process = alembic_dsp;

  n0 = trig  (audio-in 0)
  n1 = phase (audio-in 1)
  n2 = lo param
  n3 = hi param
  n4 = scaled noise
  n5 = new-val (track-hold of scaled)
  n6 = new-val@1 (1-sample delay)
  n7 = prev-val (track-hold of n6)
  n8 = output (linear interpolation)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! random-smoothed
  {:params {:lo {:range [-1.0 1.0] :default -1.0}
            :hi {:range [-1.0 1.0] :default  1.0}}}
  (let [trig     (audio-in)
        phase    (audio-in)
        n        (faust "no.noise" {})
        lo       (param :lo)
        hi       (param :hi)
        scaled   (faust "%{lo}+(%{hi}-%{lo})*0.5*(%{nn}+1.0)" {:lo lo :hi hi :nn n})
        new-val  (track-hold scaled trig)
        prev-val (track-hold (faust "%{nv}@1" {:nv new-val}) trig)
        out      (faust "%{pv}+%{ph}*(%{nv}-%{pv})" {:pv prev-val :ph phase :nv new-val})]
    (output out)))
