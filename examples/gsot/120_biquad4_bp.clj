; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.120-biquad4-bp
  "GSOT pp.182-183 — go.biquad4.bp (Chapter 6).

  'Cascades — Fourth-Order Bandpass'
  ------------------------------------
  Two identical BP biquad stages in series.  A cascaded bandpass is
  significantly narrower than a single-stage BP at the same Q.

  H4(z) = H2_bp(z)² — both stages are constant-skirt-gain BP (example 111).

  Each stage: out = b0*(w - w@2), so b1=0 and the cascade passes only a
  narrow band around fc with −40 dB/decade roll-off on each skirt.

  Parameters
  ----------
  :hz — centre frequency in Hz (20–20000; default 1000)
  :q  — bandwidth Q for both stages (0.1–20; default 1.0)

  Audio inputs  / Outputs
  -----------------------
  in: signal to filter  →  :out: 4th-order bandpass filtered output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! biquad4-bp
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
        b0    (faust "%{sw}/(2.0*%{a0})"          {:sw sw :a0 a0})
        a1    (faust "-2.0*%{cw}/%{a0}"           {:cw cw :a0 a0})
        a2    (faust "(1.0-%{al})/%{a0}"          {:al al :a0 a0})
        w1    (faust "(%{in}-%{a1}*_-%{a2}*_@1)~_" {:in audio :a1 a1 :a2 a2})
        s1    (faust "%{b0}*(%{ww}-%{ww}@2)"        {:b0 b0 :ww w1})
        w2    (faust "(%{in}-%{a1}*_-%{a2}*_@1)~_" {:in s1 :a1 a1 :a2 a2})
        out   (faust "%{b0}*(%{ww}-%{ww}@2)"        {:b0 b0 :ww w2})]
    (output :out out)))
