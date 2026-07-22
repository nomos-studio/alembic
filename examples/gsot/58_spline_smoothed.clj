; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.58-spline-smoothed
  "GSOT p.95 — go.shift.spline6.gendsp.

  'Smooth stepped random — spline interpolation' (Chapter 4)
  -----------------------------------------------------------
  Extends random_smoothed (example 57) by replacing linear interpolation with
  4-point Catmull-Rom spline interpolation, using a 6-stage shift register for
  control points.  The spline gives C1-continuous output: position AND velocity
  are continuous at trigger boundaries.

  Two audio inputs:
    trig  — trigger pulse (e.g. from go.ramp2trig, example 14)
    phase — phasor phase [0, 1] within the current step

  Shift register — 6 stages, newest to oldest
  ---------------------------------------------
  Each trigger shifts all values by one stage:

      p5 ← new noise    (newest)
      p4 ← old p5
      p3 ← old p4
      p2 ← old p3
      p1 ← old p2
      p0 ← old p1       (oldest)

  Faust implementation: each stage is track-hold(previous_stage@1, trig).
  Between triggers, each stage is constant, so stage@1 = stage value from
  the previous trigger epoch.  When trig fires, each stage latches the
  value the upstream stage held BEFORE this trigger.

  4-point Catmull-Rom — interpolates p2 → p3
  -------------------------------------------
  Uses p1 and p4 to estimate tangents at the segment endpoints:

      m2 = (p3 − p1) / 2        tangent at p2 (start of current segment)
      m3 = (p4 − p2) / 2        tangent at p3 (end of current segment)

  Hermite basis functions (t ∈ [0, 1]):
      h00(t) =  2t³ − 3t² + 1   blends p2
      h10(t) =   t³ − 2t² + t   blends m2
      h01(t) = −2t³ + 3t²       blends p3
      h11(t) =   t³ −  t²       blends m3

  out = h00·p2 + h10·m2 + h01·p3 + h11·m3

  At t=0: out = p2, ṫ = m2  (continuous with previous segment)
  At t=1: out = p3, ṫ = m3  (continuous into next segment)

  The outer two stages p0 and p5 are not directly used in the formula but
  are kept to allow p0 to seed p1's tangent and p5 to seed p4's tangent
  in a future 6-point extension.

  Latency: the output tracks the segment p2→p3, so it is 2 triggers behind
  the newest value.  This is the standard trade-off for look-around tangents.

  Emitted Faust DSP structure (abbreviated):
      n0  = trig, n1 = phase, n2 = lo, n3 = hi
      n4  = scaled noise
      n5  = p5 = track-hold(n4, trig)          newest
      n6  = n5@1;  n7 = track-hold(n6, trig)   p4
      n8  = n7@1;  n9 = track-hold(n8, trig)   p3
      n10 = n9@1;  n11 = track-hold(n10, trig) p2
      n12 = n11@1; n13 = track-hold(n12, trig) p1
      n14 = n13@1; n15 = track-hold(n14, trig) p0
      n16 = t² = n1*n1
      n17 = t³ = n16*n1
      n18 = m2 = (n9-n13)*0.5
      n19 = m3 = (n7-n11)*0.5
      n20 = (2*n17-3*n16+1)*n11 + (n17-2*n16+n1)*n18
           + (-2*n17+3*n16)*n9  + (n17-n16)*n19"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! spline-smoothed
  {:params {:lo {:range [-1.0 1.0] :default -1.0}
            :hi {:range [-1.0 1.0] :default  1.0}}}
  (let [trig    (audio-in)
        phase   (audio-in)
        n       (faust "no.noise" {})
        lo      (param :lo)
        hi      (param :hi)
        scaled  (faust "%lo+(%hi-%lo)*0.5*(%nn+1.0)" {:lo lo :hi hi :nn n})
        ; ── 6-stage shift register (p5 = newest, p0 = oldest) ────────────────
        p5 (track-hold scaled trig)
        p4 (track-hold (faust "%nv@1" {:nv p5}) trig)
        p3 (track-hold (faust "%nv@1" {:nv p4}) trig)
        p2 (track-hold (faust "%nv@1" {:nv p3}) trig)
        p1 (track-hold (faust "%nv@1" {:nv p2}) trig)
        _p0 (track-hold (faust "%nv@1" {:nv p1}) trig)
        ; ── Catmull-Rom: interpolate p2 → p3 with tangents from p1 and p4 ───
        t2  (faust "%ph*%ph"          {:ph phase})
        t3  (faust "%t2*%ph"          {:t2 t2 :ph phase})
        m2  (faust "(%p3-%p1)*0.5"   {:p3 p3 :p1 p1})
        m3  (faust "(%p4-%p2)*0.5"   {:p4 p4 :p2 p2})
        out (faust "(2.0*%t3-3.0*%t2+1.0)*%p2+(%t3-2.0*%t2+%ph)*%m2+(-2.0*%t3+3.0*%t2)*%p3+(%t3-%t2)*%m3"
                   {:t3 t3 :t2 t2 :ph phase :p2 p2 :p3 p3 :m2 m2 :m3 m3})]
    (output out)))
