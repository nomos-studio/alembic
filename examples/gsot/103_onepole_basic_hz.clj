; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.103-onepole-basic-hz
  "GSOT pp.173 — go.onepole.basic_hz.gendsp (Chapter 6).

  'One-pole lowpass filter — frequency parameterization'
  -------------------------------------------------------
  Frequency-parameterized variant of go.onepole.basic (example 100).  Instead
  of the raw balance coefficient a ∈ [0,1), the user supplies a cutoff frequency
  in Hz and the coefficient is computed from the sample rate.

  Coefficient from frequency
  --------------------------
  For a one-pole IIR lowpass with −3 dB cutoff at fc (Hz):

      a = exp(−2π × fc / ma.SR)

  ma.SR is Faust's compile-time sample rate constant.  The coefficient a moves
  the single real pole to z = a on the positive real axis.

  The filter equation is unchanged from go.onepole.basic:

      y[n] = (1 − a) × x[n] + a × y[n-1]

  Frequency ↔ coefficient correspondence (48 kHz)
  -----------------------------------------------
  fc        a           character
  ──────────────────────────────────────────────
  20000 Hz  0.083       near-transparent highshelf
   8000 Hz  0.351       gentle LP
   1000 Hz  0.877       typical LP colour
    100 Hz  0.987       strong HP-blocking / bass-only
     10 Hz  0.9987      near-integrator
      1 Hz  0.99987     very near-integrator

  As fc → 0 the coefficient approaches 1 (pure past, integrator limit).
  As fc → Nyquist the coefficient approaches 0 (pure present, identity).

  Relationship to go.onepole.basic (example 100)
  -----------------------------------------------
  go.onepole.basic exposes the raw coefficient — useful for understanding the
  balance-of-time concept and for coefficient-scheduled filtering.

  go.onepole.basic_hz maps to an audible frequency range — more useful when
  patching by ear or matching a design specification.  Both compute the same
  filter; only the interface differs.

  The two-node pattern
  --------------------
  In Alembic the coefficient conversion and the filter are separate faust calls:

      coeff = exp(-2.0*ma.PI*%hz/ma.SR)     ; Hz → coefficient
      out   = (%in*(1.0-%cf)+_*%cf)~_       ; one-pole LP (same as ex.100)

  This avoids computing the exp() twice (once for the input weight, once for
  the feedback weight) and makes each node's purpose explicit.

  Parameters
  ----------
  :hz — cutoff frequency in Hz (1–20000; default 1000)

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
          n_hz  = hslider(\"hz\", 1000.0, 1.0, 20000.0, 1.0);
          n_cf  = exp(-2.0*ma.PI*n_hz/ma.SR);
          n_out = (n0*(1.0-n_cf)+_*n_cf)~_;
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! onepole-basic-hz
  {:params {:hz {:range [1.0 20000.0] :default 1000.0}}}
  (let [in    (audio-in)
        freq  (param :hz)
        coeff (faust "exp(-2.0*ma.PI*%{hz}/ma.SR)" {:hz freq})
        out   (faust "(%{in}*(1.0-%{cf})+_*%{cf})~_" {:in in :cf coeff})]
    (output :out out)))
