; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.211-ramp-subsample-trig
  "GSOT p.328 — ramp-subsample-trig.maxpat (Chapter 10: Windows of Time).

  'Subsample-Accurate Trigger from Phasor Wrap'
  -----------------------------------------------
  A phasor running at :fp Hz completes one cycle per 1/:fp seconds.  At audio
  rates, the cycle boundary does not in general fall on a sample boundary — it
  falls somewhere BETWEEN two consecutive samples.  Standard wrap detection
  (`ph < ph'`) identifies WHICH SAMPLE the wrap occurred in, but not WHERE
  within that sample interval the wrap actually happened.

  This ±1-sample ambiguity introduces TIMING JITTER into any event triggered from
  the phasor.  For individual grains it is inaudible; in dense granulation with
  many overlapping grains starting from the same master phasor, the jitter
  accumulates and causes phase smearing — a slight blurring of the granular
  spectrum.  At high grain densities or when grains must align precisely with
  other events (e.g. MIDI sync, rhythmic granulation), subsample accuracy matters.

  The subsample fraction
  ----------------------
  Given the phasor value ph' at sample n-1 and ph at sample n (after wrap):

      inc = ph + 1.0 − ph'          — per-sample phase increment = fc/SR

  The wrap from ph'→ 1.0 → ph happened WITHIN the interval from sample n-1 to
  sample n.  The fraction of that interval that elapsed before the wrap:

      sf = (1.0 − ph') / inc        — subsample fraction ∈ (0, 1)

  Interpretation:
    sf = 0.0  — wrap happened at the very start of the sample n-1→n interval
               (at the sample n-1 boundary); a full sample has elapsed since the wrap.
    sf = 0.5  — wrap happened at the midpoint; half a sample has elapsed.
    sf = 1.0  — wrap happened at sample n exactly; zero elapsed time since wrap.

  Note: `ph'` and `ph` appear to be swapped versus intuition, but the formula is
  correct because `ph` (the post-wrap value, just above 0) plus `1 − ph'` (the
  phase remaining until wrap at n-1) equals exactly one `inc`:
      ph + (1 − ph') = inc   ✓

  Output encoding
  ---------------
  The output signal encodes the subsample fraction as a KEYED TRIGGER value:

      out = trig × sf

  At non-trigger samples: out = 0.0 (trig=0 suppresses the sf value).
  At wrap samples: out = sf ∈ (0, 1) — the subsample fraction.

  This 'trigger with value' encoding is a common gen~ pattern: a downstream
  processor can detect the trigger by checking `out > 0` (or `out != 0`) and
  extract the subsample fraction from the non-zero value.  It passes both pieces
  of information — event detection and event timing — in a single audio-rate signal.

  go-ramp.subsample.maxpat (ex.212) consumes this signal to initialize a grain
  oscillator at the subsample-corrected starting phase.

  Relationship to ph < ph' (ex.202, 194, 209)
  ---------------------------------------------
  Previous patches used `ph < ph'` (or `ph_m < ph_m'`) as a binary trigger.
  That trigger fires at the correct SAMPLE but carries no information about WHERE
  within the sample the event occurred.  ramp-subsample-trig replaces that binary
  trigger with a quantitative value encoding the subsample position.

  At :fp = 440 Hz, SR = 44100 Hz:
    inc = 440/44100 ≈ 0.00998 samples per cycle increment
    Typical sf range: (0, 1) — uniformly distributed over many cycles
    Maximum phase error without correction: ±inc/2 ≈ ±0.005 of one grain cycle

  Parameters
  ----------
  :fp — phasor frequency in Hz; wrap events occur at this rate (1–2000; default 10)

  Audio inputs / Outputs
  ----------------------
  (no audio input — autonomous phasor-based trigger)
    :out — subsample fraction at wrap events, 0.0 at all other samples;
           nonzero value encodes sf ∈ (0,1)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! ramp-subsample-trig
  {:params {:fp {:range [1.0 2000.0] :default 10.0}}}
  (let [fp  (param :fp)
        out (faust
              "trig*sf
               with {
                 ph=os.phasor(1,%fp);
                 trig=float(ph<ph');
                 sf=(1.0-ph')/max(0.001,ph+1.0-ph');
               }"
              {:fp fp})]
    (output :out out)))
