; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.13-ramp-to-steps
  "GSOT p.39 (bottom) — go.ramp2steps: quantise a ramp to N equal steps.

  'From ramps to steps'
  ---------------------
  The page caps the Chapter 2 ramp-processing vocabulary by factoring out
  operation 4 from the list established on p.37 as a reusable primitive:

      4. step — (* N → floor → / N)     quantise to N equal positions

  This is go.ramp2steps.gendsp.  Given a [0,1) ramp and integer N:

      floor(ramp * N) / N

  The result is a staircase: N equal-width steps from 0 to (N-1)/N, each
  exactly 1/N wide.  The ramp's continuous motion becomes a sequence of
  discrete jumps, one per cycle subdivision.

  Signal flow in go.ramp2steps.gendsp:
  ------------------------------------

      [in 1 ramp]   [param steps @default 8]
            |                  |
            |            int(steps) → n
            |                  |
            +──── * n ─── floor ─── / n ──→ [out 1 stepped]

  `int(steps)` coerces the float param to an integer so the quantisation
  boundary falls at exact sample positions.  Alembic maps this to
  `(floor (param :steps))`.

  Relationship to prior examples
  --------------------------------
  - Example 10 (phasor_loop_processing): ramp offset and scale; step quantisation
    would snap the playhead to fixed divisions of the loop.
  - Example 11 (phasor_beat_slicer): the slice offset `floor(jmp * slices)` is
    go.ramp2steps applied to the jump signal, not the loop ramp.
  - Example 12 (ramp_from_bpm): vocabulary list p.37 — this example is item 4.

  The self-contained patch below wires go.ramp.frombpm → go.ramp2steps to
  produce a stepped beat ramp alongside the continuous beat ramp, exactly
  as shown on p.39 of the book.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = hslider(\"bpm\", 120.0, 20.0, 300.0, 0.028);
      n1 = 60.0;
      n2 = (n0 / n1);
      n3 = hslider(\"beats\", 4.0, 1.0, 16.0, 0.0015);
      n4 = (n2 / n3);
      n5 = os.phasor(1.0, n4);
      n6 = hslider(\"steps\", 8.0, 1.0, 64.0, 0.0063);
      n7 = floor(n6);
      n8 = (n5 * n7);
      n9 = floor(n8);
      n10 = (n9 / n7);

      process = n10, n5;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! ramp-to-steps
  {:params {:bpm   {:range [20.0 300.0] :default 120.0 :unit :bpm}
            :beats {:range [1.0 16.0]   :default 4.0   :unit :beats}
            :steps {:range [1.0 64.0]   :default 8.0}}}
  (let [hz      (div (div (param :bpm) (const 60.0)) (param :beats))
        ramp    (phasor hz)
        n       (floor (param :steps))
        stepped (div (floor (mul ramp n)) n)]
    (output :stepped stepped)
    (output :ramp    ramp)))
