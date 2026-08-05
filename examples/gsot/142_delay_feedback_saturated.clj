; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.142-delay-feedback-saturated
  "GSOT p.204 — delay_feedback_saturated.maxpat (Chapter 7).

  'Feedback Delay — Saturating Feedback Limiter'
  ------------------------------------------------
  Adds a soft-clipper (tanh) inside the feedback path, after the lowpass and
  DC block (ex.141), to limit the amplitude of the recirculating signal.

  Why limit feedback amplitude?
  ------------------------------
  Even with fb < 1 (stable), long decay times push fb close to 1.  Any noise
  floor, DC residual, or input transient can cause temporary amplitude buildup
  that takes many seconds to decay.  Hard clipping (max/min) introduces harsh
  aliased distortion.  A smooth limiter (tanh) provides:

    1. Bounded output  — signal can never exceed ±1/drive regardless of fb
    2. Harmonic saturation — odd-order harmonics, analogue-warmth character
    3. Self-limiting stability — even fb ≥ 1 settles to a limit cycle rather
       than diverging; useful for intentional harmonic saturation effects

  The tanh function (implemented via exp)
  -----------------------------------------
  tanh is not a Faust primitive; implemented as the mathematically exact identity:

      tanh(x) = 1 − 2 / (exp(2x) + 1)

  This uses a single exp() call and is numerically stable for |x| < ~20.
  tanh(x) maps ℝ → (−1, 1):
    tanh(0)   = 0               (no distortion at silence)
    tanh(1)   ≈ 0.762           (soft knee begins, ~−2.4 dB)
    tanh(3)   ≈ 0.995           (near-clipping, ~−0.04 dB)
    tanh(∞)   → 1               (hard limit asymptote)

  Transfer function (Taylor): tanh(x) = x − x³/3 + x⁵/15 − …
  Predominantly adds 3rd harmonic (−THD₃ relative to fundamental).

  Drive parameter
  ----------------
  :dr scales the input to tanh before it enters saturation:

      saturated = tanh(dr × lpdc)

  At dr = 1 (default): saturation begins around |lpdc| ≈ 0.5
  At dr = 4:           saturation begins around |lpdc| ≈ 0.125 — earlier onset,
                        more aggressive harmonic content in the tail
  At dr → 0:           tanh(dr·x) ≈ dr·x (linear — no saturation, behaves as ex.141)

  Note: drive does NOT compensate output amplitude.  Increasing drive darkens the
  tail and raises harmonic content; it does NOT increase output level (tanh ≤ 1).

  Feedback signal chain
  ----------------------
  fbs  →  fi.pole(fc)  →  DC block  →  tanh(dr··)  →  × fb  →  + in  →  delay D

  Four ~ state registers (all independent):
    1.  fdl_loop ~ _         outer delay feedback (via de.delay, D samples)
    2.  fi.pole ~ *(fc)      one-pole LP state
    3.  dc_block + ~ *(R)    DC blocker feedback state (R = 0.9999)
    tanh itself is stateless (memoryless nonlinearity — no ~ needed)

  Compare to hard limiting
  -------------------------
  Hard clip: max(−1, min(1, x)) — same bound but introduces aliased odd harmonics
             and a discontinuous derivative at ±1.
  tanh:      continuous derivative everywhere; zero harmonic distortion at low
             amplitudes; graceful onset; preferred for musical feedback saturation.

  Parameters
  ----------
  :ms — delay time in milliseconds (1–5000; default 250)
  :dc — decay time to −60 dB in milliseconds (1–30000; default 2000)
  :hz — feedback filter cutoff in Hz (20–20000; default 4000)
  :dr — saturation drive; higher = earlier onset (0.1–10; default 1.0)
  :mx — wet/dry mix; 0.0 = dry only, 1.0 = wet only (default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: dry/wet mixed saturated-feedback delay output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! delay-feedback-saturated
  {:params {:ms {:range [1.0 5000.0]   :default 250.0}
            :dc {:range [1.0 30000.0]  :default 2000.0}
            :hz {:range [20.0 20000.0] :default 4000.0}
            :dr {:range [0.1 10.0]     :default 1.0}
            :mx {:range [0.0 1.0]      :default 0.5}}}
  (let [in  (audio-in)
        ms  (param :ms)
        dc  (param :dc)
        hz  (param :hz)
        dr  (param :dr)
        mx  (param :mx)
        fb  (faust "pow(0.001,%{ms}/max(1.0,%{dc}))" {:ms ms :dc dc})
        fc  (faust "exp(-2.0*ma.PI*%{hz}/ma.SR)" {:hz hz})
        fdl (faust "fdl_loop ~ _\n  with {\n    fdl_loop(fbs) = de.delay(int(ma.SR*5.0),int(max(0.0,%{ms}*ma.SR/1000.0)),%{in}+%{fb}*sat)\n      with {\n        lp   = fi.pole(%{fc},fbs);\n        lpdc = (lp-lp@1) : +~*(0.9999);\n        sat  = 1.0-2.0/(exp(2.0*%{dr}*lpdc)+1.0);\n      };\n  }"
                   {:ms ms :in in :fb fb :fc fc :dr dr})
        out (faust "(1.0-%{mx})*%{in}+%{mx}*%{fd}" {:mx mx :in in :fd fdl})]
    (output :out out)))
