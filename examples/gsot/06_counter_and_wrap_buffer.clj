; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.06-counter-and-wrap-buffer
  "GSOT p.24 — counter_and_wrap_buffer: looping buffer playback.

  The page presents two gen~ subpatchers side by side:

  wrap-VARIANT — normalized phase [0, 1)
  ---------------------------------------
      [in 1 play]   [in 2 reset]
            |              |
           [+]       [switch 0]
            |  ↑          ↓
          history    [wrap 0 1]
                          ↓
               [peek myloop] ← [buffer myloop]
                          ↓
                    [out 1 sound] [out 2 count]

  Uses `wrap 0 1` — the counter stays in [0, 1) regardless of buffer length.
  This normalized phase is the same concept as a phasor; `peek` with a
  fractional index here reads near the beginning of the buffer (not
  sample-accurate across the full file).  The variant illustrates the
  concept before the corrected version below.

  wrap-sound — wrap to buffer length (canonical)
  -----------------------------------------------
      [in 1 play]   [in 2 rewind]
            |              |
           [+]       [switch 0]
            |  ↑          ↓
          history    [wrap(0, buf_len)] ← [buffer myloop] (length outlet)
                          ↓
               [peek myloop] ← [buffer myloop]
                          ↓
                    [out 1 sound] [out 2 count]

  Uses the `buffer myloop` object's first outlet — the number of samples
  in the file — as the `wrap` hi bound.  This is the correct design: the
  counter steps through every sample of the loaded file exactly once per
  cycle.  `buffer~ myloop drumLoop.aif` in the outer Max patch loads the
  file at runtime.

  Vocabulary gap — buffer length as a signal
  -------------------------------------------
  In gen~, the `buffer` object inside a gen~ patcher exposes the loaded
  buffer's sample count as a signal outlet, wired directly to `wrap`'s hi
  inlet.  In Alembic, `:audio-file` discards the `size` output from the
  Faust `soundfile` primitive.  A future `:audio-file-size` secondary port
  (analogous to `:counter-carry` or `:naive-svf-hp`) would expose this.

  For now we use `param :loop-length` and rely on the user to set it to
  match the file.  The playback logic is identical; only the source of the
  loop boundary differs.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1) = n6, n5
        with {
          n2 = hslider(\"loop-length\", 44100.0, 1.0, 8820000.0, 881.9999);
          n3 = (select2(n1 > 0.5, 0.0, _ + n0) ~ _);
          n4 = 0.0;
          n5 = (n4 + fmod((n3 - n4), (n2 - n4)));
          n6 = ((sf_n6(0, int(max(0.0, n5)))) : (_, !, !))
        with {
          sf_n6 = soundfile(\"sf_n6[url:{'resources/drumLoop.aif'}]\", 1);
        };
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! counter-and-wrap-buffer
  {:params {:loop-length {:range [1.0 8820000.0] :default 44100.0 :unit :samples}}}
  (let [play        (audio-in)
        rewind      (audio-in)
        loop-length (param :loop-length)
        count       (wrap (accum play rewind) (const 0.0) loop-length)
        sound       (audio-file {:path "resources/drumLoop.aif"} count)]
    (output :sound sound)
    (output :count count)))
