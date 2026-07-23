; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.158-am-depth
  "GSOT pp.229-230 — AM-depth.maxpat (Chapter 8: Frequent Modulations).

  'Sidebands and Ring Modulation — AM Depth and Bias'
  ----------------------------------------------------
  The general modulation formula is:

      y = in × (:bs + :dp × osc(:fc))

  Two parameters independently control the character of the output:

    :bs — DC bias of the modulator  (1.0 = AM, 0.0 = RM, in between = mixed)
    :dp — modulation depth / index  (0.0 = dry, 1.0 = fully modulated)

  Amplitude Modulation (:bs = 1)
  --------------------------------
  y = in × (1 + :dp × cos(ωc·t))

  The modulator swings between (1+:dp) and (1−:dp).  It never crosses zero
  for :dp ≤ 1.  The output contains three components:
      carrier frequency ωi  (amplitude 1)
      upper sideband ωi+ωc  (amplitude :dp/2)
      lower sideband ωi−ωc  (amplitude :dp/2)

  The original signal is preserved alongside the sidebands — the 'carrier'
  component (ωi) is always present.

  Ring Modulation (:bs = 0)
  ---------------------------
  y = in × :dp × cos(ωc·t)
    = (:dp/2) × [cos((ωi+ωc)·t) + cos((ωi−ωc)·t)]

  No DC bias: the modulator swings symmetrically through zero.  The original
  frequency ωi vanishes entirely — only the SUM and DIFFERENCE sidebands
  remain.  This is the 'ring' of sidebands with no carrier, characteristic
  of ring modulators in analogue synthesis.

  The carrier disappearance
  --------------------------
  The carrier component ωi disappears in RM because the positive and negative
  half-cycles of the modulator cancel it in the long-term average.  Formally:
      E[cos(ωc·t)] = 0  (zero mean)
  With bias=1: the mean is 1, which multiplied by in gives the carrier back.
  With bias=0: mean is 0, no carrier.

  Bias as a continuous control
  -----------------------------
  :bs between 0 and 1 gives a smooth transition from RM to AM:
    :bs=0.0: pure RM — sidebands only, carrier absent
    :bs=0.5: equal parts — carrier at half amplitude, sidebands as in RM
    :bs=1.0: full AM — carrier present, sidebands at :dp/2

  Over-modulation (:dp > 1, :bs = 1)
  -------------------------------------
  When :dp > 1, the term (1 + :dp·cos) dips below zero on every negative
  half-cycle.  The signal's polarity inverts at those moments, introducing
  additional harmonic distortion beyond the two AM sidebands.  At :dp >> 1
  the spectrum becomes very rich.  Keep :dp ≤ 1 for 'clean' AM; push higher
  for distortion.

  At :bs=0 (:dp > 1 is just gain on the RM output — no sign inversion
  since the modulator always crosses zero anyway.

  Relationship to ex.157
  -----------------------
  ex.157 (am): hardcoded bias=1; :mx sweeps 0→1 AM depth.
  ex.158 (am-depth): explicit :bs exposes the AM↔RM axis; :dp is depth.

  Parameters
  ----------
  :fc — modulation frequency in Hz (0.1–4000; default 100)
  :dp — modulation depth; 0=dry, 1=fully modulated (0–2; default 1.0)
  :bs — modulator DC bias; 1.0=AM, 0.0=RM (0–1; default 1.0)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: y = in × (:bs + :dp × cos(2π·:fc·t))"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! am-depth
  {:params {:fc {:range [0.1 4000.0] :default 100.0}
            :dp {:range [0.0 2.0]    :default 1.0}
            :bs {:range [0.0 1.0]    :default 1.0}}}
  (let [in  (audio-in)
        fc  (param :fc)
        dp  (param :dp)
        bs  (param :bs)
        osc (faust "os.osc(%fc)" {:fc fc})
        out (faust "%in*(%bs+%dp*%os)" {:in in :bs bs :dp dp :os osc})]
    (output :out out)))
