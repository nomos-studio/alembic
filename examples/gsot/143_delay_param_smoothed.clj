; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.143-delay-param-smoothed
  "GSOT pp.206-207 — delay_param_smoothed.maxpat (Chapter 7).

  'Avoiding Clicks — Parameter Smoothing for Delay Time'
  -------------------------------------------------------
  When delay time changes abruptly the circular-buffer read pointer jumps to
  a new position.  The discontinuity is audible as a click or pitch-glitch.
  The fix: pass the delay time target through a one-pole lag before it reaches
  de.delay, so the read pointer moves continuously rather than teleporting.

  Why abrupt delay changes click
  --------------------------------
  de.delay(maxD, D, x) reads from a circular buffer at position D samples
  behind the write pointer.  If D changes instantaneously by ΔD samples:
    - The next read is at a completely different position in the buffer
    - Sample values before and after are unrelated → discontinuity → click

  Even small changes matter at audio rates: a 1-sample jump at 44.1 kHz
  corresponds to a 22 kHz discontinuity — always audible.

  The one-pole lag smoother
  --------------------------
  A one-pole IIR applied to the control signal (delay time in ms):

      sms[n] = k · sms[n−1]  +  (1−k) · ms[n]      k = exp(−1000 / (st·SR))

  The smoothed value sms tracks the target ms exponentially.  At 1 time
  constant (st ms), sms has covered 63% of the distance.  The read pointer
  glides continuously, creating a smooth Doppler-like pitch shift during
  the transition rather than a click.

  Smoothing time tradeoffs
  -------------------------
  :st too small (< 5 ms):   transition fast but audible as a short pitch blip
  :st ≈ 10–30 ms:           practical range; click-free, transition inaudible
  :st too large (> 100 ms): slow response; delay time feels 'laggy' in live use

  The Doppler artifact during transition
  ----------------------------------------
  While the smoothed delay time is changing, the read pointer moves at a
  non-unity rate relative to real time.  This causes brief pitch shift:

      pitch_ratio = 1 − Δsms/sample          (Δsms = per-sample change in delay)

  For small `:st`, the pitch shift is a fast, musically innocuous transient.
  For very small `:st` (or large `:ms` changes), it becomes a glide — the
  basis of chorus and flanger effects where deliberate, slow delay-time
  modulation creates pitch modulation.

  Coefficient formula
  --------------------
      k = exp(−1000 / (:st × ma.SR))

  At st = 20 ms, SR = 44100: k = exp(−1000/882000) ≈ 0.9989
  The read pointer reaches 99% of a target change in ~4.6 × 20 ms ≈ 92 ms.

  The same formula appears in:
    ex.103  go.onepole.basic_hz  (smoothing an audio signal)
    ex.136  go.line.ms           (lag on an audio control signal)
    ex.143  (this patch)         (lag on a DSP structural parameter)

  Relationship to chorus/flanger
  --------------------------------
  If `:ms` is modulated by an LFO rather than set by a parameter, the
  same smoothed-delay structure produces chorus (long range, slow LFO) or
  flanger (short range, fast LFO).  ex.143 is the static-parameter version.

  Parameters
  ----------
  :ms — target delay time in milliseconds (1–5000; default 250)
  :st — parameter smoothing time constant in milliseconds (1–500; default 20)
  :dc — decay time to −60 dB in milliseconds (1–30000; default 2000)
  :mx — wet/dry mix; 0.0 = dry only, 1.0 = wet only (default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: dry/wet mixed click-free delay output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! delay-param-smoothed
  {:params {:ms {:range [1.0 5000.0]  :default 250.0}
            :st {:range [1.0 500.0]   :default 20.0}
            :dc {:range [1.0 30000.0] :default 2000.0}
            :mx {:range [0.0 1.0]     :default 0.5}}}
  (let [in  (audio-in)
        ms  (param :ms)
        st  (param :st)
        dc  (param :dc)
        mx  (param :mx)
        sk  (faust "exp(-1000.0/(%st*ma.SR))" {:st st})
        sms (faust "(%sk*_+(1.0-%sk)*%ms)~_" {:sk sk :ms ms})
        fb  (faust "pow(0.001,%ms/max(1.0,%dc))" {:ms ms :dc dc})
        fdl (faust "fdl_loop ~ _\n  with { fdl_loop(fbs) = de.delay(int(ma.SR*5.0),int(max(0.0,%sm*ma.SR/1000.0)),%in+%fb*fbs); }"
                   {:sm sms :in in :fb fb})
        out (faust "(1.0-%mx)*%in+%mx*%fd" {:mx mx :in in :fd fdl})]
    (output :out out)))
