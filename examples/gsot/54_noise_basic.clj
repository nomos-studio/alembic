; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.54-noise-basic
  "GSOT p.91 — Chapter 4 'Feel the noise' — the noise operator.

  'Feel the noise' (Chapter 4, opening)
  --------------------------------------
  Chapter 4 introduces the gen~ `noise` operator: a white-noise source
  generating uniformly-distributed random samples in [−1, 1] at sample rate.
  It takes no inputs and produces a new random value each sample.

  In Faust: `no.noise` from noises.lib (included via stdfaust.lib).
  Like gen~'s `noise`, it is bipolar ([-1, 1]) and stateless from the
  caller's perspective — internal state is managed by the Faust runtime.

  Source topology
  ---------------
  Unlike all previous examples, there is no audio-in.  The emitted function
  takes no arguments:

      alembic_dsp = n1
        with {
          n0 = hslider(\"amp\", 1.0, 0.0, 1.0, 0.0001);
          n1 = no.noise * n0;
        };
      process = alembic_dsp;

  The :amp parameter scales the output amplitude.  Default 1.0 = full-scale
  white noise.  Reducing amplitude is the standard way to control noise level
  before mixing or further processing.

  White noise spectral character
  --------------------------------
  Equal energy per frequency — flat spectrum (within the Nyquist band).
  Perceived as bright, broadband hiss.  Starting point for:
    - Subtractive synthesis (filter to taste)
    - Stochastic modulation (sample-and-hold → stepped random)
    - Excitation signals (resonators, physical modeling)
    - Dithering

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp = n1
        with {
          n0 = hslider(\"amp\", 1.0, 0.0, 1.0, 0.0001);
          n1 = no.noise*n0;
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! noise-basic
  {:params {:amp {:range [0.0 1.0] :default 1.0}}}
  (let [n   (faust "no.noise" {})
        out (faust "%{nn}*%{aa}" {:nn n :aa (param :amp)})]
    (output out)))
