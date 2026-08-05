; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.212-go-ramp-subsample
  "GSOT p.331 — go-ramp.subsample.maxpat (Chapter 10: Windows of Time).

  'Subsample-Accurate Grain Oscillator'
  ----------------------------------------
  go-ramp.subsample.maxpat combines the ramp-subsample-trig computation (ex.211)
  with a grain oscillator whose STARTING PHASE is corrected for the subsample
  position of the trigger event.

  The problem
  -----------
  In poly_granulation1/2 (ex.208-210), the grain oscillator is initialised at
  phase 0 at the trigger sample.  But the actual wrap event occurs at some
  fractional position sf ∈ (0,1) WITHIN that sample interval.  By sample n (the
  trigger sample), the grain has already been running for (1−sf) samples worth of
  oscillation.  The correct initial phase for the grain oscillator AT sample n is:

      grain_init_ph = (1 − sf) × fc / SR

  Setting the feedback accumulator to this value at the trigger corrects the
  oscillator position, as if the grain had started at the precise subsample time.

  Implementation
  --------------
  The grain oscillator is a RESETTABLE ACCUMULATOR with subsample-corrected init:

      trig        = ph_m < ph_m'                         — wrap detection (int 0/1)
      sf          = (1 − ph_m') / (ph_m + 1 − ph_m')    — subsample fraction
      grain_init  = (1 − sf) × :fc / SR                  — corrected start phase
      grain_ph    = select2(trig, _ + fc/SR, grain_init) ~ _  — resettable accum

  At the trigger sample: grain_ph = grain_init (the corrected start).
  At all other samples:  grain_ph += fc/SR (normal oscillation).

  This is the same select2~_ pattern as the resettable accumulator in ex.202
  (windowed-sync), applied here with a NON-ZERO reset value derived from the
  subsample fraction.

  The Hann grain envelope
  -----------------------
  Uses the master phasor ph_m for gate and window phasing (same as ex.203-210).
  The duty cycle dc_g = :gd × :fp / 1000 sets what fraction of each period is
  the grain (the rest is silence).  This is deterministic (fixed :fc); for
  stochastic frequency, see grains2.gendsp (ex.213).

  What this patch demonstrates
  -----------------------------
  The output of go-ramp.subsample and the output of poly_granulation1 (ex.208,
  single voice at :gr = :fp) are IDENTICAL in all parameters EXCEPT the grain
  oscillator's starting phase.  In isolation the difference is inaudible.  The
  benefit becomes audible when:
    — Many subsample-accurate grains are summed: phase-coherent overlap-add is
      possible because each grain's phase is a deterministic function of time.
    — Grains are pitched to a fixed pitch and stacked: the ±1-sample jitter from
      non-corrected initiation creates a slight inharmonic spreading of the
      spectral peaks, visible on a spectrogram and audible at high grain density.
    — The patch is synchronised to an external clock or MIDI source: subsample
      accuracy ensures the grain onset is properly aligned, not smeared over one
      sample of jitter.

  The max-jitter without correction is 1/SR seconds.  At SR=44100 this is ~22μs.
  For most musical applications this is negligible; for high-density granulation,
  analysis-synthesis (phase vocoder reconstruction), or percussive transient
  preservation it matters.

  Parameters
  ----------
  :fp — grain/phasor frequency in Hz; grains fire at this rate (1–100; default 10)
  :gd — grain duration in milliseconds; dc_g = :gd×:fp/1000 (5–500; default 80)
  :fc — grain source frequency in Hz; frequency of the sine within each grain (20–4000; default 440)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained grain oscillator)
    :out — Hann-windowed sine grain, subsample-accurately timed at :fp Hz"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! go-ramp-subsample
  {:params {:fp {:range [1.0 100.0]    :default 10.0}
            :gd {:range [5.0 500.0]   :default 80.0}
            :fc {:range [20.0 4000.0] :default 440.0}}}
  (let [fp  (param :fp)
        gd  (param :gd)
        fc  (param :fc)
        out (faust
              "win*src
               with {
                 dc_g=max(0.001,%{gd}*%{fp}/1000.0);
                 ph_m=os.phasor(1,%{fp});
                 trig=ph_m<ph_m';
                 sf=(1.0-ph_m')/max(0.001,ph_m+1.0-ph_m');
                 gi=(1.0-sf)*%{fc}/ma.SR;
                 grain_ph=(select2(trig,_+%{fc}/ma.SR,gi)~_):ma.frac;
                 win=float(ph_m<dc_g)*0.5*(1.0-cos(2.0*ma.PI*ph_m/dc_g));
                 src=sin(2.0*ma.PI*grain_ph);
               }"
              {:fp fp :gd gd :fc fc})]
    (output :out out)))
