; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.215-granola-buffer
  "GSOT p.343 — granola_buffer.maxpat (Chapter 10: Windows of Time).

  'Four-Voice Buffer-Based Granular Synthesizer with Position Scatter'
  ---------------------------------------------------------------------
  granola_buffer.maxpat is the first 'whole grains at once' patch: grains are
  read from a STORED BUFFER rather than synthesised on the fly.  This moves from
  oscillator-based granulation (ex.208-214) to SAMPLE-BASED granulation, where
  the source material is a recorded audio buffer and the grain parameters control
  which segment of the buffer is read for each grain.

  '@boundmode wrap' and 'overdubbing the future' (pp.333-335)
  ------------------------------------------------------------
  In Max gen~, `@boundmode wrap` on a `data` or `buffer~` object causes
  out-of-range read indices to WRAP to the other end of the buffer.  This is
  equivalent to treating the buffer as a circular ring.

  'Overdubbing the future' refers to a specific consequence of this wrapping: if
  you set the read position AHEAD of the current write position in a live-input
  circular buffer, the reader wraps around and reads audio that was written in
  the previous cycle — audio from the 'future' relative to the write head's
  current position in the ring.  This creates a natural looping/smearing effect
  at no extra cost.

  In Faust, `ma.frac` applied to the normalised read position implements
  `@boundmode wrap` naturally: any position outside [0,1) wraps to [0,1), and
  the bitmask `& (N-1)` for power-of-2 tables achieves the same for integer
  indices.

  'Whole grains at once' — the buffer read model (p.336)
  -------------------------------------------------------
  In oscillator granulation (ex.208-210), the grain source was:

      src(ph) = sin(2π × frac(ph × fc / gr))

  which computes one grain value per sample.  In BUFFER granulation, the same
  formula gives the BUFFER READ POSITION:

      buf_pos(ph, ps) = frac(ph × :fc / gr + ps) × N

  where:
    ph   — master phasor (0→1 per grain period), acts as time within grain
    :fc  — playback rate relative to :gr; fc/gr = fraction of buffer read per grain
    ps   — per-grain random start position (0..1), S&H'd at each trigger
    N    — buffer size in samples

  The table lookup `rdtable(N, buf, int(buf_pos))` reads the buffer at this
  position, giving one sample of the grain's source audio.  This is otherwise
  identical to the oscillator grain: the Hann window and duty cycle work the same.

  Position scatter via per-grain S&H
  ------------------------------------
  Per-grain start position is randomised exactly as fc_g in ex.209-210:

      trig = ph < ph'          — grain-start trigger
      ps   = S&H(trig, ma.frac(:sp + :ps × noise))   — held random start per grain

  The position wraps via `ma.frac` so :sp=0.9, :ps=0.2 will correctly probe
  both near 0.9 and near 0.1 (the wrapped region), matching `@boundmode wrap`.

  At :ps=0.0: all grains start at :sp — reads the same buffer segment every grain.
  At :ps=0.5: start positions scatter ±0.5 of the buffer length around :sp.
  At :ps=1.0: start positions are uniformly distributed across the entire buffer.

  Buffer proxy
  ------------
  The physical gen~/Max patch reads from a `buffer~` or `data` object populated
  with recorded audio.  In Faust there is no mechanism to load an external audio
  file at compile time without a `soundfile` primitive and a host that provides
  the file.  This translation uses a SINE-BANK WAVETABLE (N=65536, single-cycle
  sine) as a structural proxy for the buffer.  The DSP structure — position
  scatter, grain envelope, duty cycle, 4-voice stagger — is identical.  To use
  with a real audio file, replace `os.sinwaveform(N)` with `soundfile(...)` and
  adapt the index arithmetic to the file's sample count.

  4-voice stagger
  ---------------
  Identical stagger to ex.208-210: voices at phase offsets 0/0.25/0.5/0.75.
  All four voices share a single `no.noise` source; per-voice independent
  positions are obtained from the staggered S&H triggers.

  Parameters
  ----------
  :gr — grain rate per voice in Hz; 4×:gr = total density (1–100; default 10)
  :gd — grain duration in milliseconds; dc_g = :gd×:gr/1000 (5–500; default 80)
  :fc — playback rate relative to :gr; fc/gr = buffer fraction read per grain (1–200; default 40)
  :sp — start position in buffer, normalised [0,1]; centre of the scatter (0–1; default 0.25)
  :ps — position scatter width, normalised; 0=fixed at :sp, 1=uniform over full buffer (0–1; default 0.3)

  Audio inputs / Outputs
  ----------------------
  (no audio input — reads from internal sine-bank buffer proxy)
    :out — sum of 4 staggered buffer-read grain voices (0.25×sum)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! granola-buffer
  {:params {:gr {:range [1.0 100.0]  :default 10.0}
            :gd {:range [5.0 500.0]  :default 80.0}
            :fc {:range [1.0 200.0]  :default 40.0}
            :sp {:range [0.0 1.0]    :default 0.25}
            :ps {:range [0.0 1.0]    :default 0.3}}}
  (let [gr  (param :gr)
        gd  (param :gd)
        fc  (param :fc)
        sp  (param :sp)
        ps  (param :ps)
        out (faust
              "0.25*(g0+g1+g2+g3)
               with {
                 N=65536;
                 rnd=no.noise;
                 dc_g=max(0.001,%gd*%gr/1000.0);
                 ph0=ma.frac(os.phasor(1,%gr)+0.0);
                 ph1=ma.frac(os.phasor(1,%gr)+0.25);
                 ph2=ma.frac(os.phasor(1,%gr)+0.5);
                 ph3=ma.frac(os.phasor(1,%gr)+0.75);
                 tr0=ph0<ph0'; tr1=ph1<ph1'; tr2=ph2<ph2'; tr3=ph3<ph3';
                 ps0=ba.sAndH(tr0,ma.frac(%sp+%ps*rnd));
                 ps1=ba.sAndH(tr1,ma.frac(%sp+%ps*rnd));
                 ps2=ba.sAndH(tr2,ma.frac(%sp+%ps*rnd));
                 ps3=ba.sAndH(tr3,ma.frac(%sp+%ps*rnd));
                 win(ph)=float(ph<dc_g)*0.5*(1.0-cos(2.0*ma.PI*ph/dc_g));
                 src(ph,ps)=rdtable(N,os.sinwaveform(N),int(ma.frac(ph*%fc/max(%gr,1.0)+ps)*float(N))&(N-1));
                 g0=win(ph0)*src(ph0,ps0);
                 g1=win(ph1)*src(ph1,ps1);
                 g2=win(ph2)*src(ph2,ps2);
                 g3=win(ph3)*src(ph3,ps3);
               }"
              {:gr gr :gd gd :fc fc :sp sp :ps ps})]
    (output :out out)))
