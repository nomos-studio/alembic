; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.sine-oscillator
  "GSOT — Sine oscillator via phasor.

  Demonstrates the foundational gen~ pattern: a phasor drives a periodic
  function.  In gen~:

      [phasor~ 440] → [cos~]   ; phase [0,1] → cosine output

  Alembic expresses this as two connected ops:

      (phasor (param :freq))   ; accumulates phase [0,1) at sample rate
      (sine-bi <phasor>)       ; sin(2π·phase) — bipolar sine on [-1, 1]

  The gen~ model: every sample tick, the phasor accumulates `freq/SR` and
  wraps at 1.0.  The sine maps that ramp to a sinusoidal waveform.  This is
  exactly what the Faust compiler lowers to — a one-state accumulator feeding
  a sin() call.

  Emitted Faust DSP
  -----------------
  Alembic's emit-faust produces this .dsp for the patch below.  The node
  labels n0, n1, n2 match the internal graph node IDs.

      import(\"stdfaust.lib\");

      n0 = hslider(\"freq\", 440.0, 20.0, 20000.0, 1.998);
      n1 = os.phasor(1.0, n0);
      n2 = sin(2.0*ma.PI*n1);

      process = n2;

  Generated C++ kernel (Faust -lang cpp, simplified)
  ---------------------------------------------------
  The compiled inner loop makes the per-sample model concrete:

      // fRec0: phasor accumulator (2-element shift register for 1-sample history)
      // fConst0: freq / SR  (set at prepare time from the freq hslider)

      for (int i = 0; i < count; i++) {
          fRec0[0] = fConst0 + (fRec0[1] - std::floor(fConst0 + fRec0[1]));
          output0[i] = std::sin(6.28318548f * fRec0[0]);
          fRec0[1] = fRec0[0];
      }

  This IS the gen~ model: one state variable, one arithmetic expression per
  sample.  No heap allocations, no branching in the hot path."
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! sine-oscillator
  {:params {:freq {:range [20.0 20000.0] :default 440.0 :unit :hz}}}
  (let [ph  (phasor (param :freq))
        osc (sine-bi ph)]
    (output osc)))
