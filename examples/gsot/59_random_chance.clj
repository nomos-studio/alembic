; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.59-random-chance
  "GSOT p.96 — go.chance.gendsp / random_chance.maxpat.

  'Chance and conditions of probability' (Chapter 4)
  ---------------------------------------------------
  A probability gate: each trigger is passed to the output with probability
  :prob and suppressed (output = 0) with probability (1 − :prob).

  Signal flow
  -----------
  audio-in: trig  — trigger pulse
  params:   :prob — pass probability ∈ [0, 1]

      rand_01 = 0.5 · (no.noise + 1)        uniform [0, 1] at sample rate
      pass    = float(rand_01 < prob)        1.0 if passed, 0.0 if not
      out     = pass · trig                  gate the trigger

  Since trig is a pulse (single high sample at each trigger edge), rand_01
  is evaluated fresh at every sample but is only consequential when trig > 0.
  No track-hold is needed — the multiplication zeros all non-trigger samples.

  Probability semantics
  ----------------------
  prob = 0.0  → no trigger ever passes   (always silent)
  prob = 0.5  → roughly every other trigger passes
  prob = 1.0  → every trigger passes     (bypass)

  At prob = 0.5 the output is a Bernoulli process: independent coin flip per
  trigger.  Each trigger is statistically independent; no memory of past
  decisions.  For conditional Markov-style logic, chain multiple chance gates
  or feed the output back as an additional trigger source.

  go.chance.gendsp vs random_chance.maxpat
  -----------------------------------------
  go.chance.gendsp is the standalone abstraction (just the probability gate).
  random_chance.maxpat wires a phasor-derived trigger into go.chance.gendsp,
  giving a BPM-rate coin-flip trigger source.  This example implements the
  go.chance core; callers supply the trigger externally.

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n3
        with {
          n1 = hslider(\"prob\", 0.5, 0.0, 1.0, 0.0001);
          n2 = 0.5*(no.noise+1.0);
          n3 = float(n2<n1)*n0;
        };
      process = alembic_dsp;

  n0 = trig (audio-in)
  n1 = prob param
  n2 = rand_01 = uniform [0,1] noise
  n3 = pass · trig (probability-gated output)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! random-chance
  {:params {:prob {:range [0.0 1.0] :default 0.5}}}
  (let [trig    (audio-in)
        rand-01 (faust "0.5*(no.noise+1.0)" {})
        prob    (param :prob)
        out     (faust "float(%{rr}<%{pp})*%{tr}" {:rr rand-01 :pp prob :tr trig})]
    (output out)))
