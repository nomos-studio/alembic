; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.63-random-distributions
  "GSOT p.101 — random_distributions.maxpat.

  'Random distributions — comparison' (Chapter 4)
  ------------------------------------------------
  Side-by-side comparison of uniform and normal random distributions, both
  scaled to the same [lo, hi] output range.  Demonstrates the statistical
  difference: uniform fills the range with equal density; normal concentrates
  probability mass near the center with exponentially falling tails.

  Outputs
  -------
  :uniform — flat density over [lo, hi]  (random_range pattern, example 55)
  :normal  — bell-shaped density, centered at midpoint of [lo, hi],
             width controlled by :sigma; clamped to [lo, hi]

  Signal flow
  -----------
      mid    = (lo + hi) / 2                          range midpoint
      spread = (hi − lo) / 2                          half-width

      uniform = lo + (hi−lo) · 0.5 · (noise + 1)     U[lo, hi]

      Z       = 0.5 · Σ₁₂ noise_i                    Z ≈ N(0, 1)
      normal  = clamp(mid + sigma · spread · Z, lo, hi)

  Scaling the normal
  ------------------
  :sigma controls the bell width as a fraction of the range half-width.

      sigma = 0.33  →  ±1σ covers ±1/3 of the half-range
              so ±3σ fills the half-range almost exactly — 99.7% within bounds.
      sigma = 1.0   →  the CLT N(0,1) std dev equals the half-range;
              roughly 32% of values fall outside [lo, hi] (hard-clamped).
      sigma = 0.1   →  very tight, values cluster near midpoint.

  The clamping at lo and hi introduces a probability mass at the boundary
  values for large sigma — the distribution develops 'ears' at the edges,
  visible on a histogram/scope.

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp = n_unif, n_norm
        with {
          n_lo    = hslider(\"lo\",    -1.0, -1.0, 1.0, 0.0002);
          n_hi    = hslider(\"hi\",     1.0, -1.0, 1.0, 0.0002);
          n_sig   = hslider(\"sigma\",  0.33,  0.0, 1.0, 0.0001);
          n_unif  = n_lo+(n_hi-n_lo)*0.5*(no.noise+1.0);
          n_mid   = (n_lo+n_hi)*0.5;
          n_sprd  = (n_hi-n_lo)*0.5;
          n_z     = 0.5*(no.noise × 12 ...);
          n_norm  = max(n_lo, min(n_hi, n_mid+n_sig*n_sprd*n_z));
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! random-distributions
  {:params {:lo    {:range [-1.0 1.0] :default -1.0}
            :hi    {:range [-1.0 1.0] :default  1.0}
            :sigma {:range [0.0  1.0] :default  0.33}}}
  (let [lo    (param :lo)
        hi    (param :hi)
        sigma (param :sigma)
        ; Uniform: flat density over [lo, hi]
        unif  (faust "%{lo}+(%{hi}-%{lo})*0.5*(no.noise+1.0)" {:lo lo :hi hi})
        ; Normal: bell-shaped, centered at midpoint, width = sigma × half-range
        mid   (faust "(%{lo}+%{hi})*0.5"   {:lo lo :hi hi})
        sprd  (faust "(%{hi}-%{lo})*0.5"   {:hi hi :lo lo})
        z     (faust "0.5*(no.noise+no.noise+no.noise+no.noise+no.noise+no.noise+no.noise+no.noise+no.noise+no.noise+no.noise+no.noise)"
                     {})
        ; Clamp to [lo, hi] — rare tail values would otherwise exceed the range
        norm  (faust "max(%{lo},min(%{hi},%{md}+%{sg}*%{sp}*%{zz}))"
                     {:lo lo :hi hi :md mid :sg sigma :sp sprd :zz z})]
    (output :uniform unif)
    (output :normal  norm)))
