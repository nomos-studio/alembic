; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.171-fmpm-ifod
  "GSOT pp.247-248 — FMPM-ifod.maxpat (Chapter 8: Frequent Modulations).

  'Index Amplitude and Ring Modulation — FM/PM as a Modulator'
  -------------------------------------------------------------
  The FM/PM patches in ex.161–170 produced self-contained synthesis voices:
  the FM oscillator *was* the output.  This patch reverses that role: the
  FM/PM signal becomes the *modulator* for an audio input, ring-modulating
  or amplitude-modulating it.

  This reunites the AM/RM thread (ex.157–159) with the FM/PM thread, using
  the richer spectral content of an FM oscillator as the carrier modulator:

      FM/PM oscillator → ring/AM modulate → audio input → output
      y = in × (:bs + fm_osc(fc, fm, ix, md))

  When the FM oscillator is a pure sine (:ix=0) this reduces to ex.157–159.
  When the FM oscillator has modulation (:ix>0) the sidebands of the FM
  signal become the sidebands of the ring/AM modulator — far richer than
  any single sinusoidal modulator could produce.

  'Index amplitude' and 'ring modulation'
  -----------------------------------------
  'Index amplitude' refers to the FM modulation index (:ix) controlling the
  spectral amplitude distribution of the modulator signal: higher :ix produces
  more and stronger sidebands in the FM oscillator, which then propagate into
  the modulation applied to the input.

  'Ring modulation' is the :bs=0 case — the FM oscillator has zero mean (it
  is a pure sinusoidal/FM signal, bounded ±1 with no DC), so multiplying the
  input by it produces only sum-and-difference components with no carrier.

  The full AM/RM axis (from ex.158) is preserved via :bs:
      :bs=0: ring modulation by FM oscillator — carrier absent
      :bs=1: amplitude modulation by FM oscillator — carrier preserved
      :bs ∈ (0,1): smooth transition

  Spectral consequences
  ---------------------
  With a sinusoidal input at fi and FM oscillator at fc with index :ix:

  :bs=0 (:ix=0):  y = sin(fi) × sin(fc)
                  Sidebands: fi±fc — pure ring mod (ex.157 territory)

  :bs=0 (:ix>0):  y = sin(fi) × FM(fc, fm, ix)
                  FM expands: fm_osc has components at fc+n×fm (Bessel J_n(ix))
                  Each component ring-modulates the input:
                      sidebands at fi ± (fc + n×fm)  for all n
                  A ring-of-rings — far denser than single-sinusoidal RM

  :bs=1 (:ix>0):  y = sin(fi) × (1 + FM(fc, fm, ix))
                  Carrier preserved at fi; plus the ring-of-rings sidebands
                  = fi, fi±fc, fi±(fc±fm), fi±(fc±2fm), …

  C:M ratio (:rt) and input frequency (fi) together determine whether the
  output sideband grid is harmonic or inharmonic.  Three independent
  frequency parameters now govern the spectrum: fc, fm (= fc×:rt), and fi.

  Relationship to ex.158 (AM-depth) and ex.157 (AM)
  ---------------------------------------------------
  ex.157:  in × (1 + mx × sin(fc))       — AM, sine modulator, :mx controls depth
  ex.158:  in × (:bs + :dp × sin(fc))    — AM/RM axis + depth, sine modulator
  ex.171:  in × (:bs + FM(fc, fm, ix))   — AM/RM axis, FM/PM modulator

  ex.171 is a strict generalisation: setting :ix=0 (no FM modulation) and
  :rt=anything produces the same output as ex.158 with :dp=1 and the same :bs.

  FM/PM routing (:md)
  --------------------
  The FM oscillator uses the same :md morph as ex.161–170:
      fm_osc = sin(2π × phasor(fc + (1−md)×ix×fm×osc(fm)) + md×ix×osc(fm))

  At :md=0 (FM): modulator routes to phasor frequency
  At :md=1 (PM): modulator routes to phase output
  The audio input is unaffected by :md directly; only the FM oscillator
  spectrum changes.

  Interesting parameter regions
  ------------------------------
  :bs=0 :ix=0  :rt=1.0   → sin(fi) × sin(fc): pure ring mod (ex.159 RM output)
  :bs=1 :ix=0  :rt=1.0   → sin(fi) × (1+sin(fc)): pure AM (ex.157)
  :bs=0 :ix=2  :rt=1.0   → ring mod by FM sig; dense sideband clusters around fi±fc
  :bs=0 :ix=2  :rt=1.4   → inharmonic FM ring mod; bell-like spectral shifting
  :bs=0 :ix=2  :rt=2.0   → hollow FM ring mod (odd-only FM modulator)
  :bs=1 :ix=3  :rt=0.5   → AM with sub-FM modulator; rich sub-harmonic content
  :bs=0 :ix=5  :rt=φ     → extreme inharmonic FM ring mod; metallic noise-like

  Parameters
  ----------
  :fc — FM oscillator carrier frequency in Hz (0.1–4000; default 100)
  :rt — C:M ratio for FM oscillator (fm = fc × :rt; 0.1–8.0; default 1.0)
  :ix — FM modulation index (0–10; default 0.0)
  :bs — AM/RM bias; 1.0=AM (carrier preserved), 0.0=RM (0–1; default 0.0)
  :md — FM→PM morph inside the FM oscillator (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  in: audio signal to be modulated
    :out — in × (:bs + FM/PM oscillator)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! fmpm-ifod
  {:params {:fc {:range [0.1 4000.0]  :default 100.0}
            :rt {:range [0.1 8.0]     :default 1.0}
            :ix {:range [0.0 10.0]    :default 0.0}
            :bs {:range [0.0 1.0]     :default 0.0}
            :md {:range [0.0 1.0]     :default 0.0}}}
  (let [in  (audio-in)
        fc  (param :fc)
        rt  (param :rt)
        ix  (param :ix)
        bs  (param :bs)
        md  (param :md)
        fm  (faust "%fc*%rt" {:fc fc :rt rt})
        mo  (faust "os.osc(%fm)" {:fm fm})
        fo  (faust "sin(2.0*ma.PI*os.phasor(1,%fc+(1.0-%md)*%ix*%fm*%mo)+%md*%ix*%mo)"
                   {:fc fc :fm fm :ix ix :md md :mo mo})
        out (faust "%in*(%bs+%fo)" {:in in :bs bs :fo fo})]
    (output :out out)))
