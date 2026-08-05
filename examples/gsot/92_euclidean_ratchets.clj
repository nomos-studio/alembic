; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.92-euclidean-ratchets
  "GSOT pp.149 — euclidean_ratchets.maxpat.

  'Euclidean rhythms — ratchets' (Chapter 5)
  --------------------------------------------
  Extends euclidean-rhythms (example 91) with ratcheting: at each Euclidean
  beat position, fires :ratchets rapid gate pulses over consecutive triggers
  rather than a single gate.

  Ratchet mechanism
  -----------------
  A countdown register loads R = :ratchets on each Euclidean gate event and
  decrements by 1 on each subsequent trigger until it reaches 0.  The output
  gate is 1 whenever the counter is above 0.

  The register advances only on trigger events (trigger-gated feedback),
  so the ratchet rate is the same as the clock rate — the R pulses occupy R
  consecutive clock ticks starting from each Euclidean beat position.

  State machine per trigger event:
    Euclidean beat fires → load R into ratchet counter
    Euclidean beat silent → decrement ratchet counter (floor at 0)
    No trigger → hold ratchet counter

  Signal flow
  -----------
  ratch_ct = (select2(trig>0.5, _,
                select2(gate>0.5, max(0, _-1), float(ratchets))))~_

  output_gate = float(ratch_ct > 0)

  Edge cases
  ----------
  If two Euclidean beats are fewer than R steps apart, the counter resets to
  R before reaching 0 — the ratchet restarts cleanly at the second beat.
  With :ratchets=1 the output is identical to euclidean-rhythms (example 91).

  Parameters
  ----------
  :beats    — N: beats per pattern cycle (1–16; default 3)
  :steps    — K: steps per pattern cycle (2–16; default 8)
  :ratchets — R: gate pulses per beat (1–8; default 2)

  Audio inputs
  ------------
  audio-in 0: trigger — clock; each rising edge advances the step counter

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_gate
        with {
          n_bt = hslider(\"beats\",    3.0, 1.0, 16.0, 1.0);
          n_ns = hslider(\"steps\",    8.0, 2.0, 16.0, 1.0);
          n_rt = hslider(\"ratchets\", 2.0, 1.0,  8.0, 1.0);
          n_ct = (select2(n0>0.5,_,float(int(_+1.0)%max(1,int(n_ns))))~_);
          n_cf = floor(n_ct*n_bt/max(1.0,n_ns));
          n_eg = max(n_cf>n_cf@1, n_ct<n_ct@1);
          n_rc = (select2(n0>0.5,_,select2(n_eg>0.5,max(0.0,_-1.0),float(int(n_rt))))~_);
          n_gate = float(n_rc>0.0);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! euclidean-ratchets
  {:params {:beats    {:range [1.0 16.0] :default 3.0}
            :steps    {:range [2.0 16.0] :default 8.0}
            :ratchets {:range [1.0 8.0]  :default 2.0}}}
  (let [trig     (audio-in)
        beats    (param :beats)
        steps    (param :steps)
        ratchets (param :ratchets)
        ; Euclidean step counter and digitized-ratio gate (same as example 91)
        counter  (faust "(select2(%{tr}>0.5,_,float(int(_+1.0)%max(1,int(%{ns}))))~_)"
                        {:tr trig :ns steps})
        cur-f    (faust "floor(%{ct}*%{bt}/max(1.0,%{ns}))"
                        {:ct counter :bt beats :ns steps})
        gate     (faust "max(%{cf}>%{cf}@1,%{ct}<%{ct}@1)"
                        {:cf cur-f :ct counter})
        ; Ratchet counter: loads R on gate, decrements per trigger, holds between
        ratch-ct (faust "(select2(%{tr}>0.5,_,select2(%{ga}>0.5,max(0.0,_-1.0),float(int(%{rt})))))~_"
                        {:tr trig :ga gate :rt ratchets})
        ; Output high while ratchet counter > 0
        out-gate (faust "float(%{rc}>0.0)" {:rc ratch-ct})]
    (output :gate out-gate)))
