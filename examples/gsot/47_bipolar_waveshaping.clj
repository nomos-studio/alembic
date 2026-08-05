; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.47-bipolar-waveshaping
  "GSOT pp.79-81 — bipolar_waveshaping_unitshapers.maxpat.

  'Waveshaping bipolar signals' (Chapter 3)
  ------------------------------------------
  Unit shapers work on [0,1].  Audio and modulation signals are typically
  bipolar: [-1,1].  Bridging the gap requires one of two canonical mappings,
  each with distinct harmonic character.

  Approach 1 — symmetric (abs/sign, odd symmetry)
  ------------------------------------------------
  Map |x| into [0,1], apply shaper, restore polarity:

      u   = |x|
      s   = sign(x)  = float(x>0) − float(x<0)   (0 at x=0)
      out = s · f(u)

  Result: f(−x) = −f(x) — the output is an odd function of the input.
  Only odd harmonics (3rd, 5th, 7th, …) are generated.  Zero crossing
  is preserved.  Character: tube saturation, symmetric soft clipping.

  Approach 2 — full-range (shift-scale)
  -------------------------------------
  Shift [-1,1] to [0,1], apply shaper, shift back:

      un  = (x + 1) / 2
      out = f(un) · 2 − 1

  Result: both odd AND even harmonics.  Not an odd function (unless f is
  symmetric around 0.5).  Positive and negative cycles clip differently;
  audible asymmetry.

  Outputs (six — symmetric and full-range for each of three unit shapers)
  -----------------------------------------------------------------------
  :sym-pow    — sign(x)·max(0,|x|)^p           symmetric power law
  :sym-cubic  — sign(x)·(3u²−2u³)              symmetric smoothstep
  :sym-log    — sign(x)·σ(|x|)                 symmetric logistic
  :full-pow   — 2·max(0,un)^p − 1              full-range power law
  :full-cubic — 2·(3un²−2un³) − 1              full-range smoothstep
  :full-log   — 2·σ(un) − 1                    full-range logistic

  u = |x|,  un = (x+1)/2,  σ(v) = 1/(1+exp(−12(v−0.5)))  k=12 fixed.

  Emitted Faust DSP (abbreviated — 13 nodes):
      import(\"stdfaust.lib\");

      n0 = hslider(\"power\", 2.0, 0.5, 8.0, 0.00075);

      alembic_dsp(n1) = n8, n9, n10, n11, n12, n13
        with {
          n2  = abs(n1);
          n3  = float(n1>0.0)-float(n1<0.0);
          n4  = (n1+1.0)*0.5;
          n8  = n3*pow(max(0.0,n2),n0);
          n9  = n3*(3.0*n2*n2-2.0*n2*n2*n2);
          n10 = n3*(1.0/(1.0+exp(-12.0*(n2-0.5))));
          n11 = pow(max(0.0,n4),n0)*2.0-1.0;
          n12 = (3.0*n4*n4-2.0*n4*n4*n4)*2.0-1.0;
          n13 = (1.0/(1.0+exp(-12.0*(n4-0.5))))*2.0-1.0;
        };
      process = alembic_dsp;

  n2  = u    (|x|)
  n3  = sgn  (sign(x) via float difference — 0 at exactly x=0)
  n4  = un   ((x+1)/2 — unipolarised)
  n8..n10 = symmetric outputs (odd harmonics only)
  n11..n13 = full-range outputs (odd + even harmonics)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! bipolar-waveshaping
  {:params {:power {:range [0.5 8.0] :default 2.0}}}
  (let [x    (audio-in)
        u    (faust "abs(%{xx})"                           {:xx x})
        sgn  (faust "float(%{xx}>0.0)-float(%{xx}<0.0)"     {:xx x})
        un   (faust "(%{xx}+1.0)*0.5"                     {:xx x})
        ; ── symmetric: sign(x)·f(|x|) — odd harmonics only ─────────────────
        sym-pow  (faust "%{sg}*pow(max(0.0,%{uu}),%{pw})"
                        {:sg sgn :uu u :pw (param :power)})
        sym-cub  (faust "%{sg}*(3.0*%{uu}*%{uu}-2.0*%{uu}*%{uu}*%{uu})"
                        {:sg sgn :uu u})
        sym-log  (faust "%{sg}*(1.0/(1.0+exp(-12.0*(%{uu}-0.5))))"
                        {:sg sgn :uu u})
        ; ── full-range: 2·f((x+1)/2)−1 — odd + even harmonics ──────────────
        full-pow (faust "pow(max(0.0,%{un}),%{pw})*2.0-1.0"
                        {:un un :pw (param :power)})
        full-cub (faust "(3.0*%{un}*%{un}-2.0*%{un}*%{un}*%{un})*2.0-1.0"
                        {:un un})
        full-log (faust "(1.0/(1.0+exp(-12.0*(%{un}-0.5))))*2.0-1.0"
                        {:un un})]
    (output :sym-pow    sym-pow)
    (output :sym-cubic  sym-cub)
    (output :sym-log    sym-log)
    (output :full-pow   full-pow)
    (output :full-cubic full-cub)
    (output :full-log   full-log)))
