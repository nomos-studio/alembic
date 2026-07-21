; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.05-counter-and-wrap
  "GSOT p.24 — counter_and_wrap: looping sample counter via wrap.

  Extends the pausable counter (example 03) by wrapping the accumulated
  position at a param-controlled duration, turning the one-shot playhead
  into a loop.

  In gen~:

      [in 1 play]   [in 2 rewind]
            |              |
           [+]       [switch 0]
            |  ↑          ↓
          history   [wrap(0, duration)] ← [param duration 4000]
                          ↓
                    [out 1 count]

  `switch 0` resets the counter to 0 when rewind fires; otherwise passes
  the incremented value.  `wrap` keeps the count in [0, duration), so after
  `duration` samples the position jumps back to 0 and the loop repeats.
  `history` feeds the wrapped output back into `+`, so the loop position
  is always in range — the accumulated state never grows without bound.

  In Alembic the same structure is: accumulate with `accum`, then `wrap`
  the output to [0, duration).  The feedback inside `accum` is already
  on the pre-wrap value, so wrap is a post-processing step.  For typical
  loop durations (< a few minutes at 44100 Hz) this has identical output
  to the gen~ patch; the book's design avoids float-precision drift by
  keeping the feedback state in range, which `wrap (accum ...)` also
  achieves because wrap is applied every sample.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1) = n5
        with {
          n2 = hslider(\"duration\", 4000.0, 1.0, 88200.0, 8.8199);
          n3 = (select2(n1 > 0.5, 0.0, _ + n0) ~ _);
          n4 = 0.0;
          n5 = (n4 + fmod((n3 - n4), (n2 - n4)));
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! counter-and-wrap
  {:params {:duration {:range [1.0 88200.0] :default 4000.0 :unit :samples}}}
  (let [play     (audio-in)
        rewind   (audio-in)
        duration (param :duration)
        count    (wrap (accum play rewind) (const 0.0) duration)]
    (output :count count)))
