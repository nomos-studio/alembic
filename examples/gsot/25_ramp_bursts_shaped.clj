; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.25-ramp-bursts-shaped
  "GSOT p.55 — go.ramp_bursts_shaped: amplitude-weighted burst ramp.

  go.ramp_bursts_shaped.maxpat
  -----------------------------
  Extends ramp_bursts (example 23) by weighting each sub-ramp in the burst
  sequence with an amplitude derived from its position in the sequence.  The
  result: instead of N equal-amplitude bursts, each burst has a distinct
  gain — a 'shaped' burst envelope.

  The default shape is a linear decay: the first burst is loudest, the last
  is near-silent.  This also suppresses the window-close spurious trigger
  noted in example 24 — when the last burst has near-zero amplitude, any
  trigger it produces is inaudible.

  Signal flow:

      [parent ramp]
            |
      ├── comparator(ramp, width)  →  active (inv-gate)
      ├── ramp / width             →  scaled (0→1 in active window)
      ├── scaled * N → wrap 0 1   →  burst  (N cycles)
      ├── floor(scaled * N)        →  idx    (burst index: 0, 1, …, N-1)
      │         |
      │   1 - (idx / N)            →  shape  (linear decay: 1 → 1/N)
      │         |
      └── burst * shape * active  →  [out 1 shaped]

  Burst index and shape
  ----------------------
  `idx = floor(scaled * N)` is the integer index of the current burst
  (0 = first burst, N-1 = last).  This is the staircase quantisation from
  go.ramp2steps (example 13) applied to the scaled ramp.

  `shape = 1 - idx/N` gives a descending staircase from 1 (first burst)
  to 1/N (last burst).  At N=4: amplitudes are 1.0, 0.75, 0.5, 0.25.

  Other shapes are trivial substitutions:
    ascending:   idx / N
    accent-last: square or exponential curve of idx
    flat:        const 1.0 (= plain ramp_bursts, example 23)

  Chapter 2 close
  ----------------
  This is the final example of Chapter 2.  The chapter has built a complete
  vocabulary of ramp operations:

    Generators:    go.ramp.frombpm (ex.12)
    Processors:    go.ramp.rotate (ex.16), go.ramp.div (ex.21)
    Quantisers:    go.ramp2steps (ex.13), go.ramp.div (ex.21)
    Analysers:     go.ramp2slope (ex.17), go.ramp2freq (ex.18)
    Detectors:     go.ramp2trig (ex.14, ex.19)
    Burst:         ramp_bursts (ex.23), go.ramp_bursts2trigs (ex.24),
                   go.ramp_bursts_shaped (ex.25)

  Every operation preserves the ramp's [0,1) domain and composes freely —
  the output of any processor is a valid input to any other.

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
      n13 = floor(n12);
      n14 = 1.0;
      n15 = (n13 / n7);
      n16 = (n14 - n15);
      n17 = (n11 * n7);
      n18 = 0.0;
      n19 = 1.0;
      n20 = (n18 + fmod((n17 - n18), (n19 - n18)));
      n21 = (n20 * n16);
      n22 = (n21 * n10);

      process = n22, n5;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! ramp-bursts-shaped
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
        idx    (floor (mul scaled n))
        shape  (sub (const 1.0) (div idx n))
        burst  (wrap (mul scaled n) (const 0.0) (const 1.0))
        out    (mul (mul burst shape) active)]
    (output :burst out)
    (output :ramp  ramp)))
