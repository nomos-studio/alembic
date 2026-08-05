; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.190-trilinear-indices
  "GSOT p.277 — gen @title trilinear-indices (Chapter 9: Navigating Waves of Data).

  'Trilinear Index Computation for 3D Wavetable Lookup'
  -------------------------------------------------------
  Extends ex.185 (bilinear-indices) from 2D to 3D by adding a Z fractional
  coordinate.  Given continuous positions (xp, wp, zp) ∈ [0,1)³:

  Full index computation (W=256, H=4, D=4 matching ex.188):

    x-axis (sample position within waveform, W=256):
      xfull = xp × W
      x0    = floor(xfull) & (W−1)     — bitmask wrap (W is power of 2)
      x1    = (x0 + 1) & (W−1)
      xf    = xfull − floor(xfull)      ∈ [0, 1)

    y-axis (harmonic-count axis, H=4):
      yfull = wp × H
      y0    = floor(yfull) % H
      y1    = (y0 + 1) % H
      yf    = yfull − floor(yfull)      ∈ [0, 1)

    z-axis (phase-offset axis, D=4):
      zfull = zp × D
      z0    = floor(zfull) % D
      z1    = (z0 + 1) % D
      zf    = zfull − floor(zfull)      ∈ [0, 1)

  Flattened 1D indices (flat = z×H×W + y×W + x):
      i000 = z0×H×W + y0×W + x0         — (x0, y0, z0) floor corner
      i100 = z0×H×W + y0×W + x1         — (x1, y0, z0)
      i010 = z0×H×W + y1×W + x0         — (x0, y1, z0)
      i110 = z0×H×W + y1×W + x1         — (x1, y1, z0)
      i001 = z1×H×W + y0×W + x0         — (x0, y0, z1)
      i101 = z1×H×W + y0×W + x1         — (x1, y0, z1)
      i011 = z1×H×W + y1×W + x0         — (x0, y1, z1)
      i111 = z1×H×W + y1×W + x1         — (x1, y1, z1)

  These 8 indices drive 8 rdtable reads in ex.188/191; xf, yf, zf drive
  the 8 interpolation weights in ex.189 (trilinear-interpolation).

  This patch outputs xf, yf, zf — the fractional parts that the interpolation
  formula needs.  The integer indices are described above and embedded in
  the wavetable patches (ex.188, ex.191).

  Comparison with ex.185 (bilinear-indices)
  ------------------------------------------
  ex.185: 1 audio-in (xp), 1 param (:wp); outputs :xf, :yf
  ex.190: 1 audio-in (xp), 2 params (:wp, :zp); outputs :xf, :yf, :zf

  The third fractional coordinate :zf adds the Z-axis interpolation weight,
  doubling the corner count (4→8) and adding one lerp step to the formula.

  Parameters
  ----------
  :wp — Y wavetable position [0,1); 0=y0 slot, 1=y3 slot (0–1; default 0.0)
  :zp — Z wavetable position [0,1); 0=z0 slot, 1=z3 slot (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  in: x position [0,1) — oscillator phasor at playback frequency
    :xf — fractional sample position (xp×256 − floor(xp×256)) ∈ [0,1)
    :yf — fractional Y position (:wp×4 − floor(:wp×4)) ∈ [0,1)
    :zf — fractional Z position (:zp×4 − floor(:zp×4)) ∈ [0,1)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! trilinear-indices
  {:params {:wp {:range [0.0 1.0] :default 0.0}
            :zp {:range [0.0 1.0] :default 0.0}}}
  (let [xp  (audio-in)
        wp  (param :wp)
        zp  (param :zp)
        xf  (faust "%{xp}*256.0-float(int(%{xp}*256.0))" {:xp xp})
        yf  (faust "%{wp}*4.0-float(int(%{wp}*4.0))"     {:wp wp})
        zf  (faust "%{zp}*4.0-float(int(%{zp}*4.0))"     {:zp zp})]
    (output :xf xf)
    (output :yf yf)
    (output :zf zf)))
