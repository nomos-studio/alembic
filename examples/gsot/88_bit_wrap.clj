; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.88-bit-wrap
  "GSOT pp.141 — go.bit.wrap.gendsp.

  'Working with the bits of an integer' (Chapter 5)
  --------------------------------------------------
  Circular left rotation of an 8-bit integer by :amount positions.
  Bits that fall off the MSB wrap around to the LSB — no bits are lost.

  Rotation vs shift
  -----------------
  go.bit.shift (example 87): bits shift off and are lost; zeros fill in.
  go.bit.wrap  (this patch): bits that fall off one end reappear at the other.

  A full 8-bit rotation by 8 (or 0) is the identity; by 4 swaps the two
  nibbles; by 1 moves each bit one position toward the MSB.

  Formula for left rotation by N positions (8-bit):
      out = ((in << N) | (in >> (8 - N))) & 255

  Musical use
  -----------
  Rotating an integer pattern creates a phase-shifted version of the same
  rhythmic content: same number of active steps, different temporal offset.
  Unlike shift, rotation preserves the step density exactly.

  For example, rotating 85 (0b01010101) by 1 gives 170 (0b10101010) —
  the same alternating gate pattern shifted by one step.

  Parameters
  ----------
  :amount — rotation distance in bit positions, 0–7 (default 1).
            0 = identity; 4 = half-rotation (swap nibbles); 7 = one right.

  Audio inputs
  ------------
  audio-in 0: in — integer value [0, 255] as a float

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n2
        with {
          n1 = hslider(\"amount\", 1.0, 0.0, 7.0, 1.0);
          n2 = float(((int(n0)<<int(n1))|(int(n0)>>(8-int(n1))))&255);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! bit-wrap
  {:params {:amount {:range [0.0 7.0] :default 1.0}}}
  (let [in  (audio-in)
        am  (param :amount)
        out (faust "float(((int(%in)<<int(%am))|(int(%in)>>(8-int(%am))))&255)"
                   {:in in :am am})]
    (output :out out)))
