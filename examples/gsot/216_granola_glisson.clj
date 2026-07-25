; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.216-granola-glisson
  "GSOT pp.333-336 — granola_glisson.maxpat (Chapter 10: Windows of Time).

  'Four-Voice Glisson Granular Synthesizer'
  ------------------------------------------
  granola_glisson.maxpat is the second 'whole grains at once' patch.  It
  implements GLISSON SYNTHESIS, a granular technique where each grain has a
  LINEAR FREQUENCY SWEEP (glissando) from one pitch to another over its duration.

  Glisson synthesis — background
  --------------------------------
  Regular granular synthesis (ex.208-214) assigns each grain a CONSTANT frequency.
  Even with stochastic scatter (:fs in ex.209-210), each individual grain is a
  single-pitch event.  The spectral texture of the cloud reflects the distribution
  of grain frequencies, but no single grain exhibits pitch motion.

  Glisson synthesis, described by Curtis Roads (Microsound, 2001) and originating
  in Xenakis's concept of 'glissandi clouds', allows each grain to SWEEP in pitch
  during its lifetime.  The result is a distinctly different texture:

    :f1 = :f2  — constant pitch grains; equivalent to ex.208-210
    :f1 < :f2  — each grain sweeps UP in pitch; cloud has an ascending shimmer
    :f1 > :f2  — each grain sweeps DOWN; cloud has a descending shimmer
    :f1 ≪ :f2 — large sweeps; 'chirping' texture; individual sweeps audible at
                 low grain density, smeared into a spectral blur at high density
    :f1 ≈ 0   — grains start near DC and sweep up; each grain is a rising transient

  At high grain density (4×:gr large, dc_g near 1), the individual sweeps blend
  into a characteristic granular 'sheen' — smooth and continuous with a slight
  directional quality that distinguishes it from pitched or noisy granulation.

  The frequency sweep
  --------------------
  The instantaneous frequency within a grain is a linear function of position
  within the grain's window:

      grain_frac(ph) = min(ph / dc_g, 1.0)       — 0→1 during grain, 1 after
      freq_t(ph)     = :f1 + (:f2 − :f1) × grain_frac(ph)   — linear interp

  At ph=0 (grain start): freq_t = :f1.
  At ph=dc_g (grain end): freq_t = :f2.
  After the grain (ph > dc_g): freq_t = :f2 (clamped), but window gate is 0.

  Phase accumulator with time-varying increment
  ----------------------------------------------
  The grain oscillator is a RESETTABLE ACCUMULATOR using the instantaneous
  frequency:

      grain_ph = (select2(trig, _ + freq_t/SR, 0.0) ~ _) : frac

  At the trigger sample (trig=1): grain_ph resets to 0.
  Between triggers (trig=0): grain_ph increments by freq_t/SR per sample.

  Since `freq_t` is derived from the master phasor ph (external signal), it
  changes every sample and the accumulator integrates the instantaneous frequency.
  This is a first-order FM integration — the total phase at grain position ph is:

      φ(ph) = ∫₀^ph freq(t)/SR dt
             = ∫₀^ph [f1 + (f2-f1)×t/dc_g] / SR dt
             = [f1×ph + (f2-f1)×ph²/(2×dc_g)] / SR

  The quadratic phase gives the chirp characteristic of glisson synthesis.

  Named `gp(tr, pf)` function
  ----------------------------
  The per-voice accumulator uses the same named-function pattern as ex.214
  (poly_granulation3):

      gp(tr,pf) = (select2(tr, _ + (%f1+(%f2-%f1)*pf)/ma.SR, 0.0) ~ _) : ma.frac

  Each call to `gp(trN, pfN)` creates an INDEPENDENT feedback state for that
  voice.  The phase fraction `pf = min(phN/dc_g, 1.0)` correctly drives the
  frequency ramp for voice N.

  Note that for glisson synthesis the grain START PHASE is always 0 (unlike
  ex.212-214 which used a subsample-corrected init).  GSOT's glisson patch does
  not combine subsample accuracy with the frequency sweep — this is a design
  choice: the sweep's prominent spectral motion renders the ±1-sample onset
  jitter perceptually negligible.

  Relationship to FM/chirp synthesis
  ------------------------------------
  A glisson grain is mathematically a CHIRP SIGNAL — a sinusoid with
  quadratically increasing phase.  Chirps appear in radar, sonar, and spread-
  spectrum communications.  In music, chirp-like sounds include plucked string
  attack transients (where f decreases as the string settles) and some percussion
  attacks.  Glisson synthesis generates a CLOUD of chirps, which at high density
  produces a characteristic spectral 'smear' different from both pitched and
  noisy granulation.

  Parameters
  ----------
  :gr — grain rate per voice in Hz; 4×:gr = total density (1–100; default 10)
  :gd — grain duration in milliseconds (5–500; default 80)
  :f1 — grain start frequency in Hz (20–4000; default 220)
  :f2 — grain end frequency in Hz (20–4000; default 880)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained glisson grain cloud synthesizer)
    :out — sum of 4 staggered glisson grain voices (0.25×sum); each grain sweeps :f1→:f2"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! granola-glisson
  {:params {:gr {:range [1.0 100.0]   :default 10.0}
            :gd {:range [5.0 500.0]   :default 80.0}
            :f1 {:range [20.0 4000.0] :default 220.0}
            :f2 {:range [20.0 4000.0] :default 880.0}}}
  (let [gr  (param :gr)
        gd  (param :gd)
        f1  (param :f1)
        f2  (param :f2)
        out (faust
              "0.25*(g0+g1+g2+g3)
               with {
                 dc_g=max(0.001,%gd*%gr/1000.0);
                 ph0=ma.frac(os.phasor(1,%gr)+0.0);
                 ph1=ma.frac(os.phasor(1,%gr)+0.25);
                 ph2=ma.frac(os.phasor(1,%gr)+0.5);
                 ph3=ma.frac(os.phasor(1,%gr)+0.75);
                 tr0=ph0<ph0'; tr1=ph1<ph1'; tr2=ph2<ph2'; tr3=ph3<ph3';
                 win(ph)=float(ph<dc_g)*0.5*(1.0-cos(2.0*ma.PI*ph/dc_g));
                 gp(tr,pf)=(select2(tr,_+(%f1+(%f2-%f1)*pf)/ma.SR,0.0)~_):ma.frac;
                 src(tr,pf)=sin(2.0*ma.PI*gp(tr,pf));
                 pf0=min(ph0/dc_g,1.0); pf1=min(ph1/dc_g,1.0);
                 pf2=min(ph2/dc_g,1.0); pf3=min(ph3/dc_g,1.0);
                 g0=win(ph0)*src(tr0,pf0);
                 g1=win(ph1)*src(tr1,pf1);
                 g2=win(ph2)*src(tr2,pf2);
                 g3=win(ph3)*src(tr3,pf3);
               }"
              {:gr gr :gd gd :f1 f1 :f2 f2})]
    (output :out out)))
