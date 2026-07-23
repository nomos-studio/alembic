; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.141-delay-feedback-filtered-dcblock
  "GSOT p.203 — delay_feedback_filtered_dcblock.maxpat (Chapter 7).

  'Feedback Delay — Filtered Feedback + DC Blocking'
  ----------------------------------------------------
  Extends ex.140 (filtered feedback) by adding a DC blocking filter in series
  with the lowpass inside the feedback loop.  Without it, DC components can
  accumulate pass-by-pass — each recirculation adds any DC in the signal to the
  already-accumulated DC, eventually saturating the delay buffer.

  Why DC accumulates in feedback loops
  --------------------------------------
  Even a signal with no intentional DC offset can accumulate bias through:
    - Floating-point rounding errors at near-unity feedback
    - Single-sample initial conditions that never fully decay
    - Input signals with subtle low-frequency content below audio range

  With fb close to 1 (long decay times), the DC component decays as slowly as
  the audio: at fb = 0.99, DC halves only every 69 passes.  A 250 ms delay at
  44.1 kHz with fb = 0.99 would take ~17 seconds to halve accumulated DC.

  The DC blocker
  ---------------
  First-order IIR highpass with a zero at DC (z = 1) and a pole just below DC:

      y[n] = (x[n] − x[n−1])  +  R · y[n−1]       R = 0.9999

  Transfer function: H(z) = (1 − z^{−1}) / (1 − R · z^{−1})
    - Zero at z = 1 → complete DC cancellation
    - Pole at z = R → −3 dB cutoff ≈ (1−R)·SR/(2π) ≈ 0.7 Hz at 44.1 kHz
    - Above ~5 Hz: effectively transparent (|H| → 1)

  The zero-at-DC means the blocker removes DC exactly on every feedback pass,
  regardless of how much has accumulated.  The pole at R provides a smooth
  high-pass rolloff rather than a hard cutch.

  Faust signal chain inside fdl_loop
  ------------------------------------
  fbs  →  fi.pole(fc, ·)  →  dc_block  →  × fb  →  + in  →  delay D  →  y

  where dc_block(x) = (x − x@1) : + ~ *(0.9999)

  Three independent ~ state registers:
    1.  fdl_loop ~ _        outer delay feedback (D samples via de.delay)
    2.  fi.pole ~ *(fc)     one-pole LP state (1 sample)
    3.  dc_block + ~ *(R)   DC blocker feedback state (1 sample)

  Faust resolves each ~ independently; no mutual dependencies between them.

  Relationship to ex.140
  -----------------------
  ex.140  filtered feedback; high freqs decay faster; DC can build up at long decay
  ex.141  (this patch) same + DC blocker in series; safe at any decay/feedback level

  For musical use ex.141 is always preferable to ex.140 — the overhead is
  negligible (2 multiply-adds per sample) and prevents the build-up artefact.

  Parameters
  ----------
  :ms — delay time in milliseconds (1–5000; default 250)
  :dc — decay time to −60 dB in milliseconds (1–30000; default 2000)
  :hz — feedback filter cutoff in Hz (20–20000; default 4000)
  :mx — wet/dry mix; 0.0 = dry only, 1.0 = wet only (default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: dry/wet mixed filtered+DC-blocked feedback delay"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! delay-feedback-filtered-dcblock
  {:params {:ms {:range [1.0 5000.0]   :default 250.0}
            :dc {:range [1.0 30000.0]  :default 2000.0}
            :hz {:range [20.0 20000.0] :default 4000.0}
            :mx {:range [0.0 1.0]      :default 0.5}}}
  (let [in  (audio-in)
        ms  (param :ms)
        dc  (param :dc)
        hz  (param :hz)
        mx  (param :mx)
        fb  (faust "pow(0.001,%ms/max(1.0,%dc))" {:ms ms :dc dc})
        fc  (faust "exp(-2.0*ma.PI*%hz/ma.SR)" {:hz hz})
        fdl (faust "fdl_loop ~ _\n  with {\n    fdl_loop(fbs) = de.delay(int(ma.SR*5.0),int(max(0.0,%ms*ma.SR/1000.0)),%in+%fb*lpdc)\n      with {\n        lp   = fi.pole(%fc,fbs);\n        lpdc = (lp-lp@1) : +~*(0.9999);\n      };\n  }"
                   {:ms ms :in in :fb fb :fc fc})
        out (faust "(1.0-%mx)*%in+%mx*%fd" {:mx mx :in in :fd fdl})]
    (output :out out)))
