; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.51-sigmoid-enveloped
  "GSOT p.86 — bipolar_waveshaping_sigmoids_enveloped.maxpat.

  'Audio waveshaping — enveloped sigmoids' (Chapter 3)
  -----------------------------------------------------
  Extends the sigmoid waveshaping vocabulary (example 50) by making the
  saturation amount time-varying: an envelope signal [0,1] continuously
  morphs between the clean input and the fully driven sigmoid output.

  Signal flow
  -----------
  Two audio inputs:
    x   — bipolar signal to waveshape (e.g. oscillator output)
    env — envelope [0,1] (e.g. from window function → phasor, ADSR, LFO)

  A fixed :drive parameter sets the maximum saturation when env=1.
  The output is a wet/dry crossfade:

      out = (1−env)·x  +  env·sigmoid(k·x)

  where k = :drive.

  At env=0: clean signal passes through unchanged.
  At env=1: fully driven sigmoid output.
  Between: linear blend — the timbre evolves continuously with the envelope.

  This pattern connects directly to the window function vocabulary (examples
  45-46): a phasor → triangle → hann output drives `env` here, creating a
  grain-shaped distortion envelope.  The same sigmoid drive can be applied
  per-grain with different envelopes.

  Why not drive·env·x?
  --------------------
  Driving with k·env·x collapses to silence at env=0 (sigmoid(0)=0), not
  the dry signal.  The wet/dry blend preserves signal continuity across the
  full envelope range, which is essential for musical use in additive or
  granular contexts.

  Three sigmoid flavors (spanning the knee hardness range)
  ---------------------------------------------------------
  :tanh      — 1−2/(1+exp(2kx))      softened, fastest to ±1
  :atan      — (2/π)·atan(kx)        softest knee, never fully clips
  :softclip  — cubic+hard, C¹ at ±1  hardest knee before full hard clip

  For logistic and sigmoid2 enveloped, substitute the wet formula from
  example 50 — the blend structure is identical.

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      n0 = hslider(\"drive\", 4.0, 1.0, 16.0, 0.0015);

      alembic_dsp(n1, n2) = n7, n8, n9
        with {
          n3 = n0*n1;
          n4 = exp(2.0*n3);
          n5 = 1.0-2.0/(1.0+n4);
          n6 = (2.0/ma.PI)*atan(n3);
          n7 = (1.0-n2)*n1 + n2*n5;
          n8 = (1.0-n2)*n1 + n2*n6;
          n9 = (1.0-n2)*n1 + n2*select2(float(abs(n3)>1.0)>0.5,
                                         1.5*n3-0.5*n3*n3*n3,
                                         max(-1.0,min(1.0,n3)));
        };
      process = alembic_dsp;

  n0 = drive param
  n1 = x      (signal audio-in)
  n2 = env    (envelope audio-in)
  n3 = kx     (k·x, shared driven signal)
  n4 = exp(2kx)  (tanh intermediate)
  n5 = tanh wet
  n6 = atan wet
  n7..n9 = blended outputs"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! sigmoids-enveloped
  {:params {:drive {:range [1.0 16.0] :default 4.0}}}
  (let [x    (audio-in)  ; bipolar signal
        env  (audio-in)  ; envelope [0,1] — controls wet/dry blend
        k    (param :drive)
        kx   (faust "%kk*%xx"       {:kk k :xx x})
        ; wet signals at full drive ─────────────────────────────────────────
        e2kx     (faust "exp(2.0*%kx)"                          {:kx kx})
        wet-tanh (faust "1.0-2.0/(1.0+%ex)"                     {:ex e2kx})
        wet-atan (faust "(2.0/ma.PI)*atan(%kx)"                 {:kx kx})
        wet-soft (faust "select2(float(abs(%kx)>1.0)>0.5,1.5*%kx-0.5*%kx*%kx*%kx,max(-1.0,min(1.0,%kx)))"
                        {:kx kx})
        ; blend: (1-env)·x + env·wet ─────────────────────────────────────
        tanh-out (faust "(1.0-%ev)*%xx+%ev*%wt" {:ev env :xx x :wt wet-tanh})
        atan-out (faust "(1.0-%ev)*%xx+%ev*%wa" {:ev env :xx x :wa wet-atan})
        soft-out (faust "(1.0-%ev)*%xx+%ev*%ws" {:ev env :xx x :ws wet-soft})]
    (output :tanh     tanh-out)
    (output :atan     atan-out)
    (output :softclip soft-out)))
