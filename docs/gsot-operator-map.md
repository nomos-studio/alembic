# GSOT operator map

Mapping from the gen~ operator vocabulary in *Generating Sound and Organizing
Time* (Wakefield & Taylor, MIT Press) to Alembic ops and idioms.  Organised
by chapter.

Where a gen~ operator has a direct Alembic analog the mapping is one-to-one.
Where no dedicated op exists the table notes it as a **vocabulary gap** — a
pattern common enough to deserve a named op that lowers to Faust internally.
The `(faust "...")` escape is reserved for genuinely one-off user-defined
expressions, not as a wrapper around Faust for patterns that recur.  Naming
concepts is the point of the DSL; `(faust "0.5*(1-cos(2*ma.PI*%ph))")` is not
vocabulary, `(window :hann phase)` is.

---

## Chapter 1 — Operators

### Signal I/O

| gen~ | Alembic | Notes |
|---|---|---|
| `param name default` | `(param :name)` | Block-rate control inlet; range declared in patch `:params` schema |
| `in N` | `(audio-in)` | One `(audio-in)` node per audio inlet; two nodes → stereo input pair |
| `out N` | `(output expr)` or `(output :name expr)` | Named or positional audio outlet |

### Memory and delay

| gen~ | Alembic | Notes |
|---|---|---|
| `history` | `(history signal)` | 1-sample unit delay; the primitive for IIR feedback |
| `delay N` | `(delay signal {:max-time t :smooth true})` | Glitch-free delay via `de.sdelay`; `:smooth false` for raw Doppler; `:time-cv true` for audio-rate time modulation |

### Waveshaping

| gen~ | Alembic | Notes |
|---|---|---|
| `wrap lo hi` | `(wrap signal lo hi)` | Modulo wrap into `[lo, hi)` |
| `fold lo hi` | `(fold signal lo hi)` | Triangle fold into `[lo, hi]` |
| `clip lo hi` | `(clip signal lo hi)` | Hard clip to `[lo, hi]` |

### Accumulators and phasors

| gen~ | Alembic | Notes |
|---|---|---|
| `accum` | `(faust "((_ + %in) ~ _)" {:in signal})` | Running sum with 1-sample feedback; gen~ also has carry/reset variants — add `:reset` inlet via additional `select2` term if needed |
| `phasor` | `(phasor freq)` | Normalised ramp `[0, 1)` at `freq` Hz; wraps at 1.0 |

### Differentiation

| gen~ | Alembic | Notes |
|---|---|---|
| `delta` | `(delta signal)` | `x - x'` — first difference; audio-rate differentiation |
| `change` | `(faust "select2(%in > %in', select2(%in < %in', 0.0, -1.0), 1.0)" {:in signal})` | Direction-only delta: +1 rising, −1 falling, 0 stable; gen~ `change` output |

### Sampling and gating

| gen~ | Alembic | Notes |
|---|---|---|
| `latch` | `(sample-hold trigger signal)` | Captures `signal` on rising edge of `trigger`; `(track-hold gate signal)` for level-triggered variant |

### Mixing and selection

| gen~ | Alembic | Notes |
|---|---|---|
| `mix pos a b` | `(crossfade pos a b)` | Linear blend; `pos=0` → full `a`, `pos=1` → full `b` |
| `switch index a b ...` | `(select {:n N} index in-0 in-1 ...)` | N-to-1 hard switch; index selects active input |

### Tables and buffers

| gen~ | Alembic | Notes |
|---|---|---|
| `peek buffer index` | `(table {:data [...] :mode :wrap} index)` | Read-only wavetable lookup; `:mode` controls boundary: `:wrap`, `:clamp`, `:fold` |
| `poke buffer index value` / `peek` | `(buffer {:size N} write-pos in read-pos)` | Read-write table via Faust `rwtable`; write and read positions are independent |
| `data name size` | `:data` key in `(table ...)` opts | Inline data vector; file-backed tables are not yet supported |

### Escape hatches

| gen~ | Alembic | Notes |
|---|---|---|
| `codebox` | `(faust "expr" {:inlet signal ...})` | Raw Faust expression with named inlet substitution; `%inlet-name` placeholders replaced by wired node identifiers |
| `gen~ / p` (abstraction) | `defpatch!` | Defines a named patch; multi-output via `(output :name expr)`; compose by binding patch outputs to node identifiers |

---

## Chapter 1 — The `go` library

The `go` library ships with GSOT as a set of gen~ abstractions organised into
functional families.  Below each family is mapped to its Alembic equivalent.
Where a direct op exists the mapping is one-to-one; where it does not, the
idiomatic form uses the `(faust "...")` escape.

