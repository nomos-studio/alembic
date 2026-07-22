; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.60-bernoulli-gate
  "GSOT p.97 — go.bern.gendsp / random_bernoulli-gate.maxpat.

  'Bernoulli gate' (Chapter 4)
  ----------------------------
  Routes each trigger to exactly one of two outputs.  With probability :prob
  the trigger emerges on output :a; with probability (1 − :prob) it emerges
  on output :b.  Every trigger goes somewhere — no trigger is ever suppressed.

  Signal flow
  -----------
  audio-in: trig  — trigger pulse
  params:   :prob — probability of routing to output :a  ∈ [0, 1]

      rand_01 = 0.5 · (no.noise + 1)        uniform [0, 1] at sample rate
      pass    = float(rand_01 < prob)        1.0 → A, 0.0 → B
      out-a   = pass · trig
      out-b   = (1 − pass) · trig

  `pass` is computed once and shared by both outputs.  Both expressions
  reference the same Faust node, so the same coin flip drives both channels:
  when out-a receives the trigger, out-b is silent, and vice versa.

  Distinction from go.chance.gendsp (example 59)
  ------------------------------------------------
  go.chance: PASS or SUPPRESS.  One output.  A trigger at prob=0.5 is active
  ~50% of the time; the remaining 50% are discarded.

  go.bern:   ROUTE to A or ROUTE to B.  Two outputs.  Every trigger fires on
  exactly one output; the total trigger rate is the same on both sides
  combined.  At prob=0.5 each output receives ~half the triggers.

  go.bern is the stereo split: the two outputs are complementary.
  out-a + out-b = trig (at every sample, since pass + (1−pass) = 1).

  Musical uses
  ------------
  - Stochastic polyrhythm: two percussion voices sharing a single clock,
    with random allocation — prob=0.5 gives equal density.
  - Markov-style branching: route to different harmonization chains.
  - Feedback path selection: bias routing between two signal paths.
  - Paired with random_chance: gate each Bernoulli output further for
    sparser textures.

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n3, n4
        with {
          n1 = hslider(\"prob\", 0.5, 0.0, 1.0, 0.0001);
          n2 = float(0.5*(no.noise+1.0)<n1);
          n3 = n2*n0;
          n4 = (1.0-n2)*n0;
        };
      process = alembic_dsp;

  n0 = trig (audio-in)
  n1 = prob param
  n2 = pass = float(rand_01 < prob)
  n3 = out-a (pass · trig)
  n4 = out-b ((1−pass) · trig)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! bernoulli-gate
  {:params {:prob {:range [0.0 1.0] :default 0.5}}}
  (let [trig    (audio-in)
        rand-01 (faust "0.5*(no.noise+1.0)" {})
        prob    (param :prob)
        pass    (faust "float(%rr<%pp)" {:rr rand-01 :pp prob})
        out-a   (faust "%pa*%tr"        {:pa pass :tr trig})
        out-b   (faust "(1.0-%pa)*%tr"  {:pa pass :tr trig})]
    (output :a out-a)
    (output :b out-b)))
