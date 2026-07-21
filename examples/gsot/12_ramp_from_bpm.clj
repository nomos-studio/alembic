; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.12-ramp-from-bpm
  "GSOT pp.37-39 — ramp_from_bpm + go.ramp.frombpm: ramp clock multiplication.

  p.37 — 'A collection of ramp processors'
  -----------------------------------------
  The page re-states the ramp processing vocabulary established in examples
  10-11 as a reference list.  Four operations on a [0,1) ramp that preserve
  [0,1) and form the toolkit for all subsequent timing constructions:

      1. scale  — (* N)                   accelerate/decelerate playback
      2. wrap   — (* N → wrap 0 1)        N sub-cycles per parent cycle
      3. offset — (+ offset → wrap 0 1)   shift start point
      4. step   — (* N → floor → / N)     quantise to N equal positions

  No new maxpat.  The section sets up the vocabulary before showing clock
  multiplication on pp.38-39.

  pp.38-39 — 'Ramp clock multiplication'
  ----------------------------------------
  The key concept: a slow measure-rate phasor multiplied by the number of
  beats in a measure, then wrapped to [0,1), gives a beat-rate ramp.

      measure_hz  = BPM / 60 / beats
      measure_phasor = phasor(measure_hz)      [0,1) per measure
      beat_ramp   = wrap(measure_phasor * beats, 0, 1)   [0,1) per beat

  This is the identical `* N → wrap 0 1` from the beat slicer and loop
  processor, now applied to the relationship between measures and beats.
  A measure phasor at BPM=120, beats=4 runs at 0.5 Hz; multiplied by 4
  and wrapped gives a 2 Hz beat ramp — four beats per measure.

  Building progression (building subpatch gen~ @title bpm)
  ---------------------------------------------------------
  Step 1 — simple form (obj-1, beat ramp only):
  Comments: 'convert BPM to Hz', 'convert to logical on/off (1 or 0)',
  'set rate to zero when paused', 'trigger here to reset phasor to zero'

      in 1 BPM → / 60 → * bool(enabled) → phasor ← [in 2 reset]
                                                |
                                           wrap 0 1 → out 1 beat-ramp

  `bool(enabled)` converts the enabled param to 0 or 1.  Multiplying the Hz
  by 0 freezes the phasor at its current phase — a clean play/pause without
  a discontinuity in the ramp signal.  Alembic maps to `(comparator enabled 0.5)`.

  Step 2 — full form with clock multiplication (obj-37, beat + measure ramps):

      in 1 BPM → / 60 → * bool(enabled) → / int(beats) → phasor ← [in 2 reset]
                                                                 |           |
                                                   out 2 measure-ramp    * int(beats)
                                                                              |
                                                                         wrap 0 1
                                                                              |
                                                                   out 1 beat-ramp

  The phasor frequency is now `BPM/60 * enabled / beats` = measure rate.
  `phasor * beats → wrap 0 1` gives beat rate — the multiplication output.
  `int(beats)` coerces the param to an integer before use as a multiplier;
  Alembic maps this to `(floor (param :beats))`.

  go.ramp.frombpm.gendsp — the GSOT go library abstraction
  ----------------------------------------------------------
  go.ramp.frombpm.gendsp is the extracted, reusable version of the full form.
  It is the object referenced in examples 09, 10, 11 as 'see also go.ramp.frombpm'.
  Its signal flow is identical to obj-37 above; it adds nothing new architecturally.
  The book makes this explicit: what you build in the patch IS the utility.

  Inlets:  in 1 BPM @default 120,  in 2 reset
  Params:  enabled (play/pause, default 1),  beats (per measure, default 4)
  Outlets: out 1 beat-ramp [0,1) per beat,  out 2 measure-ramp [0,1) per measure

  In Alembic terms, go.ramp.frombpm is a named composition — a candidate for
  a `:bpm-ramp` op or a `defbpm` authoring convenience.  Until then, the
  explicit form below is the idiomatic expression.

  Vocabulary gaps
  ---------------
  1. Phasor reset — `in 2 reset → phasor[1]` resets phase to 0 on a rising
     edge.  Alembic `:phasor` (os.phasor) has no reset inlet; see examples 07
     and 09.  A future `:phasor-reset` op would lower to the manual
     `(select2(rst > 0.5, 0.0, _ + slope) ~ _)` cycle.

  2. `go.ramp.frombpm` as a named op — the book names this abstraction
     explicitly and marks it 'see also' in five prior examples (09-11).
     A `:bpm-ramp` op (or a macro-level `defbpm!`) would encapsulate the
     full form with both beat and measure outputs, enabled gating, and reset.

  See also — nomos-studio beat time
  -----------------------------------
  The `:beat-bpm` / `:beat-phase` / `:beat-trigger` ops in Alembic implement
  the host-transport analog of go.ramp.frombpm.  In a CLAP host, the transport
  provides BPM as a first-class field (`:beat-bpm`) and the beat phase directly
  (`:beat-phase` = the beat-ramp output, without any phasor).

  go.ramp.frombpm is the self-contained version that does not require a host
  transport.  `:beat-phase` is the correct production idiom when a CLAP host
  (or Ableton Link via nomos-rt) owns the clock.  See example 09 for the full
  note.

  The percussion sub-patch (outer gen~ @title percussion)
  --------------------------------------------------------
  The outer patch feeds go.ramp.frombpm's beat-ramp output into a percussion
  synthesiser with two nested gen~ sub-patches:

  gen @title kick:
      beat-ramp → go.ramp2trig → trigger
      env: (mix 0.003 → history) — decaying one-pole envelope
      osc: env * 330 Hz → cycle (sine) * env → out 1

  gen @title crash:
      measure-ramp → go.ramp2trig → trigger
      env: (mix 0.00002 → history) — longer one-pole decay
      filtered noise: (noise → mix 0.99 → history → mix 0.99) — two-pole IIR
      out = (filtered_noise - filtered_noise_delayed) * env * 0.04

  Both use `go.ramp2trig` (the GSOT utility, same mechanism as the delta-abs-
  threshold in example 11) to fire a gate from the ramp's wrap discontinuity.
  These are consumers of the clock; the synthesis itself is not the focus of
  pp.37-39.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = hslider(\"bpm\", 120.0, 20.0, 300.0, 0.028);
      n1 = 60.0;
      n2 = (n0 / n1);
      n3 = hslider(\"enabled\", 1.0, 0.0, 1.0, 0.0001);
      n4 = 0.5;
      n5 = (float(n3 > n4));
      n7 = hslider(\"beats\", 4.0, 1.0, 16.0, 0.0015);
      n8 = floor(n7);
      n9 = (n2 * n5);
      n10 = (n9 / n8);
      n11 = os.phasor(1.0, n10);
      n12 = (n11 * n8);
      n13 = 0.0;
      n14 = 1.0;
      n15 = (n13 + fmod((n12 - n13), (n14 - n13)));

      process = n15, n11;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! ramp-from-bpm
  {:params {:bpm     {:range [20.0 300.0] :default 120.0 :unit :bpm}
            :beats   {:range [1.0 16.0]   :default 4.0   :unit :beats}
            :enabled {:range [0.0 1.0]    :default 1.0}}}
  (let [bpm-hz   (div (param :bpm) (const 60.0))
        gate     (comparator (param :enabled) (const 0.5))
        beats    (floor (param :beats))
        hz       (div (mul bpm-hz (:out gate)) beats)
        measure  (phasor hz)
        beat     (wrap (mul measure beats) (const 0.0) (const 1.0))]
    (output :beat-ramp    beat)
    (output :measure-ramp measure)))
