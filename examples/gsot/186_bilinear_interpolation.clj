; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.186-bilinear-interpolation
  "GSOT p.273 — gen @title bilinear-interpolation (Chapter 9: Navigating Waves of Data).

  'Bilinear Interpolation Formula'
  ---------------------------------
  Bilinear interpolation estimates the value at a fractional position (xf, yf)
  within a rectangular cell defined by four corner samples:

      Q00 = table[y0][x0]    Q10 = table[y0][x1]
      Q01 = table[y1][x0]    Q11 = table[y1][x1]

  where (x0, y0) is the integer floor position, (x1, y1) = (x0+1, y0+1) are
  the adjacent neighbours, and (xf, yf) ∈ [0, 1) are the fractional offsets
  within the cell (computed by ex.185 bilinear-indices).

  The formula
  -----------
  Bilinear interpolation performs LINEAR interpolation in x, then in y:

  Step 1 — interpolate across x at y=y0 and y=y1:
      R0 = Q00 × (1 − xf) + Q10 × xf      — row y0, fraction xf
      R1 = Q01 × (1 − xf) + Q11 × xf      — row y1, fraction xf

  Step 2 — interpolate across y using R0 and R1:
      out = R0 × (1 − yf) + R1 × yf

  Expanding:
      out = Q00 × (1−xf) × (1−yf)
          + Q10 × xf     × (1−yf)
          + Q01 × (1−xf) × yf
          + Q11 × xf     × yf

  This is the bilinear interpolation formula: each corner is weighted by the
  product of its fractional distances from the query point (xf, yf).

  Geometric interpretation
  -------------------------
  The four weights sum to 1:
      (1−xf)(1−yf) + xf(1−yf) + (1−xf)yf + xf×yf = 1   ✓

  Each weight is the AREA of the sub-rectangle OPPOSITE to its corner.  At
  (xf=0, yf=0) the point is at Q00, so Q00's weight is 1.  At (xf=0.5, yf=0.5)
  all four corners are equidistant and each gets weight 0.25.

  In 2D wavetable synthesis
  --------------------------
  The two dimensions serve different roles:

    x-dimension: LINEAR INTERPOLATION within a single waveform.
      Equivalent to ex.184's single-table interpolation: fractional sample
      position within one waveform cycle.

    y-dimension: WAVEFORM MORPHING between adjacent waveforms in the bank.
      At integer yf=0 the output is exactly waveform y0.  At integer yf=0
      it is exactly waveform y1.  For 0 < yf < 1 the output is a linear
      blend — bilinear interpolation smoothly morphs through the waveform
      bank as :wp sweeps from 0 to 1.

  The PPG Wave 2.x family (1978–1984) used this exact architecture: a wavetable
  position parameter sweeps through a bank of 64 waveforms, each with
  different harmonic content.  Smooth sweeping (this patch) eliminates clicks
  at waveform transitions; the original PPG hardware interpolated between
  adjacent waveforms at the sample rate.

  This patch — standalone demonstration
  ---------------------------------------
  The gen @title bilinear-interpolation sub-patcher in GSOT is a utility block
  with 6 inputs (Q00, Q10, Q01, Q11, xf, yf) and 1 output.  Here it is
  instantiated with parameter-controlled Q values so the formula can be
  inspected at any static (xf, yf) point.

  The outputs are constant (not audio-rate) since all inputs are parameters,
  but the patch validates to a Faust-compilable expression using the standard
  formula.  In ex.187 (wavetables_2D), Q00/Q10/Q01/Q11 are live rdtable reads
  and (xf, yf) are computed from the oscillator phasor and :wp control.

  Parameters
  ----------
  :xf — fractional x offset in [0, 1); 0 = exactly at x0, 1 = exactly at x1
        (default 0.5)
  :yf — fractional y offset in [0, 1); 0 = exactly at y0, 1 = exactly at y1
        (default 0.5)
  :s0 — Q00 corner value (x0, y0); default −1.0
  :s1 — Q10 corner value (x1, y0); default +1.0
  :s2 — Q01 corner value (x0, y1); default +0.5
  :s3 — Q11 corner value (x1, y1); default −0.5

  Audio inputs / Outputs
  ----------------------
  (no audio input — all values from params; see ex.187 for live table reads)
    :out — bilinearly interpolated value
           Q00×(1−xf)(1−yf) + Q10×xf(1−yf) + Q01×(1−xf)yf + Q11×xf×yf"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! bilinear-interpolation
  {:params {:xf {:range [0.0 1.0]  :default 0.5}
            :yf {:range [0.0 1.0]  :default 0.5}
            :s0 {:range [-1.0 1.0] :default -1.0}
            :s1 {:range [-1.0 1.0] :default  1.0}
            :s2 {:range [-1.0 1.0] :default  0.5}
            :s3 {:range [-1.0 1.0] :default -0.5}}}
  (let [xf  (param :xf)
        yf  (param :yf)
        s0  (param :s0)
        s1  (param :s1)
        s2  (param :s2)
        s3  (param :s3)
        out (faust
              "%{s0}*(1.0-%{xf})*(1.0-%{yf})+%{s1}*%{xf}*(1.0-%{yf})+%{s2}*(1.0-%{xf})*%{yf}+%{s3}*%{xf}*%{yf}"
              {:xf xf :yf yf :s0 s0 :s1 s1 :s2 s2 :s3 s3})]
    (output :out out)))
