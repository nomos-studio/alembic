; SPDX-License-Identifier: EPL-2.0
(ns alembic.graph-test
  (:require [alembic.graph :refer [find-back-edges ensure-dag check-dag]]
            [alembic.emit  :refer [emit-faust]]
            [alembic.patch :refer [defpatch!]]
            [clojure.test  :refer [deftest is testing]]))

;; ---------------------------------------------------------------------------
;; Minimal graph helpers
;; ---------------------------------------------------------------------------

(defn- make-node [id op & [inputs]]
  [id {:id id :op op :rate :sample :inputs (or inputs {})}])

(defn- make-edge
  ([from to inlet] {:from from :to to :inlet inlet})
  ([from to inlet feedback?] {:from from :to to :inlet inlet :feedback? feedback?}))

(defn- simple-graph
  "Build the minimal graph map expected by find-back-edges / ensure-dag."
  [nodes edges]
  {:nodes (into {} nodes) :edges edges :params {} :outputs [] :rate :sample})

;; ---------------------------------------------------------------------------
;; find-back-edges — acyclic graphs
;; ---------------------------------------------------------------------------

(deftest find-back-edges-acyclic-empty
  (testing "empty graph has no back edges"
    (is (empty? (find-back-edges {} [])))))

(deftest find-back-edges-acyclic-chain
  (testing "linear chain A→B→C has no back edges"
    (let [nodes [(make-node :n0 :const) (make-node :n1 :mul) (make-node :n2 :add)]
          edges [(make-edge :n0 :n1 :a)
                 (make-edge :n0 :n1 :b)
                 (make-edge :n1 :n2 :a)
                 (make-edge :n0 :n2 :b)]]
      (is (empty? (find-back-edges (into {} nodes) edges))))))

(deftest find-back-edges-acyclic-diamond
  (testing "diamond A→B, A→C, B→D, C→D has no back edges"
    (let [nodes [(make-node :n0 :const) (make-node :n1 :mul)
                 (make-node :n2 :mul)   (make-node :n3 :add)]
          edges [(make-edge :n0 :n1 :a)
                 (make-edge :n0 :n2 :a)
                 (make-edge :n1 :n3 :a)
                 (make-edge :n2 :n3 :b)]]
      (is (empty? (find-back-edges (into {} nodes) edges))))))

;; ---------------------------------------------------------------------------
;; find-back-edges — cyclic graphs
;; ---------------------------------------------------------------------------

(deftest find-back-edges-self-loop
  (testing "self-loop A→A produces one back edge"
    (let [nodes [(make-node :n0 :add {:a :n0 :b :n0})]
          edges [(make-edge :n0 :n0 :a)]]
      (is (= 1 (count (find-back-edges (into {} nodes) edges))))
      (is (= :n0 (:from (first (find-back-edges (into {} nodes) edges)))))
      (is (= :n0 (:to   (first (find-back-edges (into {} nodes) edges))))))))

(deftest find-back-edges-simple-cycle
  (testing "triangle A→B→C→A produces one back edge"
    (let [nodes [(make-node :n0 :const) (make-node :n1 :mul) (make-node :n2 :add)]
          edges [(make-edge :n0 :n1 :a)
                 (make-edge :n1 :n2 :a)
                 (make-edge :n2 :n0 :in)]]   ; back edge: n2→n0
      (let [back (find-back-edges (into {} nodes) edges)]
        (is (= 1 (count back)))
        (is (= :n2 (:from (first back))))
        (is (= :n0 (:to   (first back))))))))

(deftest find-back-edges-skips-feedback-edges
  (testing "edge marked :feedback? true is not reported as a back edge"
    (let [nodes [(make-node :n0 :history {:input :n1})
                 (make-node :n1 :add     {:a :n0 :b :n0})]
          edges [(make-edge :n1 :n0 :input true)   ; intentional feedback — skipped
                 (make-edge :n0 :n1 :a)
                 (make-edge :n0 :n1 :b)]]
      (is (empty? (find-back-edges (into {} nodes) edges))))))

;; ---------------------------------------------------------------------------
;; ensure-dag
;; ---------------------------------------------------------------------------

