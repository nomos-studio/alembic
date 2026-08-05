; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.173-fmpm-waveshaping-modulator
  "GSOT pp.249-250 — FMPM-waveshaping-modulator.maxpat (Chapter 8: Frequent Modulations).

  'Waveshaping Modulators — Nonlinear Transfer Functions on the FM/PM Modulator'
  -------------------------------------------------------------------------------
  All previous FM/PM patches used a sinusoidal modulator `osc(fm)` directly.
  Waveshaping applies a nonlinear transfer function f(x) to the modulator
  *before* it enters the carrier:

      shaped_mod = f(osc(fm))
      out = sin(2π × phasor(fc + ix × fm × shaped_mod))   [FM]
      out = sin(2π × phasor(fc) + ix × shaped_mod)         [PM]

  Because osc(fm) is a pure sine, f(sin(ωt)) produces harmonics of fm in the
  modulator signal.  Each harmonic of fm then acts as an independent modulator
  for the carrier, producing a Bessel cluster centred on the carrier for each.

  The tanh waveshaper
  --------------------
  This patch uses a drive-controlled hyperbolic tangent (soft saturator):

      shaped_mod = ma.tanh(:dr × osc(fm)) / ma.tanh(:dr)

  The normalisation by tanh(:dr) ensures the output remains in [−1, 1]
  regardless of :dr, so the modulation index :ix retains its meaning.

  Spectral consequences of tanh shaping
  ----------------------------------------
  tanh is an *odd* function: f(−x) = −f(x).  Applied to a sine, it produces
  only odd harmonics:

      tanh(d × sin(ωt)) / tanh(d)  =  a1×sin(ωt) + a3×sin(3ωt) + a5×sin(5ωt) + …

  The shaped modulator is a weighted sum of sinusoids at fm, 3fm, 5fm, 7fm, …
  with amplitudes a1 > a3 > a5 > … that depend on the drive :dr.

  As a result, the FM/PM output has Bessel clusters at:
      … centred on fc, driven by the a1 component at fm  (dominant)
      … centred on fc, driven by the a3 component at 3fm (weaker)
      … centred on fc, driven by the a5 component at 5fm (weaker still)

  Each cluster contributes sidebands at fc ± n×(k×fm) for k=1,3,5,…
  The resulting spectrum is denser than a single-modulator FM patch and
  richer in the upper partials.

  Odd harmonics only — why this matters
  ----------------------------------------
  Odd harmonics in the modulator means the extra Bessel clusters fall only
  at odd multiples of fm.  Compare:

    ex.166 (blending, sine→saw):  all harmonics of fm in modulator
        → clusters at fc + n×(fm, 2fm, 3fm, 4fm, …)

    ex.173 (waveshaping, tanh):   only odd harmonics of fm in modulator
        → clusters at fc + n×(fm, 3fm, 5fm, 7fm, …)

  With :rt=1 (1:1 C:M ratio), the extra tanh clusters land at 3fc, 5fc, 7fc —
  exactly the odd harmonics of the carrier.  The output has a hollow,
  clarinet-like character (odd harmonics dominant) that intensifies with :dr.

  With :rt=2 (1:2), the clusters at 3fm=6fc and 5fm=10fc fall on even harmonics
  of the fundamental — less hollow, richer.

  Drive behaviour
  ---------------
  :dr ≈ 0.1:  nearly linear (shaped_mod ≈ osc(fm)); same as no waveshaping
  :dr = 1.0:  slight saturation; a3 ≈ 0.10, a5 ≈ 0.02 — gentle enrichment
  :dr = 3.0:  noticeable saturation; a3 ≈ 0.25; hollow quality starts
  :dr = 5.0:  strong saturation; a3 ≈ 0.33; approaching square-wave modulator
  :dr = 10.0: heavily saturated; shaped_mod ≈ square wave; a1:a3:a5 ≈ 1:⅓:⅕

  At :dr=10 the shaped modulator resembles a square wave.  The effective
  modulation is equivalent to three simultaneous modulators at fm, 3fm, 5fm
  with indices ix, ix/3, ix/5 (since aN ∝ 1/N for a square wave).

  Relationship to other modulator-shaping examples
  --------------------------------------------------
  ex.166 (blending):     blend between sine and sawtooth (all harmonics)
  ex.167 (cascade):      modulator is itself FM-modulated (exponentially growing harmonic stack)
  ex.173 (waveshaping):  nonlinear shaping of a sine → controlled odd harmonic stack

  Waveshaping is the most predictable of the three: the harmonic content of the
  shaped modulator can be computed analytically from the Fourier series of tanh,
  and the :dr parameter provides a smooth, well-behaved dial from linear to
  heavily distorted modulator.

  FM/PM routing (:md)
  --------------------
  The shaped modulator enters the FM/PM formula identically to an unshped one:
      FM: sin(2π × phasor(fc + (1−md)×ix×fm×shaped_mod))
      PM: sin(2π × phasor(fc) + md×ix×shaped_mod)
      morph: as in ex.161–172

  Parameters
  ----------
  :fc — carrier frequency in Hz (20–2000; default 220)
  :rt — C:M ratio (fm = fc × :rt; 0.1–8.0; default 1.0)
  :ix — modulation index β (0–10; default 2.0)
  :dr — waveshaper drive; 0.1=linear, 10=near-square (0.1–10; default 1.0)
  :md — FM→PM morph; 0.0=FM, 1.0=PM (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained FM/PM synthesizer with shaped modulator)
    :out — FM/PM output with waveshaped modulator"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! fmpm-waveshaping-modulator
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :rt {:range [0.1 8.0]     :default 1.0}
            :ix {:range [0.0 10.0]    :default 2.0}
            :dr {:range [0.1 10.0]    :default 1.0}
            :md {:range [0.0 1.0]     :default 0.0}}}
  (let [fc  (param :fc)
        rt  (param :rt)
        ix  (param :ix)
        dr  (param :dr)
        md  (param :md)
        fm  (faust "%{fc}*%{rt}" {:fc fc :rt rt})
        mo  (faust "os.osc(%{fm})" {:fm fm})
        ws  (faust "ma.tanh(%{dr}*%{mo})/ma.tanh(%{dr})" {:dr dr :mo mo})
        out (faust "sin(2.0*ma.PI*os.phasor(1,%{fc}+(1.0-%{md})*%{ix}*%{fm}*%{ws})+%{md}*%{ix}*%{ws})"
                   {:fc fc :fm fm :ix ix :md md :ws ws})]
    (output :out out)))
