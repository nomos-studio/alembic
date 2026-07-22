; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.111-biquad-bp
  "GSOT pp.181 — go.biquad.bp (Chapter 6).

  'Biquad Filter Coefficients — Bandpass'
  ----------------------------------------
  Second-order bandpass biquad (constant skirt gain; peak gain = Q).

  Coefficient formulas (normalised by a0 = 1 + alpha):
      w0    = 2π·fc/SR
      alpha = sin(w0)/(2·Q)
      a0    = 1 + alpha
      b0    = sin(w0) / (2·a0)           [= Q·alpha/a0]
      b1    = 0
      b2    = −sin(w0) / (2·a0)          [= −b0]
      a1    = −2·cos(w0) / a0
      a2    = (1 − alpha) / a0

  Since b1=0 and b2=−b0, the output simplifies to:
      y[n] = b0·(w[n] − w[n-2])

  The bandwidth at −3 dB is fc/Q (octaves).  Peak magnitude = Q at fc.
  Higher Q → narrower band and higher peak; lower Q → wider pass band.

  Parameters
  ----------
  :hz — center frequency in Hz (20–20000; default 1000)
  :q  — bandwidth Q (0.1–20; default 1.0)

  Audio inputs  / Outputs
  -----------------------
  in: signal to filter  →  :out: bandpass filtered output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! biquad-bp
  {:params {:hz {:range [20.0 20000.0] :default 1000.0}
            :q  {:range [0.1 20.0]    :default 1.0}}}
  (let [audio (audio-in)
        hz    (param :hz)
        q     (param :q)
        w0    (faust "2.0*ma.PI*%hz/ma.SR" {:hz hz})
        cw    (faust "cos(%w0)" {:w0 w0})
        sw    (faust "sin(%w0)" {:w0 w0})
        al    (faust "%sw/(2.0*%qq)" {:sw sw :qq q})
        a0    (faust "1.0+%al" {:al al})
        b0    (faust "%sw/(2.0*%a0)"          {:sw sw :a0 a0})
        a1    (faust "-2.0*%cw/%a0"           {:cw cw :a0 a0})
        a2    (faust "(1.0-%al)/%a0"          {:al al :a0 a0})
        w     (faust "(%in-%a1*_-%a2*_@1)~_" {:in audio :a1 a1 :a2 a2})
        out   (faust "%b0*(%ww-%ww@2)"        {:b0 b0 :ww w})]
    (output :out out)))
