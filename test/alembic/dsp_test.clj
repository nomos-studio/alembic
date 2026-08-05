; SPDX-License-Identifier: EPL-2.0
(ns alembic.dsp-test
  "Tier 2 DSP testing — spectral and analytical property assertions.

  Unlike Tier 1 (which proves the graph compiles) and Tier 3 (which compares
  against a golden reference), Tier 2 asserts that the rendered output has the
  expected analytical properties: spectral peaks at the right frequencies,
  correct filter rolloff slope, etc.

  Design principle: the same analysis machinery applies to our code under test,
  to Max/gen~ golden renders, and to hardware captures (via Expert Sleepers
  ES-3/ES-6).  Two implementations of the same DSP behaviour are equivalent when
  their spectral properties agree within tolerance — not when their samples match."
  (:require [clojure.test :refer [deftest is testing]]
            [alembic.patch :refer [defpatch!]]
            [alembic.compile :refer [run-dsp]]
            [alembic.analysis :as a]))

;; ---------------------------------------------------------------------------
;; Test patches — minimal reference signals with known spectral properties
;; ---------------------------------------------------------------------------

(defpatch! tp-phasor-440 {}
  (let []
    (output (phasor 440.0))))

(defpatch! tp-phasor-880 {}
  (let []
    (output (phasor 880.0))))

(defpatch! tp-sine-440 {}
  (let [ph (phasor 440.0)]
    (output (sine-bi ph))))

(defpatch! tp-white-noise {}
  (let []
    (output (noise))))

;; One-pole LP fed by white noise.
;; cutoff-input=0.05 → Faust si.smooth(0.95) → pole at 0.95
;; Approx -3dB at (1-0.95)*sr/(2π) ≈ 352 Hz; stopband above that.
(defpatch! tp-one-pole-lp {}
  (let [n (noise)]
    (output (one-pole n 0.05))))

(def ^:private sr    44100)
(def ^:private n-sm  8192)
(def ^:private n-lg  32768) ; larger buffer for spectral estimates on noise-fed patches

;; ---------------------------------------------------------------------------
;; Tier 2 — phasor frequency accuracy
;; ---------------------------------------------------------------------------

(deftest phasor-440-spectral-peak-test
  (testing "440Hz phasor has spectral peak at 440Hz ± 5Hz"
    (let [buf  (run-dsp tp-phasor-440 :n-samples n-sm :sample-rate sr)
          mags (a/fft-magnitudes (:ch0 buf))
          r    (a/spectral-peak-hz mags sr 440.0 5.0)]
      (is (:pass? r)
          (format "Peak at %.1fHz, expected 440Hz ± 5Hz (delta %.2fHz)"
                  (:actual r) (:delta r))))))

(deftest phasor-880-spectral-peak-test
  (testing "880Hz phasor has spectral peak at 880Hz ± 5Hz"
    (let [buf  (run-dsp tp-phasor-880 :n-samples n-sm :sample-rate sr)
          mags (a/fft-magnitudes (:ch0 buf))
          r    (a/spectral-peak-hz mags sr 880.0 5.0)]
      (is (:pass? r)
          (format "Peak at %.1fHz, expected 880Hz ± 5Hz (delta %.2fHz)"
                  (:actual r) (:delta r))))))

;; ---------------------------------------------------------------------------
;; Tier 2 — sine spectral purity
;; ---------------------------------------------------------------------------

(deftest sine-440-spectral-peak-test
  (testing "440Hz sine has spectral peak near 440Hz"
    (let [buf  (run-dsp tp-sine-440 :n-samples n-sm :sample-rate sr)
          mags (a/fft-magnitudes (:ch0 buf))
          r    (a/spectral-peak-hz mags sr 440.0 5.0)]
      (is (:pass? r)
          (format "Sine peak at %.1fHz, expected 440Hz ± 5Hz (delta %.2fHz)"
                  (:actual r) (:delta r))))))

;; ---------------------------------------------------------------------------
;; Tier 2 — white noise statistical properties
;; ---------------------------------------------------------------------------

(deftest white-noise-rms-test
  (testing "white noise has usable RMS level and near-zero DC offset"
    (let [buf   (run-dsp tp-white-noise :n-samples n-sm :sample-rate sr)
          samps (:ch0 buf)]
      (is (> (a/rms samps) 0.1)
          (format "Noise RMS %.4f is unexpectedly low" (a/rms samps)))
      (is (< (Math/abs (a/dc-offset samps)) 0.05)
          (format "DC offset %.4f is larger than expected" (a/dc-offset samps))))))

;; ---------------------------------------------------------------------------
;; Tier 2 — one-pole LP filter rolloff direction
;;
;; With a very low cutoff (pole near 1.0), the stopband level should be
;; substantially lower than passband (at DC).  We do not assert the exact
;; slope here — noise variance over a finite buffer makes that brittle —
;; but we assert that the filter is doing something: high frequencies are
;; attenuated relative to low frequencies.
;; ---------------------------------------------------------------------------

(deftest one-pole-lp-attenuates-high-frequencies-test
  (testing "one-pole LP output has more energy at low frequencies than high"
    (let [buf    (run-dsp tp-one-pole-lp :n-samples n-lg :sample-rate sr)
          mags   (a/fft-magnitudes (:ch0 buf))
          ;; passband measurement well below cutoff (~352Hz)
          low-db  (a/magnitude-db (a/magnitude-at-hz mags 50.0 sr))
          ;; stopband measurement well above cutoff
          high-db (a/magnitude-db (a/magnitude-at-hz mags 3500.0 sr))]
      (is (> low-db high-db)
          (format "LP filter passband (%.1f dB at 50Hz) should exceed stopband (%.1f dB at 3500Hz)"
                  low-db high-db))
      (is (> (- low-db high-db) 15.0)
          (format "LP filter attenuation %.1f dB at a decade above cutoff, expected > 15 dB"
                  (- low-db high-db))))))
