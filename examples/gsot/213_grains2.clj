; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.213-grains2
  "GSOT p.332 — grains2.gendsp (Chapter 10: Windows of Time).

  'Stochastic Grain DSP Unit with Subsample Accuracy'
  -----------------------------------------------------
  grains2.gendsp is the per-grain DSP unit for poly_granulation3.maxpat (ex.214).
  It combines the STOCHASTIC FREQUENCY RANDOMISATION of grain1.gendsp (ex.209)
  with the SUBSAMPLE-ACCURATE ONSET TIMING of go-ramp.subsample (ex.212).

  Lineage
  -------
  grain1.gendsp (ex.209):
    — stochastic freq via S&H: fc_g = S&H(trig, fc × (1 + fs × noise))
    — grain oscillator reset to phase 0 at each trigger sample
    — simple: win × sin(2π × frac(ph × fc_g / gr))
    — timing error: up to ±1 sample jitter at grain onset

  go-ramp.subsample.maxpat (ex.212):
    — fixed :fc, no randomisation
    — grain oscillator RESET TO SUBSAMPLE-CORRECTED PHASE:
          grain_init = (1 − sf) × fc / SR
          grain_ph   = (select2(trig, _ + fc/SR, grain_init) ~ _) : frac
    — timing error: sub-sample accurate (< 1/SR ≈ 22μs at 44.1kHz)

  grains2.gendsp (this patch):
    — combines BOTH: stochastic fc_g AND subsample-corrected oscillator init
    — at each trigger: fc_g drawn from S&H, grain_init uses the NEW fc_g
    — subsample fraction sf computed from the master phasor wrap as in ex.211

  Key detail: fc_g at the trigger sample
  ----------------------------------------
  `fc_g = ba.sAndH(trig, max(20, fc × (1 + fs × noise)))`

  At the trigger sample (trig=1), S&H samples a new random frequency.
  `grain_init = (1 − sf) × fc_g / SR` then uses THIS NEW value.

  The feedback accumulator then increments by `fc_g / SR` per sample:
  `grain_ph = (select2(trig, _ + fc_g/SR, grain_init) ~ _) : frac`

  At non-trigger samples fc_g holds its previous value, so the accumulator
  increments at the currently-active grain's frequency.  When the next trigger
  fires, fc_g jumps to the new random frequency AND the accumulator resets to
  the new grain_init.  The two updates happen at the SAME SAMPLE. ✓

  Grain duration interaction with fc_g
  --------------------------------------
  The Hann window is gated by `float(ph < dc_g)` using the MASTER PHASOR ph,
  not the grain oscillator phase.  This means the grain duration dc_g depends
  only on :gd and :gr, not on the randomly selected :fc_g.  The window shape
  is always the same fraction of the grain period regardless of what fc_g was
  drawn, which keeps the temporal density stable even at high :fs scatter.

  Effect of combining stochastic freq + subsample correction
  -----------------------------------------------------------
  In dense granular clouds, grain1.gendsp (ex.209) produces a spectrogram that
  shows slight phase smearing: peaks at the nominal frequency :fc have a subtle
  blur of ±fc/SR around them, caused by the quantised onset time.  The blur
  increases with :fc (higher freq = more phase advance per sample).

  grains2.gendsp eliminates this smearing: even though each grain has a randomly
  scattered frequency, the ONSET PHASE of each grain is computed exactly.  The
  spectrogram shows the same random scatter as grain1 (the stochastic spread)
  but without the additional smearing from onset jitter.  At low grain densities
  and low :fc this difference is inaudible; at high density and high :fc it
  produces a cleaner, sharper spectral texture.

  Parameters
  ----------
  :gr — grain rate in Hz; grains trigger at this rate (1–100; default 10)
  :gd — grain duration in milliseconds (5–500; default 80)
  :fc — centre frequency in Hz; random scatter is centred here (20–4000; default 440)
  :fs — frequency scatter width; 0=no scatter, 1=±100% (0–1; default 0.3)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-triggering stochastic grain oscillator with subsample accuracy)
    :out — Hann-windowed grain; subsample-accurate onset; per-grain random frequency"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! grains2
  {:params {:gr {:range [1.0 100.0]   :default 10.0}
            :gd {:range [5.0 500.0]   :default 80.0}
            :fc {:range [20.0 4000.0] :default 440.0}
            :fs {:range [0.0 1.0]     :default 0.3}}}
  (let [gr  (param :gr)
        gd  (param :gd)
        fc  (param :fc)
        fs  (param :fs)
        out (faust
              "grain
               with {
                 dc_g=max(0.001,%gd*%gr/1000.0);
                 ph=os.phasor(1,%gr);
                 trig=ph<ph';
                 sf=(1.0-ph')/max(0.001,ph+1.0-ph');
                 fc_g=ba.sAndH(trig,max(20.0,%fc*(1.0+%fs*no.noise)));
                 gi=(1.0-sf)*fc_g/ma.SR;
                 grain_ph=(select2(trig,_+fc_g/ma.SR,gi)~_):ma.frac;
                 win=float(ph<dc_g)*0.5*(1.0-cos(2.0*ma.PI*ph/dc_g));
                 src=sin(2.0*ma.PI*grain_ph);
                 grain=win*src;
               }"
              {:gr gr :gd gd :fc fc :fs fs})]
    (output :out out)))
