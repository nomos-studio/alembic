; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.117-biquad-coeffs
  "GSOT pp.181 — go.biquad.coeffs.gendsp / biquad-coefficients.maxpat (Chapter 6).

  'Biquad Filter Coefficients — Type Selector'
  ---------------------------------------------
  Meta-patch combining all eight biquad filter types into a single
  parameterised filter.  An integer :type parameter selects the
  coefficient formula; a shared Direct Form II biquad applies the result.

  Type map
  --------
  0 = LP   lowpass          (go.biquad.lp,  example 109)
  1 = HP   highpass         (go.biquad.hp,  example 110)
  2 = BP   bandpass         (go.biquad.bp,  example 111)
  3 = RES  resonant/peaking (go.biquad.res, example 112)
  4 = NP   notch            (go.biquad.np,  example 113)
  5 = AP   2nd-order allpass(go.biquad.ap,  example 114)
  6 = LS   low shelf        (go.biquad.ls,  example 115)
  7 = HS   high shelf       (go.biquad.hs,  example 116)

  Architecture
  ------------
  1. Shared intermediates: w0, cos(w0), sin(w0), alpha, A, 2√A·alpha
  2. Per-type denominator a0 (4 variants: basic, res, ls, hs)
  3. All 8 type-specific b0/b1/b2/a1/a2 values computed in parallel
  4. Five select2 trees pick the active coefficient set by :type
  5. Unified Direct Form II biquad applied with selected coefficients

  In Faust, all branches of select2 are evaluated simultaneously;
  the selector merely routes the active branch to the output.

  Parameters
  ----------
  :type — filter type 0–7 (integer; default 0 = LP)
  :hz   — cutoff/centre/shelf frequency in Hz (20–20000; default 1000)
  :q    — resonance / bandwidth Q (0.1–20; default 0.707)
  :gain — peak / shelf gain in dB (−40–40; default 0; active for RES/LS/HS)

  Audio inputs  / Outputs
  -----------------------
  in: signal to filter  →  :out: biquad filtered output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! biquad-coeffs
  {:params {:type {:range [0.0 7.0]     :default 0.0}
            :hz   {:range [20.0 20000.0] :default 1000.0}
            :q    {:range [0.1 20.0]    :default 0.707}
            :gain {:range [-40.0 40.0]  :default 0.0}}}
  (let [audio (audio-in)
        tp    (param :type)
        hz    (param :hz)
        q     (param :q)
        gain  (param :gain)

        ;; Shared trig intermediates
        w0    (faust "2.0*ma.PI*%{hz}/ma.SR" {:hz hz})
        cw    (faust "cos(%{w0})" {:w0 w0})
        sw    (faust "sin(%{w0})" {:w0 w0})
        al    (faust "%{sw}/(2.0*%{qq})" {:sw sw :qq q})
        ga    (faust "pow(10.0,%{gn}/40.0)" {:gn gain})
        sa    (faust "2.0*sqrt(%{ga})*%{al}" {:ga ga :al al})

        ;; Denominator a0 variants
        a0b   (faust "1.0+%{al}"                           {:al al})
        a0r   (faust "1.0+%{al}/%{ga}"                       {:al al :ga ga})
        a0l   (faust "(%{ga}+1.0)+(%{ga}-1.0)*%{cw}+%{sa}"      {:ga ga :cw cw :sa sa})
        a0h   (faust "(%{ga}+1.0)-(%{ga}-1.0)*%{cw}+%{sa}"      {:ga ga :cw cw :sa sa})

        ;; b0 per type
        b0lp  (faust "(1.0-%{cw})/(2.0*%{a0})"               {:cw cw :a0 a0b})
        b0hp  (faust "(1.0+%{cw})/(2.0*%{a0})"               {:cw cw :a0 a0b})
        b0bp  (faust "%{sw}/(2.0*%{a0})"                     {:sw sw :a0 a0b})
        b0rs  (faust "(1.0+%{al}*%{ga})/%{a0}"                 {:al al :ga ga :a0 a0r})
        b0np  (faust "1.0/%{a0}"                           {:a0 a0b})
        b0ap  (faust "(1.0-%{al})/%{a0}"                     {:al al :a0 a0b})
        b0ls  (faust "%{ga}*((%{ga}+1.0)-(%{ga}-1.0)*%{cw}+%{sa})/%{a0}" {:ga ga :cw cw :sa sa :a0 a0l})
        b0hs  (faust "%{ga}*((%{ga}+1.0)+(%{ga}-1.0)*%{cw}+%{sa})/%{a0}" {:ga ga :cw cw :sa sa :a0 a0h})

        ;; b1 per type (bp=0, ap shares value with np)
        b1lp  (faust "(1.0-%{cw})/%{a0}"                    {:cw cw :a0 a0b})
        b1hp  (faust "-(1.0+%{cw})/%{a0}"                   {:cw cw :a0 a0b})
        b1np  (faust "-2.0*%{cw}/%{a0}"                     {:cw cw :a0 a0b})
        b1rs  (faust "-2.0*%{cw}/%{a0}"                     {:cw cw :a0 a0r})
        b1ls  (faust "2.0*%{ga}*((%{ga}-1.0)-(%{ga}+1.0)*%{cw})/%{a0}" {:ga ga :cw cw :a0 a0l})
        b1hs  (faust "-2.0*%{ga}*((%{ga}-1.0)+(%{ga}+1.0)*%{cw})/%{a0}" {:ga ga :cw cw :a0 a0h})

        ;; b2 per type (lp=b0lp, hp=b0hp, np=b0np reused; ap=1.0 literal)
        b2bp  (faust "-%{b0}" {:b0 b0bp})
        b2rs  (faust "(1.0-%{al}*%{ga})/%{a0}"                {:al al :ga ga :a0 a0r})
        b2ls  (faust "%{ga}*((%{ga}+1.0)-(%{ga}-1.0)*%{cw}-%{sa})/%{a0}" {:ga ga :cw cw :sa sa :a0 a0l})
        b2hs  (faust "%{ga}*((%{ga}+1.0)+(%{ga}-1.0)*%{cw}-%{sa})/%{a0}" {:ga ga :cw cw :sa sa :a0 a0h})

        ;; a1 per type (LP/HP/BP/NP/AP share a1b; RES/LS/HS differ)
        a1b   (faust "-2.0*%{cw}/%{a0}"                     {:cw cw :a0 a0b})
        a1r   (faust "-2.0*%{cw}/%{a0}"                     {:cw cw :a0 a0r})
        a1l   (faust "-2.0*((%{ga}-1.0)+(%{ga}+1.0)*%{cw})/%{a0}" {:ga ga :cw cw :a0 a0l})
        a1h   (faust "2.0*((%{ga}-1.0)-(%{ga}+1.0)*%{cw})/%{a0}" {:ga ga :cw cw :a0 a0h})

        ;; a2 per type
        a2b   (faust "(1.0-%{al})/%{a0}"                    {:al al :a0 a0b})
        a2r   (faust "(1.0-%{al}/%{ga})/%{a0}"                {:al al :ga ga :a0 a0r})
        a2l   (faust "((%{ga}+1.0)+(%{ga}-1.0)*%{cw}-%{sa})/%{a0}" {:ga ga :cw cw :sa sa :a0 a0l})
        a2h   (faust "((%{ga}+1.0)-(%{ga}-1.0)*%{cw}-%{sa})/%{a0}" {:ga ga :cw cw :sa sa :a0 a0h})

        ;; Five select2 trees — one per biquad coefficient
        b0s   (faust "select2(%{tp}<4,select2(%{tp}<2,select2(%{tp}<1,%{lp},%{hp}),select2(%{tp}<3,%{bp},%{rs})),select2(%{tp}<6,select2(%{tp}<5,%{np},%{ap}),select2(%{tp}<7,%{ls},%{hs})))"
                     {:tp tp :lp b0lp :hp b0hp :bp b0bp :rs b0rs :np b0np :ap b0ap :ls b0ls :hs b0hs})
        b1s   (faust "select2(%{tp}<4,select2(%{tp}<2,select2(%{tp}<1,%{lp},%{hp}),select2(%{tp}<3,0.0,%{rs})),select2(%{tp}<6,select2(%{tp}<5,%{np},%{np}),select2(%{tp}<7,%{ls},%{hs})))"
                     {:tp tp :lp b1lp :hp b1hp :rs b1rs :np b1np :ls b1ls :hs b1hs})
        b2s   (faust "select2(%{tp}<4,select2(%{tp}<2,select2(%{tp}<1,%{lp},%{hp}),select2(%{tp}<3,%{bp},%{rs})),select2(%{tp}<6,select2(%{tp}<5,%{np},1.0),select2(%{tp}<7,%{ls},%{hs})))"
                     {:tp tp :lp b0lp :hp b0hp :bp b2bp :rs b2rs :np b0np :ls b2ls :hs b2hs})
        a1s   (faust "select2(%{tp}<6,select2(%{tp}<3,%{ab},select2(%{tp}<4,%{ar},%{ab})),select2(%{tp}<7,%{al},%{ah}))"
                     {:tp tp :ab a1b :ar a1r :al a1l :ah a1h})
        a2s   (faust "select2(%{tp}<6,select2(%{tp}<3,%{ab},select2(%{tp}<4,%{ar},%{ab})),select2(%{tp}<7,%{al},%{ah}))"
                     {:tp tp :ab a2b :ar a2r :al a2l :ah a2h})

        ;; Direct Form II biquad with selected coefficients
        w     (faust "(%{in}-%{a1}*_-%{a2}*_@1)~_"             {:in audio :a1 a1s :a2 a2s})
        out   (faust "%{b0}*%{ww}+%{b1}*%{ww}@1+%{b2}*%{ww}@2"      {:b0 b0s :b1 b1s :b2 b2s :ww w})]
    (output :out out)))
