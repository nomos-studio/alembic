; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.193-wavetable-sincmipmap-sample
  "GSOT p.291 — wavetable_sincmipmap_sample.maxpat (Chapter 9: Navigating Waves of Data).

  'Sinc-Interpolated Wavetable with Sample-Rate Mipmap Selection'
  ----------------------------------------------------------------
  Combines the sinc interpolation kernel (ex.192) with automatic mipmap level
  selection: the waveform row is chosen each sample based on :fc so that only
  harmonics below Nyquist are read.

  Mipmap selection (per sample)
  ------------------------------
  The 8-waveform bank (waveforms 0..7, with 1..8 harmonics respectively) from
  ex.187/188 acts as a mipmap pyramid.  At playback frequency :fc, the maximum
  number of harmonics that remain below Nyquist is:

      n_safe = clamp(floor(SR / (2 × :fc)), 1, 8)

  The waveform row index (0-indexed): y_mip = n_safe − 1.

      :fc=220 Hz,  SR=44100: n_safe = min(8, floor(100.2)) = 8 → row 7 (all harmonics)
      :fc=4000 Hz, SR=44100: n_safe = min(8, floor(5.5))   = 5 → row 4 (5 harmonics)
      :fc=8000 Hz, SR=44100: n_safe = min(8, floor(2.75))  = 2 → row 1 (2 harmonics)

  This selection is recomputed at the sample rate — every sample the mipmap level
  tracks the current :fc.  For a fixed or slowly-varying pitch this is equivalent
  to wave-rate selection (ex.194); for rapid pitch sweeps it responds immediately.

  Architecture
  -------------
  1. Auto-select mipmap row y_mip from :fc
  2. Apply 4-point Hann-windowed sinc interpolation within that row
  3. No Y-axis interpolation between adjacent rows — the discrete step in harmonic
     count (from n to n+1) is inaudible compared to the aliasing artefact it prevents

  Sinc X interpolation within a mipmap row
  -----------------------------------------
  For flat table index: flat(k) = y_mip × W + ((i0 + k) & (W−1))

  The bitmask `& (W−1)` wraps the sample offset within the waveform row.
  Adding y_mip × W selects the correct waveform row.  With W=512 (power of 2)
  the bitmask correctly wraps negative offsets (k=−1) via two's-complement:
      i0=0, k=−1: `(0 + (−1)) & 511 = 0xFFFFFFFF & 0x1FF = 511 = W−1`  ✓

  Comparison with ex.187 and ex.192
  ------------------------------------
  ex.187 (wavetables-2D): bilinear X+Y interpolation; :wp manually selects
      the harmonic count; no aliasing prevention.

  ex.192 (sinc-interpolate): 4-pt sinc on a single fixed sine table; no mipmap;
      alias-free only because the source is a pure sine.

  ex.193 (this patch): 4-pt sinc on an automatically selected harmonic row;
      mipmap ensures the source table is already alias-free at :fc; per-sample
      mipmap update.

  Parameters
  ----------
  :fc — oscillator frequency in Hz (20–4000; default 220)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained wavetable oscillator)
    :out — sinc-interpolated output from the auto-selected mipmap row"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! wavetable-sincmipmap-sample
  {:params {:fc {:range [20.0 4000.0] :default 220.0}}}
  (let [fc  (param :fc)
        ph  (faust "os.phasor(1,%fc)" {:fc fc})
        out (faust
              "sinc_out
               with {
                 W=512; H=8; N=W*H;
                 xt=ba.time%W; yt=ba.time/W;
                 ang=2.0*ma.PI*float(xt)/float(W);
                 h(k)=sin(ang*float(k+1))/float(k+1)*float(k<=yt);
                 tbl_init=0.5*(h(0)+h(1)+h(2)+h(3)+h(4)+h(5)+h(6)+h(7));
                 tbl(i)=rdtable(N,tbl_init,max(0,min(N-1,i)));
                 y_mip=int(max(1.0,min(8.0,ma.SR/(2.0*%fc)))-1.0);
                 xfull=%ph*float(W);
                 i0=int(xfull)&(W-1);
                 fr=xfull-float(int(xfull));
                 sinc(x)=sin(ma.PI*max(abs(x),1e-9))/(ma.PI*max(abs(x),1e-9));
                 hann(x)=0.5*(1.0+cos(ma.PI*x/2.0));
                 w(k)=sinc(fr-float(k))*hann(fr-float(k));
                 s(k)=tbl(y_mip*W+((i0+k)&(W-1)));
                 sinc_out=sum(k,4,w(k-1)*s(k-1));
               }"
              {:ph ph :fc fc})]
    (output :out out)))
