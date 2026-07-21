; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.04-counter-play-a-buffer
  "GSOT p.23 — counter_play_a_buffer: sample-accurate buffer playback.

  Extends the pausable counter (example 03) by using the accumulated sample
  position as a read index into a runtime-loaded audio file.  The payoff of
  the chapter's modular-arithmetic-of-time thesis: once you have a sample
  counter you have a playback head.

  In gen~:

      [in 1 play]   [in 2 rewind]
            |              |
         [accum]  ←────────┘
            |
        [peek mybuf]  ←── [buffer mybuf]  ← buffer~ mybuf duduk.aif
            |
        [out 1 sound]

  `buffer~ mybuf duduk.aif` in Max loads the file at runtime.  Inside gen~,
  `buffer mybuf` is a reference to that named buffer and `peek mybuf` reads
  it at the accumulated integer sample position.

  In Alembic, `(audio-file {:path \"...\"} index)` is the runtime-loaded read.
  It lowers to Faust's `soundfile` primitive, which the CLAP host resolves
  to an audio file on disk at plugin instantiation.

  Place `duduk.aif` (or any mono audio file) at the path below relative to
  the patch resources directory.  A suitable CC0 single note can be downloaded
  from the freesound archive — freesound integration is a planned Alembic
  feature that will allow referencing samples by freesound ID directly.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1) = n3
        with {
          n2 = (select2(n1 > 0.5, 0.0, _ + n0) ~ _);
          n3 = ((sf_n3(0, int(max(0.0, n2)))) : (_, !, !))
        with {
          sf_n3 = soundfile(\"sf_n3[url:{'resources/duduk.aif'}]\", 1);
        };
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! counter-play-a-buffer
  {}
  (let [play   (audio-in)
        rewind (audio-in)
        pos    (accum play rewind)
        sound  (audio-file {:path "resources/duduk.aif"} pos)]
    (output :sound sound)))
