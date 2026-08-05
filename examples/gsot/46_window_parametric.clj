; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.46-window-parametric
  "GSOT pp.78-79 — parametric window envelope functions.

  'Window envelope functions' (Chapter 3) — parametric shapes
  -------------------------------------------------------------
  Five windows that accept a shape parameter to control the taper/plateau
  ratio.  All take the same TRIANGLE input as the fixed windows (example 45).

  The five windows and their shape semantics:

  go.unit.trapezoid  (:shape = plateau fraction)
    Linear taper from 0 to 1 over edge region; flat top at 1.
    Edge region: t < 1-shape.  Linear rise: t / max(1-shape, ε).
    Plateau:     t ≥ 1-shape.  Output = 1.

  go.unit.tukey  (:shape = plateau fraction)
    Cosine taper at edges, flat top at center.  The cosine taper provides
    a smooth first-derivative at the taper-plateau boundary.
    Edge region: t < 1-shape.  Taper: 0.5·(1 - cos(πt / max(1-shape, ε))).
    Plateau:     t ≥ 1-shape.  Output = 1.

  go.unit.plancktaper  (:shape = plateau fraction)
    Infinitely differentiable taper: flat at 0 AND all derivatives zero
    at both the edge (t=0) and the plateau boundary (t=1-shape).
    Edge region: t < 1-shape.  Taper: 1/(1+exp(z))  where
        z = ε/max(t,ε') + ε/min(t-ε, -ε')  and  ε = 1-shape.
    Plateau:     t ≥ 1-shape.  Output = 1.
    Note: both branches evaluated by Faust (select2 is not lazy); clamping
    denominators prevents inf at t=0 and division-by-zero at t=ε.

  go.unit.gauss  (:sigma = window width, :power = exponent)
    Generalized Gaussian: exp(-((1-t)/max(σ,ε))^p).
    t=1 (center) → 1.  Edges approach 0 faster as σ→0 or p→∞.
    Default σ=0.4, p=2: edge value exp(-(1/0.4)^2)≈0.002 ≈ 0.
    The gen~ abstraction uses a `param power` that can be set externally
    (windows.maxpat demonstrates p=2 and p=4); both σ and p are exposed here.

  go.unit.raisedcosine  (:shape = rolloff fraction, same as Tukey)
    Cosine rolloff from edges; shape controls the rolloff size.
    In the book and gen~ patcher this receives the same 0.6 shape as tukey,
    suggesting nearly identical behaviour at the same parameter value.
    Implemented here with shape as the ROLLOFF fraction (complement of tukey's
    plateau fraction): edge taper at t < shape; plateau at t ≥ shape.

  Parameter convention comparison (at default shape=0.6):
    tukey:         plateau at t ≥ 0.4  (taper occupies 40% from each edge)
    raisedcosine:  plateau at t ≥ 0.6  (taper occupies 60% from each edge)
    plancktaper:   plateau at t ≥ 0.4  (same as tukey, plateau fraction=0.6)

  Emitted Faust DSP (abbreviated):
      alembic_dsp(n2) = n5, n6, n7, n10, n11
        with {
          n3  = 1.0 - n0;         // 1-shape (edge region size)
          n4  = float(n2 >= n3);  // gate: 1 in plateau
          n5  = select2(n4>0.5, n2/max(n3,0.0001), 1.0);                     // trapezoid
          n6  = select2(n4>0.5, 0.5*(1.0-cos(ma.PI*n2/max(n3,0.0001))), 1.0);// tukey
          n7  = select2(n4>0.5, 1.0/(1.0+exp(n3/max(n2,0.0001)+n3/min(n2-n3,-0.0001))), 1.0);
          ...
          n10 = exp(-pow(max(0.0,(1.0-n2)/max(n1,0.0001)), n2_power));       // gauss
          n11 = select2(float(n2>=n0)>0.5, 0.5*(1.0-cos(ma.PI*n2/max(n0,0.0001))), 1.0);
        };"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! window-parametric
  {:params {:shape {:range [0.0 1.0]  :default 0.6}
            :sigma {:range [0.01 1.0] :default 0.4}
            :power {:range [0.5 8.0]  :default 2.0}}}
  (let [t    (audio-in)
        shp  (param :shape)
        sig  (param :sigma)
        pow  (param :power)
        ; shared: 1-shape = edge-region size (for tukey / plancktaper / trapezoid)
        eps  (faust "1.0 - %{sh}" {:sh shp})
        gate (faust "float(%{tt} >= %{ep})" {:tt t :ep eps})
        ; trapezoid: linear ramp in edge region, flat at plateau
        trap (faust "select2(%{gg}>0.5,%{tt}/max(%{ep},0.0001),1.0)"
                    {:gg gate :tt t :ep eps})
        ; tukey: cosine taper in edge region, flat at plateau
        tuky (faust "select2(%{gg}>0.5,0.5*(1.0-cos(ma.PI*%{tt}/max(%{ep},0.0001))),1.0)"
                    {:gg gate :tt t :ep eps})
        ; plancktaper: C∞ taper — exp formula, both denominators clamped
        plnk (faust "select2(%{gg}>0.5,1.0/(1.0+exp(%{ep}/max(%{tt},0.0001)+%{ep}/min(%{tt}-%{ep},-0.0001))),1.0)"
                    {:gg gate :tt t :ep eps})
        ; gauss: generalized Gaussian, σ and power exposed as params
        ; note: write (0.0 - pow(...)) not -pow(...) — Faust rejects unary minus before POWFUN
        gaus (faust "exp(0.0 - pow(max(0.0,(1.0-%{tt})/max(%{sg},0.0001)),%{pw}))"
                    {:tt t :sg sig :pw pow})
        ; raisedcosine: cosine taper with shape as rolloff fraction (complement of tukey)
        ; taper at t < shape; plateau at t ≥ shape
        rcgx (faust "float(%{tt} >= %{sh})" {:tt t :sh shp})
        rcos (faust "select2(%{gg}>0.5,0.5*(1.0-cos(ma.PI*%{tt}/max(%{sh},0.0001))),1.0)"
                    {:gg rcgx :tt t :sh shp})]
    (output :trapezoid    trap)
    (output :tukey        tuky)
    (output :plancktaper  plnk)
    (output :gauss        gaus)
    (output :raisedcosine rcos)))
