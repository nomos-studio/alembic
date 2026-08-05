; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.200-waveterrain-2d-carom
  "GSOT pp.301-302 — waveterrain_2D_carom.maxpat (Chapter 9: Navigating Waves of Data).

  'Wave Terrain — Carom (Billiard-Ball) Orbit'
  ----------------------------------------------
  A carom orbit is a billiard-ball-style reflecting trajectory over the terrain
  surface.  Instead of a smooth sinusoidal Lissajous path (ex.197), the orbit
  moves at constant velocity and REFLECTS off the terrain boundary [0,1) when
  it reaches an edge — like a cue ball bouncing off cushions.

  'Carom' is the billiards term for a ball that glances off one cushion onto
  another without pocketing.  In wave terrain synthesis the 'cushions' are the
  four edges of the [0,1)² terrain tile.

  Carom orbit generator
  ---------------------
  The reflecting trajectory is equivalent to a triangle wave:

      ph_x = phasor(1, fc)                    — ramps 0→1 at rate fc
      ph_y = frac(phasor(1, fc×:rt) + :ph)    — same but phase-shifted and ratio-scaled

      cx(t) = 1 − |2·ph_x − 1|               — triangle 0→1→0 at fc Hz
      cy(t) = 1 − |2·ph_y − 1|               — triangle 0→1→0 at fc×:rt Hz

  The triangle wave traces a path that goes from 0 → 1 → 0 → 1 → …, bouncing
  at each boundary.  The phase offset :ph on ph_y shifts the starting corner of
  the cy orbit, controlling the angle of the first 'shot'.

  Carom vs. Lissajous orbit (ex.197)
  ------------------------------------
  Both orbits are driven by two oscillators, one for x and one for y, with a
  frequency ratio :rt and a phase offset :ph.

  Lissajous: orbit components are sinusoids.  The path is smooth and curved; the
  rate of motion is non-uniform (fastest near the centre of each axis, slowest
  near the edges where the sinusoid turns around).

  Carom: orbit components are triangle waves.  The path has sharp corners where
  the trajectory reflects off the boundary; the rate of motion is CONSTANT (the
  orbit moves at the same speed everywhere, because a triangle wave has constant
  slope magnitude).  This makes the carom orbit dwell no longer at the edges than
  the centre — unlike the Lissajous which always slows near the boundaries.

  The constant-velocity property of the carom changes the spectral emphasis of
  the output: since the orbit spends equal time near each terrain feature, the
  output spectrum is shaped more uniformly by the terrain's overall structure.
  The Lissajous tends to over-represent the terrain's edge values.

  Orbit closure and frequency ratios
  ------------------------------------
  Like a billiard ball, the carom orbit closes after a finite number of
  reflections when :rt is rational:

  :rt = 1, :ph = 0     — orbit is the diagonal x=y; one mode per terrain tile.
  :rt = 1, :ph = 0.25  — orbit is an 'X' (two diagonals); closes in one period.
  :rt = 2, :ph = 0     — orbit completes 2 y-bounces per x-bounce; Z-pattern.
  :rt = 2, :ph = 0.5   — orbit offset by half: closes in 2 x-periods.
  :rt = 3, :ph = 0     — orbit makes 3 y-bounces per x-bounce; W-pattern.

  When :rt is irrational (e.g. √2, π) the orbit never closes and densely fills
  the terrain tile over time.

  Phase offset semantics
  -----------------------
  :ph shifts the STARTING POSITION of the y orbit within its period.  :ph = 0
  means both x and y start at 0 simultaneously.  :ph = 0.25 means the y orbit
  starts at 1/4 of its period — the x and y orbits are 90° out of phase, so
  when x is at 0.5 on its way up, y has already reached its peak and is on its
  way down.  This 'rotates' the angle of the reflected path.

  Phase is applied via frac(): frac(phasor(1, fc×:rt) + :ph) wraps the shifted
  phasor back into [0,1) for triangle wave computation.

  Terrain
  -------
  8-waveform harmonic bank (W=512, H=8): same terrain as ex.187/196/197.
  Waveform k has k+1 harmonics with 1/n rolloff.  Bilinear read throughout.

  Parameters
  ----------
  :fc — x orbit frequency in Hz; sets the fundamental repeat rate (20–2000; default 220)
  :rt — y/x frequency ratio; :rt=2 → 2 y-bounces per x-bounce (0.1–8.0; default 1.5)
  :ph — y orbit phase offset [0,1); shifts angle of first reflection (default 0.25)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained wave terrain oscillator)
    :out — terrain height sampled along the carom (reflecting triangle) orbit"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! waveterrain-2d-carom
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :rt {:range [0.1 8.0]     :default 1.5}
            :ph {:range [0.0 1.0]     :default 0.25}}}
  (let [fc  (param :fc)
        rt  (param :rt)
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
                 ph_x=os.phasor(1,%{fc});
                 ph_y=ma.frac(os.phasor(1,%{fc}*%{rt})+%{ph});
                 cx=1.0-abs(2.0*ph_x-1.0);
                 cy=1.0-abs(2.0*ph_y-1.0);
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
              {:fc fc :rt rt :ph ph})]
    (output :out out)))
