; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.01-param-operator
  "GSOT p.11 — The param operator: stereo volume control.

  The first example in the book introduces `param` as the mechanism for
  receiving control values from outside the gen~ patcher.  The patch is a
  stereo volume control: two audio inputs multiplied by a single gain param.

  In gen~:

      [in 1]   [in 2]
         \\       /
      [param vol 1.]
          |   |
       [* vol] [* vol]
          |       |
      [out 1]  [out 2]

  The key distinction `param` introduces: it operates at *block rate*, not
  sample rate.  Its value is constant for the duration of each DSP block and
  changes only when a new value is sent to it.  Audio signals (`in`, `out`)
  are sample-rate.  `param` is the seam between the two rates.

  In Alembic, `(param :name)` is the direct analog.  The rate distinction is
  encoded in the patch schema: params are declared in the `:params` map with
  range and default; audio inputs are `(audio-in)` nodes.

  Emitted Faust DSP
  -----------------
  Two `(audio-in)` nodes become function parameters in the generated Faust.
  The `param` node lowers to an `hslider` — Faust's block-rate control inlet.

      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1) = n3, n4
        with {
          n2 = hslider(\"vol\", 1.0, 0.0, 1.0, 0.0001);
          n3 = (n0 * n2);
          n4 = (n1 * n2);
        };
      process = alembic_dsp;

  The `with {}` clause scopes the inner definitions to the function so that
  `n0` and `n1` (the audio-in parameters) are in scope for `n3` and `n4`.

  Generated C++ kernel (Faust -lang cpp, simplified)
  ---------------------------------------------------
  The compiled kernel makes the two-rate model explicit: `fHslider0` (vol) is
  read once per block from the parameter table, then applied to every sample.

      float fHslider0; // vol — written by setParamValue(), read per block

      void compute(int count, float** inputs, float** outputs) {
          float* in0  = inputs[0];   float* in1  = inputs[1];
          float* out0 = outputs[0];  float* out1 = outputs[1];
          float vol = fHslider0;     // block-rate: read once
          for (int i = 0; i < count; i++) {
              out0[i] = in0[i] * vol;
              out1[i] = in1[i] * vol;
          }
      }

  No state variables, no history, no feedback.  The simplest possible
  sample-rate computation — a scalar multiply — driven by a block-rate param."
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! stereo-volume-control
  {:params {:vol {:range [0.0 1.0] :default 1.0}}}
  (let [left  (audio-in)
        right (audio-in)
        vol   (param :vol)]
    (output :left  (mul left vol))
    (output :right (mul right vol))))
