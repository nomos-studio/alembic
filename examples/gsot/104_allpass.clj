; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.104-allpass
  "GSOT pp.175 — go.allpass.gendsp (Chapter 6).

  'Allpass filters'
  -----------------
  A first-order allpass filter: flat amplitude response at all frequencies,
  frequency-dependent phase shift.

  Transfer function
  -----------------
      H(z) = (a + z⁻¹) / (1 + a·z⁻¹)

  |H(e^jω)| = 1 for all ω — the allpass passes all frequencies at equal
  amplitude.  The phase response is:

      φ(ω) = −2·arctan(a·sin(ω) / (1 + a·cos(ω)))

  At a=0: pure 1-sample delay (φ = −ω, linear phase).
  As |a| → 1: the phase shift approaches −π at DC or Nyquist.

  Difference equation
  -------------------
      y[n] = a·x[n] + x[n-1] − a·y[n-1]

  Two state elements:
    x[n-1] — 1-sample delay of the INPUT   (Faust @1 operator)
    y[n-1] — 1-sample delay of the OUTPUT  (~_ feedback register)

  This is the standard direct-form-I first-order allpass.

  Context: recentering and dcblock
  ---------------------------------
  GSOT pp.173-175 introduce dcblocking (removing DC / recentering a signal
  around zero) as a prerequisite for understanding allpass filters.  A DC
  blocker is a one-pole HP that nulls z=1:

      dcblock:  y[n] = x[n] − x[n-1] + R·y[n-1]    (R ≈ 0.995)

  The allpass generalises this idea: instead of a null at DC, the allpass
  uses a real pole and zero that cancel in magnitude, producing a flat
  amplitude response with all phase shift concentrated at a tunable frequency.

  Musical applications
  --------------------
  Phaser: chain of allpass filters, mix with dry signal; the phase cancellations
          create the characteristic comb-like notches.
  Reverb: allpass diffusion loops in the Schroeder/Moorer reverb topology.
  Tone:   subtle phase colouration added to any signal.

  Two-node implementation
  -----------------------
  Alembic splits the two delay elements explicitly:

      x-del = %in@1           ; x[n-1] via Faust @1
      out   = (%cf*%in + %xd - _*%cf) ~ _   ; full allpass recursion

  The ~_ feedback register holds y[n-1]; @1 on the input signal holds x[n-1].
  Both are 1-sample delays, but @1 is a feedforward delay while ~_ is the
  feedback delay — they require separate operators in Faust.

  Parameters
  ----------
  :coeff — allpass coefficient a (−0.99–0.99; default 0.5)
           Positive a: positive phase shift at low frequencies.
           Negative a: positive phase shift at high frequencies.
           |a| → 1: maximum phase shift, approaching ±π.

  Audio inputs
  ------------
  audio-in 0: in — signal to phase-shift

  Outputs
  -------
  :out — allpass filtered signal; same amplitude as input, shifted phase

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_out
        with {
          n_cf  = hslider(\"coeff\", 0.5, -0.99, 0.99, 0.01);
          n_xd  = n0@1;
          n_out = (n_cf*n0+n_xd-_*n_cf)~_;
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! allpass
  {:params {:coeff {:range [-0.99 0.99] :default 0.5}}}
  (let [in    (audio-in)
        coeff (param :coeff)
        ; x[n-1]: feedforward 1-sample delay via Faust @1 operator
        x-del (faust "%in@1" {:in in})
        ; y[n] = a*x[n] + x[n-1] - a*y[n-1]; ~_ holds y[n-1]
        out   (faust "(%cf*%in+%xd-_*%cf)~_" {:cf coeff :in in :xd x-del})]
    (output :out out)))
