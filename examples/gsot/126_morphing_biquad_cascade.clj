; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.126-morphing-biquad-cascade
  "GSOT pp.182-183 — morphing_biquad_cascade.maxpat (Chapter 6).

  'Morphing Biquad Cascade — LP↔HP'
  -----------------------------------
  A 4th-order filter cascade whose character sweeps continuously from
  lowpass to highpass via an intermediate bandpass-like response.  A single
  :morph parameter [0,1] controls the spectral tilt.

  Why LP↔HP morphing is analytic
  --------------------------------
  LP and HP biquads share the same feedback coefficients a1 and a2 (both
  depend only on cos(w0) and alpha).  Only b0 and b1 differ:

      LP: b0=(1−cw)/(2a0), b1=(1−cw)/a0
      HP: b0=(1+cw)/(2a0), b1=−(1+cw)/a0

  Linear interpolation at morph m ∈ [0,1]:

      b0(m) = (1−m)·b0_LP + m·b0_HP
            = [(1−m)(1−cw) + m(1+cw)] / (2a0)
            = [1 + cw·(2m−1)] / (2a0)

      b1(m) = (1−m)·b1_LP + m·b1_HP
            = [(1−m)(1−cw) − m(1+cw)] / a0
            = [1 − cw − 2m] / a0

      a1, a2 unchanged: −2·cw/a0 and (1−alpha)/a0 respectively

  These are computed directly from m without maintaining two separate
  coefficient sets.

  Spectral character by morph value
  -----------------------------------
  m = 0.0 : LP  — low frequencies pass; hi-frequency roll-off
  m = 0.25: LP-leaning tilt with broadened pass region
  m = 0.5 : bandpass-like  — b0=1/(2a0), b1=(1−cw−1)/a0=−cw/a0
  m = 0.75: HP-leaning tilt with broadened stop region
  m = 1.0 : HP  — high frequencies pass; low-frequency roll-off

  The cascade (two identical morphed stages) doubles the roll-off steepness
  and intensifies the transition between character regions.  At m=0.5 the
  cascade creates a tighter band emphasis than a single morphed stage.

  Implementation
  --------------
  1. Shared trig/coefficient intermediates: w0, cw, sw, alpha, a0
  2. Closed-form morphed coefficients: b0m, b1m (no select2 needed)
  3. Shared a1, a2 (same as LP and HP)
  4. Two DF-II biquad stages with the morphed coefficient set

  Parameters
  ----------
  :hz    — cutoff/centre frequency in Hz (20–20000; default 1000)
  :q     — resonance Q (0.1–20; default 0.707)
  :morph — LP(0)↔HP(1) crossfade (0.0–1.0; default 0.0)

  Audio inputs  / Outputs
  -----------------------
  in: signal to filter  →  :out: morphed 4th-order output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! morphing-biquad-cascade
  {:params {:hz    {:range [20.0 20000.0] :default 1000.0}
            :q     {:range [0.1 20.0]    :default 0.707}
            :morph {:range [0.0 1.0]     :default 0.0}}}
  (let [audio (audio-in)
        hz    (param :hz)
        q     (param :q)
        morph (param :morph)
        ; Shared intermediates
        w0    (faust "2.0*ma.PI*%{hz}/ma.SR" {:hz hz})
        cw    (faust "cos(%{w0})" {:w0 w0})
        sw    (faust "sin(%{w0})" {:w0 w0})
        al    (faust "%{sw}/(2.0*%{qq})" {:sw sw :qq q})
        a0    (faust "1.0+%{al}" {:al al})
        ; Morphed b0 = [1 + cw*(2*morph-1)] / (2*a0)
        b0m   (faust "(1.0+%{cw}*(2.0*%{mo}-1.0))/(2.0*%{a0})" {:cw cw :mo morph :a0 a0})
        ; Morphed b1 = [1 - cw - 2*morph] / a0
        b1m   (faust "(1.0-%{cw}-2.0*%{mo})/%{a0}" {:cw cw :mo morph :a0 a0})
        ; Shared feedback coefficients (same for LP and HP)
        a1    (faust "-2.0*%{cw}/%{a0}" {:cw cw :a0 a0})
        a2    (faust "(1.0-%{al})/%{a0}" {:al al :a0 a0})
        ; Stage 1
        w1    (faust "(%{in}-%{a1}*_-%{a2}*_@1)~_"         {:in audio :a1 a1 :a2 a2})
        s1    (faust "%{b0}*%{ww}+%{b1}*%{ww}@1+%{b0}*%{ww}@2"  {:b0 b0m :b1 b1m :ww w1})
        ; Stage 2
        w2    (faust "(%{in}-%{a1}*_-%{a2}*_@1)~_"         {:in s1 :a1 a1 :a2 a2})
        out   (faust "%{b0}*%{ww}+%{b1}*%{ww}@1+%{b0}*%{ww}@2"  {:b0 b0m :b1 b1m :ww w2})]
    (output :out out)))
