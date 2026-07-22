; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.125-biquad4-hs
  "GSOT pp.182-183 — go.biquad4.hs (Chapter 6).

  'Cascades — Fourth-Order High Shelf'
  --------------------------------------
  Two identical high-shelf stages in series.  Doubles the shelf gain per
  stage.  Complement of go.biquad4.ls: boosted/cut region is above fc.

  Parameters
  ----------
  :hz   — shelf frequency in Hz (20–20000; default 1000)
  :q    — shelf slope Q (0.1–20; default 0.707)
  :gain — gain per stage in dB (total = 2×gain; −40–40; default 0)

  Audio inputs  / Outputs
  -----------------------
  in: signal to filter  →  :out: 4th-order high-shelf output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! biquad4-hs
  {:params {:hz   {:range [20.0 20000.0] :default 1000.0}
            :q    {:range [0.1 20.0]    :default 0.707}
            :gain {:range [-40.0 40.0]  :default 0.0}}}
  (let [audio (audio-in)
        hz    (param :hz)
        q     (param :q)
        gain  (param :gain)
        w0    (faust "2.0*ma.PI*%hz/ma.SR" {:hz hz})
        cw    (faust "cos(%w0)" {:w0 w0})
        sw    (faust "sin(%w0)" {:w0 w0})
        al    (faust "%sw/(2.0*%qq)"           {:sw sw :qq q})
        ga    (faust "pow(10.0,%gn/40.0)"      {:gn gain})
        sa    (faust "2.0*sqrt(%ga)*%al"       {:ga ga :al al})
        a0    (faust "(%ga+1.0)-(%ga-1.0)*%cw+%sa"             {:ga ga :cw cw :sa sa})
        b0    (faust "%ga*((%ga+1.0)+(%ga-1.0)*%cw+%sa)/%a0"   {:ga ga :cw cw :sa sa :a0 a0})
        b1    (faust "-2.0*%ga*((%ga-1.0)+(%ga+1.0)*%cw)/%a0"  {:ga ga :cw cw :a0 a0})
        b2    (faust "%ga*((%ga+1.0)+(%ga-1.0)*%cw-%sa)/%a0"   {:ga ga :cw cw :sa sa :a0 a0})
        a1    (faust "2.0*((%ga-1.0)-(%ga+1.0)*%cw)/%a0"      {:ga ga :cw cw :a0 a0})
        a2    (faust "((%ga+1.0)-(%ga-1.0)*%cw-%sa)/%a0"      {:ga ga :cw cw :sa sa :a0 a0})
        w1    (faust "(%in-%a1*_-%a2*_@1)~_"            {:in audio :a1 a1 :a2 a2})
        s1    (faust "%b0*%ww+%b1*%ww@1+%b2*%ww@2"     {:b0 b0 :b1 b1 :b2 b2 :ww w1})
        w2    (faust "(%in-%a1*_-%a2*_@1)~_"            {:in s1 :a1 a1 :a2 a2})
        out   (faust "%b0*%ww+%b1*%ww@1+%b2*%ww@2"     {:b0 b0 :b1 b1 :b2 b2 :ww w2})]
    (output :out out)))
