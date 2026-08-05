; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.42-portamento
  "GSOT pp.76-77 — accum-change-template / portamento: glide on input change.

  'Glides and portamento' (Chapter 3)
  ------------------------------------
  The smooth-step LFO (examples 39-41) glides between targets driven by a
  free-running phasor.  Portamento inverts the control: instead of a ramp
  deciding WHEN to update, the INPUT SIGNAL decides — glide begins whenever
  the input value changes.

  gen~ topology (accum-change-template):

      in 1 ─→ change ─→ accum @resetmode pre ─→ clip 0 1 ─→ mix ─→ out
                  │                                           ↑    ↑
                  ├─→ latch(to, gate)  ───────────────────────┘    │
                  │         ↑ input                                 │
                  └─→ latch(from, gate) ──────────────────────────┘
                             ↑ history(out)

  Three mechanisms differ from smooth-step:

  1. Trigger source: `change` (fires when input value changes) instead of
     go.ramp2trig (fires at phasor wrap).

  2. Ramp generator: `accum @resetmode pre` instead of `phasor`.
     At trigger: output = slope (= 1 sample of progress, not 0).
     Between triggers: accumulates at slope per sample.
     Clips at 1.0 and holds there until next input change.

  3. from-value: `history(out)` — previous sample of the OUTPUT, not previous
     target.  This lets interrupted glides pick up from mid-way if a new note
     arrives before the ramp reaches 1.0.

  Alembic approximation
  ---------------------
  history(out) creates a circular dependency (out → from → out).  Alembic
  nodes form a strict DAG, so we substitute to' (previous sample of the `to`
  S&H node) as the from-latch input:

      from = (select2(trig > 0.5, _, to') ~ _)

  Semantics: when `trig` fires, `to'` = the value `to` held one sample
  earlier = the previous target.  When glides complete (ramp clips to 1.0
  before the next trigger), out ≈ to, so to' ≈ history(out).  For
  interrupted glides (new note before ramp reaches 1.0), the glide restarts
  from the PREVIOUS TARGET rather than the current partial-output position.
  For most musical contexts this is indistinguishable.

  Faust accum @resetmode pre
  --------------------------
      (select2(trig > 0.5, _ + slope, slope)) ~ _

  When trig > 0.5 (change fires, s=1): output = slope   (reset to slope)
  When trig ≤ 0.5 (no change,     s=0): output = _ + slope (accumulate)

  The outer `~ _` feeds the previous output as the inner `_`.  Output starts
  at `slope` (not 0) on the trigger sample — this is the @resetmode pre
  semantics: reset-then-increment.  Clipping stops the ramp at 1.0 where it
  holds until the next change.

  Change detection
  ----------------
      float(abs(x - x') > 0.5)

  Threshold 0.5 is safe for MIDI note values (discrete steps ≥ 1.0).  For
  continuously modulated signals use a smaller threshold or > 0.0.

  Slope calculation
  -----------------
      slope = 1000.0 / (ma.SR × glide_ms)

  Gives the per-sample increment so that sum over glide_ms milliseconds = 1.0.

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = hslider(\"glide-ms\", 300.0, 1.0, 5000.0, 0.4999);

      alembic_dsp(n1) = n8, n5
        with {
          n2 = float(abs(n1-n1')>0.5);
          n3 = 1000.0/(ma.SR*n0);
          n4 = (select2(n2>0.5,_+n3,n3))~_;
          n5 = max(0.0,min(1.0,n4));
          n6 = (select2(n2>0.5,_,n1)~_);
          n7 = (select2(n2>0.5,_,n6')~_);
          n8 = n7+n5*(n6-n7);
        };
      process = alembic_dsp;

  n2 = trig    (change detection: fires when input differs by > 0.5)
  n3 = slope   (per-sample increment: 1/(samplerate × glide_ms / 1000))
  n4 = ramp    (accumulator: climbs from slope to ∞; clipped)
  n5 = clipped (ramp clamped to [0,1])
  n6 = to      (S&H of input on trigger — new target)
  n7 = from    (S&H of to' on trigger — previous target as start)
  n8 = out     (from + clipped × (to − from) — linear portamento)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! portamento
  {:params {:glide-ms {:range [1.0 5000.0] :default 300.0 :unit :ms}}}
  (let [x       (audio-in)
        ; fires when input changes by more than 0.5 (safe for MIDI pitch steps)
        trig    (faust "float(abs(%{inp}-%{inp}')>0.5)" {:inp x})
        ; per-sample increment: ramp reaches 1.0 after glide-ms milliseconds
        slope   (faust "1000.0/(ma.SR*%{gms})" {:gms (param :glide-ms)})
        ; accum @resetmode pre: resets to slope at trigger, accumulates between
        ramp    (faust "(select2(%{trig}>0.5,_+%{slope},%{slope}))~_"
                       {:trig trig :slope slope})
        clipped (faust "max(0.0,min(1.0,%{r}))" {:r ramp})
        ; to = new target: S&H of input on trigger
        to      (track-hold x trig)
        ; from = S&H of to' (previous target) on trigger
        from    (faust "(select2(%{trig}>0.5,_,%{tgt}')~_)" {:trig trig :tgt to})
        out     (faust "%{from}+%{ramp}*(%{tgt}-%{from})" {:from from :ramp clipped :tgt to})]
    (output out)
    (output :ramp clipped)))
