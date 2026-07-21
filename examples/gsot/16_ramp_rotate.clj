; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.16-ramp-rotate
  "GSOT pp.41+ — go.ramp.rotate: named ramp phase-rotation utility.

  go.ramp.rotate.gendsp
  ----------------------
  The reusable abstraction for operation 3 of the ramp vocabulary (p.37):

      3. offset — (+ offset → wrap 0 1)   shift start point

  Takes a [0,1) ramp as its inlet and applies a rotation parameter,
  returning a new ramp with shifted phase.  The cycle length and rate
  are unchanged; only the zero-crossing moves.

      rotated(x, θ) = wrap(x + θ, 0, 1)

  Signal flow in go.ramp.rotate.gendsp:

      [in 1 ramp]   [param rotation @default 0.0]
            |                  |
            +──── + add ────────+
                        |
                      wrap 0 1
                        |
                  [out 1 rotated]

  This is the gendsp factoring of what example 15 (ramp_phase_shift)
  applied twice inline.  By naming it, the book makes `go.ramp.rotate`
  composable: any ramp — from go.ramp.frombpm, from a phasor, from
  another go.ramp.rotate output — can be fed in.

  Relationship to the vocabulary list (p.37, item 3)
  ---------------------------------------------------
  Example 13 (go.ramp2steps) factored item 4 into a primitive.
  Example 14 (go.ramp2trig)  factored the wrap discontinuity detection.
  This example factors item 3 — completing the named-utility coverage of
  all four ramp operations.  Item 1 (scale) and item 2 (wrap×N) do not
  have their own go.* utilities in the GSOT library because they are
  already idiomatic single-op forms (* N) and (* N → wrap 0 1).

  go.ramp.rotate as a processor (audio-in form)
  ----------------------------------------------
  Unlike go.ramp.frombpm (generator) and go.ramp2steps / go.ramp2trig
  (transformers with internal source), go.ramp.rotate is purely a
  processor.  Its input is an audio-rate ramp signal from any source.
  Alembic maps this with `(audio-in)` as the ramp inlet.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n5
        with {
          n1 = hslider(\"rotation\", 0.5, 0.0, 1.0, 0.0001);
          n2 = (n0 + n1);
          n3 = 0.0;
          n4 = 1.0;
          n5 = (n3 + fmod((n2 - n3), (n4 - n3)));
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! ramp-rotate
  {:params {:rotation {:range [0.0 1.0] :default 0.5}}}
  (let [ramp    (audio-in)
        rotated (wrap (add ramp (param :rotation)) (const 0.0) (const 1.0))]
    (output rotated)))
