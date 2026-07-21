; SPDX-License-Identifier: EPL-2.0
(ns alembic.gsot-test
  "Validation suite for Alembic GSOT examples — Chapter 2, pp.22-41.

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
            [examples.gsot.19-ramp-to-trig-gendsp]))

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
