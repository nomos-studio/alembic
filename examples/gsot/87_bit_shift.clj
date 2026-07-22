; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.87-bit-shift
  "GSOT pp.140 — go.bit.shift.gendsp.

  'Working with the bits of an integer' (Chapter 5)
  --------------------------------------------------
  Shifts the bits of an 8-bit integer left or right by :amount positions.
  The result is masked to 8 bits [0, 255].

  Shift direction
  ---------------
  :amount > 0 — left shift: bits move toward MSB; zeros fill from the right.
                Pattern becomes sparser at the low end; high bits fall off.
  :amount < 0 — right shift: bits move toward LSB; zeros fill from the left.
                Pattern becomes sparser at the high end; low bits fall off.
  :amount = 0 — identity: no change.

  Left shift by N multiplies the integer by 2^N (before masking).
  Right shift by N divides the integer by 2^N (integer division, floor).

  Musical use
  -----------
  Shifting a rhythmic pattern integer changes its phase within the 8-step
  grid.  Shifting a melodic bit-field moves it to a different octave range.
  Combined with binary-decode (example 84), integer shift transposes the
  decoded index up or down by powers of two.

  Signal flow
  -----------
  select2(%am>0, in>>(-am), (in<<am)&255)

  The select2 branches on the sign of :amount:
  - positive → left shift: (int(in) << int(am)) & 255
  - zero or negative → right shift: int(in) >> int(-am)

  Right shift is inherently bounded (result ≤ input ≤ 255); left shift
  is masked to 8 bits via & 255.

  Audio inputs
  ------------
  audio-in 0: in — integer value [0, 255] as a float

  Parameters
  ----------
  :amount — shift distance (negative=right, positive=left; default 1)

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n2
        with {
          n1 = hslider(\"amount\", 1.0, -7.0, 7.0, 1.0);
          n2 = float(select2(n1>0,
                             (int(n0)>>int(0.0-n1))&255,
                             (int(n0)<<int(n1))&255));
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! bit-shift
  {:params {:amount {:range [-7.0 7.0] :default 1.0}}}
  (let [in (audio-in)
        am (param :amount)
        out (faust "float(select2(%am>0,(int(%in)>>int(0.0-%am))&255,(int(%in)<<int(%am))&255))"
                   {:in in :am am})]
    (output :out out)))
