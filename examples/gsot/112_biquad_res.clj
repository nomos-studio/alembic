; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.112-biquad-res
  "GSOT pp.181 — go.biquad.res (Chapter 6).

  'Biquad Filter Coefficients — Resonant / Peaking EQ'
  ------------------------------------------------------
  Second-order peaking equaliser (resonant EQ band).  Adds or subtracts
  gain at a centre frequency; flat response outside the boosted/cut region.

  Coefficient formulas (A = 10^(gain_dB/40)):
      w0    = 2π·fc/SR
      alpha = sin(w0)/(2·Q)
      A     = 10^(gain/40)
      b0    = (1 + alpha·A) / a0
      b1    = −2·cos(w0)   / a0
      b2    = (1 − alpha·A) / a0
      a0    = 1 + alpha/A             ← denominator uses alpha/A
      a1    = −2·cos(w0)   / a0
      a2    = (1 − alpha/A) / a0

  Positive gain → boost; negative gain → cut.  At gain=0: flat (identity).
  The denominator uses alpha/A (not alpha·A), so numerator and denominator
  poles/zeros are dual: the zeros shape the boost/cut, the poles return
  the response to flat.

  Parameters
  ----------
  :hz   — centre frequency in Hz (20–20000; default 1000)
  :q    — bandwidth Q (0.1–20; default 1.0)
  :gain — peak gain in dB (−40–40; default 0)

  Audio inputs  / Outputs
  -----------------------
  in: signal to filter  →  :out: resonant-EQ filtered output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! biquad-res
  {:params {:hz   {:range [20.0 20000.0] :default 1000.0}
            :q    {:range [0.1 20.0]    :default 1.0}
            :gain {:range [-40.0 40.0]  :default 0.0}}}
  (let [audio (audio-in)
        hz    (param :hz)
        q     (param :q)
        gain  (param :gain)
        w0    (faust "2.0*ma.PI*%hz/ma.SR" {:hz hz})
        cw    (faust "cos(%w0)" {:w0 w0})
        sw    (faust "sin(%w0)" {:w0 w0})
        al    (faust "%sw/(2.0*%qq)" {:sw sw :qq q})
        ga    (faust "pow(10.0,%gn/40.0)" {:gn gain})
        a0    (faust "1.0+%al/%ga" {:al al :ga ga})
        b0    (faust "(1.0+%al*%ga)/%a0" {:al al :ga ga :a0 a0})
        b1    (faust "-2.0*%cw/%a0"      {:cw cw :a0 a0})
        b2    (faust "(1.0-%al*%ga)/%a0" {:al al :ga ga :a0 a0})
        a1    (faust "-2.0*%cw/%a0"      {:cw cw :a0 a0})
        a2    (faust "(1.0-%al/%ga)/%a0" {:al al :ga ga :a0 a0})
        w     (faust "(%in-%a1*_-%a2*_@1)~_"            {:in audio :a1 a1 :a2 a2})
        out   (faust "%b0*%ww+%b1*%ww@1+%b2*%ww@2" {:b0 b0 :b1 b1 :b2 b2 :ww w})]
    (output :out out)))
