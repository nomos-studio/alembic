; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.114-biquad-ap
  "GSOT pp.181 — go.biquad.ap (Chapter 6).

  'Biquad Filter Coefficients — Second-Order Allpass'
  ----------------------------------------------------
  Second-order allpass biquad: flat magnitude response, frequency-
  dependent phase shift concentrated around fc.  The complement of go.biquad.np:
  where the notch places zeros on the unit circle, the AP places them as
  the reciprocals of the poles (outside the unit circle), keeping |H|=1.

  Coefficient formulas (normalised by a0 = 1 + alpha):
      w0    = 2π·fc/SR
      alpha = sin(w0)/(2·Q)
      a0    = 1 + alpha
      b0    = (1 − alpha) / a0            [= a2]
      b1    = −2·cos(w0) / a0             [= a1]
      b2    = (1 + alpha) / a0 = 1.0      [numerically: a0/a0]
      a1    = −2·cos(w0) / a0
      a2    = (1 − alpha) / a0            [= b0]

  Since b2 = a0/a0 = 1, the output simplifies to:
      y[n] = b0·w[n] + b1·w[n-1] + w[n-2]

  The 2nd-order AP is the building block for 2nd-order phaser stages
  and Hilbert transformer networks.  At fc, the phase shift is −π.

  Parameters
  ----------
  :hz — phase transition frequency in Hz (20–20000; default 1000)
  :q  — phase transition sharpness Q (0.1–20; default 0.707)

  Audio inputs  / Outputs
  -----------------------
  in: signal to filter  →  :out: allpass filtered output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! biquad-ap
  {:params {:hz {:range [20.0 20000.0] :default 1000.0}
            :q  {:range [0.1 20.0]    :default 0.707}}}
  (let [audio (audio-in)
        hz    (param :hz)
        q     (param :q)
        w0    (faust "2.0*ma.PI*%{hz}/ma.SR" {:hz hz})
        cw    (faust "cos(%{w0})" {:w0 w0})
        sw    (faust "sin(%{w0})" {:w0 w0})
        al    (faust "%{sw}/(2.0*%{qq})" {:sw sw :qq q})
        a0    (faust "1.0+%{al}" {:al al})
        b0    (faust "(1.0-%{al})/%{a0}" {:al al :a0 a0})
        b1    (faust "-2.0*%{cw}/%{a0}"  {:cw cw :a0 a0})
        a1    (faust "-2.0*%{cw}/%{a0}"  {:cw cw :a0 a0})
        a2    (faust "(1.0-%{al})/%{a0}" {:al al :a0 a0})
        w     (faust "(%{in}-%{a1}*_-%{a2}*_@1)~_"         {:in audio :a1 a1 :a2 a2})
        out   (faust "%{b0}*%{ww}+%{b1}*%{ww}@1+%{ww}@2" {:b0 b0 :b1 b1 :ww w})]
    (output :out out)))
