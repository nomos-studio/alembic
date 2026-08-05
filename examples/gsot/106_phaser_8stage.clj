; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.106-phaser-8stage
  "GSOT pp.177 — phaser-8stage.maxpat (Chapter 6).

  'Phaser — 8 stages'
  -------------------
  Eight first-order allpass filters in series, mixed with the dry signal.
  Doubles the stage count of phaser-4stage (example 105) for deeper, denser
  notch structure and a richer phasing character.

  Architecture
  ------------
  Eight stages chained, all sharing the same coefficient:

      s1 = allpass(a, in)
      s2 = allpass(a, s1)
      ...
      s8 = allpass(a, s7)
      out = in*(1−mix) + s8*mix

  8 stages = 8 independent ~_ registers + 8 @1 delays = 16 state elements.

  Notch count
  -----------
  With 8 stages: 4 notches per sweep (vs 2 for the 4-stage phaser).  The
  additional notches create the characteristic thick, multi-layered sweep
  of vintage 8-stage phasers (Mu-Tron Bi-Phase, Maestro PS-1, etc.).

  Relationship to phaser-4stage (example 105)
  --------------------------------------------
  Identical architecture, extended with 4 additional allpass stages s5–s8.
  The final dry/wet mix output takes the s8 wet signal instead of s4.
  Each additional pair of stages adds one notch to the frequency response.

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
          n_cf  = hslider(\"coeff\",  0.0, -0.99, 0.99, 0.01);
          n_mx  = hslider(\"mix\",    0.5,  0.0,  1.0,  0.01);
          n_xd1 = n0@1;
          n_s1  = (n_cf*n0  +n_xd1-_*n_cf)~_;
          n_xd2 = n_s1@1;
          n_s2  = (n_cf*n_s1+n_xd2-_*n_cf)~_;
          n_xd3 = n_s2@1;
          n_s3  = (n_cf*n_s2+n_xd3-_*n_cf)~_;
          n_xd4 = n_s3@1;
          n_s4  = (n_cf*n_s3+n_xd4-_*n_cf)~_;
          n_xd5 = n_s4@1;
          n_s5  = (n_cf*n_s4+n_xd5-_*n_cf)~_;
          n_xd6 = n_s5@1;
          n_s6  = (n_cf*n_s5+n_xd6-_*n_cf)~_;
          n_xd7 = n_s6@1;
          n_s7  = (n_cf*n_s6+n_xd7-_*n_cf)~_;
          n_xd8 = n_s7@1;
          n_s8  = (n_cf*n_s7+n_xd8-_*n_cf)~_;
          n_out = n0*(1.0-n_mx)+n_s8*n_mx;
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! phaser-8stage
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
        xd5   (faust "%{s4}@1" {:s4 s4})
        s5    (faust "(%{cf}*%{s4}+%{xd}-_*%{cf})~_" {:cf coeff :s4 s4 :xd xd5})
        xd6   (faust "%{s5}@1" {:s5 s5})
        s6    (faust "(%{cf}*%{s5}+%{xd}-_*%{cf})~_" {:cf coeff :s5 s5 :xd xd6})
        xd7   (faust "%{s6}@1" {:s6 s6})
        s7    (faust "(%{cf}*%{s6}+%{xd}-_*%{cf})~_" {:cf coeff :s6 s6 :xd xd7})
        xd8   (faust "%{s7}@1" {:s7 s7})
        s8    (faust "(%{cf}*%{s7}+%{xd}-_*%{cf})~_" {:cf coeff :s7 s7 :xd xd8})
        out   (faust "%{in}*(1.0-%{mx})+%{s8}*%{mx}" {:in in :s8 s8 :mx mix})]
    (output :out out)))
