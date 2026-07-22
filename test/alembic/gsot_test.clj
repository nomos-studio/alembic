; SPDX-License-Identifier: EPL-2.0
(ns alembic.gsot-test
  "Validation suite for Alembic GSOT examples — Chapters 2-3, pp.22-58.

  Every example must:
    1. emit-faust — produce a non-empty Faust DSP string
    2. validate   — compile through `faust -lang cpp` without errors

  This is the ground truth for the examples.  If these tests pass, the
  emitted Faust is real and the examples are not hand-waving."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [alembic.emit :refer [emit-faust]]
            [alembic.compile :refer [validate]]
            ;; GSOT example namespaces (loaded from examples/gsot/ via dev source path)
            [examples.gsot.04-counter-play-a-buffer]
            [examples.gsot.05-counter-and-wrap]
            [examples.gsot.06-counter-and-wrap-buffer]
            [examples.gsot.07-phasor-counter]
            [examples.gsot.08-phasor-basic-table-oscillator]
            [examples.gsot.09-phasor-bpm]
            [examples.gsot.10-phasor-loop-processing]
            [examples.gsot.11-phasor-beat-slicer]
            [examples.gsot.12-ramp-from-bpm]
            [examples.gsot.13-ramp-to-steps]
            [examples.gsot.14-ramp-to-trig]
            [examples.gsot.15-ramp-phase-shift]
            [examples.gsot.16-ramp-rotate]
            [examples.gsot.17-ramp-to-slope]
            [examples.gsot.18-ramp-to-freq]
            [examples.gsot.19-ramp-to-trig-gendsp]
            [examples.gsot.20-ramp-div-simple]
            [examples.gsot.21-ramp-div]
            [examples.gsot.22-ramp-div-musical]
            [examples.gsot.23-ramp-bursts]
            [examples.gsot.24-ramp-bursts-to-trigs]
            [examples.gsot.25-ramp-bursts-shaped]
            ;; Chapter 3 — Unit shaping (p.58)
            [examples.gsot.26-unit-triangle]
            [examples.gsot.27-unit-trapezoid]
            [examples.gsot.28-unit-kink]
            [examples.gsot.29-unit-lfo]
            [examples.gsot.30-unit-pow]
            [examples.gsot.31-unit-arc]
            [examples.gsot.32-unit-cubic]
            [examples.gsot.33-unit-logistic]
            [examples.gsot.34-unit-ease-exp]
            [examples.gsot.35-unit-welch]
            [examples.gsot.36-unit-tukey]
            [examples.gsot.37-ramp-swing]
            ;; Chapter 3 — From ramps to LFOs (p.69)
            [examples.gsot.38-lfo-multi]
            ;; Chapter 3 — Smooth stepped interpolation (p.70)
            [examples.gsot.39-smooth-stepped]
            [examples.gsot.40-smooth-stepped-shaped]
            [examples.gsot.41-interpolating-lfo]
            ;; Chapter 3 — Glides and portamento (p.76)
            [examples.gsot.42-portamento]
            [examples.gsot.43-portamento-shaped]
            ;; Chapter 3 — Easing functions (p.77)
            [examples.gsot.44-easing-multi]
            ;; Chapter 3 — Window envelope functions (p.78)
            [examples.gsot.45-window-fixed]
            [examples.gsot.46-window-parametric]
            ;; Chapter 3 — Waveshaping bipolar signals (p.79)
            [examples.gsot.47-bipolar-waveshaping]
            ;; Chapter 3 — Audio waveshaping / polynomial shapers (p.81)
            [examples.gsot.48-chebyshev-waveshaping]
            ;; Chapter 3 — Sigmoid waveshaping (p.84)
            [examples.gsot.49-sigmoid-waveshaping]
            [examples.gsot.50-sigmoid-gsot]
            ;; Chapter 3 — Enveloped sigmoid waveshaping (p.86)
            [examples.gsot.51-sigmoid-enveloped]
            ;; Chapter 3 — Normalized sigmoids as unit shapers (p.87)
            [examples.gsot.52-unit-tanh]
            ;; Chapter 3 — Full set of normalized sigmoid unit shapers (p.88)
            [examples.gsot.53-unit-sigmoids]
            ;; Chapter 4 — Feel the noise (p.91)
            [examples.gsot.54-noise-basic]
            ;; Chapter 4 — Random range and random steps (p.93)
            [examples.gsot.55-random-range]
            [examples.gsot.56-random-steps]
            ;; Chapter 4 — Smooth stepped random (p.94)
            [examples.gsot.57-random-smoothed]
            ;; Chapter 4 — Spline interpolated random steps (p.95)
            [examples.gsot.58-spline-smoothed]
            ;; Chapter 4 — Probability gate (p.96)
            [examples.gsot.59-random-chance]
            ;; Chapter 4 — Bernoulli gate (p.97)
            [examples.gsot.60-bernoulli-gate]
            ;; Chapter 4 — Random periods (p.98)
            [examples.gsot.61-random-periods]
            ;; Chapter 4 — Normal distribution noise (p.100)
            [examples.gsot.62-normal-noise]
            ;; Chapter 4 — Uniform vs normal comparison (p.101)
            [examples.gsot.63-random-distributions]
            ;; Chapter 4 — Random walk (p.101-102)
            [examples.gsot.64-random-walk]
            ;; Chapter 4 — Bounded random walk with fold reflection (p.103)
            [examples.gsot.65-random-walk-bounded]
            ;; Chapter 4 — Random integers / quantized random (p.103)
            [examples.gsot.66-random-integer]
            ;; Chapter 4 — Urn model approximation (pp.104-109)
            [examples.gsot.67-random-urn]
            ;; Chapter 4 — Flexible urn: manual reshuffle + no-repeat (pp.110-111)
            [examples.gsot.68-random-urn-flexible]
            ;; Chapter 4 — Lorenz attractor as chaotic DSP source (pp.112-114)
            [examples.gsot.69-chaos-lorenz]
            ;; Chapter 4 — Running min/max tracker (pp.115-116)
            [examples.gsot.70-limits]
            ;; Chapter 4 — Auto-normalise using tracked limits (pp.116)
            [examples.gsot.71-autolimit]
            ;; Chapter 4 — Abstract chaos step; covers Lorenz/Lu-Chen family (pp.117)
            [examples.gsot.72-go-chaos]
            ;; Chapter 4 — Lu-Chen attractor, hardwired go.chaos variant (pp.118)
            [examples.gsot.73-go-chaos-liu-chen]
            ;; Chapter 4 — Chaotic tempo clock: logistic map drives phasor rate (pp.119-120)
            [examples.gsot.74-chaos-tempo-nonrobotic]
            ;; Chapter 4 — Lorenz with audio injected into x equation (pp.121)
            [examples.gsot.75-chaos-lorenz-audioinjection]
            ;; Chapter 5 — 8-step pitch sequencer with bitmask gate logic (pp.123-125)
            [examples.gsot.76-mixer-sequencer]
            ;; Chapter 5 — Step sequencer that captures its sequence from cv-in via S&H (pp.126)
            [examples.gsot.77-latched-sequencer]
            ;; Chapter 5 — 2-stage shift register subpatch (pp.127)
            [examples.gsot.78-go-shiftregister2]
            ;; Chapter 5 — 8-stage shift register subpatch; temporal canon (pp.127)
            [examples.gsot.79-go-shiftregister8]
            ;; Chapter 5 — Shift register canon with selectable imitation interval (pp.128)
            [examples.gsot.80-shift-register]
            ;; Chapter 5 — Binary shift register; Bernoulli input with fixed weight (pp.129-130)
            [examples.gsot.81-shift-register-weighted]
            ;; Chapter 5 — Binary shift register; per-trigger randomised weight (pp.130)
            [examples.gsot.82-shift-register-weighted-random]
            ;; Chapter 5 — Evolving loops: LFSR XOR feedback weighted vs random (pp.131-132)
            [examples.gsot.83-shift-register-weighted-xor]
            ;; Chapter 5 — Binary decoding: 8 shift register bits → integer index (pp.134-135)
            [examples.gsot.84-binary-decode]
            ;; Chapter 5 — Integers as patterns: counter reads integer bitmask into shift register (pp.136-139)
            [examples.gsot.85-shift-register-integer]
            ;; Chapter 5 — Bit primitives: unpack, shift, wrap, extract (pp.140-141)
            [examples.gsot.86-bit-unpack8]
            [examples.gsot.87-bit-shift]
            [examples.gsot.88-bit-wrap]
            [examples.gsot.89-bit-extract]
            ;; Chapter 5 — Rotating a bit sequence: trigger-driven accumulating rotation (pp.141-142)
            [examples.gsot.90-bit-rotate]
            ;; Chapter 5 — Euclidean rhythms via digitized ratio: floor(s*N/K) gate (pp.142-148)
            [examples.gsot.91-euclidean-rhythms]
            ;; Chapter 5 — Euclidean ratchets: R gate pulses per beat via countdown register (pp.149)
            [examples.gsot.92-euclidean-ratchets]
            ;; Chapter 5 — Euclidean LFO: fractional digitized ratio as sawtooth (pp.149)
            [examples.gsot.93-euclidean-lfo]
            ;; Chapter 5 — Pitch spaces: integer index → chromatic MIDI → Hz (pp.150-151)
            [examples.gsot.94-pitch]
            ;; Chapter 5 — Quantization: float degree → diatonic major scale → Hz (pp.151-152)
            [examples.gsot.95-pitch-quantized]
            ;; Chapter 5 — Euclidean scales: inverse digitized ratio maps N-note maximally-even scale (pp.153-154)
            [examples.gsot.96-quantizing-pitch]
            ;; Chapter 5 — Smooth-stepped quantization: lerp between hard floor and continuous ratio (pp.155-157)
            [examples.gsot.97-quantizing-pitch-smoothed]
            ;; Chapter 5 — Quantization as timbral shape: phasor → Euclidean staircase waveform (pp.157)
            [examples.gsot.98-quantizing-timbre]
            ;; Chapter 5 — Audio bitcrusher: audio [-1,1] → N-level Euclidean quantisation (pp.158)
            [examples.gsot.99-quantizing-audio-bitcrush]
            ;; Chapter 6 — One-pole lowpass filter: (1-a)*x[n] + a*y[n-1]; balance of time (pp.159-163)
            [examples.gsot.100-onepole-basic]
            ;; Chapter 6 — Vactrol LPG subpatch: asymmetric env follower drives one-pole coeff and VCA (pp.169-170)
            [examples.gsot.101-vactrol]
            ;; Chapter 6 — Lowpass gate: complete LPG patch; gate + audio → vactrol → gated filtered output (pp.171)
            [examples.gsot.102-lowpass-gate]
            ;; Chapter 6 — One-pole LP, Hz parameterization: a=exp(-2π*fc/SR); two-node coefficient+filter (pp.173)
            [examples.gsot.103-onepole-basic-hz]
            ;; Chapter 6 — Allpass filter: flat amplitude, phase-shifting; x[n-1] via @1, y[n-1] via ~_ (pp.175)
            [examples.gsot.104-allpass]
            ;; Chapter 6 — 4-stage phaser: 4 allpass in series + dry/wet mix; 2 notches per sweep (pp.176)
            [examples.gsot.105-phaser-4stage]
            ;; Chapter 6 — 8-stage phaser: 8 allpass in series + dry/wet mix; 4 notches per sweep (pp.177)
            [examples.gsot.106-phaser-8stage]
            ;; Chapter 6 — allpass Hz: bilinear transform coefficient from cutoff frequency (pp.178)
            [examples.gsot.107-allpass-hz]
            ;; Chapter 6 — biquad: second-order IIR, Direct Form II, 5 coefficients (pp.178-180)
            [examples.gsot.108-biquad]
            ;; Chapter 6 — biquad coefficient patches: 8 typed filters + type-selector (pp.181)
            [examples.gsot.109-biquad-lp]
            [examples.gsot.110-biquad-hp]
            [examples.gsot.111-biquad-bp]
            [examples.gsot.112-biquad-res]
            [examples.gsot.113-biquad-np]
            [examples.gsot.114-biquad-ap]
            [examples.gsot.115-biquad-ls]
            [examples.gsot.116-biquad-hs]
            [examples.gsot.117-biquad-coeffs]))

