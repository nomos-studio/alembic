; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.14-ramp-to-trig
  "GSOT pp.39-41 (midway) — go.ramp2trig: trigger from ramp wrap discontinuity.

  'From ramps to triggers'
  ------------------------
  A [0,1) ramp has exactly one discontinuity per cycle: the wrap from ~1 back
  to ~0.  go.ramp2trig detects this jump and emits a 1-sample trigger pulse.

  Signal flow in go.ramp2trig.gendsp:

      [in 1 ramp]
            |
          delta          (ramp[n] - ramp[n-1])
            |
          abs            (magnitude of change)
            |
          > 0.5          (threshold: normal ∂ ≪ 0.5; wrap jump ≈ 1.0)
            |
      [out 1 trig]       (1 when wrap occurred, 0 otherwise)

  The threshold 0.5 works because:
  - During normal ramp advance, each sample step is BPM/60/beats/SR ≪ 0.5
    (even at 300 BPM / 1 beat / 44100 SR the step is ~0.0000068)
  - On the wrap discontinuity the ramp jumps from ~1 to ~0, so delta ≈ -1
    and abs(delta) ≈ 1.0, well above 0.5

  This is the same pattern used for beat-slicing in example 11
  (phasor_beat_slicer): `(comparator (abs (delta sl-ph)) (const 0.5))`.
  go.ramp2trig formalises it as a named utility applicable to any [0,1) ramp.

  Usage — re-triggering a one-pole envelope (the kick from example 12)
  ----------------------------------------------------------------------
  In the percussion sub-patch of ramp_from_bpm.maxpat, go.ramp2trig fires
  a gate that re-triggers a decaying one-pole envelope:

      beat-ramp → go.ramp2trig → trigger
      env: (mix 0.003 → history) — feedback IIR; trigger resets to 1.0

  The trigger produced here is a 1-sample pulse.  To drive a VCA or AR
  envelope the pulse needs to be stretched (use `:history` feedback or a
  short sample-hold gate).  See the vocab note below.

  Vocabulary note
  ---------------
  go.ramp2trig corresponds to Alembic's `:delta` → `:abs` → `:comparator`
  chain.  The `:comparator` output is already gate-width 1 sample, suitable
  as a trigger for `:sample-hold` (ba.sAndH).  For VCA triggering, follow
  with a short AR envelope or a one-pole release stage.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = hslider(\"bpm\", 120.0, 20.0, 300.0, 0.028);
      n1 = 60.0;
      n2 = (n0 / n1);
      n3 = hslider(\"beats\", 4.0, 1.0, 16.0, 0.0015);
      n4 = (n2 / n3);
      n5 = os.phasor(1.0, n4);
      n6 = (n5 - n5');
      n7 = abs(n6);
      n8 = 0.5;
      n9 = (float(n7 > n8));

      process = n9, n5;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! ramp-to-trig
  {:params {:bpm   {:range [20.0 300.0] :default 120.0 :unit :bpm}
            :beats {:range [1.0 16.0]   :default 4.0   :unit :beats}}}
  (let [hz   (div (div (param :bpm) (const 60.0)) (param :beats))
        ramp (phasor hz)
        cmp  (comparator (abs (delta ramp)) (const 0.5))
        trig (:out cmp)]
    (output :trig trig)
    (output :ramp ramp)))
