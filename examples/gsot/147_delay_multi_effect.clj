; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.147-delay-multi-effect
  "GSOT pp.211-213 — delay_multi_effect.maxpat (Chapter 7).

  'A Garden of Earthly Delays — Modulated Feedback Delay'
  --------------------------------------------------------
  A single feedback delay line combining all Chapter 7 techniques:
  LFO modulation of delay time, decay-time parameterized feedback,
  unity-DC-gain lowpass damping in the feedback path, DC blocking,
  and wet/dry mix.

  The 'garden' is the space of effects reachable from one patch by varying
  parameters — presets at the bottom demonstrate the range.

  Signal chain
  -------------
  1. LFO modulates delay time:
         D[n] = :ms × SR/1000  +  (1 − 2×:iv) × os.osc(:lf) × :ld × SR/1000

     (1−2×:iv) = +1 when :iv=0 (normal), −1 when :iv=1 (phase-flipped LFO).

  2. Feedback coefficient from base delay and decay time:
         fb = pow(0.001, :ms / max(1, :dc))

  3. Unity-DC-gain one-pole lowpass (dampen) in feedback path:
         lp[n] = (1 − :dp) × s[n]  +  :dp × lp[n−1]

     :dp = 0 → lp = s (no damping; LP fully open; clear echoes)
     :dp → 1 → lp → DC only (maximum damping; echoes become bass-only)
     DC gain = 1 at all :dp values — does not amplify the feedback.

  4. DC blocker after LP (R = 0.9999):
         dc[n] = (lp[n] − lp[n−1])  +  0.9999 × dc[n−1]

  5. Feedback loop:
         fdl_loop(s) = de.delay(maxD, D[n], in + fb × dc_blocked(lp(s)))
         fdl = fdl_loop ~ _

  6. Wet/dry mix:
         out = (1 − :mx) × in  +  :mx × fdl

  Contrast with ex.140 / ex.141 LP
  ----------------------------------
  ex.140 uses fi.pole (unscaled, DC gain = 1/(1−fc)) inside the loop.
  Here the LP is a scaled one-pole `(1−dp)·s : + ~ ·(dp)` with DC gain = 1,
  so the dampen coefficient dp ∈ [0, 1) is stable for any fb < 1.

  Parameters
  ----------
  :ms — base delay time in milliseconds (1–5000; default 250)
  :ld — LFO depth in milliseconds (0–50; default 0)
  :lf — LFO frequency in Hz (0.01–20; default 0.5)
  :iv — LFO invert; 0.0 = normal phase, 1.0 = inverted (default 0)
  :dc — decay time to −60 dB in milliseconds (1–30000; default 2000)
  :dp — dampen pole coefficient (0.0 = no damping, <1 = low-pass; default 0.0)
  :mx — wet/dry mix; 0.0 = dry only, 1.0 = wet only (default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: LFO-modulated dampened-feedback delay output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! delay-multi-effect
  {:params {:ms {:range [1.0 5000.0]  :default 250.0}
            :ld {:range [0.0 50.0]    :default 0.0}
            :lf {:range [0.01 20.0]   :default 0.5}
            :iv {:range [0.0 1.0]     :default 0.0}
            :dc {:range [1.0 30000.0] :default 2000.0}
            :dp {:range [0.0 0.9999]  :default 0.0}
            :mx {:range [0.0 1.0]     :default 0.5}}}
  (let [in  (audio-in)
        ms  (param :ms)
        ld  (param :ld)
        lf  (param :lf)
        iv  (param :iv)
        dc  (param :dc)
        dp  (param :dp)
        mx  (param :mx)
        fb  (faust "pow(0.001,%{ms}/max(1.0,%{dc}))" {:ms ms :dc dc})
        dly (faust "int(max(0.0,%{ms}*ma.SR/1000.0+(1.0-2.0*%{iv})*os.osc(%{lf})*%{ld}*ma.SR/1000.0))"
                   {:ms ms :iv iv :lf lf :ld ld})
        fdl (faust "fdl_loop ~ _\n  with {\n    fdl_loop(s) = de.delay(int(ma.SR*5.0),%{dl},%{in}+%{fb}*lpdc)\n      with {\n        lp   = (1.0-%{dp})*s : +~*(%{dp});\n        lpdc = (lp-lp@1) : +~*(0.9999);\n      };\n  }"
                   {:dl dly :in in :fb fb :dp dp})
        out (faust "(1.0-%{mx})*%{in}+%{mx}*%{fd}" {:mx mx :in in :fd fdl})]
    (output :out out)))

