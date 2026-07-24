; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.177-fmpm-carsonrule-filtered
  "GSOT p.256 — FMPM-carsonrule-filtered.maxpat (Chapter 8: Frequent Modulations).

  'Aliasing and Bandlimiting — Carson's Rule with Output LP Filter'
  ------------------------------------------------------------------
  This patch extends ex.176 (fmpm-carsonrule) by adding a lowpass filter on
  the FM output, with its cutoff set to the Carson bandwidth upper edge:

      cutoff = min(SR/2 − 20,  fc × (1 + (ic + 1) × rt))

  where `ic` is the Carson-clamped index from ex.176.

  Why filter after clamping?
  --------------------------
  Clamping ix (ex.176) ensures the HIGHEST sideband is at or below SR/2.
  But it does not remove any content — it just prevents generating aliases.

  The LP filter here takes a different role: it shapes the FM output to contain
  only the spectral content within the Carson bandwidth, discarding energy
  above that boundary.  This is useful in two scenarios:

  1. The ix clamp prevents aliasing, but you want the output spectrum
     to stay tightly within the Carson boundary regardless of how the
     Bessel amplitudes trail off.  The LP applies a hard(ish) cutoff at
     fc + (ic+1)×fm, removing the long Bessel tail above that frequency.

  2. You want to use the Carson bandwidth as a creative filter: tuning :ix
     and :rt moves the LP cutoff, and the filter sculpts the FM spectrum
     in a musically coherent way (the cutoff is derived from the FM
     parameters, not a separate dial).

  Filter cutoff derivation
  ------------------------
      fm      = fc × :rt
      ic      = min(:ix,  max(0, (SR/2 − fc)/(fc × :rt) − 1))   [ex.176 clamp]
      cutoff  = min(SR/2 − 20,   fc × (1 + (ic + 1) × :rt))

  At low :ix (where no clamping occurs): ic = :ix and cutoff = fc × (1 + (:ix+1) × :rt)
  At high :ix (where clamping kicks in): ic < :ix and cutoff ≤ SR/2 − 20

  The `SR/2 − 20` clamp prevents the filter cutoff from reaching exactly SR/2,
  which can cause numerical instability in some Butterworth designs.

  Comparison with ex.176 (Carson clamp only)
  -------------------------------------------
  ex.176: ix clamp prevents aliases; Bessel sidebands up to n ≈ ic+3 still
          present but become inaudibly small.  Spectrum is FM-shaped above
          ic (long tail of small Bessel amplitudes).

  ex.177: ix clamp + LP at fc×(1+(ic+1)×rt).  The LP hard-cuts the Bessel
          tail, giving a sharper high-frequency rolloff.  The LP is first-order
          (−6 dB/oct); for steeper rolloff increase the order in `fi.lowpass`.

  Comparison with ex.177 (this) vs. ex.178 (FMPM-antialias-filter)
  -----------------------------------------------------------------
  ex.177 LP cutoff: derived from FM parameters (fc, ix, rt) — parametric,
  tracks the synthesis.  Changes as :ix or :rt change.

  ex.178 LP cutoff: independent user control — fixed frequency not derived
  from FM parameters.  Useful when you want to control alias suppression
  without touching the synthesis parameters.

  Filter order
  ------------
  This patch uses fi.lowpass(1, ...) — first-order, 6 dB/octave.  The filter
  attenuates rather than eliminates; Bessel tail components above the cutoff
  are reduced but not fully removed.  This matches the GSOT gen~ implementation
  which also uses a single-pole LP.  For sharper suppression use order 2 or 4.

  FM / PM routing (:md)
  ---------------------
  Identical to ex.176 — the clamped index ic enters the FM/PM formula.
  The LP filter is applied to the final output signal.

  Parameters
  ----------
  :fc — carrier frequency in Hz (20–2000; default 440)
  :rt — C:M ratio (fm = fc × :rt; 0.1–8.0; default 1.0)
  :ix — desired modulation index β (0–10; default 2.0; may be clamped)
  :md — FM→PM morph; 0.0=FM, 1.0=PM (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained FM/PM synthesizer)
    :out — FM/PM output with ix clamp + LP at Carson bandwidth cutoff"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! fmpm-carsonrule-filtered
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
        cf  (faust "min(ma.SR/2.0-20.0,%fc*(1.0+(%ic+1.0)*%rt))" {:fc fc :ic ic :rt rt})
        fm2 (faust "sin(2.0*ma.PI*os.phasor(1,%fc+(1.0-%md)*%ic*%fm*%mo)+%md*%ic*%mo)"
                   {:fc fc :fm fm :ic ic :md md :mo mo})
        out (faust "%fm : fi.lowpass(1,%cf)" {:fm fm2 :cf cf})]
    (output :out out)))
