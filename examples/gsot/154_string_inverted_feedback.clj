; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.154-string-inverted-feedback
  "GSOT p.221 — string_inverted_feedback.maxpat (Chapter 8).

  'What Happens in the Feedback Loop? — Inverting the Feedback Signal'
  --------------------------------------------------------------------
  Negating the Karplus-Strong feedback signal changes the resonant condition
  from constructive to destructive interference at even multiples of 1/D Hz,
  and constructive interference at odd multiples of 1/(2D) Hz.

  Standard feedback (ex.151):   y[n] = x[n] + g·lp(y[n−D])
  Inverted feedback (this patch): y[n] = x[n] − g·lp(y[n−D])

  Resonance analysis
  -------------------
  The loop oscillates where the round-trip gain equals unity.  With the
  averaging LP (H_lp = 0.5·(1+z^{-1})):

  Standard (+g): loop gain = g·H_lp·z^{-D} = 1  at  f = k/D Hz  (k = 0,1,2,...)
  Inverted (−g): loop gain = −g·H_lp·z^{-D} = 1  at  f = (2k+1)/(2D) Hz

  Inverted resonances: 1/(2D), 3/(2D), 5/(2D), 7/(2D) ... Hz
  These are ODD multiples of 1/(2D) — no even harmonics, no DC component.

  The fundamental resonance is at 1/(2D) = :hz/2 — ONE OCTAVE BELOW :hz.

  Set :hz = 2× the desired pitch to compensate, or accept the octave shift
  as the intended effect (a 'subharmonic string').

  Spectral character
  -------------------
  Odd harmonics only → clarinet-like timbre (hollow, woody).
  Physical analogy: a cylindrical tube closed at one end resonates at
  odd multiples of the quarter-wave frequency — the closed-end condition
  is equivalent to a sign inversion at the reflection point.

  Compare to even harmonics: a cylindrical tube open at both ends resonates
  at all multiples (1/D, 2/D, 3/D...) — the open-end condition preserves sign.

  The averaging LP still causes high harmonics to decay faster, so the timbre
  darkens over time exactly as in the non-inverted case.

  Relationship to standard K-S
  -----------------------------
  ex.151 string_basic:           y = x + g · lp(y[n-D])  — all harmonics
  ex.154 string_inverted_feedback: y = x − g · lp(y[n-D])  — odd harmonics only

  Both use the same integer-delay structure (D = SR/:hz − 1 additional samples
  after the ~ _ implicit 1-sample delay).  The only difference is the sign.

  Parameters
  ----------
  :hz — delay parameter in Hz; actual resonant fundamental is :hz/2 (0.5 oct below)
        Set to 2× desired pitch for same perceived fundamental as ex.151.
        (20–2000; default 440 → resonant fundamental at 220 Hz = A3)
  :gn — loop gain (0–0.999; default 0.99)

  Audio inputs / Outputs
  ----------------------
  in: excitation signal  →  :out: inverted-feedback odd-harmonic string output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! string-inverted-feedback
  {:params {:hz {:range [20.0 2000.0] :default 440.0}
            :gn {:range [0.0 0.999]   :default 0.99}}}
  (let [in  (audio-in)
        hz  (param :hz)
        gn  (param :gn)
        dl  (faust "int(max(1.0,ma.SR/%{hz}))" {:hz hz})
        dl1 (faust "max(0,%{dl}-1)" {:dl dl})
        out (faust "stri_loop ~ _\n  with { stri_loop(s) = %{in}-%{gn}*de.delay(int(ma.SR*5.0),%{d1},0.5*(s+s@1)); }"
                   {:in in :gn gn :d1 dl1})]
    (output :out out)))