(deftest ensure-dag-no-change-on-acyclic
  (testing "ensure-dag returns the graph unchanged when no cycles exist"
    (let [nodes [(make-node :n0 :const) (make-node :n1 :mul)]
          edges [(make-edge :n0 :n1 :a)]
          g     (simple-graph nodes edges)]
      (let [g' (ensure-dag g)]
        (is (= (count (:nodes g))  (count (:nodes g'))))
        (is (= (count (:edges g))  (count (:edges g'))))))))

(deftest ensure-dag-breaks-simple-cycle
  (testing "ensure-dag inserts _zd0 to break a two-node cycle"
    ;; n0 = (mul n1 k), n1 = (add n0 n0) — single back edge: n1→n0
    (let [nodes [(make-node :n0 :mul {:a :n1 :b :n1})
                 (make-node :n1 :add {:a :n0 :b :n0})]
          edges [(make-edge :n0 :n1 :a)   ; feedforward: n0 drives n1
                 (make-edge :n0 :n1 :b)
                 (make-edge :n1 :n0 :a)]  ; back edge: n1 drives n0
          g     (simple-graph nodes edges)
          g'    (ensure-dag g)]
      (testing "one _zd node was inserted"
        (is (= (inc (count (:nodes g))) (count (:nodes g')))))
      (testing "no back edges remain"
        (is (empty? (find-back-edges (:nodes g') (:edges g')))))
      (testing "_zd0 has :op :z-1"
        (let [zd (get (:nodes g') :_zd0)]
          (is (some? zd))
          (is (= :z-1 (:op zd))))))))

(deftest ensure-dag-breaks-triangle-cycle
  (testing "ensure-dag breaks a three-node cycle A→B→C→A"
    (let [nodes [(make-node :n0 :const)
                 (make-node :n1 :mul {:a :n0})
                 (make-node :n2 :add {:a :n1 :b :n0})]
          edges [(make-edge :n0 :n1 :a)
                 (make-edge :n1 :n2 :a)
                 (make-edge :n0 :n2 :b)
                 (make-edge :n2 :n0 :in)]   ; back edge closing the cycle
          g     (simple-graph nodes edges)
          g'    (ensure-dag g)]
      (is (empty? (find-back-edges (:nodes g') (:edges g'))))
      (is (= (inc (count (:nodes g))) (count (:nodes g'))))))  )

(deftest ensure-dag-idempotent-on-already-dag
  (testing "ensure-dag called twice produces the same result as once"
    (let [nodes [(make-node :n0 :const) (make-node :n1 :mul {:a :n0})]
          edges [(make-edge :n0 :n1 :a)]
          g     (simple-graph nodes edges)]
      (is (= (ensure-dag g) (ensure-dag (ensure-dag g)))))))

;; ---------------------------------------------------------------------------
;; ensure-dag — _zd node correctly wired for emit-faust
;; ---------------------------------------------------------------------------

(deftest ensure-dag-emittable-after-repair
  (testing "graph with broken cycle produces valid Faust output"
    ;; Manual graph: n0 = param, n1 = (add n0 n2), n2 = (mul n1 n0)
    ;; Cycle: n1→n2→n1
    (let [nodes {:n0 {:id :n0 :op :const  :rate :block :value 0.5 :inputs {}}
                 :n1 {:id :n1 :op :add    :rate :sample :inputs {:a :n0 :b :n2}}
                 :n2 {:id :n2 :op :mul    :rate :sample :inputs {:a :n1 :b :n0}}}
          edges [{:from :n0 :to :n1 :inlet :a}
                 {:from :n2 :to :n1 :inlet :b}   ; back edge (n2→n1)
                 {:from :n1 :to :n2 :inlet :a}
                 {:from :n0 :to :n2 :inlet :b}]
          g     {:nodes nodes :edges edges :params {}
                 :outputs [{:node :n2 :channel 0 :name "Main"}]
                 :rate :sample}
          g'    (ensure-dag g)]
      (testing "no remaining cycles"
        (is (empty? (find-back-edges (:nodes g') (:edges g')))))
      (testing "emit-faust succeeds (no exception)"
        (is (string? (emit-faust g')))))))

;; ---------------------------------------------------------------------------
;; check-dag
;; ---------------------------------------------------------------------------

(deftest check-dag-passes-on-acyclic
  (testing "check-dag returns nil for an acyclic graph"
    (let [nodes [(make-node :n0 :const) (make-node :n1 :mul)]
          edges [(make-edge :n0 :n1 :a)]
          g     (simple-graph nodes edges)]
      (is (nil? (check-dag g))))))

(deftest check-dag-throws-on-cycle
  (testing "check-dag throws ex-info when a cycle is present"
    (let [nodes [(make-node :n0 :add {:a :n1})
                 (make-node :n1 :mul {:a :n0})]
          edges [(make-edge :n0 :n1 :a)
                 (make-edge :n1 :n0 :a)]
          g     (simple-graph nodes edges)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"unintended cycle"
                            (check-dag g)))))  )

(deftest check-dag-includes-back-edges-in-exception
  (testing "check-dag exception data includes :back-edges"
    (let [nodes [(make-node :n0 :add {:a :n1})
                 (make-node :n1 :mul {:a :n0})]
          edges [(make-edge :n0 :n1 :a)
                 (make-edge :n1 :n0 :a)]
          g     (simple-graph nodes edges)]
      (try
        (check-dag g)
        (is false "should have thrown")
        (catch clojure.lang.ExceptionInfo e
          (is (seq (:back-edges (ex-data e)))))))))

;; ---------------------------------------------------------------------------
;; defpatch! integration — (z-1 signal) as user-authored unit delay
;; ---------------------------------------------------------------------------

(defpatch! z1-feedforward-patch {}
  (let [osc  (phasor 440.0)
        prev (z-1 osc)]
    (output prev)))

(deftest z1-as-feedforward-unit-delay
  (testing "(z-1 signal) produces a :z-1 node in the graph"
    (let [z1-nodes (filter #(= :z-1 (:op %))
                           (vals (:nodes z1-feedforward-patch)))]
      (is (= 1 (count z1-nodes)))))
  (testing "(z-1 signal) edge is marked :feedback?"
    (let [z1-id  (:id (first (filter #(= :z-1 (:op %))
                                     (vals (:nodes z1-feedforward-patch)))))
          fb-edges (filter #(and (:feedback? %)
                                 (= z1-id (:to %)))
                           (:edges z1-feedforward-patch))]
      (is (= 1 (count fb-edges)))))
  (testing "emit-faust succeeds on patch with (z-1 ...)"
    (is (string? (emit-faust z1-feedforward-patch))))
  (testing "emitted Faust contains a ' (one-sample delay)"
    (is (clojure.string/includes? (emit-faust z1-feedforward-patch) "'")))
  (testing "no spurious back edges in the graph"
    (is (empty? (find-back-edges (:nodes z1-feedforward-patch)
                                 (:edges z1-feedforward-patch))))))
