; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.197-wave-terrain-osc
  "GSOT pp.295-296 — wave terrain oscillator (codebox, no named .maxpat).

  'Wave Terrain Oscillator with Lissajous Orbit'
  ------------------------------------------------
  Assembles the terrain-reader utility (ex.196) into a complete oscillator by
  supplying an orbit generator.  The orbit is a Lissajous figure — a pair of
  sinusoids that trace closed curves on the 2D terrain surface.

  Orbit generator (Lissajous figure)
  ------------------------------------
  cx(t) = 0.5 + 0.5 × cos(2π × fc × t)           ∈ [0, 1)
  cy(t) = 0.5 + 0.5 × cos(2π × fc × :rt × t + 2π × :ph) ∈ [0, 1)

  :fc controls the orbit speed (fundamental output frequency when :rt=1 and the
  terrain is a pure sine surface).  :rt is the frequency ratio between the y and
  x oscillators.  :ph is the phase offset of the y oscillator.

  Lissajous figures and their spectra
  -------------------------------------
  The output spectrum depends on which region of the terrain the orbit visits
  and the sequence in which it visits them.  A change in :rt or :ph can shift
  the output from a near-sine to a rich multi-partial sound without changing the
  terrain at all:

  :rt=1, :ph=0.0 — orbit is a diagonal line (cx=cy always).  Output is the
      terrain's diagonal cross-section; spectrum dominated by the terrain's main
      harmonic structure along that line.

  :rt=1, :ph=0.25 — orbit is a circle (or ellipse when terrain is rectangular).
      The symmetry of the orbit imposes even/odd harmonic suppression patterns
      depending on the terrain's symmetry.

  :rt=2, :ph=0.0 — figure-8 orbit; repeats twice per x-cycle.  Output frequency
      = 2 × :fc.  Useful for sub-octave or harmonic doubling effects.

  :rt=3, :ph=0.25 — trefoil orbit; non-repeating on the timescale of a few :fc
      periods, slowly closing after 1 full y-cycle.  Rich output spectrum.

  :rt=1.5 (fractional) — non-repeating orbit; never exactly closes.  The output
      is quasi-periodic: almost-periodic but with slowly shifting timbre as the
      orbit rotates across the terrain.  Environmental, evolving sound character.

  This is the GSOT codebox wave terrain example: the codebox defines cx and cy
  from two cosine oscillators with ratio :rt and phase offset :ph, then reads
  the terrain at each (cx, cy) sample using bilinear interpolation.

  Terrain (same as ex.196)
  -------------------------
  The 8-waveform harmonic bank: W=512, H=8.  Waveform k has k+1 harmonics with
  1/n rolloff.  This terrain has gentle, regular undulations — a suitable surface
  for demonstrating how orbit shape controls the output spectrum.

  Mapping considerations
  ----------------------
  The cosine orbit places cx and cy in [0, 1) but the terrain is periodic with
  period 1 in both dimensions — successive repetitions of the terrain extend
  beyond [0,1).  Since the orbit stays in [0,1), we are always in the first
  terrain period.  The bitmask/modulo wrapping in the bilinear reader handles
  the seam at the terrain boundary (cx≈1 reads correctly as the table wraps).

  The orbit extent is controlled by the 0.5 radius in cx and cy.  Shrinking the
  radius (amplitude) keeps the orbit near the terrain centre; expanding it (with
  audio-rate orbit modulation, not shown here) explores more of the surface.

  Parameters
  ----------
  :fc — orbit frequency in Hz; ≈ output fundamental when :rt=1 (20–2000; default 220)
  :rt — y/x frequency ratio; 1=circle/line, 2=figure-8, 0.5=half-freq y orbit
        (0.1–8.0; default 1.0)
  :ph — y oscillator phase offset [0,1); 0=diagonal, 0.25=circle/ellipse
        (0–1; default 0.25)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained wave terrain oscillator)
    :out — terrain height sampled along the Lissajous orbit at :fc Hz"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! wave-terrain-osc
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :rt {:range [0.1 8.0]     :default 1.0}
            :ph {:range [0.0 1.0]     :default 0.25}}}
  (let [fc  (param :fc)
        rt  (param :rt)
        ph  (param :ph)
        out (faust
              "terrain
               with {
                 W=512; H=8; N=W*H;
                 xt=ba.time%W; yt=ba.time/W;
                 ang=2.0*ma.PI*float(xt)/float(W);
                 h(k)=sin(ang*float(k+1))/float(k+1)*float(k<=yt);
                 tbl_init=0.5*(h(0)+h(1)+h(2)+h(3)+h(4)+h(5)+h(6)+h(7));
                 tbl(i)=rdtable(N,tbl_init,max(0,min(N-1,i)));
                 cx=0.5+0.5*cos(2.0*ma.PI*os.phasor(1,%fc));
                 cy=0.5+0.5*cos(2.0*ma.PI*os.phasor(1,%fc*%rt)+2.0*ma.PI*%ph);
                 xfull=cx*float(W); yfull=cy*float(H);
                 xr=int(xfull); yr=int(yfull);
                 x0=xr&(W-1); x1=(x0+1)&(W-1);
                 y0=yr%H; y1=(y0+1)%H;
                 xf=xfull-float(xr); yf=yfull-float(yr);
                 s00=tbl(y0*W+x0); s10=tbl(y0*W+x1);
                 s01=tbl(y1*W+x0); s11=tbl(y1*W+x1);
                 terrain=s00*(1.0-xf)*(1.0-yf)+s10*xf*(1.0-yf)
                         +s01*(1.0-xf)*yf+s11*xf*yf;
               }"
              {:fc fc :rt rt :ph ph})]
    (output :out out)))
