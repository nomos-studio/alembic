; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.147-delay-multi-effect
  "GSOT pp.211-212 — delay_multi_effect.maxpat (Chapter 7).

  'A Garden of Earthly Delays — Multi-Tap Delay Effect'
  -------------------------------------------------------
  Combines three delay techniques from Chapter 7 in parallel, each contributing
  a distinct textural layer to the mix.  The 'garden' metaphor: rather than
  choosing one delay type, cultivate several simultaneously.

  Three-tap parallel architecture
  ---------------------------------

  Tap 1 — Slapback (feedforward, no feedback)  [ex.137 lineage]
      A single fixed echo at a short delay time.  No feedback, so only one
      reflection.  Classic 1950s rockabilly / vocal doubling character.
      Controlled by :t1.  Decay time irrelevant — one-shot.

  Tap 2 — Standard echo (feedback, decay-time parameterized)  [ex.139 lineage]
      Repeating echo trail with amplitude fb₂ = pow(0.001, :t2/:dc).
      Feedback makes the tail, decay time controls how long it lasts.
      Mid-range delay time creates rhythmic phrasing in the echo.
      Controlled by :t2 and :dc.

  Tap 3 — Dark filtered echo (filtered + DC-blocked feedback)  [ex.141 lineage]
      Same IIR echo structure as tap 2 but with a lowpass (fi.pole at :hz)
      and DC blocker (R = 0.9999) inside the feedback loop.  High frequencies
      decay faster than lows, producing a warm, tape-like tail.  Longer delay
      time creates a wide, spacious shadow behind the mix.
      Controlled by :t3, :dc, :hz.

  Combined texture
  -----------------
  The three taps are summed and divided by 3 to maintain unity gain.  Each
  layer occupies a distinct temporal and spectral position:

      Tap 1  short,  bright, single echo  → presence and air
      Tap 2  medium, flat,   repeating    → rhythmic definition
      Tap 3  long,   dark,   decaying     → depth and warmth

  At default delay times (125, 250, 500 ms at 120 BPM ≈ 1/16, 1/8, 1/4 notes):
  the three taps are rhythmically related, producing a complex but coherent
  echo texture.  Detuning any tap (non-integer BPM ratios) creates polyrhythm.

  Shared decay time (:dc) for taps 2 and 3
  ------------------------------------------
  Both feedback taps use the RT60 formula fb = pow(0.001, t/dc) with the same
  :dc parameter.  Since :t3 > :t2, tap 3's feedback coefficient is closer to 1
  (longer tail relative to the same decay target).  To set independent tails,
  see ex.145/146 where taps have independent decay parameters.

  Faust signal chain (three taps in parallel)
  --------------------------------------------
  Tap 1 (feedforward):
      tp1 = de.delay(maxD, D₁, in)

  Tap 2 (feedback):
      f2      = pow(0.001, :t2 / max(1, :dc))
      fdl2(s) = de.delay(maxD, D₂, in + f2×s)
      tp2     = fdl2 ~ _

  Tap 3 (filtered + DC-blocked feedback):
      f3      = pow(0.001, :t3 / max(1, :dc))
      fc      = exp(−2π × :hz / SR)
      fdl3(s) = de.delay(maxD, D₃, in + f3×lpdc)
        where lp   = fi.pole(fc, s)
              lpdc = (lp − lp@1) : + ~ *(0.9999)
      tp3     = fdl3 ~ _

  Mix:
      wet = (tp1 + tp2 + tp3) / 3.0
      out = (1 − mx)×in + mx×wet

  Parameters
  ----------
  :t1 — tap 1 delay time in ms, slapback (1–500; default 125)
  :t2 — tap 2 delay time in ms, standard echo (1–2000; default 250)
  :t3 — tap 3 delay time in ms, dark filtered echo (1–5000; default 500)
  :dc — shared decay time to −60 dB in ms for taps 2 and 3 (1–30000; default 2000)
  :hz — tap 3 feedback filter cutoff in Hz (200–8000; default 3000)
  :mx — wet/dry mix; 0.0 = dry only, 1.0 = wet only (default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: three-tap delay mix (slapback + echo + dark tail)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! delay-multi-effect
  {:params {:t1 {:range [1.0 500.0]    :default 125.0}
            :t2 {:range [1.0 2000.0]   :default 250.0}
            :t3 {:range [1.0 5000.0]   :default 500.0}
            :dc {:range [1.0 30000.0]  :default 2000.0}
            :hz {:range [200.0 8000.0] :default 3000.0}
            :mx {:range [0.0 1.0]      :default 0.5}}}
  (let [in  (audio-in)
        t1  (param :t1)
        t2  (param :t2)
        t3  (param :t3)
        dc  (param :dc)
        hz  (param :hz)
        mx  (param :mx)
        tp1 (faust "de.delay(int(ma.SR*5.0),int(max(0.0,%t1*ma.SR/1000.0)),%in)"
                   {:t1 t1 :in in})
        f2  (faust "pow(0.001,%t2/max(1.0,%dc))" {:t2 t2 :dc dc})
        tp2 (faust "fdl2 ~ _\n  with { fdl2(s) = de.delay(int(ma.SR*5.0),int(max(0.0,%t2*ma.SR/1000.0)),%in+%f2*s); }"
                   {:t2 t2 :in in :f2 f2})
        f3  (faust "pow(0.001,%t3/max(1.0,%dc))" {:t3 t3 :dc dc})
        fc  (faust "exp(-2.0*ma.PI*%hz/ma.SR)" {:hz hz})
        tp3 (faust "fdl3 ~ _\n  with {\n    fdl3(s) = de.delay(int(ma.SR*5.0),int(max(0.0,%t3*ma.SR/1000.0)),%in+%f3*lpdc)\n      with {\n        lp   = fi.pole(%fc,s);\n        lpdc = (lp-lp@1) : +~*(0.9999);\n      };\n  }"
                   {:t3 t3 :in in :f3 f3 :fc fc})
        wet (faust "(%a+%b+%c)/3.0" {:a tp1 :b tp2 :c tp3})
        out (faust "(1.0-%mx)*%in+%mx*%wt" {:mx mx :in in :wt wet})]
    (output :out out)))
