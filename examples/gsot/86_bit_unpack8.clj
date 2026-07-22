; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.86-bit-unpack8
  "GSOT pp.140 — go.bit.unpack8.gendsp.

  'Working with the bits of an integer' (Chapter 5)
  --------------------------------------------------
  Unpacks an integer value into 8 individual binary signals (one per bit).
  Inverse of binary-decode (example 84), which packs 8 bits into one integer.

  Each output :b0–:b7 is 0.0 or 1.0 depending on whether the corresponding
  bit of the input is 0 or 1:

      :b0 = LSB (bit 0, weight 1)
      :b1 = bit 1 (weight 2)
      ...
      :b7 = MSB (bit 7, weight 128)

  Extraction: `float((int(in) >> N) & 1)` for bit N.

  Usage: connect the integer output of shift-register-integer (example 85)
  or the packed shift register state to this patch to recover individual
  gate streams.  Can also unpack a parameter-driven pattern integer into
  its gate components for routing to separate voices.

  Audio inputs
  ------------
  audio-in 0: in — integer value [0, 255] as a float

  Outputs
  -------
  :b0–:b7 — individual bit signals (0.0 or 1.0), LSB-first

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n1,n2,n3,n4,n5,n6,n7,n8
        with {
          n1 = float(int(n0)    &1);
          n2 = float((int(n0)>>1)&1);
          n3 = float((int(n0)>>2)&1);
          n4 = float((int(n0)>>3)&1);
          n5 = float((int(n0)>>4)&1);
          n6 = float((int(n0)>>5)&1);
          n7 = float((int(n0)>>6)&1);
          n8 = float((int(n0)>>7)&1);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! bit-unpack8
  {}
  (let [in (audio-in)
        b0 (faust "float(int(%in)&1)"      {:in in})
        b1 (faust "float((int(%in)>>1)&1)" {:in in})
        b2 (faust "float((int(%in)>>2)&1)" {:in in})
        b3 (faust "float((int(%in)>>3)&1)" {:in in})
        b4 (faust "float((int(%in)>>4)&1)" {:in in})
        b5 (faust "float((int(%in)>>5)&1)" {:in in})
        b6 (faust "float((int(%in)>>6)&1)" {:in in})
        b7 (faust "float((int(%in)>>7)&1)" {:in in})]
    (output :b0 b0)
    (output :b1 b1)
    (output :b2 b2)
    (output :b3 b3)
    (output :b4 b4)
    (output :b5 b5)
    (output :b6 b6)
    (output :b7 b7)))
