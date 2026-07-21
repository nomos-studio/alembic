; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.23-ramp-bursts
  "GSOT p.53 — ramp_bursts.maxpat: N fast ramp cycles within a burst window.

  'Ramp bursts'
  -------------
  A ramp burst groups N rapid sub-ramps into a fraction of the parent cycle,
  leaving the remainder silent.  The result is a rhythmically dense 'burst'
  of events followed by a rest — a common texture in percussion programming,
  polyrhythmic density control, and granular articulation.

  Structure:

      parent ramp: [0,1) over one measure (or beat, or any period)
      width:       fraction of the parent period that is 'active' (0 to 1)
      bursts:      number of sub-ramp cycles within the active window

  Within the active window (ramp < width), N fast ramp cycles complete.
  After the active window (ramp ≥ width), the output is held at 0.

  Signal flow:

      [parent ramp]
            |
      ├── comparator(ramp, width)
      │         |          |
      │       :out       :inv-gate         active = inv-gate (1 when ramp < width)
      │                    |
      ├── ramp / width ─── scaled           0→1 compressed to active window
      │         |
      │   * bursts → wrap 0 1               N cycles within scaled ∈ [0,1)
      │         |
      └─────── * active ──────────→ [out 1 burst]

  Arithmetic
  ----------
  `scaled = ramp / width` normalises the active window to [0,1).
  `wrap(scaled * N, 0, 1)` produces N cycles: at scaled=0 the first burst
  begins; at scaled=1/N the second; at scaled=1 (= ramp=width) the N-th
  burst completes.  Multiplying by `active` silences the output after width.

  The gating produces a step discontinuity at ramp=width (burst snaps to 0).
  go.ramp_bursts2trigs (example 24) handles this correctly because the step
  at width is ≤ 1 in magnitude and the threshold is 0.5: only the N genuine
  burst wrap-arounds (Δ ≈ 1) fire as triggers.  The window-close step
  (Δ = 0 → burst value, magnitude < 1) does not cross the threshold.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = hslider(\"bpm\", 120.0, 20.0, 300.0, 0.028);
      n1 = 60.0;
      n2 = (n0 / n1);
      n3 = hslider(\"beats\", 4.0, 1.0, 16.0, 0.0015);
      n4 = (n2 / n3);
      n5 = os.phasor(1.0, n4);
      n6 = hslider(\"bursts\", 4.0, 1.0, 16.0, 0.0015);
      n7 = floor(n6);
      n8 = hslider(\"width\", 0.5, 0.01, 1.0, 0.0001);
      n9 = (float(n5 > n8));
      n10 = (1.0 - n9);
      n11 = (n5 / n8);
      n12 = (n11 * n7);
      n13 = 0.0;
      n14 = 1.0;
      n15 = (n13 + fmod((n12 - n13), (n14 - n13)));
      n16 = (n15 * n10);

      process = n16, n5;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! ramp-bursts
  {:params {:bpm    {:range [20.0 300.0] :default 120.0 :unit :bpm}
            :beats  {:range [1.0 16.0]   :default 4.0   :unit :beats}
            :bursts {:range [1.0 16.0]   :default 4.0}
            :width  {:range [0.01 1.0]   :default 0.5}}}
  (let [hz     (div (div (param :bpm) (const 60.0)) (param :beats))
        ramp   (phasor hz)
        n      (floor (param :bursts))
        width  (param :width)
        cmp    (comparator ramp width)
        active (:inv-gate cmp)
        scaled (div ramp width)
        burst  (wrap (mul scaled n) (const 0.0) (const 1.0))
        out    (mul burst active)]
    (output :burst out)
    (output :ramp  ramp)))
