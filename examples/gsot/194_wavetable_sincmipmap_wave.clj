; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.194-wavetable-sincmipmap-wave
  "GSOT p.292 — wavetable_sincmipmap_wave.maxpat (Chapter 9: Navigating Waves of Data).

  'Sinc-Interpolated Wavetable with Wave-Rate Mipmap Selection'
  --------------------------------------------------------------
  Identical in architecture to ex.193 (sincmipmap_sample) except that the mipmap
  level is sampled ONCE per oscillator cycle and held constant for the duration of
  that cycle.  This eliminates any potential artefact from the waveform row changing
  mid-cycle.

  Why freeze the mipmap at wave boundaries?
  ------------------------------------------
  In ex.193 the mipmap level y_mip recomputes every sample.  For a steady-state
  pitch this makes no difference; the level is constant anyway.  But during rapid
  pitch modulation (portamento, vibrato, FM) the level can change mid-cycle, causing
  a visible discontinuity in the waveform: part of the cycle is read from row k,
  the rest from row k+1.  The harmonic content changes abruptly within a single
  waveform period.

  For most audio rates (fc > 20 Hz) this change occurs faster than 50 ms and may
  be inaudible, but at very low frequencies (sub-bass, LFO-rate) it can create
  clicks or timbral glitches.

  Sample-and-hold at phasor wrap
  --------------------------------
  The oscillator phasor ph sweeps 0→1 once per cycle.  A new cycle starts when ph
  wraps around, detectable as the moment ph < ph' (current < previous):

      reset = ph < ph'

  At the wrap point (reset=1), the S&H captures the current mipmap level and holds
  it until the next wrap.  Between wraps (reset=0), the output is the held value:

      y_held = ba.sAndH(reset, y_mip_raw)

  where y_mip_raw = max(1, min(8, SR/(2×fc))) − 1 is the per-sample level.

  `ba.sAndH(trig, x)` from basics.lib: captures x when trig=1, holds the captured
  value when trig=0.

  The held mipmap level is frozen for an entire oscillator period.  At the start
  of the next cycle, if :fc has changed enough to cross a mipmap boundary, the new
  level is captured.  This gives per-cycle (not per-sample) mipmap transitions.

  Phasor wrap detection
  ----------------------
  `ph < ph'` uses the 1-sample delay operator `'`.  The phasor jumps from ≈1.0 to
  ≈0.0 at each cycle boundary.  At the wrap:

      ph ≈ 0.0  (first sample of new cycle)
      ph' ≈ 1.0 (last sample of previous cycle)
      ph < ph'  → true (reset=1) ✓

  One sample after the wrap:
      ph ≈ dp (small positive increment)
      ph' ≈ 0.0
      ph < ph' → false (reset=0) ✓

  This cleanly detects exactly one sample per cycle at the wrap point.

  Comparison with ex.193
  -----------------------
  ex.193 (sincmipmap_sample): y_mip = per-sample computation, may change mid-cycle.
  ex.194 (sincmipmap_wave):   y_held = per-cycle S&H, constant within each cycle.

  For steady pitch: identical output.
  For modulated pitch: ex.194 transitions only at cycle boundaries; ex.193 may
  create intra-cycle harmonic changes.

  Parameters
  ----------
  :fc — oscillator frequency in Hz (20–4000; default 220)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained wavetable oscillator)
    :out — sinc-interpolated output; mipmap row frozen per oscillator cycle"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! wavetable-sincmipmap-wave
  {:params {:fc {:range [20.0 4000.0] :default 220.0}}}
  (let [fc  (param :fc)
        ph  (faust "os.phasor(1,%{fc})" {:fc fc})
        out (faust
              "sinc_out
               with {
                 W=512; H=8; N=W*H;
                 xt=ba.time%W; yt=ba.time/W;
                 ang=2.0*ma.PI*float(xt)/float(W);
                 h(k)=sin(ang*float(k+1))/float(k+1)*float(k<=yt);
                 tbl_init=0.5*(h(0)+h(1)+h(2)+h(3)+h(4)+h(5)+h(6)+h(7));
                 tbl(i)=rdtable(N,tbl_init,max(0,min(N-1,i)));
                 reset=%{ph}<%{ph}';
                 y_raw=max(1.0,min(8.0,ma.SR/(2.0*%{fc})))-1.0;
                 y_held=int(ba.sAndH(reset,y_raw));
                 xfull=%{ph}*float(W);
                 i0=int(xfull)&(W-1);
                 fr=xfull-float(int(xfull));
                 sinc(x)=sin(ma.PI*max(abs(x),1e-9))/(ma.PI*max(abs(x),1e-9));
                 hann(x)=0.5*(1.0+cos(ma.PI*x/2.0));
                 w(k)=sinc(fr-float(k))*hann(fr-float(k));
                 s(k)=tbl(y_held*W+((i0+k)&(W-1)));
                 sinc_out=sum(k,4,w(k-1)*s(k-1));
               }"
              {:ph ph :fc fc})]
    (output :out out)))
