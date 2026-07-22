; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.76-mixer-sequencer
  "GSOT pp.123-125 — Chapter 5: Stepping in Time and Space.
  'Stepped pitches and logic gates' / mixer-sequencer.maxpat.

  A trigger-driven step sequencer with two outputs:
    :pitch — the current step's pitch value (held between triggers)
    :gate  — 1-sample pulse on trigger, masked by a per-step enable bit

  Pitch lookup — binary tree of select2
  --------------------------------------
  Each of 8 steps has a pitch parameter :p0–:p7.  The current step is
  selected via a balanced binary tree of select2 calls on the counter:

      counter < 4?
        yes → counter < 2?
                yes → counter < 1? p0 : p1
                no  → counter < 3? p2 : p3
        no  → counter < 6?
                yes → counter < 5? p4 : p5
                no  → counter < 7? p6 : p7

  This avoids Faust's rdtable (read-only, compile-time constants) and the
  data boundary (mutable arrays, example 67).  The pitch is a combinatorial
  function of the counter — no additional state needed.

  Logic gates — bitmask gate enable
  -----------------------------------
  A single :pattern parameter (integer 0–255) encodes the gate enable for
  all 8 steps as a bitmask.  Step N fires a gate if bit N of pattern is 1:

      gate_enable[n] = (int(pattern) >> int(counter)) & 1

  Examples:
      0xFF = 255 → all 8 steps active (default)
      0xAA = 170 → steps 1,3,5,7 active (off-beat)
      0x0F =  15 → only first 4 steps active
      0x55 =  85 → steps 0,2,4,6 active (on-beat)

  The gate output fires for one sample on the clock trigger, but only when
  the current step's gate enable bit is 1.

  Parameters
  ----------
  :steps   — active step count 1–8; counter wraps at this value (default 8)
  :pattern — gate bitmask 0–255; bit N enables step N's gate (default 255)
  :p0–:p7  — pitch values per step in V/oct or semitones (default 0.0)

  Counter
  -------
  Same trigger-gated modulo counter as examples 61 and 67:
      counter[n] = select2(trig>0.5, counter[n−1],
                           (counter[n−1]+1) % max(1, steps))

  The pitch and gate-enable read the counter at the SAME sample the counter
  updates, so the output reflects the NEW step immediately on the trigger
  edge.

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_pt, n_gt
        with {
          n_ns = hslider(\"steps\",   8.0, 1.0, 8.0, 1.0);
          n_pg = hslider(\"pattern\",255.0, 0.0,255.0,1.0);
          n_p0 = hslider(\"p0\", 0.0, -2.0, 10.0, 0.001);
          ...
          n_p7 = hslider(\"p7\", 0.0, -2.0, 10.0, 0.001);
          n_ct = (select2(n0>0.5,_,float(int(_+1.0)%max(1,int(n_ns))))~_);
          n_pt = select2(n_ct<4.0,
                   select2(n_ct<6.0,
                     select2(n_ct<5.0,n_p4,n_p5),
                     select2(n_ct<7.0,n_p6,n_p7)),
                   select2(n_ct<2.0,
                     select2(n_ct<1.0,n_p0,n_p1),
                     select2(n_ct<3.0,n_p2,n_p3)));
          n_ge = float(int(n_pg)>>int(n_ct) & 1);
          n_gt = float(n0>0.5)*n_ge;
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! mixer-sequencer
  {:params {:steps   {:range [1.0   8.0]   :default 8.0}
            :pattern {:range [0.0   255.0] :default 255.0}
            :p0      {:range [-2.0  10.0]  :default 0.0}
            :p1      {:range [-2.0  10.0]  :default 0.0}
            :p2      {:range [-2.0  10.0]  :default 0.0}
            :p3      {:range [-2.0  10.0]  :default 0.0}
            :p4      {:range [-2.0  10.0]  :default 0.0}
            :p5      {:range [-2.0  10.0]  :default 0.0}
            :p6      {:range [-2.0  10.0]  :default 0.0}
            :p7      {:range [-2.0  10.0]  :default 0.0}}}
  (let [trig    (audio-in)
        steps   (param :steps)
        pattern (param :pattern)
        p0      (param :p0)
        p1      (param :p1)
        p2      (param :p2)
        p3      (param :p3)
        p4      (param :p4)
        p5      (param :p5)
        p6      (param :p6)
        p7      (param :p7)
        counter (faust "(select2(%tr>0.5,_,float(int(_+1.0)%max(1,int(%ns))))~_)"
                       {:tr trig :ns steps})
        pitch   (faust "select2(%ct<4.0,select2(%ct<6.0,select2(%ct<5.0,%p4,%p5),select2(%ct<7.0,%p6,%p7)),select2(%ct<2.0,select2(%ct<1.0,%p0,%p1),select2(%ct<3.0,%p2,%p3)))"
                       {:ct counter :p0 p0 :p1 p1 :p2 p2 :p3 p3 :p4 p4 :p5 p5 :p6 p6 :p7 p7})
        gate-en (faust "float(int(%pg)>>int(%ct) & 1)" {:pg pattern :ct counter})
        gate    (faust "float(%tr>0.5)*%ge"            {:tr trig :ge gate-en})]
    (output :pitch pitch)
    (output :gate  gate)))
