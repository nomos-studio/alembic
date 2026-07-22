; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.91-euclidean-rhythms
  "GSOT pp.142-148 — euclidean_rhythms.maxpat.

  'Euclidean rhythms (digitized ratios)' / 'Euclidean ramps' (Chapter 5)
  -----------------------------------------------------------------------
  Generates Euclidean rhythms — N beats distributed as evenly as possible
  over K steps — by comparing a 'digitized ratio' against its previous value.

  The digitized ratio
  -------------------
  A step counter runs 0 → K-1, then wraps.  At each step s:

      floor(s × N / K)

  is a step function that increases exactly N times as s goes from 0 to K-1.
  The increase happens precisely at the steps that are 'maximally even' for
  N beats in K steps — the Euclidean (Bjorklund) distribution.

  Gate generation: compare the current floor value with the previous step's
  floor value.  A gate fires when the floor value increments.

  The wrap problem
  ----------------
  When the counter resets from K-1 to 0, the floor value drops back to 0
  rather than incrementing.  Step 0 always carries a beat in Euclidean
  rhythms by convention.  A separate wrap-detection term fires a gate
  whenever the counter goes backwards (current < previous):

      gate = max(floor(s×N/K) > floor(s_prev×N/K),
                 counter < counter_prev)

  Musical properties
  ------------------
  E(N,K) generates a 'maximally even' distribution that is structurally
  identical to many traditional rhythmic patterns:

    E(2,4)  — half-time feel
    E(3,8)  — tresillo; common in Afro-Cuban, reggae, Middle Eastern
    E(3,4)  — waltz / triplet feel within a 4-beat bar
    E(4,12) — habanera / clave precursor
    E(5,8)  — Yoruba bell pattern; rotation of the common 5-over-8
    E(5,16) — bossa nova-adjacent
    E(7,8)  — near-saturation; one rest in 8 steps
    E(7,12) — West African bell patterns

  The distribution produced by the digitized-ratio algorithm is a valid
  maximal-evenness rotation; the specific rotation may differ from the
  canonical Bjorklund rotation but is musically equivalent.

  Euclidean ramps
  ---------------
  GSOT builds toward this patch across pp.142-148 using 'Euclidean ramps':
  the phasor (counter) running at K steps per cycle is the 'ramp'; scaling
  it by N/K and flooring it 'digitizes' the ratio into discrete gates.
  The ramp conception allows the pattern to be generated continuously from
  a running phasor rather than a stored lookup table.

  Parameters
  ----------
  :beats — N: number of beats per pattern cycle (1–16; default 3)
  :steps — K: number of steps per pattern cycle (2–16; default 8)

  Audio inputs
  ------------
  audio-in 0: trigger — clock; each rising edge advances the step counter

  Outputs
  -------
  :gate  — Euclidean gate pulse (0.0 or 1.0); high for one sample per beat
  :phase — normalized step position [0, 1); useful for visualization and sync

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_gate, n_phase
        with {
          n_bt = hslider(\"beats\", 3.0, 1.0, 16.0, 1.0);
          n_ns = hslider(\"steps\", 8.0, 2.0, 16.0, 1.0);
          n_ct = (select2(n0>0.5,_,float(int(_+1.0)%max(1,int(n_ns))))~_);
          n_cf = floor(n_ct*n_bt/max(1.0,n_ns));
          n_gate  = max(n_cf>n_cf@1, n_ct<n_ct@1);
          n_phase = n_ct/max(1.0,n_ns);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! euclidean-rhythms
  {:params {:beats {:range [1.0 16.0] :default 3.0}
            :steps {:range [2.0 16.0] :default 8.0}}}
  (let [trig    (audio-in)
        beats   (param :beats)
        steps   (param :steps)
        ; Step counter 0 → steps-1; advances by 1 on each trigger
        counter (faust "(select2(%tr>0.5,_,float(int(_+1.0)%max(1,int(%ns))))~_)"
                       {:tr trig :ns steps})
        ; Digitized ratio: floor value increments N times per K-step cycle
        cur-f   (faust "floor(%ct*%bt/max(1.0,%ns))"
                       {:ct counter :bt beats :ns steps})
        ; Gate: floor incremented (Euclidean beat) OR counter wrapped (step 0)
        gate    (faust "max(%cf>%cf@1,%ct<%ct@1)"
                       {:cf cur-f :ct counter})
        ; Normalized phase for sync and visualization
        phase   (faust "%ct/max(1.0,%ns)"
                       {:ct counter :ns steps})]
    (output :gate gate)
    (output :phase phase)))
