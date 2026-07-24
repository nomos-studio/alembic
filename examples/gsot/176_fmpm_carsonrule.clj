; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.176-fmpm-carsonrule
  "GSOT p.255 — FMPM-carsonrule.maxpat (Chapter 8: Frequent Modulations).

  'Aliasing and Bandlimiting — Carson's Rule for FM Bandwidth'
  -------------------------------------------------------------
  FM synthesis generates sidebands at fc ± n×fm (n = 0,1,2,…).  The amplitude
  of the nth sideband is J_n(β) where β = :ix.  For β = 5, sidebands through
  n ≈ 8 are significant (|J_8(5)| ≈ 0.03).  If fc + 8×fm > SR/2, those
  sidebands alias back into the audio band, producing inharmonic artefacts.

  Carson's Rule
  -------------
  J.R. Carson's 1922 bandwidth rule states that 98% of FM signal power falls
  within a bandwidth of:

      BW ≈ 2 × (β + 1) × fm  =  2 × (:ix + 1) × :fc × :rt

  The upper edge of this bandwidth is:

      fc_upper = fc + (β + 1) × fm  =  :fc × (1 + (:ix + 1) × :rt)

  Carson's rule gives a conservative (slightly generous) bound: Bessel
  amplitudes decay rapidly beyond n = β + 1, so the true 'last significant'
  sideband is often at n ≈ β + 2.  But 98% of power is captured within the
  Carson bandwidth, which is the threshold GSOT uses for aliasing analysis.

  Aliasing condition
  ------------------
  Aliasing occurs when:

      fc_upper = fc × (1 + (ix+1) × rt)  >  SR/2

  Solving for the maximum safe :ix (call it ix_safe):

      (ix_safe + 1) × fc × rt  ≤  SR/2 − fc
      ix_safe  ≤  (SR/2 − fc) / (fc × rt)  −  1
      ix_safe  =  max(0,  (SR/2 − fc) / (fc × rt)  −  1)

  This patch clamps :ix to ix_safe, preventing the Carson bandwidth from
  exceeding Nyquist.  The clamp is computed per-sample so it tracks
  parameter changes continuously.

  Example values at SR=44100, fc=440, rt=1
  -----------------------------------------
      fm = 440,  SR/2 = 22050
      ix_safe = (22050 − 440) / 440 − 1 = 50.11 − 1 = 49.11

  At ratio 1:1 and fc=440 Hz, you have enormous headroom.  Move up:

      fc=5000, rt=2:  fm=10000, ix_safe = (22050−5000)/10000−1 = 0.705
      fc=8000, rt=1:  fm=8000,  ix_safe = (22050−8000)/8000−1  = 0.756

  High carrier frequencies and large C:M ratios exhaust the safe index
  quickly.  At fc=10000, rt=2, any :ix > 0.1 aliases.

  Why clamping is musically useful
  ---------------------------------
  Without clamping: at fc=8000, :ix=3.0, :rt=1.0, the Carson upper edge is
  at 8000 × (1+4×1) = 40000 Hz, far above SR/2=22050.  Sidebands from n=2
  onward (8000+2×8000=24000 Hz) alias into the audio band, producing
  nonharmonic components at 44100−24000=20100 Hz, 44100−32000=12100 Hz, etc.

  With ix clamped to ix_safe ≈ 0.756: the outermost significant sideband
  lands at 8000×(1+1.756×1) = 22048 Hz — just inside Nyquist, clean.

  The clamped FM still produces sidebands; their amplitudes follow J_n(ix_safe)
  normally.  It just ensures all sidebands land below Nyquist.

  Note: clamping ix does not filter aliased content already present.  If the
  system receives audio input that already contains aliases, this patch does
  not remove them.  See ex.177 (FMPM-carsonrule-filtered) for output LP
  filtering after FM synthesis.

  FM / PM routing (:md)
  ---------------------
  The FM/PM morph applies to the clamped index:
      FM (md=0): sin(2π × phasor(fc + ic×fm×osc(fm)))
      PM (md=1): sin(2π × phasor(fc) + ic×osc(fm))
  where ic = min(:ix, ix_safe).

  Parameters
  ----------
  :fc — carrier frequency in Hz (20–2000; default 440)
  :rt — C:M ratio (fm = fc × :rt; 0.1–8.0; default 1.0)
  :ix — desired modulation index β (0–10; default 2.0; may be clamped)
  :md — FM→PM morph; 0.0=FM, 1.0=PM (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained FM/PM synthesizer)
    :out — FM/PM output with ix clamped to Carson-safe maximum"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! fmpm-carsonrule
  {:params {:fc {:range [20.0 2000.0] :default 440.0}
            :rt {:range [0.1 8.0]     :default 1.0}
            :ix {:range [0.0 10.0]    :default 2.0}
            :md {:range [0.0 1.0]     :default 0.0}}}
  (let [fc  (param :fc)
        rt  (param :rt)
        ix  (param :ix)
        md  (param :md)
        fm  (faust "%fc*%rt" {:fc fc :rt rt})
        is  (faust "max(0.0,(ma.SR/2.0-%fc)/(%fc*%rt)-1.0)" {:fc fc :rt rt})
        ic  (faust "min(%ix,%is)" {:ix ix :is is})
        mo  (faust "os.osc(%fm)" {:fm fm})
        out (faust "sin(2.0*ma.PI*os.phasor(1,%fc+(1.0-%md)*%ic*%fm*%mo)+%md*%ic*%mo)"
                   {:fc fc :fm fm :ic ic :md md :mo mo})]
    (output :out out)))
