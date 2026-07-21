; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.45-window-fixed
  "GSOT pp.78-79 — fixed window envelope functions (no parameters).

  'Window envelope functions' (Chapter 3)
  -----------------------------------------
  Window functions shape the amplitude envelope of a buffer or grain.
  Unlike unit shapers (ramp → shaped ramp), window functions take a
  TRIANGLE input: 0 at the edges, 1 at the center.  Feeding the triangle
  output of a phasor into these functions produces a symmetric envelope.

  Triangle input convention
  -------------------------
  t ∈ [0, 1]: t=0 at window edge, t=1 at window center.
  In gen~: `phasor → triangle → window_function`.
  In Alembic: audio-in provides the pre-computed triangle signal.

  Cosine-sum substitution
  -----------------------
  All cosine-sum windows are defined in the time-position domain as:
      w(x) = a0 - a1·cos(2πx/N) + a2·cos(4πx/N) - …   x ∈ [0, N]

  With triangle t = 1 - |2x/N - 1| (0 at edges, 1 at x=N/2):
      2πx/N → πt   (the half-period substitution)

  So: cos(2πx/N) → cos(πt), cos(4πx/N) → cos(2πt), etc.
  The cosines c1..c4 are computed once and shared across all nine windows.

  Shared cosine intermediates: c1=cos(πt), c2=cos(2πt), c3=cos(3πt), c4=cos(4πt).

  Window formulas (triangle input t):
    hann            = 0.5 - 0.5·c1
    hamming         = 0.54 - 0.46·c1
    blackman        = 0.42 - 0.5·c1 + 0.08·c2
    blackman-harris = 0.35875 - 0.48829·c1 + 0.14128·c2 - 0.01168·c3
    blackman-nuttall= 0.3635819 - 0.4891775·c1 + 0.1365995·c2 - 0.0106411·c3
    nuttall         = 0.355768 - 0.487396·c1 + 0.144232·c2 - 0.012604·c3
    flat-top        = (1 - 1.93·c1 + 1.29·c2 - 0.388·c3 + 0.028·c4) / 4.636
    welch           = t·(2-t)   [= 1-(1-t)² in window time coords]
    parzen          = piecewise cubic (see below)

  Flat-top normalization: the unnormalized sum at t=1 is 1+1.93+1.29+0.388+0.028=4.636;
  dividing yields peak=1.  The flat-top window does NOT go to 0 at the edges
  (unnormalized edge ≈ 0); the normalized edge ≈ 0.

  Welch domain note: in ramp-domain (example 35) go.unit.welch = 4x(1-x),
  peaking at x=0.5.  In triangle domain, Welch(t) = t(2-t) = 1-(1-t)²,
  which is a monotone-rising function on [0,1] — the triangle input then
  produces the symmetric window envelope as t cycles 0→1→0.

  Parzen (de la Vallée-Poussin):
      u = 1 - t   (normalized distance from center)
      u ≤ 0.5  (center region, t ≥ 0.5): 1 - 6u² + 6u³
      u  > 0.5 (edge   region, t  < 0.5): 2·(1-u)³ = 2·t³
  Continuity at u=0.5: both formulas give 0.25. ✓

  Emitted Faust DSP (abbreviated — 15 nodes)
  -------------------------------------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n5, n6, n7, n8, n9, n10, n11, n12, n13
        with {
          n1 = cos(ma.PI*n0);
          n2 = cos(2.0*ma.PI*n0);
          n3 = cos(3.0*ma.PI*n0);
          n4 = cos(4.0*ma.PI*n0);
          n5 = 0.5 - 0.5*n1;
          n6 = 0.54 - 0.46*n1;
          n7 = 0.42 - 0.5*n1 + 0.08*n2;
          n8 = 0.35875 - 0.48829*n1 + 0.14128*n2 - 0.01168*n3;
          n9 = 0.3635819 - 0.4891775*n1 + 0.1365995*n2 - 0.0106411*n3;
          n10 = 0.355768 - 0.487396*n1 + 0.144232*n2 - 0.012604*n3;
          n11 = (1.0-1.93*n1+1.29*n2-0.388*n3+0.028*n4)/4.636;
          n12 = n0*(2.0-n0);
          n13 = select2(float(n0>=0.5)>0.5,
                        2.0*n0*n0*n0,
                        1.0-6.0*(1.0-n0)*(1.0-n0)+6.0*(1.0-n0)*(1.0-n0)*(1.0-n0));
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! window-fixed
  {}
  (let [t  (audio-in)
        ; shared cosine harmonics (computed once, reused across all windows)
        c1 (faust "cos(ma.PI*%tt)"       {:tt t})
        c2 (faust "cos(2.0*ma.PI*%tt)"   {:tt t})
        c3 (faust "cos(3.0*ma.PI*%tt)"   {:tt t})
        c4 (faust "cos(4.0*ma.PI*%tt)"   {:tt t})
        ; cosine-sum windows
        hann    (faust "0.5 - 0.5*%c"     {:c c1})
        hamming (faust "0.54 - 0.46*%c"   {:c c1})
        blackman (faust "0.42 - 0.5*%c1 + 0.08*%c2" {:c1 c1 :c2 c2})
        bh       (faust "0.35875 - 0.48829*%c1 + 0.14128*%c2 - 0.01168*%c3"
                        {:c1 c1 :c2 c2 :c3 c3})
        bn       (faust "0.3635819 - 0.4891775*%c1 + 0.1365995*%c2 - 0.0106411*%c3"
                        {:c1 c1 :c2 c2 :c3 c3})
        nuttall  (faust "0.355768 - 0.487396*%c1 + 0.144232*%c2 - 0.012604*%c3"
                        {:c1 c1 :c2 c2 :c3 c3})
        flattop  (faust "(1.0 - 1.93*%c1 + 1.29*%c2 - 0.388*%c3 + 0.028*%c4) / 4.636"
                        {:c1 c1 :c2 c2 :c3 c3 :c4 c4})
        ; welch: t*(2-t) = 1-(1-t)^2 in window (triangle) coordinates
        welch    (faust "%tt*(2.0-%tt)" {:tt t})
        ; parzen (de la Vallée-Poussin): piecewise cubic
        ; center half (t >= 0.5, u=1-t <= 0.5): 1 - 6u^2 + 6u^3  where u=1-t
        ; edge  half (t  < 0.5, u > 0.5):       2*t^3             (= 2*(1-u)^3)
        parzen   (faust "select2(float(%tt>=0.5)>0.5, 2.0*%tt*%tt*%tt, 1.0-6.0*(1.0-%tt)*(1.0-%tt)+6.0*(1.0-%tt)*(1.0-%tt)*(1.0-%tt))"
                        {:tt t})]
    (output :hann            hann)
    (output :hamming         hamming)
    (output :blackman        blackman)
    (output :blackman-harris bh)
    (output :blackman-nuttall bn)
    (output :nuttall         nuttall)
    (output :flat-top        flattop)
    (output :welch           welch)
    (output :parzen          parzen)))
