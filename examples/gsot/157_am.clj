; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.157-am
  "GSOT pp.227-228 — AM.maxpat (Chapter 8: Frequent Modulations).

  'Amplitude Modulation and Ring Modulation'
  ------------------------------------------
  Multiplication is the simplest nonlinear operation in DSP — two signals in,
  one product out.  When the multiplier is a sinusoidal oscillator, the result
  is amplitude modulation (AM) or ring modulation (RM), depending on whether
  the oscillator carries a DC offset.

  Ring modulation (RM)
  ---------------------
  y[n] = x[n] · cos(ωc·n)

  For input x = cos(ωi·n):
      y = cos(ωi·n) · cos(ωc·n)
        = ½·cos((ωi−ωc)·n) + ½·cos((ωi+ωc)·n)

  The original frequency ωi is absent.  Only the SUM (ωi+ωc) and DIFFERENCE
  (|ωi−ωc|) frequencies remain — the 'ring' of sum-and-difference sidebands.
  When fc = 0 Hz: y = x (identity).  When fc = fi: y has DC + 2fi component
  (one sideband collapses to DC, the other doubles the frequency).

  Amplitude modulation (AM)
  --------------------------
  y[n] = x[n] · (1 + m·cos(ωc·n))

  Expanding:
      y = x·cos(ωi·n) + x·m·cos(ωi·n)·cos(ωc·n)
        = cos(ωi·n) + (m/2)·cos((ωi−ωc)·n) + (m/2)·cos((ωi+ωc)·n)

  Three components: the original frequency ωi (the 'carrier'), and two
  sidebands at ωi±ωc with amplitude m/2 each.
  At m=0: y = x (dry).  At m=1: sidebands are half the amplitude of the carrier.

  Relationship between AM and RM
  --------------------------------
  AM = dry  +  m · RM
     = x  +  m · (x · cos(ωc·n))
     = x · (1 + m·cos(ωc·n))

  AM is RM plus the unmodulated dry signal.  Removing the '1' from (1+m·osc)
  converts AM to RM.  The ':mx' parameter below scales the RM component; at
  :mx=0 the output is dry, at :mx=1 it is fully AM-modulated.

  For pure RM, use :mx=1 and subtract the dry signal externally, or see the
  note below on interpreting the patch at the extremes.

  Modulation rate regimes
  ------------------------
  :fc < 20 Hz:   sub-audio LFO — amplitude tremolo, no audible sidebands
  :fc 20–200 Hz: sidebands within the audio range; timbral enrichment
  :fc 200 Hz+:   sidebands move above the original; metallic, inharmonic
  :fc = fi:      one sideband at DC (rumble), one at 2fi (octave up)
  :fc >> fi:     sidebands far from original; dissonant, bell-like

  Parameters
  ----------
  :fc — modulation (carrier) frequency in Hz (0.1–4000; default 100)
  :mx — modulation depth; 0=dry, 1=full AM (0–1; default 1.0)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: AM-modulated output
                            y = in × (1 + :mx × cos(2π·:fc·t))"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! am
  {:params {:fc {:range [0.1 4000.0] :default 100.0}
            :mx {:range [0.0 1.0]    :default 1.0}}}
  (let [in  (audio-in)
        fc  (param :fc)
        mx  (param :mx)
        osc (faust "os.osc(%{fc})" {:fc fc})
        out (faust "%{in}*(1.0+%{mx}*%{os})" {:in in :mx mx :os osc})]
    (output :out out)))
