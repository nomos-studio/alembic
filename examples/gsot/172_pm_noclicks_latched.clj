; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.172-pm-noclicks-latched
  "GSOT p.248 — PM-noclicks-latched.maxpat (Chapter 8: Frequent Modulations).

  'Phase Modulation — Suppressing Clicks via Phasor-Synchronized Latching'
  -------------------------------------------------------------------------
  In PM synthesis, the output is:
      y[n] = sin(2π × phasor(fc) + ix × osc(fm))

  If the modulation index :ix changes abruptly at an arbitrary sample, the
  PM phase offset jumps instantaneously by:
      Δphase = Δix × osc(fm)

  The sin() of this jump may be discontinuous — an audible click.  The worst
  case is when Δix is large and osc(fm) ≈ ±1 (modulator near its peak).

  Similarly, changing :rt (the C:M frequency ratio) abruptly alters fm and
  therefore the modulator signal's instantaneous value → another click source.

  Why clicks happen in PM but not as readily in FM
  -------------------------------------------------
  In FM, the modulator perturbs the *frequency* (the rate of phase change),
  not the phase directly.  A sudden change to :ix in FM changes how fast the
  phasor is moving, but the phasor's current position is unchanged — the
  output changes gradually.

  In PM, :ix directly scales the *instantaneous phase offset* (the argument
  of sin).  Any step in :ix causes a step in the sin argument → audible
  discontinuity.  PM is inherently more click-prone under abrupt parameter
  changes.

  The latching solution
  ----------------------
  Allow :ix (and :rt) to change only at the moment the carrier phasor resets
  (wraps from ~1 to 0 — the start of a new cycle).  Between resets, hold the
  parameter at its last-committed value.

  This does not eliminate clicks entirely: if the FM modulator phase is not
  also at zero when the carrier resets, sin(ix_new × osc(fm)) ≠ sin(ix_old × osc(fm))
  at the transition boundary.  However, the jump is now periodic and predictable
  (once per carrier cycle), and for typical musical parameter rates it is far
  smaller than a mid-cycle abrupt change.

  True zero-click transitions require the modulator to also be at a zero
  crossing at the carrier reset moment — which happens only when fc/fm is a
  simple integer ratio (C:M = 1:1, 1:2, 2:3, etc.).  For irrational ratios
  (:rt irrational), the latched approach gives 'much fewer clicks' rather than
  literal silence.

  Phasor reset detection
  -----------------------
      ph[n]  = os.phasor(1, fc)          — 0-to-1 ramp at fc
      rst[n] = ph[n] < ph[n-1]           — 1 when ph just wrapped, 0 otherwise

  `ph < ph@1` fires exactly once per carrier cycle, at the moment ph wraps.

  Sample-and-hold latch
  ----------------------
      latch(rst, x) = (x*rst + prev*(1-rst)) ~ _

  When rst=1: output = x (update to new value)
  When rst=0: output = prev (hold previous value)
  The `~ _` feeds the output back as `prev` on the next sample.

  Both :ix and :rt are latched separately:
      lix — latched modulation index
      lr  — latched C:M ratio

  The carrier frequency :fc is NOT latched.  Latching :fc would create a more
  complex chicken-and-egg dependency (the phasor that detects the reset runs at
  :fc, which would become self-referential if also latched).  Changes to :fc
  cause standard FM-like transients rather than PM phase jumps, so they are
  generally less click-prone.

  Comparison: ar envelope (ex.162) vs latching (this patch)
  -----------------------------------------------------------
  ex.162 (FMPM-enveloped): the AR envelope naturally ramps :ix smoothly →
    no abrupt changes → no clicks.  Suitable for pre-programmed note shapes.

  ex.172 (PM-noclicks-latched): latching allows :ix to change on demand
    (e.g., from a MIDI controller) without artefacts.  Suitable for
    interactive/live control where changes happen at unpredictable moments.

  Parameters
  ----------
  :fc — carrier frequency in Hz (20–2000; default 220)
  :rt — C:M ratio (fm = fc × :rt; latched at phase resets; 0.1–8.0; default 1.0)
  :ix — modulation index (latched at phase resets; 0–10; default 2.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained latched PM oscillator)
    :out — click-suppressed PM synthesis output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! pm-noclicks-latched
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :rt {:range [0.1 8.0]     :default 1.0}
            :ix {:range [0.0 10.0]    :default 2.0}}}
  (let [fc  (param :fc)
        rt  (param :rt)
        ix  (param :ix)
        ph  (faust "os.phasor(1,%{fc})" {:fc fc})
        rs  (faust "%{ph}<%{ph}@1" {:ph ph})
        lix (faust "(%{ix}*%{rs}+_*(1-%{rs})) ~ _" {:ix ix :rs rs})
        lr  (faust "(%{rt}*%{rs}+_*(1-%{rs})) ~ _" {:rt rt :rs rs})
        fm  (faust "%{fc}*%{lr}" {:fc fc :lr lr})
        mo  (faust "os.osc(%{fm})" {:fm fm})
        out (faust "sin(2.0*ma.PI*%{ph}+%{li}*%{mo})" {:ph ph :li lix :mo mo})]
    (output :out out)))
