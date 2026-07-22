; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.62-normal-noise
  "GSOT p.100 — go.noise.normal.gendsp.

  'Random distributions — normal distribution' (Chapter 4)
  ---------------------------------------------------------
  Generates approximately normally distributed random values using the
  Central Limit Theorem: the sum of N independent uniform random variables
  converges to a normal distribution as N grows.

  CLT sum-of-12 derivation
  ------------------------
  Each no.noise instance is U[−1, 1]:  E[x] = 0,  Var[x] = 1/3.
  Scaling by 0.5:  E[x/2] = 0,  Var[x/2] = 1/12.

  Sum of 12 independent scaled instances:
      E[sum] = 0,   Var[sum] = 12 × 1/12 = 1   →   std dev ≈ 1.

  Output ≈ N(0, 1) — zero mean, unit variance.

  In Faust, each literal occurrence of no.noise in the signal graph is
  compiled to a separate linear congruential generator (its own feedback
  register), so the 12 occurrences are independent.

  Parameters
  ----------
  :mu    — output mean       (shifts distribution center)
  :sigma — output std dev    (scales width)

  Final output ≈ N(mu, sigma²):
      out = mu + sigma × Z        Z ≈ N(0, 1)

  With default :mu = 0 and :sigma = 0.3 the ±3σ range is ±0.9, fitting
  almost entirely within the audio range [−1, 1].  Rare outliers beyond
  ±1 are expected and may be soft-clipped downstream.

  Relationship to uniform noise
  ------------------------------
  random_range (example 55) gives uniform density — equal probability of
  any value in [lo, hi].  Normal noise concentrates density near the mean:
  ~68% within ±1σ, ~95% within ±2σ, ~99.7% within ±3σ.  The two
  distributions sound different: uniform noise has 'harsh' statistics;
  normal noise sounds 'smoother' because extreme values are rare.

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp = n2
        with {
          n0 = hslider(\"mu\",    0.0, -1.0, 1.0, 0.0001);
          n1 = hslider(\"sigma\", 0.3,  0.0, 1.0, 0.0001);
          n_z = 0.5*(no.noise+no.noise+no.noise+no.noise+no.noise+no.noise
                    +no.noise+no.noise+no.noise+no.noise+no.noise+no.noise);
          n2 = n0+n1*n_z;
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! normal-noise
  {:params {:mu    {:range [-1.0 1.0] :default 0.0}
            :sigma {:range [0.0  1.0] :default 0.3}}}
  (let [z   (faust "0.5*(no.noise+no.noise+no.noise+no.noise+no.noise+no.noise+no.noise+no.noise+no.noise+no.noise+no.noise+no.noise)"
                   {})
        out (faust "%mu+%sg*%zz" {:mu (param :mu) :sg (param :sigma) :zz z})]
    (output out)))
