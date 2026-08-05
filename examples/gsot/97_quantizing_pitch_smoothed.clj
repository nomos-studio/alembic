; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.97-quantizing-pitch-smoothed
  "GSOT pp.155-157 — quantizing-pitch-smoothed.maxpat.

  'Smooth-stepped quantization' (Chapter 5)
  ------------------------------------------
  Extends quantizing-pitch (example 96) with a :smooth parameter that blends
  between hard-quantized (stepped) and continuously interpolated pitch output.

  The smooth-step formula
  -----------------------
  Example 96 maps degree index i to semitone using:

      semi = floor(12 × i / N)          N = :beats

  The fractional part of the same ratio is:

      frac = (12 × i / N) − floor(12 × i / N)

  This frac encodes 'how far through the current scale step' the input is.
  Blending by :smooth gives:

      semi_smooth = floor(12 × i / N) + smooth × frac
                  = (1 − smooth) × floor(ratio) + smooth × ratio
                  = lerp(quantized, continuous, smooth)

  where:
    smooth = 0.0 → hard step (identical to quantizing-pitch, example 96)
    smooth = 1.0 → linear interpolation between adjacent scale degrees

  At intermediate values the pitch still gravitates toward the quantized
  scale notes but glides into and out of each step.

  Continuous degree input
  -----------------------
  The float audio-in allows a fractional degree: input 2.7 with :beats=5
  (major pentatonic) lands 70% of the way between the 3rd and 4th scale
  degrees.  At smooth=0 this outputs the same pitch as degree 2; at smooth=1
  it outputs a pitch 70% of the semitone interval between degrees 2 and 3.

  Euclidean scale families
  ------------------------
  The :beats parameter selects the Euclidean N-note scale (same as example 96):

  N=1 → [0]                       Octave
  N=2 → [0,6]                     Tritone
  N=3 → [0,4,8]                   Augmented (major 3rds)
  N=4 → [0,3,6,9]                 Diminished (minor 3rds)
  N=5 → [0,2,4,7,9]               Major pentatonic
  N=6 → [0,2,4,6,8,10]            Whole tone
  N=7 → diatonic heptatonic        Major/natural minor rotation
  N=8 → [0,1,3,4,6,7,9,10]       Octatonic

  At smooth=1 the output is a continuous pitch space between the scale notes;
  at smooth=0 it snaps to the Euclidean pitch classes exactly.

  Implementation
  --------------
  Degree-within-octave (float, includes fractional part):
      deg_oc = in − N × floor(in / N)     [= in mod N, float]
      oct_n  = floor(in / N)

  Continuous ratio:
      ratio = 12 × deg_oc / N

  Hard-quantized semitone:
      semi_q = floor(ratio)

  Smooth blend:
      semi = semi_q + smooth × (ratio − semi_q)

  MIDI note and frequency:
      note = root + semi + oct_n × 12
      freq = 440 × 2^((note − 69) / 12)

  Parameters
  ----------
  :beats  — N: scale notes per octave (1–8; default 7 = diatonic)
  :smooth — blend 0.0=hard-step → 1.0=linear-interpolated (default 0.5)
  :root   — MIDI note for degree 0 (0–127; default 60 = middle C)

  Audio inputs
  ------------
  audio-in 0: in — degree index (float; fractional parts interpolated by :smooth)

  Outputs
  -------
  :freq — frequency in Hz
  :note — MIDI note number (float; non-integer when smooth > 0)

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_freq, n_note
        with {
          n_bt   = hslider(\"beats\",  7.0, 1.0,   8.0, 1.0);
          n_sm   = hslider(\"smooth\", 0.5, 0.0,   1.0, 0.01);
          n_rt   = hslider(\"root\",  60.0, 0.0, 127.0, 1.0);
          n_oc   = floor(n0/max(1.0,n_bt));
          n_dg   = n0-n_bt*n_oc;
          n_qr   = 12.0*n_dg/max(1.0,n_bt);
          n_sq   = floor(n_qr);
          n_note = n_rt + n_sq + n_sm*(n_qr-n_sq) + n_oc*12.0;
          n_freq = 440.0*pow(2.0,(n_note-69.0)/12.0);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! quantizing-pitch-smoothed
  {:params {:beats  {:range [1.0 8.0]   :default 7.0}
            :smooth {:range [0.0 1.0]   :default 0.5}
            :root   {:range [0.0 127.0] :default 60.0}}}
  (let [in      (audio-in)
        beats   (param :beats)
        smooth  (param :smooth)
        root    (param :root)
        ; Octave number and float degree within current scale octave
        oct-n   (faust "floor(%{in}/max(1.0,%{bt}))" {:in in :bt beats})
        deg-oc  (faust "%{in}-%{bt}*%{oc}" {:in in :bt beats :oc oct-n})
        ; Continuous inverse digitized ratio: position within [0,12)
        q-ratio (faust "12.0*%{dg}/max(1.0,%{bt})" {:dg deg-oc :bt beats})
        ; Hard-quantized semitone (floor = scale degree boundary)
        semi-q  (faust "floor(%{qr})" {:qr q-ratio})
        ; Smooth blend: lerp from quantized floor to continuous ratio
        note    (faust "%{rt}+%{sq}+%{sm}*(%{qr}-%{sq})+%{oc}*12.0"
                       {:rt root :sq semi-q :sm smooth :qr q-ratio :oc oct-n})
        freq    (faust "440.0*pow(2.0,(%{nt}-69.0)/12.0)" {:nt note})]
    (output :freq freq)
    (output :note note)))
