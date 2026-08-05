; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.195-sinc-interpolate-wave
  "GSOT p.293 — sinc-interpolate (complete oscillator) (Chapter 9: Navigating Waves of Data).

  'Sinc X + Linear Y — Complete Wavetable Oscillator'
  -----------------------------------------------------
  The complete musical instrument combining:
    — 4-point Hann-windowed sinc interpolation on the X axis (within a waveform)
    — Linear interpolation on the Y axis (:wp waveform morphing)
    — The 8-waveform harmonic bank from ex.187

  This is the gen~ sinc-interpolate sub-patcher from ex.192 deployed as a
  full-quality version of the 1D wavetable oscillator (ex.184) and 2D wavetable
  oscillator (ex.187).

  Two-axis interpolation strategy
  ---------------------------------
  X axis (sample position within waveform):
    4-point sinc — high-quality reconstruction of the waveform's spectral content.
    Reduces high-frequency interpolation artefacts compared to linear (ex.184/187).
    Cost: 4 multiply-adds + 4 sinc/hann evaluations per sample.

  Y axis (waveform morphing via :wp):
    Linear interpolation between adjacent waveform rows.
    For smooth timbral sweeps, linear Y is sufficient — the waveform shapes
    in adjacent rows are similar (they differ by one added harmonic) so linear
    blending gives a perceptually smooth morph.

  Architecture: sinc_y(row) + Y-linear blend
  -------------------------------------------
  For each of the two Y-adjacent waveform rows (y0 and y1):

      sinc_y(yr) = Σ_{k=−1}^{2}  w(k) × table[yr × W + ((i0 + k) & (W−1))]

  Then linear Y blend:
      out = sinc_y(y0) × (1 − yf) + sinc_y(y1) × yf

  Total table reads: 4 (sinc taps) × 2 (Y rows) = 8 reads per sample.
  Compare with ex.187 (bilinear): 4 reads; ex.188 (trilinear): 8 reads.

  The 8-read count is the same as ex.188 (trilinear) but the QUALITY
  is higher in the X dimension (sinc vs. linear) and comparable in Y.

  Comparison of ex.184 / ex.187 / ex.195
  ----------------------------------------
  ex.184 (wavetable-1D): 2 reads, linear X, fixed sine only.
  ex.187 (wavetables-2D): 4 reads, bilinear X+Y, 8-waveform bank, :wp morphing.
  ex.195 (this):          8 reads, sinc X + linear Y, 8-waveform bank, :wp morphing.

  The quality improvement of ex.195 over ex.187 is most audible at high playback
  frequencies (:fc near SR/2) or with waveforms rich in high-frequency partials.
  At low frequencies and with smooth waveforms, the difference is negligible.

  Note: ex.195 does NOT include automatic mipmap level selection (see ex.193/194).
  At very high :fc values, the waveform bank may contain harmonics above Nyquist
  regardless of sinc interpolation quality.  The correct production architecture
  is ex.193 or ex.194 (mipmap) rather than this patch.  Ex.195 demonstrates the
  sinc+linear interpolation algorithm in isolation without the mipmap layer.

  Parameters
  ----------
  :fc — oscillator frequency in Hz (20–2000; default 220)
  :wp — wavetable Y position [0,1); 0=sine (1 harmonic), 1=8-harmonic saw
        (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained wavetable oscillator)
    :out — sinc-X + linear-Y interpolated output; 8 table reads per sample"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! sinc-interpolate-wave
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :wp {:range [0.0 1.0]     :default 0.0}}}
  (let [fc  (param :fc)
        wp  (param :wp)
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
                 xfull=%{ph}*float(W);
                 i0=int(xfull)&(W-1);
                 fr=xfull-float(int(xfull));
                 sinc(x)=sin(ma.PI*max(abs(x),1e-9))/(ma.PI*max(abs(x),1e-9));
                 hann(x)=0.5*(1.0+cos(ma.PI*x/2.0));
                 w(k)=sinc(fr-float(k))*hann(fr-float(k));
                 yfull=%{wp}*float(H);
                 yi=int(yfull);
                 y0=yi%H; y1=(y0+1)%H;
                 yf=yfull-float(yi);
                 sinc_y(yr)=sum(k,4,w(k-1)*tbl(yr*W+((i0+k)&(W-1))));
                 sinc_out=sinc_y(y0)*(1.0-yf)+sinc_y(y1)*yf;
               }"
              {:ph ph :wp wp})]
    (output :out out)))
