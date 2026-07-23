; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.146-go-background-change
  "GSOT p.211 — go.background.change.gendsp (Chapter 7).

  'Background Change — Automatic Pitch-Shift-Free Delay Time Update'
  -------------------------------------------------------------------
  Reusable gen~ abstraction that automates the two-tap crossfade from ex.145.
  Takes a single delay target (:tg); detects when it changes; crossfades from
  the old position to the new position via gain (not delay time movement) so
  no Doppler pitch shift occurs.

  ex.145 requires the user to manage two explicit delay times (:ta, :tb) and
  a crossfade parameter (:mo).  This abstraction collapses those into a single
  delay target and handles the bookkeeping internally.

  Mechanism: the three-phase update cycle
  -----------------------------------------
  Phase 1 — stable (xfg = 1, output reads at committed delay time):
      The delay output plays from the 'committed' delay time.
      The second ('background') tap is also reading at the committed time.

  Phase 2 — change detected (xfg resets to 0):
      :tg changes.  abs(:tg − :tg@1) > 0.001 fires for one sample.
      The 'held' delay time latches :tg@1 (the old value, one sample ago).
      The crossfade gain xfg resets to 0.
      The background tap now reads at the NEW :tg — but at zero volume,
      so the discontinuous jump in read position is inaudible.

  Phase 3 — crossfade (xfg ramps 0 → 1 over :cf ms):
      xfg ramps smoothly from 0 to 1 at rate 1000/(:cf×SR) per sample.
      During ramp: old tap (at 'held' time) fades out; new tap (at :tg) fades in.
      Neither tap's read pointer moves — no Doppler.
      When xfg reaches 1: new tap is fully active; cycle completes.

  If :tg changes again before xfg = 1: xfg resets to 0 again and the process
  restarts from whatever position the crossfade was at.  'held' captures the
  current interpolated position.

  Change detection via _@1
  -------------------------
  Faust's `@N` operator delays a signal by N samples.  `%tg@1` is the value of
  the delay target one sample ago.

      changed = abs(:tg[n] − :tg[n−1]) > 0.001

  This fires for exactly one sample per step change (longer for a ramp or sweep
  of :tg — in that case the mechanism re-arms each sample, which is intentional:
  xfg stays near 0 while the target is moving, preserving the old read position
  until the target settles).

  Sample-and-hold for the committed delay time
  ----------------------------------------------
  Faust's `~` feedback plus `select2` implements a sample-and-hold:

      bgch_hld(p) = select2(changed, p, :tg@1)
      held = bgch_hld ~ _

  select2(changed, hold, update):
      changed = 0 → p (hold previous committed value)
      changed = 1 → :tg@1 (latch old :tg value on change)

  Feedforward-only delay reads
  -----------------------------
  Both delay taps are feedforward (no feedback loop) to keep the mechanism
  clean.  Feedback can be layered on top but complicates the two-tap structure
  (each tap would develop an independent echo trail, as in ex.145).

  Parameters
  ----------
  :tg — delay target in milliseconds; changes trigger automatic crossfade (1–5000; default 250)
  :cf — crossfade duration in milliseconds (10–2000; default 100)
  :mx — wet/dry mix; 0.0 = dry only, 1.0 = wet only (default 0.5)

  Audio inputs / Outputs
  ----------------------
  in: audio signal  →  :out: pitch-shift-free auto-crossfaded delay output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! go-background-change
  {:params {:tg {:range [1.0 5000.0]  :default 250.0}
            :cf {:range [10.0 2000.0] :default 100.0}
            :mx {:range [0.0 1.0]     :default 0.5}}}
  (let [in  (audio-in)
        tg  (param :tg)
        cf  (param :cf)
        mx  (param :mx)
        ; one sample when :tg steps; sustained when :tg ramps
        chg (faust "abs(%tg-(%tg@1))>0.001" {:tg tg})
        ; crossfade gain: reset to 0 on change, ramp to 1 over :cf ms
        xfg (faust "bgch_xfg ~ _\n  with { bgch_xfg(p) = select2(%ch,min(1.0,p+1000.0/(%cf*ma.SR)),0.0); }"
                   {:ch chg :cf cf})
        ; held committed delay time: latches old :tg when change fires
        hld (faust "bgch_hld ~ _\n  with { bgch_hld(p) = select2(%ch,p,%tg@1); }"
                   {:ch chg :tg tg})
        ; old tap at committed (held) delay — read pointer never moves during crossfade
        dlo (faust "de.delay(int(ma.SR*5.0),int(max(0.0,%hl*ma.SR/1000.0)),%in)"
                   {:hl hld :in in})
        ; new tap at current target — jumped to new position while at zero volume
        dln (faust "de.delay(int(ma.SR*5.0),int(max(0.0,%tg*ma.SR/1000.0)),%in)"
                   {:tg tg :in in})
        ; crossfade: xfg=0 → old only (stable), xfg=1 → new only (stable)
        wet (faust "(1.0-%xf)*%lo+%xf*%ln" {:xf xfg :lo dlo :ln dln})
        out (faust "(1.0-%mx)*%in+%mx*%wt" {:mx mx :in in :wt wet})]
    (output :out out)))
