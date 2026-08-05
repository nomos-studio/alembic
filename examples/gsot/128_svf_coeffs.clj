; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.128-svf-coeffs
  "GSOT pp.188-190 — go.svf.coeffs (Chapter 6).

  'State Variable Filter — Coefficients Subpatch'
  ------------------------------------------------
  The SVF coefficients are derived from the bilinear transform of the
  continuous-time SVF prototype:

      hp = in − 2R·bp − lp      (continuous)
      bp = ∫ hp  dt
      lp = ∫ bp  dt

  Discretising the two integrators with the trapezoidal rule (bilinear
  transform / Tustin method) introduces the warp factor g = tan(π·fc/SR).
  This is the same g used in the one-pole trapezoidal filter (ex.127).

  ZDF (Zero-Delay Feedback) reformulation
  ----------------------------------------
  The 'zero-delay' approach avoids the algebraic loop that arises in the
  naive Chamberlin formulation (where hp = in − 2R·bp − lp and bp depends
  on hp).  Instead of storing bp and lp directly, we store transformed
  t-states s1 and s2 such that:

      s1[n] = 2·bp[n] − s1[n−1]   ↔   bp[n]  = g·hp[n] + s1[n−1]
      s2[n] = 2·lp[n] − s2[n−1]   ↔   lp[n]  = g·bp[n] + s2[n−1]

  Substituting into hp:

      hp[n] = (in[n] − 2R·(g·hp[n]+s1) − (g·bp[n]+s2)) / (1 + 2Rg + g²)

  Solving for hp — the denominator is the coefficient dn below.

  Coefficient definitions
  -----------------------
      g   = tan(π·fc/SR)        bilinear warp factor (same as ex.127)
      k   = 1/Q = 2R            damping coefficient (k=0 → self-oscillation)
      dn  = 1 + k·g + g²        common denominator (resolves the algebraic loop)

  These three values are computed once per block and shared across all SVF
  branches (hp, bp, lp).  In gen~ this is a separate 'coefficients' subpatch
  so that the coefficient computation is not duplicated if multiple SVF
  instances share the same fc/Q setting.

  In Alembic, a patch is a complete signal processor; there is no reusable
  subpatch primitive.  Example 128 demonstrates the coefficient computation
  as a standalone patch (outputting g, k, dn on channels 0–2) to mirror the
  GSOT chapter structure.  Example 129 combines coefficients and SVF body
  into the complete go.svf.gendsp patch.

  Relationship to other examples
  --------------------------------
  example 107  go.allpass.hz      g = tan(π·fc/SR) — same warp factor
  example 127  go.onepole.hz      g and k = g/(1+g) — simplified 1-pole case
  example 128  go.svf.coeffs      this patch — full 2nd-order g, k, dn
  example 129  go.svf             complete ZDF SVF (uses these coefficients)

  Parameter Q vs. damping coefficient k
  ----------------------------------------
  The quality factor Q (resonance) relates to the damping coefficient k via:

      k = 1/Q = 2R

  At Q=0.707 (1/√2): k = √2 ≈ 1.414 — Butterworth (maximally flat) response
  At Q=1.0:          k = 1.0          — single peak at fc
  At Q→∞:           k → 0.0          — self-oscillation threshold

  Outputs
  -------
  :out0  (ch 4) — g   = tan(π·fc/SR)   bilinear warp
  :out1  (ch 5) — k   = 1/Q            damping coefficient
  :out2  (ch 6) — dn  = 1+k·g+g²      denominator"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! svf-coeffs
  {:params {:hz {:range [20.0 20000.0] :default 1000.0}
            :q  {:range [0.1 20.0]    :default 0.707}}}
  (let [hz  (param :hz)
        q   (param :q)
        gv  (faust "tan(ma.PI*%{hz}/ma.SR)" {:hz hz})
        kk  (faust "1.0/%{qq}" {:qq q})
        dn  (faust "1.0+%{kk}*%{gv}+%{gv}*%{gv}" {:kk kk :gv gv})]
    (output :out0 gv)
    (output :out1 kk)
    (output :out2 dn)))
