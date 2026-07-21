; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.15-ramp-phase-shift
  "GSOT pp.41+ — shifting ramps: phase rotation via offset + wrap.

  'Shifting ramps (phase rotation)'
  ----------------------------------
  Operation 3 from the p.37 vocabulary list, now explored as a composition
  tool:

      3. offset — (+ offset → wrap 0 1)   shift start point

  Adding a constant offset to a [0,1) ramp and wrapping back into [0,1)
  rotates its phase.  The ramp's cycle length and rate are unchanged; only
  the point in the cycle where the ramp begins (its zero-crossing) is moved.

      shifted(ramp, θ) = wrap(ramp + θ, 0, 1)

  θ = 0.0 → identical to source ramp (zero rotation)
  θ = 0.5 → 180° rotation; ramp starts at the midpoint of the source cycle
  θ = 0.25 → 90° offset; four shifted ramps at 0, 0.25, 0.5, 0.75 are
              evenly spaced — a canon in four voices at quarter-measure offsets

  Signal flow:

      [in 1 ramp]   [param offset @default 0.5]
            |                  |
            +──────────────────+
                        |
                      + add
                        |
                      wrap 0 1
                        |
                  [out 1 shifted]

  Polyrhythm / canon usage
  ------------------------
  A single phasor driving two shifted ramps gives two time-streams at the
  same period but different phases.  Each stream can trigger its own
  go.ramp2trig → voice chain.  With four offsets at 0, 0.25, 0.5, 0.75
  you get a mechanical 4-voice canon — each voice enters one beat after the
  previous (at 4 beats/measure).

  The maxpat on this page derives two shifted ramps from one go.ramp.frombpm
  beat-ramp output.  The book calls this 'phase rotation' to distinguish it
  from sample-domain phase (as in FM synthesis).

  Patch: one phasor, two offset ramps
  ------------------------------------
  This example produces two independently-offset ramps from a single BPM
  phasor.  The offsets are params, not hard-coded, so you can sweep them in
  real time to create phasing effects (cf. Steve Reich's Piano Phase).

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = hslider(\"bpm\", 120.0, 20.0, 300.0, 0.028);
      n1 = 60.0;
      n2 = (n0 / n1);
      n3 = hslider(\"beats\", 4.0, 1.0, 16.0, 0.0015);
      n4 = (n2 / n3);
      n5 = os.phasor(1.0, n4);
      n6 = hslider(\"offset-a\", 0.0, 0.0, 1.0, 0.0001);
      n7 = (n5 + n6);
      n8 = 0.0;
      n9 = 1.0;
      n10 = (n8 + fmod((n7 - n8), (n9 - n8)));
      n11 = hslider(\"offset-b\", 0.5, 0.0, 1.0, 0.0001);
      n12 = (n5 + n11);
      n13 = 0.0;
      n14 = 1.0;
      n15 = (n13 + fmod((n12 - n13), (n14 - n13)));

      process = n10, n15;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! ramp-phase-shift
  {:params {:bpm      {:range [20.0 300.0] :default 120.0 :unit :bpm}
            :beats    {:range [1.0 16.0]   :default 4.0   :unit :beats}
            :offset-a {:range [0.0 1.0]    :default 0.0}
            :offset-b {:range [0.0 1.0]    :default 0.5}}}
  (let [hz     (div (div (param :bpm) (const 60.0)) (param :beats))
        ramp   (phasor hz)
        ramp-a (wrap (add ramp (param :offset-a)) (const 0.0) (const 1.0))
        ramp-b (wrap (add ramp (param :offset-b)) (const 0.0) (const 1.0))]
    (output :ramp-a ramp-a)
    (output :ramp-b ramp-b)))
