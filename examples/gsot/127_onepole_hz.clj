; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.127-onepole-hz
  "GSOT pp.184-188 — go.onepole.hz.gendsp (Chapter 6).

  'Trapezoidal filters — one-pole lowpass'
  -----------------------------------------
  The culmination of GSOT's integration-method series (pp.184-188):
  backward Euler → forward Euler → trapezoidal.

  The three numerical integration methods
  ----------------------------------------
  All three start from the continuous-time first-order LP:

      dy/dt = ωc·(x − y)     where ωc = 2π·fc

  Discretised with step h = 1/SR:

  Backward Euler (go.onepole.basic, examples 100/103):
      y[n] = y[n-1] + h·ωc·(x[n]    − y[n])
           = a·x[n] + (1−a)·y[n-1]      where a = exp(−2π·fc/SR)

      This overestimates the area under the curve at each step.
      Good stability; cutoff is approximate at high fc.

  Forward Euler:
      y[n] = y[n-1] + h·ωc·(x[n-1] − y[n-1])

      This underestimates the area.  Can become unstable when
      h·ωc ≥ 2 (fc ≥ SR/π), long before Nyquist.

  Trapezoidal (this patch — bilinear transform / Tustin method):
      y[n] = y[n-1] + (h·ωc/2)·((x[n] − y[n]) + (x[n-1] − y[n-1]))

      Averages forward and backward Euler.  Solving for y[n]:

      y[n] = k·x[n] + k·x[n-1] + (1−2k)·y[n-1]

      where k = g/(1+g)   and   g = tan(π·fc/SR)

  Why tan instead of exp
  ----------------------
  The bilinear transform maps the continuous-time frequency ωc to the
  digital frequency ω_d via the warped relationship:

      ωc = 2·SR·tan(ω_d/2)  →  tan(π·fc/SR) = ωc/(2·SR)

  Using g = tan(π·fc/SR) (instead of the linear approximation g ≈ π·fc/SR)
  corrects the frequency warping introduced by the bilinear transform.
  The result: the digital filter has exactly −3 dB at fc, at all audio
  frequencies, not just at low fc.

  Compare to go.onepole.basic_hz (example 103)
  ---------------------------------------------
  Example 103 uses one-sample feedback with coefficient a = exp(−2π·fc/SR):
      y[n] = (1−a)·x[n] + a·y[n-1]

  Example 127 (this patch) uses TWO state elements: the feedback y[n-1]
  via ~_ AND the feedforward x[n-1] via @1.  This matches the pattern of
  the first-order allpass (example 104): both a feedforward delay and a
  feedback delay.  The difference is that here the two delays cooperate to
  implement trapezoidal integration rather than pure phase shifting.

  Comparison of one-pole variants
  --------------------------------
  example 100  go.onepole.basic     coefficient-based backward Euler
  example 103  go.onepole.basic_hz  Hz-parameterized backward Euler (exp)
  example 127  go.onepole.hz        Hz-parameterized trapezoidal (tan/bilinear)

  Trapezoidal node structure
  --------------------------
      g    = tan(π·fc/SR)          ; bilinear warp factor (same as ex.107 allpass)
      k    = g/(1+g)               ; filter coefficient (k ∈ (0,1) for fc ∈ (0,SR/2))
      x[n-1] via @1                ; feedforward delay (same pattern as ex.104 allpass)
      out  = (k·x[n] + k·x[n-1] + (1−2k)·y[n-1]) ~ _

  At low fc: k ≈ π·fc/SR → same small-signal behaviour as the exp formula.
  At fc = SR/4: k = 0.5, coefficient 1−2k = 0 → simple averaging filter.
  At fc → SR/2: k → 1, coefficient 1−2k → −1 → stability boundary.

  Stability: always stable for 0 < fc < SR/2 (k ∈ (0,1), so |1−2k| < 1).

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_out
        with {
          n_hz  = hslider(\"hz\", 1000.0, 1.0, 20000.0, 1.0);
          n_gv  = tan(ma.PI*n_hz/ma.SR);
          n_kk  = n_gv/(1.0+n_gv);
          n_xd  = n0@1;
          n_out = (n_kk*n0+n_kk*n_xd+(1.0-2.0*n_kk)*_)~_;
        };
      process = alembic_dsp;

  Parameters
  ----------
  :hz — cutoff frequency in Hz (1–20000; default 1000)

  Audio inputs  / Outputs
  -----------------------
  in: signal to filter  →  :out: trapezoidal lowpass filtered output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! onepole-hz
  {:params {:hz {:range [1.0 20000.0] :default 1000.0}}}
  (let [in  (audio-in)
        hz  (param :hz)
        gv  (faust "tan(ma.PI*%{hz}/ma.SR)" {:hz hz})
        kk  (faust "%{gv}/(1.0+%{gv})" {:gv gv})
        xd  (faust "%{in}@1" {:in in})
        out (faust "(%{kk}*%{in}+%{kk}*%{xd}+(1.0-2.0*%{kk})*_)~_" {:kk kk :in in :xd xd})]
    (output :out out)))
