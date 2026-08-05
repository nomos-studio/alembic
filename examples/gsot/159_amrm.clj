; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.159-amrm
  "GSOT p.232 — AMRM.maxpat (Chapter 8: Frequent Modulations).

  'AM and Ring Modulation — Explicit Sideband Comparison'
  -------------------------------------------------------
  The key algebraic identity unifying AM and RM:

      AM  =  RM  +  dry
      in×(1+osc)  =  in×osc  +  in

  AM contains exactly RM plus the unmodulated original.  The +1 DC offset
  inside the modulator is what restores the carrier (original frequency).
  Remove the +1 and RM emerges; add it back and AM returns.

  This patch makes that relationship explicit with two distinct outputs:

    :am  — amplitude modulation:  in × (1 + osc(fc))
             Spectrum: fi, fi+fc, fi-fc  (carrier + two sidebands)

    :rm  — ring modulation:       in × osc(fc)
             Spectrum: fi+fc, fi-fc      (two sidebands only, carrier absent)

  Route :am and :rm to separate outputs to hear them simultaneously and
  confirm what changes: the carrier component at fi, nothing else.

  Morph output
  ------------
  :out morphs continuously from :am to :rm via the :mr parameter:

    :out  =  (1 − :mr) × :am  +  :mr × :rm
           =  in × ((1 − :mr) + osc)

  At :mr=0: identical to :am — carrier present.
  At :mr=0.5: carrier at half amplitude, sidebands as in RM.
  At :mr=1: identical to :rm — carrier absent.

  Compared with ex.158 (am-depth)
  --------------------------------
  ex.158 separates DC bias (:bs) and modulation depth (:dp) independently.
  Here :mr couples them: bias = 1−:mr, depth = 1.  The result is a single
  knob that sweeps from AM to RM at full modulation, without the freedom to
  dial in sub-unity depth.  Use ex.158 when you want fine control over the
  sideband levels; use this patch when you want the cleanest AM↔RM comparison.

  Sideband amplitudes (for a sinusoidal input at fi)
  --------------------------------------------------
  AM  (:mr=0):
      carrier component:  1     (full)
      sideband amplitude: 1/2   (each sideband)

  RM  (:mr=1):
      carrier component:  0     (absent)
      sideband amplitude: 1/2   (each sideband — same level as AM!)

  Both AM and RM sidebands have identical amplitude (1/2 each).  The only
  perceptual difference is the presence or absence of the carrier at fi.

  Modulation rate regimes (same as ex.157)
  -----------------------------------------
  :fc < 20 Hz:   sub-audio LFO — tremolo, no audible sidebands
  :fc 20–200 Hz: sidebands within audio range; timbral enrichment
  :fc 200 Hz+:   sidebands above original; metallic, inharmonic quality
  :fc = fi:      RM sideband at DC + 2fi (pitch doubles; carrier removed)
  :fc >> fi:     dissonant, bell-like, unrecognisable

  Parameters
  ----------
  :fc — modulation frequency in Hz (0.1–4000; default 100)
  :mr — AM→RM morph; 0.0=AM, 1.0=RM (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  in: audio signal
    :am  — in × (1 + osc(:fc))   amplitude modulation
    :rm  — in × osc(:fc)          ring modulation
    :out — morphed blend from :am (:mr=0) to :rm (:mr=1)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! amrm
  {:params {:fc {:range [0.1 4000.0] :default 100.0}
            :mr {:range [0.0 1.0]    :default 0.0}}}
  (let [in  (audio-in)
        fc  (param :fc)
        mr  (param :mr)
        osc (faust "os.osc(%{fc})" {:fc fc})
        am  (faust "%{in}*(1.0+%{os})" {:in in :os osc})
        rm  (faust "%{in}*%{os}"       {:in in :os osc})
        out (faust "(1.0-%{mr})*%{am}+%{mr}*%{rm}" {:am am :rm rm :mr mr})]
    (output :am am)
    (output :rm rm)
    (output :out out)))
