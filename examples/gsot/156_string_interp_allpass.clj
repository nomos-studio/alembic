; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.156-string-interp-allpass
  "GSOT pp.223-226 — delay_interpolation_types.maxpat (Chapter 8).

  'Cleaner Interpolation for a Consistent Sound — Allpass vs. Linear vs. Integer'
  -------------------------------------------------------------------------------
  Fractional sample delay is unavoidable in a pitched string resonator: the
  desired loop length D = SR/:hz is almost never an integer.  The method used
  to implement that fractional sample determines whether the string's timbre
  stays consistent across pitches.

  The three interpolation strategies
  ------------------------------------
  1. Nearest / truncation  (ex.151, 154):
       de.delay(maxD, int(D), x)
       No interpolation at all — the delay is rounded to the nearest integer.
       The rounding error changes the pitch by up to ±0.5 semitone at high
       frequencies.  Timbre is consistent because the error is fixed, but
       the pitch is wrong.

  2. Linear interpolation  (ex.152, 153):
       de.fdelay(maxD, D, x)
       Blends adjacent integer samples: y = (1−f)·x[n-N] + f·x[n-N-1].
       Transfer function: H_li(z) = (1−f) + f·z^{-1}
       This is a first-order FIR LP whose cutoff DEPENDS on the fractional
       part f.  At f=0: no filtering (H=1).  At f=0.5: mild −3 dB at Nyquist.
       Pitch is accurate; timbre changes with f, which changes with :hz.
       Every pitch gets a slightly different tone — the 'inconsistency' GSOT
       names.

  3. Allpass interpolation  (this patch):
       First-order allpass: H_ap(z) = (a + z^{-1}) / (1 + a·z^{-1})
       Coefficient: a = (1−f) / (1+f)
       Group delay at DC: (1−a)/(1+a) = f  (exact)
       Magnitude:         |H_ap(e^jω)| = 1  for all ω
       The allpass has flat magnitude at every frequency — it delays the signal
       by f samples without any HF rolloff.  Timbre is consistent across pitches
       because the loop's only LP contribution is the fixed string-body averaging
       filter, not an f-dependent artefact.

  Allpass coefficient derivation
  --------------------------------
  Target group delay f samples at DC.  For H(z) = (a+z^{-1})/(1+a·z^{-1}):
      τ_gd(0) = (1 − a²) / (1 + a)² = (1−a)/(1+a)  →  set equal to f:
      f·(1+a) = 1−a  →  a = (1−f)/(1+f)

  At f=0: a=1, H(z)=1 (identity — integer delay degenerates cleanly).
  At f=0.5: a=1/3, τ_gd(0)=0.5.
  At f→1: a→0, H(z)=z^{-1} (pure unit delay).

  Loop delay accounting
  ----------------------
  Total loop delay = 1 (~ _ implicit) + f (allpass DC group delay) + D_int
  where D_int = floor(SR/:hz) − 1.
  Total = 1 + f + floor(D) − 1 = f + floor(D) = D = SR/:hz  ✓

  In Faust:
      dl  = max(1.0, ma.SR/:hz)          — float D
      df  = dl − int(dl)                 — fractional part f ∈ [0,1)
      di  = max(0, int(dl) − 1)         — integer part for de.delay
      ap  = (1.0 − df) / (1.0 + df)    — allpass coefficient

  The inner `apn ~ _` loop implements the allpass state:
      apn(p) = ap·(s − p) + s@1
             = ap·x[n] + x[n-1] − ap·y[n-1]   (standard 1st-order allpass)

  Comparison summary
  -------------------
  Method       Pitch accuracy   Magnitude          Consistency
  -----------  ---------------  -----------------  ----------------
  Truncation   ±0.5 semitone    flat               good (fixed error)
  Linear       accurate         LP (f-dependent)   poor (varies with :hz)
  Allpass      accurate         flat               good (f-independent)

  Note: linear interpolation is fine for slow or non-pitched applications
  (chorus, vibrato, delay effects).  For pitched resonators, allpass is
  strongly preferred.

  Book 2 and beyond
  ------------------
  GSOT Book 1 closes this string section here.  Book 2 promises more refined
  physical models: Thiran higher-order allpass (N-point, even flatter group
  delay), multi-string coupling, body resonance, excitation models (hammer,
  bow).  See the repository seed for the extension thread.

  Parameters
  ----------
  :hz — fundamental frequency in Hz (20–2000; default 220)
  :dc — RT60 decay time in milliseconds (1–10000; default 1000)

  Audio inputs / Outputs
  ----------------------
  in: excitation signal  →  :out: allpass-interpolated string resonator output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! string-interp-allpass
  {:params {:hz {:range [20.0 2000.0]  :default 220.0}
            :dc {:range [1.0 10000.0]  :default 1000.0}}}
  (let [in  (audio-in)
        hz  (param :hz)
        dc  (param :dc)
        fb  (faust "pow(0.001,1000.0/(%{hz}*max(1.0,%{dc})))" {:hz hz :dc dc})
        dl  (faust "max(1.0,ma.SR/%{hz})" {:hz hz})
        df  (faust "%{dl}-int(%{dl})" {:dl dl})
        di  (faust "max(0,int(%{dl})-1)" {:dl dl})
        ap  (faust "(1.0-%{df})/(1.0+%{df})" {:df df})
        out (faust "strc_loop ~ _\n  with {\n    strc_loop(s) = %{in}+%{fb}*de.delay(int(ma.SR*5.0),%{di},apf)\n      with {\n        apf = apn ~ _\n          with { apn(p) = %{ap}*(s-p)+s@1; };\n      };\n  }"
                   {:in in :fb fb :di di :ap ap})]
    (output :out out)))
