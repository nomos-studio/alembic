; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.206-voice1
  "GSOT p.325 — voice1.gendsp (Chapter 10: Windows of Time).

  'Single Voice DSP Unit (gen~ sub-patcher)'
  -------------------------------------------
  voice1.gendsp is a COMPILED gen~ SUB-PATCHER — a self-contained voice DSP
  definition saved as a .gendsp file and instantiated inside Max's `poly~`
  object by poly_voices.maxpat (ex.207).

  In Max's polyphonic framework:
    — `poly~` manages N instances of a gen~ DSP patcher.
    — Each instance receives per-voice signals from `poly~`: frequency, gate,
      velocity, and a voice index.
    — `poly~` handles voice allocation (round-robin or LRU), note-on/off
      assignment, and voice stealing when all slots are occupied.
    — The outputs of all instances are summed inside `poly~` before being
      returned to the calling patch.

  voice1.gendsp defines WHAT ONE VOICE SOUNDS LIKE.  poly_voices.maxpat defines
  HOW MANY VOICES PLAY and HOW THEY ARE ALLOCATED.

  This voice
  ----------
  A sinusoidal oscillator (os.phasor → sin) with an ADSR amplitude envelope.
  This is the minimal voice unit: a single frequency, a gate, and a shaped
  amplitude contour.

      env  = en.adsr(0.01, 0.1, 0.8, 0.3, gate)
           — 10ms attack, 100ms decay, 80% sustain, 300ms release
      osc  = sin(2π · phasor(hz))
      out  = env × osc

  The envelope ensures:
    — Note-on (:gt → 1): amplitude ramps from 0 to 1 over 10ms (attack),
      then decays to 80% over 100ms (decay), then holds (sustain).
    — Note-off (:gt → 0): amplitude decays from sustain to 0 over 300ms (release).
    — Clean start: no click at note onset (attack smooths the transition).
    — Clean end: no click at note offset (release fades to silence).

  Alembic vs. gen~ .gendsp
  -------------------------
  In gen~, the .gendsp is compiled to native code and loaded by `poly~` at
  patch load time.  Multiple instances share the compiled code but each maintains
  independent DSP state (phasor phase, envelope state).

  In Alembic, `defpatch!` defines a single voice instance with its own Faust DSP
  graph.  Multiple voices are created by running multiple Faust process instances
  (one per voice) rather than by `poly~` — see ex.207 (poly-voices).

  Parameters
  ----------
  :hz — voice frequency in Hz (20–4000; default 440)
  :gt — gate signal; 1=note on (attack+sustain), 0=note off (release) (0–1; default 0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained voice oscillator)
    :out — ADSR-enveloped sine tone at :hz Hz"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! voice1
  {:params {:hz {:range [20.0 4000.0] :default 440.0}
            :gt {:range [0.0 1.0]     :default 0.0}}}
  (let [hz  (param :hz)
        gt  (param :gt)
        out (faust
              "env*osc
               with {
                 env=en.adsr(0.01,0.1,0.8,0.3,%{gt});
                 osc=sin(2.0*ma.PI*os.phasor(1,%{hz}));
               }"
              {:hz hz :gt gt})]
    (output :out out)))
