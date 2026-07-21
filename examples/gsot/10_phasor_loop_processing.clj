; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.10-phasor-loop-processing
  "GSOT pp.30-32 — phasor_loop_processing: ramps as cyclical time.

  The chapter's central architectural claim: a phasor produces a [0, 1)
  ramp signal, and *any function that maps [0, 1) → [0, 1)* is valid ramp
  processing between the phasor and the buffer read.  The gen~
  start-point subpatcher makes this explicit with the comment:

      \"insert ramp processing here\"
      \"whatever ramp processing we do, the result must always be between 0 and 1\"

  The patch contains nine gen~ subpatchers in pedagogical sequence.

  ── slope_examples ────────────────────────────────────────────────────────
  Four independent `phasor 1` instances, each showing a different
  transformation.  No audio output — visual demonstration only.

      1. scale:      phasor → * 4 → out (range [0, 4), four times the speed)
      2. wrap:       phasor → * 4 → wrap 0 1 → out (4 sub-cycles per loop)
      3. step:       phasor → * 4 → floor → out (4 discrete integer steps)
      4. step-norm:  phasor → * 4 → floor → / 4 → out ([0, 0.25, 0.5, 0.75])

  In Alembic these are pure compositions of existing ops:
      scale:      (mul ph (const 4.0))
      wrap:       (wrap (mul ph (const 4.0)) (const 0.0) (const 1.0))
      step:       (floor (mul ph (const 4.0)))           ;; needs :floor op
      step-norm:  (div (floor (mul ph (const 4.0))) (const 4.0))

  Note: `:floor` (Faust floor()) is a vocabulary gap — a trivial polymorphic
  op, no state, wraps a single Faust primitive.

  ── start-point (baseline) ────────────────────────────────────────────────
      param bpm 140 → / 60 → / numbeats → phasor
          → wrap 0 1   ← \"insert ramp processing here\"
          → sample myloop
          → out 1 sound

  The `wrap 0 1` with nothing else is a no-op (phasor is already [0,1)) but
  marks the insertion point.  All subsequent variants replace it.

  ── rate ──────────────────────────────────────────────────────────────────
      phasor → * rate → wrap 0 1 → sample

  Multiplying the ramp by `rate` changes playback speed.  rate > 1 = faster,
  rate < 1 = slower, rate < 0 = reverse (negative phase, wrap handles it).
  wrap 0 1 keeps the scaled ramp in range regardless of rate sign or magnitude.

  ── scrub ─────────────────────────────────────────────────────────────────
      phasor → + scrub → wrap 0 1 → sample

  Adding a [−1, 1] offset shifts the start point anywhere in the loop.
  wrap 0 1 handles the modular arithmetic.

  ── scrub_and_rate (implemented below) ───────────────────────────────────
      phasor → * rate → + scrub → wrap 0 1 → sample

  The complete composition: rate scales first, then scrub shifts.  Both
  params combined give full control over speed and start position.

  ── 16ths ─────────────────────────────────────────────────────────────────
      phasor → * slices → wrap 0 1 → / slices → sample

  `* slices → wrap 0 1` creates `slices` sub-cycles per main loop cycle —
  each sub-cycle is a mini-phasor [0,1).  `/ slices` maps it back to
  [0, 1/slices]: the read head cycles through only the first 1/N of the
  buffer, `slices` times per loop.  The first slice plays on repeat.

  In Alembic:
      (let [slice-ph (div (wrap (mul phase slices) (const 0.0) (const 1.0))
                         slices)
            pos      (mul slice-ph (param :loop-length))]
        ...)

  ── 16ths-scrub ───────────────────────────────────────────────────────────
      phasor → * slices → wrap 0 1 → + offset → / slices → wrap 0 1 → sample

  Adding `offset` (integer slice number) before `/ slices` selects which
  1/N segment of the buffer plays.  offset=0 → first slice, offset=1 →
  second slice, etc.

  ── 16ths-scrub-jumpy (the complex variant) ───────────────────────────────
  Adds jump detection using `latch` (Alembic `:sample-hold`):

      wrap 0 1 → delta → abs → > 0.5 → latch trigger
      noise * chance * jump → floor → latch input
      latch → + → / slices → wrap 0 1 → sample

  When the ramp makes a large backward jump (> 0.5 — the wrap discontinuity),
  `delta → abs → > 0.5` fires a trigger.  The latch captures a new random
  floor(noise * chance * jump) slice offset, creating stochastic slice
  selection on each loop boundary.

  `delta` is Alembic `:delta` (x - x').  `latch` = Alembic `:sample-hold`
  (ba.sAndH).  `:floor` is Alembic `:floor` (rate :polymorphic).

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = hslider(\"bpm\", 140.0, 20.0, 300.0, 0.028);
      n1 = 60.0;
      n2 = (n0 / n1);
      n3 = hslider(\"numbeats\", 8.0, 1.0, 64.0, 0.0063);
      n4 = (n2 / n3);
      n5 = os.phasor(1.0, n4);
      n6 = hslider(\"rate\", 1.0, 0.0, 4.0, 0.0004);
      n7 = (n5 * n6);
      n8 = hslider(\"scrub\", 0.0, -1.0, 1.0, 0.0002);
      n9 = (n7 + n8);
      n10 = 0.0;
      n11 = 1.0;
      n12 = (n10 + fmod((n9 - n10), (n11 - n10)));
      n13 = hslider(\"loop-length\", 44100.0, 1.0, 8820000.0, 881.9999);
      n14 = (n12 * n13);
      n15 = ((sf_n15(0, int(max(0.0, n14)))) : (_, !, !))
        with {
          sf_n15 = soundfile(\"sf_n15[url:{'resources/jongly.aif'}]\", 1);
        };

      process = n15, n12;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! phasor-loop-processing
  {:params {:bpm         {:range [20.0 300.0]    :default 140.0  :unit :bpm}
            :numbeats    {:range [1.0 64.0]       :default 8.0    :unit :beats}
            :rate        {:range [0.0 4.0]        :default 1.0}
            :scrub       {:range [-1.0 1.0]       :default 0.0}
            :loop-length {:range [1.0 8820000.0]  :default 44100.0 :unit :samples}}}
  (let [hz    (div (div (param :bpm) (const 60.0)) (param :numbeats))
        phase (phasor hz)
        ramp  (wrap (add (mul phase (param :rate)) (param :scrub))
                    (const 0.0) (const 1.0))
        pos   (mul ramp (param :loop-length))
        sound (audio-file {:path "resources/jongly.aif"} pos)]
    (output :sound sound)
    (output :ramp  ramp)))
