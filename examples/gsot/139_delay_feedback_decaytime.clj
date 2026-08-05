; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.139-delay-feedback-decaytime
  "GSOT pp.201-202 — delay_feedback_decaytime.maxpat (Chapter 7).

  'Feedback Delay — Decay-Time Parameterization'
  -------------------------------------------------
  Replaces the raw feedback coefficient (ex.138) with a musically meaningful
  decay time: how many milliseconds until the echoes are −60 dB (inaudible).

  Derivation
  -----------
  With feedback coefficient fb and delay time D samples, the amplitude after
  k echoes is fb^k.  The echo at time t has k = t×SR/D echoes behind it.
  Setting amplitude = 10^(−60/20) = 0.001 at time T60 (the desired decay):

      fb^(T60_samps / D_samps) = 0.001
      fb = 0.001^(D_samps / T60_samps)
         = 0.001^(delay_ms / decay_ms)          (ms units cancel)
         = pow(0.001, delay_ms / decay_ms)

  Equivalently (using natural log):
      fb = exp(ln(0.001) × delay_ms / decay_ms)
         = exp(−6.9078 × delay_ms / decay_ms)

  Both forms are identical.  pow(0.001, ...) makes the −60 dB convention
  visible in the source; the exp form is sometimes faster at runtime.

  Behaviour of the formula
  -------------------------
  decay_ms >> delay_ms  →  exponent ≈ 0  →  fb ≈ 1     (very slow decay)
  decay_ms == delay_ms  →  exponent = 1  →  fb = 0.001  (one echo at −60 dB)
  decay_ms << delay_ms  →  exponent ≫ 1  →  fb ≈ 0     (near-instant decay)

  Musical examples at delay_ms = 250 ms (16th-note at 60 BPM)
  -------------------------------------------------------------
  decay_ms = 250    → fb = 0.001  (single audible echo, −60 dB immediately)
  decay_ms = 1000   → fb ≈ 0.125  (approx. 3 audible echoes, 1-second tail)
  decay_ms = 2000   → fb ≈ 0.354  (5–6 echoes, 2-second tail)
  decay_ms = 8000   → fb ≈ 0.750  (long reverberant tail, 8-second)
  decay_ms = 30000  → fb ≈ 0.946  (very slow decay; near-infinite sustain)

  Why not just use raw fb?
  -------------------------
  fb = 0.9 sounds different at delay=10 ms vs delay=500 ms — the decay tail
  length depends on BOTH fb and delay.  Decay-time parameterization makes
  the perceptual result independent of delay: 2000 ms decay means 2000 ms
  regardless of whether delay is 50 ms or 400 ms.

  Relationship to Schroeder reverb (later in Chapter 7)
  -------------------------------------------------------
  The same fb = pow(0.001, D/T60) formula drives all comb filter branches in
  the Schroeder reverb structure.  This example is the single-comb precursor.

  Faust computation
  ------------------
  Step 1 — feedback coefficient from decay time:
      fb = pow(0.001, %ms / max(1.0, %dc))

  Step 2 — feedback delay loop (same as ex.138):
      fdl_loop(fbs) = de.delay(int(ma.SR×5), int(max(0, ms×SR/1000)), in + fb×fbs)
      fdl = fdl_loop ~ _

  Step 3 — dry/wet mix:
      out = (1 − mx)×in + mx×fdl

  Parameters
  ----------
  :ms — delay time in milliseconds (1–5000; default 250)
  :dc — decay time to −60 dB in milliseconds (1–30000; default 2000)
  :mx — wet/dry mix; 0.0 = dry only, 1.0 = wet only (default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: dry/wet mixed feedback delay output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! delay-feedback-decaytime
  {:params {:ms {:range [1.0 5000.0]  :default 250.0}
            :dc {:range [1.0 30000.0] :default 2000.0}
            :mx {:range [0.0 1.0]     :default 0.5}}}
  (let [in  (audio-in)
        ms  (param :ms)
        dc  (param :dc)
        mx  (param :mx)
        fb  (faust "pow(0.001,%{ms}/max(1.0,%{dc}))" {:ms ms :dc dc})
        fdl (faust "fdl_loop ~ _\n  with { fdl_loop(fbs) = de.delay(int(ma.SR*5.0),int(max(0.0,%{ms}*ma.SR/1000.0)),%{in}+%{fb}*fbs); }"
                   {:ms ms :in in :fb fb})
        out (faust "(1.0-%{mx})*%{in}+%{mx}*%{fd}" {:mx mx :in in :fd fdl})]
    (output :out out)))
