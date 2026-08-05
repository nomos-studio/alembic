; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.38-lfo-multi
  "GSOT p.69 — go.lfo.multi.gendsp: all unit shapers applied to one ramp.

  'From ramps to LFOs' (Chapter 3)
  ----------------------------------
  A cyclic ramp is already a complete description of phase — what the unit
  shapers add is the transfer function from phase to amplitude.  Apply any
  shaper and you get a shaped oscillator.  Apply all of them simultaneously
  and you get a multi-output LFO bank: one ramp in, eleven waveforms out,
  all phase-coherent.

  go.lfo.multi.gendsp is exactly that: processor form (audio-in provides the
  ramp), eleven named outputs, parameterised shapers at fixed musical defaults.

  Fixed defaults for parameterised shapers:
    trapezoid  r=0.25  s=0.5  f=0.75  — symmetric rise/hold/fall
    kink       k=0.67              — classic triplet-swing ratio (2:1)
    pow        p=2.0               — quadratic ease-in
    logistic   k=10.0              — steep sigmoid
    ease-exp   k=3.0               — moderate exponential ease-in
    tukey      alpha=0.5           — equal taper and flat top

  Signal flow:
      ramp (audio-in, [0,1))
        ├── triangle   1 - |2x - 1|
        ├── trapezoid  piecewise linear rise-hold-fall
        ├── kink       slope-change at midpoint
        ├── lfo        0.5·(1 - cos(2π·x))   raised cosine
        ├── pow        x²
        ├── arc        √(x·(2-x))
        ├── cubic      3x² - 2x³
        ├── logistic   1/(1 + exp(-10·(x-0.5)))
        ├── ease-exp   (exp(3x)-1)/(exp(3)-1)
        ├── welch      4x·(1-x)
        └── tukey      cosine-tapered flat-top window

  All outputs are [0,1) and phase-coherent — they share the same input ramp,
  so they all complete one cycle per period and can be mixed, subtracted, or
  multiplied without drift.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n7, n8, n9, n10, n11, n12, n19, n20, n21, n26, n27
        with {
          n1 = 1.0;
          n2 = 2.0;
          n3 = (n0 * n2);
          n4 = 1.0;
          n5 = (n3 - n4);
          n6 = abs(n5);
          n7 = (n1 - n6);
          n8 = select2(n0 >= 0.75, select2(n0 >= 0.5, select2(n0 >= 0.25, 1.0, n0 / 0.25), (0.75 - n0) / (0.75 - 0.5)), 0.0);
          n9 = select2(n0 >= 0.67, n0 / max(0.67, 0.0001) * 0.5, 0.5 + (n0 - 0.67) / max(1.0 - 0.67, 0.0001) * 0.5);
          n10 = 0.5*(1.0-cos(2.0*ma.PI*n0));
          n11 = pow(max(0.0, n0), 2.0);
          n12 = sqrt(max(0.0, n0 * (2.0 - n0)));
          n13 = (n0 * n0);
          n14 = (n13 * n0);
          n15 = 3.0;
          n16 = (n15 * n13);
          n17 = 2.0;
          n18 = (n17 * n14);
          n19 = (n16 - n18);
          n20 = 1.0 / (1.0 + exp(-10.0 * (n0 - 0.5)));
          n21 = (exp(3.0 * n0) - 1.0) / (exp(3.0) - 1.0);
          n22 = 4.0;
          n23 = (n22 * n0);
          n24 = 1.0;
          n25 = (n24 - n0);
          n26 = (n23 * n25);
          n27 = select2(n0 >= (1.0 - 0.5/2.0), select2(n0 >= (0.5/2.0), 1.0, 0.5*(1.0-cos(2.0*ma.PI*n0/max(0.5,0.0001)))), 0.5*(1.0-cos(2.0*ma.PI*(1.0-n0)/max(0.5,0.0001))));
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! lfo-multi
  {}
  (let [x        (audio-in)
        triangle (sub (const 1.0)
                      (abs (sub (mul x (const 2.0)) (const 1.0))))
        trapezoid (faust "select2(%{x} >= 0.75, select2(%{x} >= 0.5, select2(%{x} >= 0.25, 1.0, %{x} / 0.25), (0.75 - %{x}) / (0.75 - 0.5)), 0.0)"
                         {:x x})
        kink     (faust "select2(%{x} >= 0.67, %{x} / max(0.67, 0.0001) * 0.5, 0.5 + (%{x} - 0.67) / max(1.0 - 0.67, 0.0001) * 0.5)"
                        {:x x})
        lfo      (faust "0.5*(1.0-cos(2.0*ma.PI*%{x}))"
                        {:x x})
        pow      (faust "pow(max(0.0, %{x}), 2.0)"
                        {:x x})
        arc      (faust "sqrt(max(0.0, %{x} * (2.0 - %{x})))"
                        {:x x})
        x2       (mul x x)
        x3       (mul x2 x)
        cubic    (sub (mul (const 3.0) x2) (mul (const 2.0) x3))
        logistic (faust "1.0 / (1.0 + exp(-10.0 * (%{x} - 0.5)))"
                        {:x x})
        ease-exp (faust "(exp(3.0 * %{x}) - 1.0) / (exp(3.0) - 1.0)"
                        {:x x})
        welch    (mul (mul (const 4.0) x) (sub (const 1.0) x))
        tukey    (faust "select2(%{x} >= (1.0 - 0.5/2.0), select2(%{x} >= (0.5/2.0), 1.0, 0.5*(1.0-cos(2.0*ma.PI*%{x}/max(0.5,0.0001)))), 0.5*(1.0-cos(2.0*ma.PI*(1.0-%{x})/max(0.5,0.0001))))"
                        {:x x})]
    (output :triangle  triangle)
    (output :trapezoid trapezoid)
    (output :kink      kink)
    (output :lfo       lfo)
    (output :pow       pow)
    (output :arc       arc)
    (output :cubic     cubic)
    (output :logistic  logistic)
    (output :ease-exp  ease-exp)
    (output :welch     welch)
    (output :tukey     tukey)))
