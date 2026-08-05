; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.84-binary-decode
  "GSOT pp.134-135 — binary decoding using shift-register-weighted-xor.maxpat.

  'Binary decoding' (Chapter 5)
  ------------------------------
  The 8 binary stage outputs of a shift register (gate 0.0 or 1.0 per stage)
  can be combined into a multi-bit integer by interpreting them as binary
  digits.  This turns a sequence of independent gate streams into a single
  pseudo-random integer that cycles through all 2^N - 1 non-zero values
  before repeating (when the underlying shift register is driven by pure LFSR
  feedback, weight=1).

  Weighted bit sum
  ----------------
  Each stage si contributes 2^i to the output:

      out = s0 + 2*s1 + 4*s2 + 8*s3 + 16*s4 + 32*s5 + 64*s6 + 128*s7

  Result range:
    1 bit  (s0 only)   →  0 or 1        (2 values)
    3 bits (s0–s2)     →  0 to 7        (8 values; standard 8-note scale index)
    4 bits (s0–s3)     →  0 to 15       (16 values)
    8 bits (s0–s7)     →  0 to 255      (255 non-zero values per LFSR period)

  Musical use
  -----------
  A 3-bit decode gives an index 0–7, usable directly as a scale degree
  (multiply by semitone interval or feed to a lookup table).  The sequence
  visits all 7 non-zero values before repeating if the shift register is
  in pure-LFSR mode (3-bit LFSR with taps x^3+x^2+1 uses stages s0–s2 of
  a longer LFSR — values from s0–s2 of the 8-bit shift register form a
  statistically independent sub-sequence).

  With weight < 1 on the shift register, the LFSR orbit is perturbed by
  random injections; the decoded value drifts between sub-orbits, creating
  evolving melodic cells that share structure but vary over time.

  Usage
  -----
  This patch is typically chained after shift-register-weighted-xor (example 83):
  connect s0–s7 from that patch to audio inputs 0–7 of this one.

  Audio inputs
  ------------
  audio-in 0: s0 — LSB (weight 1)
  audio-in 1: s1 — weight 2
  audio-in 2: s2 — weight 4
  audio-in 3: s3 — weight 8
  audio-in 4: s4 — weight 16
  audio-in 5: s5 — weight 32
  audio-in 6: s6 — weight 64
  audio-in 7: s7 — MSB (weight 128)

  Output
  ------
  :out — decoded integer value [0, 255] as a float

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0,n1,n2,n3,n4,n5,n6,n7) = n8
        with {
          n8 = n0+2.0*n1+4.0*n2+8.0*n3+16.0*n4+32.0*n5+64.0*n6+128.0*n7;
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! binary-decode
  {}
  (let [b0 (audio-in)
        b1 (audio-in)
        b2 (audio-in)
        b3 (audio-in)
        b4 (audio-in)
        b5 (audio-in)
        b6 (audio-in)
        b7 (audio-in)
        out (faust "%{b0}+2.0*%{b1}+4.0*%{b2}+8.0*%{b3}+16.0*%{b4}+32.0*%{b5}+64.0*%{b6}+128.0*%{b7}"
                   {:b0 b0 :b1 b1 :b2 b2 :b3 b3 :b4 b4 :b5 b5 :b6 b6 :b7 b7})]
    (output :out out)))
