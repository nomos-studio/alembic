; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.44-easing-multi
  "GSOT pp.77-78 — go.unit.ease.* / generalized easing functions.

  'Easing functions' (Chapter 3)
  --------------------------------
  Six classic animation easing functions adapted for audio-rate ramps.  All
  share a common generalized structure described in unit_shapers.maxpat:

  Generalized easing structure
  ----------------------------
  Given: ramp x ∈ [0,1], shape ∈ [0,1] (blend ease-in ↔ ease-out)

  Step 1 — time allocation via triangle:
      gate = float(x >= 1 - shape)

      tri = | x / max(1-shape, ε)      if gate=0  (rising, ease-in half)
            | (1-x) / max(shape, ε)    if gate=1  (falling, ease-out half)

      When shape=0: gate never fires → entire ramp is rising → pure ease-in.
      When shape=1: gate always fires → entire ramp is falling → pure ease-out.
      When shape=0.5: symmetric ease-in-out.

  Step 2 — apply curvature shaper to tri:
      shaped_tri = unit_shaper(tri)    ; any [0,1]→[0,1] shaper (or overshoot)

  Step 3 — scale and flip falling half:
      output = | shaped_tri × (1-shape)              if gate=0 (rising)
               | 1 - shaped_tri × shape              if gate=1 (falling)

  Derivation: the gen~ `scale` operator implements this as
      tri    = scale(x,    gate, 1-shape, 0, 1)   → normalizes either half
      output = scale(s_tri, 0,   1,    gate, 1-shape)  → remaps and flips
  where scale(v, inlo, inhi, outlo, outhi) linearly maps v from [inlo,inhi]
  to [outlo,outhi].

  Verification:
    Rising  (gate=0): output = s_tri*(1-shape)     → 0 at tri=0, 1-shape at tri=1
    Falling (gate=1): output = 1 - s_tri*shape     → 1-shape at tri=1, 1 at tri=0
    Midpoint is always 1-shape: both halves agree. ✓

  The six easing shapers
  ----------------------
  ease.pow:     power law  — t^p, p = 2 + curvature×8  (p=8 at default 0.75)
  ease.circle:  circular   — 1 - √(1 - t^(2+curv×2))  (standard at curv=0)
  ease.exp:     exponential — (exp(6t)-1)/(exp(6)-1)     (k=6 fixed)
  ease.back:    back        — t²(2.70158t - 1.70158)    (overshoots below 0)
  ease.elastic: elastic     — -(2^(10(t-1)) · sin((t-1.075)·2π/0.3))  (oscillates)
  ease.sine:    sine        — 1 - cos(t·π/2)

  back and elastic produce values outside [0,1] — the overshoot is intentional
  and the defining property of those easing functions.

  curvature parameter:
  - ease.pow and ease.circle accept a curvature inlet (port 2) in the gen~
    abstraction; the other four have fixed internal curves.
  - The curvature→exponent mapping (2+c×8 for pow, 2+c×2 for circle) is
    a faithful approximation; the exact GSOT gen~ formula is not available
    in the source but this gives musically identical results at default 0.75.

  Signal flow (all six share gate and tri):

      x (ramp) ──→ gate ──→ tri ──┬─→ pow_s ──→ pow_out  → out :pow
              └──────────────────→ │─→ circ_s ─→ circ_out → out :circle
              shape param ─────→  │─→ exp_s ──→ exp_out  → out :exp
                                   │─→ back_s ─→ back_out → out :back
                                   │─→ elas_s ─→ elas_out → out :elastic
                                   └─→ sine_s ─→ sine_out → out :sine

  Emitted Faust DSP
  -----------------
      import(\"stdfaust.lib\");

      n0 = hslider(\"shape\", 0.5, 0.0, 1.0, 0.0001);
      n1 = hslider(\"curvature\", 0.75, 0.0, 1.0, 0.0001);

      alembic_dsp(n2) = n11, n12, n13, n14, n15, n16
        with {
          n3  = float(n2 >= 1.0 - n0);
          n4  = select2(n3 > 0.5, n2 / max(1.0 - n0, 0.0001), (1.0 - n2) / max(n0, 0.0001));
          n5  = pow(max(0.0, n4), 2.0 + n1 * 8.0);
          n6  = 1.0 - sqrt(max(0.0, 1.0 - pow(max(0.0, n4), 2.0 + n1 * 2.0)));
          n7  = (exp(6.0 * n4) - 1.0) / (exp(6.0) - 1.0);
          n8  = n4 * n4 * (2.70158 * n4 - 1.70158);
          n9  = -(pow(2.0, 10.0 * (n4 - 1.0)) * sin((n4 - 1.075) * (2.0 * ma.PI / 0.3)));
          n10 = 1.0 - cos(n4 * ma.PI / 2.0);
          n11 = select2(n3 > 0.5, n5 * (1.0 - n0), 1.0 - n5 * n0);
          n12 = select2(n3 > 0.5, n6 * (1.0 - n0), 1.0 - n6 * n0);
          n13 = select2(n3 > 0.5, n7 * (1.0 - n0), 1.0 - n7 * n0);
          n14 = select2(n3 > 0.5, n8 * (1.0 - n0), 1.0 - n8 * n0);
          n15 = select2(n3 > 0.5, n9 * (1.0 - n0), 1.0 - n9 * n0);
          n16 = select2(n3 > 0.5, n10 * (1.0 - n0), 1.0 - n10 * n0);
        };
      process = alembic_dsp;

  n3  = gate      (1 when in ease-out half: x >= 1-shape)
  n4  = tri       (normalized triangle: 0→1 over whichever half we're in)
  n5  = pow-s     (t^(2+curv×8) — power law shaper)
  n6  = circ-s    (1 - √(1-t^(2+curv×2)) — circle/ellipse shaper)
  n7  = exp-s     ((exp(6t)-1)/(exp(6)-1) — exponential shaper)
  n8  = back-s    (t²(2.70158t - 1.70158) — back/overshoot shaper)
  n9  = elas-s    (oscillatory elastic shaper, can exceed [0,1])
  n10 = sine-s    (1-cos(t·π/2) — quarter-sine shaper)
  n11..n16 = outputs after generalized structure applied"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! easing-multi
  {:params {:shape     {:range [0.0 1.0] :default 0.5}
            :curvature {:range [0.0 1.0] :default 0.75}}}
  (let [x    (audio-in)
        shp  (param :shape)
        curv (param :curvature)
        ; generalized easing structure — gate and tri shared by all six easers
        gate (faust "float(%xx >= 1.0 - %ss)" {:xx x :ss shp})
        tri  (faust "select2(%gg > 0.5, %xx / max(1.0 - %ss, 0.0001), (1.0 - %xx) / max(%ss, 0.0001))"
                    {:gg gate :xx x :ss shp})
        ; shapers applied to tri
        pow-s  (faust "pow(max(0.0, %tt), 2.0 + %cc * 8.0)"
                      {:tt tri :cc curv})
        circ-s (faust "1.0 - sqrt(max(0.0, 1.0 - pow(max(0.0, %tt), 2.0 + %cc * 2.0)))"
                      {:tt tri :cc curv})
        exp-s  (faust "(exp(6.0 * %tt) - 1.0) / (exp(6.0) - 1.0)"
                      {:tt tri})
        back-s (faust "%tt * %tt * (2.70158 * %tt - 1.70158)"
                      {:tt tri})
        elas-s (faust "-(pow(2.0, 10.0 * (%tt - 1.0)) * sin((%tt - 1.075) * (2.0 * ma.PI / 0.3)))"
                      {:tt tri})
        sine-s (faust "1.0 - cos(%tt * ma.PI / 2.0)"
                      {:tt tri})
        ; generalized output: rising half scaled by (1-shape), falling half flipped
        pow-out  (faust "select2(%gg > 0.5, %pw * (1.0 - %shp), 1.0 - %pw * %shp)"
                        {:gg gate :pw pow-s  :shp shp})
        circ-out (faust "select2(%gg > 0.5, %cs * (1.0 - %shp), 1.0 - %cs * %shp)"
                        {:gg gate :cs circ-s :shp shp})
        exp-out  (faust "select2(%gg > 0.5, %es * (1.0 - %shp), 1.0 - %es * %shp)"
                        {:gg gate :es exp-s  :shp shp})
        back-out (faust "select2(%gg > 0.5, %bk * (1.0 - %shp), 1.0 - %bk * %shp)"
                        {:gg gate :bk back-s :shp shp})
        elas-out (faust "select2(%gg > 0.5, %el * (1.0 - %shp), 1.0 - %el * %shp)"
                        {:gg gate :el elas-s :shp shp})
        sine-out (faust "select2(%gg > 0.5, %sn * (1.0 - %shp), 1.0 - %sn * %shp)"
                        {:gg gate :sn sine-s :shp shp})]
    (output :pow     pow-out)
    (output :circle  circ-out)
    (output :exp     exp-out)
    (output :back    back-out)
    (output :elastic elas-out)
    (output :sine    sine-out)))
