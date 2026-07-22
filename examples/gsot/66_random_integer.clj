; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.66-random-integer
  "GSOT p.103 — go.random.gendsp / random_integer.maxpat.

  'Random integers / quantized random' (Chapter 4)
  --------------------------------------------------
  Generates a uniformly distributed random integer in [lo, hi] on each
  trigger.  The output is an integer-valued float: 0.0, 1.0, 2.0, etc.
  Used as a pitch-class selector, step sequencer index, or any discrete
  stochastic source.

  Formula
  -------
  count   = int(hi) − int(lo) + 1              number of integers in [lo, hi]
  rand_01 = 0.5 · (noise + 1)                  uniform [0, 1)
  scaled  = lo + count · rand_01               uniform [lo, lo+count)
  out     = min(hi, floor(scaled))             floor to integer, clip at hi

  The clip at hi handles the edge case rand_01 = 1.0 exactly (which would
  produce floor(lo+count) = hi+1).  In practice this never fires (noise
  is a closed interval) but is included for correctness.

  int() truncates toward zero; for integer :lo and :hi values this is
  equivalent to rounding.  Non-integer param values (e.g. lo=0.7, hi=3.2)
  are truncated to the nearest integer toward zero before counting.

  Signal flow
  -----------
  audio-in: trig   — trigger pulse
  params:   :lo    — minimum integer value (inclusive)
            :hi    — maximum integer value (inclusive)

      rand-int = min(%hi, floor(%lo + float(int(%hi)-int(%lo)+1) · 0.5·(noise+1)))
      out      = track-hold(rand-int, trig)

  go.random.gendsp vs random_integer.maxpat
  ------------------------------------------
  go.random.gendsp is the abstraction: a continuously running random
  integer source that produces a new value every sample.  random_integer.
  maxpat adds the trigger input (track-hold) to sample a new integer only
  when the trigger fires.  This example combines both: the formula is the
  go.random core; track-hold is the maxpat layer.

  Musical uses
  ------------
  - Scale degree selector: lo=0, hi=6 → 7 degrees; use pitch table to
    map to intervals.
  - Pitch-class quantiser: lo=0, hi=11 → chromatic semitones.
  - Euclidean rhythm step index: lo=0, hi=N-1.
  - Combine with Bernoulli gate (example 60) for sparse random melodies.

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n3
        with {
          n1 = hslider(\"lo\", 0.0, -16.0, 16.0, 1.0);
          n2 = hslider(\"hi\", 7.0, -16.0, 16.0, 1.0);
          n_ri = min(n2,floor(n1+float(int(n2)-int(n1)+1)*0.5*(no.noise+1.0)));
          n3 = (select2(n0>0.5,_,n_ri)~_);
        };
      process = alembic_dsp;

  n0 = trig (audio-in)
  n1 = lo param    n2 = hi param
  n_ri = random integer (fresh each sample)
  n3 = track-hold of n_ri on trig"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! random-integer
  {:params {:lo {:range [-16.0 16.0] :default  0.0}
            :hi {:range [-16.0 16.0] :default  7.0}}}
  (let [trig     (audio-in)
        lo       (param :lo)
        hi       (param :hi)
        rand-int (faust "min(%hi,floor(%lo+float(int(%hi)-int(%lo)+1)*0.5*(no.noise+1.0)))"
                        {:lo lo :hi hi})
        out      (track-hold rand-int trig)]
    (output out)))
