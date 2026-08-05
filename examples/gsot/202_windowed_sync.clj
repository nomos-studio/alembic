; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.202-windowed-sync
  "GSOT pp.309-314 — windowed-sync.maxpat (Chapter 10: Windows of Time).

  'Windowed Hard Sync'
  ---------------------
  Hard sync locks a SLAVE oscillator's phase to a MASTER oscillator.  Each time
  the master phasor completes a cycle (resets from ≈1 to ≈0), the slave phasor
  is forcibly reset to 0 regardless of where it currently is.  The slave's output
  frequency is determined by :fs but its phase is periodically snapped to match
  the master's timing.

  When :fs > :fm (slave faster than master), the slave completes more than one
  cycle per master period.  The last partial cycle is cut short at each master
  reset, creating a characteristic 'hard sync' timbre: additional partials whose
  spacing is determined by the slave/master frequency ratio.

  When :fs = :fm (slave at same speed), sync has no audible effect (the reset
  coincides with the slave's natural cycle boundary).

  Hard sync — the discontinuity problem
  ----------------------------------------
  At the reset moment, the slave phasor jumps discontinuously from some value
  (wherever it happened to be mid-cycle) to 0.  This phase discontinuity is
  equivalent to a rectangular window applied to the slave waveform at the reset
  point — the Gibbs phenomenon causes spectral spreading across all frequencies,
  adding high-frequency aliasing artefacts ('zipper noise') that worsen as the
  slave/master frequency ratio increases or changes.

  Windowed sync — the solution
  ------------------------------
  Instead of letting the discontinuity radiate freely, apply a SHORT WINDOW
  centred on the reset moment.  The window is a raised-cosine (Hann half-window)
  that fades the slave output from 0 to full amplitude over :ww milliseconds
  after each reset:

      wc(t) = 0 at t = t_reset               (counter resets on sync)
             counts up to ww_n samples        (then holds at ww_n)

      win(t) = 0.5 · (1 − cos(π · wc / ww_n))   ∈ [0, 1]
             = 0 immediately after reset          (fully attenuated)
             = 1 after ww_n samples               (full amplitude)

      out(t) = slave(t) × win(t)

  The window tapers the slave output to 0 at the reset point and smoothly
  restores full amplitude over ww_n samples.  The phase discontinuity still
  exists in the PHASOR, but the WINDOW prevents the discontinuity from reaching
  the output — the signal is already attenuated to 0 at the moment of the jump.

  The tradeoff
  ------------
  Widening the window (:ww ↑) attenuates more of the slave signal around each
  sync event, removing more aliasing but also reducing the fundamental amplitude
  and 'sharpness' of the sync timbre.  Narrowing the window (:ww ↓) approaches
  hard sync; widening approaches silence at high slave/master ratios.

  The optimal :ww depends on :fs/:fm: roughly one period of the slave frequency
  (1000 / :fs ms) gives a good balance — wide enough to cover the discontinuity,
  narrow enough to preserve the sync character.

  Reset detection
  ---------------
  Master phasor wrap detected by `ph_m < ph_m'` — the same pattern as ex.194
  (per-cycle mipmap S&H).  Current sample (ph_m ≈ 0) is less than previous
  sample (ph_m' ≈ 1) exactly once per master period.

  Resettable slave phasor
  -----------------------
  Built from Faust's feedback operator `~` and `select2`:

      (select2(reset, (_ + fs/SR), 0.0) ~ _) : ma.frac

  When reset=0: output = prev_output + fs/SR   — normal increment
  When reset=1: output = 0.0                   — hard reset to phase 0

  `ma.frac` keeps the accumulated phase in [0, 1).

  Window counter
  --------------
      (select2(reset, min(_ + 1.0, ww_n), 0.0) ~ _)

  When reset=0: counter = min(prev + 1, ww_n)   — counts up, clamps at ww_n
  When reset=1: counter = 0.0                    — resets on sync

  Once wc reaches ww_n it holds, keeping win=1 until the next reset.

  Slave wavetable
  ---------------
  A 1024-point sine rdtable (os.sinwaveform) with no interpolation.  This is
  the simplest possible slave oscillator, isolating the sync mechanism from
  interpolation and terrain complexity of Chapters 8-9.  The sync technique
  applies equally to any slave waveform or wave terrain reader.

  Parameters
  ----------
  :fm — master oscillator frequency in Hz; sets the sync rate (20–2000; default 220)
  :fs — slave oscillator frequency in Hz; :fs > :fm = classic sync timbre (20–4000; default 330)
  :ww — window width in milliseconds; 0=hard sync click, ≈(1000/:fs)=optimal (0–50; default 2.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained windowed-sync oscillator)
    :out — sine wavetable slave output, windowed at each master sync event"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! windowed-sync
  {:params {:fm {:range [20.0 2000.0] :default 220.0}
            :fs {:range [20.0 4000.0] :default 330.0}
            :ww {:range [0.0 50.0]    :default 2.0}}}
  (let [fm  (param :fm)
        fs  (param :fs)
        ww  (param :ww)
        out (faust
              "sync_out
               with {
                 N=1024;
                 ph_m=os.phasor(1,%{fm});
                 reset=ph_m<ph_m';
                 ph_s=(select2(reset,(_+%{fs}/ma.SR),0.0)~_):ma.frac;
                 slave=rdtable(N,os.sinwaveform(N),int(ph_s*float(N))&(N-1));
                 ww_n=max(1.0,%{ww}*ma.SR/1000.0);
                 wc=(select2(reset,min(_+1.0,ww_n),0.0)~_);
                 win=0.5*(1.0-cos(ma.PI*min(wc/ww_n,1.0)));
                 sync_out=slave*win;
               }"
              {:fm fm :fs fs :ww ww})]
    (output :out out)))
