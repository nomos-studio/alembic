; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.191-wavetable-3d-attractor
  "GSOT p.278 — wavetable_3D_attractor.maxpat (Chapter 9: Navigating Waves of Data).

  '3D Wavetable Oscillator with Lorenz Attractor Navigation'
  -----------------------------------------------------------
  Replaces the explicit :wp/:zp knobs in ex.188 with a Lorenz strange attractor
  that drives the Y and Z wavetable axes automatically.  The oscillator phasor
  still drives X; the attractor's chaotic trajectory navigates the waveform bank
  in a bounded, non-repeating pattern.

  The Lorenz attractor
  --------------------
  The Lorenz system (Lorenz 1963) is a set of three coupled ODEs:

      dx/dt = σ × (y − x)
      dy/dt = x × (ρ − z) − y
      dz/dt = x × y − β × z

  Classic parameters: σ=10 (Prandtl number), ρ=28 (Rayleigh number), β=8/3.

  The system has a STRANGE ATTRACTOR: trajectories are bounded within a
  'butterfly' shaped region but never repeat.  The attractor fills a finite
  region of (x,y,z) space:
      x: roughly ±20 (normalise to [0,1] via (x+25)/50)
      z: roughly [0,50] (normalise via z/50)

  Discrete Euler integration (audio rate)
  -----------------------------------------
  Faust integrates at sample rate using the forward Euler method:

      lx[n] = lx[n-1] + dt × σ × (ly[n-1] − lx[n-1])
      ly[n] = ly[n-1] + dt × (lx[n-1] × (ρ − lz[n-1]) − ly[n-1])
      lz[n] = lz[n-1] + dt × (lx[n-1] × ly[n-1] − β × lz[n-1])

  The `~` feedback operator gives exactly these 1-sample delays.  `lorenz_step`
  is a 3-input, 3-output circuit whose output feeds back to its input via
  `si.bus(3)` (identity), creating a closed autonomous loop:

      lorenz = lorenz_step ~ si.bus(3)

  This is a 0-input, 3-output Faust circuit.  Initial state: (0,0,0).

  Initialisation kick
  -------------------
  At (0,0,0) the Lorenz system is at an unstable fixed point — numerically it
  stays near zero indefinitely.  `imp = float(1-1')` (1 at t=0, 0 forever after) adds:
    — +1 to lx at t=0   → x kicks away from the fixed point
    — +25 to lz at t=0  → z starts near the centre of the attractor (z≈25)

  This seeds the trajectory into the strange attractor basin from the first sample.

  Speed control (:dt)
  --------------------
  The step size :dt controls how fast the trajectory moves through the attractor:
    — Very small dt (e.g., 1e-5): slow drift, nearly static timbre; useful for
      very gradual spectral evolution over seconds/minutes
    — Moderate dt (1e-4): timbre evolves over a few seconds per 'wing' of the
      butterfly; musical modulation rate
    — Large dt (1e-3): trajectory moves quickly; audible rapid timbral change;
      may also introduce integration instability

  At dt = 1e-4 and SR = 44100 Hz: one attractor period ≈ 1–5 seconds.

  CSE and single attractor instance
  -----------------------------------
  The `lorenz` circuit is referenced twice in the `with` block:
  once for the Y normalisation (uses lx via `lorenz:(_,!,!)`) and once
  for Z normalisation (uses lz via `lorenz:(!,!,_)`).  Faust's CSE
  (Common Subexpression Elimination) recognises that both references name
  the same `lorenz_step ~ si.bus(3)` circuit and shares the feedback registers —
  one attractor instance drives both axes.

  Wavetable bank (same as ex.188)
  --------------------------------
  W=256, H=4, D=4.  Waveform at (y,z): y+1 harmonics with 1/n rolloff, phase
  offset z×π/4.  Generated at init time via ba.time in rdtable init.

  Attractor axes → wavetable axes:
    lx (x Lorenz, ≈ ±20) → Y wavetable position (harmonic count 1..4)
    lz (z Lorenz, ≈ 0..50) → Z wavetable position (phase offset 0..3π/4)

  ly is not used (it maps to the same attractor geometry as lx but slightly
  shifted — one of the three is always redundant for a 2D navigated 3D table).

  Parameters
  ----------
  :fc — oscillator frequency in Hz (20–2000; default 220)
  :dt — Lorenz integration step size (1e-6–1e-3; default 1e-4)

  Audio inputs / Outputs
  ----------------------
  (no audio input — autonomous Lorenz attractor + wavetable oscillator)
    :out — trilinearly interpolated output; timbre evolves along the Lorenz
           attractor's chaotic trajectory in the 3D waveform bank"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! wavetable-3d-attractor
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :dt {:range [1.0e-6 1.0e-3] :default 1.0e-4}}}
  (let [fc  (param :fc)
        dt  (param :dt)
        ph  (faust "os.phasor(1,%fc)" {:fc fc})
        out (faust
              "trilinear
               with {
                 sg=10.0; rh=28.0; bt=8.0/3.0;
                 imp=float(1-1');
                 lorenz_step(lx,ly,lz) = (
                   lx+%dt*sg*(ly-lx)+imp,
                   ly+%dt*(lx*(rh-lz)-ly),
                   lz+%dt*(lx*ly-bt*lz)+25.0*imp
                 );
                 lorenz = lorenz_step~si.bus(3);
                 wp_a = max(0.0,min(1.0,((lorenz:(_,!,!))+25.0)/50.0));
                 zp_a = max(0.0,min(1.0,(lorenz:(!,!,_))/50.0));
                 W=256; H=4; D=4; N=W*H*D;
                 xt=ba.time%W; yt=(ba.time/W)%H; zt=ba.time/(W*H);
                 ang=2.0*ma.PI*float(xt)/float(W);
                 ph_off=float(zt)*ma.PI/4.0;
                 h(k)=sin(ang*float(k+1)+ph_off)/float(k+1)*float(k<=yt);
                 tbl_init=0.5*(h(0)+h(1)+h(2)+h(3));
                 tbl(i)=rdtable(N,tbl_init,max(0,min(N-1,i)));
                 xfull=%ph*float(W); yfull=wp_a*float(H); zfull=zp_a*float(D);
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
              {:ph ph :dt dt})]
    (output :out out)))
