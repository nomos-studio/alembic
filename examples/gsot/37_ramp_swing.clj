; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.37-ramp-swing
  "GSOT pp.59-61 — ramp.swing.maxpat: swing feel via go.unit.kink on a beat ramp.

  'Plugging shapers into ramp generators'
  ----------------------------------------
  p.59 establishes the compositional principle: the unit shapers from p.58 are
  transfer functions on [0,1).  Any ramp generator produces [0,1).  Therefore
  any unit shaper composes freely with any ramp generator:

      go.ramp.frombpm → beat_ramp → go.unit.* → shaped_ramp

  The shaped ramp drives the same downstream consumers (go.ramp2trig, buffer
  read, sample-hold) as the original ramp.  The shaper changes *when* within
  the cycle events occur, not what events occur.

  pp.60-61 — ramp.swing.maxpat
  ------------------------------
  Swing is the first and most musically familiar application.  Straight 8ths
  divide the beat evenly (50/50); swing pushes the midpoint forward so the
  first half of the beat is longer than the second (2:1 = triplet swing,
  67/33 split).

  go.unit.kink is the natural shaper: its kink point is exactly the beat
  midpoint, and adjusting the kink position changes the ratio.

      kink = 0.5:  straight (50/50) — no swing
      kink = 0.67: triplet swing (2:1 ratio) — classic jazz eighth-note feel
      kink = 0.75: heavy swing (3:1 ratio)
      kink < 0.5:  'reverse swing' (second hit longer than first)

  Signal flow (ramp.swing.maxpat):

      BPM ──→ go.ramp.frombpm ──→ beat_ramp
                                       |
                                  go.unit.kink(swing)
                                       |
                                  swing_ramp ──→ go.ramp2trig / buffer read

  The swing_ramp is a valid [0,1) ramp — it advances non-linearly but still
  completes one cycle per beat and wraps cleanly.  Every downstream consumer
  from Chapter 2 (go.ramp2trig, go.ramp2steps, go.ramp.div, etc.) receives
  it without modification.

  Implementation
  --------------
  go.unit.kink inline — same formula as example 28 but applied to the live
  beat_ramp rather than a static audio-in:

      kink(x, k) = x < k  →  x/k * 0.5
                   x >= k →  0.5 + (x-k)/(1-k) * 0.5

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = hslider(\"bpm\", 120.0, 20.0, 300.0, 0.028);
      n1 = 60.0;
      n2 = (n0 / n1);
      n3 = hslider(\"beats\", 4.0, 1.0, 16.0, 0.0015);
      n4 = (n2 / n3);
      n5 = os.phasor(1.0, n4);
      n6 = hslider(\"swing\", 0.5, 0.01, 0.99, 0.0001);
      n7 = select2(n5 >= n6, n5 / max(n6, 0.0001) * 0.5, 0.5 + (n5 - n6) / max(1.0 - n6, 0.0001) * 0.5);

      process = n7, n5;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! ramp-swing
  {:params {:bpm   {:range [20.0 300.0] :default 120.0 :unit :bpm}
            :beats {:range [1.0 16.0]   :default 4.0   :unit :beats}
            :swing {:range [0.01 0.99]  :default 0.5}}}
  (let [hz    (div (div (param :bpm) (const 60.0)) (param :beats))
        ramp  (phasor hz)
        k     (param :swing)
        swing (faust "select2(%x >= %k, %x / max(%k, 0.0001) * 0.5, 0.5 + (%x - %k) / max(1.0 - %k, 0.0001) * 0.5)"
                     {:x ramp :k k})]
    (output :swing swing)
    (output :ramp  ramp)))
