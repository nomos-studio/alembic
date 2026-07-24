; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.174-pm-asymmetric
  "GSOT pp.251-253 — PM-asymmetric.maxpat (Chapter 8: Frequent Modulations).

  'Using AM for Asymmetric PM Spectra'
  --------------------------------------
  Standard PM produces symmetric sidebands.  For a sine modulator with
  index β, the sideband amplitudes are J_n(β) at fc + n×fm for all integers n.
  Since J_{-n}(β) = (−1)^n × J_n(β), the magnitudes |J_n| = |J_{-n}| are
  equal — upper and lower sidebands at fc ± n×fm have the same amplitude.

  This patch breaks that symmetry by multiplying the PM carrier signal by a
  quadrature AM term:

      y = (1 + mx × cos(2π×fm×t)) × sin(2π×fc×t + ix × sin(2π×fm×t))

  The sine modulates phase (PM); the cosine modulates amplitude (AM); both
  derive from the same phasor — they are in exact quadrature (90° apart).

  Why the quadrature relationship creates asymmetry
  --------------------------------------------------
  Expanding the AM × PM product:

      y = sin(PM)  +  mx × cos(fm×t) × sin(PM)

  First term A = sin(PM) = Σ J_n(β) × sin((fc + n×fm)×t)  — symmetric PM

  Second term B = mx×cos(fm×t) × A
    = (mx/2) × Σ J_n(β) × [sin((fc+(n+1)fm)t) + sin((fc+(n−1)fm)t)]

  Re-indexing B to collect terms at fc + k×fm:
    coefficient at k:  (mx/2) × (J_{k−1}(β) + J_{k+1}(β))

  Bessel recurrence: J_{n−1}(β) + J_{n+1}(β) = (2n/β) × J_n(β)
    → B coefficient at k: (mx/2) × (2k/β) × J_k(β) = mx×k/β × J_k(β)

  Combined amplitude at fc + k×fm:
      J_k(β)  +  mx×k/β × J_k(β)  =  J_k(β) × (1 + k × mx/β)

  Upper sidebands (k > 0):  factor > 1  — STRONGER than symmetric PM
  Lower sidebands (k < 0):  factor < 1  — WEAKER   than symmetric PM

  At k=+1: J_1(β) × (1 + mx/β)    upper first sideband amplified
  At k=−1: J_1(β) × (1 − mx/β)    lower first sideband reduced
  At mx = β: lower first sideband cancelled; upper doubled
  At mx > β: lower sideband sign inverts (phase inversion of that component)

  Why quadrature specifically?
  ----------------------------
  If the AM and PM modulator were the SAME signal (both sine), the interaction
  term would produce cosine sidebands — a *phase* shift rather than *amplitude*
  asymmetry.  The amplitude spectrum would remain symmetric.

  Only when AM and PM are in quadrature (cos/sin from the same phasor) does
  the interaction produce sine sidebands that add directly to the PM sidebands,
  creating genuine amplitude asymmetry.  The orthogonality of sin and cos is
  what allows them to interfere constructively on one side and destructively
  on the other.

  Shared phasor
  -------------
  Both ms (sine, for PM) and mc (cosine, for AM) derive from the same phasor:

      ph  = os.phasor(1, fm)
      ms  = sin(2π × ph)     — PM modulator
      mc  = cos(2π × ph)     — AM modulator

  This guarantees exact 90° phase relationship.  Using two independent
  oscillators would introduce phase drift and degrade the asymmetry.

  Asymmetry spectrum summary
  --------------------------
  :mx = 0:          symmetric PM — identical to ex.161 at :md=1 with same β
  :mx = 0.5 × :ix:  mild asymmetry; upper side about 50% stronger than lower
  :mx = :ix:        maximum useful range; lower first sideband cancelled
  :mx > :ix:        lower sidebands sign-inverted; complex spectral behaviour
  :mx = 1 (clamp):  practical ceiling; avoids (1 + k×mx/β) < 0 for k≤1

  Effect on perceived timbre
  ---------------------------
  Symmetric FM/PM sounds 'centred' — upper and lower sidebands contribute
  equally to timbre.  Asymmetric PM shifts spectral weight toward the upper
  or lower sidebands, producing a brighter or darker colouring respectively.
  At strong :mx the suppressed lower sidebands give a more 'single-sideband'
  quality — closer to vocoded or pitch-shifted sound than classic FM.

  Parameters
  ----------
  :fc — carrier frequency in Hz (20–2000; default 220)
  :rt — C:M ratio (fm = fc × :rt; 0.1–8.0; default 1.0)
  :ix — PM modulation index β (0–10; default 2.0)
  :mx — AM depth for asymmetry; 0=symmetric, :ix=maximum (0–:ix; default 0.5)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained asymmetric PM synthesizer)
    :out — (1 + mx×cos(2π×fm×t)) × sin(2π×fc×t + ix×sin(2π×fm×t))"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! pm-asymmetric
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :rt {:range [0.1 8.0]     :default 1.0}
            :ix {:range [0.0 10.0]    :default 2.0}
            :mx {:range [0.0 1.0]     :default 0.5}}}
  (let [fc  (param :fc)
        rt  (param :rt)
        ix  (param :ix)
        mx  (param :mx)
        fm  (faust "%fc*%rt" {:fc fc :rt rt})
        ph  (faust "os.phasor(1,%fm)" {:fm fm})
        ms  (faust "sin(2.0*ma.PI*%ph)" {:ph ph})
        mc  (faust "cos(2.0*ma.PI*%ph)" {:ph ph})
        out (faust "(1.0+%mx*%mc)*sin(2.0*ma.PI*os.phasor(1,%fc)+%ix*%ms)"
                   {:mx mx :mc mc :fc fc :ix ix :ms ms})]
    (output :out out)))
