; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.140-delay-feedback-filtered
  "GSOT pp.202-203 — delay_feedback_filtered.maxpat (Chapter 7).

  'Feedback Delay — Filtered Feedback Path'
  -------------------------------------------
  Inserts a one-pole lowpass filter inside the feedback loop.  The delayed
  signal is filtered before being recirculated, so high frequencies lose
  energy on every echo pass while low frequencies sustain longer.

  Signal flow
  -----------
      w[n] = x[n] + fb · lp(y[n−1])        lp applied to feedback tap
      y[n] = delay(w[n], D)

  where lp is a one-pole lowpass with pole coefficient a = exp(−2π·fc/SR).

  Effect on the echo tail
  ------------------------
  Each echo pass multiplies the spectrum by  fb · |H_lp(ω)|:

      feedback gain at DC:       fb · 1           = fb    (full level)
      feedback gain at fc:       fb · (1/√2)     ≈ 0.707·fb  (−3 dB)
      feedback gain at Nyquist:  fb · 0           → 0    (fully attenuated)

  High frequencies saturate first — the echo tail becomes progressively warmer
  with each reflection.  Physical analogy: tape delay with head-to-head loss,
  or a room with frequency-dependent absorption.

  Effective decay time per frequency band
  -----------------------------------------
  The effective feedback gain at frequency f is:

      g(f) = fb · |H_lp(f)| = pow(0.001, delay_ms/decay_ms) · |H_lp(f)|

  High frequencies (f >> fc): g(f) → 0 → very short effective decay
  Low frequencies  (f << fc): g(f) → fb → full decay_ms duration

  The filter cutoff (:hz) controls the crossover between 'sustained' and
  'rapidly-dying' frequency regions.  Lower cutoff = warmer, darker tail.

  Relationship to ex.138 and ex.139
  -----------------------------------
  ex.138  raw feedback coefficient — no frequency shaping
  ex.139  decay-time parameterization — still flat frequency response
  ex.140  (this patch) — per-frequency decay via filtered feedback

  Setting :hz → SR/2 (Nyquist) degenerates to ex.139 (flat filter, no effect).
  Setting :hz → 0 silences feedback entirely (infinite attenuation).

  Faust computation
  ------------------
  Step 1 — feedback coefficient from decay time (same as ex.139):
      fb = pow(0.001, %ms / max(1.0, %dc))

  Step 2 — one-pole coefficient from cutoff:
      fc = exp(−2.0 × ma.PI × %hz / ma.SR)

  Step 3 — filtered feedback delay loop:
      fdl_loop(fbs) = de.delay(int(ma.SR×5), int(max(0, ms×SR/1000)),
                                in + fb × fi.pole(fc, fbs))
      fdl = fdl_loop ~ _

  fi.pole(p, x) from Faust filters.lib: x : + ~ *(p)
  The fi.pole ~ and the outer fdl_loop ~ are independent state registers.

  Step 4 — dry/wet mix:
      out = (1 − mx)×in + mx×fdl

  Parameters
  ----------
  :ms — delay time in milliseconds (1–5000; default 250)
  :dc — decay time to −60 dB in milliseconds (1–30000; default 2000)
  :hz — feedback filter cutoff in Hz (20–20000; default 4000)
  :mx — wet/dry mix; 0.0 = dry only, 1.0 = wet only (default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: dry/wet mixed filtered-feedback delay output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! delay-feedback-filtered
  {:params {:ms {:range [1.0 5000.0]   :default 250.0}
            :dc {:range [1.0 30000.0]  :default 2000.0}
            :hz {:range [20.0 20000.0] :default 4000.0}
            :mx {:range [0.0 1.0]      :default 0.5}}}
  (let [in  (audio-in)
        ms  (param :ms)
        dc  (param :dc)
        hz  (param :hz)
        mx  (param :mx)
        fb  (faust "pow(0.001,%{ms}/max(1.0,%{dc}))" {:ms ms :dc dc})
        fc  (faust "exp(-2.0*ma.PI*%{hz}/ma.SR)" {:hz hz})
        fdl (faust "fdl_loop ~ _\n  with { fdl_loop(fbs) = de.delay(int(ma.SR*5.0),int(max(0.0,%{ms}*ma.SR/1000.0)),%{in}+%{fb}*fi.pole(%{fc},fbs)); }"
                   {:ms ms :in in :fb fb :fc fc})
        out (faust "(1.0-%{mx})*%{in}+%{mx}*%{fd}" {:mx mx :in in :fd fdl})]
    (output :out out)))
