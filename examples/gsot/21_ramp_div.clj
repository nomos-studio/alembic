; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.21-ramp-div
  "GSOT p.47 — go.ramp.div: phase-locked ramp clock division.

  go.ramp.div.gendsp
  -------------------
  The full form of ramp division.  Where go.ramp.div.simple (example 20)
  runs a free phasor at 1/N the detected frequency, go.ramp.div stays
  phase-coherent with the input by counting input cycles and constructing
  the divided ramp directly from the count and the current ramp position:

      trig    = go.ramp2trig(ramp)       1-sample pulse per input cycle
      count   = trigger_count mod N      0, 1, …, N-1, 0, 1, …
      divided = (count + ramp) / N       [0,1) over N input cycles

  Signal flow in go.ramp.div.gendsp:

      [in 1 ramp]
            |
      ├── go.ramp2trig ──────── trig
      │         |
      │   trigger counter
      │      mod N               count ∈ {0, 1, …, N-1}
      │         |
      └─── + count ── / N ────→ [out 1 divided]

  Phase coherence
  ---------------
  At the moment trig fires (ramp wraps to 0), count advances.  So divided
  jumps from (N-1)/N to (N-1+0)/N = (N-1)/N ... wait — at the wrap instant
  ramp ≈ 0 and count advances to the next value, so:

      end of slow cycle:  count=N-1, ramp→1  →  divided → (N-1+1)/N = 1
      start of next:      count=0,   ramp=0  →  divided = 0

  The slow ramp completes exactly [0,1) over N input cycles with no
  discontinuities other than the intended wrap at the N-th input boundary.

  Counter implementation
  ----------------------
  A trigger counter modulo N is implemented as:

      cnt    = counter (large max, wrap) clocked by trig
      count  = wrap(cnt, 0, N)          fmod(cnt, N) in Faust

  The `counter` op counts up on each trigger (0,1,…,max,0,…).  `wrap` with
  hi=N then gives fmod(cnt, N), producing 0,1,…,N-1,0,1,…  This works for
  any runtime value of N without requiring a compile-time-fixed modulus.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n14
        with {
          n1 = (n0 - n0');
          n2 = abs(n1);
          n3 = 0.5;
          n4 = (float(n2 > n3));
          n6 = hslider(\"div\", 4.0, 1.0, 16.0, 0.0015);
          n7 = floor(n6);
          n8 = 0.0;
          n9 = (select2(n8 > 0.5, select2((n4 > 0.5) & (n4' <= 0.5), _, fmod(_ + 1.0, 1024.0)), 0.0) ~ _);
          n11 = 0.0;
          n12 = (n11 + fmod((n9 - n11), (n7 - n11)));
          n13 = (n12 + n0);
          n14 = (n13 / n7);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! ramp-div
  {:params {:div {:range [1.0 16.0] :default 4.0}}}
  (let [ramp    (audio-in)
        d       (delta ramp)
        cmp     (comparator (abs d) (const 0.5))
        trig    (:out cmp)
        n       (floor (param :div))
        cnt     (counter {:max 1024 :dir :up :wrap true} trig (const 0.0))
        count   (wrap (:out cnt) (const 0.0) n)
        divided (div (add count ramp) n)]
    (output divided)))
