; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.11-phasor-beat-slicer
  "GSOT pp.33-36 — phasor_beat_slicer: random beat slicing via ramp processing.

  The maxpat contains two sections:
  - An outer 'another way' beatslicer (obj-9, uses go.ramp2steps / go.ramp2trig
    abstractions from the GSOT go library)
  - An annotated beatslicer (obj-111, explicit signal flow with comments)
  - A 'building the patcher step by step' subpatch that walks through the
    progression from scrub_and_rate → 16ths → 16ths-scrub → synced → jumpy → final

  The Alembic implementation below maps to obj-111, the annotated form.
  All signal flow comments in that gen~ are reproduced in the section headers.

  ── Building progression ──────────────────────────────────────────────────────

  Step 0 — scrub_and_rate (prior example, shown as starting point):
      phasor → * rate → + scrub → wrap 0 1 → sample myloop

  Step 1 — 16ths subdivision (gen~ @title 16ths, building subpatch obj-54):
  Comment: 'run a faster synced ramp at slices rate', 'bring it back down to normal speed'
      phasor → * slices → wrap 0 1 → / slices → sample myloop

  `* slices → wrap 0 1` creates N sub-cycles per loop (the slice phasor).
  `/ slices` maps each [0,1) sub-cycle back to [0, 1/N]: the read head plays
  only the first 1/N of the buffer, N times per loop cycle.  The first slice
  plays on repeat.

  In Alembic:
      (let [sl-ph (div (wrap (mul phase slices) (const 0.0) (const 1.0)) slices)
            pos   (mul sl-ph (param :loop-length))]
        ...)

  Step 2 — 16ths-scrub: pick which slice (gen~ @title 16ths-scrub, obj-46):
  Comment: 'slice phasor', 'normal speed', 'pick an offset into the loop'
      phasor → * slices → wrap 0 1 → + offset → / slices → wrap 0 1 → sample

  Adding a fixed integer `offset` before `/ slices` selects which 1/N segment
  to play.  `wrap 0 1` at the end handles wraparound when offset overflows.

  Step 3 — 16ths-scrub-synced: latch offset at slice boundaries (obj-78):
  Comments: 'slice phasor', 'get the absolute difference in the ramp since the
  last sample frame', 'did it make a big jump?', 'if so, pick a new offset',
  'only do whole number slice offsets'

      slice_ph → delta → abs → > 0.5 → latch trigger
      floor(param offset 12) → latch data
      latch → offset in the + node

  The delta-abs-threshold detects the sawtooth wrap discontinuity in the slice
  phasor (backward jump > 0.5) — this fires at every slice boundary.  The
  `latch` (Alembic `:sample-hold`) captures `floor(12) = 12` on each trigger.
  Since the input is a constant, the offset never changes — this step introduces
  the latch mechanism but uses a fixed offset.

  Step 4 — 16ths-scrub-jumpy: floor(scaled_phasor * jump) (obj-90):
  Comments: 'jump through slices at a certain rate'

      phasor * slices * param jump → floor → latch data

  The latch data is now `floor(phasor * slices * jump)`.  At slice boundary k
  (phasor ≈ k/slices): data = floor(k * jump).  With jump=1.235:
      k=0 → 0,  k=1 → 1,  k=2 → 2,  k=3 → 3,  …,  k=8 → 9,  k=10 → 12, …
  Each slice fires the latch, capturing a new integer offset that traces a
  deterministic but non-sequential pattern through the buffer.

  Step 5 — final beatslicer: add noise * chance (obj-111, implemented below):
  Comments: 'sometimes skip about'

      floor(phasor * slices * jump + noise * chance) → latch data

  `noise * chance` (noise ∈ [−1, 1], chance ∈ [0, 1]) adds sub-integer noise
  to the scaled phasor before flooring.  With chance=0.2, approximately 20% of
  boundaries get a ±1 slice deviation, creating probabilistic variation around
  the deterministic jump pattern.

  ── Annotated signal flow (obj-111) ──────────────────────────────────────────

      param bpm 140 → / 60 → / numbeats → phasor          [loop phasor, 0..1)
                                               |
                                          * slices           [0, slices)
                                               |
                                          wrap 0 1           [slice phasor, 0..1)
                                           ↙     ↘
               delta → abs → > 0.5            +  ← latch
               (slice boundary trigger)        ↓       ↑
                        ↓                  / slices  floor(phasor*slices*jump
                    latch[1]               / slices   + noise*chance)
                                               ↓
                                          wrap 0 1           [final, 0..1)
                                               ↓
                                         sample myloop → out 1 sound
                                         out 2 phasor

  The outer beatslicer (obj-9) uses the same topology, implemented via
  go.ramp2steps / go.ramp2trig higher-level abstractions from the GSOT go
  library.  The underlying signal flow is identical.

  ── go.ramp2steps / go.ramp2trig ─────────────────────────────────────────────
  These are GSOT utility gen~ subpatchers (not standard gen~ objects):
  - go.ramp2steps: given a ramp and N steps, produces step index, step phase,
    step trigger, and step-normalized position.
  - go.ramp2trig: ramp-to-trigger — fires when ramp makes a large backward jump.

  These are pedagogical wrappers around the same delta-abs-threshold pattern.
  Alembic does not have `:ramp2steps` or `:ramp2trig` ops; the explicit
  composition below is both more transparent and Alembic-idiomatic.

  ── Vocabulary gap resolved: :floor ──────────────────────────────────────────
  This example required adding `:floor` to Alembic (ops.clj + emit.clj).
  It lowers to Faust's `floor()` — round toward −∞, stateless, polymorphic.
  The integer beat slicer requires floor for correct behavior: without it,
  the latch offset would be a continuous value like 7.3 rather than an integer
  slice index 7, causing sub-slice position mixing between slices.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = hslider(\"bpm\", 140.0, 20.0, 300.0, 0.028);
      n1 = 60.0;
      n2 = (n0 / n1);
      n3 = hslider(\"numbeats\", 8.0, 1.0, 64.0, 0.0063);
      n4 = (n2 / n3);
      n5 = os.phasor(1.0, n4);
      n6 = hslider(\"slices\", 16.0, 1.0, 64.0, 0.0063);
      n7 = (n5 * n6);
      n8 = 0.0;
      n9 = 1.0;
      n10 = (n8 + fmod((n7 - n8), (n9 - n8)));
      n11 = (n10 - n10');
      n12 = abs(n11);
      n13 = 0.5;
      n14 = (float(n12 > n13));
      n16 = (n5 * n6);
      n17 = hslider(\"jump\", 1.235, 0.0, 4.0, 0.0004);
      n18 = (n16 * n17);
      n19 = no.noise;
      n20 = hslider(\"chance\", 0.2, 0.0, 1.0, 0.0001);
      n21 = (n19 * n20);
      n22 = (n18 + n21);
      n23 = floor(n22);
      n24 = (ba.sAndH(n14, n23));
      n25 = (n10 + n24);
      n26 = (n25 / n6);
      n27 = 0.0;
      n28 = 1.0;
      n29 = (n27 + fmod((n26 - n27), (n28 - n27)));
      n30 = hslider(\"loop-length\", 44100.0, 1.0, 8820000.0, 881.9999);
      n31 = (n29 * n30);
      n32 = ((sf_n32(0, int(max(0.0, n31)))) : (_, !, !))
        with {
          sf_n32 = soundfile(\"sf_n32[url:{'resources/jongly.aif'}]\", 1);
        };

      process = n32, n29;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! phasor-beat-slicer
  {:params {:bpm         {:range [20.0 300.0]    :default 140.0  :unit :bpm}
            :numbeats    {:range [1.0 64.0]       :default 8.0    :unit :beats}
            :slices      {:range [1.0 64.0]       :default 16.0}
            :jump        {:range [0.0 4.0]        :default 1.235}
            :chance      {:range [0.0 1.0]        :default 0.2}
            :loop-length {:range [1.0 8820000.0]  :default 44100.0 :unit :samples}}}
  (let [hz      (div (div (param :bpm) (const 60.0)) (param :numbeats))
        phase   (phasor hz)
        slices  (param :slices)

        ; slice phasor: N sub-cycles per loop, each [0,1)
        sl-ph   (wrap (mul phase slices) (const 0.0) (const 1.0))

        ; trigger at each slice boundary: abs(delta(slice_ph)) > 0.5
        trig    (comparator (abs (delta sl-ph)) (const 0.5))

        ; jump offset: floor(phase*slices*jump + noise*chance)
        jmp-raw (add (mul (mul phase slices) (param :jump))
                     (mul (noise) (param :chance)))
        offset  (sample-hold (floor jmp-raw) (:out trig))

        ; select slice: (slice_ph + offset) / slices → wrap 0 1 → sample index
        addr    (wrap (div (add sl-ph offset) slices) (const 0.0) (const 1.0))
        pos     (mul addr (param :loop-length))
        sound   (audio-file {:path "resources/jongly.aif"} pos)]
    (output :sound sound)
    (output :ramp  addr)))
