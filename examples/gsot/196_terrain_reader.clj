; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.196-terrain-reader
  "GSOT p.295 — gen @title terrain-reader (Chapter 9: Navigating Waves of Data).

  'Wave Terrain Synthesis — 2D Terrain Reader'
  ----------------------------------------------
  Wave terrain synthesis (Beauchamp & Polansky, 1979; Roads 1996) uses a 2D
  surface (the 'terrain') and a parametric curve on that surface (the 'orbit').
  The output signal is the terrain height sampled along the orbit path:

      out(t) = T(cx(t), cy(t))

  where T(x,y) is the terrain and (cx(t), cy(t)) is the orbit position at time t.

  Wave terrain vs. wavetable synthesis
  --------------------------------------
  In wavetable synthesis (ex.184–187):
      x = os.phasor(1, fc)   — x always sweeps linearly 0→1
      y = :wp                — y is a slowly-varying waveform selector
      The x trajectory is fixed; only the waveform (y-selection) changes.

  In wave terrain synthesis:
      x = cx(t)              — arbitrary audio-rate orbit x-component
      y = cy(t)              — arbitrary audio-rate orbit y-component
      BOTH x and y follow a closed orbit; their combined trajectory determines
      the spectrum.

  A simple orbit example (Lissajous):
      cx(t) = 0.5 + 0.5 × cos(2π × fx × t)     ∈ [0, 1)
      cy(t) = 0.5 + 0.5 × cos(2π × fy × t + φ) ∈ [0, 1)

  Different fx:fy ratios (1:1, 1:2, 2:3, …) create fundamentally different
  timbral structures from the same terrain because the orbit visits different
  regions of the terrain in different sequences.

  The terrain and the orbit are orthogonal degrees of freedom:
  — Changing the terrain reshapes the spectral envelope.
  — Changing the orbit shape/size/position reshapes the harmonic relationships.

  Both can be modulated independently at audio or control rate.

  The terrain
  -----------
  The terrain in this patch is the 8-waveform harmonic bank from ex.187:
  W=512 (x samples per row), H=8 (y rows), each row having 1..8 harmonics with
  1/n rolloff.  This terrain was designed for wavetable morphing but is equally
  valid as a wave terrain surface.

  At the terrain-reader level, the terrain is just a 2D function — its physical
  interpretation (harmonic waveforms) is incidental.  Any 2D function could
  serve as a terrain: sin(x)×sin(y), a fractal heightmap, a spectrogram, etc.

  This patch
  ----------
  The terrain-reader utility sub-patcher in GSOT takes (x, y) audio-rate inputs
  and returns the terrain height via bilinear interpolation.  Here x comes from
  audio-in (a free audio-rate signal — an orbit x-component, phasor, or noise)
  and y comes from parameter :yp.

  In practice :yp would itself be an audio-rate signal from an orbit generator;
  the parameter constraint reflects the Alembic single-audio-in API.  The full
  wave terrain oscillator (forthcoming) generates its own orbit internally.

  Bilinear read (identical mechanism to ex.185–187):
      xfull = xp × W;  x0 = int(xfull) & (W−1);  x1 = (x0+1) & (W−1)
      yfull = yp × H;  y0 = int(yfull) % H;       y1 = (y0+1) % H
      xf = xfull − floor(xfull);  yf = yfull − floor(yfull)
      out = s00(1−xf)(1−yf) + s10·xf(1−yf) + s01(1−xf)yf + s11·xf·yf

  Parameters
  ----------
  :yp — y terrain coordinate [0,1); 0=first waveform row, 1=last waveform row
        (0–1; default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: x terrain coordinate [0,1) — any audio-rate signal (orbit x-component,
      phasor, noise, …); period 1 = one cycle through the terrain x-axis
    :out — terrain height T(in, :yp) via bilinear interpolation"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! terrain-reader
  {:params {:yp {:range [0.0 1.0] :default 0.5}}}
  (let [xp  (audio-in)
        yp  (param :yp)
        out (faust
              "terrain
               with {
                 W=512; H=8; N=W*H;
                 xt=ba.time%W; yt=ba.time/W;
                 ang=2.0*ma.PI*float(xt)/float(W);
                 h(k)=sin(ang*float(k+1))/float(k+1)*float(k<=yt);
                 tbl_init=0.5*(h(0)+h(1)+h(2)+h(3)+h(4)+h(5)+h(6)+h(7));
                 tbl(i)=rdtable(N,tbl_init,max(0,min(N-1,i)));
                 xfull=%xp*float(W); yfull=%yp*float(H);
                 xr=int(xfull); yr=int(yfull);
                 x0=xr&(W-1); x1=(x0+1)&(W-1);
                 y0=yr%H; y1=(y0+1)%H;
                 xf=xfull-float(xr); yf=yfull-float(yr);
                 s00=tbl(y0*W+x0); s10=tbl(y0*W+x1);
                 s01=tbl(y1*W+x0); s11=tbl(y1*W+x1);
                 terrain=s00*(1.0-xf)*(1.0-yf)+s10*xf*(1.0-yf)
                         +s01*(1.0-xf)*yf+s11*xf*yf;
               }"
              {:xp xp :yp yp})]
    (output :out out)))
