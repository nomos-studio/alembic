; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.89-bit-extract
  "GSOT pp.141 — go.bit.extract.gendsp.

  'Working with the bits of an integer' (Chapter 5)
  --------------------------------------------------
  Extracts a contiguous field of :width bits starting at bit :offset from
  an 8-bit integer.  The result is an integer in [0, 2^width - 1].

  This is a generalisation of the single-bit extraction `(in >> N) & 1`
  used in examples 76, 83, and 85 — here the window can be 1–8 bits wide.

  Formula
  -------
      mask = 2^width - 1           (a run of :width ones)
      out  = (int(in) >> offset) & mask

  Examples (with in = 0b10110101 = 181):
    offset=0, width=3  →  101 (binary) = 5  (low 3 bits)
    offset=4, width=4  →  1011 (binary) = 11 (high 4 bits / upper nibble)
    offset=2, width=4  →  1101 (binary) = 13 (middle 4 bits)
    offset=0, width=1  →  1 (same as go.bit.unpack8 :b0)

  Musical use
  -----------
  Extract a 3-bit field for an 8-note scale index (0–7).
  Extract a 4-bit field for a 16-step sub-sequence (0–15).
  Extract overlapping windows at different offsets for related but distinct
  pitch voices from the same source integer.

  Parameters
  ----------
  :offset — starting bit position (0 = LSB; default 0)
  :width  — number of bits to extract (1–8; default 3)

  Audio inputs
  ------------
  audio-in 0: in — integer value [0, 255] as a float

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n3
        with {
          n1 = hslider(\"offset\", 0.0, 0.0, 7.0, 1.0);
          n2 = hslider(\"width\",  3.0, 1.0, 8.0, 1.0);
          n3 = float((int(n0)>>int(n1))&(int(pow(2.0,float(int(n2))))-1));
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! bit-extract
  {:params {:offset {:range [0.0 7.0] :default 0.0}
            :width  {:range [1.0 8.0] :default 3.0}}}
  (let [in     (audio-in)
        offset (param :offset)
        width  (param :width)
        out    (faust "float((int(%{in})>>int(%{of}))&(int(pow(2.0,float(int(%{wd}))))-1))"
                      {:in in :of offset :wd width})]
    (output :out out)))
