; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.107-allpass-hz
  "GSOT pp.178 — go.allpass.hz.gendsp (Chapter 6).

  'Allpass filter — frequency parameterization'
  ----------------------------------------------
  Frequency-parameterized variant of go.allpass.gendsp (example 104).
  Instead of the raw allpass coefficient a, the user supplies the frequency
  fc (Hz) at which the phase shift is −π/2, and the coefficient is derived
  via the bilinear transform.

  Coefficient from frequency
  --------------------------
  The bilinear transform maps a first-order analog allpass H_a(s) = (s−b)/(s+b)
  — which has −π/2 phase shift at ω=b — to a digital allpass whose −π/2
  phase shift occurs at the warped digital frequency fc:

      t  = tan(π × fc / ma.SR)
      a  = (t − 1) / (t + 1)

  The resulting coefficient a drives the same first-order allpass expression
  as example 104:

      y[n] = a·x[n] + x[n-1] − a·y[n-1]

  Frequency ↔ coefficient correspondence (48 kHz)
  -----------------------------------------------
  fc          t = tan(π·fc/SR)    a
  ─────────────────────────────────────────────
     1 Hz     0.0000654          −0.9999
   100 Hz     0.00654            −0.987
  1000 Hz     0.0655             −0.877
  6000 Hz     0.414              −0.415
 12000 Hz     1.000               0.000   ← identity (pure 1-sample delay)
 20000 Hz     4.165               0.610

  At fc = SR/4 (12 kHz at 48 kHz SR): a = 0 → identity delay.
  Below SR/4: a < 0 (phase shift concentrated at high frequencies).
  Above SR/4: a > 0 (phase shift concentrated at low frequencies).

  Relationship to go.allpass.gendsp (example 104)
  ------------------------------------------------
  go.allpass.gendsp exposes the raw coefficient — useful for direct control
  or when chaining allpass stages in a phaser where the coefficient is swept.

  go.allpass.hz.gendsp maps to an audible frequency parameter, useful when
  designing a network where a specific phase-shift frequency is required
  (quadrature synthesis, Hilbert approximation, frequency-specific EQ).

  Three-node pattern
  ------------------
      tan-w = tan(π × :hz / ma.SR)        ; bilinear warp
      coeff = (tan-w − 1) / (tan-w + 1)   ; coefficient from warped frequency
      x-del = in@1                          ; feedforward 1-sample delay
      out   = (coeff*in + x-del − _*coeff) ~ _    ; allpass recursion

  Parameters
  ----------
  :hz — frequency in Hz at which phase shift is −π/2 (1–20000; default 1000)

  Audio inputs
  ------------
  audio-in 0: in — signal to phase-shift

  Outputs
  -------
  :out — allpass filtered output; flat amplitude, phase shifted

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_out
        with {
          n_hz  = hslider(\"hz\", 1000.0, 1.0, 20000.0, 1.0);
          n_tw  = tan(ma.PI*n_hz/ma.SR);
          n_cf  = (n_tw-1.0)/(n_tw+1.0);
          n_xd  = n0@1;
          n_out = (n_cf*n0+n_xd-_*n_cf)~_;
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! allpass-hz
  {:params {:hz {:range [1.0 20000.0] :default 1000.0}}}
  (let [in     (audio-in)
        freq   (param :hz)
        tan-w  (faust "tan(ma.PI*%hz/ma.SR)" {:hz freq})
        coeff  (faust "(%tw-1.0)/(%tw+1.0)" {:tw tan-w})
        x-del  (faust "%in@1" {:in in})
        out    (faust "(%cf*%in+%xd-_*%cf)~_" {:cf coeff :in in :xd x-del})]
    (output :out out)))
