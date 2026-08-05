; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.49-sigmoid-waveshaping
  "GSOT pp.84-85 — sigmoid waveshaping.

  'Audio waveshaping — sigmoids' (Chapter 3)
  -------------------------------------------
  Sigmoid functions map ℝ → (−1, 1) with an S-curve that compresses large
  amplitudes while leaving small amplitudes linear.  Applied to audio they
  implement controlled soft saturation.

  Drive model
  -----------
  A gain (drive, k) multiplies the input before shaping:

      out = f(k · x)

  For small |x|, f(kx) ≈ k·x — gain is k.
  For large |x|, f(kx) → ±1 — hard compression.

  Increasing k sharpens the knee and raises the small-signal gain.  In
  practice, output level is corrected afterwards (÷k or AGC); the examples
  here emit the raw driven form so the knee shape is directly audible.

  Normalized form (for reference, not emitted here):
      f(k·x) / f(k)   →   unity gain at x = ±1 regardless of drive
  For tanh: tanh(kx)/tanh(k).  Useful when integrating with downstream
  gain staging, but adds a division at sample rate.

  Sigmoid shapes (all odd functions: f(−x) = −f(x))
  ---------------------------------------------------
  tanh   — hyperbolic tangent.  C∞ everywhere.  Canonical tube/transistor
            soft-clipper character.  Implemented as 1−2/(1+exp(2x)) since
            Faust exposes no tanh primitive.

  Padé   — rational approximation kx/(1+|kx|).  C¹ at the inflection, C∞
            everywhere else.  Cheaper than tanh; very similar knee shape.
            Exact match to tanh in odd-harmonic content is not guaranteed
            but difference is inaudible at musical drive levels.

  sqrt   — kx/√(1+(kx)²).  C∞.  Slower knee than tanh at the same k;
            approaches ±1 more gradually.  Algebraic (no transcendental
            call) but requires a divide and sqrt.

  hard   — max(−1, min(1, kx)).  Brick-wall clip.  Limiting case of all
            smooth sigmoids as k→∞.  Produces all odd harmonics equally
            (Gibbs ringing); useful as a reference for the hardest limit.

  Comparison:
    knee hardness:  tanh  ≈  Padé  <  sqrt  <<  hard
    CPU:            Padé  <  sqrt  <  tanh   <  hard (branch)

  Faust note: tanh is not a primitive in Faust's language or stdfaust.lib.
  Implemented via the identity tanh(x) = 1 − 2/(1+exp(2x)) — exact, one
  exp call, no overflow in the drive range [1,16] (max kx=16, exp(32)≈7e13
  is well within float range).

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      n0 = hslider(\"drive\", 4.0, 1.0, 16.0, 0.0015);

      alembic_dsp(n1) = n4, n5, n6, n7
        with {
          n2 = n0*n1;
          n3 = exp(2.0*n2);
          n4 = 1.0-2.0/(1.0+n3);
          n5 = n2/(1.0+abs(n2));
          n6 = n2/sqrt(1.0+n2*n2);
          n7 = max(-1.0,min(1.0,n2));
        };
      process = alembic_dsp;

  n0 = drive param
  n1 = audio-in
  n2 = k·x  (shared driven signal)
  n3 = exp(2kx)  (tanh intermediate)
  n4 = tanh output  (1 − 2/(1+exp(2kx)))
  n5 = Padé output
  n6 = sqrt output
  n7 = hard-clip output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! sigmoid-waveshaping
  {:params {:drive {:range [1.0 16.0] :default 4.0}}}
  (let [x    (audio-in)
        k    (param :drive)
        kx   (faust "%{kk}*%{xx}" {:kk k :xx x})
        ; tanh: C∞ soft clip via 1-2/(1+exp(2x)) — Faust has no tanh primitive
        e2kx     (faust "exp(2.0*%{kx})"                 {:kx kx})
        tanh-out (faust "1.0-2.0/(1.0+%{ex})"            {:ex e2kx})
        ; Padé rational: kx/(1+|kx|) — algebraic, same odd symmetry
        pade-out (faust "%{kx}/(1.0+abs(%{kx}))"           {:kx kx})
        ; sqrt algebraic: kx/sqrt(1+(kx)²) — slower knee than tanh
        sqrt-out (faust "%{kx}/sqrt(1.0+%{kx}*%{kx})"       {:kx kx})
        ; hard clip: limiting case, brick-wall at ±1
        hard-out (faust "max(-1.0,min(1.0,%{kx}))"       {:kx kx})]
    (output :tanh tanh-out)
    (output :pade pade-out)
    (output :sqrt sqrt-out)
    (output :hard hard-out)))
