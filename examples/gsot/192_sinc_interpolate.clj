; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.192-sinc-interpolate
  "GSOT p.290 — gen @title sinc-interpolate (Chapter 9: Navigating Waves of Data).

  '4-Point Windowed Sinc Interpolation'
  --------------------------------------
  Replaces the linear interpolation in ex.184 (wavetable-1D) with a windowed
  sinc reconstruction filter.  The sinc function is the IDEAL interpolator for
  band-limited signals: it perfectly reconstructs any signal whose spectrum
  is wholly below Nyquist.  In practice it is truncated (windowed) to N points.

  Why sinc?
  ---------
  Linear interpolation (ex.184) is equivalent to convolving the ideal sinc
  kernel with a triangular window.  Its frequency response rolls off at high
  frequencies and creates mirror-image spectral artefacts near Nyquist.  For
  a sine wavetable the error is inaudible; for a complex multi-harmonic table
  the upper partials can be noticeably smeared or aliased.

  The sinc kernel
  ---------------
  For a fractional read position ph with floor i and fractional part fr = ph − i:

      The ideal sinc weight for the k-th neighbour sample is:
          w_ideal(k) = sinc(fr − k) = sin(π(fr−k)) / (π(fr−k))

  This kernel is infinite in extent.  Truncation to N points uses a window
  to taper the edges smoothly to zero, reducing Gibbs ripple.

  4-point Hann-windowed sinc
  ---------------------------
  Kernel radius L=2; reads from table positions i−1, i, i+1, i+2 (k = −1..2):

      w(k) = sinc(fr − k) × hann((fr − k) / L)

  where:
      sinc(x) = sin(π×x) / (π×x)      [sinc(0) = 1 by L'Hôpital's rule]
      hann(t) = 0.5 × (1 + cos(π×t))  [Hann window on t ∈ [−1, 1]]

  With L=2: the window argument to hann is (fr−k)/2, which maps the range
  [−2, 2] to [−1, 1]:
      hann at k=−1:  hann((fr+1)/2),  ∈ [0, 0.5]  for fr ∈ [0,1)
      hann at k=0:   hann(fr/2),      ∈ [0.5, 1]  for fr ∈ [0,1)
      hann at k=1:   hann((fr−1)/2),  ∈ [0.5, 1]  for fr ∈ [0,1)
      hann at k=2:   hann((fr−2)/2),  ∈ [0, 0.5]  for fr ∈ [0,1)

  At fr=0 (exact integer position):
      w(−1) = sinc(1) × hann(0.5)  = 0 × 0.5  = 0
      w(0)  = sinc(0) × hann(0)    = 1 × 1.0  = 1
      w(1)  = sinc(−1) × hann(−0.5) = 0 × 0.5 = 0
      w(2)  = sinc(−2) × hann(−1)  = 0 × 0    = 0
      output = table[i]  ✓ (exact sample, no interpolation error)

  sinc(0) singularity
  --------------------
  sinc(x) = sin(πx)/(πx) is undefined at x=0 (0/0 form).  By L'Hôpital,
  lim_{x→0} sin(πx)/(πx) = 1.  To avoid NaN in floating-point computation:

      sinc(x) = sin(π × max(|x|, ε)) / (π × max(|x|, ε))

  With ε = 1e-9: at x=0, both sin(π×10⁻⁹) ≈ π×10⁻⁹ and the denominator
  π×10⁻⁹ cancel, giving ≈ 1.0.  Error < 10⁻¹⁸ relative to true value.

  The sinc(x) values are even (sinc(−x) = sinc(x)), so using abs(x) gives the
  correct result for both positive and negative arguments.

  Faust sum() unrolling
  ---------------------
  `sum(k, 4, w(k−1) * s(k−1))` — Faust expands `k` as a compile-time integer
  0, 1, 2, 3 at code generation time.  The argument `k−1` gives literals −1,
  0, 1, 2.  Functions `w(k)` and `s(k)` receive these as literal integers:
      k=0: w(−1)×s(−1)     — sample at i−1
      k=1: w(0) ×s(0)      — sample at i
      k=2: w(1) ×s(1)      — sample at i+1
      k=3: w(2) ×s(2)      — sample at i+2

  Wrapping: `s(k) = rdtable(N, ..., (i0+k)&(N-1))`.  For k=−1 and i0=0:
  `(0 + (−1)) & 1023 = 0xFFFFFFFF & 0x3FF = 1023 = N−1`.  Two's-complement
  bitmask wrap works correctly for negative offsets when N is a power of 2.

  Quality vs. linear interpolation (ex.184)
  ------------------------------------------
  For a sine wavetable (single harmonic), both give nearly identical results —
  the waveform is already maximally smooth and neither method introduces
  significant error.  The quality difference is pronounced for complex waveforms
  (harmonically rich tables) where the linear interpolator smears high partials
  and the windowed sinc preserves them more accurately.

  Increasing the kernel radius from 4 to 8 or 16 points gives further quality
  improvement at proportionally higher cost (N multiply-adds per sample).

  Relationship to mipmapping (pp.280-287)
  ----------------------------------------
  Sinc interpolation and mipmapping are complementary, not competing:
  — Mipmapping ensures harmonics above Nyquist are absent from the table at
    the outset (prefiltering).
  — Sinc interpolation reconstructs the band-limited signal from the table
    samples with minimal error (postfiltering / reconstruction).
  A complete production wavetable oscillator uses both.

  Parameters
  ----------
  :fc — playback frequency in Hz (20–4000; default 220)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained wavetable oscillator)
    :out — 4-point Hann-windowed sinc interpolated output from sine wavetable"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! sinc-interpolate
  {:params {:fc {:range [20.0 4000.0] :default 220.0}}}
  (let [fc  (param :fc)
        out (faust
              "sum(k,4,w(k-1)*s(k-1))
               with {
                 N = 1024;
                 ph = os.phasor(1,%{fc})*float(N);
                 i0 = int(ph)&(N-1);
                 fr = ph-float(int(ph));
                 sinc(x) = sin(ma.PI*max(abs(x),1e-9))/(ma.PI*max(abs(x),1e-9));
                 hann(x) = 0.5*(1.0+cos(ma.PI*x/2.0));
                 w(k) = sinc(fr-float(k))*hann(fr-float(k));
                 s(k) = rdtable(N,os.sinwaveform(N),(i0+k)&(N-1));
               }"
              {:fc fc})]
    (output :out out)))
