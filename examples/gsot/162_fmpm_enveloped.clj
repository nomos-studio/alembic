; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.162-fmpm-enveloped
  "GSOT pp.234-235 — FMPM-enveloped.maxpat (Chapter 8: Frequent Modulations).

  'Enveloped FM/PM — Timbral Shaping through Modulation Index Decay'
  -------------------------------------------------------------------
  The most important parameter in FM synthesis is not the amplitude — it is
  the modulation index β.  Enveloping β over time produces the timbral arc
  that makes FM synthesis perceptually convincing: dense harmonics at attack,
  gradual simplification to a near-sine at the tail.

  This matches the natural behaviour of acoustic instruments: a struck bell
  has a bright, complex onset and a pure, decaying tone afterward; a plucked
  string excites many harmonics in the first milliseconds and quiets to a
  fundamental-heavy sustain.

  The envelope here applies to *both* the modulation index and the amplitude:

      env = AR_envelope(at, rl, gate)
      iy  = ix × env              — peak β scaled by envelope (0 at silence)
      out = env × FM/PM(fc, fm, iy, md)

  This couples timbre and amplitude so the signal is silent when the envelope
  is at zero — the index envelope and amplitude envelope track together.

  Sideband evolution over the envelope
  --------------------------------------
  β = iy (envelope-scaled):

    β ≈ 0:   output ≈ pure sine at fc
    β ≈ 0.5: weak first sidebands fc±fm appear (J_1(0.5) ≈ 0.24)
    β ≈ 1:   first sidebands strong, second sidebands appear faintly
    β ≈ 2:   4–5 significant sidebands; carrier amplitude has dipped
    β ≈ 3:   carrier (J_0) dips again; rich, buzzy spectrum
    β ≥ 5:   very dense; some sidebands below 0 Hz fold up (inharmonic artefact
              for integer C:M ratios; handled naturally by the Bessel expansion)

  C:M ratio and timbre
  ---------------------
  :fc / :fm determines whether the sidebands land on harmonic or inharmonic
  positions of the carrier:

    :fc=440 :fm=440   (1:1)   — odd+even harmonics; bright sawtooth-like
    :fc=440 :fm=220   (2:1)   — sidebands at 220, 440, 660, 880,… — rich
    :fc=440 :fm=616   (≈1:1.4) — inharmonic; bell-like, metallic
    :fc=440 :fm=293   (3:2)   — missing harmonics; hollow, hollow
    :fc=220 :fm=293   (3:4)   — sub-partials; gong-like

  Classic FM bell: fc=220, fm=308 (C:M≈1:1.4), ix=5, at=0.01, rl=3.0

  AR envelope
  -----------
  `en.ar(at, rl, gate)` from Faust's envelopes.lib:
    - gate > 0: envelope rises from current level to 1 over :at seconds
    - gate = 0: envelope falls from current level to 0 over :rl seconds
  Audio input carries the gate signal (0 or 1 per sample).  A single-sample
  impulse (value > 0 for one sample, then 0) is enough to trigger; the release
  phase begins as soon as the gate goes low.

  FM/PM routing (from ex.161)
  ---------------------------
  :md = 0.0 → FM: sin(2π × phasor(fc + iy×fm×mod(fm)))
  :md = 1.0 → PM: sin(2π × phasor(fc) + iy×mod(fm))

  The :md morph now shapes both the routing and the timbral evolution
  simultaneously — FM and PM have subtly different timbral arcs even with
  the same envelope, because the phase accumulation path differs.

  Parameters
  ----------
  :fc — carrier frequency in Hz (20–4000; default 440)
  :fm — modulator frequency in Hz (1–4000; default 220)
  :ix — peak modulation index β at envelope maximum (0–10; default 3.0)
  :md — FM→PM morph; 0.0=FM, 1.0=PM (0–1; default 0.0)
  :at — attack time in seconds (0.001–2.0; default 0.01)
  :rl — release time in seconds (0.001–5.0; default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: gate signal (0 = off, >0 = on; edge triggers attack phase)
    :out — enveloped FM/PM output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! fmpm-enveloped
  {:params {:fc {:range [20.0 4000.0]  :default 440.0}
            :fm {:range [1.0 4000.0]   :default 220.0}
            :ix {:range [0.0 10.0]     :default 3.0}
            :md {:range [0.0 1.0]      :default 0.0}
            :at {:range [0.001 2.0]    :default 0.01}
            :rl {:range [0.001 5.0]    :default 0.5}}}
  (let [gt  (audio-in)
        fc  (param :fc)
        fm  (param :fm)
        ix  (param :ix)
        md  (param :md)
        at  (param :at)
        rl  (param :rl)
        env (faust "en.ar(%{at},%{rl},%{gt})" {:at at :rl rl :gt gt})
        mo  (faust "os.osc(%{fm})" {:fm fm})
        iy  (faust "%{ix}*%{en}" {:ix ix :en env})
        out (faust "%{en}*sin(2.0*ma.PI*os.phasor(1,%{fc}+(1.0-%{md})*%{iy}*%{fm}*%{mo})+%{md}*%{iy}*%{mo})"
                   {:en env :fc fc :fm fm :iy iy :md md :mo mo})]
    (output :out out)))
