; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.17-ramp-to-slope
  "GSOT p.42 — go.ramp2slope: extract per-sample slope from a ramp.

  go.ramp2slope.gendsp
  ---------------------
  A [0,1) ramp advances by a fixed amount per sample.  That amount —
  the slope — is `frequency / samplerate`.  `go.ramp2slope` extracts it,
  returning a block-stable value suitable for pitch detection or re-use as
  a frequency control.

  The naive approach — `delta(ramp)` — works for every sample *except* the
  wrap discontinuity, where the ramp jumps from ~1 back to ~0 and delta
  produces a large negative spike ≈ -1.

  go.ramp2slope conditions the delta using the same wrap detector as
  go.ramp2trig, but instead of emitting a trigger it *holds* the previous
  slope value across the wrap boundary:

      [in 1 ramp]
            |
          delta                   (ramp[n] - ramp[n-1])
            |
          ├── abs → > 0.5         wrap-detect comparator
          │          |    \\
          │        :out  :inv-gate
          │                |
          └─────── track-hold     follows delta while inv-gate > 0.5 (normal),
                        |         holds previous slope at the wrap (inv-gate = 0)
                  [out 1 slope]

  The key: `:comparator` returns `{:out gate :inv-gate inv}`.
  - `:out` is 1 at the wrap, 0 otherwise  → go.ramp2trig uses this
  - `:inv-gate` is 0 at the wrap, 1 otherwise → go.ramp2slope uses this

  `track-hold` (Alembic level-triggered T&H) follows its input while
  the gate is high (> 0.5) and holds when the gate falls.  With
  `:inv-gate` as the gate:
  - Normal samples: inv-gate = 1 → track follows delta → slope updated
  - Wrap sample:   inv-gate = 0 → track holds previous slope → no spike

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n6
        with {
          n1 = (n0 - n0');
          n2 = abs(n1);
          n3 = 0.5;
          n4 = (float(n2 > n3));
          n5 = (1.0 - n4);
          n6 = (select2(n5 > 0.5, _, n1) ~ _);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! ramp-to-slope
  {}
  (let [ramp  (audio-in)
        d     (delta ramp)
        cmp   (comparator (abs d) (const 0.5))
        slope (track-hold d (:inv-gate cmp))]
    (output slope)))
