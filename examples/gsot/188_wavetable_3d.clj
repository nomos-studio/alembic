; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.188-wavetable-3d
  "GSOT pp.273-276 — wavetable_3D.maxpat (Chapter 9: Navigating Waves of Data).

  '3D Wavetable Oscillator with Trilinear Interpolation'
  -------------------------------------------------------
  Extends ex.187 (wavetables_2D) from a 2D to a 3D wavetable by adding a
  third navigation axis Z.  The table is now a rectangular cuboid of
  dimensions W × H × D, stored as a flattened 1D array of N = W×H×D entries.

  Three-dimensional layout
  -------------------------
  Flat index: i = z × H × W  +  y × W  +  x

    x-axis (0..W−1): sample position within waveform; driven by phasor at :fc.
    y-axis (0..H−1): waveform index, Y-dimension; controlled by :wp.
    z-axis (0..D−1): waveform index, Z-dimension; controlled by :zp.

  Table dimensions: W=256, H=4, D=4 → N=4096 entries total.

  Waveform bank design (3D)
  --------------------------
  Waveform at grid position (y, z):

      wt[z][y][x] = 0.5 × Σ_{k=1}^{y+1}  sin(2π × k × x/W + z × π/4) / k

  Y-axis (harmonic count): waveform row y has y+1 harmonics with 1/n rolloff.
    y=0: 1 harmonic (pure sine)
    y=3: 4 harmonics (band-limited sawtooth approximation)

  Z-axis (phase offset): waveform column z shifts all harmonics by z × π/4.
    z=0: standard phase (same as ex.187 waveforms, with fewer harmonics)
    z=1: π/4 phase shift on all harmonics
    z=2: π/2 phase shift (cosine-phase harmonics)
    z=3: 3π/4 phase shift

  Phase offsets change the waveform shape subtly (time-shifted versions of the
  same harmonic content), which affects perceived attack transient character
  when Z is swept dynamically.

  In Faust, with flat index i = ba.time during rdtable init:
      xt = i % W            — sample position
      yt = (i / W) % H      — harmonic count axis
      zt = i / (W × H)      — phase offset axis
      ph_off = zt × π/4     — phase shift for this Z layer

  Trilinear interpolation
  -----------------------
  8 surrounding grid points: all combinations of (x0,x1) × (y0,y1) × (z0,z1).
  Three fractional parts (xf, yf, zf) ∈ [0,1).

  The bilinear formula (ex.186) generalises to trilinear by factoring in the
  z fractional weight (1−zf) or zf for the two z-slices:

      out = s000 × (1−xf)(1−yf)(1−zf) + s100 × xf(1−yf)(1−zf)
          + s010 × (1−xf) yf (1−zf)   + s110 × xf  yf (1−zf)
          + s001 × (1−xf)(1−yf) zf    + s101 × xf(1−yf) zf
          + s011 × (1−xf) yf  zf      + s111 × xf  yf  zf

  Each corner is weighted by the VOLUME of the opposite sub-cuboid.  The 8
  weights sum to 1 for all (xf, yf, zf) ∈ [0,1)³.

  Each of the 8 reads calls tbl(i), which resolves to rdtable(N, tbl_init, i).
  Faust's CSE allocates one table and emits 8 read operations into it.

  Compared to ex.187 (2D)
  ------------------------
  ex.187: 4 table reads, 2 fractional coords, W×H = 4096 entries
  ex.188: 8 table reads, 3 fractional coords, W×H×D = 4096 entries (same size)

  The added dimension here (Z = phase offset) can also be used for:
  — A second independent harmonic parameter (e.g., even vs. odd harmonic mix)
  — Spectral tilt (brightness axis)
  — Any parameter that defines a continuum of related waveforms

  4ms SWN connection
  -------------------
  The SWN (Spherical Wavetable Navigator) uses a 3D waveform bank with
  latitude and longitude mapping to two of the three axes, and 6 oscillators
  positioned at different points on a sphere surface.  The sphere position
  determines (x, y, z) → waveform bank coordinates for all 6 voices.
  Ex.188 is the single-voice building block; the SWN seed extends this to
  6-voice `par(i,6,...)` with per-voice spherical offsets.

  Parameters
  ----------
  :fc — oscillator frequency in Hz (20–2000; default 220)
  :wp — wavetable Y position [0,1); 0=pure sine (y=0), 1=4-harmonic saw (y=3)
        (0–1; default 0.0)
  :zp — wavetable Z position [0,1); sweeps through phase offsets 0, π/4, π/2, 3π/4
        (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained 3D wavetable oscillator)
    :out — trilinearly interpolated output from the 4×4 harmonic/phase bank"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! wavetable-3d
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :wp {:range [0.0 1.0]     :default 0.0}
            :zp {:range [0.0 1.0]     :default 0.0}}}
  (let [fc  (param :fc)
        wp  (param :wp)
        zp  (param :zp)
        ph  (faust "os.phasor(1,%fc)" {:fc fc})
        out (faust
              "trilinear
               with {
                 W=256; H=4; D=4; N=W*H*D;
                 xt=ba.time%W; yt=(ba.time/W)%H; zt=ba.time/(W*H);
                 ang=2.0*ma.PI*float(xt)/float(W);
                 ph_off=float(zt)*ma.PI/4.0;
                 h(k)=sin(ang*float(k+1)+ph_off)/float(k+1)*float(k<=yt);
                 tbl_init=0.5*(h(0)+h(1)+h(2)+h(3));
                 tbl(i)=rdtable(N,tbl_init,max(0,min(N-1,i)));
                 xfull=%ph*float(W); yfull=%wp*float(H); zfull=%zp*float(D);
                 xr=int(xfull); yr=int(yfull); zr=int(zfull);
                 x0=xr&(W-1); x1=(x0+1)&(W-1);
                 y0=yr%H; y1=(y0+1)%H;
                 z0=zr%D; z1=(z0+1)%D;
                 xf=xfull-float(xr); yf=yfull-float(yr); zf=zfull-float(zr);
                 s000=tbl(z0*H*W+y0*W+x0); s100=tbl(z0*H*W+y0*W+x1);
                 s010=tbl(z0*H*W+y1*W+x0); s110=tbl(z0*H*W+y1*W+x1);
                 s001=tbl(z1*H*W+y0*W+x0); s101=tbl(z1*H*W+y0*W+x1);
                 s011=tbl(z1*H*W+y1*W+x0); s111=tbl(z1*H*W+y1*W+x1);
                 trilinear=s000*(1.0-xf)*(1.0-yf)*(1.0-zf)+s100*xf*(1.0-yf)*(1.0-zf)
                           +s010*(1.0-xf)*yf*(1.0-zf)+s110*xf*yf*(1.0-zf)
                           +s001*(1.0-xf)*(1.0-yf)*zf+s101*xf*(1.0-yf)*zf
                           +s011*(1.0-xf)*yf*zf+s111*xf*yf*zf;
               }"
              {:ph ph :wp wp :zp zp})]
    (output :out out)))
