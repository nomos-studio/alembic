; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.90-bit-rotate
  "GSOT pp.141-142 — go.bit.rotate.gendsp.

  'Rotating a bit sequence' (Chapter 5)
  ---------------------------------------
  A trigger-driven circular rotation: on each trigger, the 8-bit integer is
  rotated left by one position.  The rotation accumulates across triggers,
  cycling through all 8 positions before returning to the original pattern.

  Distinction from go.bit.wrap (example 88)
  ------------------------------------------
  go.bit.wrap: static, one-shot rotation by a fixed :amount parameter.
               Output changes immediately when :amount changes.
               No state — same input always gives same output for same :amount.

  go.bit.rotate: stateful, trigger-driven accumulation.
                 A counter advances on each trigger (0→1→...→7→0).
                 Output is the input rotated by the current counter value.
                 Each trigger produces the NEXT rotation in sequence.

  Rotation formula (left rotation by N positions):
      out = ((in << N) | (in >> (8-N))) & 255

  Musical use
  -----------
  Feed the output of shift-register-integer (example 85) or any integer
  pattern into go.bit.rotate, then decode with binary-decode (example 84)
  or unpack with bit-unpack8 (example 86).  Each trigger reveals the same
  pattern from a new phase, creating melodic phasing effects.

  Example with pattern 0b01010101 (85):
    trigger 1 → 0b10101010 (170)  — rotated 1
    trigger 2 → 0b01010101 (85)   — rotated 2 = identity for this pattern
    ...

  Counter mechanics
  -----------------
  The counter is a modulo-8 register:
      counter[n] = (counter[n-1] + 1) % 8

  After 8 triggers the counter wraps to 0 and the original pattern is
  restored.  The counter starts at 0, so the first output is the unrotated
  input; the first trigger produces the 1-step rotation.

  Audio inputs
  ------------
  audio-in 0: in      — integer value [0, 255] as a float
  audio-in 1: trigger — clock; each rising edge advances the rotation

  Output
  ------
  :out — the input integer rotated by the current counter value [0, 255]

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1) = n3
        with {
          n2 = (select2(n1>0.5,_,float(int(_+1.0)%8))~_);
          n3 = float(((int(n0)<<int(n2))|(int(n0)>>(8-int(n2))))&255);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! bit-rotate
  {}
  (let [in      (audio-in)
        trigger (audio-in)
        ; Modulo-8 counter: advances by 1 on each trigger
        counter (faust "(select2(%{tr}>0.5,_,float(int(_+1.0)%8))~_)"
                       {:tr trigger})
        ; Circular left rotation of input by counter positions
        out     (faust "float(((int(%{in})<<int(%{ct}))|(int(%{in})>>(8-int(%{ct}))))&255)"
                       {:in in :ct counter})]
    (output :out out)))
