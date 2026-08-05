; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.129-svf
  "GSOT pp.188-190 — go.svf.gendsp (Chapter 6).

  'State Variable Filter — ZDF Trapezoidal SVF'
  -----------------------------------------------
  The state variable filter simultaneously produces lowpass, bandpass, and
  highpass outputs from a network of two coupled integrators.  This patch
  implements the ZDF (Zero-Delay Feedback) variant using trapezoidal
  integration — the same integration method as the one-pole filter (ex.127)
  but extended to second order.

  Continuous-time prototype
  --------------------------
  The three SVF outputs satisfy the following continuous differential equations:

      hp = in − 2R·bp − lp
      bṗ = hp         (first integrator)
      lṗ = bp         (second integrator)

  Solving algebraically: hp is defined in terms of bp and lp, which in turn
  are defined via integrals of hp.  This creates an algebraic loop at the
  current time step.

  ZDF reformulation (Zavalishin)
  --------------------------------
  The ZDF approach stores *transformed* states (t-states) s1 and s2 instead
  of bp and lp directly.  The t-states represent the state of the trapezoidal
  integrator at the midpoint of each sample interval:

      bp[n] = g·hp[n] + s1[n−1]       (first integrator output)
      lp[n] = g·bp[n] + s2[n−1]       (second integrator output)
      hp[n] = (in[n] − k·s1[n−1] − s2[n−1]) / dn    (algebraic hp)

  where  g = tan(π·fc/SR),  k = 1/Q,  dn = 1 + k·g + g².

  No algebraic loop: hp[n] depends only on PAST states s1[n−1] and s2[n−1],
  not on the current bp or lp.

  State update recurrences
  -------------------------
  After computing hp[n], bp[n], lp[n]:

      s1[n] = 2·bp[n] − s1[n−1] = 2·g·hp[n] + s1[n−1]
      s2[n] = 2·lp[n] − s2[n−1] = 2·g·bp[n] + s2[n−1]

  The factor-of-2 arises from the trapezoidal approximation: the midpoint
  state s1 is updated to reflect the full-step transition.

  Implementation strategy — nested Faust 'with' block
  -----------------------------------------------------
  The s1 and s2 recurrences are MUTUALLY DEPENDENT:
    s1 uses s2[n−1] (past s2) to compute hp
    s2 uses s1[n−1] (past s1) to compute bp/hp

  In Alembic's sequential let bindings, one symbol cannot reference another
  that is defined later.  The solution is to express BOTH integrators inside
  a single faust string with a nested Faust 'with' block:

      n_svf = (hp, bp, lp)
        with {
          s1 = (2.0*n_gv*hp+_)~_;
          s2 = (2.0*n_gv*bp+_)~_;
          hp = (n0-n_kk*(s1@1)-(s2@1))/n_dn;
          bp = n_gv*hp+(s1@1);
          lp = n_gv*bp+(s2@1);
        };

  Faust's 'with' block is DECLARATIVE (no sequential evaluation constraint),
  so it resolves the s1↔s2 mutual dependency correctly by detecting that all
  cross-state references use @1 (past values only) and therefore have no
  combinatorial loop.

  The nested 'with' produces a 3-channel signal (hp, bp, lp).  Three routing
  nodes extract each channel using Faust's sequential composition:

      n_hp = n_svf:(_, !, !);   — first channel
      n_bp = n_svf:(!, _, !);   — second channel
      n_lp = n_svf:(!, !, _);   — third channel

  Type selection
  ---------------
  The :type parameter [0,2] selects the output mode:

      0.0 — LP (lowpass)   6 dB/oct roll-off above fc
      1.0 — BP (bandpass)  peaked at fc, 6 dB/oct on each side
      2.0 — HP (highpass)  6 dB/oct roll-off below fc

  Non-integer values morph between adjacent modes via select2 trees.

  Self-oscillation
  -----------------
  At Q→∞ (k→0): the SVF becomes a lossless resonator.  At exactly k=0,
  the filter self-oscillates at fc.  In practice k is clamped above a small
  positive value by the :q range floor (0.1 → k≤10; high Q, not oscillation).

  Comparing the GSOT SVF family
  --------------------------------
  example 127  go.onepole.hz       1st-order ZDF LP; single ~_; g=tan(·)/1+g)
  example 128  go.svf.coeffs       coefficient-only subpatch (g, k, dn)
  example 129  go.svf (this patch) 2nd-order ZDF SVF; two ~_ integrators

  Implementation strategy — Faust tuple recursion
  --------------------------------------------------
  The s1 and s2 state updates are MUTUALLY DEPENDENT:
    s1 needs s2[n−1] (past s2) via the hp formula
    s2 needs s1[n−1] (past s1) via the bp formula

  Using Faust's ~_ with @1 cross-references triggers an 'endless evaluation
  cycle' in Faust's normalising evaluator.  The correct approach is to encode
  BOTH states as explicit function parameters in a named Faust function, then
  use ~ (_, _) for tuple recursion.  Faust's ~ operator introduces the unit
  delays cleanly without any @1 references.

      svf_kern(s1p, s2p) = hp, bp, lp, s1n, s2n
        with {
          hp   = (n0 - n_kk*s1p - s2p) / n_dn ;
          bp   = n_gv*hp + s1p ;
          lp   = n_gv*bp + s2p ;
          s1n  = 2.0*n_gv*hp + s1p ;
          s2n  = 2.0*n_gv*bp + s2p ;
        };
      n_svf = svf_kern ~ (_, _) ;
        -- last 2 of 5 outputs (s1n, s2n) feed back as first 2 inputs (s1p, s2p)
        -- primary outputs (first 3): hp, bp, lp

  Emitted Faust DSP (simplified):
      n_gv  = tan(ma.PI*n_hz/ma.SR);
      n_kk  = 1.0/n_qq;
      n_dn  = 1.0+n_kk*n_gv+n_gv*n_gv;
      n_svf = svf_kern ~ (_, _)
        with {
          svf_kern(s1p, s2p) = hp, bp, lp, s1n, s2n
            with {
              hp = (n0-n_kk*s1p-s2p)/n_dn;
              bp = n_gv*hp+s1p;
              lp = n_gv*bp+s2p;
              s1n = 2.0*n_gv*hp+s1p;
              s2n = 2.0*n_gv*bp+s2p;
            };
        };
      n_hp  = n_svf:(_, !, !);
      n_bp  = n_svf:(!, _, !);
      n_lp  = n_svf:(!, !, _);

  Parameters
  ----------
  :hz   — cutoff frequency in Hz (20–20000; default 1000)
  :q    — resonance Q (0.1–20; default 0.707 = Butterworth)
  :type — output mode: 0=LP, 1=BP, 2=HP (0.0–2.0; default 0.0)

  Audio inputs / Outputs
  ----------------------
  in: signal to filter  →  :out: type-selected SVF output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! svf
  {:params {:hz   {:range [20.0 20000.0] :default 1000.0}
            :q    {:range [0.1 20.0]    :default 0.707}
            :type {:range [0.0 2.0]     :default 0.0}}}
  (let [in   (audio-in)
        hz   (param :hz)
        q    (param :q)
        tp   (param :type)
        ; Coefficients (see ex.128 svf-coeffs)
        gv   (faust "tan(ma.PI*%{hz}/ma.SR)" {:hz hz})
        kk   (faust "1.0/%{qq}" {:qq q})
        dn   (faust "1.0+%{kk}*%{gv}+%{gv}*%{gv}" {:kk kk :gv gv})
        ; SVF kernel — two coupled ZDF integrators via Faust tuple recursion.
        ;
        ; Mutual dependency s1↔s2 CANNOT be expressed with separate ~_ nodes
        ; in Alembic's sequential let: s1 needs s2[n-1] and s2 needs s1[n-1].
        ; Faust's @1 on a ~_ signal triggers an "endless evaluation cycle" in
        ; the normalising evaluator.
        ;
        ; SOLUTION: encode both states as explicit FUNCTION PARAMETERS (s1p, s2p)
        ; in a named Faust function, then use ~ (_, _) tuple recursion.
        ; Faust's ~ operator properly introduces unit delays for the feedback
        ; signals — no @1 needed.
        ;
        ; svf_kern(s1p, s2p) = hp, bp, lp, s1n, s2n
        ;   s1p, s2p: feedback inputs (previous-step state)
        ;   hp, bp, lp: primary outputs (first 3 of 5 total)
        ;   s1n, s2n: feedback outputs (last 2 of 5 total)
        ;
        ; With svf_kern ~ (_, _):
        ;   (_, _): B is 2→2 identity (routes feedback back)
        ;   Last 2 outputs of svf_kern (s1n, s2n) → B → first 2 inputs (s1p, s2p)
        ;   Primary outputs (first 3): hp, bp, lp → 3-channel n_svf signal
        ;
        ; n0, n_gv, n_kk, n_dn are free variables from the outer function scope.
        ; Produces a 3-channel signal: (hp, bp, lp).
        svf  (faust
               "svf_kern ~ (_, _)\n  with {\n    svf_kern(s1p, s2p) = hp, bp, lp, s1n, s2n\n      with {\n        hp = (%{in}-%{kk}*s1p-s2p)/%{dn};\n        bp = %{gv}*hp+s1p;\n        lp = %{gv}*bp+s2p;\n        s1n = 2.0*%{gv}*hp+s1p;\n        s2n = 2.0*%{gv}*bp+s2p;\n      };\n  }"
               {:gv gv :in in :kk kk :dn dn})
        ; Extract individual channels from the 5-channel svf node.
        ; Faust's ~ keeps ALL outputs as primary AND routes the last K through
        ; feedback. svf_kern ~ (_, _) produces 5 channels:
        ;   ch0=hp, ch1=bp, ch2=lp, ch3=s1n, ch4=s2n (s1n/s2n also feed back)
        ; (_, !, !, !, !) selects ch0; (!, _, !, !, !) selects ch1; etc.
        hp   (faust "%{sv}:(_, !, !, !, !)" {:sv svf})
        bp   (faust "%{sv}:(!, _, !, !, !)" {:sv svf})
        lp   (faust "%{sv}:(!, !, _, !, !)" {:sv svf})
        ; Type selector: 0=LP, 1=BP, 2=HP via nested select2
        out  (faust "select2(%{tp}<1.5,select2(%{tp}<0.5,%{lp},%{bp}),%{hp})"
               {:tp tp :lp lp :bp bp :hp hp})]
    (output :out out)))
