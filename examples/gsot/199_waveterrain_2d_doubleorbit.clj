; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.199-waveterrain-2d-doubleorbit
  "GSOT pp.300-301 — waveterrain_2D_doubleorbit.maxpat (Chapter 9: Navigating Waves of Data).

  'Wave Terrain — Compound (Double) Orbit'
  -----------------------------------------
  A compound orbit is the sum of two circular Lissajous orbits at different
  frequencies.  This creates epicycloid, hypocycloid, and spirograph-style
  trajectories over the terrain surface, depending on the ratio of the two
  orbit frequencies and their radii.

  The orbit in ex.197 and ex.198 was a SINGLE Lissajous:

      cx(t) = 0.5 + r·cos(2π·fc·t)
      cy(t) = 0.5 + r·sin(2π·fc·t)

  The DOUBLE orbit adds a second circular component at a different frequency:

      cx(t) = 0.5 + r1·cos(2π·fc·t)   + r2·cos(2π·fc·:rm·t + 2π·:ph)
      cy(t) = 0.5 + r1·sin(2π·fc·t)   + r2·sin(2π·fc·:rm·t + 2π·:ph)

  The first component (radius :r1, frequency :fc) drives the primary orbital
  motion.  The second component (radius :r2, frequency :fc×:rm) orbits around
  the first component's current position — a planet orbiting a sun.

  Orbit types by frequency ratio :rm
  ------------------------------------
  :rm = 1   — The two components rotate at the same rate.  If :ph = 0, the
               orbits add in-phase: cx = 0.5 + (r1+r2)·cos(2π·fc·t), a larger
               circle.  As :ph varies the combined orbit sweeps from expanded
               circle → ellipse → smaller circle.

  :rm = 2   — Limaçon orbit.  The inner component rotates twice per outer
               rotation.  Creates cardioid-like (heart-shaped) curves when
               :r1 ≈ :r2, kidney-bean-shaped curves when :r1 > :r2.

  :rm = 3   — Three-petal rose.  Closes after one primary orbit period.

  :rm = 1.5 — Orbit closes after 2 primary periods.  Knot-like figure.

  :rm = √2  — Never closes.  The trajectory fills the terrain densely over
               time (Lissajous-like quasi-periodicity in two dimensions).

  Effect on the terrain output spectrum
  ---------------------------------------
  Because the orbit visits terrain positions in a more complex sequence than
  a single Lissajous, the compound orbit generates sidebands around the
  fundamental.  At :rm = 2 the spectrum has components at fc, 2·fc, 3·fc, …
  shaped by which terrain regions the compound path crosses and how long it
  dwells near each region.  This is wave terrain's equivalent of FM synthesis:
  the terrain shape sets the carrier, the orbit modulates the spectral character.

  When :r2 → 0 (small :r2), the output converges to the single-orbit (ex.197)
  result.  As :r2 → :r1, the compound orbit develops a pronounced secondary
  lobe and the output becomes richer in sidebands.

  Clamping / tiling
  -----------------
  The compound orbit can extend outside [0,1) when r1 + r2 > 0.5.  The terrain
  reader uses bitmask wrap in x (W=512, power-of-2) and modulo wrap in y (H=8),
  so values outside [0,1) tile into the next terrain period — the terrain is
  infinite in all directions.  This is intentional: different 'copies' of the
  terrain contribute to the sum when the orbit escapes the primary tile.

  Terrain
  -------
  8-waveform harmonic bank (W=512, H=8): same terrain as ex.187/196/197.
  Waveform k has k+1 harmonics with 1/n rolloff.  Bilinear read throughout.

  Parameters
  ----------
  :fc — primary orbit frequency in Hz (20–2000; default 220)
  :rm — secondary orbit frequency ratio; :rm=2 → limaçon (0.1–8.0; default 2.0)
  :r1 — primary orbit radius [0,0.5] → stays in [0,1) at r1=0.5 (default 0.4)
  :r2 — secondary orbit radius; r1+r2 ≤ 0.5 to stay in one terrain tile (default 0.1)
  :ph — secondary orbit phase offset [0,1); 0=in-phase, 0.25=quadrature (default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained wave terrain oscillator)
    :out — terrain height sampled along the compound orbit"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! waveterrain-2d-doubleorbit
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :rm {:range [0.1 8.0]     :default 2.0}
            :r1 {:range [0.0 0.5]     :default 0.4}
            :r2 {:range [0.0 0.5]     :default 0.1}
            :ph {:range [0.0 1.0]     :default 0.0}}}
  (let [fc  (param :fc)
        rm  (param :rm)
        r1  (param :r1)
        r2  (param :r2)
        ph  (param :ph)
        out (faust
              "terrain
               with {
                 W=512; H=8; N=W*H;
                 xt=ba.time%W; yt=ba.time/W;
                 ang=2.0*ma.PI*float(xt)/float(W);
                 h(k)=sin(ang*float(k+1))/float(k+1)*float(k<=yt);
                 tbl_init=0.5*(h(0)+h(1)+h(2)+h(3)+h(4)+h(5)+h(6)+h(7));
                 tbl(i)=rdtable(N,tbl_init,max(0,min(N-1,i)));
                 ph1=os.phasor(1,%fc);
                 ph2=os.phasor(1,%fc*%rm);
                 cx=0.5+%r1*cos(2.0*ma.PI*ph1)
                       +%r2*cos(2.0*ma.PI*ph2+2.0*ma.PI*%ph);
                 cy=0.5+%r1*sin(2.0*ma.PI*ph1)
                       +%r2*sin(2.0*ma.PI*ph2+2.0*ma.PI*%ph);
                 xfull=cx*float(W); yfull=cy*float(H);
                 xr=int(xfull); yr=int(yfull);
                 x0=xr&(W-1); x1=(x0+1)&(W-1);
                 y0=yr%H; y1=(y0+1)%H;
                 xf=xfull-float(xr); yf=yfull-float(yr);
                 s00=tbl(y0*W+x0); s10=tbl(y0*W+x1);
                 s01=tbl(y1*W+x0); s11=tbl(y1*W+x1);
                 terrain=s00*(1.0-xf)*(1.0-yf)+s10*xf*(1.0-yf)
                         +s01*(1.0-xf)*yf+s11*xf*yf;
               }"
              {:fc fc :rm rm :r1 r1 :r2 r2 :ph ph})]
    (output :out out)))
