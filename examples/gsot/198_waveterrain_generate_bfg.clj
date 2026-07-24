; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.198-waveterrain-generate-bfg
  "GSOT pp.298-299 — waveterrain_generate_BFG.maxpat (Chapter 9: Navigating Waves of Data).

  'Basis Function Generator (BFG) Wave Terrain Oscillator'
  ---------------------------------------------------------
  A BFG terrain replaces the harmonic waveform bank (ex.187) with a terrain
  surface defined as a weighted sum of 2D basis functions.  Each basis function
  is a product of 1D sinusoids — the 2D analog of adding harmonics to a waveform:

      T(x, y) = Σ_{m,n}  w_{mn} · sin(m·2π·x) · sin(n·2π·y)

  where x,y ∈ [0,1) and (m,n) index the 2D spatial frequency.  A terrain with
  only the (1,1) basis function is a 'checkerboard' sine surface; adding (2,1)
  introduces a second spatial harmonic along x; (1,2) along y; (2,2) adds a
  'double-checkerboard' fourth-order mode.

  BFG terrain vs. harmonic bank (ex.187)
  ----------------------------------------
  In ex.187 the terrain has a fixed structure (k+1 harmonics per row) and the
  user navigates it with :wp.  In BFG terrain the shape itself is parameterized:
  the basis function weights :w1–:w4 define the terrain topology.  Changing the
  weights reshapes the terrain in real time, changing which spectral components
  the orbit encounters as it traces its path.

  The fundamental difference is orthogonality:  each (m,n) mode is independently
  controllable.  If :w1 is the only non-zero weight, the terrain is a pure sine
  product and the orbit extracts a specific 2D sinusoidal cross-section.  Adding
  :w4 (the (2,2) mode) adds a 2D second-harmonic without disturbing the (1,1)
  component — the two modes are orthogonal on the square terrain domain.

  Basis functions implemented here
  ----------------------------------
  Mode (m=1, n=1): sin(2π·x) · sin(2π·y)           — weight :w1
  Mode (m=1, n=2): sin(2π·x) · sin(4π·y)           — weight :w2
  Mode (m=2, n=1): sin(4π·x) · sin(2π·y)           — weight :w3
  Mode (m=2, n=2): sin(4π·x) · sin(4π·y)           — weight :w4

  The terrain is normalised so the maximum possible value (all weights = 1) does
  not exceed ±4.  In practice each mode contributes ±1 at most, so the output
  amplitude is bounded by Σ|w_i|.

  Max/MSP Jitter pipeline (for context)
  ----------------------------------------
  The GSOT patch on pp.298-299 uses three Jitter objects to build the terrain:

      jit.bfg       — Max Jitter object that generates a 2D BFG texture matrix.
                       Its rows/cols define the terrain width/height; its
                       frequency and phase parameters set the basis function mix.

      jit.scanwrap  — Linearises the 2D Jitter matrix into a 1D row-major scan.
                       Maps matrix[row][col] → buffer[row * width + col].

      jit.buffer    — Exposes the scanned matrix data as a buffer~ for use
                       inside a gen~ sub-patcher (terrain reader).

  In Max, this three-stage pipeline pre-computes the BFG texture into a buffer
  (at control rate or once on init) and the gen~ terrain reader samples from that
  buffer at audio rate.  The table acts as a lookup cache.

  Alembic implementation — analytical BFG (no table)
  ---------------------------------------------------
  Faust's rdtable initialisation signal must be computable at DSP init time using
  only constants and `ba.time` — it cannot depend on runtime slider values.  Since
  the :w1–:w4 weights are runtime parameters, we cannot use them in rdtable init.

  Instead, we compute the BFG terrain value analytically at each audio sample:

      ax = 2π · cx(t)         (normalised x angle, ∈ [0, 2π))
      ay = 2π · cy(t)         (normalised y angle, ∈ [0, 2π))
      T  = w1·sin(ax)·sin(ay) + w2·sin(ax)·sin(2ay)
           + w3·sin(2ax)·sin(ay) + w4·sin(2ax)·sin(2ay)

  This is mathematically identical to reading from a dense jit.bfg texture at
  (cx, cy) — the texture just has infinite resolution since there's no table.
  It is more accurate than the table approach (no bilinear interpolation error)
  and naturally supports runtime weight modulation.

  Orbit generator (Lissajous, same as ex.197)
  -------------------------------------------
  cx(t) = 0.5 + 0.5·cos(2π·:fc·t)                    ∈ [0,1)
  cy(t) = 0.5 + 0.5·cos(2π·:fc·:rt·t + 2π·:ph)       ∈ [0,1)

  The orbit reads from the BFG terrain analytically.

  Parameter space
  ---------------
  The most musically useful initial states:

  :w1=1 :w2=0 :w3=0 :w4=0 — pure sine product; single spectral mode per orbit.
  :w1=1 :w2=1 :w3=0 :w4=0 — adds y-second-harmonic; richer vertical modulation.
  :w1=1 :w2=0 :w3=1 :w4=0 — adds x-second-harmonic; asymmetric horizontal content.
  :w1=1 :w2=1 :w3=1 :w4=1 — all four modes; complex interference terrain.
  :w1=0 :w2=0 :w3=0 :w4=1 — only the (2,2) mode; high-frequency grid pattern.

  Modulating the weights at control rate reshapes the terrain in real time,
  changing the timbral character of the orbit without altering its trajectory.

  Parameters
  ----------
  :fc — orbit frequency in Hz (20–2000; default 220)
  :rt — y/x frequency ratio; 1=circle/diagonal, 2=figure-8, etc. (0.1–8.0; default 1.0)
  :ph — y oscillator phase offset [0,1); 0=diagonal, 0.25=circle/ellipse (0–1; default 0.25)
  :w1 — weight for basis mode (m=1, n=1) sin(2πx)·sin(2πy)  (-1–1; default 1.0)
  :w2 — weight for basis mode (m=1, n=2) sin(2πx)·sin(4πy)  (-1–1; default 0.0)
  :w3 — weight for basis mode (m=2, n=1) sin(4πx)·sin(2πy)  (-1–1; default 0.0)
  :w4 — weight for basis mode (m=2, n=2) sin(4πx)·sin(4πy)  (-1–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained wave terrain oscillator)
    :out — BFG terrain height sampled analytically along the Lissajous orbit"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! waveterrain-generate-bfg
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :rt {:range [0.1 8.0]     :default 1.0}
            :ph {:range [0.0 1.0]     :default 0.25}
            :w1 {:range [-1.0 1.0]    :default 1.0}
            :w2 {:range [-1.0 1.0]    :default 0.0}
            :w3 {:range [-1.0 1.0]    :default 0.0}
            :w4 {:range [-1.0 1.0]    :default 0.0}}}
  (let [fc  (param :fc)
        rt  (param :rt)
        ph  (param :ph)
        w1  (param :w1)
        w2  (param :w2)
        w3  (param :w3)
        w4  (param :w4)
        out (faust
              "terrain
               with {
                 cx=0.5+0.5*cos(2.0*ma.PI*os.phasor(1,%fc));
                 cy=0.5+0.5*cos(2.0*ma.PI*os.phasor(1,%fc*%rt)+2.0*ma.PI*%ph);
                 ax=2.0*ma.PI*cx;
                 ay=2.0*ma.PI*cy;
                 terrain=%w1*sin(ax)*sin(ay)
                        +%w2*sin(ax)*sin(2.0*ay)
                        +%w3*sin(2.0*ax)*sin(ay)
                        +%w4*sin(2.0*ax)*sin(2.0*ay);
               }"
              {:fc fc :rt rt :ph ph :w1 w1 :w2 w2 :w3 w3 :w4 w4})]
    (output :out out)))
