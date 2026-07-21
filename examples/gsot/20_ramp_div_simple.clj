; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.20-ramp-div-simple
  "GSOT p.46 — go.ramp.div.simple: frequency-based ramp clock division.

  'Ramp/clock division'
  ----------------------
  Clock multiplication (example 12) accelerates a ramp: N sub-cycles per
  parent cycle via `wrap(ramp * N, 0, 1)`.  Division is the inverse:
  one output cycle per N input cycles.

  The simple form recovers the input ramp's frequency via go.ramp2slope +
  samplerate (the go.ramp2freq pipeline from example 18), divides by N, and
  feeds the result into a new phasor:

      slope  = go.ramp2slope(ramp)      (conditioned delta, wrap-safe)
      hz     = slope * samplerate       (go.ramp2freq)
      slow   = phasor(hz / N)           (free-running at 1/N rate)

  Signal flow in go.ramp.div.simple.gendsp:

      [in 1 ramp]
            |
        delta + abs + track-hold       go.ramp2slope: slope per sample
            |
          * samplerate                 go.ramp2freq: cycles/sec
            |
          / int(N)                     divide frequency by N
            |
          phasor                       free-running slow ramp
            |
      [out 1 divided]

  Limitation: 'simple'
  ---------------------
  The output phasor is free-running — it is not phase-locked to the input.
  On startup, or if the divisor N changes mid-cycle, the output ramp starts
  at an arbitrary phase relative to the input.  This is the 'simple' in the
  name: correct frequency, but no phase synchronisation.

  go.ramp.div (example 21) fixes this by counting input triggers modulo N
  and constructing the divided ramp from `(count + ramp) / N`, which is
  always phase-coherent with the input regardless of when division starts.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n12
        with {
          n1 = (n0 - n0');
          n2 = abs(n1);
          n3 = 0.5;
          n4 = (float(n2 > n3));
          n5 = (1.0 - n4);
          n6 = (select2(n5 > 0.5, _, n1) ~ _);
          n7 = hslider(\"div\", 4.0, 1.0, 16.0, 0.0015);
          n8 = floor(n7);
          n9 = float(ma.SR);
          n10 = (n6 * n9);
          n11 = (n10 / n8);
          n12 = os.phasor(1.0, n11);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! ramp-div-simple
  {:params {:div {:range [1.0 16.0] :default 4.0}}}
  (let [ramp  (audio-in)
        d     (delta ramp)
        cmp   (comparator (abs d) (const 0.5))
        slope (track-hold d (:inv-gate cmp))
        n     (floor (param :div))
        freq  (div (mul slope (faust "float(ma.SR)")) n)
        slow  (phasor freq)]
    (output slow)))
