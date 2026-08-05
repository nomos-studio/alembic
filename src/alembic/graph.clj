; SPDX-License-Identifier: EPL-2.0
(ns alembic.graph
  "Graph analysis and transformation passes for alembic patch graphs.

  The alembic graph model is a DAG.  Feedback arcs must be explicit:
  either via the (z-1 signal) authoring form (which marks its incoming edge
  :feedback? true and emits as Faust x') or via Faust ~ _ recursion inside
  a (faust \"...\") escape.

  Programmatically constructed graphs may contain unintentional cycles.
  The functions here detect and repair them.

  Primary entry points:
    (check-dag  graph)    — throw if any unintended cycles exist
    (ensure-dag graph)    — insert _zd<n> nodes to break all cycles; return graph
    (find-back-edges n e) — low-level: return edges closing cycles in DFS order")

;; ---------------------------------------------------------------------------
;; Cycle detection
;; ---------------------------------------------------------------------------

(defn find-back-edges
  "Return a vector of edges that close a cycle (back edges in DFS traversal).
  Edges marked :feedback? true are intentional feedback arcs and are excluded.

  A back edge {from A, to B} means the DFS found A→B where B is an ancestor
  of A in the current DFS stack — i.e., B→...→A→B is a cycle."
  [nodes edges]
  (let [adj   (reduce (fn [acc e]
                        (if (:feedback? e)
                          acc
                          (update acc (:from e) (fnil conj []) e)))
                      {}
                      edges)
        ;; Sort for deterministic traversal order (same as topo-sort in emit.clj)
        order (sort-by #(try (Integer/parseInt (subs (name %) 1))
                             (catch Exception _ Integer/MAX_VALUE))
                       (keys nodes))
        color (atom (into {} (map #(vector % :white) (keys nodes))))
        back  (atom [])]
    (letfn [(visit! [id]
              (swap! color assoc id :gray)
              (doseq [edge (get adj id [])]
                (let [to (:to edge)]
                  (case (get @color to :black)
                    :gray  (swap! back conj edge)
                    :white (visit! to)
                    :black nil)))
              (swap! color assoc id :black))]
      (doseq [id order]
        (when (= :white (get @color id))
          (visit! id))))
    @back))

;; ---------------------------------------------------------------------------
;; Cycle repair
;; ---------------------------------------------------------------------------

(defn ensure-dag
  "Break all unintended cycles in `graph` by inserting compiler-generated
  unit-delay nodes.

  For each back edge {from A, to B, inlet K}:
    - A _zd<n> node (:op :z-1) is inserted
    - The edge A→B is removed
    - A feedback edge A→_zd<n> is added (marked :feedback? so topo-sort skips it)
    - A normal edge _zd<n>→B is added (signal now arrives delayed by one sample)
    - Node B's :inputs map is updated: inlet K now points to _zd<n>

  Repeats until no back edges remain (multiple cycles need multiple passes).
  Returns the modified graph unchanged if no cycles are detected."
  [{:keys [nodes edges] :as graph}]
  (loop [g graph, n 0]
    (let [back (find-back-edges (:nodes g) (:edges g))]
      (if (empty? back)
        g
        (let [{:keys [from to inlet]} (first back)
              zd-id    (keyword (str "_zd" n))
              zd-node  {:id zd-id :op :z-1 :rate :sample :inputs {:in from}}
              ;; Redirect destination node's inlet from `from` to the new delay
              to-node  (get (:nodes g) to)
              to-node' (update to-node :inputs assoc inlet zd-id)
              new-nodes (-> (:nodes g)
                            (assoc zd-id zd-node)
                            (assoc to to-node'))
              ;; Remove the cycle-closing edge; add two replacement edges
              new-edges (-> (into [] (remove #(and (= (:from %) from)
                                                   (= (:to %) to)
                                                   (= (:inlet %) inlet))
                                             (:edges g)))
                            (conj {:from from :to zd-id :inlet :in :feedback? true})
                            (conj {:from zd-id :to to :inlet inlet}))]
          (recur (assoc g :nodes new-nodes :edges new-edges) (inc n)))))))

;; ---------------------------------------------------------------------------
;; Validation
;; ---------------------------------------------------------------------------

(defn check-dag
  "Verify that `graph` has no unintended cycles.
  Returns nil on success.
  Throws ex-info with :back-edges on failure, suggesting remedies."
  [{:keys [nodes edges]}]
  (let [back (find-back-edges nodes edges)]
    (when (seq back)
      (throw (ex-info
               (str "Graph contains " (count back) " unintended cycle(s). "
                    "Use (z-1 signal) in the patch to break explicit feedback arcs, "
                    "or call alembic.graph/ensure-dag to insert unit delays automatically.")
               {:back-edges back}))))
  nil)
