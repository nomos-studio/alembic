; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.71-autolimit
  "GSOT pp.116 — go.autolimit.gendsp.

  Auto-normalise an unbounded signal to the audio range [−1, 1]
  by tracking its running min and max (go.limits, example 70) and
  rescaling on the fly.

  Signal flow
  -----------
      lo[n]  = min(in[n], lo[n−1] × decay)          (running min)
      hi[n]  = max(in[n], hi[n−1] × decay)          (running max)
      rng[n] = max(hi[n] − lo[n], ε)                (guard against ÷0)
      out[n] = (in[n] − lo[n]) / rng[n] × 2 − 1    (map to [−1, 1])

  The formula maps lo → −1 and hi → +1 linearly.  For a signal
  symmetric around zero (e.g., Lorenz x or y): lo ≈ −|extreme| and
  hi ≈ +|extreme|, so the mapping is equivalent to in / |extreme|.

  Warmup
  ------
  Both lo and hi initialise at 0.0.  Until the tracked limits have
  converged to the true extremes, the output can exceed [−1, 1].
  For chaotic signals one or two attractor orbits is sufficient.

  Parameters
  ----------
  :decay — per-sample decay factor for tracked limits (1.0 = all-time)

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_out
        with {
          n_dc  = hslider(\"decay\", 1.0, 0.9, 1.0, 0.0001);
          n_lo  = (min(n0, _*n_dc) ~ _);
          n_hi  = (max(n0, _*n_dc) ~ _);
          n_rn  = max(n_hi-n_lo, 0.0001);
          n_out = (n0-n_lo)/n_rn*2.0-1.0;
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! autolimit
  {:params {:decay {:range [0.9 1.0] :default 1.0}}}
  (let [in    (audio-in)
        decay (param :decay)
        lo    (faust "(min(%{in},_*%{dc}))~_" {:in in :dc decay})
        hi    (faust "(max(%{in},_*%{dc}))~_" {:in in :dc decay})
        rng   (faust "max(%{hi}-%{lo},0.0001)" {:hi hi :lo lo})
        out   (faust "(%{in}-%{lo})/%{rn}*2.0-1.0" {:in in :lo lo :rn rng})]
    (output out)))
