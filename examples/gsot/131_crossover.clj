; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.131-crossover
  "GSOT pp.191-192 — crossover.maxpat (Chapter 6).

  'Crossover — SVF-Based Two-Way Split'
  ----------------------------------------
  The full GSOT crossover uses the ZDF state variable filter (ex.129) to
  produce LP and HP bands simultaneously from a single shared filter state.
  This is more efficient than running two independent filters: both bands
  emerge from the same two integrators with zero additional state cost.

  Why the SVF is ideal for crossovers
  --------------------------------------
  The SVF simultaneously computes:

      hp = (in − k·s1 − s2) / dn       highpass
      bp = g·hp + s1                     bandpass
      lp = g·bp + s2                     lowpass

  LP and HP share the same poles (same g, k, dn, same integrator states
  s1/s2) and therefore have a consistent phase relationship through the
  crossover region.  At Q = 1/√2 (Butterworth), the LP and HP magnitude
  responses cross at −3 dB at fc and their sum is nearly flat.

  Comparison to the simple crossover (ex.130)
  ---------------------------------------------
                  ex.130 (simple)      ex.131 (this patch)
  Filter order    1st (one-pole)       2nd (ZDF SVF)
  Slope           6 dB/oct             12 dB/oct
  LP + HP         = x exactly          ≈ flat (Q-dependent)
  State           1 (lp feedback)      2 (s1, s2 shared)
  Q control       none                 :q parameter

  Q and crossover response
  --------------------------
  Q = 0.707 (1/√2): Butterworth — minimal ringing, smooth transition
  Q = 0.5:          LP and HP sum flat in voltage (Linkwitz-Riley character)
  Q > 1.0:          amplitude bump at fc in both LP and HP; narrow transition

  For a flat-summing crossover, Q = 0.5 is the closest approximation using
  a single SVF stage.  For true 4th-order Linkwitz-Riley flat summation,
  cascade two SVF stages at Q = 0.707 (see go.biquad4.lp/hp, ex.118/119).

  Implementation
  ---------------
  Reuses the SVF kernel introduced in example 129.  svf_kern(s1p, s2p) is
  defined locally in the faust string and called with ~ (_, _) tuple recursion.
  Outputs 5 channels (hp, bp, lp, s1n, s2n); routing nodes extract hp (ch0)
  and lp (ch2) with (_, !, !, !, !) / (!, !, _, !, !).

  Outputs
  -------
  :out0  (ch 4) — lp: low-frequency band (12 dB/oct above fc)
  :out1  (ch 5) — hp: high-frequency band (12 dB/oct below fc)

  Parameters
  ----------
  :hz  — crossover frequency in Hz (20–20000; default 1000)
  :q   — resonance Q (0.1–20; default 0.707 = Butterworth)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! crossover
  {:params {:hz {:range [20.0 20000.0] :default 1000.0}
            :q  {:range [0.1 20.0]    :default 0.707}}}
  (let [in  (audio-in)
        hz  (param :hz)
        q   (param :q)
        ; SVF coefficients (same as ex.128/129)
        gv  (faust "tan(ma.PI*%{hz}/ma.SR)" {:hz hz})
        kk  (faust "1.0/%{qq}" {:qq q})
        dn  (faust "1.0+%{kk}*%{gv}+%{gv}*%{gv}" {:kk kk :gv gv})
        ; SVF kernel via Faust tuple recursion (see ex.129 for full derivation).
        ; svf_kern(s1p, s2p) = hp, bp, lp, s1n, s2n  (5-channel output).
        ; ~ (_, _) routes last 2 outputs (s1n, s2n) back as (s1p, s2p).
        svf (faust
              "svf_kern ~ (_, _)\n  with {\n    svf_kern(s1p, s2p) = hp, bp, lp, s1n, s2n\n      with {\n        hp = (%{in}-%{kk}*s1p-s2p)/%{dn};\n        bp = %{gv}*hp+s1p;\n        lp = %{gv}*bp+s2p;\n        s1n = 2.0*%{gv}*hp+s1p;\n        s2n = 2.0*%{gv}*bp+s2p;\n      };\n  }"
              {:gv gv :in in :kk kk :dn dn})
        ; Extract HP (ch0) and LP (ch2) from the 5-channel SVF signal.
        hp  (faust "%{sv}:(_, !, !, !, !)" {:sv svf})
        lp  (faust "%{sv}:(!, !, _, !, !)" {:sv svf})]
    (output :out0 lp)
    (output :out1 hp)))
