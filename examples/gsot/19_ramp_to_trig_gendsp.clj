; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.19-ramp-to-trig-gendsp
  "GSOT pp.43-45 — ramp_to_trig.maxpat + go.ramp2trig: formal derivation.

  p.43 (bottom) — 'Getting triggers from a cyclic ramp'
  -------------------------------------------------------
  The section opens by motivating the trigger primitive: many musical
  processes (envelope re-trigger, counter advance, sample latch) need a
  1-sample pulse at the moment the ramp wraps.  The wrap discontinuity
  is detectable because it is the only point where a [0,1) ramp's
  sample-to-sample difference is large — ≈ -1 rather than the normal
  tiny positive slope.

  The chapter has already used this detection in two places:
    - example 11 (beat slicer): `(comparator (abs (delta sl-ph)) (const 0.5))`
      fires the slice-offset latch at each slice boundary
    - example 12 docstring: go.ramp2trig referenced as the trigger source
      for the kick and crash voices

  pp.43-45 formalise it as a named, reusable utility.

  p.44 — ramp_to_trig.maxpat
  ---------------------------
  The maxpat builds the circuit from scratch to show the derivation:

      [in 1 ramp]
            |
          delta               ramp[n] - ramp[n-1]
            |
          abs                 magnitude; positive whether delta is ±
            |
          > 0.5               threshold — normal advance ≪ 0.5, wrap ≈ 1
            |
      [out 1 trig]            1-sample pulse at each wrap

  This is exactly the `abs → delta → comparator` chain in example 14
  (ramp_to_trig, pp.39-41 midway), which anticipated the formal derivation.

  p.45 — go.ramp2trig.gendsp
  ---------------------------
  The gendsp extraction wraps the maxpat circuit as a reusable processor.
  It takes a ramp as audio-in and emits the trigger signal.  No params.

  The processor form here differs from example 14 (which was self-contained,
  generating its own BPM ramp):
    - example 14: generator form — internal phasor, :trig and :ramp outputs
    - this example: processor form — (audio-in) ramp, single trigger output

  The processor form is what go.ramp2trig.gendsp actually is in the GSOT
  library: a black box that consumes any [0,1) ramp and produces a gate.

  `:comparator` port semantics
  ----------------------------
  `(comparator x threshold)` returns a port-map `{:out gate :inv-gate inv}`.
  For go.ramp2trig only the `:out` port is needed — the trigger gate.
  The `:inv-gate` is used by go.ramp2slope (example 17) to condition the
  delta hold.  These two utilities are complementary consumers of the same
  comparator result.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n4
        with {
          n1 = (n0 - n0');
          n2 = abs(n1);
          n3 = 0.5;
          n4 = (float(n2 > n3));
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! ramp-to-trig-gendsp
  {}
  (let [ramp (audio-in)
        cmp  (comparator (abs (delta ramp)) (const 0.5))]
    (output (:out cmp))))
