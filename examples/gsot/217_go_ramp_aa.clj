; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.217-go-ramp-aa
  "GSOT pp.343-353 — go.ramp.aa (Chapter 10: Windows of Time).

  'Band-Limited Sawtooth with Hard Sync and Waveform Shaping'
  ------------------------------------------------------------
  go.ramp.aa is the capstone of the 'A band limited saw with hardsync' section.
  It combines three techniques, each built up through unlabelled codebox examples
  between pp.343 and pp.352:

    1. Band-limited ramp via 2-point PolyBLEP (anti-aliasing)
    2. Hard sync via resettable slave phasor (ex.202 pattern)
    3. Anti-aliased sync reset using the master's subsample fraction (ex.211 pattern)

  Then pp.352-353 ('Shaping') demonstrates applying Chapter 3 shaping operators to
  the anti-aliased output — this translation captures the sine-fold morphing shaper
  described there as the parametric :sh control.

  1. Band-limited ramp via PolyBLEP
  -----------------------------------
  A naive ramp (phasor 0→1 at :fs Hz) has a jump discontinuity at the wrap.  When
  converted to a sawtooth wave (2×ph - 1), the discontinuity aliases as high-frequency
  spectral energy that folds back into the audible range.

  The 2-point POLYNOMIAL BLEP (PolyBLEP) adds a smooth correction residual in the
  two samples adjacent to the discontinuity.  The correction is a degree-2 polynomial
  fit to the Gibbs overshoot of an ideal band-limited step.

  The correction is computed from the NORMALISED PHASE POSITIONS:
      ta = ph / inc        — position 'just after' the wrap; ta ∈ [0,1) at wrap sample
      tb = (ph − 1) / inc  — position 'just before' the wrap; tb ∈ (-1,0) at pre-wrap

  where inc = fc/SR is the per-sample phase increment.  At all other times these
  fall outside their respective ranges and the correction is zero.

      blep_after(ta)  = ta² − 2ta + 1 ... wait no:
      blep_after(ta)  = ta + ta − ta² − 1     for ta ∈ [0,1)   — negative, pushes saw UP
      blep_before(tb) = tb² + tb + tb + 1     for tb ∈ (-1,0)  — positive, pushes saw DOWN

      saw_aa = (2×ph − 1) − blep_after − blep_before

  At the wrap sample (ta≈0): blep_after ≈ -1; saw − (-1) = saw + 1, pushing the saw from -1
  toward 0.  At the pre-wrap sample (tb≈0): blep_before ≈ +1; saw − 1, pushing the saw from
  ~+1 toward 0.  The two corrections bracket the discontinuity and smooth it.

  This is equivalent to the subsample fraction formula from ex.211: at the wrap sample,
  ta = 1 − sf, and blep_after = ta² − 2ta + 1 = sf²... wait that's different.  Let me
  recalculate: ta+ta-ta²-1 with ta=1-sf gives:
    2(1-sf) - (1-sf)² - 1 = 2 - 2sf - 1 + 2sf - sf² - 1 = -sf²
  So blep_after = -sf² at the wrap sample — matching the sf-based formula.

  2. Hard sync via resettable slave phasor
  -----------------------------------------
  Same pattern as windowed-sync (ex.202): the slave phasor is a resettable accumulator
  that resets to 0.0 each time the MASTER wraps:

      trig_m  = ph_m < ph_m'              — master wrap trigger (int 0/1)
      ph_s    = (select2(trig_m, _ + fs/SR, 0.0) ~ _) : frac   — slave

  When :fm < :fs: each master period contains multiple slave cycles.  The ratio fs/fm
  sets the harmonic interval (fm=110, fs=220 → octave above, forced to reset at 110Hz).
  When :fm > :fs: the slave is reset before it completes a cycle, creating only the
  lower-frequency portion of a slave wave per master period — the characteristic
  'pinched' sync timbre.

  3. Anti-aliasing the sync reset
  ----------------------------------
  The forced slave reset from ph_s' (any value) to 0 creates a discontinuity of
  amplitude −2×ph_s' in the slave saw (in [-1,+1] normalisation).  Unlike the natural
  wrap (fixed amplitude -2), the sync reset amplitude varies cycle to cycle.

  Using the master's subsample fraction sf_m from ex.211:

      sf_m = (1 − ph_m') / max(ε, ph_m + 1 − ph_m')

  The correction at the reset sample (1−sf_m samples after the forced zero):
      sync_corr = ph_s' × sf_m²

  where ph_s' is the slave's phase BEFORE the reset (one-sample delay).
  This correction is ADDED to the saw (pushes it up from the too-low value at reset).

  The natural wrap correction is gated OFF at sync reset samples (is_nat = 1−trig_m)
  because the slave's phase-crossing at a sync reset is NOT a full-amplitude wrap —
  it is a variable-amplitude forced jump that uses the sync correction instead.

  4. Waveform shaping (pp.352-353)
  ----------------------------------
  The 'Shaping' section demonstrates substituting Chapter 3 shaping operators for
  the raw anti-aliased saw.  A parametric sine-fold morphing shaper:

      shaped = (1 − :sh) × saw_aa + :sh × sin(π × saw_aa)

  At :sh = 0: pure anti-aliased saw.
  At :sh = 0.5: blend of saw and sine fold; mid-way between the two spectra.
  At :sh = 1: sin(π × saw_aa) — a sine-fold waveshaper applied to the saw.
    The sine fold inverts the saw's harmonics: the fundamental is preserved but
    higher harmonics are progressively phased and summed into a softer waveform.

  Other Chapter 3 operators (trapezoid, saturation, polynomial fold) can be swapped
  in at the shaping stage.  The anti-aliased saw is a good input for any waveshaper
  because its spectral content is already band-limited — applying a nonlinear shaper
  introduces new harmonics but from a clean base, so the aliasing budget of the
  shaper is the only concern.

  Hard sync timbre control
  -------------------------
  The primary timbral control in hard sync synthesis is the RATIO :fs/:fm.
  The sync creates a formant at :fm: the spectral envelope peaks at :fm and the
  harmonic teeth are spaced at :fs.

  :fs/:fm = 1.0  — unison; saw wraps at same rate as master; no sync effect.
  :fs/:fm = 2.0  — octave; slave does two cycles per master; strong 2nd harmonic peak.
  :fs/:fm = 1.5  — fifth above; partial cycles per master; complex resonant spectral envelope.
  :fs/:fm = 0.5  — octave below; slave resets before one cycle; 'pinched' rising ramp tone.

  Sweeping :fs with :fm fixed is the classic sync sweep: a rising pitch glide with a
  static 'formant' staying at :fm, resembling a resonant filter sweep.

  Parameters
  ----------
  :fm — master (sync) frequency in Hz; determines the sync rate and spectral envelope (20–2000; default 110)
  :fs — slave (tone) frequency in Hz; determines the spacing of spectral teeth (20–4000; default 220)
  :sh — shaping amount; 0=pure anti-aliased saw, 1=sine-folded (0–1; default 0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — autonomous hard-sync oscillator)
    :out — anti-aliased hard-sync saw with parametric shaping"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! go-ramp-aa
  {:params {:fm {:range [20.0 2000.0]  :default 110.0}
            :fs {:range [20.0 4000.0]  :default 220.0}
            :sh {:range [0.0 1.0]      :default 0.0}}}
  (let [fm  (param :fm)
        fs  (param :fs)
        sh  (param :sh)
        out (faust
              "shaped
               with {
                 inc_s=%{fs}/ma.SR;
                 ph_m=os.phasor(1,%{fm});
                 trig_m=ph_m<ph_m';
                 sf_m=(1.0-ph_m')/max(0.001,ph_m+1.0-ph_m');
                 ph_s=(select2(trig_m,_+%{fs}/ma.SR,0.0)~_):ma.frac;
                 ph_sp=ph_s';
                 is_nat=float(1-trig_m);
                 ta_s=ph_s/max(0.001,inc_s);
                 tb_s=(ph_s-1.0)/max(0.001,inc_s);
                 blep_nat=is_nat*(float(ta_s<1.0)*(ta_s+ta_s-ta_s*ta_s-1.0)+float(tb_s>-1.0)*(tb_s*tb_s+tb_s+tb_s+1.0));
                 sync_corr=float(trig_m)*ph_sp*sf_m*sf_m;
                 saw_aa=2.0*ph_s-1.0-blep_nat+sync_corr;
                 shaped=(1.0-%{sh})*saw_aa+%{sh}*sin(ma.PI*saw_aa);
               }"
              {:fm fm :fs fs :sh sh})]
    (output :out out)))
