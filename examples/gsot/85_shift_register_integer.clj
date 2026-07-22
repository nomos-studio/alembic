; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.85-shift-register-integer
  "GSOT pp.136-139 — shift-register-integer.maxpat.

  'Integers as patterns' (Chapter 5)
  ------------------------------------
  An integer encodes a rhythmic or melodic pattern in its bit representation.
  Bit N of the integer is 1 if step N is active, 0 if silent.  A counter
  cycles through the bit positions on each trigger, extracting the pattern
  one step at a time.  The shift register stages hold the extracted bits for
  previous triggers, giving temporally offset copies of the same pattern.

  Integer-as-pattern
  ------------------
  The :pattern parameter (0–255) encodes up to 8 steps.  Some examples:

    85  = 0b01010101  alternating every step
    170 = 0b10101010  alternating, phase-shifted by one step
    15  = 0b00001111  last four steps active (fill)
    240 = 0b11110000  first four steps active
    60  = 0b00111100  middle four steps (syncopated)
    102 = 0b01100110  off-beat pairs

  The :steps parameter sets the cycle length (1–8).  Setting :steps to 4
  with :pattern 5 (0b0101) gives a 4-step alternating gate.

  Counter and bit extraction
  --------------------------
  A modulo counter advances on each trigger:

      counter[n] = (counter[n-1] + 1) % max(1, steps)

  Bit at step N: `float(int(pattern) >> int(counter) & 1)`

  Same trigger-gated counter as examples 61, 67, and 76.

  Shift register stages
  ---------------------
  The extracted bit enters stage s0 on each trigger, displacing previous
  bits down the chain.  After 8 triggers the register holds one full cycle
  of the pattern (assuming steps=8):

      s0 = pattern[counter]         — current step
      s1 = pattern[counter-1]       — one step ago
      ...
      s7 = pattern[counter-7]       — seven steps ago

  Connecting s0–s7 to separate voices creates a temporal canon where each
  voice plays the same pattern at a different phase offset.

  Comparison with prior shift register examples
  ---------------------------------------------
  Examples 81–82 generate bits randomly (Bernoulli).
  Example 83 generates bits via LFSR XOR feedback (pseudo-random).
  This example reads bits directly from a user-specified integer —
  fully deterministic, no randomness.

  The three approaches are interchangeable in the shift register frame:
  any binary signal can drive the input to stage s0.

  Parameters
  ----------
  :pattern — integer 0–255 encoding the step pattern as a bitmask (default 85 = 0b01010101)
  :steps   — cycle length 1–8; counter wraps here (default 8)

  Audio inputs
  ------------
  audio-in 0: trig — clock; rising edge advances counter and shifts register

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_s0,n_s1,n_s2,n_s3,n_s4,n_s5,n_s6,n_s7
        with {
          n_pt = hslider(\"pattern\", 85.0, 0.0, 255.0, 1.0);
          n_ns = hslider(\"steps\",   8.0,  1.0,   8.0, 1.0);
          n_ct = (select2(n0>0.5,_,float(int(_+1.0)%max(1,int(n_ns))))~_);
          n_nb = float(int(n_pt)>>int(n_ct) & 1);
          n_s0 = (select2(n0>0.5,_,n_nb)~_);
          n_s1 = (select2(n0>0.5,_,n_s0@1)~_);
          ...
          n_s7 = (select2(n0>0.5,_,n_s6@1)~_);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! shift-register-integer
  {:params {:pattern {:range [0.0 255.0] :default 85.0}
            :steps   {:range [1.0 8.0]   :default 8.0}}}
  (let [trig    (audio-in)
        pattern (param :pattern)
        steps   (param :steps)
        counter (faust "(select2(%tr>0.5,_,float(int(_+1.0)%max(1,int(%ns))))~_)"
                       {:tr trig :ns steps})
        new-bit (faust "float(int(%pt)>>int(%ct) & 1)"
                       {:pt pattern :ct counter})
        s0 (track-hold new-bit trig)
        s1 (track-hold (faust "%pv@1" {:pv s0}) trig)
        s2 (track-hold (faust "%pv@1" {:pv s1}) trig)
        s3 (track-hold (faust "%pv@1" {:pv s2}) trig)
        s4 (track-hold (faust "%pv@1" {:pv s3}) trig)
        s5 (track-hold (faust "%pv@1" {:pv s4}) trig)
        s6 (track-hold (faust "%pv@1" {:pv s5}) trig)
        s7 (track-hold (faust "%pv@1" {:pv s6}) trig)]
    (output :s0 s0)
    (output :s1 s1)
    (output :s2 s2)
    (output :s3 s3)
    (output :s4 s4)
    (output :s5 s5)
    (output :s6 s6)
    (output :s7 s7)))
