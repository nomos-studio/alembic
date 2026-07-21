; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.02-counter-simple-timer
  "GSOT p.20 — counter_simple_timer: elapsed sample count and seconds.

  Chapter 2 opens with this patch to introduce modular arithmetic of time.
  A running accumulator counts elapsed samples; dividing by the sample rate
  converts to seconds.  There is no gate and no reset — the counter runs
  indefinitely from the moment the patch starts.

  In gen~:

      [+ 1]  ←── [history]
        |              ↑
        └──────────────┘
        |
      [out 1 samples]
        |
      [/ samplerate]
        |
      [out 2 seconds]

  The `history` op holds the previous output and feeds it back into the
  addition.  Each sample: count = count_prev + 1.

  In Alembic, `(accum in reset)` names this pattern directly.  `(sample-rate)`
  is the block-rate constant `ma.SR`, used to convert samples to seconds.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = (select2(0.0 > 0.5, 0.0, _ + 1.0) ~ _);
      n1 = float(ma.SR);
      n2 = (n0 / n1);

      process = n0, n2;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! counter-simple-timer
  {}
  (let [count   (accum (const 1.0) (const 0.0))
        seconds (div count (sample-rate))]
    (output :samples count)
    (output :seconds seconds)))
