; SPDX-License-Identifier: EPL-2.0
(ns alembic.analysis
  "Spectral analysis for DSP output verification and as a first-class library.

  The functions here are both testing infrastructure (used by the Tier 2 DSP
  test suite) and synthesis vocabulary — the FFT and spectral analysis chapter
  of the GSOT-with-Alembic manual draws from this namespace.

  The key property-based testing principle: two different DSP implementations
  (Alembic code under test, Max/gen~ golden render, or hardware capture) can be
  compared by asserting that their spectral analysis results agree within
  tolerance — not by comparing samples directly.  This avoids platform-specific
  floating-point divergence and makes the assertions meaningful rather than brittle.

  Primary entry points:
    (fft-magnitudes samples)              → one-sided magnitude spectrum
    (peak-frequency magnitudes sr)        → Hz of dominant component
    (spectral-peak-hz mags sr f tol)      → assertion: peak near f ± tol Hz
    (level-at-hz mags sr hz db tol)       → assertion: level at hz near db ± tol dB
    (rolloff-db-per-decade mags sr f1 f2) → slope estimate between f1 and f2
    (rms samples)                         → RMS level
    (dc-offset samples)                   → mean value"
  (:import [org.apache.commons.math3.transform
            FastFourierTransformer DftNormalization TransformType]))

;; ---------------------------------------------------------------------------
;; FFT core
;; ---------------------------------------------------------------------------

(defn- next-power-of-2 [n]
  (int (Math/pow 2 (Math/ceil (/ (Math/log (double n)) (Math/log 2.0))))))

(defn fft-magnitudes
  "Compute the one-sided magnitude spectrum of `samples`.
  Zero-pads to the next power of 2 if the length is not already a power of 2.
  Returns a vector of (N/2 + 1) magnitudes for bins 0 (DC) through N/2 (Nyquist)."
  [samples]
  (let [n      (next-power-of-2 (count samples))
        padded (double-array n)
        _      (dotimes [i (min (count samples) n)]
                 (aset padded i (double (nth samples i))))
        xfm    (FastFourierTransformer. DftNormalization/STANDARD)
        result (.transform xfm padded TransformType/FORWARD)
        half   (inc (/ n 2))]
    (mapv (fn [c]
            (let [re (.getReal c) im (.getImaginary c)]
              (Math/sqrt (+ (* re re) (* im im)))))
          (take half result))))

;; ---------------------------------------------------------------------------
;; Bin / frequency conversion
;; ---------------------------------------------------------------------------

(defn bin-hz
  "Convert FFT bin index to frequency in Hz.
  `n-fft` is the full FFT size (= 2 * (count magnitudes) - 2)."
  [bin n-fft sample-rate]
  (* (double bin) (/ (double sample-rate) (double n-fft))))

(defn hz-bin
  "Convert frequency in Hz to the nearest FFT bin index."
  [hz n-fft sample-rate]
  (Math/round (* (double hz) (/ (double n-fft) (double sample-rate)))))

(defn- n-fft-from-mags [magnitudes]
  (* 2 (dec (count magnitudes))))

;; ---------------------------------------------------------------------------
;; Spectral statistics
;; ---------------------------------------------------------------------------

(defn peak-bin
  "Return the bin index with maximum magnitude, excluding DC (bin 0).
  Suitable for periodic signals where DC is not the dominant component."
  [magnitudes]
  (let [indexed (map-indexed vector (rest magnitudes))]
    (-> (apply max-key second indexed)
        first
        inc)))

(defn peak-frequency
  "Return the frequency (Hz) of the strongest non-DC spectral component."
  [magnitudes sample-rate]
  (bin-hz (peak-bin magnitudes)
          (n-fft-from-mags magnitudes)
          sample-rate))

(defn magnitude-at-hz
  "Return the magnitude at the FFT bin nearest to `hz`."
  [magnitudes hz sample-rate]
  (let [n     (n-fft-from-mags magnitudes)
        b     (-> (hz-bin hz n sample-rate)
                  (max 0)
                  (min (dec (count magnitudes))))]
    (nth magnitudes b)))

(defn magnitude-db
  "Convert linear magnitude to dBFS.  Returns -120.0 for silence (< 1e-12)."
  [m]
  (if (< (double m) 1e-12)
    -120.0
    (* 20.0 (Math/log10 (double m)))))

(defn rolloff-db-per-decade
  "Estimate the spectral rolloff slope in dB/decade between `f1-hz` and `f2-hz`.
  A first-order one-pole LP has a theoretical slope of -20 dB/decade above cutoff."
  [magnitudes sample-rate f1-hz f2-hz]
  (let [db1 (magnitude-db (magnitude-at-hz magnitudes f1-hz sample-rate))
        db2 (magnitude-db (magnitude-at-hz magnitudes f2-hz sample-rate))]
    (/ (- db2 db1)
       (Math/log10 (/ (double f2-hz) (double f1-hz))))))

(defn rms
  "Compute the RMS level of a sample vector."
  [samples]
  (if (empty? samples)
    0.0
    (Math/sqrt (/ (transduce (map #(* (double %) (double %))) + samples)
                  (count samples)))))

(defn dc-offset
  "Compute the mean (DC offset) of a sample vector."
  [samples]
  (if (empty? samples)
    0.0
    (/ (transduce (map double) + samples) (count samples))))

;; ---------------------------------------------------------------------------
;; Assertion helpers — return {:pass? bool ...} maps for use with clojure.test
;; ---------------------------------------------------------------------------

(defn spectral-peak-hz
  "Assert that the strongest spectral component is within `tolerance-hz` of `expected-hz`.
  Returns {:pass? bool :expected hz :actual hz :delta hz}."
  [magnitudes sample-rate expected-hz tolerance-hz]
  (let [actual (peak-frequency magnitudes sample-rate)
        delta  (Math/abs (- actual (double expected-hz)))]
    {:pass?    (<= delta (double tolerance-hz))
     :expected (double expected-hz)
     :actual   actual
     :delta    delta}))

(defn level-at-hz
  "Assert that the magnitude at `hz` is within `tolerance-db` dB of `expected-db`.
  Returns {:pass? bool :hz hz :expected-db db :actual-db db :delta db}."
  [magnitudes sample-rate hz expected-db tolerance-db]
  (let [actual-db (magnitude-db (magnitude-at-hz magnitudes hz sample-rate))
        delta     (Math/abs (- actual-db (double expected-db)))]
    {:pass?       (<= delta (double tolerance-db))
     :hz          (double hz)
     :expected-db (double expected-db)
     :actual-db   actual-db
     :delta       delta}))

(defn spectral-properties
  "Summarise the key spectral properties of `magnitudes` for diagnostic output.
  Useful for understanding an unknown buffer before writing assertions."
  [magnitudes sample-rate]
  (let [peak-f (peak-frequency magnitudes sample-rate)
        peak-m (apply max magnitudes)]
    {:peak-hz      peak-f
     :peak-db      (magnitude-db peak-m)
     :dc-db        (magnitude-db (first magnitudes))
     :nyquist-hz   (* 0.5 sample-rate)}))
