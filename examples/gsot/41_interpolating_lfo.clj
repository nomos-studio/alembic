; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.41-interpolating-lfo
  "GSOT Chapter 3 — go.lfo / interpolating-LFO: full parameterised smooth-step LFO.

  The culminating example of the 'From ramps to LFOs' section.  Builds on
  smooth-stepped-shaped (example 40) with four additional ideas:

  1. Skewed triangle → two rising ramps per cycle
     go.unit.triangle(phasor, :skew) produces a triangle wave that peaks at
     the :skew fraction of the cycle.  The falling half is inverted (1-x) so
     BOTH halves are always-rising ramps.  go.ramp2trig fires TWICE per cycle
     — once at the skew point, once at the wrap — giving two interpolation
     steps per LFO period.

  2. Arc-blend shape with :shape parameter
     go.unit.arc(x, shape) is NOT just sqrt(x*(2-x)).  It is a blend between
     linear and the quarter-circle arc:
         arc_blend(x, sf) = x + sf * (sqrt(max(0, x*(2-x))) - x)
     :shape = 0 → linear ramp; :shape = 1 → full arc; :shape = 0.5 → blend.

  3. Symmetry: mirror the shape on falling half-cycles
     When :symmetry > 0.5 AND the current interpolation is heading downward
     (from >= to), use shape_factor = 1 - :shape instead of :shape.
     This makes the descent curve mirror the ascent: if shape eases in on the
     way up, it eases out on the way down.

  4. Bipolar output
     :bipolar = 0 → unipolar [0, 1].
     :bipolar = 1 → bipolar [-1, 1] via out*(1+bp)-bp.

  Signal flow:

      phasor(:hz)
        ├─→ go.unit.triangle(:skew) → tri
        │     rising? = (tri - tri') > 0
        │     r = rising? tri : 1-tri       (always-rising ramp)
        │     go.ramp2trig(r) → trig        (2× per cycle)
        │
        ├─→ noise S&H on trig → to          (new target each half-cycle)
        │     from = S&H of to' on trig     (previous target as start)
        │
        ├─→ symmetry: (from>=to AND sym)? (1-shape) : shape → sf
        │
        └─→ arc_blend(r, sf) → blend
              mix(from, to, blend) → out
              out*(1+bp)-bp → scaled

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0  = hslider(\"hz\", 2.0, 0.01, 20.0, 0.001999);
      n1  = os.phasor(1.0, n0);
      n2  = hslider(\"skew\", 0.5, 0.01, 0.99, 0.0001);
      n3  = select2(n1>=n2,n1/max(n2,0.0001),(1.0-n1)/max(1.0-n2,0.0001));
      n4  = float((n3-n3')>0.0);
      n5  = select2(n4>0.5,1.0-n3,n3);
      n6  = (n5 - n5');
      n7  = abs(n6);
      n8  = 0.5;
      n9  = (float(n7 > n8));
      n11 = abs(no.noise);
      n12 = (select2(n9 > 0.5, _, n11) ~ _);
      n13 = (select2(n9>0.5,_,n12')~_);
      n14 = float(n13>=n12);
      n15 = hslider(\"symmetry\", 1.0, 0.0, 1.0, 0.0001);
      n16 = hslider(\"shape\", 0.5, 0.0, 1.0, 0.0001);
      n17 = select2(float(n14>0.5)*float(n15>0.5)>0.5,n16,1.0-n16);
      n18 = sqrt(max(0.0,n5*(2.0-n5)));
      n19 = n5+n17*(n18-n5);
      n20 = n13+n19*(n12-n13);
      n21 = hslider(\"bipolar\", 0.0, 0.0, 1.0, 0.0001);
      n22 = n20*(1.0+n21)-n21;

      process = n22, n1;

  n3  = tri    (skewed triangle from phasor)
  n4  = gate   (1=rising, 0=falling)
  n5  = r      (always-rising ramp — falling half inverted)
  n9  = trig   (go.ramp2trig of r — fires twice per cycle)
  n12 = to     (S&H noise on trig)
  n13 = from   (S&H of n12' = prev target on trig)
  n14 = fal    (1 when from >= to = heading down)
  n17 = sf     (shape factor, possibly inverted for falling+symmetry)
  n18 = arc    (quarter-circle arc of r)
  n19 = blend  (arc-blended rising ramp: r + sf*(arc-r))
  n20 = out    (from + blend*(to-from))
  n22 = scaled (bipolar-scaled output)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! interpolating-lfo
  {:params {:hz       {:range [0.01 20.0] :default 2.0}
            :skew     {:range [0.01 0.99] :default 0.5}
            :shape    {:range [0.0  1.0]  :default 0.5}
            :symmetry {:range [0.0  1.0]  :default 1.0}
            :bipolar  {:range [0.0  1.0]  :default 0.0}}}
  (let [ramp  (phasor (param :hz))
        skew  (param :skew)
        ; skewed triangle: rises 0→1 in first :skew fraction, falls 1→0 in remainder
        tri   (faust "select2(%phase>=%skew,%phase/max(%skew,0.0001),(1.0-%phase)/max(1.0-%skew,0.0001))"
                     {:phase ramp :skew skew})
        ; detect rising half (positive delta); falling half uses inverted ramp
        gate  (faust "float((%tri-%tri')>0.0)" {:tri tri})
        r     (faust "select2(%gate>0.5,1.0-%tri,%tri)" {:gate gate :tri tri})
        ; two triggers per cycle: one at skew (kink), one at wrap
        cmp   (comparator (abs (delta r)) (const 0.5))
        trig  (:out cmp)
        ; target: S&H of noise on trigger
        noise (faust "abs(no.noise)")
        to    (track-hold noise trig)
        ; from: S&H of previous target (to') on trigger — no forward reference needed
        from  (faust "(select2(%trg>0.5,_,%tgt')~_)" {:trg trig :tgt to})
        ; symmetry: when heading down (from>=to) AND sym>0.5, flip shape factor
        fal   (faust "float(%frm>=%tgt)" {:frm from :tgt to})
        sym   (param :symmetry)
        shp   (param :shape)
        sf    (faust "select2(float(%fal>0.5)*float(%sym>0.5)>0.5,%shp,1.0-%shp)"
                     {:fal fal :sym sym :shp shp})
        ; arc-blend: linear interpolation with arc(r) mixed in by sf
        arc   (faust "sqrt(max(0.0,%rr*(2.0-%rr)))" {:rr r})
        bld   (faust "%rr+%sf*(%arc-%rr)" {:rr r :sf sf :arc arc})
        ; smooth interpolation: from → to over the arc-blend ramp
        out   (faust "%frm+%bld*(%tgt-%frm)" {:frm from :bld bld :tgt to})
        ; optional bipolar scaling: [0,1] → [-1,1] when bp=1
        bp    (param :bipolar)
        scl   (faust "%out*(1.0+%bp)-%bp" {:out out :bp bp})]
    (output scl)
    (output :ramp ramp)))
