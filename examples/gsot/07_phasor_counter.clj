; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.07-phasor-counter
  "GSOT pp.25-27 — phasor_counter: Hz-rate phasor as looping buffer playhead.

  The chapter's central argument: the manual counter-and-wrap from p.24 is
  a phasor.  The connection is the `/ samplerate` step: dividing the loop
  frequency in Hz by the sample rate gives the phase increment per sample —
  the slope of the ramp.  Accumulating that slope and wrapping at 1 produces
  a [0, 1) ramp cycling at the given frequency.

  In gen~ (`gen~ @title bpm-sample-loop`):

      [in 1 Hz]
          |
      [/ samplerate]  ← \"slope (change of phase per sample frame)\"
          |
         [+]  ←── [history]
          |              ↑
      [switch 0]         |
          |              |
      [wrap 0 1] ────────┘
          |         ↓
      [out 1 ramp]  [sample myloop] ← [buffer myloop]
                          ↓
                    [out 2 audio]

  [in 2 reset] → [switch 0] inlet 0: when reset fires, switch outputs 0.0,
  resetting the phase immediately.

  `sample myloop` reads from the buffer at a normalised [0, 1) position —
  it maps 0.0 to the first sample and 1.0 to the last, with interpolation.
  This differs from `peek`, which takes an absolute integer sample index.

  In Alembic:
  -----------
  `:phasor` already encapsulates `Hz/SR → + → wrap 0 1 → history`.  It
  lowers to `os.phasor(1.0, freq)`, which is exactly the gen~ manual
  pattern.  The phase output is [0, 1), same as `wrap 0 1`.

  For the buffer read, `sample myloop` at normalised position maps to
  `:audio-file` at `phase * loop-length`.  The multiplication converts the
  normalised phase to an absolute sample index, which Faust's `soundfile`
  read expects.  The `int()` and `max(0.0, ...)` guards in the `:audio-file`
  emitter handle the final truncation.

  Vocabulary gaps
  ---------------
  1. Reset inlet — `:phasor` (os.phasor) has no phase-reset signal.  The
     gen~ patch uses `switch 0` to conditionally output 0.0 on the reset
     sample.  A future `:phasor-reset` op (or a `:phasor` with a :reset opt)
     would lower to a manual `select2(rst > 0.5, 0.0, _ + slope) ~ _` cycle
     identical to the gen~ design.  For now, omit the reset inlet.

  2. Buffer length as signal — `sample myloop` in gen~ receives the buffer
     length automatically from the `buffer` reference.  We use
     `param :loop-length` as a stand-in; see example 06 for the full note.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = hslider(\"hz\", 0.25, 0.01, 100.0, 0.009999);
      n1 = os.phasor(1.0, n0);
      n2 = hslider(\"loop-length\", 44100.0, 1.0, 8820000.0, 881.9999);
      n3 = (n1 * n2);
      n4 = ((sf_n4(0, int(max(0.0, n3)))) : (_, !, !))
        with {
          sf_n4 = soundfile(\"sf_n4[url:{'resources/jongly.aif'}]\", 1);
        };

      process = n1, n4;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! phasor-counter
  {:params {:hz          {:range [0.01 100.0]    :default 0.25    :unit :hz}
            :loop-length {:range [1.0 8820000.0] :default 44100.0 :unit :samples}}}
  (let [phase (phasor (param :hz))
        pos   (mul phase (param :loop-length))
        sound (audio-file {:path "resources/jongly.aif"} pos)]
    (output :ramp  phase)
    (output :audio sound)))
