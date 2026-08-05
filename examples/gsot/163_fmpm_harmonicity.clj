; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.163-fmpm-harmonicity
  "GSOT p.236 — FMPM-harmonicity.maxpat (Chapter 8: Frequent Modulations).

  'Frequency Ratios and Harmonicity in FM Synthesis'
  --------------------------------------------------
  The carrier-to-modulator (C:M) ratio is the primary timbral parameter in FM
  synthesis — more fundamental than the modulation index β.  By expressing the
  modulator frequency as a *ratio* of the carrier:

      fm  =  fc × :rt

  a single knob (:rt) sweeps the entire harmonic/inharmonic spectrum.

  Why ratios produce harmonic spectra
  -------------------------------------
  FM sidebands land at  fc + n×fm  for all integers n.  For a sinusoidal
  input at fc and modulator at fm:

      Sideband n:  fc + n×fm  =  fc × (1 + n×:rt)

  When :rt = p/q (a ratio of small integers), the sidebands land on multiples
  of a common fundamental F = fc / p = fm / q, producing a harmonic series.
  When :rt is irrational, no such common fundamental exists — the sidebands
  are inharmonic and the spectrum sounds metallic or bell-like.

  C:M ratio table
  ---------------
  :rt   C:M    Spectrum                        Timbre
  ----  -----  ------------------------------  ---------------------
  1.0   1:1    All harmonics of fc             Bright, sawtooth-like
  2.0   1:2    Odd harmonics of fc only        Hollow, clarinet-like
  3.0   1:3    Harmonics with gaps             Rich, complex
  0.5   2:1    Sub-harmonics; 2fc fundamental  Deep, thick
  0.333 3:1    Sub-sub-harmonics               Very deep fundamental
  1.5   2:3    Harmonic series of fc/2         Dense (sub-octave)
  1.414 1:√2   Mildly inharmonic               Slightly metallic
  1.618 1:φ    Strongly inharmonic (golden)    Bell-like, stretched
  1.4   7:5    Tritone ratio; semi-harmonic    Tense, ambiguous
  1.732 1:√3   Inharmonic                      Bell-like
  0.707 √2:1   Inharmonic sub-partials         Gong-like

  Negative sidebands (n < 0) fold to positive frequencies (|fc + n×fm|),
  which can introduce additional components at low frequencies when |n×fm| > fc.
  At :rt=2, the n=-1 sideband lands at |-fc|=fc (reinforces carrier),
  and the spectrum is exactly the odd harmonic series: fc, 3fc, 5fc, …

  Harmonicity vs inharmonicity
  -----------------------------
  'Harmonicity' is the degree to which the sideband series aligns with integer
  multiples of a common fundamental.  A useful continuous measure:

      harmonicity(r) = 1 − frac(r)     where frac is the fractional part

  At integer :rt: harmonicity = 1 (fully harmonic).
  At :rt = n+0.5: harmonicity = 0.5 (maximally between harmonics).

  The timbral perception tracks this measure: integer ratios sound pitched and
  fused; ratios near n+0.5 sound ambiguous; strongly irrational ratios (like φ)
  sound inharmonic and metallic even at low β.

  C:M and β interaction
  ----------------------
  :ix and :rt interact: at high β, even harmonic spectra grow very dense and
  can obscure the ratio structure.  The 'interesting zone' for ratio exploration
  is usually β ∈ [1, 4] — enough sidebands to hear the ratio, few enough to
  preserve its character.  Above β≈8 most ratios converge toward noise-like
  density.

  At β=0 (:ix=0): always a pure sine at fc regardless of :rt.

  FM/PM morph (:md)
  -----------------
  The FM/PM routing is inherited from ex.161/162.  FM and PM produce the same
  Bessel amplitude spectrum for the same β; the difference is inaudible for
  sustained tones but may be faintly perceptible on attack transients.

  Parameters
  ----------
  :fc — carrier frequency in Hz (20–2000; default 220)
  :rt — C:M ratio (fm = fc × :rt); integer=harmonic, irrational=inharmonic (0.1–8.0; default 1.0)
  :ix — modulation index β (0–10; default 2.0)
  :md — FM→PM morph; 0.0=FM, 1.0=PM (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained FM/PM synthesizer)
    :out — FM/PM output with C:M ratio control"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! fmpm-harmonicity
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :rt {:range [0.1 8.0]     :default 1.0}
            :ix {:range [0.0 10.0]    :default 2.0}
            :md {:range [0.0 1.0]     :default 0.0}}}
  (let [fc  (param :fc)
        rt  (param :rt)
        ix  (param :ix)
        md  (param :md)
        fm  (faust "%{fc}*%{rt}" {:fc fc :rt rt})
        mo  (faust "os.osc(%{fm})" {:fm fm})
        out (faust "sin(2.0*ma.PI*os.phasor(1,%{fc}+(1.0-%{md})*%{ix}*%{fm}*%{mo})+%{md}*%{ix}*%{mo})"
                   {:fc fc :fm fm :ix ix :md md :mo mo})]
    (output :out out)))
