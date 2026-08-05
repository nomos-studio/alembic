; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.93-euclidean-lfo
  "GSOT pp.149 — euclidean_LFO.maxpat.

  'Euclidean rhythms — LFO' (Chapter 5)
  ----------------------------------------
  Outputs the underlying continuous ramp that generates Euclidean beats when
  floored — a sawtooth LFO whose drops mark Euclidean beat positions.

  The Euclidean ramp
  ------------------
  The digitized-ratio gate (example 91) is derived from:

      d(s) = floor(s × N / K)

  The FRACTIONAL PART of this expression — d(s) before flooring minus the
  floor — forms a sawtooth that resets at each Euclidean beat:

      lfo(s) = (s × N / K) − floor(s × N / K)

  Properties:
  - Rises linearly between Euclidean beats (rate = N/K per step)
  - Drops back to a lower value at each beat (the floor increases by 1)
  - The drop positions are exactly the Euclidean gate positions

  For E(3, 8):
    s:   0     1      2      3      4      5      6      7
    val: 0.0   0.375  0.75   0.125  0.5    0.875  0.25   0.625
    (gates at steps where the drop occurs: 0, 3, 6)

  Musical use
  -----------
  The LFO signal encodes the Euclidean rhythm as a continuous shape rather
  than discrete events.  Connect to a VCA, filter cutoff, or wavetable index
  to modulate timbre in sync with the Euclidean beat structure.

  Since the LFO amplitude between beats equals (gap_size × N/K), closely
  spaced beats produce shallower LFO arcs and distant beats produce deeper
  ones — a direct analog of the rhythm's density.

  Relationship to euclidean-rhythms (example 91)
  -----------------------------------------------
  This patch exposes the ramp that example 91 implicitly floors.  The gate
  output here reproduces example 91's gate for reference.  The :lfo output
  is new — it is the pre-floor ramp, completing the 'Euclidean ramps' picture.

  Parameters
  ----------
  :beats — N: beats per pattern cycle (1–16; default 3)
  :steps — K: steps per pattern cycle (2–16; default 8)

  Audio inputs
  ------------
  audio-in 0: trigger — clock; each rising edge advances the step counter

  Outputs
  -------
  :lfo  — fractional part of digitized ratio; sawtooth resetting at each beat
  :gate — Euclidean gate (same as example 91; included for patching convenience)

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_lfo, n_gate
        with {
          n_bt = hslider(\"beats\", 3.0, 1.0, 16.0, 1.0);
          n_ns = hslider(\"steps\", 8.0, 2.0, 16.0, 1.0);
          n_ct = (select2(n0>0.5,_,float(int(_+1.0)%max(1,int(n_ns))))~_);
          n_cf = floor(n_ct*n_bt/max(1.0,n_ns));
          n_lfo  = n_ct*n_bt/max(1.0,n_ns) - n_cf;
          n_gate = max(n_cf>n_cf@1, n_ct<n_ct@1);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! euclidean-lfo
  {:params {:beats {:range [1.0 16.0] :default 3.0}
            :steps {:range [2.0 16.0] :default 8.0}}}
  (let [trig    (audio-in)
        beats   (param :beats)
        steps   (param :steps)
        counter (faust "(select2(%{tr}>0.5,_,float(int(_+1.0)%max(1,int(%{ns}))))~_)"
                       {:tr trig :ns steps})
        ; Digitized ratio floor value — used for both gate and LFO
        cur-f   (faust "floor(%{ct}*%{bt}/max(1.0,%{ns}))"
                       {:ct counter :bt beats :ns steps})
        ; LFO: fractional part of digitized ratio — the pre-floor ramp
        lfo     (faust "%{ct}*%{bt}/max(1.0,%{ns})-%{cf}"
                       {:ct counter :bt beats :ns steps :cf cur-f})
        ; Gate: same as euclidean-rhythms (example 91)
        gate    (faust "max(%{cf}>%{cf}@1,%{ct}<%{ct}@1)"
                       {:cf cur-f :ct counter})]
    (output :lfo lfo)
    (output :gate gate)))
