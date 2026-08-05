; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.75-chaos-lorenz-audioinjection
  "GSOT pp.121 — chaos_Lorenz_audioinjection.maxpat.

  'Injecting audio into chaos'
  ----------------------------
  The Lorenz attractor (example 69) runs in autonomous mode; its trajectory
  is determined entirely by initial conditions and parameters.  Audio
  injection drives one of the state equations with an external signal,
  coupling the input audio's dynamics into the chaotic orbit.

  Modified x equation (audio injected on x):
      x[n] = x[n−1] + h·σ·(y[n−1] − x[n−1]) + amount · audio[n]

  y and z equations are unchanged:
      y[n] = y[n−1] + h·(x[n−1]·(ρ − z[n−1]) − y[n−1])
      z[n] = z[n−1] + h·(x[n−1]·y[n−1] − β·z[n−1])

  Musical character
  -----------------
  Injecting into x perturbs the 'driving' variable — the one that defines
  the shape of the Lorenz wings.  Effects range by injection amount:

    amount ≈ 0:   pure Lorenz chaos; audio has no effect
    amount small: audio subtly deflects the trajectory between attractors;
                  the output carries a chaotic texture coloured by the input
    amount large: audio dominates; the chaotic attractor geometry breaks down
                  and the system behaves as a nonlinear distortion of the input

  At moderate injection the output sounds like ring modulation or wavefold
  distortion, but with a chaotically time-varying transfer function.

  Injection point
  ---------------
  x is the 'thermodynamic driving' variable — physically it represents the
  intensity of convective motion.  Injecting there couples most directly
  into the butterfly-wing switching behaviour.  Injecting into y (horizontal
  velocity) or z (vertical temperature) gives different timbral characters;
  those variants are left as exercises.

  Architecture: same external-feedback topology as example 69
  -----------------------------------------------------------
  Self-oscillation requires wiring x-out → x-in, y-out → y-in,
  z-out → z-in with sample-accurate feedback (see example 69 docstring).
  The audio signal is the fourth audio input — it requires no feedback.

  Audio inputs
  ------------
  audio-in 0: x-in    — x state from previous sample (feedback)
  audio-in 1: y-in    — y state from previous sample (feedback)
  audio-in 2: z-in    — z state from previous sample (feedback)
  audio-in 3: sig-in  — audio signal to inject into x equation

  Parameters
  ----------
  :h      — integration step (default 0.01; keep < 0.02 for stability)
  :sigma  — σ (default 10.0)
  :rho    — ρ (default 28.0)
  :beta   — β (default 2.667)
  :inject — injection scaling factor (default 1.0; 0 = pure Lorenz)

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1, n2, n3) = n_x, n_y, n_z
        with {
          n_hh = hslider(\"h\",      0.01,  0.001, 0.05,  0.0001);
          n_sg = hslider(\"sigma\",  10.0,  1.0,   50.0,  0.5);
          n_rh = hslider(\"rho\",    28.0,  1.0,   60.0,  0.5);
          n_bt = hslider(\"beta\",   2.667, 0.1,   10.0,  0.001);
          n_am = hslider(\"inject\",  1.0,  0.0,   10.0,  0.01);
          n_x  = n0+n_hh*n_sg*(n1-n0)+n_am*n3;
          n_y  = n1+n_hh*(n0*(n_rh-n2)-n1);
          n_z  = n2+n_hh*(n0*n1-n_bt*n2);
        };
      process = alembic_dsp;

  n0 = x-in, n1 = y-in, n2 = z-in, n3 = sig-in"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! chaos-lorenz-audioinjection
  {:params {:h      {:range [0.001 0.05]  :default 0.01}
            :sigma  {:range [1.0   50.0]  :default 10.0}
            :rho    {:range [1.0   60.0]  :default 28.0}
            :beta   {:range [0.1   10.0]  :default 2.667}
            :inject {:range [0.0   10.0]  :default 1.0}}}
  (let [x-in    (audio-in)
        y-in    (audio-in)
        z-in    (audio-in)
        sig-in  (audio-in)
        h       (param :h)
        sigma   (param :sigma)
        rho     (param :rho)
        beta    (param :beta)
        inj-amt (param :inject)
        x-out   (faust "%{xi}+%{hh}*%{sg}*(%{yi}-%{xi})+%{am}*%{ix}"
                       {:xi x-in :yi y-in :hh h :sg sigma :am inj-amt :ix sig-in})
        y-out   (faust "%{yi}+%{hh}*(%{xi}*(%{rh}-%{zi})-%{yi})"
                       {:xi x-in :yi y-in :zi z-in :hh h :rh rho})
        z-out   (faust "%{zi}+%{hh}*(%{xi}*%{yi}-%{bt}*%{zi})"
                       {:xi x-in :yi y-in :zi z-in :hh h :bt beta})]
    (output :x x-out)
    (output :y y-out)
    (output :z z-out)))
