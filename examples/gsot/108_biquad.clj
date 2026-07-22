; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.108-biquad
  "GSOT pp.178-180 — go.biquad.gendsp (Chapter 6).

  'Biquad filters'
  ----------------
  Second-order IIR filter (the biquad) — the foundation for nearly every
  parametric equaliser, resonant LP/HP/BP, notch, and shelving filter in
  practical DSP.  GSOT introduces it as the second-order generalization of
  the one-pole: 2 poles + 2 zeros + direct term.

  Transfer function
  -----------------
      H(z) = (b0 + b1·z⁻¹ + b2·z⁻²) / (1 + a1·z⁻¹ + a2·z⁻²)

  Numerator (b-coefficients) → zeros (shape the frequency spectrum).
  Denominator (a-coefficients) → poles (resonance, stability).
  At a1=a2=0: FIR (non-recursive), purely determined by b0/b1/b2.
  At b1=b2=0: degenerate IIR (one feedforward + two feedback taps).

  Difference equation
  -------------------
      y[n] = b0·x[n] + b1·x[n-1] + b2·x[n-2]
                      − a1·y[n-1] − a2·y[n-2]

  Four delay elements — same as gen~ history operators in the original patch:
    x[n-1], x[n-2]: feedforward delays (input history)
    y[n-1], y[n-2]: feedback delays   (output history)

  Stability
  ---------
  The biquad is stable iff its poles lie inside the unit circle:

      |a2| < 1
      |a1| < 1 + a2

  Exceeding these bounds causes the filter to grow without bound (infinite
  output). The default coefficients (b0=1, rest=0) are always stable
  (identity pass-through).

  Direct Form II implementation
  -----------------------------
  Instead of maintaining four separate delay nodes (x1, x2, y1, y2),
  Direct Form II collapses the feedback into a single state signal w:

      w[n]  = x[n] − a1·w[n-1] − a2·w[n-2]         ; denominator part
      y[n]  = b0·w[n] + b1·w[n-1] + b2·w[n-2]        ; numerator part

  This halves the number of state registers (2 instead of 4), which is
  why real implementations (including gen~'s biquad operator) prefer it.

  In Faust, the w recursion can be expressed with ~_ for w[n-1] and _@1
  for w[n-2] (the feedback signal delayed one additional sample).  The
  output node then uses w@1 and w@2 for the numerator feedforward taps:

      w   = (in − a1·_ − a2·_@1) ~ _
      out = b0·w + b1·w@1 + b2·w@2

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_out
        with {
          n_b0  = hslider(\"b0\",  1.0, -2.0, 2.0, 0.01);
          n_b1  = hslider(\"b1\",  0.0, -2.0, 2.0, 0.01);
          n_b2  = hslider(\"b2\",  0.0, -2.0, 2.0, 0.01);
          n_a1  = hslider(\"a1\",  0.0, -2.0, 2.0, 0.01);
          n_a2  = hslider(\"a2\",  0.0, -2.0, 2.0, 0.01);
          n_w   = (n0-n_a1*_-n_a2*_@1)~_;
          n_out = n_b0*n_w+n_b1*n_w@1+n_b2*n_w@2;
        };
      process = alembic_dsp;

  Coefficient recipes (Audio EQ Cookbook — fc = cutoff, Q = resonance)
  -----------------------------------------------------------------------
  Lowpass:
    w0 = 2π·fc/SR;  α = sin(w0)/(2Q)
    b0 = b2 = (1−cos(w0))/2;  b1 = 1−cos(w0)
    a0 = 1+α;  a1 = −2cos(w0)/a0;  a2 = (1−α)/a0
    (b0, b1, b2 also divided by a0)

  Highpass:
    same w0, α; b0=b2=(1+cos(w0))/2; b1=−(1+cos(w0))
    same a0, a1, a2

  Bandpass:
    b0 = sin(w0)/2 = Q·α;  b1 = 0;  b2 = −b0
    same a0, a1, a2

  Notch:
    b0=b2=1;  b1=−2cos(w0);  same a0, a1, a2

  Peaking EQ (gain A in dB):
    α = sin(w0)/(2Q);  A = 10^(dBgain/40)
    b0/a0 = 1+α·A;  b1/a0 = −2cos(w0);  b2/a0 = 1−α·A
    (denominator: a0=1+α/A, a1=−2cos(w0), a2=1−α/A)

  Parameters
  ----------
  :b0 — feedforward coefficient b0 (−2–2; default 1.0)
  :b1 — feedforward coefficient b1 (−2–2; default 0.0)
  :b2 — feedforward coefficient b2 (−2–2; default 0.0)
  :a1 — feedback coefficient a1    (−2–2; default 0.0)
  :a2 — feedback coefficient a2    (−2–2; default 0.0)

  Audio inputs
  ------------
  audio-in 0: in — signal to filter

  Outputs
  -------
  :out — biquad filtered signal"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! biquad
  {:params {:b0 {:range [-2.0 2.0] :default 1.0}
            :b1 {:range [-2.0 2.0] :default 0.0}
            :b2 {:range [-2.0 2.0] :default 0.0}
            :a1 {:range [-2.0 2.0] :default 0.0}
            :a2 {:range [-2.0 2.0] :default 0.0}}}
  (let [in   (audio-in)
        b0   (param :b0)
        b1   (param :b1)
        b2   (param :b2)
        a1   (param :a1)
        a2   (param :a2)
        w    (faust "(%in-%a1*_-%a2*_@1)~_" {:in in :a1 a1 :a2 a2})
        out  (faust "%b0*%ww+%b1*%ww@1+%b2*%ww@2" {:b0 b0 :b1 b1 :b2 b2 :ww w})]
    (output :out out)))