### Filters

| go | Alembic | Notes |
|---|---|---|
| `go.onepole` | `(one-pole in cutoff)` | Trapezoidal 1-pole LP; `go.onepole.hz/ms/samples/seconds` are time-unit variants of the same |
| `go.svf` | `(svf input cutoff resonance mode)` | go.svf is bilinear-transform with 8 named outputs (lp/hp/bp/ubp/notch/peak/bshelf/ap); Alembic `:svf` exposes a morph blend — named outputs require `:naive-svf` + `:naive-svf-hp`, or `(faust "fi.svf_morph(...)")` with per-output wiring |
| `go.biquad.lp/hp/bp` | `(faust "fi.lowpass(2,...)")` etc. | go.biquad family is RBJ 2nd-order; Alembic `:shelf-lo`, `:shelf-hi`, `:peak-eq` cover hs/ls/np directly; other modes via faust |
| `go.biquad4.*` | `(faust "fi.lowpass(4,...)")` etc. | 4th-order (two cascaded biquads) |
| `go.allpass` | `(allpass in time coeff)` | Direct; `go.allpass.hz/stable` are frequency-parameterised variants |
| `go.allpass.diffuser.stereo` | `(faust "...")` | Stereo diffuser (4 allpass stages); composition of `:allpass` nodes |
| `go.crossover` | `(crossover in cutoff)` + `(crossover-hp in cutoff)` | LP+HP pair; go.crossover2/3 are 2nd/3rd order variants |
| `go.lowpass.elliptic5.sr4` | `(faust "fi.lowpass(5,...)")` | 5th-order elliptic LP at SR/4; specialist use |

### Slew and lag

| go | Alembic | Notes |
|---|---|---|
| `go.slewlimit` | `(slew in rise fall)` | Asymmetric RC slew; `go.slewlimit.hz/ms/samples/seconds` are time-unit variants |
| `go.slewlimit2` | `(slew in rise fall)` | Two-sided variant; same op |
| `go.slewdam` | **Vocabulary gap** — `(slew-dam in rise fall damp)` | Slew with additional damping term; distinct enough from `:slew` to name |
| `go.slide` / `go.slide2` | `(smooth in)` or `(one-pole in cutoff)` | Exponential lag (portamento) |
| `go.vactrol` | **Vocabulary gap** — `(vactrol in attack release)` | Optical isolator model with nonlinear curve; a named concept in modular synthesis |

### Ramp and phasor utilities

All ramp utilities take a normalised phasor `[0, 1)` as input and produce a
transformed ramp.  `:phasor` is the Alembic primitive; the transformations
below are expressed via `(faust "...")`.

| go | Alembic / notes |
|---|---|
| `go.ramp2trig` | `(beat-trigger phasor)` — 1-sample pulse on wrap |
| `go.ramp2freq` | `(faust "(%ramp - %ramp') * ma.SR" {:ramp ph})` — instantaneous frequency from phase derivative |
| `go.ramp2slope` | `(delta phasor)` — slope = `ph - ph'` |
| `go.ramp2steps` | `(faust "floor(%ramp * %n)" {:ramp ph :n steps})` — quantise ramp to N steps |
| `go.ramp.div` / `go.ramp.mul` | `(faust "fmod(%ramp * %n, 1.0)" {:ramp ph :n n})` — frequency multiply/divide |
| `go.ramp.swing` | **Vocabulary gap** — `(ramp-swing ramp amount)` — displaces the midpoint of a phasor; essential timing concept |
| `go.ramp.ratchet` | **Vocabulary gap** — `(ramp-ratchet ramp gate divisions)` — subdivides the ramp within a gate window |
| `go.ramp.euclidean` | **Vocabulary gap** — `(ramp-euclidean ramp steps pulses)` — Euclidean rhythm from phasor |
| `go.ramp.rotate` | `(wrap (add ramp offset) 0.0 1.0)` — composable from existing ops |
| `go.ramp.subsample` | **Vocabulary gap** — `(ramp-subsample ramp factor)` |
| `go.ramp.frombpm` | `(phasor (div bpm 60.0))` — phasor at BPM rate |
| `go.ramp.aa` | `(faust "...")` — anti-aliased ramp (PolyBLEP correction) |

### Unit-domain waveshapes

These take a normalised phasor `[0, 1)` and output a shaped waveform.
Alembic's waveform ops are already phasor-driven and map directly.

