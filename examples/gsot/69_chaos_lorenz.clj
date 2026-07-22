; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.69-chaos-lorenz
  "GSOT pp.112-114 — chaos_Lorenz.maxpat.

  'Chaos (and why we like it)' / 'A Lorenz attractor' (Chapter 4)
  ----------------------------------------------------------------
  The Lorenz attractor is a continuous-time chaotic dynamical system:

      dx/dt = σ(y − x)
      dy/dt = x(ρ − z) − y
      dz/dt = xy − βz

  Classic parameters (Lorenz 1963): σ=10, ρ=28, β=8/3≈2.667.
  The system is bounded but never repeating — a strange attractor with
  the characteristic butterfly shape in the (x,z) plane.

  Discrete-time (explicit Euler) integration at audio rate:
      x[n] = x[n−1] + h·σ·(y[n−1] − x[n−1])
      y[n] = y[n−1] + h·(x[n−1]·(ρ − z[n−1]) − y[n−1])
      z[n] = z[n−1] + h·(x[n−1]·y[n−1] − β·z[n−1])

  Architecture: update step as a 3-in / 3-out patch
  --------------------------------------------------
  The three equations form a mutually coupled cycle — x needs y_prev and
  z_prev, y needs x_prev and z_prev, z needs x_prev and y_prev.  In gen~
  this is resolved with `history` (1-sample delay feedback) inside a
  single patch.  Faust's native solution is `step ~ si.bus(3)` (3-channel
  feedback); Alembic has no mechanism to emit multi-channel ~.

  This patch implements ONLY the Euler step: given x, y, z from the
  previous sample as audio inputs, it outputs x', y', z' for the next
  sample.  Self-oscillation requires connecting x' → x-in, y' → y-in,
  z' → z-in with SAMPLE-ACCURATE (zero-buffer-latency) feedback.

  In a modular context: patch cables, or a dedicated 'Lorenz feedback
  cable' that holds the previous block's outputs as the next block's
  inputs — exactly how analog Lorenz oscillator modules work.

  See threads/2026/07/20260721-alembic-codebox-boundary.org for the
  multi-channel ~ extension that would enable a self-contained implementation.

  Audio inputs
  ------------
  audio-in 0: x-in   — x state from previous sample
  audio-in 1: y-in   — y state from previous sample
  audio-in 2: z-in   — z state from previous sample

  Parameters
  ----------
  :h     — integration step size (smaller = more stable; default 0.01)
  :sigma — σ coefficient (default 10.0)
  :rho   — ρ coefficient (default 28.0)
  :beta  — β coefficient (default 2.667)

  Initial conditions and scaling
  --------------------------------
  (0, 0, 0) is a fixed point — provide at least one non-zero input to
  start the attractor.  The Lorenz system with default parameters produces:
      x ∈ [−20, 20],  y ∈ [−30, 30],  z ∈ [0, 50]
  Scale outputs by 1/30 or similar before mixing into audio signals.

  Stability: h < 0.02 is safe for default parameters.  Larger h causes
  the explicit Euler integration to diverge.

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1, n2) = n_x, n_y, n_z
        with {
          n_h  = hslider(\"h\",     0.01, 0.001, 0.05,  0.0001);
          n_sg = hslider(\"sigma\", 10.0, 1.0,   50.0,  0.5);
          n_rh = hslider(\"rho\",   28.0, 1.0,   60.0,  0.5);
          n_bt = hslider(\"beta\",  2.667,0.1,   10.0,  0.001);
          n_x  = n0+n_h*n_sg*(n1-n0);
          n_y  = n1+n_h*(n0*(n_rh-n2)-n1);
          n_z  = n2+n_h*(n0*n1-n_bt*n2);
        };
      process = alembic_dsp;

  n0 = x-in,  n1 = y-in,  n2 = z-in"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! chaos-lorenz
  {:params {:h     {:range [0.001 0.05]  :default 0.01}
            :sigma {:range [1.0   50.0]  :default 10.0}
            :rho   {:range [1.0   60.0]  :default 28.0}
            :beta  {:range [0.1   10.0]  :default 2.667}}}
  (let [x-in  (audio-in)
        y-in  (audio-in)
        z-in  (audio-in)
        h     (param :h)
        sigma (param :sigma)
        rho   (param :rho)
        beta  (param :beta)
        x-out (faust "%xi+%hh*%sg*(%yi-%xi)"       {:xi x-in :yi y-in :hh h :sg sigma})
        y-out (faust "%yi+%hh*(%xi*(%rh-%zi)-%yi)" {:xi x-in :yi y-in :zi z-in :hh h :rh rho})
        z-out (faust "%zi+%hh*(%xi*%yi-%bt*%zi)"   {:xi x-in :yi y-in :zi z-in :hh h :bt beta})]
    (output :x x-out)
    (output :y y-out)
    (output :z z-out)))
