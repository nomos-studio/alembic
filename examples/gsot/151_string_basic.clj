; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.151-string-basic
  "GSOT pp.218-219 — string_basic.maxpat (Chapter 8).

  'Strings — Karplus-Strong String Synthesis'
  -------------------------------------------
  A feedback comb filter whose loop contains a two-point moving average (LP)
  causes high harmonics to decay faster than low ones — exactly the behaviour
  of a plucked string.  Feed one period of noise as excitation, then let the
  loop run: the result is a pitched, decaying tone with a natural harmonic
  envelope.

  Karplus-Strong loop
  --------------------
  Classic Karplus-Strong (1983):
      y[n] = 0.5 · (y[n−D] + y[n−D−1])      (no input after excitation)

  With continuous input:
      y[n] = x[n] + g · 0.5 · (y[n−D] + y[n−D−1])

  The two-point average is a linear-phase FIR LP with H(z) = 0.5·(1+z^{-1}):
    - Unity gain at DC
    - 3 dB point at SR/4 Hz
    - Zero at Nyquist (complete HF cancellation each cycle)

  Each traversal of the loop attenuates high frequencies relative to low,
  so the harmonic envelope decays from the top down — the timbre darkens
  over time, matching the behaviour of a plucked or struck string.

  Feedback gain :gn controls overall decay rate: at g=1.0 the loop is
  marginally stable; at g<1 the string decays faster.  The averaging filter
  itself contributes additional HF damping independently of :gn.

  Pitch and the averaging filter detuning
  ----------------------------------------
  The loop delay D sets the fundamental: D = round(SR / :hz).
  The averaging filter introduces a half-sample of additional phase delay,
  which shifts the pitch slightly flat of 1/D Hz — particularly noticeable
  for high pitches (small D).  At A3 (220 Hz, D≈200 at 44.1 kHz) the error
  is ~0.25 cent; at A5 (880 Hz, D≈50) it rises to ~1 cent.  GSOT addresses
  this with fractional-delay correction in later examples.

  ~ _ implementation
  -------------------
  In Faust's `~ _` feedback form, s = output[n-1].  To recover y[n-D]:
      de.delay(maxD, D−1, s)   →   output[n−1−(D−1)] = output[n−D]  ✓
  The averaging is applied before the extra D−1 delay:
      0.5·(s + s@1)  =  0.5·(y[n−1] + y[n−2])
      de.delay(D−1, 0.5·(s+s@1))  =  0.5·(y[n−D] + y[n−D−1])  ✓

  Relationship to ex.147 'String' preset and ex.150
  ---------------------------------------------------
  ex.147 String preset: 3 ms, dc=1000 ms, :dp=0.75 — a one-pole LP in the
  feedback of a multi-effect delay.  The one-pole is tunable but not LP-exact.

  ex.150 (dispersive): allpass instead of averaging filter — inharmonic decay.
  ex.151 (this patch): averaging filter — harmonic decay, classic Karplus-Strong.

  Parameters
  ----------
  :hz — fundamental frequency in Hz; D = round(SR/:hz) samples (20–2000; default 220)
  :gn — loop gain; 0 = single pass (click only), 0.99 = long decay (0–0.999; default 0.99)

  Audio inputs / Outputs
  ----------------------
  in: excitation signal (noise burst, impulse, or audio)
   →  :out: string resonator output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! string-basic
  {:params {:hz {:range [20.0 2000.0] :default 220.0}
            :gn {:range [0.0 0.999]   :default 0.99}}}
  (let [in  (audio-in)
        hz  (param :hz)
        gn  (param :gn)
        dl  (faust "int(max(1.0,ma.SR/%{hz}))" {:hz hz})
        dl1 (faust "max(0,%{dl}-1)" {:dl dl})
        out (faust "str_loop ~ _\n  with { str_loop(s) = %{in}+%{gn}*de.delay(int(ma.SR*5.0),%{d1},0.5*(s+s@1)); }"
                   {:in in :gn gn :d1 dl1})]
    (output :out out)))
