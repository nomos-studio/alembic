; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.179-pm-is-doppler-delay
  "GSOT pp.257-258 — PM-is-doppler-delay.maxpat (Chapter 8: Frequent Modulations).

  'Delay as Phase Modulation'
  ---------------------------
  This patch demonstrates that a sinusoidally-modulated delay line applied to
  a pure sine carrier is mathematically identical to phase modulation.

  The equivalence
  ---------------
  A pure sine carrier at frequency fc:

      carrier(t) = sin(2π × fc × t)

  Applying a time-varying delay τ(t):

      output(t) = carrier(t − τ(t)) = sin(2π × fc × (t − τ(t)))
                = sin(2π × fc × t  −  2π × fc × τ(t))
                = sin(2π × fc × t  +  φ(t))

  where φ(t) = −2π × fc × τ(t) is the induced phase deviation.

  Setting the delay modulation as a sine at rate fm:

      τ(t) = (ix / (2π × fc)) × sin(2π × fm × t)

  gives:

      φ(t) = −2π × fc × (ix / (2π × fc)) × sin(2π × fm × t)
           = −ix × sin(2π × fm × t)

  So:

      output(t) = sin(2π × fc × t − ix × sin(2π × fm × t))

  This is PM with modulation index β = ix and a sign-inverted modulator.
  The sign inversion is perceptually transparent (same Bessel sideband
  amplitudes; the asymmetry in ex.174 depends on a cosine AM companion,
  which is unaffected by sign inversion of the sine modulator).

  The Doppler connection
  ---------------------
  When a sound source moves relative to a listener, the propagation path
  length changes over time.  The change in path length Δd at propagation
  speed c corresponds to a time delay Δτ = Δd/c.

  The instantaneous frequency of the received signal is:

      f_inst = fc × (1 − dτ/dt)

  — the well-known Doppler formula (approaching source: dτ/dt < 0 → pitch
  rises; receding source: dτ/dt > 0 → pitch falls).

  For sinusoidal delay modulation τ(t) = A × sin(2π × fm × t):

      dτ/dt = A × 2π × fm × cos(2π × fm × t)

      f_inst = fc − fc × A × 2π × fm × cos(2π × fm × t)

  With A = ix / (2π × fc):

      f_inst = fc − ix × fm × cos(2π × fm × t)

  Peak frequency deviation = ix × fm Hz — identical to FM with index ix.

  Summary: PM is Doppler delay.
  A variable delay applied to a signal IS phase modulation of the signal.
  The delay amplitude A controls the PM index: β = 2π × fc × A.
  The Doppler effect IS variable-delay PM of a travelling wavefront.

  Delay in samples
  ----------------
  Converting τ(t) to samples:

      delay_samples(t) = τ(t) × SR
                       = (ix × SR / (2π × fc)) × sin(2π × fm × t)

  This can be positive or negative (phase advance or delay).  A physical
  delay line cannot produce negative delay.  To support both directions,
  we use a fixed base delay of 1.0 second and modulate around it:

      total_delay(t) = SR × 1.0  +  (ix × SR / (2π × fc)) × sin(2π × fm × t)

  The 1-second offset introduces a fixed latency but is otherwise inaudible
  (it is a static phase offset of 2π × fc at the carrier frequency, which
  is perceptually irrelevant since we are interested in the modulation).

  The 2-second de.fdelay buffer safely contains the full delay range for
  all parameter combinations:

      max_deviation = ix_max × SR / (2π × fc_min)
                    = 10 × 44100 / (2π × 20)  ≈ 35,115 samples  ≈ 0.79 s

  Total delay range: [1.0 − 0.79, 1.0 + 0.79] = [0.21 s, 1.79 s] — well
  within the 2-second buffer.

  Effect on broadband audio input
  --------------------------------
  For a pure sine at exactly :fc, the output is exactly PM with index :ix.

  For broadband audio input (multiple frequency components), each component
  at frequency f experiences a phase deviation of 2π × f × τ(t) radians,
  which corresponds to PM index β(f) = ix × f/fc.  Components at f > fc
  experience a larger PM index; components at f < fc experience a smaller
  one.  This frequency-dependent PM produces the characteristic
  chorus/vibrato quality of a modulated delay line.

  The deviation amplitude :ix × SR / (2π × :fc) in samples:
      :ix=2, :fc=440, SR=44100: deviation = ±31.9 samples  ≈ ±0.7 ms
      :ix=5, :fc=220, SR=44100: deviation = ±159.6 samples ≈ ±3.6 ms
      :ix=1, :fc=100, SR=44100: deviation = ±70.2 samples  ≈ ±1.6 ms

  de.fdelay in Faust
  -------------------
  `de.fdelay(maxdel, del, x)` from delays.lib (via stdfaust.lib):
  Linear interpolation between adjacent integer samples.  `maxdel` sets
  the internal ring buffer size and must be a compile-time constant.
  Since `ma.SR` is a compile-time constant in Faust (set by the host),
  `2*ma.SR` is valid as the buffer size specification.

  Parameters
  ----------
  :fc — reference carrier frequency for PM-index-to-delay conversion (20–2000; default 440)
        For exact PM equivalence, the audio input should be a sine at :fc.
        For broadband input, :fc sets the 'center' frequency for index calculation.
  :fm — modulation frequency in Hz (0.1–20; default 2.0)
  :ix — PM modulation index β; sets delay amplitude as ix×SR/(2π×fc) (0–10; default 2.0)

  Audio inputs / Outputs
  ----------------------
  in: audio signal (for exact PM equiv, feed sin(2π×:fc×t))
    :out — input delayed by SR + ix×SR/(2π×fc)×sin(2π×fm×t) samples"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! pm-is-doppler-delay
  {:params {:fc {:range [20.0 2000.0] :default 440.0}
            :fm {:range [0.1 20.0]    :default 2.0}
            :ix {:range [0.0 10.0]    :default 2.0}}}
  (let [in  (audio-in)
        fc  (param :fc)
        fm  (param :fm)
        ix  (param :ix)
        da  (faust "%{ix}*ma.SR/(2.0*ma.PI*%{fc})" {:ix ix :fc fc})
        ds  (faust "max(0.0,ma.SR+%{da}*os.osc(%{fm}))" {:da da :fm fm})
        out (faust "de.fdelay(2*ma.SR,min(2*ma.SR-1.0,%{ds}),%{in})" {:ds ds :in in})]
    (output :out out)))
