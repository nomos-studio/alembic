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
            [examples.gsot.68-random-urn-flexible]))

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
