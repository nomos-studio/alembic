; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.178-fmpm-antialias-filter
  "GSOT p.257 — FMPM-antialias-filter.maxpat (Chapter 8: Frequent Modulations).

  'Aliasing and Bandlimiting — Explicit Anti-Alias LP Filter'
  -----------------------------------------------------------
  This patch applies a direct lowpass filter on the FM output with a
  user-controlled cutoff frequency (:cf), independent of the FM synthesis
  parameters.  This is distinct from ex.177 (FMPM-carsonrule-filtered) where
  the LP cutoff is derived from the FM parameters.

  The aliasing mechanism
  ----------------------
  Faust's `os.osc()` and `os.phasor()` are mathematically exact (no
  wavetable lookup), but aliasing still occurs in digital FM/PM when the
  instantaneous frequency exceeds SR/2.  The instantaneous frequency is:

      f_inst(t) = fc + ix × fm × sin(2π × fm × t)   [FM]
      f_inst(t) = fc                                  [PM, modulation is phase not freq]

  For FM: f_inst ranges from fc − ix×fm to fc + ix×fm.  When ix×fm is large,
  the instantaneous frequency can exceed SR/2, causing the phasor to 'wrap'
  in a way that generates alias components.

  For PM: the instantaneous frequency is always `fc` and doesn't directly
  alias, but the sideband amplitudes can produce energy above SR/2 in the
  Fourier domain that aliases on reconstruction.

  In practice the dominant aliasing for both FM and PM comes from the
  sideband structure: high-order Bessel sidebands at fc + n×fm exceed
  Nyquist and fold back.

  The anti-alias filter approach
  ------------------------------
  Rather than clamping :ix (ex.176) or deriving the cutoff from FM parameters
  (ex.177), this patch uses an explicit LP with a fixed, user-controlled
  cutoff.  The key insight:

    - Setting :cf < SR/2 does NOT prevent alias generation — aliases that
      have already been created by the synthesis fold into the audio band.
    - The LP removes AUDIBLE CONTENT above :cf, including any aliases that
      landed in [0, :cf] from foldback.  Aliases in [0, :cf] are not removed.
    - The LP does remove aliases that would otherwise land above :cf.

  Effective aliasing suppression strategy:
      1. Set :cf to the expected highest significant sideband frequency:
         :cf ≈ fc + (β+1)×fm = fc + (:ix+1)×fc×:rt  (Carson upper edge).
         This mirrors the Carson-bandwidth LP from ex.177.
      2. Set :cf below where aliases actually fold back to:
         alias at SR − (fc + n×fm).  E.g. if fc=5000, fm=8000, ix=3,
         highest alias from n=4: SR − (5000+32000) = 44100−37000 = 7100 Hz.
         Setting :cf below 7100 Hz removes that alias component.

  Use vs. ex.176 and ex.177
  --------------------------
  ex.176 (Carson clamp):      prevents alias generation — ix capped so no
                              sideband exceeds SR/2.  Safest approach, but
                              restricts musical :ix range at high fc or rt.

  ex.177 (Carson filtered):   ix clamp + LP at Carson upper edge.  Tight
                              spectral boundary derived from FM parameters.

  ex.178 (this):              NO ix clamp.  LP at user :cf.  For high :ix
                              settings aliases can still be present below :cf,
                              but content above :cf is removed.  Gives the
                              most direct control and is useful when you want
                              aliased FM sound up to a certain frequency
                              ceiling (deliberately aliased bass FM with a
                              LP to control spectral spread).

  Creative use: deliberate aliasing
  ----------------------------------
  Aliases are not always unwanted.  At moderate alias levels the fold-back
  components produce inharmonic partials that give FM a metallic, bell-like
  roughness.  Setting :cf well above the fundamental but below the alias
  cluster lets you dial in just enough aliasing.  Ex.178 is designed for this
  exploration — unlike ex.176 which hard-prevents aliasing, this LP lets
  you choose your ceiling.

  Filter order and character
  --------------------------
  fi.lowpass(1, cf): first-order, gentle 6 dB/oct rolloff. Retains a long
  spectral tail.  Good for subtle alias shaping.

  fi.lowpass(2, cf): second-order, 12 dB/oct.  More decisively cuts above :cf.
  The patch uses order 2 for more useful alias suppression — change to 1 or 4
  as desired.

  FM / PM routing (:md)
  ---------------------
  Full FM/PM morph as in ex.161–177.  The anti-alias LP applies equally to
  both FM and PM modes.

  Parameters
  ----------
  :fc — carrier frequency in Hz (20–2000; default 440)
  :rt — C:M ratio (fm = fc × :rt; 0.1–8.0; default 1.0)
  :ix — modulation index β (0–10; default 4.0; NOT clamped — aliasing possible)
  :cf — anti-alias LP cutoff in Hz (100–20000; default 8000)
  :md — FM→PM morph; 0.0=FM, 1.0=PM (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained FM/PM synthesizer)
    :out — FM/PM output with LP at :cf (aliasing may be present below :cf)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! fmpm-antialias-filter
  {:params {:fc {:range [20.0 2000.0]  :default 440.0}
            :rt {:range [0.1 8.0]      :default 1.0}
            :ix {:range [0.0 10.0]     :default 4.0}
            :cf {:range [100.0 20000.0] :default 8000.0}
            :md {:range [0.0 1.0]      :default 0.0}}}
  (let [fc  (param :fc)
        rt  (param :rt)
        ix  (param :ix)
        cf  (param :cf)
        md  (param :md)
        fm  (faust "%{fc}*%{rt}" {:fc fc :rt rt})
        mo  (faust "os.osc(%{fm})" {:fm fm})
        sy  (faust "sin(2.0*ma.PI*os.phasor(1,%{fc}+(1.0-%{md})*%{ix}*%{fm}*%{mo})+%{md}*%{ix}*%{mo})"
                   {:fc fc :fm fm :ix ix :md md :mo mo})
        out (faust "%{sy} : fi.lowpass(2,max(20.0,%{cf}))" {:sy sy :cf cf})]
    (output :out out)))
