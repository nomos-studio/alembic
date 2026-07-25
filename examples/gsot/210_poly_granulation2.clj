; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.210-poly-granulation2
  "GSOT pp.326-327 — poly_granulation2.maxpat (Chapter 10: Windows of Time).

  'Four-Voice Asynchronous Stochastic Granular Synthesizer'
  ----------------------------------------------------------
  Extends poly_granulation1.maxpat (ex.208) with per-grain stochastic frequency
  variation, implementing the grain1.gendsp DSP unit (ex.209) in four parallel
  voices.

  Asynchronous and stochastic (pp.326-327)
  -----------------------------------------
  poly_granulation1 (ex.208) produced a DETERMINISTIC grain stream: all grains
  had the same frequency :fc and fired at regular staggered intervals.  The result
  was rhythmically regular and tonally static — a useful starting point, but not
  the organic cloud texture characteristic of granular synthesis.

  'Asynchronous' and 'stochastic' refer to two related departures from regularity:

  STOCHASTIC parameters:  Each grain draws its parameters from a random distribution
  centred on a nominal value.  Here, grain frequency is drawn from a uniform
  distribution [fc×(1−fs), fc×(1+fs)].  Other parameters (duration, amplitude,
  pan) can also be randomised in later versions.

  ASYNCHRONOUS texture:  The temporal regularity of the grain stream is broken
  (or appears broken) because successive grains at the same voice phase have
  DIFFERENT FREQUENCIES.  Even though the grain timing is still metronomic
  (voices still staggered at 0/0.25/0.5/0.75), the random spectral content per
  grain disrupts the audible periodicity, creating a cloud-like rather than
  pulse-like texture.  True asynchronous timing (random inter-onset intervals)
  is the next step beyond this patch.

  Per-voice independent randomisation via shared noise + staggered S&H
  ----------------------------------------------------------------------
  All four voices share a SINGLE `no.noise` source (white noise, audio rate, ±1).
  Independent random values per voice are obtained by exploiting the DIFFERENT
  TRIGGER TIMES of the four staggered phasors:

    — Voice 0 phasor wraps at t=0, T, 2T, ...         → S&H fires at those times
    — Voice 1 phasor wraps at t=T/4, 5T/4, ...        → S&H fires at those times
    — Voice 2 phasor wraps at t=T/2, 3T/2, ...        → fires at different samples
    — Voice 3 phasor wraps at t=3T/4, 7T/4, ...       → fires at different samples

  Each S&H captures whatever value `no.noise` happens to output at its trigger
  moment.  Since `no.noise` is white noise (uncorrelated sample-to-sample), the
  four captured values are statistically independent for any grain rate above a
  few Hz.  No separate noise generators or seeds are needed.

  The four per-voice frequencies (fc0–fc3) are held constant within each grain
  and updated independently at each voice's grain boundary.

  Architecture
  ------------
  Identical to ex.208 except `win(ph)×src(ph)` becomes `win(ph)×src(ph, fc_g)`:

      rnd    = no.noise                          — shared noise source
      phN    = frac(phasor(gr) + N/4)            — staggered phasors (N=0,1,2,3)
      trN    = phN < phN'                         — grain-start triggers
      fcN    = S&H(trN, max(20, fc×(1+fs×rnd)))  — per-grain random frequency
      win(ph)= gate×Hann(ph/dc_g)                — Hann envelope
      src(ph,fc_g) = sin(2π·frac(ph·fc_g/gr))   — source sine at held frequency
      gN     = win(phN) × src(phN, fcN)           — per-voice grain output
      out    = 0.25 × (g0 + g1 + g2 + g3)        — sum and scale

  Effect of :fs on texture
  -------------------------
  :fs=0.0  — identical to ex.208; deterministic grain stream, single pitch :fc.
  :fs=0.1  — gentle shimmer; grains cluster tightly around :fc (±10%).
             Pitched texture with slight chorus-like width.
  :fs=0.3  — moderate scatter; approximately ±1/3 octave spread.
             Recognisably pitched cloud with noticeable timbral variation.
  :fs=0.5  — grains span a tritone above and below :fc.
             Borderline between pitched and noisy; spectral centroid smears.
  :fs=1.0  — maximum scatter; grains span from silence (near 0Hz) to 2×:fc.
             Broad-bandwidth noise cloud; :fc sets only the spectral envelope peak.

  Parameters
  ----------
  :gr — grain rate per voice in Hz; total density = 4×:gr (1–100; default 10)
  :gd — grain duration in milliseconds; dc_g = :gd×:gr/1000 (5–500; default 80)
  :fc — centre frequency in Hz; random scatter is distributed around this value (20–4000; default 440)
  :fs — frequency scatter width; 0=deterministic, 1=±100% (0–1; default 0.3)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained stochastic granular oscillator)
    :out — sum of 4 independent stochastic grain voices (0.25×sum)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! poly-granulation2
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
                 tr0=ph0<ph0'; tr1=ph1<ph1';
                 tr2=ph2<ph2'; tr3=ph3<ph3';
                 fc0=ba.sAndH(tr0,max(20.0,%fc*(1.0+%fs*rnd)));
                 fc1=ba.sAndH(tr1,max(20.0,%fc*(1.0+%fs*rnd)));
                 fc2=ba.sAndH(tr2,max(20.0,%fc*(1.0+%fs*rnd)));
                 fc3=ba.sAndH(tr3,max(20.0,%fc*(1.0+%fs*rnd)));
                 win(ph)=float(ph<dc_g)*0.5*(1.0-cos(2.0*ma.PI*ph/dc_g));
                 src(ph,fg)=sin(2.0*ma.PI*ma.frac(ph*fg/max(%gr,1.0)));
                 g0=win(ph0)*src(ph0,fc0);
                 g1=win(ph1)*src(ph1,fc1);
                 g2=win(ph2)*src(ph2,fc2);
                 g3=win(ph3)*src(ph3,fc3);
               }"
              {:gr gr :gd gd :fc fc :fs fs})]
    (output :out out)))
