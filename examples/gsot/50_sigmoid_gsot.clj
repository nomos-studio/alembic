; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.50-sigmoid-gsot
  "GSOT pp.84-85 — go.sigmoid.* and go.sigmoid2.

  'Audio waveshaping — sigmoids' (Chapter 3) — named GSOT abstractions
  ---------------------------------------------------------------------
  Six sigmoid flavors sharing a common drive (k) parameter.  All are odd
  functions (f(−x) = −f(x)) and approach ±1 asymptotically (except softclip
  which reaches ±1 exactly at |kx|=1).

  go.sigmoid.tanh  (output :tanh)
  --------------------------------
  1 − 2/(1+exp(2kx)) — exact tanh identity; Faust has no tanh primitive.
  The reference sigmoid: fastest to ±1 among the smooth shapes.
  All odd harmonics; even harmonics suppressed by odd symmetry.

  go.sigmoid.logistic  (output :logistic)
  ----------------------------------------
  2/(1+exp(−kx)) − 1  — bipolar (centered) logistic function.
  Mathematically: logistic(kx) = tanh(kx/2).  Same curve shape as tanh
  but with half the effective drive; at the same k, the logistic knee is
  softer than tanh.

  go.sigmoid2  (output :sigmoid2)
  --------------------------------
  kx/(1+|kx|) — rational sigmoid (a.k.a. softsign, Padé approximant).
  Algebraic: no transcendental function needed.  C∞ except the second
  derivative has a kink at x=0 (from abs).  Approaches ±1 most slowly
  of the transcendental-free shapes.  At drive=4, x=1: output=4/5=0.8
  (does not reach ±1 in the audio range — intentionally soft).

  go.sigmoid.guderman  (output :gudermann)
  ------------------------------------------
  (4/π)·atan(exp(kx)) − 1 — the normalized Gudermannian function.
  gd(x) = 2·atan(exp(x)) − π/2  maps ℝ → (−π/2, π/2); normalize by ÷(π/2).
  Key identity: sin(gd(x)) = tanh(x).  The Gudermannian connects circular
  and hyperbolic functions: same zero crossing and odd symmetry as tanh but
  a distinctly different harmonic profile near the knee.
  Implementation avoids sinh (not a Faust primitive) via the atan(exp) form.

  go.sigmoid.atan  (output :atan)
  --------------------------------
  (2/π)·atan(kx) — arctan sigmoid normalized to [−1,1].
  Softest knee of all six: approaches ±1 most gradually, never fully
  reaching the limit within finite amplitude.  Infinite harmonic series
  of odd harmonics decaying as 1/(2n−1).

  go.sigmoid.softclip  (output :softclip)
  ------------------------------------------
  Cubic soft clip:
      |kx| ≤ 1 → 1.5·kx − 0.5·(kx)³   (cubic, C∞ on this region)
      |kx| > 1 → ±1                      (hard clip, select2 branch)
  Both branches agree at |kx|=1: 1.5−0.5=1. ✓  Continuous but not C¹ at
  |kx|=1 (slope transitions from 0 to 1 at the boundary).
  Note: select2 evaluates both branches at sample rate (not lazy); the
  cubic branch has no singularity so this is safe.

  Knee hardness (from softest to hardest at same k):
      atan < sigmoid2 < gudermann < logistic < tanh < softclip

  Shared intermediate: kx = k·x (all six applied to the same driven signal).
  tanh uses an additional exp(2kx) node; gudermann uses exp(kx).

  Emitted Faust DSP (abbreviated — 11 nodes):
      import(\"stdfaust.lib\");

      n0 = hslider(\"drive\", 4.0, 1.0, 16.0, 0.0015);

      alembic_dsp(n1) = n4, n5, n6, n8, n9, n10
        with {
          n2  = n0*n1;
          n3  = exp(2.0*n2);
          n4  = 1.0-2.0/(1.0+n3);
          n5  = 2.0/(1.0+exp(0.0-n2))-1.0;
          n6  = n2/(1.0+abs(n2));
          n7  = exp(n2);
          n8  = (4.0/ma.PI)*atan(n7)-1.0;
          n9  = (2.0/ma.PI)*atan(n2);
          n10 = select2(float(abs(n2)>1.0)>0.5,
                        1.5*n2-0.5*n2*n2*n2,
                        max(-1.0,min(1.0,n2)));
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! sigmoid-gsot
  {:params {:drive {:range [1.0 16.0] :default 4.0}}}
  (let [x    (audio-in)
        k    (param :drive)
        kx   (faust "%{kk}*%{xx}" {:kk k :xx x})
        ; go.sigmoid.tanh — 1-2/(1+exp(2kx)); Faust has no tanh primitive
        e2kx     (faust "exp(2.0*%{kx})"              {:kx kx})
        tanh-out (faust "1.0-2.0/(1.0+%{ex})"         {:ex e2kx})
        ; go.sigmoid.logistic — 2/(1+exp(-kx))-1; 0.0- avoids unary minus
        logistic-out (faust "2.0/(1.0+exp(0.0-%{kx}))-1.0" {:kx kx})
        ; go.sigmoid2 — rational softsign: kx/(1+|kx|)
        sig2-out (faust "%{kx}/(1.0+abs(%{kx}))"        {:kx kx})
        ; go.sigmoid.guderman — (4/π)*atan(exp(kx))-1 via gd(x)=2*atan(exp(x))-π/2
        ekx      (faust "exp(%{kx})"                   {:kx kx})
        gude-out (faust "(4.0/ma.PI)*atan(%{ex})-1.0" {:ex ekx})
        ; go.sigmoid.atan — (2/π)*atan(kx)
        atan-out (faust "(2.0/ma.PI)*atan(%{kx})"     {:kx kx})
        ; go.sigmoid.softclip — cubic for |kx|≤1, hard clip outside
        soft-out (faust "select2(float(abs(%{kx})>1.0)>0.5,1.5*%{kx}-0.5*%{kx}*%{kx}*%{kx},max(-1.0,min(1.0,%{kx})))"
                        {:kx kx})]
    (output :tanh      tanh-out)
    (output :logistic  logistic-out)
    (output :sigmoid2  sig2-out)
    (output :gudermann gude-out)
    (output :atan      atan-out)
    (output :softclip  soft-out)))
