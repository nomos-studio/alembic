; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.79-go-shiftregister8
  "GSOT pp.127 — go.shiftregister8.gendsp.

  8-stage shift register.  Same pattern as go.shiftregister2 (example 78)
  extended to 8 stages.  Each trigger shifts all values one position toward
  higher-numbered stages; the new cv-in value enters at stage 0.

  Signal flow
  -----------
  Each stage Si is a track-hold whose input is the PREVIOUS stage's
  1-sample-delayed value (S_{i-1}@1).  Because the previous stage only
  changes on trigger events, the @1 delay is equivalent to 'the value
  that stage held during the preceding trigger event':

      s0[n] = if trig: cv-in[n]     else s0[n-1]
      s1[n] = if trig: s0[n-1]      else s1[n-1]
      s2[n] = if trig: s1[n-1]      else s2[n-1]
      ...
      s7[n] = if trig: s6[n-1]      else s7[n-1]

  After 8 triggers with inputs [a,b,c,d,e,f,g,h] (h = most recent):
      s0=h, s1=g, s2=f, s3=e, s4=d, s5=c, s6=b, s7=a

  Temporal canon
  --------------
  All 8 stages are available simultaneously on every sample.  Connect them
  to 8 separate voices (oscillators, VCAs, etc.) sharing the same clock:

    s0 plays the melody NOW
    s4 plays the same melody 4 beats ago  →  voice 2 enters 4 beats late
    s7 plays 7 beats ago                  →  voice 3 enters 7 beats late

  This is a 3-voice shift-register canon without any additional sequencer
  logic.  The imitation interval is set purely by which stage index is
  used as the voice's pitch source.

  Evolving sequences
  ------------------
  When the input cv-in changes continuously (random source, another
  sequencer, performer CV), the register is a 'rolling memory' of the last
  8 input values.  All voices simultaneously play different points in the
  recent history.  Feeding the latched-sequencer (example 77) into cv-in
  combines both approaches: the latch defines a repeating phrase; the shift
  register layers it in canon.

  Audio inputs
  ------------
  audio-in 0: trig  — clock
  audio-in 1: cv-in — signal to shift in at stage 0

  Emitted Faust DSP:
      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1) = n_s0,n_s1,n_s2,n_s3,n_s4,n_s5,n_s6,n_s7
        with {
          n_s0 = (select2(n0>0.5,_,n1)~_);
          n_s1 = (select2(n0>0.5,_,n_s0@1)~_);
          n_s2 = (select2(n0>0.5,_,n_s1@1)~_);
          n_s3 = (select2(n0>0.5,_,n_s2@1)~_);
          n_s4 = (select2(n0>0.5,_,n_s3@1)~_);
          n_s5 = (select2(n0>0.5,_,n_s4@1)~_);
          n_s6 = (select2(n0>0.5,_,n_s5@1)~_);
          n_s7 = (select2(n0>0.5,_,n_s6@1)~_);
        };
      process = alembic_dsp;

  n0 = trig, n1 = cv-in"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! go-shiftregister8
  {}
  (let [trig  (audio-in)
        cv-in (audio-in)
        s0    (track-hold cv-in trig)
        s1    (track-hold (faust "%{pv}@1" {:pv s0}) trig)
        s2    (track-hold (faust "%{pv}@1" {:pv s1}) trig)
        s3    (track-hold (faust "%{pv}@1" {:pv s2}) trig)
        s4    (track-hold (faust "%{pv}@1" {:pv s3}) trig)
        s5    (track-hold (faust "%{pv}@1" {:pv s4}) trig)
        s6    (track-hold (faust "%{pv}@1" {:pv s5}) trig)
        s7    (track-hold (faust "%{pv}@1" {:pv s6}) trig)]
    (output :s0 s0)
    (output :s1 s1)
    (output :s2 s2)
    (output :s3 s3)
    (output :s4 s4)
    (output :s5 s5)
    (output :s6 s6)
    (output :s7 s7)))
