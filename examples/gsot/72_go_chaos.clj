; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.72-go-chaos
  "GSOT pp.117 — go.chaos / Liu-Chen.maxpat.

  Abstract parameterised chaos step function.
  -------------------------------------------
  The Lorenz (1963) and Lu-Chen (2002) attractors share a common
  algebraic skeleton:

      dx/dt = a(y − x)
      dy/dt = bx + cy − xz
      dz/dt = xy − dz

  With different parameters the same skeleton produces qualitatively
  different strange attractors:

    Lorenz:   a=10,  b=28,  c=−1,   d=2.667
              → the original butterfly attractor
    Lu-Chen:  a=36,  b=0,   c=20,   d=3
              → a new attractor (Lu & Chen 2002); also called the Lü attractor

  `go.chaos` is the gen~ subpatch that implements this abstract Euler
  step with all four coefficients exposed as parameters.  `Liu-Chen.maxpat`
  instantiates it with the Lu-Chen values.

  In Alembic: same 3-audio-in / 3-audio-out architecture as example 69
  (chaos-lorenz).  Default parameters reproduce the Lorenz attractor.

  Audio inputs
  ------------
  audio-in 0: x-in   — x state from previous sample
  audio-in 1: y-in   — y state from previous sample
  audio-in 2: z-in   — z state from previous sample

  Parameters
  ----------
  :h  — integration step size (default 0.01; keep < 0.02 for Lorenz,
         < 0.01 for Lu-Chen due to larger a coefficient)
  :a  — dx/dt coefficient (default 10.0 / Lorenz σ)
  :b  — x coefficient in dy/dt (default 28.0 / Lorenz ρ; set to 0 for Lu-Chen)
  :c  — y coefficient in dy/dt (default −1.0 / Lorenz; set to 20 for Lu-Chen)
  :d  — z damping in dz/dt (default 2.667 / Lorenz β; set to 3 for Lu-Chen)

  Typical signal ranges
  ---------------------
  Lorenz (default):  x ∈ [−20,20], y ∈ [−30,30], z ∈ [0,50]
  Lu-Chen:           x ∈ [−25,25], y ∈ [−35,35], z ∈ [0,55]
  Use autolimit (example 71) to normalise before mixing into audio.

  See examples.gsot.73-go-chaos-liu-chen for the Liu-Chen hardwired variant.

  Emitted Faust DSP (Lorenz defaults):
      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1, n2) = n_x, n_y, n_z
        with {
          n_hh = hslider(\"h\",  0.01,  0.001, 0.05, 0.0001);
          n_aa = hslider(\"a\",  10.0,  1.0,  50.0,  0.5);
          n_bb = hslider(\"b\",  28.0, -1.0,  60.0,  0.5);
          n_cc = hslider(\"c\",  -1.0, -5.0,  30.0,  0.1);
          n_dd = hslider(\"d\",  2.667, 0.1,  10.0,  0.001);
          n_x  = n0+n_hh*n_aa*(n1-n0);
          n_y  = n1+n_hh*(n_bb*n0+n_cc*n1-n0*n2);
          n_z  = n2+n_hh*(n0*n1-n_dd*n2);
        };
      process = alembic_dsp;

  n0 = x-in,  n1 = y-in,  n2 = z-in"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! go-chaos
  {:params {:h {:range [0.001 0.05]  :default 0.01}
            :a {:range [1.0   50.0]  :default 10.0}
            :b {:range [-1.0  60.0]  :default 28.0}
            :c {:range [-5.0  30.0]  :default -1.0}
            :d {:range [0.1   10.0]  :default 2.667}}}
  (let [x-in (audio-in)
        y-in (audio-in)
        z-in (audio-in)
        h    (param :h)
        a    (param :a)
        b    (param :b)
        c    (param :c)
        d    (param :d)
        x-out (faust "%{xi}+%{hh}*%{aa}*(%{yi}-%{xi})"            {:xi x-in :yi y-in :hh h :aa a})
        y-out (faust "%{yi}+%{hh}*(%{bb}*%{xi}+%{cc}*%{yi}-%{xi}*%{zi})" {:xi x-in :yi y-in :zi z-in :hh h :bb b :cc c})
        z-out (faust "%{zi}+%{hh}*(%{xi}*%{yi}-%{dd}*%{zi})"         {:xi x-in :yi y-in :zi z-in :hh h :dd d})]
    (output :x x-out)
    (output :y y-out)
    (output :z z-out)))
