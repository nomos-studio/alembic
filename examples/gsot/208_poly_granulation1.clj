; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.208-poly-granulation1
  "GSOT p.325 — poly_granulation1.maxpat (Chapter 10: Windows of Time).

  'Four-Voice Staggered Granular Synthesizer'
  ---------------------------------------------
  poly_granulation1.maxpat extends the polyphonic voice framework of
  poly_voices.maxpat (ex.207) from pitched ADSR voices to GRAINS.

  What is a grain?
  ----------------
  A grain is a very short event — typically 10–200ms — consisting of:
    1. A SOURCE: a snippet of audio (recorded buffer, wavetable, or analytical waveform)
    2. A WINDOW: an amplitude envelope (typically Hann) applied over the grain duration
    3. A POSITION: where in the source to start reading
    4. A RATE: playback speed (affects pitch)

  The grain is the atomic unit of granular synthesis.  The musical texture emerges
  from the cumulative effect of many overlapping grains, each potentially with
  slightly randomised parameters.

  poly_granulation1.maxpat — architecture
  -----------------------------------------
  Max's `poly~` is used with a grain-voice DSP patcher.  Each `poly~` voice is
  one grain slot.  A metronome or scheduler in poly_voices.maxpat triggers new
  grains into available voice slots at rate :gr (grains per second per voice).
  Multiple simultaneous grains from different `poly~` slots overlap in time,
  creating a granular stream.

  This first version (poly_granulation1) uses:
    — Periodic grain triggering at a fixed rate :gr
    — Fixed grain duration :gd ms
    — Sine source at formant frequency :fc
    — No randomisation (later versions would scatter position, rate, or pan)

  Alembic — 4 staggered grain voices
  -------------------------------------
  Since Alembic has no `poly~` allocation, 4 grain voices are generated in
  parallel with STAGGERED PHASE OFFSETS (0, 1/4, 1/2, 3/4 of the grain period).
  Each voice fires its own grain train at rate :gr; the stagger ensures grains
  from different voices do not coincide, creating evenly-distributed events.

  Voice 0: phase 0.00 — grains at t=0, T, 2T, 3T, ...
  Voice 1: phase 0.25 — grains at t=T/4, 5T/4, 9T/4, ...
  Voice 2: phase 0.50 — grains at t=T/2, 3T/2, 5T/2, ...
  Voice 3: phase 0.75 — grains at t=3T/4, 7T/4, 11T/4, ...

  Effective grain density: 4 × :gr grains per second.

  Grain duty cycle
  -----------------
  The ratio of grain duration to grain period determines the overlap:

      dc_g = :gd (ms) × :gr (Hz) / 1000

  dc_g < 0.25  — each grain ends before the next voice's grain begins.
                 Sparse texture; audible gaps between grains.
  dc_g = 0.25  — grains just touch (no overlap); continuous at density 4×:gr.
  dc_g = 0.5   — adjacent voices overlap by 50% of their grain duration.
                 Smooth, continuous texture (typical musical target).
  dc_g ≥ 1.0   — grains from the SAME voice overlap (before the voice fires again).
                 Dense cloud texture; Hann overlap-add sum ≈ constant amplitude.

  Pulsar synthesis connection
  ----------------------------
  The grain mechanism — gate × Hann window × source — is IDENTICAL to the pulsaret
  in ex.203 (pulsar.maxpat):

      pulsaret:  gate = ph_p < :dc;   win = gate × Hann(ph_f);  src = formant sine
      grain:     gate = ph_g < dc_g;  win = gate × Hann(ph_f);  src = source sine

  The difference is conceptual:
    — Pulsar synthesis starts from FREQUENCY (pitch = :fp) and derives a duty cycle.
    — Granular synthesis starts from DURATION (:gd ms) and a DENSITY (:gr grains/s).
    — In pulsar, the formant :fc sets the spectral centroid of a pitched event.
    — In granulation, :fc sets the source frequency; the 'pitch' is not :gr directly
      (the Hann window's spectral smearing blurs the pitch), though :fc dominates
      when dc_g is large.

  Hann window and overlap-add
  ----------------------------
  The Hann window applied to each grain ensures smooth edges: the grain amplitude
  starts at 0, peaks at the grain midpoint, returns to 0.  When multiple grains
  overlap with correct phase spacing, the Hann window sum approaches a constant:

      Sum of N Hann windows at 1/N spacing ≈ N × 0.5 (for the normalisation used here)

  The 0.25 output scale compensates for N=4 voices, giving approximately unity gain
  when dc_g is large enough that all 4 voices contribute simultaneously.

  Parameters
  ----------
  :gr — grain rate in Hz per voice; 4×:gr = total grain density (1–100; default 10)
  :gd — grain duration in milliseconds; dc_g = :gd×:gr/1000 sets overlap (5–500; default 80)
  :fc — source/formant frequency in Hz; sine oscillator inside each grain (20–4000; default 440)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained granular oscillator)
    :out — sum of 4 staggered grain voices (0.25 × sum), Hann-windowed sine source"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! poly-granulation1
  {:params {:gr {:range [1.0 100.0]   :default 10.0}
            :gd {:range [5.0 500.0]   :default 80.0}
            :fc {:range [20.0 4000.0] :default 440.0}}}
  (let [gr  (param :gr)
        gd  (param :gd)
        fc  (param :fc)
        out (faust
              "0.25*(g0+g1+g2+g3)
               with {
                 dc_g=max(0.001,%gd*%gr/1000.0);
                 ph0=ma.frac(os.phasor(1,%gr)+0.0);
                 ph1=ma.frac(os.phasor(1,%gr)+0.25);
                 ph2=ma.frac(os.phasor(1,%gr)+0.5);
                 ph3=ma.frac(os.phasor(1,%gr)+0.75);
                 win(ph)=float(ph<dc_g)*0.5*(1.0-cos(2.0*ma.PI*ph/dc_g));
                 src(ph)=sin(2.0*ma.PI*ma.frac(ph*%fc/max(%gr,1.0)));
                 g0=win(ph0)*src(ph0);
                 g1=win(ph1)*src(ph1);
                 g2=win(ph2)*src(ph2);
                 g3=win(ph3)*src(ph3);
               }"
              {:gr gr :gd gd :fc fc})]
    (output :out out)))
