; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.80-shift-register
  "GSOT pp.128 — shift-register.maxpat.

  Top-level shift-register canon demonstration.
  Extends go.shiftregister8 (example 79) with a selectable canon interval:
  voice 1 always plays stage 0 (the current input); voice 2 plays the stage
  selected by :canon (1–7), lagging voice 1 by that many trigger events.

  The :canon parameter sets the temporal distance of the imitation:
    :canon 1 → voice 2 is one trigger behind voice 1
    :canon 4 → voice 2 is four triggers behind (a classic 4-step canon)
    :canon 7 → voice 2 is seven triggers behind (near-complete loop offset)

  Signal flow
  -----------
  All 8 stages are computed (same chain as example 79).  Voice 2 is then
  selected from stages 1–7 via a binary tree of select2 calls on :canon:

      :canon < 5 ?
        yes → :canon < 3 ?
                yes → :canon < 2 ? s1 : s2
                no  → :canon < 4 ? s3 : s4
        no  → :canon < 7 ?
                yes → :canon < 6 ? s5 : s6
                no  → s7

  :canon is quantised by the Faust hslider step (1.0), so it always
  selects a well-defined stage at integer boundaries.

  Additional canon voices
  -----------------------
  For 3-voice or 4-voice canons, use go.shiftregister8 (example 79)
  directly and wire the desired stage indices to separate oscillators.
  This patch demonstrates the 2-voice case with a single interval knob.

  Audio inputs
  ------------
  audio-in 0: trig  — clock; each rising edge advances the register
  audio-in 1: cv-in — signal to shift in (pitch CV, noise, etc.)

  Parameters
  ----------
  :canon — imitation interval in steps; selects stage for voice 2 (default 4)

  Outputs
  -------
  :v1 — stage 0: current input (the 'leader' voice)
  :v2 — stage :canon: the 'follower' voice, lagging by :canon triggers

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1) = n_v1, n_v2
        with {
          n_sl = hslider(\"canon\", 4.0, 1.0, 7.0, 1.0);
          n_s0 = (select2(n0>0.5,_,n1)~_);
          n_s1 = (select2(n0>0.5,_,n_s0@1)~_);
          ...
          n_s7 = (select2(n0>0.5,_,n_s6@1)~_);
          n_v1 = n_s0;
          n_v2 = select2(n_sl<5.0,
                   select2(n_sl<7.0,
                     select2(n_sl<6.0,n_s5,n_s6),n_s7),
                   select2(n_sl<3.0,
                     select2(n_sl<2.0,n_s1,n_s2),
                     select2(n_sl<4.0,n_s3,n_s4)));
        };
      process = alembic_dsp;

  n0 = trig, n1 = cv-in"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! shift-register
  {:params {:canon {:range [1.0 7.0] :default 4.0}}}
  (let [trig   (audio-in)
        cv-in  (audio-in)
        s0     (track-hold cv-in trig)
        s1     (track-hold (faust "%pv@1" {:pv s0}) trig)
        s2     (track-hold (faust "%pv@1" {:pv s1}) trig)
        s3     (track-hold (faust "%pv@1" {:pv s2}) trig)
        s4     (track-hold (faust "%pv@1" {:pv s3}) trig)
        s5     (track-hold (faust "%pv@1" {:pv s4}) trig)
        s6     (track-hold (faust "%pv@1" {:pv s5}) trig)
        s7     (track-hold (faust "%pv@1" {:pv s6}) trig)
        sel    (param :canon)
        voice2 (faust "select2(%sl<5.0,select2(%sl<7.0,select2(%sl<6.0,%s5,%s6),%s7),select2(%sl<3.0,select2(%sl<2.0,%s1,%s2),select2(%sl<4.0,%s3,%s4)))"
                      {:sl sel :s1 s1 :s2 s2 :s3 s3 :s4 s4 :s5 s5 :s6 s6 :s7 s7})]
    (output :v1 s0)
    (output :v2 voice2)))
