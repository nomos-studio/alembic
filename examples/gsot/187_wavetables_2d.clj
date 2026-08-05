; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.187-wavetables-2d
  "GSOT p.273 — wavetables_2D.maxpat (Chapter 9: Navigating Waves of Data).

  '2D Wavetable Oscillator with Bilinear Interpolation'
  -------------------------------------------------------
  Combines the 1D wavetable oscillator (ex.184), bilinear index computation
  (ex.185), and the bilinear interpolation formula (ex.186) into a complete
  2D wavetable synthesizer.

  Two-dimensional wavetable layout
  ---------------------------------
  The table is W×H samples stored as a flattened 1D array of N = W×H entries.
  Here W = 512 (samples per waveform, power of 2) and H = 8 (waveforms).

    x-axis (0..W−1): sample position within one waveform cycle, driven by the
      oscillator phasor at :fc Hz.  Wrapped via bitmask: `i & (W−1)`.

    y-axis (0..H−1): waveform index, controlled by the :wp parameter.
      :wp = 0.0 → waveform 0 (pure sine, 1 harmonic)
      :wp = 1/7 → waveform 1 (2 harmonics: 1/1 + 1/2)
      :wp = k/7 → waveform k (k+1 harmonics, 1/n rolloff)
      :wp = 1.0 → waveform 7 (8 harmonics)

  Waveform bank design
  ---------------------
  Waveform y (y = 0..7) contains y+1 harmonics of the fundamental:

      wt[y][x] = 0.5 × Σ_{k=1}^{y+1}  sin(2π × k × x / W) / k

  The 0.5 gain normalises the maximum amplitude: at y=7, the 8-harmonic sum
  peaks at about ±1.4 unreduced; ×0.5 keeps output within ±0.7 for all rows.

  Waveform 0 is a pure sine (single harmonic, ±0.5 amplitude).
  Waveform 7 is a band-limited partial sawtooth (8 harmonics, 1/n rolloff,
  ±0.7 peak amplitude).

  This mirrors the design of the PPG Wave 2.x (1981): bank of 64 waveforms
  each with increasing harmonic density, swept by a single wavetable-position
  parameter.  The :wp control here gives a smooth 8-waveform version of
  that bank; see the PPG Wave-alike seed (seeds/2026/07/20260724-ppg-wave-alike-
  wavetable.org) for the 64-waveform extension plan.

  Table initialisation in Faust (rdtable init and ba.time)
  ----------------------------------------------------------
  `rdtable(N, init_signal, read_idx)` evaluates `init_signal` for integer
  indices 0..N-1 at program startup.  During initialisation, `ba.time` (from
  basics.lib) counts from 0 upward — it is the current table index inside the
  init expression.

      xt = ba.time % W    — sample index within waveform (0..W−1)
      yt = ba.time / W    — waveform index (0..H−1, integer division)
      ang = 2π × xt / W  — phase angle for this sample

  The harmonic helper h(k) adds harmonic k+1 to the sum IF k ≤ yt:

      h(k) = sin(ang × (k+1)) / (k+1)  ×  float(k ≤ yt)

  `float(k ≤ yt)` converts the Faust integer comparison result to 0.0/1.0,
  enabling/disabling each harmonic according to the waveform row.  The Faust
  compiler expands `h(0)+h(1)+…+h(7)` with k as a literal integer in each call.

  Bilinear interpolation
  ----------------------
  At runtime, the phasor gives a fractional x-position in [0, W):

      xfull = phasor(1, :fc) × W     — fractional sample position
      xi    = int(xfull)               — integer floor
      x0    = xi & (W−1)              — bitmask wrap (faster than modulo)
      x1    = (x0 + 1) & (W−1)        — next sample index, wrapped
      xf    = xfull − float(xi)       — fractional part, [0, 1)

  The wavetable position :wp gives a fractional y-position in [0, H):

      yfull = :wp × H                 — fractional waveform position
      yi    = int(yfull)              — integer floor
      y0    = yi % H                  — waveform index (clamped/wrapped)
      y1    = (y0 + 1) % H            — next waveform index
      yf    = yfull − float(yi)       — fractional part

  Four table reads and bilinear blend:

      s00 = tbl[y0 × W + x0]
      s10 = tbl[y0 × W + x1]
      s01 = tbl[y1 × W + x0]
      s11 = tbl[y1 × W + x1]

      out = s00 × (1−xf)(1−yf) + s10 × xf(1−yf)
          + s01 × (1−xf)yf    + s11 × xf × yf

  At :wp = k/7 (exactly on waveform boundary): y0 = k, yf = 0, so the output
  is the linearly-interpolated read of waveform k exactly — the y-axis
  interpolation contributes nothing.  Between boundaries, the output is a
  smooth blend of adjacent waveforms.

  Table sharing in Faust
  ----------------------
  `tbl(i)` is defined once in the `with` block as a function of the read index
  `i`.  All four calls to `tbl(...)` share the same rdtable (N=4096, same
  `tbl_init` signal).  The Faust compiler allocates one table and emits four
  read operations into it, equivalent to four sequential array accesses in the
  generated C++ code.

  Aliasing consideration
  ----------------------
  The table is computed at fixed harmonic counts (1..8) and does not change
  with playback pitch.  At high :fc, the stored harmonics alias if they exceed
  Nyquist.  Waveform 7 at :fc = 500 Hz, SR = 44100 Hz: harmonic 8 = 4000 Hz
  (within Nyquist at 22050 Hz — fine).  Waveform 7 at :fc = 3000 Hz: harmonic
  8 = 24000 Hz > Nyquist (aliases).  Band-limiting (different waveform bank per
  octave) is not implemented here; a PPG-alike extension would add it.

  Parameters
  ----------
  :fc — oscillator frequency in Hz (20–2000; default 220)
  :wp — wavetable position [0, 1); 0=pure sine (1 harmonic),
        1=8-harmonic band-limited sawtooth (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained 2D wavetable oscillator)
    :out — bilinearly interpolated output from the 8-waveform harmonic bank"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! wavetables-2d
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :wp {:range [0.0 1.0]     :default 0.0}}}
  (let [fc  (param :fc)
        wp  (param :wp)
        ph  (faust "os.phasor(1,%{fc})" {:fc fc})
        out (faust
              "bilinear
               with {
                 W=512; H=8; N=W*H;
                 xt=ba.time%W; yt=ba.time/W;
                 ang=2.0*ma.PI*float(xt)/float(W);
                 h(k)=sin(ang*float(k+1))/float(k+1)*float(k<=yt);
                 tbl_init=0.5*(h(0)+h(1)+h(2)+h(3)+h(4)+h(5)+h(6)+h(7));
                 tbl(i)=rdtable(N,tbl_init,max(0,min(N-1,i)));
                 xfull=%{ph}*float(W); yfull=%{wp}*float(H);
                 xi=int(xfull); yi=int(yfull);
                 x0=xi&(W-1); x1=(x0+1)&(W-1);
                 y0=yi%H; y1=(y0+1)%H;
                 xf=xfull-float(xi); yf=yfull-float(yi);
                 s00=tbl(y0*W+x0); s10=tbl(y0*W+x1);
                 s01=tbl(y1*W+x0); s11=tbl(y1*W+x1);
                 bilinear=s00*(1.0-xf)*(1.0-yf)+s10*xf*(1.0-yf)+s01*(1.0-xf)*yf+s11*xf*yf;
               }"
              {:ph ph :wp wp})]
    (output :out out)))
