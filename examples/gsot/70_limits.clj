; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.70-limits
  "GSOT pp.115-116 — go.limits.gendsp.

  'Finding the limits' (Chapter 4)
  ---------------------------------
  Tracks the running minimum and maximum of an input signal.  Used by
  go.autolimit (example 71) to normalise chaotic or unbounded signals
  to the audio range before mixing or further processing.

  Signal flow
  -----------
      lo[n] = min(in[n],  lo[n−1] × decay)
      hi[n] = max(in[n],  hi[n−1] × decay)

  With decay=1.0 (default), lo and hi are all-time extremes — they can
  only move outward.  With decay<1.0 the tracked limits slowly relax
  toward zero between new extremes, weighting recent history more than
  distant past.

  Initial conditions
  ------------------
  Both registers initialise at 0.0 (Faust's ~ initial value):
  • hi: max(in, 0) — correctly tracks positive peaks; converges to the
    true maximum after the first positive sample.
  • lo: min(in, 0) — correctly tracks negative peaks; converges to the
    true minimum after the first negative sample.

  For signals that are always positive (e.g., Lorenz z ∈ [0,50]):
  lo stays at 0.0, which is a reasonable floor for autolimit purposes
  (normalisation maps 0→−1 and z_max→+1).

  For signals that cross zero (Lorenz x ∈ [−20,20], y ∈ [−30,30]):
  lo and hi both converge within one or two attractor orbits.

  Parameters
  ----------
  :decay — per-sample decay factor for tracked limits (1.0 = all-time)

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_lo, n_hi
        with {
          n_dc = hslider(\"decay\", 1.0, 0.9, 1.0, 0.0001);
          n_lo = (min(n0, _*n_dc) ~ _);
          n_hi = (max(n0, _*n_dc) ~ _);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! limits
  {:params {:decay {:range [0.9 1.0] :default 1.0}}}
  (let [in    (audio-in)
        decay (param :decay)
        lo    (faust "(min(%in,_*%dc))~_" {:in in :dc decay})
        hi    (faust "(max(%in,_*%dc))~_" {:in in :dc decay})]
    (output :lo lo)
    (output :hi hi)))