| go | Alembic |
|---|---|
| `go.unit.sine` | `(sine-bi phasor)` — bipolar; `(sine-uni phasor)` — unipolar |
| `go.unit.triangle` | `(tri phasor)` |
| `go.unit.pulse` | `(rect phasor width)` |
| `go.unit.trapezoid` | `(segment phase shape curve)` — morphable slope/plateau |
| `go.unit.kink` | `(faust "...")` — piecewise linear with variable knee point |
| `go.unit.cubic` / `go.unit.spline` | `(faust "...")` — higher-order curve fits |
| `go.unit.lfo` | `(sine-bi (phasor freq))` — shorthand for the common LFO pattern |

### Window functions

**Vocabulary gap** — `(window :hann phase)`, `(window :hamming phase)`, etc.
Window functions are a coherent family: phasor → weight `[0, 1]`.  They
deserve a single named op with a `:type` keyword rather than inline Faust
expressions.  The Faust lowering is a closed-form expression per type.

| go | Proposed op |
|---|---|
| `go.unit.hann` | `(window :hann phase)` |
| `go.unit.hamming` | `(window :hamming phase)` |
| `go.unit.blackman` | `(window :blackman phase)` |
| `go.unit.blackmanharris` | `(window :blackman-harris phase)` |
| `go.unit.gauss` | `(window :gauss phase)` |
| `go.unit.flattop` | `(window :flat-top phase)` |
| others | `(window :type phase)` — type keyword selects the closed-form expression |

### Easing and curve functions

**Vocabulary gap** — `(ease :sine t)`, `(ease :pow t exp)`, etc.  Easing
functions are a coherent family (same signature, different curve) and belong
alongside `:segment` as named shape ops.

| go | Proposed op |
|---|---|
| `go.unit.ease.pow` | `(ease :pow t exp)` |
| `go.unit.ease.sine` | `(ease :sine t)` |
| `go.unit.ease.exp` | `(ease :exp t)` |
| `go.unit.ease.back` / `.elastic` / `.circle` | `(ease :back t)` etc. |

### LFO

| go | Alembic |
|---|---|
| `go.lfo.simple` | `(sine-bi (phasor (param :rate)))` |
| `go.lfo` | `(crossfade shape (tri ph) (sine-bi ph))` — shape-morphing LFO |
| `go.lfo.multi` | multi-output composition of unit shapes at a common phasor |

### Sigmoid and saturation

| go | Alembic |
|---|---|
| `go.sigmoid.softclip` | `(soft-clip in)` — `x/(1+|x|)` |
| `go.sigmoid.tanh` | `(faust "tanh(%in)" {:in signal})` |
| `go.sigmoid.atan` | `(faust "(2.0/ma.PI)*atan(%in)" {:in signal})` |
| `go.sigmoid.cubic` | `(hard-clip in)` — cubic polynomial approximation |
| `go.sigmoid.logistic` | `(faust "1.0/(1.0+exp(-%in))" {:in signal})` |
| `go.sigmoid.gudermann` | `(faust "(2.0/ma.PI)*atan(tanh(%in*ma.PI/2.0))" {:in signal})` |
| `go.sigmoid2` | `(faust "...")` — two-parameter generalised sigmoid |

### Gate and trigger logic

| go | Alembic |
|---|---|
| `go.schmitt` | `(hysteresis in threshold width)` — direct analog |
| `go.gate2trig` | **Vocabulary gap** — `(gate->trig gate)` — rising-edge pulse; common enough to name |
| `go.kink2trig` | **Vocabulary gap** — `(kink->trig in threshold)` |
| `go.zerox` | **Vocabulary gap** — `(zero-cross in)` — zero-crossing detector |
| `go.latchsync` | `(sample-hold trigger signal)` |
| `go.compare` | `(comparator in threshold)` |
| `go.chance` / `go.bern` | **Vocabulary gap** — `(chance prob)` — probability gate; named concept in modular synthesis |

### Shift registers

**Vocabulary gap** — `(shift-register {:stages 8} clock signal)`.  A clocked
shift register is a named concept with clear parameters; it should not be
expressed as chained sample-hold nodes or inline Faust string construction.
The Faust lowering uses `ba.latch` stages chained with 1-sample feedback.

### Chaos and ODE systems

**Vocabulary gap** — `(chaos :lorenz dt)`, `(chaos :rossler dt)`, etc.  The
`go.chaos.*` family implements ~30 strange attractor systems via 1-sample
Euler integration of coupled ODEs.  Each system is a named concept with known
constants and a standard 3-output signature (x, y, z).  They belong as named
ops — `(chaos :system dt reset)` — not as raw Faust string construction.  The
Faust lowering uses `~ _` per dimension with optional reset via `select2`.

The chaos family is large enough to warrant its own namespace entry
(`:chaos {:system :lorenz}` with a `:system` keyword, or individual op names
like `:lorenz`, `:rossler`).  The go library has ~30 systems; all have the
same interface and the same Euler integration pattern.

