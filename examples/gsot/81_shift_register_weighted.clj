; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.81-shift-register-weighted
  "GSOT pp.129-130 — shift-register-weighted.maxpat.

  'Sequencing algorithms with binary shift registers' (Chapter 5)
  ---------------------------------------------------------------
  A binary shift register: each stage holds a 0 or 1 (gate off/on) rather
  than a continuous CV value.  On each trigger a new bit is generated via
  a Bernoulli draw with probability :weight and shifted into stage 0.

  New bit generation
  ------------------
      uniform_01 = 0.5 × (no.noise + 1)         ∈ [0, 1)
      new_bit    = float(uniform_01 < weight)     1.0 if drawn, 0.0 otherwise

  With weight=0.5 each bit is independently 50% likely to be 1.  With
  weight=0.8 the register fills rapidly with 1s (dense gate pattern);
  with weight=0.2 it stays mostly 0s (sparse pattern).

  All 8 stage outputs are available simultaneously — route them to
  separate voices, envelope triggers, or filter gate inputs to create
  independent rhythmic streams from a shared random source.

  The same bit enters at stage 0 regardless of which output is used, so
  all 8 streams share the same statistical density but are offset in time
  by their stage index (same temporal relationship as the CV shift register
  canon in examples 78-80).

  Comparison with Bernoulli gate (example 60)
  --------------------------------------------
  Example 60 (bernoulli-gate) routes a single trigger to either output A
  or output B on each event.  This patch generates N simultaneous gate
  streams at different time offsets, all statistically independent after
  the first N steps.

  Audio inputs
  ------------
  audio-in 0: trig — clock; each rising edge shifts the register

  Parameters
  ----------
  :weight — probability that each new bit is 1 (default 0.5)

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_s0,n_s1,n_s2,n_s3,n_s4,n_s5,n_s6,n_s7
        with {
          n_wt = hslider(\"weight\", 0.5, 0.0, 1.0, 0.001);
          n_nb = float(0.5*(no.noise+1.0)<n_wt);
          n_s0 = (select2(n0>0.5,_,n_nb)~_);
          n_s1 = (select2(n0>0.5,_,n_s0@1)~_);
          ...
          n_s7 = (select2(n0>0.5,_,n_s6@1)~_);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! shift-register-weighted
  {:params {:weight {:range [0.0 1.0] :default 0.5}}}
  (let [trig    (audio-in)
        weight  (param :weight)
        new-bit (faust "float(0.5*(no.noise+1.0)<%{wt})" {:wt weight})
        s0      (track-hold new-bit trig)
        s1      (track-hold (faust "%{pv}@1" {:pv s0}) trig)
        s2      (track-hold (faust "%{pv}@1" {:pv s1}) trig)
        s3      (track-hold (faust "%{pv}@1" {:pv s2}) trig)
        s4      (track-hold (faust "%{pv}@1" {:pv s3}) trig)
        s5      (track-hold (faust "%{pv}@1" {:pv s4}) trig)
        s6      (track-hold (faust "%{pv}@1" {:pv s5}) trig)
        s7      (track-hold (faust "%{pv}@1" {:pv s6}) trig)]
    (output :s0 s0)
    (output :s1 s1)
    (output :s2 s2)
    (output :s3 s3)
    (output :s4 s4)
    (output :s5 s5)
    (output :s6 s6)
    (output :s7 s7)))
