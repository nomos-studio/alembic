; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.201-polygonal
  "GSOT pp.303-306 — polygonal.maxpat (Chapter 9: Navigating Waves of Data).

  'Wave Terrain — Polygonal Orbit with PM and Wavefolding'
  ---------------------------------------------------------
  A polygonal orbit traces the perimeter of a regular N-gon over the terrain
  surface, rather than the smooth sinusoidal path of Lissajous (ex.197/199) or
  the reflecting triangle of the carom (ex.200).  The polygon has :ns sides
  (3 = equilateral triangle, 4 = square, 5 = pentagon, 6 = hexagon, …) and
  orbit radius :r1.

  GSOT pp.303-306 discusses four features of the polygonal orbit patch:
    1. The polygonal orbit itself
    2. Sync — phase reset to a fixed starting corner
    3. Phase modulation of the orbit traversal
    4. Wavefolding of the terrain output

  Polygonal orbit — edge-tracing
  --------------------------------
  A regular N-gon has N vertices equally spaced on a circle of radius :r1:

      vertex k:  angle = 2π·k/N,  position = (:r1·cos(angle), :r1·sin(angle))

  The orbit traverses the polygon perimeter at constant speed:
    — Divide the phasor period [0,1) into N equal sectors (one per side).
    — Within each sector, linearly interpolate from vertex k to vertex k+1.
    — This traces each polygon EDGE at constant velocity.

  Constant-speed edge traversal
  -------------------------------
  Constant-speed polygon traversal is achieved naturally by linear interpolation
  between vertices — each edge has equal length (regular polygon) and is covered
  in equal time (equal sector width 1/N).

  Compare with the alternative polar-form approach (r(θ) = apothem/cos(θ mod 2π/N − π/N)):
    — Polar form parametrises by ANGLE: moves faster near vertices (steeper curvature)
      and slower near edge midpoints.
    — Edge-tracing (this implementation) parametrises by ARC LENGTH: moves at constant
      speed everywhere along the perimeter.

  As N increases the polygon approaches a circle; as N → ∞ the orbit converges
  to the Lissajous circle of ex.197.  At N=3 the triangle orbit has sharp 60°
  corners; at N=6 the hexagon corners are 30°.

  Phase modulation
  ----------------
  The phasor driving polygon traversal is phase-modulated by an internal sine
  at 2× :fc (the classic FM carrier:modulator ratio 1:2):

      pm_sig = :pm · sin(2π · 2fc · t)
      ph     = frac(phasor(fc) + pm_sig)

  At :pm=0 the orbit traverses the polygon once per 1/fc period.  At :pm > 0
  the polygon traversal accelerates and decelerates rhythmically, stretching and
  compressing each edge in time.  At :pm ≈ 0.5 the phasor nearly folds back on
  itself, creating audible sidebands around the fundamental.

  Hard sync (GSOT feature, not implemented here)
  -----------------------------------------------
  The GSOT polygonal.maxpat also implements HARD SYNC: an incoming trigger resets
  the phasor to 0 (vertex 0) regardless of where the orbit currently is.  This
  truncates the current polygon traversal, snapping the orbit to the starting
  corner.  Hard sync in wave terrain synthesis creates the same timbral
  'bite' as hard sync in subtractive synthesis — bright transients with a
  fixed phase relationship to the sync source.

  In this Alembic version the phasor is inherently synced to :fc (it completes
  exactly one polygon traversal per period); hard sync from an external trigger
  is not implemented (would require a trigger audio input).

  Wavefolding
  -----------
  A sin-based wavefolder is applied to the terrain output:

      driven  = raw · (1 + :wf · 7)    — amplify 1× → 8× as :wf goes 0 → 1
      wf_out  = sin(driven · π/2)       — fold into [-1, 1]
      terrain = :wf · wf_out + (1−:wf) · raw   — crossfade

  At :wf = 0:  identity — raw terrain output, no shaping.
  At :wf = 0.5: 4× drive through sin; moderate wavefolding, added harmonics.
  At :wf = 1.0: 8× drive through sin; heavy folding; rich upper partial content.

  sin(x·π/2) at x=1 gives 1.0 exactly; at x=2 gives 0; at x=3 gives -1; each
  fold adds a new partial at odd multiples.  With 8× drive the sin folds 4 times
  across the ±1 terrain range, potentially adding partials at 3f, 5f, 7f, 9f
  depending on the orbit's terrain cross-section.

  Tiling
  ------
  With :r1 ≤ 0.5 the entire polygon orbit stays within [0,1).  The harmonic
  bank terrain (W=512, H=8) tiles in x (bitmask) and wraps in y (modulo H).
  For :r1 > 0.5 orbit corners escape the primary tile and re-enter from the
  opposite edge.

  Parameters
  ----------
  :fc — orbit frequency in Hz; one polygon traversal per period (20–2000; default 220)
  :ns — number of polygon sides as a float; 3=triangle, 4=square, 6=hexagon (3–16; default 4)
  :r1 — orbit radius [0, 0.5] for orbit contained in [0,1) terrain tile (default 0.4)
  :pm — phase modulation depth from internal 2× oscillator; 0=off, 0.5=strong (0–1; default 0.0)
  :wf — wavefold amount; 0=transparent, 1=full 8× sin folding (0–1; default 0.0)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained wave terrain oscillator)
    :out — wavefolded terrain height sampled along the polygonal orbit"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! polygonal
  {:params {:fc {:range [20.0 2000.0] :default 220.0}
            :ns {:range [3.0 16.0]    :default 4.0}
            :r1 {:range [0.0 0.5]     :default 0.4}
            :pm {:range [0.0 1.0]     :default 0.0}
            :wf {:range [0.0 1.0]     :default 0.0}}}
  (let [fc  (param :fc)
        ns  (param :ns)
        r1  (param :r1)
        pm  (param :pm)
        wf  (param :wf)
        out (faust
              "terrain
               with {
                 W=512; H=8; N=W*H;
                 xt=ba.time%W; yt=ba.time/W;
                 ang=2.0*ma.PI*float(xt)/float(W);
                 h(k)=sin(ang*float(k+1))/float(k+1)*float(k<=yt);
                 tbl_init=0.5*(h(0)+h(1)+h(2)+h(3)+h(4)+h(5)+h(6)+h(7));
                 tbl(i)=rdtable(N,tbl_init,max(0,min(N-1,i)));
                 pm_s=%{pm}*sin(2.0*ma.PI*os.phasor(1,%{fc}*2.0));
                 ph=ma.frac(os.phasor(1,%{fc})+pm_s);
                 ns_f=max(3.0,%{ns});
                 sec=int(ph*ns_f);
                 t=ph*ns_f-float(sec);
                 a0=2.0*ma.PI*float(sec)/ns_f;
                 a1=2.0*ma.PI*float(sec+1)/ns_f;
                 cx=0.5+%{r1}*(cos(a0)*(1.0-t)+cos(a1)*t);
                 cy=0.5+%{r1}*(sin(a0)*(1.0-t)+sin(a1)*t);
                 xfull=cx*float(W); yfull=cy*float(H);
                 xr=int(xfull); yr=int(yfull);
                 x0=xr&(W-1); x1=(x0+1)&(W-1);
                 y0=yr%H; y1=(y0+1)%H;
                 xf=xfull-float(xr); yf=yfull-float(yr);
                 s00=tbl(y0*W+x0); s10=tbl(y0*W+x1);
                 s01=tbl(y1*W+x0); s11=tbl(y1*W+x1);
                 raw=s00*(1.0-xf)*(1.0-yf)+s10*xf*(1.0-yf)
                     +s01*(1.0-xf)*yf+s11*xf*yf;
                 driven=raw*(1.0+%{wf}*7.0);
                 wf_o=sin(driven*ma.PI*0.5);
                 terrain=%{wf}*wf_o+(1.0-%{wf})*raw;
               }"
              {:fc fc :ns ns :r1 r1 :pm pm :wf wf})]
    (output :out out)))
