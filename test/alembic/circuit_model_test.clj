; SPDX-License-Identifier: EPL-2.0
(ns alembic.circuit-model-test
  (:require [clojure.test   :refer [deftest is testing]]
            [clojure.string :as str]
            [alembic.emit   :refer [emit-faust]]
            [alembic.patch  :refer [defpatch!]]))

;; ---------------------------------------------------------------------------
;; :triode-pre — 12AX7 tube stage via tubes.lib
;; ---------------------------------------------------------------------------

(defpatch! triode-pre-patch {}
  (let [sig (triode-pre (audio-in) 1.0)]
    (output sig)))

(deftest triode-pre-structure
  (let [src (emit-faust triode-pre-patch)]
    (testing "references tubes.lib via component()"
      (is (str/includes? src "component(\"tubes.lib\")")))
    (testing "uses T1_12AX7 stage"
      (is (str/includes? src "T1_12AX7")))
    (testing "gain multiplied before tube stage"
      (is (str/includes? src "1.0")))))

;; gain as a param-driven signal
(defpatch! triode-pre-param-patch
  {:params {:drive {:range [0.5 4.0] :default 1.5}}}
  (let [sig (triode-pre (audio-in) (param :drive))]
    (output sig)))

(deftest triode-pre-param-gain
  (let [src (emit-faust triode-pre-param-patch)]
    (testing "emits string"
      (is (string? src)))
    (testing "tube stage present"
      (is (str/includes? src "T1_12AX7")))))

;; multi-stage chain: two triode stages in series
(defpatch! two-stage-triode-patch
  {:params {:pre {:range [0.5 4.0] :default 1.5}}}
  (let [gain  (param :pre)
        s1    (triode-pre (audio-in) gain)
        s2    (triode-pre s1 gain)]
    (output s2)))

(deftest two-stage-triode-chain
  (let [src (emit-faust two-stage-triode-patch)]
    (testing "two T1_12AX7 references"
      (is (= 2 (-> src (str/split #"T1_12AX7") count dec))))
    (testing "produces valid string"
      (is (string? src)))))

;; ---------------------------------------------------------------------------
;; Faust compilation
;; ---------------------------------------------------------------------------

(defn- faust-available? []
  (try
    (let [p (.exec (Runtime/getRuntime) "faust --version")]
      (.waitFor p)
      (= 0 (.exitValue p)))
    (catch Exception _ false)))

(deftest triode-pre-compiles
  (when (faust-available?)
    (let [validate (requiring-resolve 'alembic.compile/validate)]
      (testing "single-stage triode compiles"
        (is (nil? (validate triode-pre-patch))))
      (testing "param-driven gain compiles"
        (is (nil? (validate triode-pre-param-patch))))
      (testing "two-stage chain compiles"
        (is (nil? (validate two-stage-triode-patch)))))))
