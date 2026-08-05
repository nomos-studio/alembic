; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.204-pulsar-fm
  "GSOT pp.317 — pulsar_fm.maxpat (Chapter 10: Windows of Time).

  'Pulsar Synthesis with FM Formant'
  ------------------------------------
  Extends ex.203 (pulsar.maxpat) by applying FREQUENCY MODULATION to the
  formant oscillator.  The formant's instantaneous frequency varies each sample
  according to a sinusoidal modulator, creating sidebands within the pulsaret's
  spectral envelope.

  FM in the pulsar context
  -------------------------
  In standard FM synthesis (ex.171–178), the carrier is a free-running oscillator
  and the FM sidebands extend indefinitely in both directions.  In pulsar synthesis,
  the formant oscillator runs INSIDE the pulsaret window.  FM applied to the formant
  creates sidebands, but those sidebands are themselves shaped by the pulsaret
  window — both in amplitude (Hann envelope) and duration (:dc × 1/:fp seconds).

  The result is a spectrum where:
    — The fundamental and its harmonics are at k×:fp (from the pulsar rate)
    — FM sidebands appear at :fc ± k×(:fp×:mr), shaped by the pulsaret window
    — The sideband amplitude follows the FM C/M ratio: J_k(:mi) Bessel coefficients
    — All of this is additionally windowed by the Hann pulsaret envelope

  This gives a richer spectral structure than either standard FM or basic pulsar
  alone: the duty cycle, formant, and FM index interact to shape the spectral
  centroid, bandwidth, and sideband pattern simultaneously.

  Implementation — phase modulation
  ------------------------------------
  The formant oscillator uses PHASE MODULATION (PM) rather than frequency
  modulation.  PM and FM are mathematically equivalent at steady state but PM
  is implemented by adding the modulator's output directly to the carrier phase:

      mod_ph  = phasor(1, fp × :mr)
      mod_sig = sin(2π · mod_ph)           — modulator sine at fp×:mr Hz
      form_ph = frac(ph_p × fc/fp + :mi × mod_sig)   — PM of formant phase
      formant = sin(2π · form_ph)

  The modulator is locked to the PULSAR RATE :fp scaled by :mr:
    — :mr = 1:  modulator at :fp; one modulation cycle per pulsar period.
               PM creates sidebands at :fc ± k×:fp — same spacing as pulsar harmonics.
    — :mr = 2:  modulator at 2×:fp; two modulation cycles per period.
               Sidebands at :fc ± k×2:fp — every other harmonic slot.
    — :mr = 0.5: modulator at :fp/2; one cycle per two pulsar periods.
               Slowly sweeping formant; the pitch of the sideband pattern changes
               at half the pulsar rate.
    — :mr = 3.7: incommensurate ratio; the modulator is not harmonically locked
               to the pulsar rate → inharmonic sideband spectrum → metallic or
               bell-like character depending on :mi.

  Modulation index :mi
  ----------------------
  :mi = 0   — no FM; identical output to ex.203 (basic pulsar).
  :mi = 1   — moderate sidebands; a ring of partials around :fc.
  :mi = 3   — strong FM sidebands dominate; the spectral centroid shifts
               significantly from the nominal :fc.
  :mi > 5   — many sidebands; the formant frequency :fc becomes less useful as
               a direct spectral centroid predictor; tonal or noisy character
               depending on the ratio :fc/:fp.

  Architecture
  ------------
  All parameters from ex.203 are retained unchanged.  Two new parameters are added:
    :mi — FM/PM modulation index
    :mr — modulator frequency ratio relative to :fp

  Everything else (gate, ph_f, Hann win, pulsaret structure) is identical to ex.203.

  Parameters
  ----------
  :fp — pulsar rate in Hz; fundamental pitch (1–500; default 110)
  :dc — duty cycle [0,1); pulsaret fraction of pulsar period (0.01–0.99; default 0.5)
  :fc — formant carrier frequency in Hz; nominal spectral centroid (20–4000; default 440)
  :mi — PM modulation index; 0=no FM, 3=moderate sidebands (0–10; default 1.0)
  :mr — modulator frequency ratio relative to :fp (0.1–8.0; default 1.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained pulsar FM oscillator)
    :out — FM formant pulsaret windowed by Hann envelope, silent interval zeroed"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! pulsar-fm
  {:params {:fp {:range [1.0 500.0]   :default 110.0}
            :dc {:range [0.01 0.99]   :default 0.5}
            :fc {:range [20.0 4000.0] :default 440.0}
            :mi {:range [0.0 10.0]    :default 1.0}
            :mr {:range [0.1 8.0]     :default 1.0}}}
  (let [fp  (param :fp)
        dc  (param :dc)
        fc  (param :fc)
        mi  (param :mi)
        mr  (param :mr)
        out (faust
              "puls_fm
               with {
                 dc_f=max(0.001,%{dc});
                 ph_p=os.phasor(1,%{fp});
                 gate=float(ph_p<dc_f);
                 ph_f=ph_p/dc_f;
                 win=gate*0.5*(1.0-cos(2.0*ma.PI*ph_f));
                 mod=sin(2.0*ma.PI*os.phasor(1,%{fp}*%{mr}));
                 form_ph=ma.frac(ph_p*%{fc}/max(%{fp},1.0)+%{mi}*mod);
                 formant=sin(2.0*ma.PI*form_ph);
                 puls_fm=win*formant;
               }"
              {:fp fp :dc dc :fc fc :mi mi :mr mr})]
    (output :out out)))