### Noise and random

| go | Alembic |
|---|---|
| `go.random` | `(noise)` — white noise uniform `[-1, 1]` |
| `go.noise.normal` | **Vocabulary gap** — `(gaussian-noise)`.  Normally distributed noise is a named concept; it belongs as a named op, not an inline CLT approximation. |

### Conversion utilities

| go | Alembic |
|---|---|
| `go.bi2unipolar` | `(mul (add signal 1.0) 0.5)` — composable from existing ops |
| `go.uni2bipolar` | `(sub (mul signal 2.0) 1.0)` — composable |
| `go.hz2octave` | **Vocabulary gap** — `(hz->oct signal)` |
| `go.octave2hz` | **Vocabulary gap** — `(oct->hz signal)` |
| `go.midi2octave` | **Vocabulary gap** — `(midi->oct signal)` |
| `go.octave2midi` | **Vocabulary gap** — `(oct->midi signal)` |
| `go.proportion` | **Vocabulary gap** — `(scale signal lo hi)` — linear map from `[0,1]` to `[lo,hi]`; common enough to name |
| `go.equalpower` | **Vocabulary gap** — `(equal-power pos)` — two-output equal-power pan law |

### Quantize

| go | Alembic |
|---|---|
| `go.quantize` | `(faust "floor(%in * %n + 0.5) / %n" {:in signal :n steps})` |
| `go.quantize.smooth` / `go.quantize2.smooth` | `(smooth (faust "..."))` — same with one-pole lag |

### Line generators

`go.line.*` produce a linear ramp from a start to an end value over a given
duration (in hz, ms, samples, or seconds).  In Alembic the closest native
equivalent is `:slew` driven by a step; for sample-accurate linear segments
use `(faust "...")`.

### Pitch shift

`go.shift.linear/cosine/cubic/spline/spline6` are variable-delay pitch
shifters with different interpolation qualities.  All use `(faust "...")` with
`de.fdelay` variants.

### Simple synthesis

`go.simple.kick/snare/hihat` are self-contained single-voice drum voices.
These are natural Alembic compositions:

- **kick**: `(vca (sine-bi (phasor (add base-freq pitch-env))) amp-env)` with `:ar-env` for both pitch drop and amplitude
- **snare**: noise filtered through `:svf` with short `:ar-env`
- **hihat**: filtered noise through `:svf` in HP mode with very short `:ar-env`

### Harmonic

`go.harmonic` is an additive oscillator summing harmonics from a phasor.
Expressible as a sum of `(sine-bi (mul phasor N))` terms for harmonics 1..N,
or via `(faust "...")` with a loop if N is large.

---

## Vocabulary gaps summary

The go library survey reveals several families of missing Alembic vocabulary.
These are not faust-escape territory — they are named concepts that belong in
the DSL and lower to Faust internally.

| Family | Proposed ops |
|---|---|
| Window functions | `(window :hann/:hamming/:blackman/... phase)` |
| Easing curves | `(ease :sine/:pow/:exp/:back/... t)` |
| Chaos / ODE systems | `(chaos :lorenz/:rossler/... dt)` or individual named ops |
| Shift registers | `(shift-register {:stages N} clock signal)` |
| Gaussian noise | `(gaussian-noise)` |
| Pitch conversion | `(hz->oct hz)`, `(oct->hz oct)`, `(midi->oct midi)`, `(oct->midi oct)` |
| Linear map | `(scale signal lo hi)` |
| Equal-power pan | `(equal-power pos)` — two outputs |
| Vactrol | `(vactrol in attack release)` |
| Slew with damping | `(slew-dam in rise fall damp)` |
| Ramp utilities | `(ramp-swing ramp amount)`, `(ramp-ratchet ramp gate div)`, `(ramp-euclidean ramp steps pulses)` |
| Gate/trigger | `(gate->trig gate)`, `(zero-cross in)`, `(chance prob)` |

## Notes on the faust escape

The `(faust "..." {:inlet signal})` form is for genuinely one-off expressions
that a user defines for their own purposes and that do not generalise into
named vocabulary.  The legitimate uses are narrow:

1. **Multi-node feedback cycles** — Faust's `~ _` combinator for loops that
   the Alembic graph model cannot represent without it (e.g. Karplus-Strong,
   running accumulators, ODE integration).  Even here, if the pattern recurs
   it should become a named op.

2. **Truly custom signal math** — expressions a user invents for a specific
   patch that have no generalised name.  If you find yourself naming an inlet
   and giving the expression a descriptive comment, that is a signal it should
   be an op.
