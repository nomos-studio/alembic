; SPDX-License-Identifier: EPL-2.0
(ns alembic.param-type-test
  (:require [clojure.test   :refer [deftest is testing]]
            [clojure.string :as str]
            [alembic.emit   :refer [emit-faust]]
            [alembic.patch  :refer [defpatch!]]))

;; ---------------------------------------------------------------------------
;; :float (default) — hslider with auto-derived step
;; ---------------------------------------------------------------------------

(defpatch! float-param-patch
  {:params {:freq {:type :float :range [20.0 20000.0] :default 440.0}}}
  (let [osc (phasor (param :freq))]
    (output osc)))

(deftest float-param-emits-hslider
  (let [src (emit-faust float-param-patch)]
    (testing "hslider widget"
      (is (str/includes? src "hslider(")))
    (testing "param name"
      (is (str/includes? src "\"freq\"")))
    (testing "default value"
      (is (str/includes? src "440.0")))
    (testing "range bounds"
      (is (str/includes? src "20.0"))
      (is (str/includes? src "20000.0")))
    (testing "not wrapped in int cast"
      (is (not (str/includes? src "int(hslider("))))))

;; Default :type :float when :type key is absent — regression guard
(defpatch! float-param-implicit-patch
  {:params {:rate {:range [0.0 1.0] :default 0.5}}}
  (let [sig (phasor (param :rate))]
    (output sig)))

(deftest float-param-implicit-type
  (let [src (emit-faust float-param-implicit-patch)]
    (testing "no :type still emits hslider"
      (is (str/includes? src "hslider(\"rate\",")))
    (testing "default 0.5 in output"
      (is (str/includes? src "0.5")))))

;; ---------------------------------------------------------------------------
;; :int — float(int(hslider(..., 1.0)))
;; ---------------------------------------------------------------------------

(defpatch! int-param-patch
  {:params {:steps {:type :int :range [1 32] :default 8}}}
  (let [sig (phasor (param :steps))]
    (output sig)))

(deftest int-param-emits-float-int-hslider
  (let [src (emit-faust int-param-patch)]
    (testing "outer float(...) wrapper"
      (is (str/includes? src "float(")))
    (testing "int(...) wrapper around hslider"
      (is (str/includes? src "int(hslider(")))
    (testing "param name"
      (is (str/includes? src "\"steps\"")))
    (testing "step is 1.0"
      (is (str/includes? src "1.0)")))
    (testing "default 8 is present"
      (is (str/includes? src "8.0")))
    (testing "range bounds 1 and 32"
      (is (str/includes? src "1.0"))
      (is (str/includes? src "32.0")))))

;; :int with default absent — should fall back to lo
(defpatch! int-param-no-default-patch
  {:params {:voices {:type :int :range [1 8]}}}
  (let [sig (phasor (param :voices))]
    (output sig)))

(deftest int-param-default-falls-back-to-lo
  (let [src (emit-faust int-param-no-default-patch)]
    (testing "uses lo=1 as default when absent"
      (is (str/includes? src "float(int(hslider(\"voices\", 1.0,"))))  )

;; ---------------------------------------------------------------------------
;; :bool — checkbox(...)
;; ---------------------------------------------------------------------------

(defpatch! bool-param-patch
  {:params {:gate {:type :bool :default false}}}
  (let [sig (mul (phasor 440.0) (param :gate))]
    (output sig)))

(deftest bool-param-emits-checkbox
  (let [src (emit-faust bool-param-patch)]
    (testing "checkbox widget"
      (is (str/includes? src "checkbox(")))
    (testing "param name in checkbox"
      (is (str/includes? src "\"gate\"")))
    (testing "no hslider for bool"
      (is (not (str/includes? src "hslider(\"gate\","))))
    (testing "no nentry for bool"
      (is (not (str/includes? src "nentry(\"gate\","))))))

;; :bool default true — checkbox is the same widget regardless
(defpatch! bool-param-true-patch
  {:params {:enabled {:type :bool :default true}}}
  (let [sig (mul (phasor 220.0) (param :enabled))]
    (output sig)))

(deftest bool-param-true-default-still-checkbox
  (let [src (emit-faust bool-param-true-patch)]
    (is (str/includes? src "checkbox(\"enabled\")"))))

