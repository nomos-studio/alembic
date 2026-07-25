; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.207-poly-voices
  "GSOT pp.323-324 — poly_voices.maxpat (Chapter 10: Windows of Time).

  'Four-Voice Polyphonic Synthesizer'
  -------------------------------------
  poly_voices.maxpat is the VOICE ORCHESTRATOR that uses Max's `poly~` object to
  manage multiple instances of voice1.gendsp (ex.206).

  Max poly~ framework
  --------------------
  `poly~` is Max's built-in polyphonic voice manager.  It:
    — Loads N instances of a gen~ sub-patcher (.gendsp file).
    — Receives MIDI-style note data (pitch, velocity, voice-index).
    — Routes each note to an available voice using round-robin or LRU allocation.
    — Steals the oldest/quietest voice when all N slots are occupied.
    — Sums the outputs of all voice instances (with optional gain scaling).

  poly_voices.maxpat wraps `poly~ voice1 4` (4 instances of voice1.gendsp) with:
    — MIDI input processing (note number → frequency conversion: 440×2^((n-69)/12))
    — Note-on/off → gate signal routing
    — Voice index selection when triggering a specific voice slot
    — Output gain scaling by 1/N

  The voice COUNT (N=4) and ALLOCATION STRATEGY are properties of poly_voices.maxpat;
  the per-voice DSP is entirely defined in voice1.gendsp.  This separation means the
  voice timbre (voice1.gendsp) can be swapped without changing the voice management,
  and vice versa.

  Alembic — explicit 4-voice Faust
  ---------------------------------
  Alembic has no equivalent of `poly~` voice allocation.  In CLAP, polyphony is
  managed by the host (kairos-grid handles note events and dispatches to voice
  slots via the ctrl-tree).  At the Alembic/Faust level, a polyphonic patch is
  expressed as N parallel voice instances sharing the same compiled Faust process.

  The Faust `voice(hz, gt)` local function is called 4 times with independent
  frequency and gate parameters.  Each call generates its own independent phasor
  and envelope state via Faust's compile-time unrolling.  The 4 voice outputs
  are summed and scaled by 0.25 to prevent clipping when all voices are active.

  In place of `poly~`'s dynamic note assignment, this patch uses EXPLICIT per-voice
  parameters (:f1–:f4 for frequencies, :g1–:g4 for gates).  Actual voice allocation
  (which voice slot gets the next note, voice stealing) is a layer above this DSP —
  the CLAP host layer or a nous scheduling expression.

  Default frequencies — C major chord
  --------------------------------------
  :f1 = 261.63 Hz — C4  (middle C)
  :f2 = 329.63 Hz — E4  (major third above C4)
  :f3 = 392.00 Hz — G4  (perfect fifth above C4)
  :f4 = 523.25 Hz — C5  (octave above C4)

  Setting all four gates to 1 simultaneously plays a C major chord.  Varying
  gate timing individually produces arpeggios or independent note events.

  Voice definition
  ----------------
  Each voice is: en.adsr(0.01, 0.1, 0.8, 0.3, gate) × sin(2π·phasor(hz))

  The Faust `voice(hz, gt)` named function in the `with{}` block is instantiated
  four times.  Because Faust is a functional-dataflow language, each call to
  `voice(fN, gN)` creates an INDEPENDENT signal subgraph with its own phasor and
  ADSR state — no shared mutable state between voices.

  Parameters
  ----------
  :f1–:f4 — per-voice frequency in Hz (20–4000; defaults: C4, E4, G4, C5)
  :g1–:g4 — per-voice gate; 1=note on, 0=note off (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained 4-voice polyphonic synthesizer)
    :out — sum of 4 ADSR-enveloped sine voices, scaled by 0.25"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! poly-voices
  {:params {:f1 {:range [20.0 4000.0] :default 261.63}
            :f2 {:range [20.0 4000.0] :default 329.63}
            :f3 {:range [20.0 4000.0] :default 392.0}
            :f4 {:range [20.0 4000.0] :default 523.25}
            :g1 {:range [0.0 1.0]     :default 0.0}
            :g2 {:range [0.0 1.0]     :default 0.0}
            :g3 {:range [0.0 1.0]     :default 0.0}
            :g4 {:range [0.0 1.0]     :default 0.0}}}
  (let [f1  (param :f1)
        f2  (param :f2)
        f3  (param :f3)
        f4  (param :f4)
        g1  (param :g1)
        g2  (param :g2)
        g3  (param :g3)
        g4  (param :g4)
        out (faust
              "0.25*(v1+v2+v3+v4)
               with {
                 voice(hz,gt)=en.adsr(0.01,0.1,0.8,0.3,gt)*sin(2.0*ma.PI*os.phasor(1,hz));
                 v1=voice(%f1,%g1);
                 v2=voice(%f2,%g2);
                 v3=voice(%f3,%g3);
                 v4=voice(%f4,%g4);
               }"
              {:f1 f1 :f2 f2 :f3 f3 :f4 f4 :g1 g1 :g2 g2 :g3 g3 :g4 g4})]
    (output :out out)))
