; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.189-trilinear-interpolation
  "GSOT p.276 — gen @title trilinear-interpolation (Chapter 9: Navigating Waves of Data).

  'Trilinear Interpolation Formula'
  ----------------------------------
  Trilinear interpolation estimates the value at a fractional position
  (xf, yf, zf) within a rectangular cuboid defined by 8 corner samples.

  Corner naming convention (binary index z·y·x):
      s000 = table[z0][y0][x0]    s100 = table[z0][y0][x1]
      s010 = table[z0][y1][x0]    s110 = table[z0][y1][x1]
      s001 = table[z1][y0][x0]    s101 = table[z1][y0][x1]
      s011 = table[z1][y1][x0]    s111 = table[z1][y1][x1]

  where (x0,y0,z0) is the integer floor position, (x1,y1,z1) = (x0+1,y0+1,z0+1)
  are the adjacent neighbours, and (xf,yf,zf) ∈ [0,1) are the fractional
  offsets within the cell (computed by ex.190 trilinear-indices).

  Derivation as nested bilinear steps
  -------------------------------------
  Trilinear = bilinear interpolation in x×y, done twice (once per z slice),
  then linear interpolation between the two z-slice results.

  Step 1 — bilinear interpolation at z=z0:
      R0 = s000(1−xf)(1−yf) + s100·xf(1−yf) + s010(1−xf)yf + s110·xf·yf

  Step 2 — bilinear interpolation at z=z1:
      R1 = s001(1−xf)(1−yf) + s101·xf(1−yf) + s011(1−xf)yf + s111·xf·yf

  Step 3 — linear interpolation across z:
      out = R0(1−zf) + R1·zf

  Expanded full formula (8 terms):
      out = s000·(1−xf)(1−yf)(1−zf) + s100·xf(1−yf)(1−zf)
          + s010·(1−xf)·yf·(1−zf)   + s110·xf·yf·(1−zf)
          + s001·(1−xf)(1−yf)·zf    + s101·xf(1−yf)·zf
          + s011·(1−xf)·yf·zf       + s111·xf·yf·zf

  Each of the 8 corner samples is weighted by the VOLUME of the opposing
  sub-cuboid.  The 8 weights always sum to 1:

      Σ weights = [(1−xf)+xf] × [(1−yf)+yf] × [(1−zf)+zf] = 1·1·1 = 1  ✓

  Comparison with bilinear (ex.186)
  -----------------------------------
  ex.186 (bilinear):   4 corners, 2 fractional coords, weights = area products
  ex.189 (trilinear):  8 corners, 3 fractional coords, weights = volume products

  Each additional dimension DOUBLES the number of corners and adds one more
  linear interpolation step.  N-linear interpolation requires 2^N corners.

  At the boundary (zf=0): the z1-slice terms vanish and the result reduces
  exactly to the bilinear formula for the z0 slice. ✓

  This patch
  ----------
  Standalone demonstration with parameters for all 8 corner values and 3
  fractional coords.  In ex.188 (wavetable_3D) and ex.191 (attractor), the
  corner values are live rdtable reads and the fractional coords come from
  the phasor, :wp, and :zp / attractor state.

  Parameters
  ----------
  :xf — fractional x offset [0,1); 0=exactly at x0, 1=exactly at x1 (default 0.5)
  :yf — fractional y offset [0,1) (default 0.5)
  :zf — fractional z offset [0,1) (default 0.5)
  :s0 — s000 corner (x0,y0,z0) default −1.0
  :s1 — s100 corner (x1,y0,z0) default +1.0
  :s2 — s010 corner (x0,y1,z0) default +0.5
  :s3 — s110 corner (x1,y1,z0) default −0.5
  :s4 — s001 corner (x0,y0,z1) default +0.8
  :s5 — s101 corner (x1,y0,z1) default −0.8
  :s6 — s011 corner (x0,y1,z1) default −0.3
  :s7 — s111 corner (x1,y1,z1) default +0.3

  Audio inputs / Outputs
  ----------------------
  (no audio input — all values from params; see ex.188/191 for live table reads)
    :out — trilinearly interpolated value; 8-term volume-weighted blend"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! trilinear-interpolation
  {:params {:xf {:range [0.0 1.0]  :default 0.5}
            :yf {:range [0.0 1.0]  :default 0.5}
            :zf {:range [0.0 1.0]  :default 0.5}
            :s0 {:range [-1.0 1.0] :default -1.0}
            :s1 {:range [-1.0 1.0] :default  1.0}
            :s2 {:range [-1.0 1.0] :default  0.5}
            :s3 {:range [-1.0 1.0] :default -0.5}
            :s4 {:range [-1.0 1.0] :default  0.8}
            :s5 {:range [-1.0 1.0] :default -0.8}
            :s6 {:range [-1.0 1.0] :default -0.3}
            :s7 {:range [-1.0 1.0] :default  0.3}}}
  (let [xf  (param :xf)
        yf  (param :yf)
        zf  (param :zf)
        s0  (param :s0)
        s1  (param :s1)
        s2  (param :s2)
        s3  (param :s3)
        s4  (param :s4)
        s5  (param :s5)
        s6  (param :s6)
        s7  (param :s7)
        out (faust
              "%{s0}*(1.0-%{xf})*(1.0-%{yf})*(1.0-%{zf})+%{s1}*%{xf}*(1.0-%{yf})*(1.0-%{zf})
               +%{s2}*(1.0-%{xf})*%{yf}*(1.0-%{zf})+%{s3}*%{xf}*%{yf}*(1.0-%{zf})
               +%{s4}*(1.0-%{xf})*(1.0-%{yf})*%{zf}+%{s5}*%{xf}*(1.0-%{yf})*%{zf}
               +%{s6}*(1.0-%{xf})*%{yf}*%{zf}+%{s7}*%{xf}*%{yf}*%{zf}"
              {:xf xf :yf yf :zf zf
               :s0 s0 :s1 s1 :s2 s2 :s3 s3
               :s4 s4 :s5 s5 :s6 s6 :s7 s7})]
    (output :out out)))
