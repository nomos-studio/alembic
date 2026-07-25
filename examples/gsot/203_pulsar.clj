; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.203-pulsar
  "GSOT pp.315-316 — pulsar.maxpat (Chapter 10: Windows of Time).

  'Pulsar Synthesis'
  ------------------
  Pulsar synthesis (Curtis Roads, 2001) generates pitched audio from a periodic
  sequence of micro-events called PULSARS.  Each pulsar consists of two parts:

      1. PULSARET — a short burst of a formant waveform, duration D/fp seconds
      2. SILENT INTERVAL — the remaining (1−D)/fp seconds of the pulsar period

  The PULSAR RATE :fp sets the fundamental pitch.  The DUTY CYCLE :dc (= D, range
  0..1) sets the fraction of each pulsar period occupied by the pulsaret.  The
  FORMANT FREQUENCY :fc sets the pitch (and hence the number of cycles) of the
  waveform played during the pulsaret.

  Spectrum and timbre
  --------------------
  The pulsar spectrum has partials at k×:fp for integer k.  The ENVELOPE of that
  spectrum (spectral centroid, bandwidth) is controlled by :dc and :fc:

  :dc close to 1 (long pulsaret):
      The pulsaret fills most of the period; the silent interval is very short.
      Output resembles a normal sine oscillator.

  :dc = 0.5:
      Pulsaret occupies half the period; silence fills the other half.
      Equivalent to half-wave rectification of a sine — rich in even harmonics.

  :dc close to 0 (short pulsaret):
      Very short bursts of waveform with long silences between.
      Output approaches a pulse train; the spectrum becomes nearly flat, with many
      high partials of roughly equal amplitude.

  :fc / :fp ratio (formant-to-pulsar ratio):
      Sets how many formant cycles occur within each pulsaret.
      :fc = :fp    → one cycle per pulsaret (the waveform occupies the entire active part).
      :fc = 3×:fp  → three cycles per pulsaret (formant 3× above fundamental pitch).
      :fc = 0.5×:fp → half a cycle per pulsaret; low spectral centroid.

  Together, :dc and :fc/fp give independent control over PITCH (:fp) and TIMBRE
  (:dc + :fc/fp) — the defining feature of pulsar synthesis.

  Implementation
  --------------
  Master phasor ph_p sweeps 0→1 at rate :fp.  Gate is 1 when ph_p < :dc:

      ph_p  = phasor(fp)              — pulsar phasor [0, 1)
      gate  = ph_p < dc               — 1 during pulsaret, 0 during silence
      ph_f  = ph_p / dc               — normalised pulsaret phase [0,1) during active part

  Hann window applied over the pulsaret to eliminate click artefacts:

      win   = gate × ½(1 − cos(2π·ph_f))   — 0→1→0 over the pulsaret duration

  The window goes to 0 at both the start (ph_f=0) and end (ph_f=1) of the
  pulsaret.  Combined with the gate going to 0 at ph_f=1, the output has no
  discontinuity at either the pulsaret start or the pulsaret→silence transition.

  Formant waveform: analytical sine running at :fc within the pulsar period.
  The formant phase is the fractional part of ph_p × fc/fp — this advances at
  rate fc during the pulsaret and continues (inaudibly, behind the gate) through
  the silence, resetting naturally when ph_p resets.

      formant = sin(2π · frac(ph_p × fc / fp))
      puls    = win × formant

  The frac() call wraps the formant phase, giving a continuously-running sine at
  apparent frequency fc, sampled at the pulsaret phase position.

  Relation to windowed sync (ex.202)
  ------------------------------------
  Both pulsar and windowed sync apply a window to a periodic event:
    — ex.202 applies a FADE-IN window AFTER a hard-sync reset.
    — ex.203 applies a HANN window OVER the entire pulsaret.
  Pulsar synthesis is a generative technique (window IS the signal); windowed sync
  is a corrective technique (window removes an artefact from an existing signal).

  Parameters
  ----------
  :fp — pulsar rate in Hz; sets the fundamental pitch (1–500; default 110)
  :dc — duty cycle [0,1); fraction of period occupied by pulsaret (0.01–0.99; default 0.5)
  :fc — formant frequency in Hz; sets spectral centroid (20–4000; default 440)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained pulsar oscillator)
    :out — pulsaret waveform windowed by Hann envelope, silent interval zeroed"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! pulsar
  {:params {:fp {:range [1.0 500.0]  :default 110.0}
            :dc {:range [0.01 0.99]  :default 0.5}
            :fc {:range [20.0 4000.0] :default 440.0}}}
  (let [fp  (param :fp)
        dc  (param :dc)
        fc  (param :fc)
        out (faust
              "puls
               with {
                 dc_f=max(0.001,%dc);
                 ph_p=os.phasor(1,%fp);
                 gate=float(ph_p<dc_f);
                 ph_f=ph_p/dc_f;
                 win=gate*0.5*(1.0-cos(2.0*ma.PI*ph_f));
                 formant=sin(2.0*ma.PI*ma.frac(ph_p*%fc/max(%fp,1.0)));
                 puls=win*formant;
               }"
              {:fp fp :dc dc :fc fc})]
    (output :out out)))
