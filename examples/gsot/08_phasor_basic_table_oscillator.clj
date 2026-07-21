; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.08-phasor-basic-table-oscillator
  "GSOT p.28 — phasor_basic_table_oscillator: phasor as oscillator via table read.

  The patch presents two gen~ subpatchers side by side, explicitly closing
  the argument begun on p.25:

  Left — manual derivation (unnamed gen~):
  -----------------------------------------
      [in 1 Hz]
          |
      [/ samplerate]   ← \"phase change per sample frame\"
          |
         [+] ←── [history]
          |             ↑
      [switch 0]        |
          |             |
      [wrap 0 1] ───────┘
          |
      [sample mytable]  ← [buffer mytable]
          |
      [out 1 sound]

  A visual panel with red border surrounds this section and labels it:
  \"all of this section could be replaced by a [phasor]\"

  Right — clean form (gen~ @title using-phasor):
  ------------------------------------------------
      [in 1 Hz]
          |
      [phasor]
          |
      [sample mytable]  ← [buffer mytable]
          |
      [out 1 sound]

  The outer patch loads `sinc.wav` into `buffer~ mytable` — a 512-sample
  single-cycle wavetable shipped with the GSOT media directory.  At audio
  frequencies (e.g. 440 Hz), the phasor sweeps through all 512 samples 440
  times per second, producing the waveform's pitch at that frequency.  The
  same structure drove a drum loop at 0.25 Hz in example 07; only the rate
  and table content differ.

  This is the table oscillator pattern: phasor (Hz) → normalized position
  [0, 1) → table lookup → audio.  Any single-cycle waveform stored in the
  table becomes the oscillator's timbre.

  In Alembic:
  -----------
  `:phasor` IS the clean gen~ version: it lowers to `os.phasor(1.0, freq)`,
  which is the [0, 1) ramp at the given frequency — identical to the manual
  derivation above.  The two-gen~ side-by-side in the book maps to a single
  Alembic expression.

  `(mul phase table-size)` converts the [0, 1) phase to a sample index in
  [0, 512), which `:audio-file` passes to the Faust `soundfile` read after
  `int()` truncation.

  `sinc.wav` is in the GSOT reference media directory (512 samples, mono,
  48 kHz).  The `param :table-size` default of 512 matches it exactly.
  Other single-cycle wavetables can be swapped in by changing :path and
  adjusting :table-size.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = hslider(\"hz\", 440.0, 20.0, 20000.0, 1.998);
      n1 = os.phasor(1.0, n0);
      n2 = hslider(\"table-size\", 512.0, 1.0, 65536.0, 6.5535);
      n3 = (n1 * n2);
      n4 = ((sf_n4(0, int(max(0.0, n3)))) : (_, !, !))
        with {
          sf_n4 = soundfile(\"sf_n4[url:{'resources/sinc.wav'}]\", 1);
        };

      process = n4;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! phasor-basic-table-oscillator
  {:params {:hz         {:range [20.0 20000.0] :default 440.0  :unit :hz}
            :table-size {:range [1.0 65536.0]  :default 512.0  :unit :samples}}}
  (let [phase (phasor (param :hz))
        pos   (mul phase (param :table-size))
        sound (audio-file {:path "resources/sinc.wav"} pos)]
    (output :sound sound)))
