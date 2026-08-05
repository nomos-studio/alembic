; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.122-biquad4-np
  "GSOT pp.182-183 — go.biquad4.np (Chapter 6).

  'Cascades — Fourth-Order Notch'
  --------------------------------
  Two identical notch stages in series.  The cascade deepens the null at fc:
  a single notch has infinite rejection in theory (zeros on unit circle);
  the cascade doubles the zero order, making the null even more robust to
  coefficient quantisation and practical non-idealities.

  Parameters
  ----------
  :hz — notch centre frequency in Hz (20–20000; default 1000)
  :q  — notch Q for both stages (0.1–20; default 1.0)

  Audio inputs  / Outputs
  -----------------------
  in: signal to filter  →  :out: 4th-order notch filtered output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! biquad4-np
  {:params {:hz {:range [20.0 20000.0] :default 1000.0}
            :q  {:range [0.1 20.0]    :default 1.0}}}
  (let [audio (audio-in)
        hz    (param :hz)
        q     (param :q)
        w0    (faust "2.0*ma.PI*%{hz}/ma.SR" {:hz hz})
        cw    (faust "cos(%{w0})" {:w0 w0})
        sw    (faust "sin(%{w0})" {:w0 w0})
        al    (faust "%{sw}/(2.0*%{qq})" {:sw sw :qq q})
        a0    (faust "1.0+%{al}" {:al al})
        b0    (faust "1.0/%{a0}"                 {:a0 a0})
        b1    (faust "-2.0*%{cw}/%{a0}"            {:cw cw :a0 a0})
        a1    (faust "-2.0*%{cw}/%{a0}"            {:cw cw :a0 a0})
        a2    (faust "(1.0-%{al})/%{a0}"           {:al al :a0 a0})
        w1    (faust "(%{in}-%{a1}*_-%{a2}*_@1)~_"       {:in audio :a1 a1 :a2 a2})
        s1    (faust "%{b0}*%{ww}+%{b1}*%{ww}@1+%{b0}*%{ww}@2" {:b0 b0 :b1 b1 :ww w1})
        w2    (faust "(%{in}-%{a1}*_-%{a2}*_@1)~_"       {:in s1 :a1 a1 :a2 a2})
        out   (faust "%{b0}*%{ww}+%{b1}*%{ww}@1+%{b0}*%{ww}@2" {:b0 b0 :b1 b1 :ww w2})]
    (output :out out)))
