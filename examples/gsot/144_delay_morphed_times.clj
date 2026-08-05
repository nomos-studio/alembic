; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.144-delay-morphed-times
  "GSOT p.208 — delay_morphed_times.maxpat (Chapter 7).

  'Smooth Changes to Delay Time Cause Doppler Shifts'
  -----------------------------------------------------
  Deliberately exploits the Doppler pitch shift that ex.143 tried to minimise.
  Two delay time targets (:ta, :tb) are blended by a morph parameter (:mo);
  the blended target is smoothed before reaching the delay line.  The smooth
  transition shifts the read pointer at a non-unity rate, producing pitch shift
  proportional to the rate of change of the delay time.

  Doppler mechanics
  ------------------
  A delay line with write pointer W and read pointer R = W − D:
    - Each sample, W advances by 1 sample
    - If D increases by Δ samples this cycle, R effectively advances by (1 − Δ)
    - Pitch ratio = 1 − (per-sample delay rate of change in samples)

  Delay increasing (reading slower, R lags behind):  pitch goes DOWN
  Delay decreasing (reading faster, R gains on W):   pitch goes UP

  Rate of change and smoothing time
  -----------------------------------
  With the one-pole smoother at time constant :st samples:
      Initial rate ≈ (Δdelay_samples) / τ   where τ = :st × SR / 1000

  For the read pointer not to reverse (pitch ratio > 0):
      rate < 1 sample/sample  →  :st > Δdelay_ms

  When :st < Δdelay_ms, the read pointer briefly reverses during transition —
  audible as a sharp pitch-down then pitch-up.  At :st >> Δdelay_ms, the
  transition is a slow, smooth pitch glide.  This section uses :st as a
  'Doppler rate' parameter: small = dramatic effect, large = subtle.

  Morph parameter
  ----------------
  :mo linearly blends between the two delay targets:
      ms[n] = (1 − mo) × ta  +  mo × tb

  At :mo = 0: delay = :ta (pitch-stable at target A)
  At :mo = 1: delay = :tb (pitch-stable at target B)
  During transition: pitch shifts in the direction of travel

  This is the controlled version of Doppler:
    - ex.143 uses large :st to hide the shift (click avoidance)
    - ex.144 exposes the shift as a musical gesture by letting :mo change
      at a rate that makes the Doppler audible and controllable

  Musical applications
  ---------------------
  Pitch glide between two echo times:
      :ta = 250 ms (dotted-eighth delay), :tb = 333 ms (triplet delay)
      Slowly sweep :mo to glide pitch while changing rhythmic character.

  Tape wow / flutter simulation:
      :ta and :tb close together (~1–10 ms apart); :mo modulated by a slow
      LFO with :st small → subtle, continuous pitch wobble.

  Chorus / vibrato:
      :ta = 20 ms, :tb = 30 ms; :mo modulated by LFO at 0.5–5 Hz
      → classic chorus pitch modulation.  See also Chapter 8.

  Dramatic pitch-down on delay extension:
      :ta = 100 ms → :tb = 1000 ms with :st = 50 ms → octave-ish pitch drop
      while the echo tail lengthens.

  Faust signal chain
  -------------------
  Step 1 — blend delay targets:
      ms = (1 − %mo)×%ta + %mo×%tb

  Step 2 — smooth the blended target (Doppler rate controlled by :st):
      sk  = exp(−1000 / (%st × ma.SR))
      sms = (%sk×_ + (1−%sk)×%ms) ~ _

  Step 3 — feedback coefficient from blended delay and decay time:
      fb = pow(0.001, %ms / max(1, %dc))

  Step 4 — feedback delay using smoothed delay time:
      fdl_loop(fbs) = de.delay(int(ma.SR×5), int(max(0, sms×SR/1000)), in + fb×fbs)
      fdl = fdl_loop ~ _

  Step 5 — dry/wet mix:
      out = (1 − mx)×in + mx×fdl

  Parameters
  ----------
  :ta — delay target A in milliseconds (1–5000; default 125)
  :tb — delay target B in milliseconds (1–5000; default 500)
  :mo — morph amount; 0.0 = target A, 1.0 = target B (default 0.0)
  :st — smoothing time constant in milliseconds; controls Doppler rate (1–2000; default 200)
  :dc — decay time to −60 dB in milliseconds (1–30000; default 2000)
  :mx — wet/dry mix; 0.0 = dry only, 1.0 = wet only (default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: dry/wet mixed Doppler-morphed delay output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! delay-morphed-times
  {:params {:ta {:range [1.0 5000.0]  :default 125.0}
            :tb {:range [1.0 5000.0]  :default 500.0}
            :mo {:range [0.0 1.0]     :default 0.0}
            :st {:range [1.0 2000.0]  :default 200.0}
            :dc {:range [1.0 30000.0] :default 2000.0}
            :mx {:range [0.0 1.0]     :default 0.5}}}
  (let [in  (audio-in)
        ta  (param :ta)
        tb  (param :tb)
        mo  (param :mo)
        st  (param :st)
        dc  (param :dc)
        mx  (param :mx)
        ms  (faust "(1.0-%{mo})*%{ta}+%{mo}*%{tb}" {:mo mo :ta ta :tb tb})
        sk  (faust "exp(-1000.0/(%{st}*ma.SR))" {:st st})
        sms (faust "(%{sk}*_+(1.0-%{sk})*%{ms})~_" {:sk sk :ms ms})
        fb  (faust "pow(0.001,%{ms}/max(1.0,%{dc}))" {:ms ms :dc dc})
        fdl (faust "fdl_loop ~ _\n  with { fdl_loop(fbs) = de.delay(int(ma.SR*5.0),int(max(0.0,%{sm}*ma.SR/1000.0)),%{in}+%{fb}*fbs); }"
                   {:sm sms :in in :fb fb})
        out (faust "(1.0-%{mx})*%{in}+%{mx}*%{fd}" {:mx mx :in in :fd fdl})]
    (output :out out)))