(defn- check [graph]
  (let [src (emit-faust graph)]
    (is (string? src) "emit-faust returns a string")
    (is (str/starts-with? src "import(\"stdfaust.lib\");") "starts with stdfaust import")
    (is (re-find #"process\s*=" src) "has a process declaration")
    (is (nil? (validate graph)) "compiles through faust -lang cpp")))

;; ---------------------------------------------------------------------------
;; Chapter 2 — Modular Arithmetic of Time
;; ---------------------------------------------------------------------------

(deftest gsot-04-counter-play-a-buffer
  (testing "p.23 counter_play_a_buffer.maxpat — accum + audio-file"
    (check examples.gsot.04-counter-play-a-buffer/counter-play-a-buffer)))

(deftest gsot-05-counter-and-wrap
  (testing "p.24 counter_and_wrap.maxpat — accum + wrap to duration"
    (check examples.gsot.05-counter-and-wrap/counter-and-wrap)))

(deftest gsot-06-counter-and-wrap-buffer
  (testing "p.24 counter_and_wrap_buffer.maxpat — accum + wrap + audio-file"
    (check examples.gsot.06-counter-and-wrap-buffer/counter-and-wrap-buffer)))

(deftest gsot-07-phasor-counter
  (testing "pp.25-27 phasor_counter.maxpat — phasor as Hz-rate buffer playhead"
    (check examples.gsot.07-phasor-counter/phasor-counter)))

(deftest gsot-08-phasor-basic-table-oscillator
  (testing "p.28 phasor_basic_table_oscillator.maxpat — phasor + table lookup = oscillator"
    (check examples.gsot.08-phasor-basic-table-oscillator/phasor-basic-table-oscillator)))

(deftest gsot-09-phasor-bpm
  (testing "pp.28-29 phasor_bpm.maxpat — BPM-clocked drum loop"
    (check examples.gsot.09-phasor-bpm/phasor-bpm)))

(deftest gsot-10-phasor-loop-processing
  (testing "pp.30-32 phasor_loop_processing.maxpat — scrub_and_rate ramp processing"
    (check examples.gsot.10-phasor-loop-processing/phasor-loop-processing)))

(deftest gsot-11-phasor-beat-slicer
  (testing "pp.33-36 phasor_beat_slicer.maxpat — random beat slicing"
    (check examples.gsot.11-phasor-beat-slicer/phasor-beat-slicer)))

(deftest gsot-12-ramp-from-bpm
  (testing "pp.37-39 ramp_from_bpm.maxpat + go.ramp.frombpm — clock multiplication"
    (check examples.gsot.12-ramp-from-bpm/ramp-from-bpm)))

(deftest gsot-13-ramp-to-steps
  (testing "p.39 go.ramp2steps — floor(x*n)/n staircase quantisation"
    (check examples.gsot.13-ramp-to-steps/ramp-to-steps)))

(deftest gsot-14-ramp-to-trig
  (testing "pp.39-41 go.ramp2trig — trigger from ramp wrap discontinuity"
    (check examples.gsot.14-ramp-to-trig/ramp-to-trig)))

(deftest gsot-15-ramp-phase-shift
  (testing "pp.41+ shifting ramps — phase rotation via offset + wrap"
    (check examples.gsot.15-ramp-phase-shift/ramp-phase-shift)))

(deftest gsot-16-ramp-rotate
  (testing "pp.41+ go.ramp.rotate — named phase-rotation processor (audio-in form)"
    (check examples.gsot.16-ramp-rotate/ramp-rotate)))

(deftest gsot-17-ramp-to-slope
  (testing "p.42 go.ramp2slope — conditioned delta; holds slope across wrap"
    (check examples.gsot.17-ramp-to-slope/ramp-to-slope)))

(deftest gsot-18-ramp-to-freq
  (testing "p.43 go.ramp2freq — slope * samplerate → Hz"
    (check examples.gsot.18-ramp-to-freq/ramp-to-freq)))

(deftest gsot-19-ramp-to-trig-gendsp
  (testing "pp.43-45 ramp_to_trig.maxpat + go.ramp2trig — processor form"
    (check examples.gsot.19-ramp-to-trig-gendsp/ramp-to-trig-gendsp)))

(deftest gsot-20-ramp-div-simple
  (testing "p.46 go.ramp.div.simple — freq-detection division, free-running phasor"
    (check examples.gsot.20-ramp-div-simple/ramp-div-simple)))

(deftest gsot-21-ramp-div
  (testing "p.47 go.ramp.div — phase-locked division via trigger counter mod N"
    (check examples.gsot.21-ramp-div/ramp-div)))

(deftest gsot-22-ramp-div-musical
  (testing "pp.48-49 go.ramp.div musical context — note-value subdivisions of beat ramp"
    (check examples.gsot.22-ramp-div-musical/ramp-div-musical)))

;; ---------------------------------------------------------------------------
;; Chapter 2 close — ramp bursts (pp.53-55)
;; ---------------------------------------------------------------------------

(deftest gsot-23-ramp-bursts
  (testing "p.53 ramp_bursts.maxpat — N fast sub-ramps within a burst window"
    (check examples.gsot.23-ramp-bursts/ramp-bursts)))

(deftest gsot-24-ramp-bursts-to-trigs
  (testing "p.54 go.ramp_bursts2trigs — trigger pulses from burst ramp"
    (check examples.gsot.24-ramp-bursts-to-trigs/ramp-bursts-to-trigs)))

(deftest gsot-25-ramp-bursts-shaped
  (testing "p.55 go.ramp_bursts_shaped — amplitude-weighted burst ramp (Chapter 2 close)"
    (check examples.gsot.25-ramp-bursts-shaped/ramp-bursts-shaped)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Unit shaping (p.58)
;; ---------------------------------------------------------------------------

(deftest gsot-26-unit-triangle
  (testing "p.58 go.unit.triangle — 1 - |2x-1|"
    (check examples.gsot.26-unit-triangle/unit-triangle)))

(deftest gsot-27-unit-trapezoid
  (testing "p.58 go.unit.trapezoid — piecewise linear rise-hold-fall"
    (check examples.gsot.27-unit-trapezoid/unit-trapezoid)))

(deftest gsot-28-unit-kink
  (testing "p.58 go.unit.kink — slope-change ramp with variable kink point"
    (check examples.gsot.28-unit-kink/unit-kink)))

(deftest gsot-29-unit-lfo
  (testing "p.58 go.unit.lfo — 0.5*(1-cos(2π·x)) raised cosine"
    (check examples.gsot.29-unit-lfo/unit-lfo)))

(deftest gsot-30-unit-pow
  (testing "p.58 go.unit.pow — x^p power-law curve"
    (check examples.gsot.30-unit-pow/unit-pow)))

(deftest gsot-31-unit-arc
  (testing "p.58 go.unit.arc — sqrt(x*(2-x)) quarter-circle arc"
    (check examples.gsot.31-unit-arc/unit-arc)))

(deftest gsot-32-unit-cubic
  (testing "p.58 go.unit.cubic — 3x²-2x³ smoothstep S-curve"
    (check examples.gsot.32-unit-cubic/unit-cubic)))

(deftest gsot-33-unit-logistic
  (testing "p.58 go.unit.logistic — 1/(1+exp(-k(x-0.5))) sigmoid"
    (check examples.gsot.33-unit-logistic/unit-logistic)))

(deftest gsot-34-unit-ease-exp
  (testing "p.58 go.unit.ease.exp — (exp(k·x)-1)/(exp(k)-1) exponential ease"
    (check examples.gsot.34-unit-ease-exp/unit-ease-exp)))

(deftest gsot-35-unit-welch
  (testing "p.58 go.unit.welch — 4x(1-x) parabolic arch"
    (check examples.gsot.35-unit-welch/unit-welch)))

(deftest gsot-36-unit-tukey
  (testing "p.58 go.unit.tukey — cosine-tapered window with flat top"
    (check examples.gsot.36-unit-tukey/unit-tukey)))

(deftest gsot-37-ramp-swing
  (testing "pp.59-61 ramp.swing.maxpat — go.unit.kink applied to beat ramp"
    (check examples.gsot.37-ramp-swing/ramp-swing)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — From ramps to LFOs (p.69)
;; ---------------------------------------------------------------------------

(deftest gsot-38-lfo-multi
  (testing "p.69 go.lfo.multi.gendsp — all 11 unit shapers on one ramp, 11 outputs"
    (check examples.gsot.38-lfo-multi/lfo-multi)))

(deftest gsot-39-smooth-stepped
  (testing "p.70 smooth-stepped-template / linear-stepped-noise — phasor-driven lerp with history feedback"
    (check examples.gsot.39-smooth-stepped/smooth-stepped)))

(deftest gsot-40-smooth-stepped-shaped
  (testing "p.70 shaped-stepped-noise — raised-cosine blend replaces linear mix factor"
    (check examples.gsot.40-smooth-stepped-shaped/smooth-stepped-shaped)))

(deftest gsot-41-interpolating-lfo
  (testing "Chapter 3 go.lfo — skewed triangle, arc-blend shape, symmetry, bipolar"
    (check examples.gsot.41-interpolating-lfo/interpolating-lfo)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Glides and portamento (p.76)
;; ---------------------------------------------------------------------------

(deftest gsot-42-portamento
  (testing "pp.76-77 accum-change-template — linear glide on input change"
    (check examples.gsot.42-portamento/portamento)))

(deftest gsot-43-portamento-shaped
  (testing "pp.76-77 smooth-stepped-noise — ease-exp shaped glide on input change"
    (check examples.gsot.43-portamento-shaped/portamento-shaped)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Easing functions (p.77)
;; ---------------------------------------------------------------------------

(deftest gsot-44-easing-multi
  (testing "pp.77-78 go.unit.ease.{pow,circle,exp,back,elastic,sine} — generalized easing structure"
    (check examples.gsot.44-easing-multi/easing-multi)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Window envelope functions (p.78)
;; ---------------------------------------------------------------------------

(deftest gsot-45-window-fixed
  (testing "pp.78-79 hann, hamming, blackman, blackman-harris, blackman-nuttall, nuttall, flat-top, welch, parzen"
    (check examples.gsot.45-window-fixed/window-fixed)))

(deftest gsot-46-window-parametric
  (testing "pp.78-79 trapezoid, tukey, plancktaper, gauss, raisedcosine — parametric window shapes"
    (check examples.gsot.46-window-parametric/window-parametric)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Waveshaping bipolar signals (p.79)
;; ---------------------------------------------------------------------------

(deftest gsot-47-bipolar-waveshaping
  (testing "pp.79-81 bipolar_waveshaping_unitshapers — symmetric (odd) and full-range mappings"
    (check examples.gsot.47-bipolar-waveshaping/bipolar-waveshaping)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Audio waveshaping / polynomial shapers (p.81)
;; ---------------------------------------------------------------------------

(deftest gsot-48-chebyshev-waveshaping
  (testing "pp.81-83 bipolar_waveshaping_chebyshev — T1..T7 via recurrence"
    (check examples.gsot.48-chebyshev-waveshaping/chebyshev-waveshaping)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Sigmoid waveshaping (p.84)
;; ---------------------------------------------------------------------------

(deftest gsot-49-sigmoid-waveshaping
  (testing "pp.84-85 sigmoid waveshaping — tanh, Padé, sqrt, hard clip with drive param"
    (check examples.gsot.49-sigmoid-waveshaping/sigmoid-waveshaping)))

(deftest gsot-50-sigmoid-gsot
  (testing "pp.84-85 go.sigmoid.{tanh,logistic,guderman,atan,softclip} + go.sigmoid2"
    (check examples.gsot.50-sigmoid-gsot/sigmoid-gsot)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Enveloped sigmoid waveshaping (p.86)
;; ---------------------------------------------------------------------------

(deftest gsot-51-sigmoid-enveloped
  (testing "p.86 bipolar_waveshaping_sigmoids_enveloped — wet/dry blend via envelope input"
    (check examples.gsot.51-sigmoid-enveloped/sigmoids-enveloped)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Normalized sigmoids as unit shapers (p.87)
;; ---------------------------------------------------------------------------

(deftest gsot-52-unit-tanh
  (testing "p.87 go.unit.tanh.gendsp — normalized tanh as unit shaper [0,1]->[0,1]"
    (check examples.gsot.52-unit-tanh/unit-tanh)))

(deftest gsot-53-unit-sigmoids
  (testing "p.88 go.unit.{logistic,sigmoid2,gundermann,ata,softclip} — normalized sigmoid unit shapers"
    (check examples.gsot.53-unit-sigmoids/unit-sigmoids)))

;; ---------------------------------------------------------------------------
;; Chapter 4 — Feel the noise (p.91)
;; ---------------------------------------------------------------------------

(deftest gsot-54-noise-basic
  (testing "p.91 noise operator — white noise source, no audio-in, amplitude param"
    (check examples.gsot.54-noise-basic/noise-basic)))

(deftest gsot-55-random-range
  (testing "p.93 random_range.maxpat — noise scaled to [lo,hi] via affine map"
    (check examples.gsot.55-random-range/random-range)))

(deftest gsot-56-random-steps
  (testing "p.93 random_steps.maxpat — track-hold of range-scaled noise on trigger"
    (check examples.gsot.56-random-steps/random-steps)))

(deftest gsot-57-random-smoothed
  (testing "p.94 random_smoothed.maxpat — linear interp between random steps via phasor phase"
    (check examples.gsot.57-random-smoothed/random-smoothed)))

(deftest gsot-58-spline-smoothed
  (testing "p.95 go.shift.spline6.gendsp — Catmull-Rom spline on 6-stage shift register"
    (check examples.gsot.58-spline-smoothed/spline-smoothed)))

(deftest gsot-59-random-chance
  (testing "p.96 go.chance.gendsp / random_chance.maxpat — probability-gated trigger"
    (check examples.gsot.59-random-chance/random-chance)))

(deftest gsot-60-bernoulli-gate
  (testing "p.97 go.bern.gendsp / random_bernoulli-gate.maxpat — two-output trigger router"
    (check examples.gsot.60-bernoulli-gate/bernoulli-gate)))

(deftest gsot-61-random-periods
  (testing "p.98 random_periods.maxpat — random-length period counter/phasor with trig-out"
    (check examples.gsot.61-random-periods/random-periods)))

(deftest gsot-62-normal-noise
  (testing "p.100 go.noise.normal.gendsp — CLT sum-of-12 approximately N(mu, sigma²)"
    (check examples.gsot.62-normal-noise/normal-noise)))

(deftest gsot-63-random-distributions
  (testing "p.101 random_distributions.maxpat — uniform vs normal side-by-side comparison"
    (check examples.gsot.63-random-distributions/random-distributions)))

(deftest gsot-64-random-walk
  (testing "p.101-102 random_walks.maxpat — trigger-gated accumulating random walk"
    (check examples.gsot.64-random-walk/random-walk)))

(deftest gsot-65-random-walk-bounded
  (testing "p.103 random_walk_bounded.maxpat — fold-reflected bounded random walk"
    (check examples.gsot.65-random-walk-bounded/random-walk-bounded)))

(deftest gsot-66-random-integer
  (testing "p.103 go.random.gendsp / random_integer.maxpat — discrete uniform integer on trigger"
    (check examples.gsot.66-random-integer/random-integer)))

(deftest gsot-67-random-urn
  (testing "pp.104-109 random_urn.maxpat — rotating-permutation urn approximation (data.deck not expressible in Faust)"
    (check examples.gsot.67-random-urn/random-urn)))

(deftest gsot-68-random-urn-flexible
  (testing "pp.110-111 random_urn.maxpat extended — manual reshuffle trigger + no-immediate-repeat"
    (check examples.gsot.68-random-urn-flexible/random-urn-flexible)))

(deftest gsot-69-chaos-lorenz
  (testing "pp.112-114 chaos_Lorenz.maxpat — Lorenz attractor Euler step (3-in / 3-out; self-oscillation via external feedback)"
    (check examples.gsot.69-chaos-lorenz/chaos-lorenz)))

(deftest gsot-70-limits
  (testing "pp.115-116 go.limits.gendsp — running min/max tracker with per-sample decay"
    (check examples.gsot.70-limits/limits)))

(deftest gsot-71-autolimit
  (testing "pp.116 go.autolimit.gendsp — auto-normalise unbounded signal to [-1,1] via tracked limits"
    (check examples.gsot.71-autolimit/autolimit)))

(deftest gsot-72-go-chaos
  (testing "pp.117 go.chaos / Liu-Chen.maxpat — abstract chaos step unifying Lorenz and Lu-Chen families"
    (check examples.gsot.72-go-chaos/go-chaos)))

(deftest gsot-73-go-chaos-liu-chen
  (testing "pp.118 go.chaos.liu_chen.gendsp — Lu-Chen attractor with hardwired coefficients (a=36, b=0, c=20, d=3)"
    (check examples.gsot.73-go-chaos-liu-chen/go-chaos-liu-chen)))

(deftest gsot-74-chaos-tempo-nonrobotic
  (testing "pp.119-120 chaos.tempo.nonrobotic.maxpat — logistic map drives chaotic phasor rate for natural tempo looseness"
    (check examples.gsot.74-chaos-tempo-nonrobotic/chaos-tempo-nonrobotic)))

(deftest gsot-75-chaos-lorenz-audioinjection
  (testing "pp.121 chaos_Lorenz_audioinjection.maxpat — audio signal injected into Lorenz x equation; chaotic nonlinear coupling"
    (check examples.gsot.75-chaos-lorenz-audioinjection/chaos-lorenz-audioinjection)))

(deftest gsot-76-mixer-sequencer
  (testing "pp.123-125 mixer-sequencer.maxpat — 8-step pitch sequencer; bitmask gate enable; binary-tree select2 pitch lookup"
    (check examples.gsot.76-mixer-sequencer/mixer-sequencer)))

(deftest gsot-77-latched-sequencer
  (testing "pp.126 latched-sequencer.maxpat — 8 per-step S&H cells capture cv-in on each step trigger; sequence fills in over time"
    (check examples.gsot.77-latched-sequencer/latched-sequencer)))

(deftest gsot-78-go-shiftregister2
  (testing "pp.127 go.shiftregister2.gendsp — 2-stage shift register; s1 lags s0 by one trigger event via @1 delay"
    (check examples.gsot.78-go-shiftregister2/go-shiftregister2)))

(deftest gsot-79-go-shiftregister8
  (testing "pp.127 go.shiftregister8.gendsp — 8-stage shift register; all stages simultaneously available for temporal canon"
    (check examples.gsot.79-go-shiftregister8/go-shiftregister8)))

(deftest gsot-80-shift-register
  (testing "pp.128 shift-register.maxpat — 2-voice canon; v1=stage 0, v2=stage :canon (1-7); selectable imitation interval"
    (check examples.gsot.80-shift-register/shift-register)))

(deftest gsot-81-shift-register-weighted
  (testing "pp.129-130 shift-register-weighted.maxpat — binary shift register; Bernoulli(weight) new-bit; 8 simultaneous gate streams"
    (check examples.gsot.81-shift-register-weighted/shift-register-weighted)))

(deftest gsot-82-shift-register-weighted-random
  (testing "pp.130-131 shift-register-weighted-random.maxpat — binary shift register; per-trigger random weight from centre±spread"
    (check examples.gsot.82-shift-register-weighted-random/shift-register-weighted-random)))

(deftest gsot-83-shift-register-weighted-xor
  (testing "pp.131-132 shift-register-weighted-xor.maxpat — LFSR XOR feedback (x^8+x^4+x^3+x^2+1, period 255) weighted vs Bernoulli random; packed integer state"
    (check examples.gsot.83-shift-register-weighted-xor/shift-register-weighted-xor)))

(deftest gsot-84-binary-decode
  (testing "pp.134-135 binary decoding — 8 shift register bits weighted-summed to integer index [0,255]; pseudo-random melodic index from LFSR stages"
    (check examples.gsot.84-binary-decode/binary-decode)))

(deftest gsot-85-shift-register-integer
  (testing "pp.136-139 shift-register-integer.maxpat — integer bitmask as step pattern; counter reads bits into shift register; deterministic canon voices"
    (check examples.gsot.85-shift-register-integer/shift-register-integer)))

(deftest gsot-86-bit-unpack8
  (testing "pp.140 go.bit.unpack8.gendsp — unpack integer to 8 binary signals; inverse of binary-decode (example 84)"
    (check examples.gsot.86-bit-unpack8/bit-unpack8)))

(deftest gsot-87-bit-shift
  (testing "pp.140 go.bit.shift.gendsp — left/right shift of 8-bit integer by signed :amount; zeros fill vacated positions"
    (check examples.gsot.87-bit-shift/bit-shift)))

(deftest gsot-88-bit-wrap
  (testing "pp.141 go.bit.wrap.gendsp — circular left rotation of 8-bit integer; bits wrap from MSB to LSB; preserves step density"
    (check examples.gsot.88-bit-wrap/bit-wrap)))

(deftest gsot-89-bit-extract
  (testing "pp.141 go.bit.extract.gendsp — extract :width-bit field from 8-bit integer at :offset; generalises single-bit extraction"
    (check examples.gsot.89-bit-extract/bit-extract)))

(deftest gsot-90-bit-rotate
  (testing "pp.141-142 go.bit.rotate.gendsp — trigger-driven accumulating circular rotation; counter 0-7 cycles through all 8 positions"
    (check examples.gsot.90-bit-rotate/bit-rotate)))

(deftest gsot-91-euclidean-rhythms
  (testing "pp.142-148 euclidean_rhythms.maxpat — digitized ratio floor(s*N/K) generates maximally-even N-beat/K-step gate; wrap detection for step 0"
    (check examples.gsot.91-euclidean-rhythms/euclidean-rhythms)))

(deftest gsot-92-euclidean-ratchets
  (testing "pp.149 euclidean_ratchets.maxpat — countdown register fires R pulses per beat; trigger-gated load-or-decrement; restarts cleanly on overlapping beats"
    (check examples.gsot.92-euclidean-ratchets/euclidean-ratchets)))

(deftest gsot-93-euclidean-lfo
  (testing "pp.149 euclidean_LFO.maxpat — fractional part of digitized ratio; sawtooth resetting at Euclidean beat positions; drop amplitude encodes gap length"
    (check examples.gsot.93-euclidean-lfo/euclidean-lfo)))

(deftest gsot-94-pitch
  (testing "pp.150-151 pitch.maxpat — chromatic pitch space: integer index → MIDI note (root + index semitones) → Hz via equal temperament"
    (check examples.gsot.94-pitch/pitch)))

(deftest gsot-95-pitch-quantized
  (testing "pp.151-152 pitch-quantized.maxpat — diatonic pitch space: float degree → rint → major scale select2 lookup → Hz; correct octave wrapping for all integer degrees"
    (check examples.gsot.95-pitch-quantized/pitch-quantized)))

(deftest gsot-96-quantizing-pitch
  (testing "pp.153-154 quantizing-pitch.maxpat — inverse digitized ratio floor(12*i/N) maps degree index to maximally-even N-note scale; N=1-8 generates octave/tritone/augmented/diminished/pentatonic/whole-tone/diatonic/octatonic"
    (check examples.gsot.96-quantizing-pitch/quantizing-pitch)))

(deftest gsot-97-quantizing-pitch-smoothed
  (testing "pp.155-157 quantizing-pitch-smoothed.maxpat — lerp(floor(ratio), ratio, smooth) blends hard-quantized Euclidean scale (smooth=0) to continuous linear interpolation (smooth=1); frac encodes position within each scale step"
    (check examples.gsot.97-quantizing-pitch-smoothed/quantizing-pitch-smoothed)))

(deftest gsot-98-quantizing-timbre
  (testing "pp.157 quantizing-timbre.maxpat — phasor [0,1) → Euclidean N-step staircase waveform; smooth=0 hard steps, smooth=1 linear sawtooth; N cancels in ratio so all N converge at smooth=1"
    (check examples.gsot.98-quantizing-timbre/quantizing-timbre)))

(deftest gsot-99-quantizing-audio-bitcrush
  (testing "pp.158 quantizing-audio-bitcrush.maxpat — audio [-1,1) → N Euclidean quantisation levels via inverse digitized ratio; smooth interpolates adjacent levels; N=12 smooth=1 is passthrough"
    (check examples.gsot.99-quantizing-audio-bitcrush/quantizing-audio-bitcrush)))

;; ---------------------------------------------------------------------------
;; Chapter 6 — Filters, Diagrams, and the Balance of Time
;; ---------------------------------------------------------------------------

(deftest gsot-100-onepole-basic
  (testing "pp.159-163 go.onepole.basic.gendsp — one-pole IIR LP: y[n]=(1-a)*x[n]+a*y[n-1]; a=balance of time; single ~_ feedback; pole at z=a"
    (check examples.gsot.100-onepole-basic/onepole-basic)))

(deftest gsot-101-vactrol
  (testing "pp.169-170 go.vactrol.gendsp — vactrol LPG subpatch: asymmetric one-pole env follower (fast attack/slow release) drives filter coeff (1-env)*0.999 and VCA (×env); 0.999 ceiling ensures drain at gate=0"
    (check examples.gsot.101-vactrol/vactrol)))

(deftest gsot-102-lowpass-gate
  (testing "pp.171 lowpass-gate.maxpat — complete LPG: gate + audio in; vactrol envelope; LP filter + VCA from same env; separate LP-only and gate-only mix controls"
    (check examples.gsot.102-lowpass-gate/lowpass-gate)))

(deftest gsot-103-onepole-basic-hz
  (testing "pp.173 go.onepole.basic_hz.gendsp — Hz-parameterized one-pole LP: a=exp(-2π*fc/ma.SR); two-node pattern (Hz→coeff, coeff→filter); same DSP as ex.100, musical interface"
    (check examples.gsot.103-onepole-basic-hz/onepole-basic-hz)))

(deftest gsot-104-allpass
  (testing "pp.175 go.allpass.gendsp — first-order allpass: y[n]=a*x[n]+x[n-1]-a*y[n-1]; x[n-1] via @1 feedforward, y[n-1] via ~_ feedback; flat amplitude, frequency-dependent phase shift"
    (check examples.gsot.104-allpass/allpass)))

(deftest gsot-105-phaser-4stage
  (testing "pp.176 phaser-4stage.maxpat — 4 allpass stages in series (8 state elements); shared coefficient; dry/wet mix; 2 notches per sweep"
    (check examples.gsot.105-phaser-4stage/phaser-4stage)))

(deftest gsot-106-phaser-8stage
  (testing "pp.177 phaser-8stage.maxpat — 8 allpass stages in series (16 state elements); shared coefficient; dry/wet mix; 4 notches per sweep; vintage thick phaser character"
    (check examples.gsot.106-phaser-8stage/phaser-8stage)))

(deftest gsot-107-allpass-hz
  (testing "pp.178 go.allpass.hz.gendsp — Hz-parameterized first-order allpass; bilinear transform a=(tan(π·fc/SR)−1)/(tan(π·fc/SR)+1); −π/2 phase shift at fc"
    (check examples.gsot.107-allpass-hz/allpass-hz)))

(deftest gsot-108-biquad
  (testing "pp.178-180 go.biquad.gendsp — second-order IIR; Direct Form II: w=(in-a1*_-a2*_@1)~_, out=b0*w+b1*w@1+b2*w@2; 5 coefficients b0/b1/b2/a1/a2"
    (check examples.gsot.108-biquad/biquad)))

(deftest gsot-109-biquad-lp
  (testing "pp.181 go.biquad.lp — RBJ LP: b0=b2=(1-cw)/(2*a0), b1=(1-cw)/a0; Butterworth at Q=0.707"
    (check examples.gsot.109-biquad-lp/biquad-lp)))

(deftest gsot-110-biquad-hp
  (testing "pp.181 go.biquad.hp — RBJ HP: b0=b2=(1+cw)/(2*a0), b1=-(1+cw)/a0; LP/HP complement"
    (check examples.gsot.110-biquad-hp/biquad-hp)))

(deftest gsot-111-biquad-bp
  (testing "pp.181 go.biquad.bp — RBJ BP constant skirt: b0=sw/(2*a0), b1=0, b2=-b0; output=b0*(w-w@2)"
    (check examples.gsot.111-biquad-bp/biquad-bp)))

(deftest gsot-112-biquad-res
  (testing "pp.181 go.biquad.res — RBJ peaking EQ: A=10^(gain/40); a0=1+alpha/A; b0=(1+alpha*A)/a0"
    (check examples.gsot.112-biquad-res/biquad-res)))

(deftest gsot-113-biquad-np
  (testing "pp.181 go.biquad.np — RBJ notch: b0=b2=1/a0, b1=a1=-2cw/a0; perfect null at fc"
    (check examples.gsot.113-biquad-np/biquad-np)))

(deftest gsot-114-biquad-ap
  (testing "pp.181 go.biquad.ap — RBJ 2nd-order allpass: b0=(1-alpha)/a0, b2=1.0; flat amplitude, phase shift"
    (check examples.gsot.114-biquad-ap/biquad-ap)))

(deftest gsot-115-biquad-ls
  (testing "pp.181 go.biquad.ls — RBJ low shelf: A=10^(gain/40), sAa=2*sqrt(A)*alpha; separate a0 formula"
    (check examples.gsot.115-biquad-ls/biquad-ls)))

(deftest gsot-116-biquad-hs
  (testing "pp.181 go.biquad.hs — RBJ high shelf: LP shelf reflected to high frequencies; LS/HS are cw-sign duals"
    (check examples.gsot.116-biquad-hs/biquad-hs)))

(deftest gsot-117-biquad-coeffs
  (testing "pp.181 go.biquad.coeffs.gendsp/biquad-coefficients.maxpat — type selector 0-7 (LP/HP/BP/RES/NP/AP/LS/HS) via select2 trees; unified DF-II biquad"
    (check examples.gsot.117-biquad-coeffs/biquad-coeffs)))
