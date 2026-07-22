; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.68-random-urn-flexible
  "GSOT pp.110-111 — random_urn.maxpat (extended, 'a more flexible urn').

  Extends the basic urn approximation (example 67) with two additions:

  1. Manual reshuffle trigger (audio-in 1)
     Force a new rotation key and reset the counter at any time, without
     waiting for the deck to exhaust naturally.  Useful for reshuffling on
     bar boundaries or phrase changes.

  2. No-immediate-repeat at cycle boundary
     Prevents the first value of the new cycle from equalling the last value
     of the previous cycle.  When a key collision is detected, the key is
     incremented by 1 (mod N).  Probability of collision = 1/N; the +1
     correction handles it with probability (N-2)/N; for N>2 a second
     collision is negligibly unlikely and is left uncorrected.

  Signal flow
  -----------
  audio-in 0: trig       — advance-and-latch trigger
  audio-in 1: reshuffle  — force reshuffle (resets counter, picks new key)
  params:     :size      — deck size N (output integers 0 to N-1)

      counter   resets to 0 on reshuffle; advances (mod N) on trig
      nat-start fires when counter wraps naturally (trig, not reshuffle)
      key-gate  = max(nat-start, reshuffle)          OR of both events
      key       = track-hold(rand-key, key-gate)     new key on either
      last-val  = (N-1 + prev-key) mod N             last output of old cycle
      key-adj   = key + float(key==last-val)         +1 if would repeat
      key-final = key-adj mod N
      out       = (counter + key-final) mod N

  How key collision avoidance works
  ----------------------------------
  Within a cycle, outputs are  counter + key  (mod N) for counter ∈ {0..N-1}.
  The last output of the old cycle is  (N-1 + old-key) mod N.
  The first output of the new cycle is  (0 + new-key) mod N = new-key.
  Collision iff new-key == (N-1 + old-key) mod N.
  Fix: new-key' = (new-key + 1) mod N.  Now new-key' ≠ last-val (for N > 1).

  Limitations (inherited from example 67)
  -----------------------------------------
  True urn semantics require mutable array state (gen~'s data.deck +
  codebox).  This remains a rotating-permutation approximation:
  N distinct orderings vs N! for a true Fisher-Yates shuffle.
  See threads/2026/07/20260721-alembic-codebox-boundary.org.

  Emitted Faust DSP (abbreviated):
      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1) = n_out
        with {
          n_sz  = hslider(\"size\", 8.0, 1.0, 16.0, 1.0);
          n_cnt = (select2(n1>0.5,
                           select2(n0>0.5,_,
                                   float(int(_+1.0)%max(1,int(n_sz)))),
                           0.0) ~ _);
          n_ns  = float(n_cnt<0.5)*float(n0>0.5)*(1.0-float(n1>0.5));
          n_kg  = max(n_ns, float(n1>0.5));
          n_rk  = float(int(float(int(n_sz))*0.5*(no.noise+1.0)));
          n_key = (select2(n_kg>0.5,_,n_rk)~_);           // raw key
          n_lv  = float(int(n_sz-1.0+n_key)%max(1,int(n_sz)));  // last val
          n_rk2 = float(int(n_rk+float(float(int(n_rk)%max(1,int(n_sz)))==n_lv))
                        %max(1,int(n_sz)));                // collision-adjusted
          n_kf  = (select2(n_kg>0.5,_,n_rk2)~_);          // final key
          n_out = float(int(n_cnt+n_kf)%max(1,int(n_sz)));
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! random-urn-flexible
  {:params {:size {:range [1.0 16.0] :default 8.0}}}
  (let [trig      (audio-in)
        reshuffle (audio-in)
        n         (param :size)
        ; Counter: reset on reshuffle, advance mod N on trig
        counter   (faust "(select2(%rs>0.5,select2(%tr>0.5,_,float(int(_+1.0)%max(1,int(%nn)))),0.0)~_)"
                         {:rs reshuffle :tr trig :nn n})
        ; Natural cycle-start: counter wrapped to 0 via trig (not reshuffle)
        nat-start (faust "float(%ct<0.5)*float(%tr>0.5)*(1.0-float(%rs>0.5))"
                         {:ct counter :tr trig :rs reshuffle})
        ; Key gate: fire on natural wrap OR forced reshuffle
        key-gate  (faust "max(%ns,float(%rs>0.5))" {:ns nat-start :rs reshuffle})
        ; Raw random key [0, N-1]
        rand-key  (faust "float(int(float(int(%nn))*0.5*(no.noise+1.0)))" {:nn n})
        ; Previous key (held between key-gate events)
        prev-key  (track-hold rand-key key-gate)
        ; Last value output by previous cycle: (N-1 + prev-key) mod N
        last-val  (faust "float(int(%nn-1.0+%pk)%max(1,int(%nn)))" {:nn n :pk prev-key})
        ; Collision-adjusted key: +1 mod N if new key == last-val
        key-adj   (faust "float(int(%rk+float(float(int(%rk)%max(1,int(%nn)))==%lv))%max(1,int(%nn)))"
                         {:rk rand-key :lv last-val :nn n})
        ; Final key (held between key-gate events)
        key-final (track-hold key-adj key-gate)
        ; Output: all N values per cycle, different rotation each cycle
        out       (faust "float(int(%ct+%kf)%max(1,int(%nn)))"
                         {:ct counter :kf key-final :nn n})]
    (output out)))
