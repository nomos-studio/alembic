; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.180-kp-fm
  "GSOT p.259 — KP-FM.maxpat (Chapter 8: Frequent Modulations).

  'Karplus-Strong with FM-Modulated Delay'
  -----------------------------------------
  The previous section (ex.179 PM-is-doppler-delay) established that a
  time-varying delay applied to a signal IS phase modulation.  This patch
  applies that principle inside a Karplus-Strong feedback loop: the delay
  time is FM-modulated each sample, creating pitch modulation (vibrato) on
  the plucked string resonance.

  Karplus-Strong review
  ---------------------
  The Karplus-Strong (KS) algorithm produces a decaying pitched tone by
  circulating energy through a short delay line with a low-pass filter in
  the feedback path:

      y(n) = excite(n)  +  LP(y(n − N)) × fb

  where N = SR/fc is the delay length in samples (sets the pitch) and fb < 1
  is the feedback coefficient controlling the decay rate.

  The LP filter is in the feedback path (not on the excitation).  Its cutoff
  controls the spectral decay: higher frequencies decay faster than lower
  ones, producing the characteristic brightening-then-darkening timbre of a
  plucked string.

  Excitation: a brief burst of noise when the gate signal goes high:

      excite(n) = noise × gate(n)

  With a gate signal (0 or 1), noise is injected into the loop while the
  gate is high.  The KS loop then resonates and decays after the gate drops.

  FM modulation of delay time
  ---------------------------
  From ex.179: a variable delay τ(t) on a pure tone at fc induces phase
  deviation φ(t) = −2π × fc × τ(t).  Inside a KS loop, modulating the
  delay time N produces pitch modulation of the resonant frequency:

      N(t) = SR / (fc + ix × fm × sin(2π × fm × t))  − 1

  This gives the resonant pitch of the KS loop as:

      pitch(t) = SR / (N(t) + 1)
               = fc + ix × fm × sin(2π × fm × t)   [FM of the KS pitch]

  Peak pitch deviation: ±ix × fm Hz around the fundamental fc.

  At small :ix (< 0.1): subtle vibrato — natural, warm, string-like
  At :ix = 1.0:          moderate FM vibrato; pitch variation audible
  At :ix = 2.0–4.0:      wide vibrato; metallic, bell-like character
  At :ix > 5.0:          extreme FM; inharmonic, chaotic territory

  The FM modulation interacts with the KS decay: as N changes each sample,
  the feedback path sees a slightly different delay, which shapes both the
  pitch and the spectral content of the resonance.  This creates more complex
  timbral evolution than a static KS string.

  Connection to PM-as-delay (ex.179)
  -----------------------------------
  In ex.179, we applied variable delay to an external audio input.  Here the
  delay IS the resonating structure (not just a transformation applied to an
  existing signal).  The feedback loop amplifies the PM effect: each cycle
  through the loop re-modulates the phase, accumulating PM effects over the
  decay time.

  This is the KS equivalent of FM feedback (ex.168): the delay-PM interaction
  is self-reinforcing through the feedback path, generating richer spectral
  evolution than a one-shot variable delay.

  KS parameters and their perceptual effect
  ------------------------------------------
  :fc — fundamental pitch (string length): SR/fc samples
        Range 20–2000 Hz; default 110 Hz (low A) for a clear bass-string sound.

  :fb — feedback coefficient (decay rate)
        0.9 = fast decay (~20 ms at 440 Hz)
        0.98 = moderate sustain (~200 ms)
        0.999 = very long sustain (seconds)
        The LP filter further attenuates high frequencies, so the actual
        high-frequency decay is faster than the low-frequency decay.

  :fm — FM modulation rate (vibrato speed)
        0.01–1 Hz: very slow drift
        2–8 Hz: typical string vibrato range
        10–20 Hz: rapid tremolo / timbral smearing
        > 50 Hz: audio-rate FM; KS sidebands appear

  :ix — FM modulation index (vibrato depth)
        0: no modulation, static KS string
        0.5–2: musical vibrato
        2–5: wide pitch FM, metallic-electric character
        > 5: extreme, chaotic timbres (approach with feedback care)

  LP filter in the feedback path
  --------------------------------
  `fi.lowpass(1, 7000.0)` — first-order LP at 7 kHz in the feedback path.
  This passes the first ~16 harmonics of a 440 Hz tone and attenuates above.
  The feedback coefficient :fb controls the amplitude decay; the LP controls
  the rate at which high harmonics decay relative to low ones.

  A softer string: lower LP cutoff (e.g. fi.lowpass(1, 3000.0))
  A brighter string: higher cutoff (fi.lowpass(1, 12000.0))
  More spectral rolloff: higher order (fi.lowpass(2, 7000.0))

  These are not exposed as parameters — edit the patch for alternative tunings.

  de.fdelay for fractional delay
  --------------------------------
  Integer delay `de.delay` would quantize N to the nearest sample, causing
  pitch quantization and click artefacts when N changes.  Fractional delay
  `de.fdelay` uses linear interpolation between adjacent samples, giving
  smooth N transitions and alias-free pitch modulation.

  Buffer size: 65536 samples ≈ 1.5 seconds at 44100 Hz.  This covers the
  maximum delay (N = SR / fc_min = 44100 / 20 = 2205 samples) with large
  safety margin.

  Parameters
  ----------
  :fc — fundamental pitch in Hz (20–2000; default 110)
  :fm — FM modulation rate in Hz (0.01–20; default 5.0)
  :ix — FM modulation index (0–5; default 1.0)
  :fb — feedback coefficient (0.8–0.999; default 0.98)

  Audio inputs / Outputs
  ----------------------
  in: gate signal (0/1) — noise burst injected into KS loop while gate > 0
    :out — KS-FM resonance; decays after gate drops"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! kp-fm
  {:params {:fc {:range [20.0 2000.0]  :default 110.0}
            :fm {:range [0.01 20.0]    :default 5.0}
            :ix {:range [0.0 5.0]      :default 1.0}
            :fb {:range [0.8 0.999]    :default 0.98}}}
  (let [gt  (audio-in)
        fc  (param :fc)
        fm  (param :fm)
        ix  (param :ix)
        fb  (param :fb)
        out (faust "ks ~ _\n  with {\n    N = max(0.0,ma.SR/max(20.0,%fc+%ix*%fm*os.osc(%fm))-1.0);\n    ks(y) = no.noise*%gt+(de.fdelay(65536,N,y):fi.lowpass(1,7000.0):*(%fb));\n  }"
                   {:gt gt :fc fc :fm fm :ix ix :fb fb})]
    (output :out out)))
