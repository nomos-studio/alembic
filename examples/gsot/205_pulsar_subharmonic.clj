; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.205-pulsar-subharmonic
  "GSOT pp.318-320 — pulsar_subharmonic.maxpat (Chapter 10: Windows of Time).

  'Pulsar Synthesis — Subharmonic via Retrigger Suppression'
  -----------------------------------------------------------
  Extends ex.203 (pulsar.maxpat) to handle the case where the PULSARET DURATION
  exceeds one master clock period.  This introduces the retrigger problem and its
  solution: integer subharmonics.

  The retrigger problem (pp.318-319)
  ------------------------------------
  In basic pulsar synthesis the pulsaret duration is :dc × (1/:fp) seconds.  When
  :dc < 1 the Hann window decays to 0 before the next master trigger fires — no
  overlap, no problem.

  If the pulsaret is stretched to last LONGER than one master period (by reducing
  :fp while keeping :dc fixed, or by wanting a window wider than 1/:fp), the master
  clock fires a new trigger before the current pulsaret has ended.  That new trigger
  restarts the Hann window from 0 mid-decay — a phase discontinuity that creates a
  click.

  Retrigger suppression (pp.319-320)
  -------------------------------------
  The solution: SUPPRESS any master trigger that arrives while the current pulsaret
  is still active (gate = 1).  Only when the gate has returned to 0 does the next
  master trigger open a new pulsaret.

  Because the master clock runs at a fixed rate :fp (integer number of ticks per
  second), and because suppression only skips whole master cycles, the effective
  pulsaret rate is always a rational fraction of :fp:

      effective rate = :fp / N   for some integer N ≥ 1

  The integer N is determined dynamically by how many master cycles the pulsaret
  spans.  If the pulsaret lasts between (N−1)/fp and N/fp seconds, exactly N
  master triggers fire during one pulsaret, and N−1 of them are suppressed.
  The resulting SUBHARMONIC pitch is :fp/N — one, two, three, or more octaves
  (or non-octave integer ratios) below the master clock.

  Musically, this gives a second dimension of pitch control beyond the master
  rate: the subharmonic order N can be swept continuously (between integer values)
  by slowly varying :dc, with the pitch snapping to integer subharmonics each
  time N crosses a boundary.

  Implementation
  --------------
  The GSOT gen~ patch implements retrigger suppression via a latch: a one-shot
  gate that opens on each non-suppressed master trigger and closes after :dc of
  a subharmonic period.  The latch prevents subsequent triggers until it closes.

  The Alembic version takes the equivalent shortcut: drive the pulsaret phasor
  at the explicit subharmonic rate :fp / int(:ns).  Integer quantisation of :ns
  ensures the pulsaret rate is always a rational fraction of the master clock,
  identical to what retrigger suppression would produce.

      fp_eff = :fp / int(:ns)                  — effective pulsaret rate
      ph_p   = phasor(1, fp_eff)               — pulsaret phasor at fp_eff
      gate   = ph_p < :dc                       — pulsaret active fraction
      ph_f   = ph_p / :dc                       — normalised pulsaret phase [0,1)
      win    = gate × ½(1 − cos(2π·ph_f))       — Hann envelope over pulsaret
      formant = sin(2π · frac(ph_p × fc/fp_eff)) — formant at :fc
      output  = win × formant

  The difference from ex.203: ph_p runs at fp_eff = fp/:ns, not fp.  With :ns=1,
  the patch is identical to ex.203.  With :ns=2 the pulsaret rate halves (one
  octave down); with :ns=3 it thirds, etc.

  Pulsaret duration and overlap
  -------------------------------
  Effective pulsaret duration = :dc / fp_eff = :dc × :ns / :fp seconds.

  :ns=2, :dc=0.5  → pulsaret lasts 1/:fp seconds = exactly one master period.
                     No silence gap between pulsarets; continuous sound at fp/2.
  :ns=2, :dc=0.9  → pulsaret lasts 1.8/:fp seconds.  Pulsarets overlap across
                     the subharmonic silence interval — consecutive pulsarets are
                     windowed simultaneously.  Rich slow-attack envelope character.
  :ns=3, :dc=0.3  → pulsaret lasts 0.9/:fp seconds.  Short burst, then long
                     silence (2.1/:fp seconds).  Sparse, impulsive texture.
  :ns=5, :dc=0.9  → pulsaret lasts 4.5/:fp seconds.  Very long pulsaret with a
                     short silence; quasi-continuous pitched sound at :fp/5.

  Subharmonic spectrum
  ----------------------
  The output spectrum has partials at k × fp_eff = k × :fp/:ns.  The spectral
  envelope (controlled by :dc and :fc/fp_eff) shapes these partials.  At high
  :ns, more partials fall in the audible range for a given :fp, making the pitch
  texture denser even as the fundamental drops.

  Parameters
  ----------
  :fp — master pulsar rate in Hz (1–500; default 110)
  :dc — duty cycle [0,1); fraction of subharmonic period occupied by pulsaret (0.01–0.99; default 0.5)
  :fc — formant frequency in Hz (20–4000; default 440)
  :ns — subharmonic integer divisor; :ns=1 → no subharmonic (same as ex.203) (1–16; default 2)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained pulsar oscillator)
    :out — Hann-windowed pulsaret at subharmonic rate :fp/int(:ns)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! pulsar-subharmonic
  {:params {:fp {:range [1.0 500.0]   :default 110.0}
            :dc {:range [0.01 0.99]   :default 0.5}
            :fc {:range [20.0 4000.0] :default 440.0}
            :ns {:range [1.0 16.0]    :default 2.0}}}
  (let [fp  (param :fp)
        dc  (param :dc)
        fc  (param :fc)
        ns  (param :ns)
        out (faust
              "puls_sub
               with {
                 ns_f=max(1.0,float(int(%ns)));
                 fp_e=%fp/ns_f;
                 dc_f=max(0.001,%dc);
                 ph_p=os.phasor(1,fp_e);
                 gate=float(ph_p<dc_f);
                 ph_f=ph_p/dc_f;
                 win=gate*0.5*(1.0-cos(2.0*ma.PI*ph_f));
                 formant=sin(2.0*ma.PI*ma.frac(ph_p*%fc/max(fp_e,1.0)));
                 puls_sub=win*formant;
               }"
              {:fp fp :dc dc :fc fc :ns ns})]
    (output :out out)))
