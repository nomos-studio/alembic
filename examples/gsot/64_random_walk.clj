; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.64-random-walk
  "GSOT p.101-102 — random_walks.maxpat.

  'Random walks in nature' (Chapter 4)
  -------------------------------------
  A random walk: at each trigger a random increment is added to the
  current position.  Between triggers the position holds.  The position
  drifts without any preferred direction or mean-reversion; variance
  grows linearly with the number of steps taken.

  Signal flow
  -----------
  audio-in: trig    — trigger pulse (controls step rate)
  params:   :step   — maximum step magnitude (one-sided, output in
                       [-step, step] per trigger)

      rand-inc = step × no.noise                      ∈ [−step, step]
      inc-held = track-hold(rand-inc, trig)           latch new step on trigger
      position = (select2(trig>0.5, _, _+inc-held) ~ _)
                 when trig: position += inc-held
                 otherwise: position holds

  Random walk statistics
  ----------------------
  After N steps of uniform increments U[−step, step]:
      E[position]   = 0              (unbiased — no preferred direction)
      Var[position] = N × step²/3    (grows with each step)
      std dev       = step × √(N/3)

  The walk is unbounded: given enough steps it will eventually reach any
  value.  For musical use, downstream processing controls the range:
    - Clamp:       max(-1, min(1, position))   — hard limit
    - Portamento:  example 42 — smooth the position signal
    - Fold:        subtract floor(position) — wrap around [0,1]

  'Random walks in nature'
  ------------------------
  Brownian motion, animal foraging paths, stock prices, and thermal noise
  all exhibit random-walk statistics.  The key signature is the √N growth
  of displacement — slow at first, then faster over longer timescales.
  In gen~, random_walks.maxpat demonstrates this as a slowly drifting CV
  source: unlike noise (new random value each step) or smooth-stepped noise
  (interpolation within a bounded range), the walk has long-term memory —
  each position depends on the entire history of past steps.

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n3
        with {
          n1 = hslider(\"step\", 0.1, 0.0, 1.0, 0.0001);
          n2 = (select2(n0>0.5,_,n1*no.noise)~_);  // inc-held
          n3 = (select2(n0>0.5,_,_+n2)~_);          // position
        };
      process = alembic_dsp;

  n0 = trig (audio-in)
  n1 = step param
  n2 = inc-held (track-hold of step×noise)
  n3 = position (running sum, holds between triggers)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! random-walk
  {:params {:step {:range [0.0 1.0] :default 0.1}}}
  (let [trig     (audio-in)
        step     (param :step)
        rand-inc (faust "%{st}*no.noise"              {:st step})
        inc-held (track-hold rand-inc trig)
        position (faust "(select2(%{tr}>0.5,_,_+%{ih})~_)" {:tr trig :ih inc-held})]
    (output position)))
