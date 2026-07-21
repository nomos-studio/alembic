; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.56-random-steps
  "GSOT p.93 — random_steps.maxpat.

  'Feel the noise' (Chapter 4) — triggered sample-and-hold of noise
  ------------------------------------------------------------------
  Combines `random_range` (example 55) with a trigger input to produce
  a stepped random signal: a new random value is sampled each time a
  trigger fires, held until the next trigger.

  Signal flow
  -----------
  audio-in: trig  — trigger pulse (e.g. from go.ramp2trig, example 14)
  params:   :lo, :hi  — output range (same semantics as random_range)

      noise → scale to [lo,hi] → track-hold(scaled, trig) → out

  `track-hold` emits the standard gen~ S&H idiom:
      (select2(trig>0.5, _, scaled) ~ _)
  On trig=1: passes scaled noise to output.
  On trig=0: feeds back previous output (holds).

  This produces a new random step at each trigger edge and holds between.
  With a ramp-derived trigger at BPM rate, this is a classic random
  step sequencer.  With a slower phasor (e.g. 0.5 Hz), it produces
  random LFO-rate values for stochastic modulation.

  Relationship to smooth-stepped-noise (example 39)
  --------------------------------------------------
  random_steps is the UNSMOOTHED predecessor of smooth_stepped (example 39).
  Feeding the output of random_steps into a portamento (example 42)
  recreates the smooth-stepped interpolation pattern from first principles.

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n3
        with {
          n1 = hslider(\"lo\", -1.0, -1.0, 1.0, 0.0002);
          n2 = hslider(\"hi\",  1.0, -1.0, 1.0, 0.0002);
          n3 = n1+(n2-n1)*0.5*(no.noise+1.0);
          n4 = (select2(n0>0.5,_,n3)~_);
        };
      process = alembic_dsp;

  n0 = trig (audio-in)
  n1 = lo param
  n2 = hi param
  n3 = scaled noise (lo + (hi-lo)*0.5*(noise+1))
  n4 = track-hold output (S&H)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! random-steps
  {:params {:lo {:range [-1.0 1.0] :default -1.0}
            :hi {:range [-1.0 1.0] :default  1.0}}}
  (let [trig   (audio-in)
        n      (faust "no.noise" {})
        lo     (param :lo)
        hi     (param :hi)
        scaled (faust "%lo+(%hi-%lo)*0.5*(%nn+1.0)" {:lo lo :hi hi :nn n})
        out    (track-hold scaled trig)]
    (output out)))
