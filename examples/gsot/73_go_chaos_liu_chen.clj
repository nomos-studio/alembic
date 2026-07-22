; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.73-go-chaos-liu-chen
  "GSOT pp.118 — go.chaos.liu_chen.gendsp.

  go.chaos configured for the Lu-Chen (Lü) attractor (Lu & Chen 2002).
  -----------------------------------------------------------------------
  The Lu-Chen attractor is a member of the same algebraic family as Lorenz
  (see example 72 go-chaos), obtained by setting:

      a=36, b=0, c=20, d=3

  in the abstract chaos skeleton:

      dx/dt = a(y − x)        →   36(y − x)
      dy/dt = bx + cy − xz    →   20y − xz       (b=0: no x linear term)
      dz/dt = xy − dz         →   xy − 3z

  The b=0 case eliminates the saddle-point structure that defines the
  Lorenz wings and produces a topologically distinct strange attractor —
  the butterfly shape is preserved but the trajectory distribution and
  crossing statistics differ.

  This is the hardwired gen~ subpatch (go.chaos.liu_chen.gendsp).  The
  coefficients are not parameters; they are compiled into the patch.  If
  you need to interpolate between Lorenz and Lu-Chen, use go-chaos
  (example 72) and automate the b and c parameters.

  Audio inputs
  ------------
  audio-in 0: x-in   — x state from previous sample
  audio-in 1: y-in   — y state from previous sample
  audio-in 2: z-in   — z state from previous sample

  Parameters
  ----------
  :h  — integration step size (default 0.005; Lu-Chen a=36 is faster than
         Lorenz a=10, requiring a smaller step for stability)

  Typical signal ranges: x ∈ [−25,25], y ∈ [−35,35], z ∈ [0,55]
  Use autolimit (example 71) to normalise to audio range.

  Initial conditions: (0,0,0) is a fixed point — provide at least one
  non-zero audio-in value to seed the attractor.  Any (x,y,z) ≠ (0,0,0)
  reaches the strange attractor after a short transient.

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1, n2) = n_x, n_y, n_z
        with {
          n_hh = hslider(\"h\", 0.005, 0.001, 0.02, 0.0001);
          n_x  = n0+n_hh*36.0*(n1-n0);
          n_y  = n1+n_hh*(20.0*n1-n0*n2);
          n_z  = n2+n_hh*(n0*n1-3.0*n2);
        };
      process = alembic_dsp;

  n0 = x-in,  n1 = y-in,  n2 = z-in"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! go-chaos-liu-chen
  {:params {:h {:range [0.001 0.02] :default 0.005}}}
  (let [x-in (audio-in)
        y-in (audio-in)
        z-in (audio-in)
        h    (param :h)
        x-out (faust "%xi+%hh*36.0*(%yi-%xi)"   {:xi x-in :yi y-in :hh h})
        y-out (faust "%yi+%hh*(20.0*%yi-%xi*%zi)" {:xi x-in :yi y-in :zi z-in :hh h})
        z-out (faust "%zi+%hh*(%xi*%yi-3.0*%zi)" {:xi x-in :yi y-in :zi z-in :hh h})]
    (output :x x-out)
    (output :y y-out)
    (output :z z-out)))
