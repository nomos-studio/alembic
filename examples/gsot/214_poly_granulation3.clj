; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.214-poly-granulation3
  "GSOT p.333 — poly_granulation3.maxpat (Chapter 10: Windows of Time).

  'Four-Voice Stochastic Granular Synthesizer with Subsample Accuracy'
  ---------------------------------------------------------------------
  poly_granulation3.maxpat is the capstone of the Chapter 10 granulation sequence.
  It extends poly_granulation2.maxpat (ex.210) by replacing the grain oscillators
  with SUBSAMPLE-ACCURATE variants from grains2.gendsp (ex.213).

  Architecture — what changes from poly_granulation2 (ex.210)
  -------------------------------------------------------------
  poly_granulation2: each voice drives a simple table-read oscillator:
      src(ph, fc_g) = sin(2π × frac(ph × fc_g / gr))
  The oscillator phase is derived FROM THE MASTER PHASOR — at the trigger sample
  (ph ≈ 0), the inner phasor reads at position frac(0 × fc_g / gr) = 0. There is
  no explicit reset and no subsample correction.

  poly_granulation3: each voice drives a RESETTABLE ACCUMULATOR oscillator:
      gp(tr,sf,fg) = (select2(tr, _ + fg/SR, (1-sf)×fg/SR) ~ _) : frac
      src(tr,sf,fg) = sin(2π × gp(tr,sf,fg))
  At each trigger: gp resets to (1-sf)×fg/SR (subsample-corrected start).
  Between triggers: gp accumulates fg/SR per sample.

  Each call to gp(trN,sfN,fcN) creates an INDEPENDENT feedback state for voice N.
  This is the same principle as voice(hz,gt) in poly_voices.maxpat (ex.207) — named
  functions instantiate separate state per call site in Faust's dataflow graph.

  Per-voice subsample fractions
  ------------------------------
  Each voice has its own phasor phN and its own subsample fraction sfN:

      sfN = (1.0 - phN') / max(0.001, phN + 1.0 - phN')

  The four phasors have offsets 0/0.25/0.5/0.75 so their wraps are staggered.
  Because the phasors are at the same frequency (:gr) with only a phase offset,
  their per-sample phase increments are identical.  The sfN values will be
  different at each voice's trigger time because the NOISE SOURCE samples at
  different moments — but the subsample geometry (the wrap fraction relative to
  the increment) is the same for all four voices.  The correction is applied
  independently to each voice's own grain oscillator accumulator.

  Independent per-voice S&H with shared noise
  ---------------------------------------------
  Identical to poly_granulation2 (ex.210): a single `no.noise` source is shared;
  per-voice independence comes from the staggered S&H trigger times.  The four
  S&H triggers fire at samples separated by T/4 (T = 1/gr).  Since white noise
  is uncorrelated sample-to-sample, each voice captures an independent random
  value.

  When each fcN is captured AND used immediately to compute giN (grain_init),
  the subsample correction is automatically applied with the per-voice random
  frequency.  The two randomness mechanisms (stochastic fc, stochastic sf) are
  independent:
    — fcN is random (drawn from noise each trigger)
    — sfN is pseudo-random (depends on the exact wrap position, which varies with
      the ratio gr/SR)

  Parameters
  ----------
  :gr — grain rate per voice in Hz; 4×:gr = total density (1–100; default 10)
  :gd — grain duration in milliseconds; dc_g = :gd×:gr/1000 (5–500; default 80)
  :fc — centre frequency in Hz; scatter distributed around this value (20–4000; default 440)
  :fs — frequency scatter width; 0=deterministic, 1=±100% (0–1; default 0.3)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained stochastic granular oscillator)
    :out — sum of 4 stochastic subsample-accurate grain voices (0.25×sum)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! poly-granulation3
  {:params {:gr {:range [1.0 100.0]   :default 10.0}
            :gd {:range [5.0 500.0]   :default 80.0}
            :fc {:range [20.0 4000.0] :default 440.0}
            :fs {:range [0.0 1.0]     :default 0.3}}}
  (let [gr  (param :gr)
        gd  (param :gd)
        fc  (param :fc)
        fs  (param :fs)
        out (faust
              "0.25*(g0+g1+g2+g3)
               with {
                 rnd=no.noise;
                 dc_g=max(0.001,%gd*%gr/1000.0);
                 ph0=ma.frac(os.phasor(1,%gr)+0.0);
                 ph1=ma.frac(os.phasor(1,%gr)+0.25);
                 ph2=ma.frac(os.phasor(1,%gr)+0.5);
                 ph3=ma.frac(os.phasor(1,%gr)+0.75);
                 tr0=ph0<ph0'; tr1=ph1<ph1'; tr2=ph2<ph2'; tr3=ph3<ph3';
                 sf0=(1.0-ph0')/max(0.001,ph0+1.0-ph0');
                 sf1=(1.0-ph1')/max(0.001,ph1+1.0-ph1');
                 sf2=(1.0-ph2')/max(0.001,ph2+1.0-ph2');
                 sf3=(1.0-ph3')/max(0.001,ph3+1.0-ph3');
                 fc0=ba.sAndH(tr0,max(20.0,%fc*(1.0+%fs*rnd)));
                 fc1=ba.sAndH(tr1,max(20.0,%fc*(1.0+%fs*rnd)));
                 fc2=ba.sAndH(tr2,max(20.0,%fc*(1.0+%fs*rnd)));
                 fc3=ba.sAndH(tr3,max(20.0,%fc*(1.0+%fs*rnd)));
                 win(ph)=float(ph<dc_g)*0.5*(1.0-cos(2.0*ma.PI*ph/dc_g));
                 gp(tr,sf,fg)=(select2(tr,_+fg/ma.SR,(1.0-sf)*fg/ma.SR)~_):ma.frac;
                 src(tr,sf,fg)=sin(2.0*ma.PI*gp(tr,sf,fg));
                 g0=win(ph0)*src(tr0,sf0,fc0);
                 g1=win(ph1)*src(tr1,sf1,fc1);
                 g2=win(ph2)*src(tr2,sf2,fc2);
                 g3=win(ph3)*src(tr3,sf3,fc3);
               }"
              {:gr gr :gd gd :fc fc :fs fs})]
    (output :out out)))