; ---------------------------------------------------------------------------
; Example presets — GSOT p.213
; Column order: name         | :ms  | :ld    | :lf  | :iv | :dc  | :dp    | :mx
; ---------------------------------------------------------------------------

; Delays                     | 700  | 0      |      |     | 6000 | 0.8    | 0.4
(comment
  (delay-multi-effect {:ms 700 :ld 0 :lf 0.5 :iv 0 :dc 6000 :dp 0.8 :mx 0.4}))

; Mad dub                    | 300  | 0.03   | 1    | 1   | 4000 | 0.9999 | 0.8
; (Dampen=1 in source; clamped — at exactly 1.0 the LP becomes a pure integrator
;  whose DC-blocked output is zero, silencing feedback.)
(comment
  (delay-multi-effect {:ms 300 :ld 0.03 :lf 1 :iv 1 :dc 4000 :dp 0.9999 :mx 0.8}))

; Garage echo                | 150  | 0      |      |     | 1000 | 0.5    | 0.3
(comment
  (delay-multi-effect {:ms 150 :ld 0 :lf 0.5 :iv 0 :dc 1000 :dp 0.5 :mx 0.3}))

; Slap echo                  | 60   | 0      |      | 1   | 1000 | 0.5    | 0.3
(comment
  (delay-multi-effect {:ms 60 :ld 0 :lf 0.5 :iv 1 :dc 1000 :dp 0.5 :mx 0.3}))

; Tape flutter               | 30   | 0.1    | 3    |     | 10   | 0.9999 | 1.0
; (Dampen=1 in source; clamped. :dc=10ms → fb≈0, single-pass flutter.)
(comment
  (delay-multi-effect {:ms 30 :ld 0.1 :lf 3 :iv 0 :dc 10 :dp 0.9999 :mx 1.0}))

; Chorus                     | 20   | 0.03   | 4    |     | 0    | 0.9999 | 0.6
; (Dampen=1 in source; clamped. :dc=0 → fb≈0 via max(1,dc) clamp; no feedback trail.)
(comment
  (delay-multi-effect {:ms 20 :ld 0.03 :lf 4 :iv 0 :dc 0 :dp 0.9999 :mx 0.6}))

; Didgerimetal               | 10   | 0.01   | 99   | 1   | 2000 | 0.15   | 1.0
; (99Hz LFO ≈ audio-rate FM; 0.01ms depth ≈ 0.44 samples. fb≈0.979, strong ~100Hz resonance.)
(comment
  (delay-multi-effect {:ms 10 :ld 0.01 :lf 99 :iv 1 :dc 2000 :dp 0.15 :mx 1.0}))

; Flanger                    | 5    | 0.8    | 0.1  | 1   | 50   | 0.9999 | 0.5
; (Dampen=1 in source; clamped.)
(comment
  (delay-multi-effect {:ms 5 :ld 0.8 :lf 0.1 :iv 1 :dc 50 :dp 0.9999 :mx 0.5}))

; String                     | 3    | 0.0001 | 7    |     | 1000 | 0.75   | 1.0
; (:ld≈0 — LFO inaudible; Karplus-Strong-adjacent resonator at ~333Hz. fb≈0.979.)
(comment
  (delay-multi-effect {:ms 3 :ld 0.0001 :lf 7 :iv 0 :dc 1000 :dp 0.75 :mx 1.0}))

; Phaser                     | 1.5  | 1.0    | 0.1  |     | 1    | 0.9999 | 0.5
; (Dampen=1 in source; clamped. :dc=1ms → fb≈0, feedforward comb; sweeps 0.5–2.5ms.)
(comment
  (delay-multi-effect {:ms 1.5 :ld 1.0 :lf 0.1 :iv 0 :dc 1 :dp 0.9999 :mx 0.5}))

; Toothpaste                 | 0.3  | 0.6    | 1.0  | 1   | 12   | 0.9999 | 1.0
; (Dampen=1 in source; clamped. :ld > :ms → clamps to 0 on negative LFO half;
;  sweeps 0–0.9ms. fb≈0.841, strong resonant comb sweep.)
(comment
  (delay-multi-effect {:ms 0.3 :ld 0.6 :lf 1.0 :iv 1 :dc 12 :dp 0.9999 :mx 1.0}))

; Filter Wobble              | 0.1  | 1.0    | 5.0  | 1   | 2    | 0.5    | 1.0
; (:ld 10× :ms → sweeps 0–1.1ms; comb freq races 10kHz→900Hz at 5Hz. fb≈0.708.)
(comment
  (delay-multi-effect {:ms 0.1 :ld 1.0 :lf 5.0 :iv 1 :dc 2 :dp 0.5 :mx 1.0}))
