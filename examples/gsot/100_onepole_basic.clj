; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.100-onepole-basic
  "GSOT pp.159-163 — go.onepole.basic.gendsp (Chapter 6).

  'One-pole lowpass filter — from blocks to operators'
  -----------------------------------------------------
  The fundamental IIR filter.  One feedback coefficient; one sample of memory.

  The filter equation
  -------------------
      y[n] = (1 − a) × x[n]  +  a × y[n-1]

  x[n]   — current input sample
  y[n-1] — previous output (one sample of memory; the ~_ feedback register)
  a      — the 'balance' coefficient  [0, 1)

  The coefficient a is the 'balance of time': it controls the weight given to
  the PRESENT (input) versus the PAST (previous output).

    a = 0.0 → y[n] = x[n]        pure present; no filtering; identity
    a = 0.5 → equal weighting of now and one sample ago
    a → 1.0 → y[n] ≈ y[n-1]      pure past; integrator / DC accumulator

  (1 − a) and a always sum to 1, so the filter is a weighted average.

  Transfer function
  -----------------
      H(z) = (1 − a) / (1 − a·z⁻¹)

  Single pole at z = a on the real axis.  As a → 1 the pole approaches the
  unit circle → the −3 dB cutoff drops toward DC.  As a → 0 the pole moves to
  the origin → flat frequency response (identity).

  Cutoff frequency relationship
  -----------------------------
  For sample rate sr (Hz) and −3 dB cutoff fc (Hz):

      a = exp(−2π × fc / sr)          (exact; one-pole IIR)

  At sr=48000 and fc=1000: a ≈ exp(−0.131) ≈ 0.877.
  Increasing a lowers the cutoff; decreasing a raises it.

  The 'go.onepole.basic' subpatch concept (GSOT)
  -----------------------------------------------
  GSOT pp.159-163 builds the filter from first principles — block diagram →
  signal flow → operator form — before arriving at this minimal gen~ subpatch.
  The 'from blocks to operators' subtitle traces how the block diagram (two
  multipliers, one adder, one unit delay) collapses to a single feedback
  expression.

  In the Alembic DSL this is one faust call with the ~_ recursion:

      (%in*(1.0-%cf) + _*%cf) ~ _

  The _  inside the expression is the 1-sample-delayed feedback (y[n-1]).
  The ~_ makes the expression recursive.  This is the ~_ operator finally
  named as what it has been throughout Chapter 5: a memory of the past.

  Parameters
  ----------
  :coeff — balance coefficient a (0.0–0.999; default 0.5)
           0.0 = transparent passthrough; 0.999 ≈ strong LP / near-integrator

  Audio inputs
  ------------
  audio-in 0: in — signal to filter

  Outputs
  -------
  :out — lowpass filtered output

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_out
        with {
          n_cf  = hslider(\"coeff\", 0.5, 0.0, 0.999, 0.001);
          n_out = (n0*(1.0-n_cf)+_*n_cf)~_;
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! onepole-basic
  {:params {:coeff {:range [0.0 0.999] :default 0.5}}}
  (let [in    (audio-in)
        coeff (param :coeff)
        out   (faust "(%{in}*(1.0-%{cf})+_*%{cf})~_" {:in in :cf coeff})]
    (output :out out)))
