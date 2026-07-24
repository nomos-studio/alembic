; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.175-amrm-bandlimited
  "GSOT p.254 — AMRM-bandlimited.maxpat (Chapter 8: Frequent Modulations).

  'Things That Can Go Wrong — Removing Inaudible Frequencies and Aliasing'
  -------------------------------------------------------------------------
  Two related problems affect AM/RM synthesis at high modulation frequencies:

  1. HEADROOM: AM output can reach twice the input amplitude.
  2. ALIASING: upper sidebands above Nyquist fold back into the audio band.

  Headroom
  ---------
  AM: y = in × (1 + osc(fc))
  The modulator (1 + osc) ranges from 0 to 2.  When the input is at full
  scale (+1), the output reaches +2 — 6 dB above the input.  In a digital
  system this clips.

  RM: y = in × osc(fc)
  The modulator osc is bounded to [−1, 1], so the output is always within
  the input amplitude — no headroom problem.

  Fix: scale the AM output by 0.5.  This patch applies a fixed 0.5 gain,
  keeping the output bounded by |in| at all times:

      y = 0.5 × in_filtered × (:bs + osc(fc))

  At :bs=0 (RM): y = 0.5 × in × osc   — RM at −6 dB (safe; could remove 0.5)
  At :bs=1 (AM): y = 0.5 × in × (1+osc) — AM at 0 dB peak headroom (correct)

  Aliasing
  ---------
  For a sinusoidal input at frequency fi, AM/RM produces sidebands at fi±fc.
  The upper sideband at fi+fc aliases if fi+fc > SR/2.

  Example: SR=44100, fc=10000.  The upper sideband of any input component
  above 12050 Hz (= SR/2 − fc) will fold back below Nyquist, polluting the
  audio spectrum with alias artefacts.

  Fix: lowpass filter the input at SR/2 − fc before modulating.  Any input
  content above that cutoff would have produced aliased upper sidebands; by
  removing it first, the upper sidebands stay within [0, SR/2].

      cutoff = max(20.0, SR/2 − fc)
      in_filtered = LP(cutoff, in)
      y = 0.5 × in_filtered × (:bs + osc(fc))

  The `max(20.0, ...)` prevents a degenerate cutoff when fc is very close to
  SR/2.  At fc > SR/2 (invalid), the cutoff would be negative — clamping to
  20 Hz effectively mutes the input, which is the correct safe behaviour.

  Oversampling (gen~ approach, noted for reference)
  --------------------------------------------------
  The gen~ `AMRM-bandlimited.maxpat` demonstrates 2× oversampling:

    1. Run the AM/RM gen~ patch at 2× the host sample rate (using gen~'s
       @gen rate or Max's oversampling facility).
    2. At 2× SR, Nyquist is now SR rather than SR/2.  The AM/RM sidebands
       land at fi±fc which are all below SR as long as fi < SR−fc.
       No aliasing.
    3. Downsample back to 1× SR: apply a brickwall LP at SR/2 (or an
       anti-aliasing LP), then decimate by 2.

  In Faust, oversampling is a host-level concern (the CLAP/VST host sets the
  sample rate).  The LP pre-filter approach used here achieves the same alias
  suppression without true oversampling — it simply removes input content
  that would alias, rather than temporarily expanding the Nyquist ceiling.

  The pre-filter IS lossier: it permanently removes high-frequency input
  content rather than preserving it at a higher sample rate.  For synthesis
  (no audio input) or when the input has limited bandwidth, the two approaches
  are equivalent.

  fi.lowpass in Faust
  --------------------
  `fi.lowpass(N, fc)` from filters.lib (via stdfaust.lib):
  N=1 is a first-order Butterworth (−6 dB/octave above cutoff).
  For steeper rolloff use N=2 or N=4, at the cost of phase distortion and
  CPU.  N=1 is adequate for alias suppression at the cost of a gentle rolloff.

  Parameters
  ----------
  :fc — modulation frequency in Hz (0.1–4000; default 100)
  :bs — AM/RM bias; 0.0=RM (no headroom issue), 1.0=AM (headroom fix applied)
        (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  in: audio signal (LP-filtered at SR/2−:fc before modulation)
    :out — 0.5 × LP(in) × (:bs + osc(:fc))"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! amrm-bandlimited
  {:params {:fc {:range [0.1 4000.0] :default 100.0}
            :bs {:range [0.0 1.0]    :default 0.0}}}
  (let [in  (audio-in)
        fc  (param :fc)
        bs  (param :bs)
        lp  (faust "%in : fi.lowpass(1,max(20.0,ma.SR/2.0-%fc))" {:in in :fc fc})
        os  (faust "os.osc(%fc)" {:fc fc})
        out (faust "0.5*%lp*(%bs+%os)" {:lp lp :bs bs :os os})]
    (output :out out)))
