; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.101-vactrol
  "GSOT pp.169-170 — go.vactrol.gendsp (Chapter 6).

  'A lowpass gate (LPG)'
  ----------------------
  Simulates the electro-optical response of a vactrol (LED + LDR in a sealed
  package) used in analogue lowpass gate circuits.

  Physical model
  --------------
  In hardware a vactrol LPG works as follows:
    1. A control voltage drives an LED.
    2. The LED illuminates a light-dependent resistor (LDR / photocell).
    3. The LDR forms part of an RC lowpass filter in the signal path.
    4. Low LDR resistance (LED bright / gate open) → short time constant → high cutoff.
    5. High LDR resistance (LED dim / gate closed) → long time constant → low cutoff.

  The LDR also controls amplitude because at high resistance very little signal
  passes through the RC network before the output stage.  This coupling of VCF
  and VCA from a single control source is the characteristic LPG sound.

  Two-stage DSP model
  --------------------
  Stage 1 — Asymmetric envelope follower (the 'LED + LDR' response):

      rate  = select2(cv > env, :release, :attack)     ; slow release, fast attack
      env   = (env + rate × (cv − env)) ~ _

  Stage 2 — Time-varying one-pole LP + VCA:

      filter_coeff = (1 − env) × 0.999               ; never reach 1 → always drains
      filtered     = (audio + _prev × filter_coeff) ~ _
      out          = filtered × env                   ; amplitude = env

  Coefficient ceiling
  -------------------
  The 0.999 ceiling on the filter coefficient ensures the LP register always
  decays even when env ≈ 0 (gate fully closed).  Without it the one-pole would
  be a pure integrator (pole at z=1) that never drains when the gate closes.

  Characteristic response
  -----------------------
  :attack → fast (LED-on response is quick; the LDR resistance drops rapidly)
  :release → slow (LED-off response is sluggish; the LDR resistance rises slowly)

  Typical physical vactrol times at 48 kHz:
    attack  ≈ 1–5 ms   → rate ≈ 0.01–0.05
    release ≈ 50–300ms → rate ≈ 0.0001–0.002

  At these settings the LPG produces the characteristic 'boing' or 'thwack'
  envelope: near-instant attack, long tail with simultaneous filter close.

  Dual control (CV + audio)
  -------------------------
  audio-in 0 (n0): cv    — gate or CV signal [0,1]; envelope follower input
  audio-in 1 (n1): audio — signal to gate/filter

  Parameters
  ----------
  :attack  — envelope attack rate (0.001–1.0; default 0.02 ≈ 1 ms at 48 kHz)
  :release — envelope release rate (0.0001–0.1; default 0.001 ≈ 20 ms at 48 kHz)

  Outputs
  -------
  :out     — gated + filtered audio
  :env     — envelope signal [0,1]; useful for modulation or metering

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0,n1) = n_out, n_ev
        with {
          n_at = hslider(\"attack\",  0.02,  0.001, 1.0,  0.001);
          n_rl = hslider(\"release\", 0.001, 0.0001, 0.1, 0.0001);
          n_ev = (select2(n0>_,n_rl,n_at)*(n0-_)+_)~_;
          n_fl = (n1+_*(1.0-n_ev)*0.999)~_;
          n_out = n_fl*n_ev;
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! vactrol
  {:params {:attack  {:range [0.001 1.0]    :default 0.02}
            :release {:range [0.0001 0.1]   :default 0.001}}}
  (let [cv     (audio-in)
        audio  (audio-in)
        atk    (param :attack)
        rel    (param :release)
        ; Asymmetric envelope: fast attack (LED-on), slow release (LED-off)
        env    (faust "(select2(%cv>_,%rl,%at)*(%cv-_)+_)~_"
                      {:cv cv :at atk :rl rel})
        ; One-pole LP with env-controlled coefficient; 0.999 ceiling ensures drain
        filter (faust "(%au+_*(1.0-%ev)*0.999)~_" {:au audio :ev env})
        ; VCA: amplitude = env (same source couples VCF and VCA)
        out    (faust "%fl*%ev" {:fl filter :ev env})]
    (output :out out)
    (output :env env)))
