; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.09-phasor-bpm
  "GSOT pp.28-29 — phasor_bpm: BPM-clocked drum loop.

  The patch presents two gen~ subpatchers.

  Right — bpm utility kernel (gen~ @title bpm):
  -----------------------------------------------
      [param bpm 140]
           |
        [/ 60]   ← \"convert beats per-minute to a per-second frequency (Hz)\"
           |
       [phasor]  ← [in 2 reset]
           |
      [out 1 phasor]

  Produces a phasor cycling at one cycle per beat — a beat clock.  The gen~
  also annotates a `go.ramp.frombpm` object as \"see also\", explicitly
  flagging that this BPM-to-phasor conversion is common enough to have been
  given a name in the go library.

  Left — bpm-sample-loop (gen~ @title bpm-sample-loop):
  -------------------------------------------------------
      [param bpm 140]
           |
        [/ 60]   ← \"convert beats per-minute to a per-second frequency (Hz)\"
           |
         [/]     ← [param numbeats 8]   \"divide by number of beats per loop\"
           |
       [phasor]  ← [in 2 reset]
           |
      [sample myloop]  ← [buffer myloop]
           |
      [out 1 sound]

  The full derivation: `bpm / 60` converts BPM to beats per second (Hz).
  Dividing by `numbeats` converts from beats-per-second to loops-per-second:
  one loop = numbeats beats.  At BPM=140, numbeats=8: loop frequency =
  140/60/8 ≈ 0.292 Hz, one loop every ≈ 3.4 seconds.

  `buffer~ myloop jongly.aif` in the outer patch loads the audio file.

  Vocabulary gaps
  ---------------
  1. go.ramp.frombpm — the book's own \"see also\" reference names this
     conversion pattern.  A future `:bpm-phasor` op (or `:bpm->hz` utility)
     would encapsulate `(div (param :bpm) (const 60.0))`.

  2. phasor reset — `:phasor` (os.phasor) has no phase-reset signal; the
     gen~ `phasor` inlet 1 accepts a reset trigger.  See example 07.

  3. Buffer length as signal — `param :loop-length` stands in for the buffer
     length; see example 06.

  See also — nomos-studio beat time
  -----------------------------------
  nomos-studio has first-class beat time infrastructure that directly
  supersedes the `param bpm / 60` derivation in this example:

  - txlog carries BPM as a first-class field on every event; tempo is
    infrastructure, not a param knob.
  - `:beat-bpm` — host BPM at block rate (the DAW/Link tempo, not a param).
  - `:beat-phase` — host beat position [0,1) at sample rate; this IS the
    phasor output for one beat, delivered by the faust_modulator.
  - `:beat-trigger` — 1-sample pulse at each beat boundary (phase wrap).

  The production idiom for an N-beat loop does not use `(phasor (div bpm 60))`
  at all.  Instead, count host beats and combine with the fractional phase:

      beat-phase  → beat-trigger → counter (wrap at numbeats) → beat-num
      (add beat-num beat-phase) / numbeats → loop phase in [0,1)

  In Alembic:

      (let [bph   (beat-phase)
            btick (beat-trigger bph)
            bnum  (counter {:max 8 :wrap true} btick (const 0.0))
            lph   (div (add bnum bph) (const 8.0))
            pos   (mul lph (param :loop-length))
            sound (audio-file {:path \"resources/jongly.aif\"} pos)]
        (output :sound sound))

  This locks the loop to the host transport exactly — no drift, no float
  accumulation, instant re-sync on tempo change.  The `param bpm` form in
  this example is the self-contained version that runs without a host, which
  is why the book teaches it first.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = hslider(\"bpm\", 140.0, 20.0, 300.0, 0.028);
      n1 = hslider(\"numbeats\", 8.0, 1.0, 64.0, 0.0063);
      n2 = 60.0;
      n3 = (n0 / n2);
      n4 = (n3 / n1);
      n5 = os.phasor(1.0, n4);
      n6 = hslider(\"loop-length\", 44100.0, 1.0, 8820000.0, 881.9999);
      n7 = (n5 * n6);
      n8 = ((sf_n8(0, int(max(0.0, n7)))) : (_, !, !))
        with {
          sf_n8 = soundfile(\"sf_n8[url:{'resources/jongly.aif'}]\", 1);
        };

      process = n8;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! phasor-bpm
  {:params {:bpm         {:range [20.0 300.0]   :default 140.0  :unit :bpm}
            :numbeats    {:range [1.0 64.0]      :default 8.0    :unit :beats}
            :loop-length {:range [1.0 8820000.0] :default 44100.0 :unit :samples}}}
  (let [bpm      (param :bpm)
        numbeats (param :numbeats)
        hz       (div (div bpm (const 60.0)) numbeats)
        phase    (phasor hz)
        pos      (mul phase (param :loop-length))
        sound    (audio-file {:path "resources/jongly.aif"} pos)]
    (output :sound sound)))