;; ---------------------------------------------------------------------------
;; :enum — float(int(nentry(..., 0.0, N-1, 1.0)))
;; ---------------------------------------------------------------------------

(defpatch! enum-param-patch
  {:params {:waveform {:type :enum
                       :values [:sine :tri :saw :square]
                       :default :tri}}}
  (let [sig (phasor (param :waveform))]
    (output sig)))

(deftest enum-param-emits-nentry
  (let [src (emit-faust enum-param-patch)]
    (testing "nentry widget"
      (is (str/includes? src "nentry(")))
    (testing "param name"
      (is (str/includes? src "\"waveform\"")))
    (testing "default is index of :tri = 1"
      (is (str/includes? src "nentry(\"waveform\", 1.0,")))
    (testing "max index is N-1 = 3"
      (is (str/includes? src "3.0")))
    (testing "step is 1.0"
      (is (str/includes? src ", 1.0)")))
    (testing "outer float(int(...)) wrapper"
      (is (str/includes? src "float(int(nentry(")))))

;; default is first value when :default omitted
(defpatch! enum-param-no-default-patch
  {:params {:mode {:type :enum :values [:a :b :c]}}}
  (let [sig (phasor (param :mode))]
    (output sig)))

(deftest enum-param-default-first-value
  (let [src (emit-faust enum-param-no-default-patch)]
    (testing "default index 0 when :default omitted"
      (is (str/includes? src "nentry(\"mode\", 0.0,")))))

;; ---------------------------------------------------------------------------
;; :enum error cases
;; ---------------------------------------------------------------------------

(deftest enum-param-no-values-throws
  (testing ":enum without :values throws at emit time"
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #":enum param.*:values"
          (emit-faust
            (let [nodes {:n0 {:id :n0 :op :param :name :bad :rate :block}}
                  edges []
                  params {:bad {:type :enum}}]
              {:nodes nodes :edges edges :params params
               :outputs [{:node :n0 :channel 0 :name "Main"}]
               :rate :block}))))))

(deftest enum-param-default-not-in-values-throws
  (testing ":enum :default not in :values throws at emit time"
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #":default.*:values|:values.*:default"
          (emit-faust
            (let [nodes {:n0 {:id :n0 :op :param :name :mode :rate :block}}
                  edges []
                  params {:mode {:type :enum :values [:a :b :c] :default :z}}]
              {:nodes nodes :edges edges :params params
               :outputs [{:node :n0 :channel 0 :name "Main"}]
               :rate :block}))))))

;; ---------------------------------------------------------------------------
;; Unknown :type — throws descriptive error
;; ---------------------------------------------------------------------------

(deftest unknown-param-type-throws
  (testing "unknown :type throws ex-info"
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"Unknown param :type"
          (emit-faust
            (let [nodes {:n0 {:id :n0 :op :param :name :x :rate :block}}
                  edges []
                  params {:x {:type :quaternion}}]
              {:nodes nodes :edges edges :params params
               :outputs [{:node :n0 :channel 0 :name "Main"}]
               :rate :block}))))))

;; ---------------------------------------------------------------------------
;; Faust compilation smoke — each type must produce syntactically valid Faust
;; (only runs when faust binary is on PATH; skipped gracefully otherwise)
;; ---------------------------------------------------------------------------

(defn- faust-available? []
  (try
    (= 0 (.exitValue (doto (.exec (Runtime/getRuntime) "faust --version")
                       .waitFor)))
    (catch Exception _ false)))

(defpatch! bool-compile-patch
  {:params {:on {:type :bool}}}
  (let [sig (mul (phasor 440.0) (param :on))]
    (output sig)))

(defpatch! int-compile-patch
  {:params {:n {:type :int :range [1 16] :default 4}}}
  (let [sig (phasor (param :n))]
    (output sig)))

(defpatch! enum-compile-patch
  {:params {:wave {:type :enum :values [:a :b] :default :a}}}
  (let [sig (phasor (param :wave))]
    (output sig)))

(deftest param-types-produce-valid-faust
  (when (faust-available?)
    (let [validate (requiring-resolve 'alembic.compile/validate)]
      (testing ":bool compiles"
        (is (nil? (validate bool-compile-patch))))
      (testing ":int compiles"
        (is (nil? (validate int-compile-patch))))
      (testing ":enum compiles"
        (is (nil? (validate enum-compile-patch))))))  )
