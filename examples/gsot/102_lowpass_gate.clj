; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.102-lowpass-gate
  "GSOT pp.171 — lowpass-gate.maxpat (Chapter 6).

  'A lowpass gate (LPG)' — complete patch
  -----------------------------------------
  Top-level patch instantiating the vactrol model (example 101) in a complete
  LPG configuration with independent control over the LP filter and amplitude
  (VCA) contributions.

  Relationship to go.vactrol.gendsp (example 101)
  -------------------------------------------------
  go.vactrol.gendsp couples the filter cutoff and amplitude to the same
  envelope — they always move together, as in a true analogue vactrol.

  This patch exposes :lp-mix and :gate-mix parameters to blend the vactrol
  envelope into the filter and amplitude separately:

      filtered   = one-pole LP where coeff = (1 − env × lp-mix) × 0.999
      amplitude  = 1 − gate-mix + env × gate-mix   (lerp from 1 to env by gate-mix)
      out        = filtered × amplitude

  :lp-mix = 0, :gate-mix = 0   → bypass (audio passes unmodified)
  :lp-mix = 1, :gate-mix = 0   → LP filter only (env controls cutoff, no amplitude)
  :lp-mix = 0, :gate-mix = 1   → gate/VCA only (env controls amplitude, no filtering)
  :lp-mix = 1, :gate-mix = 1   → full LPG (coupled VCF + VCA; same as go.vactrol)

  Signal flow
  -----------
      cv  ──→ env follower (asymmetric one-pole) ──→ env
                                                       │
                    ┌──────────────────────────────────┤
                    │                                  │
                    ▼                                  ▼
               LP filter coeff                    VCA amplitude
           (1 − env×lp-mix)×0.999           1 − gate-mix + env×gate-mix
                    │                                  │
             audio ─┤                                  │
                    ▼                                  │
                filtered ──────────────────────────────┴──→ ×  ──→ out

  Envelope model
  --------------
  Same asymmetric one-pole as go.vactrol.gendsp:

      rate = select2(cv > env, :release, :attack)
      env  = (env + rate × (cv − env)) ~ _

  Outputs
  -------
  :out — processed audio
  :env — envelope [0,1]; tap for modulation or analysis

  Parameters
  ----------
  :attack   — envelope attack rate (0.001–1.0; default 0.02)
  :release  — envelope release rate (0.0001–0.1; default 0.001)
  :lp-mix   — filter coupling depth 0=no-filter → 1=full-LP (default 1.0)
  :gate-mix — amplitude coupling depth 0=no-VCA → 1=full-gate (default 1.0)

  Audio inputs
  ------------
  audio-in 0: gate  — gate or CV control [0,1]; drives the vactrol envelope
  audio-in 1: audio — signal to process

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0,n1) = n_out, n_ev
        with {
          n_at = hslider(\"attack\",   0.02, 0.001,  1.0,  0.001);
          n_rl = hslider(\"release\",  0.001, 0.0001, 0.1, 0.0001);
          n_lp = hslider(\"lp-mix\",   1.0, 0.0, 1.0, 0.01);
          n_gm = hslider(\"gate-mix\", 1.0, 0.0, 1.0, 0.01);
          n_ev = (select2(n0>_,n_rl,n_at)*(n0-_)+_)~_;
          n_fc = (1.0-n_ev*n_lp)*0.999;
          n_fl = (n1+_*n_fc)~_;
          n_am = 1.0-n_gm+n_ev*n_gm;
          n_out = n_fl*n_am;
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! lowpass-gate
  {:params {:attack   {:range [0.001 1.0]   :default 0.02}
            :release  {:range [0.0001 0.1]  :default 0.001}
            :lp-mix   {:range [0.0 1.0]     :default 1.0}
            :gate-mix {:range [0.0 1.0]     :default 1.0}}}
  (let [gate    (audio-in)
        audio   (audio-in)
        atk     (param :attack)
        rel     (param :release)
        lp-mix  (param :lp-mix)
        gat-mix (param :gate-mix)
        ; Asymmetric vactrol envelope follower
        env     (faust "(select2(%gv>_,%rl,%at)*(%gv-_)+_)~_"
                       {:gv gate :at atk :rl rel})
        ; LP filter coefficient: (1 - env*lp-mix)*0.999; 0.999 ceiling ensures drain
        filter  (faust "(%au+_*(1.0-%ev*%lp)*0.999)~_"
                       {:au audio :ev env :lp lp-mix})
        ; Amplitude: lerp from unity to env by gate-mix
        out     (faust "%fl*(1.0-%gm+%ev*%gm)"
                       {:fl filter :gm gat-mix :ev env})]
    (output :out out)
    (output :env env)))
