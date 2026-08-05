; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.105-phaser-4stage
  "GSOT pp.176 — phaser-4stage.maxpat (Chapter 6).

  'Phaser — 4 stages'
  -------------------
  Four first-order allpass filters in series, mixed with the dry signal.
  The phase-shifted wet signal cancels the dry signal at specific frequencies,
  creating a comb of notches that sweeps as the allpass coefficient is varied.

  Architecture
  ------------
  All four stages share the same coefficient (synchronised sweep):

      s1 = allpass(a, in)
      s2 = allpass(a, s1)
      s3 = allpass(a, s2)
      s4 = allpass(a, s3)
      out = in*(1−mix) + s4*mix

  Each allpass stage: y[n] = a·x[n] + x[n-1] − a·y[n-1]

  Two delay elements per stage (same pattern as go.allpass.gendsp, example 104):
    feedforward: x[n-1] via Faust @1 operator
    feedback:    y[n-1] via ~_ register

  So 4 stages = 4 independent ~_ registers + 4 @1 delays = 8 state elements.

  Notch count and placement
  -------------------------
  A phaser with N allpass stages creates N/2 notches per octave (for 1st-order
  stages).  With 4 stages: 2 notches whose positions are controlled by the
  allpass coefficient.

  The notch frequencies fk are where the total accumulated phase shift of the
  4-stage chain equals π (mod 2π), causing destructive cancellation with the
  dry signal.

  Sweeping the coefficient
  ------------------------
  Connect an LFO output to the :coeff input to create the classic phasing sweep.
  The coefficient maps approximately to frequency as:

      a ≈ (1 − 2πfc/SR) / (1 + 2πfc/SR)   (bilinear transform approximation)

  Negative a: notches near Nyquist.
  Positive a: notches near DC.
  Sweeping a from −0.9 to 0.9 sweeps notches from Nyquist to DC.

  Relationship to go.allpass.gendsp (example 104)
  ------------------------------------------------
  This patch chains four copies of the example 104 allpass.  Each stage is
  an independent allpass with its own @1 feedforward and ~_ feedback register.

  Parameters
  ----------
  :coeff — allpass coefficient a (−0.99–0.99; default 0.0); sweep with LFO
  :mix   — dry/wet balance (0.0=dry → 1.0=wet; default 0.5)

  Audio inputs
  ------------
  audio-in 0: in — signal to phase-shift

  Outputs
  -------
  :out — phased output

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_out
        with {
          n_cf = hslider(\"coeff\",  0.0, -0.99, 0.99, 0.01);
          n_mx = hslider(\"mix\",    0.5,  0.0,  1.0,  0.01);
          n_xd1 = n0@1;
          n_s1  = (n_cf*n0  +n_xd1-_*n_cf)~_;
          n_xd2 = n_s1@1;
          n_s2  = (n_cf*n_s1+n_xd2-_*n_cf)~_;
          n_xd3 = n_s2@1;
          n_s3  = (n_cf*n_s2+n_xd3-_*n_cf)~_;
          n_xd4 = n_s3@1;
          n_s4  = (n_cf*n_s3+n_xd4-_*n_cf)~_;
          n_out = n0*(1.0-n_mx)+n_s4*n_mx;
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! phaser-4stage
  {:params {:coeff {:range [-0.99 0.99] :default 0.0}
            :mix   {:range [0.0  1.0]   :default 0.5}}}
  (let [in    (audio-in)
        coeff (param :coeff)
        mix   (param :mix)
        xd1   (faust "%{in}@1" {:in in})
        s1    (faust "(%{cf}*%{in}+%{xd}-_*%{cf})~_" {:cf coeff :in in :xd xd1})
        xd2   (faust "%{s1}@1" {:s1 s1})
        s2    (faust "(%{cf}*%{s1}+%{xd}-_*%{cf})~_" {:cf coeff :s1 s1 :xd xd2})
        xd3   (faust "%{s2}@1" {:s2 s2})
        s3    (faust "(%{cf}*%{s2}+%{xd}-_*%{cf})~_" {:cf coeff :s2 s2 :xd xd3})
        xd4   (faust "%{s3}@1" {:s3 s3})
        s4    (faust "(%{cf}*%{s3}+%{xd}-_*%{cf})~_" {:cf coeff :s3 s3 :xd xd4})
        out   (faust "%{in}*(1.0-%{mx})+%{s4}*%{mx}" {:in in :s4 s4 :mx mix})]
    (output :out out)))
