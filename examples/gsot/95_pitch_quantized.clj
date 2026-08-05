; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.95-pitch-quantized
  "GSOT pp.151-152 — pitch-quantized.maxpat.

  'Quantization' (Chapter 5)
  --------------------------
  Maps a continuous (possibly non-integer) pitch index to the nearest diatonic
  degree of a major scale, then converts to Hz via equal temperament.

  This extends pitch (example 94) in two ways:

    1. Quantization step — round the float input to the nearest integer before
       mapping.  This 'snaps' a continuous or jittered index to discrete
       diatonic degrees.

    2. Diatonic mapping — each integer degree selects a major-scale interval
       rather than a chromatic semitone.  The scale spans [0,6] per octave;
       indices outside this range wrap with correct octave arithmetic.

  Major scale intervals (from root)
  ----------------------------------
  degree:    0   1   2   3   4   5   6
  semitones: 0   2   4   5   7   9  11
  note (C):  C   D   E   F   G   A   B

  Octave wrapping
  ---------------
  For degree N ≥ 7: maps to degree (N mod 7) plus additional octave(s).
  For degree N < 0: wraps correctly via `(N%7 + 7) % 7` pitch-class arithmetic.

  With :root=60 (C4):
    index=0.0  → C4   = 261.6 Hz
    index=2.1  → E4   = 329.6 Hz   (rounds to degree 2)
    index=4.0  → G4   = 392.0 Hz
    index=7.0  → C5   = 523.3 Hz   (octave above, degree 0)
    index=-7.0 → C3   = 130.8 Hz   (octave below, degree 0)

  Scale step lookup — select2 tree
  ---------------------------------
  degree_in_octave ∈ {0,1,2,3,4,5,6} → semitone offset ∈ {0,2,4,5,7,9,11}

      d<4?
        true  (d in 0-3):  d<2? { d<1? 0:2 } : { d<3? 4:5 }
        false (d in 4-6):  d<6? { d<5? 7:9 } : 11

  Relationship to pitch (example 94)
  -----------------------------------
  pitch (ex.94) maps each integer unit to ONE semitone (chromatic pitch space).
  pitch-quantized (ex.95) maps each integer unit to a diatonic major-scale degree
  (7 pitches per octave rather than 12).  The quantization step lets a smooth
  or continuous signal drive the diatonic selection naturally.

  Parameters
  ----------
  :root — MIDI note for degree 0 (0–127; default 60 = middle C)

  Audio inputs
  ------------
  audio-in 0: in — pitch index as float (non-integers are rounded to nearest degree)

  Outputs
  -------
  :freq — frequency in Hz
  :note — MIDI note number (float)

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_freq, n_note
        with {
          n_rt = hslider(\"root\", 60.0, 0.0, 127.0, 1.0);
          n_qu = rint(n0);
          n_dg = float((int(n_qu)%7+7)%7);
          n_oc = floor(n_qu/7.0);
          n_ss = select2(n_dg<4.0,
                   select2(n_dg<6.0,11.0,select2(n_dg<5.0,9.0,7.0)),
                   select2(n_dg<2.0,select2(n_dg<3.0,5.0,4.0),
                                    select2(n_dg<1.0,2.0,0.0)));
          n_note = n_rt + n_ss + n_oc * 12.0;
          n_freq = 440.0*pow(2.0,(n_note-69.0)/12.0);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! pitch-quantized
  {:params {:root {:range [0.0 127.0] :default 60.0}}}
  (let [in       (audio-in)
        root     (param :root)
        ; Quantize float to nearest integer degree
        quant    (faust "rint(%{in})" {:in in})
        ; Degree within one octave, with correct wrapping for negative inputs
        deg-oc   (faust "float((int(%{qu})%7+7)%7)" {:qu quant})
        ; Octave number (may be negative)
        oct-n    (faust "floor(%{qu}/7.0)" {:qu quant})
        ; Major scale step in semitones via select2 tree on degree 0-6
        scale-st (faust "select2(%{dg}<4.0,select2(%{dg}<6.0,11.0,select2(%{dg}<5.0,9.0,7.0)),select2(%{dg}<2.0,select2(%{dg}<3.0,5.0,4.0),select2(%{dg}<1.0,2.0,0.0)))"
                        {:dg deg-oc})
        ; MIDI note = root + diatonic semitone offset + octave transposition
        note     (faust "%{rt}+%{ss}+%{oc}*12.0" {:rt root :ss scale-st :oc oct-n})
        freq     (faust "440.0*pow(2.0,(%{nt}-69.0)/12.0)" {:nt note})]
    (output :freq freq)
    (output :note note)))
