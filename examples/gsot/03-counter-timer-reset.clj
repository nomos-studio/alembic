; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.03-counter-timer-reset
  "GSOT p.21 — counter_timer_reset: pausable and rewindable sample counter.

  Extends the simple timer (p.20) with two control inputs: a play gate that
  pauses accumulation when zero, and a rewind trigger that resets the count.

  In gen~:

      [in 1 play]   [in 2 rewind]
            |              |
       [switch 0]          |
            |              |
           [+]  ←── [history] ←── reset when rewind > 0
            |              ↑
            └──────────────┘
            |
         [out 1 count]
            |
         [/ samplerate]
            |
         [out 2 seconds]

  The `switch 0` passes 0 (rather than 1) into the accumulator when play=0,
  freezing the count without resetting it.  `rewind` fires into the accumulator
  reset inlet: any nonzero value clears the running total to zero.

  In Alembic, gating the increment is composition: multiply the increment
  by the play signal before passing to `accum`.  The reset inlet handles
  the rewind directly.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1) = n3, n4
        with {
          n2 = (select2(n1 > 0.5, 0.0, _ + n0) ~ _);
          n3 = float(ma.SR);
          n4 = (n2 / n3);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! counter-timer-reset
  {}
  (let [play    (audio-in)
        rewind  (audio-in)
        count   (accum play rewind)
        seconds (div count (sample-rate))]
    (output :count   count)
    (output :seconds seconds)))
