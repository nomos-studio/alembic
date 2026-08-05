; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.96-quantizing-pitch
  "GSOT pp.153-154 — quantizing-pitch.maxpat.

  'Euclidean patterns of length 12 produce common scales' (Chapter 5)
  -------------------------------------------------------------------
  Uses the INVERSE digitized ratio to map an integer degree index to a semitone
  position within a maximally-even N-note scale embedded in a 12-tone octave.

  The inverse digitized ratio
  ---------------------------
  Euclidean rhythms (example 91) map K=12 steps → N beats via:

      beat fires at step k  when  floor(k × N / 12)  increments

  The beat positions form a maximally-even distribution of N events in 12 slots.

  Inverting: for beat index i (0 ≤ i < N), the step position is:

      semitone = floor(12 × i / N)

  This maps N scale degrees to N maximally-even semitone positions within [0,12).
  With octave wrapping: for any integer degree D,

      i       = D mod N          (degree within current octave)
      octave  = floor(D / N)     (octave number)
      semi    = floor(12 × i / N)
      note    = root + semi + octave × 12
      freq    = 440 × 2^((note − 69) / 12)

  N-note Euclidean scales in 12 semitones
  ----------------------------------------
  N=1  → [0]                     Octave (single pitch class)
  N=2  → [0, 6]                  Tritone + octave
  N=3  → [0, 4, 8]               Augmented chord (major 3rds, 4 semitones)
  N=4  → [0, 3, 6, 9]            Diminished 7th (minor 3rds, 3 semitones)
  N=5  → [0, 2, 4, 7, 9]         Major pentatonic / classical pentatonic modes
  N=6  → [0, 2, 4, 6, 8, 10]     Whole-tone scale
  N=7  → [0, 1, 3, 5, 6, 8, 10]  Heptatonic (major scale rotation / diatonic)
  N=8  → [0, 1, 3, 4, 6, 7, 9, 10]  Octatonic / diminished scale (HS-WS)

  These are the same patterns listed in GSOT under 'parameter K': K here is N.

  Relationship to euclidean-rhythms (example 91)
  -----------------------------------------------
  Example 91 ran the step counter k 0→11 and used floor(k×N/12) as a gate
  generator.  This patch inverts the relationship: run the BEAT index i 0→N-1
  (via audio-in) and use floor(12×i/N) as the PITCH position.

  Compared to pitch-quantized (example 95), which hard-wires the major scale
  via a select2 tree, this patch generates any maximally-even scale from one
  formula by varying the :beats parameter.  Setting :beats=7 gives the same
  family of scales (different rotation) as example 95; setting :beats=5 gives
  the pentatonic modes instead.

  Chain examples
  --------------
  Binary decode → quantizing-pitch:
    (euclidean-rhythms :beats 3 :steps 8) gate → binary-decode → quantizing-pitch

  Shift register → quantizing-pitch → audio oscillator:
    (shift-register-integer :pattern 85 :steps 8) → binary-decode
    → quantizing-pitch :beats 5 → (oscillator :freq)

  Parameters
  ----------
  :beats — N: number of notes in the scale (1–8; default 7 = heptatonic)
  :root  — MIDI note for degree 0 (0–127; default 60 = middle C)

  Audio inputs
  ------------
  audio-in 0: in — integer scale degree (non-negative; octave wrapping at N)

  Outputs
  -------
  :freq — frequency in Hz
  :note — MIDI note number (float)

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_freq, n_note
        with {
          n_bt   = hslider(\"beats\", 7.0, 1.0, 8.0, 1.0);
          n_rt   = hslider(\"root\",  60.0, 0.0, 127.0, 1.0);
          n_dg   = float(int(n0) % max(1, int(n_bt)));
          n_oc   = floor(n0 / max(1.0, n_bt));
          n_sm   = floor(12.0 * n_dg / max(1.0, n_bt));
          n_note = n_rt + n_sm + n_oc * 12.0;
          n_freq = 440.0*pow(2.0,(n_note-69.0)/12.0);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! quantizing-pitch
  {:params {:beats {:range [1.0 8.0]   :default 7.0}
            :root  {:range [0.0 127.0] :default 60.0}}}
  (let [in     (audio-in)
        beats  (param :beats)
        root   (param :root)
        ; Degree within current octave of the scale (0..N-1)
        deg-oc (faust "float(int(%{in})%max(1,int(%{bt})))" {:in in :bt beats})
        ; Octave number (integer floor division)
        oct-n  (faust "floor(%{in}/max(1.0,%{bt}))" {:in in :bt beats})
        ; Inverse digitized ratio: semitone = floor(12 * degree / N)
        semi   (faust "floor(12.0*%{dg}/max(1.0,%{bt}))" {:dg deg-oc :bt beats})
        ; MIDI note and frequency
        note   (faust "%{rt}+%{sm}+%{oc}*12.0" {:rt root :sm semi :oc oct-n})
        freq   (faust "440.0*pow(2.0,(%{nt}-69.0)/12.0)" {:nt note})]
    (output :freq freq)
    (output :note note)))
