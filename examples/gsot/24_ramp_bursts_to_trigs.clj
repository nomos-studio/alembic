; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.24-ramp-bursts-to-trigs
  "GSOT p.54 — go.ramp_bursts2trigs: trigger pulses from a burst ramp.

  go.ramp_bursts2trigs.maxpat
  ----------------------------
  Extracts N trigger pulses from a burst ramp produced by ramp_bursts
  (example 23).  Applies the go.ramp2trig algorithm (example 19) directly
  to the burst output signal.

  Signal flow:

      [in 1 burst_ramp]
            |
          delta
            |
          abs
            |
          > 0.5
            |
      [out 1 trig]

  This is identical to go.ramp2trig (example 19) — the same `abs(delta) > 0.5`
  detector works on any [0,1) ramp whether it is a simple cyclic ramp or a
  burst ramp.

  Why this works on burst ramps
  ------------------------------
  In a burst ramp, the signal is:
  - During active window: N successive [0,1) ramps — each wrap-around
    produces delta ≈ -1, abs(delta) ≈ 1 > 0.5 → trigger fires.  N firings.
  - At the window boundary (ramp = width): burst snaps from its current
    value to 0 (because active gate closes).  The magnitude of this step
    is at most 1 (if the last burst was near completion).  However the step
    direction and magnitude depend on timing — it may or may not cross 0.5.

  The book notes this edge case: the window-close event may produce an extra
  trigger.  go.ramp_bursts_shaped (example 25) addresses this by giving the
  last burst a near-zero amplitude, making any spurious end-of-window trigger
  musically inaudible.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n4
        with {
          n1 = (n0 - n0');
          n2 = abs(n1);
          n3 = 0.5;
          n4 = (float(n2 > n3));
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! ramp-bursts-to-trigs
  {}
  (let [burst (audio-in)
        cmp   (comparator (abs (delta burst)) (const 0.5))]
    (output (:out cmp))))
