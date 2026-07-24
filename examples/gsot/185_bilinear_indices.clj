; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.185-bilinear-indices
  "GSOT p.272 — gen @title bilinear-indices (Chapter 9: Navigating Waves of Data).

  'Bilinear Index Computation for 2D Wavetable Lookup'
  -----------------------------------------------------
  A 2D wavetable is a rectangular grid of single-cycle waveforms.  The two
  dimensions are:

    x — SAMPLE POSITION within one waveform (0..W-1), driven by the oscillator
        phasor.  W = table width (samples per waveform), typically 512 or 1024.

    y — WAVEFORM SELECTION: which waveform in the bank (0..H-1).  H = table
        height (number of waveforms in the bank), typically 8–64.

  The table is stored as a FLATTENED 1D array of size W×H, with waveform y
  occupying entries [y×W .. (y+1)×W − 1].

  Bilinear interpolation requires four surrounding grid points.  Given
  continuous positions (xp, yp) ∈ [0, 1) × [0, 1):

    Fractional sample position:   xfull = xp × W
    Fractional waveform position: yfull = yp × H

    Integer base indices:
      x0 = floor(xfull) % W        — floor sample index, wrapped
      y0 = floor(yfull) % H        — floor waveform index, wrapped

    Adjacent indices (with wrap):
      x1 = (x0 + 1) % W            — next sample index
      y1 = (y0 + 1) % H            — next waveform index

    Fractional parts (distance into cell):
      xf = xfull − floor(xfull)    ∈ [0, 1)
      yf = yfull − floor(yfull)    ∈ [0, 1)

  The four flattened 1D array indices for the four surrounding grid points:
      i00 = y0 × W + x0            — (floor x, floor y)
      i10 = y0 × W + x1            — (next  x, floor y)
      i01 = y1 × W + x0            — (floor x, next  y)
      i11 = y1 × W + x1            — (next  x, next  y)

  These indices drive four rdtable reads; the fractional parts (xf, yf) drive
  the interpolation weights in ex.186 (bilinear-interpolation).

  This patch
  ----------
  Demonstrates the fractional part computation — the information the bilinear
  interpolation engine consumes at runtime.  In practice this computation is
  embedded inside the wavetables_2D patch (ex.187) rather than run as a
  standalone circuit, but isolating it here makes the index arithmetic
  inspectable.

  Inputs:
    audio-in — x position [0, 1): typically the oscillator phasor os.phasor(1,fc)
    :wp      — y position / wavetable position [0, 1): waveform selection

  The table dimensions are fixed to W=512, H=8, matching the waveform bank
  used in ex.187 (wavetables_2D).

  Float floor via int truncation
  --------------------------------
  Faust computes floor via int truncation: `ph − float(int(ph))` gives the
  fractional part in [0, 1) for any ph ≥ 0.  For ph < 0, int() rounds toward
  zero (not toward −∞), so this formula gives values in (−1, 0) for negative
  ph — the phasor is always non-negative, so this is not an issue in practice.

  Parameters
  ----------
  :wp — wavetable position [0, 1); 0 = first waveform, 1 = last waveform
        (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  in: x position [0, 1) — oscillator phasor
    :xf — fractional part of sample position  (xp×W − floor(xp×W)) ∈ [0, 1)
    :yf — fractional part of waveform position (:wp×H − floor(:wp×H)) ∈ [0, 1)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! bilinear-indices
  {:params {:wp {:range [0.0 1.0] :default 0.0}}}
  (let [xp (audio-in)
        wp (param :wp)
        xf (faust "%xp*512.0-float(int(%xp*512.0))" {:xp xp})
        yf (faust "%wp*8.0-float(int(%wp*8.0))"     {:wp wp})]
    (output :xf xf)
    (output :yf yf)))
