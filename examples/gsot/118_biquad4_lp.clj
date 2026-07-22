; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.118-biquad4-lp
  "GSOT pp.182-183 — go.biquad4.lp (Chapter 6).

  'Cascades — Fourth-Order Lowpass'
  ----------------------------------
  A 4th-order lowpass filter built by cascading two identical 2nd-order
  LP biquad sections.  Each section is go.biquad.lp (example 109) with
  the same fc and Q; the output of stage 1 feeds stage 2.

  Transfer function
  -----------------
      H4(z) = H2(z)²

  Where H2(z) is the 2nd-order LP biquad transfer function.  Squaring
  the transfer function doubles the roll-off rate:

      2nd order: −40 dB/decade (−12 dB/octave)
      4th order: −80 dB/decade (−24 dB/octave)

  Implementation
  --------------
  Both stages share a single coefficient computation (b0, b1, a1, a2 are
  computed once from fc and Q).  The DF-II recursion is applied twice,
  each with its own state register (w1 for stage 1, w2 for stage 2):

      ; Shared coefficients
      w0, cw, sw, alpha, a0, b0, b1, a1, a2  (same as go.biquad.lp)

      ; Stage 1
      w1  = (in  − a1·w1[n-1] − a2·w1[n-2]) ~ _
      s1  = b0·w1 + b1·w1@1 + b0·w1@2

      ; Stage 2
      w2  = (s1  − a1·w2[n-1] − a2·w2[n-2]) ~ _
      out = b0·w2 + b1·w2@1 + b0·w2@2

  Butterworth note
  ----------------
  Cascading two identical biquad sections (same Q) does NOT give an
  optimal Butterworth response — that requires different Q values for each
  stage.  For a 4th-order Butterworth LP, the two sections need:

      Q1 = 1/(2·cos(22.5°)) ≈ 0.5412   (inner pair, closer to passband)
      Q2 = 1/(2·cos(67.5°)) ≈ 1.3066   (outer pair, closer to stopband)

  Using equal Q gives a slightly different (non-Butterworth) 4th-order
  response, but still −80 dB/decade roll-off.  The cascade concept is
  the same regardless of Q choice.

  Parameters
  ----------
  :hz — cutoff frequency in Hz (20–20000; default 1000)
  :q  — resonance Q for both stages (0.1–20; default 0.707)

  Audio inputs  / Outputs
  -----------------------
  in: signal to filter  →  :out: 4th-order lowpass filtered output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! biquad4-lp
  {:params {:hz {:range [20.0 20000.0] :default 1000.0}
            :q  {:range [0.1 20.0]    :default 0.707}}}
  (let [audio (audio-in)
        hz    (param :hz)
        q     (param :q)
        ; Shared LP coefficients (computed once, applied to both stages)
        w0    (faust "2.0*ma.PI*%hz/ma.SR" {:hz hz})
        cw    (faust "cos(%w0)" {:w0 w0})
        sw    (faust "sin(%w0)" {:w0 w0})
        al    (faust "%sw/(2.0*%qq)" {:sw sw :qq q})
        a0    (faust "1.0+%al" {:al al})
        b0    (faust "(1.0-%cw)/(2.0*%a0)" {:cw cw :a0 a0})
        b1    (faust "(1.0-%cw)/%a0"        {:cw cw :a0 a0})
        a1    (faust "-2.0*%cw/%a0"         {:cw cw :a0 a0})
        a2    (faust "(1.0-%al)/%a0"        {:al al :a0 a0})
        ; Stage 1
        w1    (faust "(%in-%a1*_-%a2*_@1)~_"        {:in audio :a1 a1 :a2 a2})
        s1    (faust "%b0*%ww+%b1*%ww@1+%b0*%ww@2" {:b0 b0 :b1 b1 :ww w1})
        ; Stage 2
        w2    (faust "(%in-%a1*_-%a2*_@1)~_"        {:in s1 :a1 a1 :a2 a2})
        out   (faust "%b0*%ww+%b1*%ww@1+%b0*%ww@2" {:b0 b0 :b1 b1 :ww w2})]
    (output :out out)))
