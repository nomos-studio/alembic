; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.18-ramp-to-freq
  "GSOT p.43 — go.ramp2freq: extract frequency in Hz from a ramp.

  go.ramp2freq.gendsp
  --------------------
  Extends go.ramp2slope (example 17) by scaling the per-sample slope to
  cycles per second:

      freq_hz = slope * samplerate

  If slope = f / SR (the per-sample increment of a ramp at frequency f Hz),
  then `slope * SR = f`.  go.ramp2freq recovers the original Hz value that
  would generate the ramp — useful for pitch tracking, tempo detection, or
  driving a second oscillator at the same frequency as an incoming ramp.

  Signal flow in go.ramp2freq.gendsp:

      [in 1 ramp]
            |
      go.ramp2slope          (conditioned delta, wrap-safe — see example 17)
            |
          * samplerate       (gen~: samplerate operator; Faust: ma.SR)
            |
      [out 1 freq_hz]

  In gen~, `samplerate` is a compile-time constant operator that emits the
  host sample rate.  In Faust the equivalent is `ma.SR` (from stdfaust.lib).
  Alembic maps this with an inline `(faust \"float(ma.SR)\")` — a self-
  contained Faust expression with no wired inlets, emitting the SR constant
  at sample rate.

  Ramp round-trip
  ---------------
  go.ramp.frombpm → beat-ramp → go.ramp2freq recovers the beat frequency
  in Hz (e.g. 2.0 Hz at 120 BPM / 1 beat).  This closes the round-trip:
  any ramp can be interrogated for its tempo, making go.ramp2freq a
  primitive for ramp-domain pitch detection and adaptive synthesis.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n8
        with {
          n1 = (n0 - n0');
          n2 = abs(n1);
          n3 = 0.5;
          n4 = (float(n2 > n3));
          n5 = (1.0 - n4);
          n6 = (select2(n5 > 0.5, _, n1) ~ _);
          n7 = float(ma.SR);
          n8 = (n6 * n7);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! ramp-to-freq
  {}
  (let [ramp  (audio-in)
        d     (delta ramp)
        cmp   (comparator (abs d) (const 0.5))
        slope (track-hold d (:inv-gate cmp))
        sr    (faust "float(ma.SR)")
        freq  (mul slope sr)]
    (output freq)))
