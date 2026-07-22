; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.99-quantizing-audio-bitcrush
  "GSOT pp.158 — quantizing-audio-bitcrush.maxpat (Chapter 5).

  'Quantization as a timbral shape — audio bitcrusher'
  -----------------------------------------------------
  Applies the Euclidean N-note quantization as a DISTORTION EFFECT on an
  incoming audio signal.  The signal is mapped to N maximally-even quantisation
  levels derived from the inverse digitized ratio floor(12 × i / N).

  Dual of quantizing-timbre (example 98)
  ----------------------------------------
  Example 98 takes a PHASOR and GENERATES audio — the Euclidean staircase IS
  the waveform.  This patch takes an AUDIO SIGNAL and PROCESSES it — the
  Euclidean staircase IS the quantiser transfer function.

  The transfer function (smooth=0)
  ---------------------------------
  For input x ∈ [-1,1) and N quantisation levels:

    1. Map to degree:    deg = (x + 1) × N / 2         [0, N)
    2. Integer degree:   deg_i = floor(deg)             0..N-1
    3. Current level:    semi_q  = floor(12 × deg_i / N)
    4. Next level:       semi_nx = floor(12 × (deg_i+1) / N)
    5. Fractional part:  frac    = deg − deg_i          [0, 1)
    6. Blend:            semi_out = semi_q + smooth × frac × (semi_nx − semi_q)
    7. Normalize:        out = semi_out / 6 − 1

  At smooth=0: output snaps to the N Euclidean semitone levels.  Each level is
  held across a 1/N-wide band of the input range.

  At smooth=1: output linearly interpolates between adjacent levels — the
  Euclidean non-uniformity is still present in the SLOPE of each segment but
  hard steps are gone.  At N=12 (chromatic), smooth=1 is a transparent passthrough.

  Euclidean vs uniform bitcrushing
  ---------------------------------
  Standard bit-depth reduction quantises to 2^B evenly-spaced levels.  This
  patch uses N Euclidean levels instead — maximally even in 12-semitone space,
  so the levels are NOT uniformly spaced (except N=6, which is the whole-tone
  scale and coincidentally uniform at 2-semitone intervals).

  The non-uniform Euclidean spacing creates asymmetric clipping and adds
  pitch-class bias to the distortion.  For N=5 (pentatonic), the five
  output levels correspond to the major pentatonic scale positions; distorted
  pitches are biased toward those harmonics.

  Quantisation level positions per N
  ------------------------------------
  N=2  output levels: −1.0,  0.0                          (2 levels, gap=1.0)
  N=3  output levels: −1.0, −0.33, +0.33                  (3 uniform major-3rd levels)
  N=4  output levels: −1.0, −0.5,   0.0, +0.5             (4 uniform minor-3rd levels)
  N=5  output levels: −1.0, −0.67, −0.33, +0.17, +0.50   (5 non-uniform pentatonic)
  N=6  output levels: −1.0, −0.67, −0.33, 0.0, +0.33, +0.67 (6 uniform whole-tone)
  N=7  output levels: 7 non-uniform diatonic levels
  N=8  output levels: 8 non-uniform octatonic levels

  Smooth passthrough at N=12, smooth=1
  --------------------------------------
  At N=12 and smooth=1: each of 12 quantisation segments has slope exactly 1,
  so the output equals the input (transparent).  At smooth=0 it becomes 12-level
  uniform bitcrush (standard bit reduction).

  Parameters
  ----------
  :beats  — N: quantisation levels (1–8; default 5 = pentatonic)
  :smooth — 0.0=hard Euclidean steps → 1.0=linear interpolation (default 0.0)

  Audio inputs
  ------------
  audio-in 0: in — audio signal; expected range [-1.0, 1.0)

  Outputs
  -------
  :out — quantised audio; N Euclidean levels at smooth=0, smoothly distorted at smooth=1

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n_out
        with {
          n_bt  = hslider(\"beats\",  5.0, 1.0, 8.0,  1.0);
          n_sm  = hslider(\"smooth\", 0.0, 0.0, 1.0, 0.01);
          n_dg  = (n0+1.0)*n_bt/2.0;
          n_di  = floor(n_dg);
          n_sq  = floor(12.0*n_di/max(1.0,n_bt));
          n_sn  = floor(12.0*(n_di+1.0)/max(1.0,n_bt));
          n_fr  = n_dg-n_di;
          n_out = (n_sq+n_sm*n_fr*(n_sn-n_sq))/6.0-1.0;
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! quantizing-audio-bitcrush
  {:params {:beats  {:range [1.0 8.0] :default 5.0}
            :smooth {:range [0.0 1.0] :default 0.0}}}
  (let [audio   (audio-in)
        beats   (param :beats)
        smooth  (param :smooth)
        ; Map audio [-1,1) to continuous degree [0,N)
        degree  (faust "(%in+1.0)*%bt/2.0" {:in audio :bt beats})
        ; Integer degree 0..N-1 and fractional part
        deg-i   (faust "floor(%dg)" {:dg degree})
        frac    (faust "%dg-%di" {:dg degree :di deg-i})
        ; Euclidean semitone level at current degree and next degree
        semi-q  (faust "floor(12.0*%di/max(1.0,%bt))" {:di deg-i :bt beats})
        semi-nx (faust "floor(12.0*(%di+1.0)/max(1.0,%bt))" {:di deg-i :bt beats})
        ; Smooth-step blend between adjacent Euclidean levels; normalize to [-1,1)
        out     (faust "(%sq+%sm*%fr*(%sn-%sq))/6.0-1.0"
                       {:sq semi-q :sm smooth :fr frac :sn semi-nx})]
    (output :out out)))
