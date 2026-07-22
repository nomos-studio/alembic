; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.67-random-urn
  "GSOT pp.104-109 — random_urn.maxpat / go.data.deck.

  'The urn model: pick a card, any card' (Chapter 4)
  ---------------------------------------------------
  The urn model samples WITHOUT replacement: each of N values is drawn
  exactly once per cycle, in a random order, before reshuffling.  In gen~
  this is implemented with the `data` and `data.deck` operators (mutable
  arrays) and the `codebox` operator (arbitrary C code for complex logic).

  Why this cannot be implemented faithfully in Faust
  ---------------------------------------------------
  Faust's only state mechanism is the ~ (1-sample delay feedback) operator.
  True without-replacement sampling requires:
    1. A mutable array of N elements (the deck)        — no Faust primitive
    2. Fisher-Yates shuffle (random swaps in the array) — requires array writes
    3. Exhaustion tracking (a used-set or index)        — requires array reads

  None of these are expressible in Faust's signal-flow model.  In gen~,
  `codebox` is the escape hatch that lets the author drop into C to implement
  the shuffle and index logic.  Alembic has no equivalent.

  Approximation: rotating permutation
  ------------------------------------
  This implementation captures the key guarantee — all N values appear in
  each window of N triggers — by applying a random rotation to a sequential
  counter.  A new random rotation key is sampled at each cycle boundary.

  Signal flow:
      counter     = trigger-gated counter cycling 0,1,2,...,N-1,0,1,...
      cycle-start = fires when counter wraps back to 0
      key         = track-hold(random-int(0..N-1), cycle-start)
      out         = (counter + key) mod N

  Each cycle the same N values {0,...,N-1} appear, in a rotation determined
  by `key`.  The ordering within a cycle is always 0+k, 1+k, ..., N-1+k
  (mod N) — a circular shift, not a full random permutation.  Only N distinct
  orderings are possible (one per key value), versus N! for a true shuffle.

  Musical behaviour vs true urn:
    True urn:  any of N! orders per cycle → highly varied sequences
    This impl: one of N circular shifts per cycle → repeated melodic shapes
               but different starting points each cycle

  For many musical purposes (guaranteeing all scale degrees appear before
  repeating, preventing long runs of the same value) the approximation is
  sufficient.  For true card-deck semantics, the Alembic DSL would need a
  `codebox` or `data.deck` extension backed by non-Faust state.

  Parameters
  ----------
  :size  — deck size N, integer 1-16 (output values are 0 to N-1)

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n4
        with {
          n1 = hslider(\"size\", 8.0, 1.0, 16.0, 1.0);
          n2 = (select2(n0>0.5,_,float(int(_+1.0)%max(1,int(n1))))~_);
          n3 = (select2(float(n2<0.5)*n0>0.5,_,
                        float(int(float(int(n1))*0.5*(no.noise+1.0))))~_);
          n4 = float(int(n2+n3)%max(1,int(n1)));
        };
      process = alembic_dsp;

  n0 = trig (audio-in)
  n1 = size param
  n2 = counter (0..N-1, increments on trigger)
  n3 = key (random rotation, updated each cycle-start)
  n4 = (counter + key) mod N"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! random-urn
  {:params {:size {:range [1.0 16.0] :default 8.0}}}
  (let [trig        (audio-in)
        n           (param :size)
        ; Counter: advances on each trigger, wraps at N
        counter     (faust "(select2(%tr>0.5,_,float(int(_+1.0)%max(1,int(%nn))))~_)"
                           {:tr trig :nn n})
        ; Cycle-start: fires when counter just wrapped back to 0
        cycle-start (faust "float(%ct<0.5)*%tr"
                           {:ct counter :tr trig})
        ; Random rotation key, resampled at each cycle boundary
        rand-key    (faust "float(int(float(int(%nn))*0.5*(no.noise+1.0)))" {:nn n})
        key         (track-hold rand-key cycle-start)
        ; Output: all N values per cycle in a randomly rotated order
        out         (faust "float(int(%ct+%ky)%max(1,int(%nn)))"
                           {:ct counter :ky key :nn n})]
    (output out)))
