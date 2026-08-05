; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.182-amrm-blended-harmonics
  "GSOT p.260 — AMRM-blended-harmonics.maxpat (Chapter 8: Frequent Modulations).

  'Blended Harmonic AM/RM and PM'
  --------------------------------
  This patch replaces the sinusoidal modulator in AM/RM (ex.159) and PM
  (ex.161) with the blended harmonic oscillator from ex.181, producing
  richer sideband structures that grow with :bl.

  Harmonic modulator
  ------------------
  The modulator is the go.harmonic oscillator at fundamental :fm:

      ph  = phasor(1, :fm)
      mo  = Σ_{n=1}^{8}  bl^(n−1) / n  ×  sin(2π × n × ph)    [× 0.5 gain]

  At :bl=0: mo = 0.5 × sin(2π × ph)   — pure sine at :fm (±0.5 amplitude)
  At :bl=1: mo ≈ sawtooth spectrum, 8 partials at :fm, 2:fm, …, 8:fm (±0.7 peak)

  The 0.5 gain (carried from ex.181) keeps the modulator within ±0.7 for
  all :bl values.  This avoids the headroom problem of a full-amplitude
  modulator in AM mode (AM with this modulator peaks at ≤ 1.7 × |in|, vs.
  2.0 × |in| for a unit-amplitude sine — see ex.175 for the headroom discussion).

  AM/RM sideband structure
  -------------------------
  For a sinusoidal input component at frequency fi:

      RM: fi × mo = Σ_{n=1}^{8}  (a_n / 2) × [cos(2π(fi−n·fm)t) − cos(2π(fi+n·fm)t)]

  where a_n = 0.5 × bl^(n−1) / n  is the nth modulator harmonic amplitude.

  Sideband pairs appear at fi ± n×fm for n=1..8, with amplitude a_n/2.

  This is LINEAR sideband generation (AM/RM), not Bessel (FM/PM).  Each
  harmonic of the modulator creates exactly one sideband pair — no
  side-of-sideband clusters.

  Compare with ex.166 (blending modulator in FM): there, the sawtooth
  modulator created Bessel CLUSTERS centred at the carrier for each harmonic
  of the modulator.  Here in AM/RM, each harmonic creates a SINGLE sideband
  PAIR — the spectrum is simpler and more predictable.

  At :bl=0: one sideband pair at fi ± :fm                       (ex.159 baseline)
  At :bl=0.5: dominant pair at ±:fm; visible pairs at ±2:fm, ±3:fm (softer)
  At :bl=1: eight sideband pairs at ±:fm through ±8:fm; 1/n rolloff

  The AM/RM bias :bs:
      :bs=0 → RM: input suppressed; sidebands only
      :bs=1 → AM: input preserved; sidebands + original fi component

  PM with harmonic modulator
  ---------------------------
  The same `mo` node is used as the PM modulation signal:

      pm = sin(2π × phasor(1, :fc) + :ix × mo)

  This is PM with a complex modulating signal.  The PM output has:
  - A Bessel cluster for each harmonic of mo, centred at :fc
  - The nth harmonic of mo contributes a cluster at :fc ± k × n × :fm
  - With amplitudes J_k(:ix × a_n) where a_n = 0.5 × bl^(n−1) / n

  At :bl=0: standard PM with a single sine modulator (β_eff = :ix × 0.5)
  At :bl=1: PM with sawtooth-like modulator; multiple Bessel clusters

  This is the harmonic version of ex.166 (sine/saw blend in FM/PM):
  the :bl parameter continuously sweeps from single-modulator PM (single
  Bessel cluster) toward multi-modulator PM (multiple overlapping clusters).
  The difference from ex.166: here the harmonic amplitudes are explicitly
  1/n-weighted (not Fourier series of a true sawtooth which has (2/π)/n).

  Shared modulator: synchronised AM/RM and PM outputs
  ----------------------------------------------------
  Both :amrm and :pm outputs use the same `mo` signal.  The two outputs
  are phase-locked: they share the harmonic phasor at :fm.  This makes
  them suitable for cross-fading, summing, or A/B comparison in a
  single playback context.

  Parameters
  ----------
  :fm — modulator fundamental frequency in Hz (0.1–4000; default 100)
  :bl — harmonic blend; 0.0=sine, 1.0=sawtooth-like (0–1; default 0.5)
  :bs — AM/RM bias; 0.0=RM, 1.0=AM (0–1; default 1.0)
  :fc — PM carrier frequency in Hz (20–4000; default 220)
  :ix — PM modulation index (0–10; default 2.0)

  Audio inputs / Outputs
  ----------------------
  in: audio signal for AM/RM
    :amrm — in × (:bs + harmonic_mo(:fm, :bl))
    :pm   — sin(2π×phasor(:fc) + :ix×harmonic_mo(:fm, :bl))"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! amrm-blended-harmonics
  {:params {:fm {:range [0.1 4000.0]  :default 100.0}
            :bl {:range [0.0 1.0]     :default 0.5}
            :bs {:range [0.0 1.0]     :default 1.0}
            :fc {:range [20.0 4000.0] :default 220.0}
            :ix {:range [0.0 10.0]    :default 2.0}}}
  (let [in   (audio-in)
        fm   (param :fm)
        bl   (param :bl)
        bs   (param :bs)
        fc   (param :fc)
        ix   (param :ix)
        ph   (faust "os.phasor(1,%{fm})" {:fm fm})
        mo   (faust "par(i,8,sin(2.0*ma.PI*(i+1)*%{ph})*pow(%{bl},i)/(i+1.0)):>*(0.5)"
                    {:ph ph :bl bl})
        amrm (faust "%{in}*(%{bs}+%{mo})" {:in in :bs bs :mo mo})
        pm   (faust "sin(2.0*ma.PI*os.phasor(1,%{fc})+%{ix}*%{mo})" {:fc fc :ix ix :mo mo})]
    (output :amrm amrm)
    (output :pm   pm)))
