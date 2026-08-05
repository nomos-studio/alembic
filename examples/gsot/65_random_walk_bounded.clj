; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.65-random-walk-bounded
  "GSOT p.103 — random_walk_bounded.maxpat.

  'Bounded random walks' (Chapter 4)
  ------------------------------------
  Extends the unbounded random walk (example 64) by reflecting the position
  off both boundaries [lo, hi].  Every step stays within the range; the walk
  bounces rather than drifting out of bounds.

  Bounding strategy: fold (infinite reflection)
  -----------------------------------------------
  Clamping (max/min) causes the walk to 'stick' at the boundary until a
  step of the opposite sign pulls it free.  Reflection is more natural:
  the walk bounces back exactly as much as it would have overshot.

  Fold formula — maps any real value to [lo, hi] with reflections:
      r    = hi − lo                     range width
      y    = ma.decimal((x − lo) / 2r) · 2r   period-2r sawtooth on x
      fold = lo + |y − r|                triangular wave, values ∈ [lo, hi]

  ma.decimal(x) = x − floor(x), the positive fractional part.  It gives
  0.85 for x = −0.15 (floor = −1), so negative overshoots are handled
  correctly.

  The fold is embedded inside the feedback (~ _) expression so the
  Faust feedback register holds the already-folded position.  Each new
  step starts from within [lo, hi], keeping the internal state bounded
  for all time.

  Signal flow
  -----------
  audio-in: trig    — trigger pulse (controls step rate)
  params:   :step   — maximum step magnitude
            :lo     — lower boundary
            :hi     — upper boundary

      rand-inc  = step × no.noise               ∈ [−step, step]
      inc-held  = track-hold(rand-inc, trig)    latch on trigger
      raw       = select2(trig>0.5, _, _+inc)   _ is folded prev-position
      r         = hi − lo
      y         = ma.decimal((raw−lo)/(2·r))·2·r
      position  = (lo + |y − r|) ~ _            fold inside feedback

  After each trigger, position ∈ [lo, hi] exactly.  Between triggers it
  holds.  The walk is ergodic over [lo, hi] for any nonzero step.

  Emitted Faust DSP (abbreviated):
      import(\"stdfaust.lib\");

      alembic_dsp(n0) = n4
        with {
          n1 = hslider(\"step\", 0.1, 0.0, 1.0, 0.0001);
          n2 = hslider(\"lo\",  -1.0,-1.0, 1.0, 0.0002);
          n3 = hslider(\"hi\",   1.0,-1.0, 1.0, 0.0002);
          n5 = (select2(n0>0.5,_,n1*no.noise)~_);    // inc-held
          n4 = (
            (n2+abs(
              ma.decimal(
                (select2(n0>0.5,_,_+n5)-n2) /
                (2.0*max(n3-n2,0.0001))
              ) * (2.0*max(n3-n2,0.0001))
              - (n3-n2)
            ))
          ~_);
        };
      process = alembic_dsp;"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! random-walk-bounded
  {:params {:step {:range [0.0 1.0] :default 0.1}
            :lo   {:range [-1.0 1.0] :default -1.0}
            :hi   {:range [-1.0 1.0] :default  1.0}}}
  (let [trig     (audio-in)
        step     (param :step)
        lo       (param :lo)
        hi       (param :hi)
        rand-inc (faust "%{st}*no.noise" {:st step})
        inc-held (track-hold rand-inc trig)
        ; Fold embedded in feedback: _ = previous folded position
        ; raw = _+inc on trigger, _ on hold
        ; fold maps raw to [lo,hi] via triangular wave
        position (faust "((%{lo}+abs(ma.decimal((select2(%{tr}>0.5,_,_+%{ih})-%{lo})/(2.0*max(%{hi}-%{lo},0.0001)))*(2.0*max(%{hi}-%{lo},0.0001))-(%{hi}-%{lo})))~_)"
                        {:lo lo :hi hi :tr trig :ih inc-held})]
    (output position)))
