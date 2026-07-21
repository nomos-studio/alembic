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
            [examples.gsot.37-ramp-swing]))

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
