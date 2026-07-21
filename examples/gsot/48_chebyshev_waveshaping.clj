; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.48-chebyshev-waveshaping
  "GSOT pp.81-83 — bipolar_waveshaping_chebyshev.maxpat.

  'Audio waveshaping — polynomial shapers' (Chapter 3)
  -----------------------------------------------------
  Chebyshev polynomials of the first kind are the canonical waveshaping tool
  for additive harmonic synthesis.  Their defining property:

      If x = cos(θ),  then  Tn(x) = cos(nθ).

  Feeding a full-amplitude cosine through Tn extracts exactly the nth harmonic
  in isolation.  A weighted sum of T1..Tn applied to a sine oscillator is
  equivalent to additive synthesis of those harmonics.

  This ONLY holds when the input has amplitude exactly 1 (peaks at ±1).
  At lower amplitudes, each Tn generates a spread of harmonics — useful as
  a driven-distortion effect but not clean additive synthesis.

  Recurrence relation
  -------------------
  T0(x) = 1
  T1(x) = x
  T_{n+1}(x) = 2x·Tn(x) − T_{n-1}(x)

  Expanded formulas (for reference):
    T2(x) = 2x² − 1
    T3(x) = 4x³ − 3x
    T4(x) = 8x⁴ − 8x² + 1
    T5(x) = 16x⁵ − 20x³ + 5x
    T6(x) = 32x⁶ − 48x⁴ + 18x² − 1
    T7(x) = 64x⁷ − 112x⁵ + 56x³ − 7x

  Symmetry:
    Odd  n → Tn is an odd function  (Tn(−x) = −Tn(x)) — odd harmonics
    Even n → Tn is an even function (Tn(−x) =  Tn(x)) — even harmonics
  T1 and T3 are always odd; T2 and T4 are always even.  Mixing odd-degree
  Chebyshevs preserves bipolar symmetry.

  Outputs — seven individual harmonic shapers
  -------------------------------------------
  :t1 ... :t7   each Tn(x), n=1..7

  To synthesize a specific harmonic spectrum, sum the outputs with weights:
      out = a1·T1 + a2·T2 + ... + a7·T7
  where each an controls the amplitude of the nth harmonic.

  Implementation: recurrence built node-by-node.  T0=1 is inlined into the
  T2 formula (avoids a constant-only node); T3 onwards follow the pattern
  T_{n+1} = 2x·Tn − T_{n-1} referencing the two preceding Alembic nodes.

  Emitted Faust DSP:
      alembic_dsp(n0) = n0, n1, n2, n3, n4, n5, n6
        with {
          n1 = 2.0*n0*n0-1.0;
          n2 = 2.0*n0*n1-n0;
          n3 = 2.0*n0*n2-n1;
          n4 = 2.0*n0*n3-n2;
          n5 = 2.0*n0*n4-n3;
          n6 = 2.0*n0*n5-n4;
        };
      process = alembic_dsp;

  n0     = x  / T1 — fundamental (audio-in pass-through)
  n1..n6 = T2..T7"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! chebyshev-waveshaping
  {}
  (let [x  (audio-in)
        ; Recurrence: T_{n+1}(x) = 2x·Tn(x) − T_{n-1}(x)
        ; T0 = 1 (inlined), T1 = x (audio-in)
        t2 (faust "2.0*%xx*%xx-1.0"    {:xx x})
        t3 (faust "2.0*%xx*%t2-%xx"    {:xx x :t2 t2})
        t4 (faust "2.0*%xx*%t3-%t2"    {:xx x :t3 t3 :t2 t2})
        t5 (faust "2.0*%xx*%t4-%t3"    {:xx x :t4 t4 :t3 t3})
        t6 (faust "2.0*%xx*%t5-%t4"    {:xx x :t5 t5 :t4 t4})
        t7 (faust "2.0*%xx*%t6-%t5"    {:xx x :t6 t6 :t5 t5})]
    (output :t1 x)
    (output :t2 t2)
    (output :t3 t3)
    (output :t4 t4)
    (output :t5 t5)
    (output :t6 t6)
    (output :t7 t7)))
