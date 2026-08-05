; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.121-biquad4-res
  "GSOT pp.182-183 — go.biquad4.res (Chapter 6).

  'Cascades — Fourth-Order Resonant / Peaking EQ'
  -------------------------------------------------
  Two identical peaking EQ stages in series.  Squaring the transfer
  function doubles the boost/cut in dB:

      gain_4th = 2 × gain_2nd (in dB)

  A cascade of peaking EQ stages at the same frequency creates a sharper,
  steeper bell curve — useful for precise harmonic sculpting.

  Parameters
  ----------
  :hz   — centre frequency in Hz (20–20000; default 1000)
  :q    — bandwidth Q for both stages (0.1–20; default 1.0)
  :gain — gain per stage in dB (−40–40; total boost = 2×gain; default 0)

  Audio inputs  / Outputs
  -----------------------
  in: signal to filter  →  :out: 4th-order peaking EQ output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! biquad4-res
  {:params {:hz   {:range [20.0 20000.0] :default 1000.0}
            :q    {:range [0.1 20.0]    :default 1.0}
            :gain {:range [-40.0 40.0]  :default 0.0}}}
  (let [audio (audio-in)
        hz    (param :hz)
        q     (param :q)
        gain  (param :gain)
        w0    (faust "2.0*ma.PI*%{hz}/ma.SR" {:hz hz})
        cw    (faust "cos(%{w0})" {:w0 w0})
        sw    (faust "sin(%{w0})" {:w0 w0})
        al    (faust "%{sw}/(2.0*%{qq})"         {:sw sw :qq q})
        ga    (faust "pow(10.0,%{gn}/40.0)"    {:gn gain})
        a0    (faust "1.0+%{al}/%{ga}"           {:al al :ga ga})
        b0    (faust "(1.0+%{al}*%{ga})/%{a0}"    {:al al :ga ga :a0 a0})
        b1    (faust "-2.0*%{cw}/%{a0}"          {:cw cw :a0 a0})
        b2    (faust "(1.0-%{al}*%{ga})/%{a0}"    {:al al :ga ga :a0 a0})
        a1    (faust "-2.0*%{cw}/%{a0}"          {:cw cw :a0 a0})
        a2    (faust "(1.0-%{al}/%{ga})/%{a0}"    {:al al :ga ga :a0 a0})
        w1    (faust "(%{in}-%{a1}*_-%{a2}*_@1)~_"            {:in audio :a1 a1 :a2 a2})
        s1    (faust "%{b0}*%{ww}+%{b1}*%{ww}@1+%{b2}*%{ww}@2"     {:b0 b0 :b1 b1 :b2 b2 :ww w1})
        w2    (faust "(%{in}-%{a1}*_-%{a2}*_@1)~_"            {:in s1 :a1 a1 :a2 a2})
        out   (faust "%{b0}*%{ww}+%{b1}*%{ww}@1+%{b2}*%{ww}@2"     {:b0 b0 :b1 b1 :b2 b2 :ww w2})]
    (output :out out)))
