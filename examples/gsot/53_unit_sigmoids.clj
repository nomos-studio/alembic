; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.53-unit-sigmoids
  "GSOT p.88 — go.unit.logistic, go.unit.sigmoid2, go.unit.gundermann,
               go.unit.ata, go.unit.softclip.

  'Normalized sigmoids as unit shapers' (Chapter 3) — full set
  -------------------------------------------------------------
  Extends the pattern from go.unit.tanh.gendsp (example 52) to all five
  remaining sigmoid flavors from go.unit.*.  Each takes x ∈ [0,1] and
  returns y ∈ [0,1] using the same bipolarize → shape → normalize pipeline:

      u   = k · (2x − 1)
      out = (f(u) + f(k)) / (2 · f(k))

  where f is the chosen odd sigmoid.  Odd symmetry guarantees f(−k) = −f(k),
  so the denominator always simplifies to 2·f(k).

  Per-sigmoid normalization denominators
  ---------------------------------------
  go.unit.logistic   f(v) = 2/(1+exp(−v))−1
      f(k) = 2/(1+exp(−k))−1                     (one exp call)

  go.unit.sigmoid2   f(v) = v/(1+|v|)
      f(k) = k/(1+k)  [closed form, k>0]          (no exp/trig)

  go.unit.gundermann f(v) = (4/π)·atan(exp(v))−1
      f(k) = (4/π)·atan(exp(k))−1                 (one exp + one atan call)

  go.unit.ata        f(v) = (2/π)·atan(v)
      f(k) = (2/π)·atan(k)  [closed form]         (one atan call)

  go.unit.softclip   f(v) = 1.5v−0.5v³  (|v|≤1), ±1 otherwise
      f(k) = min(1, 1.5k−0.5k³)  [= 1 when k≥1]
      softclip reaches ±1 exactly at |u|=1; for k≥1 (default range),
      f(k)=1 and normalization is (f(u)+1)/2.

  sigmoid2 closed-form advantage
  --------------------------------
  f(k) = k/(1+k) avoids any exp or trig in the normalization denominator,
  making go.unit.sigmoid2 the most efficient unit shaper of the set.
  Full output: u/(1+|u|) normalized = ((u/(1+|u|)) + k/(1+k)) / (2k/(1+k))
             = (1+k)·(u/(1+|u|)+k/(1+k)) / (2k)
  Simplified: (u·(1+k)/((1+|u|)·2k)) + 1/2  (not used below; kept as two nodes)

  Note on 'go.unit.ata'
  ----------------------
  The name 'ata' (not 'atan') appears in the GSOT abstraction library.
  Likely a typo or truncation for 'atan'; the implementation is the standard
  arctan sigmoid (2/π)·atan(k·(2x−1)), normalized.

  Emitted Faust DSP (abbreviated — 17 nodes):
      import(\"stdfaust.lib\");

      n0 = hslider(\"drive\", 2.0, 0.5, 8.0, 0.00075);

      alembic_dsp(n1) = n_log, n_sig2, n_gude, n_ata, n_soft
        with {
          n2  = 2.0*n1-1.0;               // bipolarize
          n3  = n0*n2;                     // u = k(2x-1)
          // logistic
          n4  = 2.0/(1.0+exp(0.0-n3))-1.0;            // f(u)
          n5  = 2.0/(1.0+exp(0.0-n0))-1.0;            // f(k)
          n6  = (n4+n5)/(2.0*max(n5,0.0001));
          // sigmoid2
          n7  = n3/(1.0+abs(n3));
          n8  = n0/(1.0+n0);
          n9  = (n7+n8)/(2.0*max(n8,0.0001));
          // gudermann
          n10 = (4.0/ma.PI)*atan(exp(n3))-1.0;
          n11 = (4.0/ma.PI)*atan(exp(n0))-1.0;
          n12 = (n10+n11)/(2.0*max(n11,0.0001));
          // ata (atan)
          n13 = (2.0/ma.PI)*atan(n3);
          n14 = (2.0/ma.PI)*atan(n0);
          n15 = (n13+n14)/(2.0*max(n14,0.0001));
          // softclip
          n16 = select2(float(abs(n3)>1.0)>0.5,1.5*n3-0.5*n3*n3*n3,max(-1.0,min(1.0,n3)));
          n17 = min(1.0,1.5*n0-0.5*n0*n0*n0);         // f(k), =1 for k>=1
          n18 = (n16+n17)/(2.0*max(n17,0.0001));
        };"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! unit-sigmoids
  {:params {:drive {:range [0.5 8.0] :default 2.0}}}
  (let [x    (audio-in)
        k    (param :drive)
        bx   (faust "2.0*%xx-1.0" {:xx x})
        u    (faust "%kk*%bx"     {:kk k :bx bx})
        ; ── go.unit.logistic ────────────────────────────────────────────────
        log-u  (faust "2.0/(1.0+exp(0.0-%uu))-1.0" {:uu u})
        log-k  (faust "2.0/(1.0+exp(0.0-%kk))-1.0" {:kk k})
        log-out (faust "(%fu+%fk)/(2.0*max(%fk,0.0001))" {:fu log-u :fk log-k})
        ; ── go.unit.sigmoid2 (rational softsign) ────────────────────────────
        ; f(k) = k/(1+k) — closed form, k>0 guaranteed by drive range
        sig2-u  (faust "%uu/(1.0+abs(%uu))" {:uu u})
        sig2-k  (faust "%kk/(1.0+%kk)"      {:kk k})
        sig2-out (faust "(%fu+%fk)/(2.0*max(%fk,0.0001))" {:fu sig2-u :fk sig2-k})
        ; ── go.unit.gundermann ──────────────────────────────────────────────
        gude-u  (faust "(4.0/ma.PI)*atan(exp(%uu))-1.0" {:uu u})
        gude-k  (faust "(4.0/ma.PI)*atan(exp(%kk))-1.0" {:kk k})
        gude-out (faust "(%fu+%fk)/(2.0*max(%fk,0.0001))" {:fu gude-u :fk gude-k})
        ; ── go.unit.ata (atan) ───────────────────────────────────────────────
        ; f(k) = (2/π)*atan(k) — closed form
        ata-u  (faust "(2.0/ma.PI)*atan(%uu)" {:uu u})
        ata-k  (faust "(2.0/ma.PI)*atan(%kk)" {:kk k})
        ata-out (faust "(%fu+%fk)/(2.0*max(%fk,0.0001))" {:fu ata-u :fk ata-k})
        ; ── go.unit.softclip ─────────────────────────────────────────────────
        ; f(k) = min(1, 1.5k-0.5k³) — equals 1 for k>=1
        soft-u  (faust "select2(float(abs(%uu)>1.0)>0.5,1.5*%uu-0.5*%uu*%uu*%uu,max(-1.0,min(1.0,%uu)))"
                        {:uu u})
        soft-k  (faust "min(1.0,1.5*%kk-0.5*%kk*%kk*%kk)" {:kk k})
        soft-out (faust "(%fu+%fk)/(2.0*max(%fk,0.0001))" {:fu soft-u :fk soft-k})]
    (output :logistic  log-out)
    (output :sigmoid2  sig2-out)
    (output :gundermann gude-out)
    (output :ata       ata-out)
    (output :softclip  soft-out)))
