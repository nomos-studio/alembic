; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.61-random-periods
  "GSOT p.98 — random_periods.maxpat.

  'Random periods' (Chapter 4) — self-oscillating random-length clock
  -------------------------------------------------------------------
  A trigger input starts a period of random length [lo, hi] seconds.
  Two outputs: normalised phase [0,1] running across that period and a
  trigger pulse when the period elapses.  Patch :trig output back to the
  trigger input (audio-in) for self-oscillation with randomly varying
  period.  The initial trigger from outside starts the process; after that
  the patch self-clocks.

  Signal flow
  -----------
  audio-in: trig    — trigger pulse (initial kick or self-patched :trig out)
  params:   :lo     — minimum period in seconds
            :hi     — maximum period in seconds

      rand-p  = (lo + (hi−lo)·0.5·(noise+1)) · SR          samples, [lo·SR, hi·SR]
      period  = track-hold(rand-p, trig)                     hold period on trigger
      counter = (select2(trig>0.5, _+1, 0) ~ _)             resets to 0 on trigger
      phase   = min(1, counter / max(1, period))             normalised [0, 1]
      trig-out = float(counter≥period) · float(counter@1<period)   end-of-period pulse

  The counter accumulates by 1 each sample and resets to 0 on each input
  trigger.  When the counter reaches the held period (in samples) trig-out
  fires a 1-sample pulse.  A new random period is latched on the next trigger.

  Self-oscillation
  ----------------
  Patch :trig → trig-in (audio-in) via a cable:
      trig-out fires → counter resets + new period sampled → trig-out fires again
  The period varies randomly each cycle over [lo, hi] seconds.  lo=hi gives a
  fixed-period clock (deterministic).

  Why not embed the feedback in the DSP?
  ---------------------------------------
  Alembic's graph is a DAG.  Self-oscillation requires a cycle:
      period_hold → phasor → wrap_trig → period_hold.
  The modular answer is to externalise that cycle as a patch cable — exactly
  as in the gen~  patch, where `random_periods.maxpat` is wired into a larger
  patch via trigger cables.  The self-clocking behavior emerges from the
  patch, not from a single node.

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n4, n5
        with {
          n1 = hslider(\"lo\", 0.2, 0.01, 4.0, 0.0001);
          n2 = hslider(\"hi\", 2.0, 0.01, 4.0, 0.0001);
          n3 = (n1+(n2-n1)*0.5*(no.noise+1.0))*float(ma.SR);
          n6 = (select2(n0>0.5,_,n3)~_);              // period held (samples)
          n7 = (select2(n0>0.5,_+1.0,0.0)~_);         // counter (resets on trig)
          n4 = min(1.0,n7/max(1.0,n6));               // phase [0,1]
          n5 = float(n7>=n6)*float(n7@1<n6);           // end-of-period trig
        };
      process = alembic_dsp;

  n0 = trig (audio-in)
  n1 = lo param  n2 = hi param
  n3 = random period in samples
  n6 = period_held  n7 = counter
  n4 = phase  n5 = trig-out"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! random-periods
  {:params {:lo {:range [0.01 4.0] :default 0.2}
            :hi {:range [0.01 4.0] :default 2.0}}}
  (let [trig     (audio-in)
        lo       (param :lo)
        hi       (param :hi)
        rand-p   (faust "(%{lo}+(%{hi}-%{lo})*0.5*(no.noise+1.0))*float(ma.SR)"
                        {:lo lo :hi hi})
        period   (track-hold rand-p trig)
        counter  (faust "(select2(%{tr}>0.5,_+1.0,0.0)~_)"       {:tr trig})
        phase    (faust "min(1.0,%{ct}/max(1.0,%{pr}))"             {:ct counter :pr period})
        trig-out (faust "float(%{ct}>=%{pr})*float(%{ct}@1<%{pr})"      {:ct counter :pr period})]
    (output :phase phase)
    (output :trig  trig-out)))
