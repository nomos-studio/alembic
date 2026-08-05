; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.113-biquad-np
  "GSOT pp.181 — go.biquad.np (Chapter 6).

  'Biquad Filter Coefficients — Notch / Null Pass'
  -------------------------------------------------
  Second-order notch (band-reject) filter.  Creates a deep null at the
  centre frequency, passing all other frequencies unattenuated.

  Coefficient formulas (normalised by a0 = 1 + alpha):
      w0    = 2π·fc/SR
      alpha = sin(w0)/(2·Q)
      a0    = 1 + alpha
      b0    = 1 / a0                      [= b2]
      b1    = −2·cos(w0) / a0             [= a1]
      a1    = −2·cos(w0) / a0
      a2    = (1 − alpha) / a0

  Note that b1 = a1: numerator and denominator share the same cos(w0) term.
  This makes the zeros of H(z) lie on the unit circle (z = e^±jw0),
  producing a perfect null.  Higher Q → narrower notch width.

  Parameters
  ----------
  :hz — notch centre frequency in Hz (20–20000; default 1000)
  :q  — notch Q (0.1–20; default 1.0)

  Audio inputs  / Outputs
  -----------------------
  in: signal to filter  →  :out: notch filtered output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! biquad-np
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
        w     (faust "(%{in}-%{a1}*_-%{a2}*_@1)~_"  {:in audio :a1 a1 :a2 a2})
        out   (faust "%{b0}*%{ww}+%{b1}*%{ww}@1+%{b0}*%{ww}@2" {:b0 b0 :b1 b1 :ww w})]
    (output :out out)))
