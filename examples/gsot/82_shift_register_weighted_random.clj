; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.82-shift-register-weighted-random
  "GSOT pp.130 — shift-register-weighted-random.maxpat.

  Extends shift-register-weighted (example 81) by randomising the weight
  itself on each trigger.  The effective fill probability varies from
  trigger to trigger rather than being fixed, creating gate patterns whose
  density fluctuates continuously.

  Weight randomisation
  --------------------
  On each trigger a new random weight is drawn from a distribution centred
  on :weight with spread controlled by :rnd-amt:

      raw_weight = weight + rnd-amt × no.noise    (audio rate, continuous)
      held_weight = track-hold(clamp(raw_weight, 0, 1), trig)

  The clamp and track-hold ensure:
  - The weight stays in [0,1] (valid probability)
  - The weight is constant between triggers (no intra-step jitter)

  The new bit then uses this per-trigger random weight:
      uniform_01 = 0.5 × (no.noise + 1)           (independent of weight noise)
      new_bit    = float(uniform_01 < held_weight)

  Note: raw_weight uses one no.noise instance; new_bit uses a second
  independent no.noise instance.  In Faust, each literal `no.noise`
  occurrence is a separate LCG register (same mechanism as the 12-noise
  CLT sum in example 62), so the two draws are statistically independent.

  Parameters
  ----------
  :weight  — centre of the weight distribution (default 0.5)
  :rnd-amt — spread of per-trigger weight randomisation (default 0.25)
             At 0: identical to shift-register-weighted with fixed weight
             At 0.5: effective weight varies roughly in [weight±0.5]
             At 1.0: weight can swing from 0 to 1 each trigger

  Outputs
  -------
  :s0–:s7 — 8 gate stages (same chain as examples 78-81)
  :wt     — the held weight for the current trigger (CV monitor output)

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_s0,...,n_s7,n_wh
        with {
          n_wt = hslider(\"weight\",  0.5, 0.0, 1.0, 0.001);
          n_ra = hslider(\"rnd-amt\", 0.25,0.0, 1.0, 0.001);
          n_rw = max(0.0,min(1.0,n_wt+n_ra*no.noise));
          n_wh = (select2(n0>0.5,_,n_rw)~_);
          n_nb = float(0.5*(no.noise+1.0)<n_wh);
          n_s0 = (select2(n0>0.5,_,n_nb)~_);
          n_s1 = (select2(n0>0.5,_,n_s0@1)~_);
          ...
          n_s7 = (select2(n0>0.5,_,n_s6@1)~_);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! shift-register-weighted-random
  {:params {:weight  {:range [0.0 1.0] :default 0.5}
            :rnd-amt {:range [0.0 1.0] :default 0.25}}}
  (let [trig     (audio-in)
        weight   (param :weight)
        rnd-amt  (param :rnd-amt)
        ; Raw weight: centre ± spread, clamped to [0,1]
        raw-wt   (faust "max(0.0,min(1.0,%wt+%ra*no.noise))" {:wt weight :ra rnd-amt})
        ; Hold the weight constant for the duration of each step
        held-wt  (track-hold raw-wt trig)
        ; New bit: independent Bernoulli draw using held weight
        new-bit  (faust "float(0.5*(no.noise+1.0)<%wh)" {:wh held-wt})
        s0       (track-hold new-bit trig)
        s1       (track-hold (faust "%pv@1" {:pv s0}) trig)
        s2       (track-hold (faust "%pv@1" {:pv s1}) trig)
        s3       (track-hold (faust "%pv@1" {:pv s2}) trig)
        s4       (track-hold (faust "%pv@1" {:pv s3}) trig)
        s5       (track-hold (faust "%pv@1" {:pv s4}) trig)
        s6       (track-hold (faust "%pv@1" {:pv s5}) trig)
        s7       (track-hold (faust "%pv@1" {:pv s6}) trig)]
    (output :s0 s0)
    (output :s1 s1)
    (output :s2 s2)
    (output :s3 s3)
    (output :s4 s4)
    (output :s5 s5)
    (output :s6 s6)
    (output :s7 s7)
    (output :wt held-wt)))
