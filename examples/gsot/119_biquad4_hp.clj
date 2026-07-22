; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.119-biquad4-hp
  "GSOT pp.182-183 — go.biquad4.hp (Chapter 6).

  'Cascades — Fourth-Order Highpass'
  ------------------------------------
  Two identical HP biquad stages in series.  Coefficient structure mirrors
  go.biquad4.lp but uses HP formulas: zeros at DC (z=+1) rather than Nyquist.

  H4(z) = H2_hp(z)² — roll-off −80 dB/decade above fc, −24 dB/octave.

  Parameters
  ----------
  :hz — cutoff frequency in Hz (20–20000; default 1000)
  :q  — resonance Q for both stages (0.1–20; default 0.707)

  Audio inputs  / Outputs
  -----------------------
  in: signal to filter  →  :out: 4th-order highpass filtered output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! biquad4-hp
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
        w1    (faust "(%in-%a1*_-%a2*_@1)~_"        {:in audio :a1 a1 :a2 a2})
        s1    (faust "%b0*%ww+%b1*%ww@1+%b0*%ww@2" {:b0 b0 :b1 b1 :ww w1})
        w2    (faust "(%in-%a1*_-%a2*_@1)~_"        {:in s1 :a1 a1 :a2 a2})
        out   (faust "%b0*%ww+%b1*%ww@1+%b0*%ww@2" {:b0 b0 :b1 b1 :ww w2})]
    (output :out out)))
