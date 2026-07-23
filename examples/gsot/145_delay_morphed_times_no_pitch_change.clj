; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.145-delay-morphed-times-no-pitch-change
  "GSOT pp.209-210 — delay_morphed_times_no_pitch_change.maxpat (Chapter 7).

  'Changing Delay Time Without Pitch Shift — Two-Tap Crossfade'
  --------------------------------------------------------------
  Contrast with ex.144 (delay_morphed_times): instead of smoothing the DELAY
  TIME (which moves the read pointer, causing Doppler), smooth the CROSSFADE
  GAIN between two delay taps that are each held at a fixed read position.

  The key insight
  ----------------
  Doppler pitch shift happens only when a delay line's read pointer MOVES.
  If the read pointer is stationary, the delay output has no pitch shift
  regardless of what the delay time value is.

  Two-tap approach:
    - Tap A: reads continuously at :ta — stationary read pointer, no pitch shift
    - Tap B: reads continuously at :tb — stationary read pointer, no pitch shift
    - Crossfade: blend A→B by changing the MIX LEVEL, not the delay times

  During the crossfade, both taps are playing simultaneously.  The transition
  is heard as a volume crossfade between two pitch-stable echo streams, NOT as
  a pitch glide.  The 'current' delay time effectively changes from :ta to :tb,
  but without any Doppler artefact.

  What to smooth
  ---------------
  ex.144  smooth the DELAY TIME (:ms)  → Doppler pitch shift (read pointer moves)
  ex.145  smooth the CROSSFADE GAIN (:mo) → no pitch shift (read pointers fixed)

  The one-pole lag is applied to :mo (the morph amount), not to :ta or :tb.

  Transition artefact in the two-tap approach
  ---------------------------------------------
  During the crossfade both echo streams are audible simultaneously — a brief
  period where the delay effect plays at both times at once.  At 50 % into the
  crossfade, BOTH :ta and :tb echos are at equal volume.  This is an artefact of
  the technique.  For short crossfade times (small :st) it is barely audible;
  for long :st it is more prominent.

  The independent feedback loops for each tap mean the echo trails develop
  separately.  When the crossfade completes, only one trail remains.

  Relationship between ex.143, ex.144, ex.145
  ---------------------------------------------
  ex.143  smooth delay TIME → suppresses clicks (tiny :st, effect barely audible)
  ex.144  smooth delay TIME → Doppler as musical effect (large :st, effect prominent)
  ex.145  smooth crossfade GAIN → pitch-shift-free transition at any :st

  Faust signal chain
  -------------------
  Step 1 — smooth the morph amount (not the delay times):
      sk  = exp(−1000 / (:st × ma.SR))
      smo = (sk×_ + (1−sk)×:mo) ~ _

  Step 2 — feedback coefficients for each tap:
      fba = pow(0.001, :ta / max(1, :dc))
      fbb = pow(0.001, :tb / max(1, :dc))

  Step 3 — two independent feedback delay loops at fixed read positions:
      dla = dla_loop ~ _  where dla_loop(fbs) = de.delay(maxD, D_a, in + fba×fbs)
      dlb = dlb_loop ~ _  where dlb_loop(fbs) = de.delay(maxD, D_b, in + fbb×fbs)

  Step 4 — crossfade between taps using smoothed gain:
      wet = (1 − smo)×dla + smo×dlb

  Step 5 — dry/wet mix:
      out = (1 − mx)×in + mx×wet

  Parameters
  ----------
  :ta — delay tap A time in milliseconds (1–5000; default 125)
  :tb — delay tap B time in milliseconds (1–5000; default 500)
  :mo — crossfade morph; 0.0 = tap A only, 1.0 = tap B only (default 0.0)
  :st — smoothing time for crossfade gain in ms; controls transition speed (1–2000; default 50)
  :dc — decay time to −60 dB in milliseconds (1–30000; default 2000)
  :mx — wet/dry mix; 0.0 = dry only, 1.0 = wet only (default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: pitch-shift-free crossfaded delay output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! delay-morphed-times-no-pitch-change
  {:params {:ta {:range [1.0 5000.0]  :default 125.0}
            :tb {:range [1.0 5000.0]  :default 500.0}
            :mo {:range [0.0 1.0]     :default 0.0}
            :st {:range [1.0 2000.0]  :default 50.0}
            :dc {:range [1.0 30000.0] :default 2000.0}
            :mx {:range [0.0 1.0]     :default 0.5}}}
  (let [in  (audio-in)
        ta  (param :ta)
        tb  (param :tb)
        mo  (param :mo)
        st  (param :st)
        dc  (param :dc)
        mx  (param :mx)
        sk  (faust "exp(-1000.0/(%st*ma.SR))" {:st st})
        smo (faust "(%sk*_+(1.0-%sk)*%mo)~_" {:sk sk :mo mo})
        fba (faust "pow(0.001,%ta/max(1.0,%dc))" {:ta ta :dc dc})
        fbb (faust "pow(0.001,%tb/max(1.0,%dc))" {:tb tb :dc dc})
        dla (faust "dla_loop ~ _\n  with { dla_loop(fbs) = de.delay(int(ma.SR*5.0),int(max(0.0,%ta*ma.SR/1000.0)),%in+%fa*fbs); }"
                   {:ta ta :in in :fa fba})
        dlb (faust "dlb_loop ~ _\n  with { dlb_loop(fbs) = de.delay(int(ma.SR*5.0),int(max(0.0,%tb*ma.SR/1000.0)),%in+%fb*fbs); }"
                   {:tb tb :in in :fb fbb})
        wet (faust "(1.0-%sm)*%da+%sm*%db" {:sm smo :da dla :db dlb})
        out (faust "(1.0-%mx)*%in+%mx*%wt" {:mx mx :in in :wt wet})]
    (output :out out)))
