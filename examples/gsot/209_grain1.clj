; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.209-grain1
  "GSOT p.326 — grain1.gendsp (Chapter 10: Windows of Time).

  'Single Stochastic Grain DSP Unit (gen~ sub-patcher)'
  -------------------------------------------------------
  grain1.gendsp is the per-grain DSP unit for stochastic granular synthesis —
  the grain-voice equivalent of voice1.gendsp (ex.206).  In Max, `poly~` in
  poly_granulation2.maxpat (ex.210) instantiates N copies of this sub-patcher,
  each maintaining independent DSP state (phasor phase, S&H held value).

  Grain vs. voice
  ---------------
  voice1.gendsp (ex.206) is a SUSTAINED voice: a sine oscillator with ADSR
  envelope held open for as long as the gate is on.  The pitch (:hz) is fixed
  for the duration of the note; the note ends when the gate closes.

  grain1.gendsp is a ONE-SHOT event: a Hann-windowed burst at a RANDOMLY
  CHOSEN frequency, triggered automatically at rate :gr, independent of any
  external gate signal.  The grain repeats indefinitely; each repetition gets
  a new randomly scattered frequency.

  Stochastic frequency per grain
  --------------------------------
  At the start of each grain (detected by phasor wrap: `ph < ph'`), a NEW
  RANDOM FREQUENCY is sampled from `no.noise` (white noise, range ±1) scaled
  by the scatter amount :fs:

      trigger  = ph < ph'             — fires once per grain period at wrap
      fc_grain = S&H(trigger, fc × (1 + fs × noise))   — random freq held per grain

  S&H captures the noise-perturbed frequency at the moment of each trigger and
  holds it constant for the duration of that grain.  Between triggers (during the
  grain and the silence interval) the frequency does not change.

  At :fs=0: all grains use exactly :fc.  Output = deterministic grain train
  (same as one voice of poly_granulation1, ex.208).
  At :fs=0.5: each grain's frequency is independently drawn from
  [0.5×:fc, 1.5×:fc] — a ±50% scatter around the nominal frequency.
  At :fs=1.0: scatter spans [0×:fc, 2×:fc] (clamped to 20Hz minimum).

  The source sine reads at the held frequency:
      src_ph = frac(ph × fc_grain / gr)   — position in formant cycle
      src    = sin(2π × src_ph)

  This is identical to ex.208 except the fixed formant frequency is replaced by
  the per-grain random frequency.

  Grain envelope
  --------------
  Identical to ex.208: Hann window applied over the pulsaret fraction dc_g = gd×gr/1000.
  The envelope ensures smooth amplitude at grain start and end regardless of the
  randomly chosen frequency.

  Parameters
  ----------
  :gr — grain rate in Hz; one grain per 1/:gr seconds (1–100; default 10)
  :gd — grain duration in milliseconds (5–500; default 80)
  :fc — base (centre) frequency in Hz; random scatter is centred here (20–4000; default 440)
  :fs — frequency scatter width; 0=no scatter, 1=±100% around :fc (0–1; default 0.3)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-triggering grain oscillator)
    :out — Hann-windowed grain with randomly scattered frequency, repeating at :gr Hz"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! grain1
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
                 fc_g=ba.sAndH(trig,max(20.0,%fc*(1.0+%fs*no.noise)));
                 win=float(ph<dc_g)*0.5*(1.0-cos(2.0*ma.PI*ph/dc_g));
                 src=sin(2.0*ma.PI*ma.frac(ph*fc_g/max(%gr,1.0)));
                 grain=win*src;
               }"
              {:gr gr :gd gd :fc fc :fs fs})]
    (output :out out)))
