; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.74-chaos-tempo-nonrobotic
  "GSOT pp.119-120 — chaos.tempo.nonrobotic.maxpat.

  'Balancing order and unpredictability' — adding natural looseness to a
  tempo clock by driving its phase increment with a chaotic signal.

  Concept (pp.119-120)
  ---------------------
  A perfectly metronomic clock sounds robotic because it is deterministic
  and periodic.  True human performance is neither: beat durations vary in
  ways that are bounded, correlated across successive beats, and never
  exactly repeating — properties shared by chaotic dynamical systems.

  GSOT uses the 3D Lorenz/Liu-Chen attractor (from go-chaos, example 72)
  as the chaos source.  The x output is autolimited and added as a small
  modulation to the phasor rate.  This makes the clock speed up or slow
  down continuously in an aperiodic but bounded way.

  Alembic implementation: logistic map
  -------------------------------------
  The 3D attractors from examples 69-73 require external feedback cables
  for self-oscillation (Faust cannot express their mutual coupling as a
  single ~ node).  The logistic map is 1D chaos expressible with a single
  ~ feedback register:

      x[n] = r × x[n−1] × (1 − x[n−1])

  With r ∈ (3.57, 4.0) the map is chaotic: bounded, aperiodic, sensitive
  to initial conditions.  At r=4.0 the map is fully-developed chaos on
  (0,1); at r=3.57 it transitions from periodic to chaotic.  The
  max(_, 0.01) guards the initial 0-state (a fixed point) and provides
  a non-zero seed; once the orbit leaves 0 it plays no role.

  Signal flow
  -----------
      chaos[n]  = r × max(prev, 0.01) × (1 − max(prev, 0.01))  [~ feedback]
      centered  = 2 × chaos − 1                                  [→ (−1, 1)]
      rate[n]   = max(0, (bpm + amount × centered) / 60 / SR)
      phase[n]  = frac(phase[n−1] + rate[n])                    [~ feedback]
      trig[n]   = float(phase[n] < phase[n−1])                  [wrap detect]

  The `amount` parameter sets the maximum BPM deviation from the nominal
  tempo.  With bpm=120 and amount=3 the clock varies between 117 and 123 BPM
  in a chaotic, never-repeating pattern.  `max(0, rate)` ensures the phasor
  never runs backwards if amount > bpm.

  Parameters
  ----------
  :bpm    — nominal tempo (default 120 BPM)
  :amount — maximum chaos-induced BPM deviation (default 3.0 BPM)
  :r      — logistic map rate parameter (default 3.9, range 3.0–4.0)
             3.0 → period-2 cycle (tight, periodic looseness)
             3.45 → period-4
             3.54 → period-8
             > 3.57 → chaos; 3.9 is deep chaotic, 4.0 is fully developed

  Outputs
  -------
  :phase — chaotic phasor in [0,1), useful for modulating amplitude envelopes
  :trig  — 1-sample pulse on each beat (fires when phase wraps)

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp = n_ph, n_tr
        with {
          n_bp = hslider(\"bpm\",    120.0,  20.0, 300.0, 0.01);
          n_am = hslider(\"amount\",   3.0,   0.0,  20.0, 0.01);
          n_rr = hslider(\"r\",        3.9,   3.0,   4.0, 0.001);
          n_ch = (n_rr*max(_,0.01)*(1.0-max(_,0.01)))~_;
          n_rt = max(0.0,(n_bp+n_am*(2.0*n_ch-1.0))/60.0/float(ma.SR));
          n_ph = (ma.frac(_+n_rt))~_;
          n_tr = float(n_ph<n_ph@1);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! chaos-tempo-nonrobotic
  {:params {:bpm    {:range [20.0  300.0] :default 120.0}
            :amount {:range [0.0   20.0]  :default 3.0}
            :r      {:range [3.0   4.0]   :default 3.9}}}
  (let [bpm    (param :bpm)
        amount (param :amount)
        r      (param :r)
        ; Logistic map — chaotic orbit in (0,1); max(_, 0.01) seeds from 0
        chaos  (faust "(%{rr}*max(_,0.01)*(1.0-max(_,0.01)))~_" {:rr r})
        ; Rate: nominal BPM ± amount, converted to phase-per-sample
        rate   (faust "max(0.0,(%{bp}+%{am}*(2.0*%{ch}-1.0))/60.0/float(ma.SR))"
                      {:bp bpm :am amount :ch chaos})
        ; Wrapped phasor driven by chaotic rate
        phase  (faust "(ma.frac(_+%{rt}))~_" {:rt rate})
        ; Trigger fires for one sample when phasor wraps (phase decreases)
        trig   (faust "float(%{ph}<%{ph}@1)" {:ph phase})]
    (output :phase phase)
    (output :trig  trig)))
