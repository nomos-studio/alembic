; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.110-biquad-hp
  "GSOT pp.181 — go.biquad.hp (Chapter 6).

  'Biquad Filter Coefficients — Highpass'
  ----------------------------------------
  Second-order highpass biquad.  Mirror of go.biquad.lp: zeros at DC
  (z = +1) instead of at Nyquist (z = −1).

  Coefficient formulas (normalised by a0 = 1 + alpha):
      w0    = 2π·fc/SR
      alpha = sin(w0)/(2·Q)
      a0    = 1 + alpha
      b0    = (1 + cos(w0)) / (2·a0)     [= b2]
      b1    = −(1 + cos(w0)) / a0        [= −2·b0]
      a1    = −2·cos(w0) / a0
      a2    = (1 − alpha) / a0

  LP vs HP symmetry: substituting cos → −cos flips the zero placement
  from z=−1 (Nyquist, LP) to z=+1 (DC, HP), yielding the complementary
  response.  At Q=0.707: −3 dB at the cutoff, flat above.

  Parameters
  ----------
  :hz — cutoff frequency in Hz (20–20000; default 1000)
  :q  — resonance Q (0.1–20; default 0.707)

  Audio inputs  / Outputs
  -----------------------
  in: signal to filter  →  :out: highpass filtered output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! biquad-hp
  {:params {:hz {:range [20.0 20000.0] :default 1000.0}
            :q  {:range [0.1 20.0]    :default 0.707}}}
  (let [audio (audio-in)
        hz    (param :hz)
        q     (param :q)
        w0    (faust "2.0*ma.PI*%hz/ma.SR" {:hz hz})
        cw    (faust "cos(%w0)" {:w0 w0})
        sw    (faust "sin(%w0)" {:w0 w0})
        al    (faust "%sw/(2.0*%qq)" {:sw sw :qq q})
        a0    (faust "1.0+%al" {:al al})
        b0    (faust "(1.0+%cw)/(2.0*%a0)"  {:cw cw :a0 a0})
        b1    (faust "-(1.0+%cw)/%a0"       {:cw cw :a0 a0})
        a1    (faust "-2.0*%cw/%a0"          {:cw cw :a0 a0})
        a2    (faust "(1.0-%al)/%a0"         {:al al :a0 a0})
        w     (faust "(%in-%a1*_-%a2*_@1)~_" {:in audio :a1 a1 :a2 a2})
        out   (faust "%b0*%ww+%b1*%ww@1+%b0*%ww@2" {:b0 b0 :b1 b1 :ww w})]
    (output :out out)))
