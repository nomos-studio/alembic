; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.94-pitch
  "GSOT pp.150-151 — pitch.maxpat.

  'Pitch spaces' (Chapter 5)
  ---------------------------
  Maps an integer pitch index to a frequency in Hz using equal temperament.
  Each unit of index is one semitone above the :root parameter.  This is the
  foundational 'pitch space' patch — a direct chromatic mapping without scale
  quantization.

  Formula
  -------
      MIDI note = :root + index
      Hz        = 440 × 2^((note − 69) / 12)

  The reference A4 = 440 Hz is at MIDI note 69.  Middle C (C4) = MIDI 60.

  Pitch space concept
  -------------------
  A 'pitch space' is an organised continuum or set of pitches.  The simplest
  pitch space is the 12-tone equal-temperament chromatic scale, where each
  integer index selects one semitone above a root.

  This patch forms the output stage of a typical sequencer chain:
    binary-decode → pitch → audio oscillator
    Euclidean gate → shift register → pitch → oscillator

  With :root=60 (middle C):
    index=0  → 261.6 Hz (C4)
    index=7  → 392.0 Hz (G4, a perfect fifth above)
    index=12 → 523.3 Hz (C5, one octave above)
    index=-12 → 130.8 Hz (C3, one octave below)

  Parameters
  ----------
  :root — MIDI note number for index 0 (0–127; default 60 = middle C)

  Audio inputs
  ------------
  audio-in 0: in — integer pitch index (semitone offset from :root)

  Outputs
  -------
  :freq — frequency in Hz
  :note — MIDI note number (root + index)

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_freq, n_note
        with {
          n_rt   = hslider(\"root\", 60.0, 0.0, 127.0, 1.0);
          n_note = n_rt + n0;
          n_freq = 440.0*pow(2.0,(n_note-69.0)/12.0);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! pitch
  {:params {:root {:range [0.0 127.0] :default 60.0}}}
  (let [in   (audio-in)
        root (param :root)
        note (faust "%{rt}+%{in}" {:rt root :in in})
        freq (faust "440.0*pow(2.0,(%{nt}-69.0)/12.0)" {:nt note})]
    (output :freq freq)
    (output :note note)))
