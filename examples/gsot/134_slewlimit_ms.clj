; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.134-slewlimit-ms
  "GSOT p.195 — go.slewlimit.ms (Chapter 6).

  'Slew Limiting — Millisecond-Parameterized'
  ---------------------------------------------
  A usability wrapper over go.slewlimit (ex.132) that takes the slew time in
  milliseconds rather than a raw per-sample rate.  The conversion:

      slew_per_sample = full_scale / (time_ms × SR / 1000)
                      = 1.0 / (time_ms × SR / 1000)

  For a [−1, 1] signal with full-scale change of 2.0:
      slew_per_sample = 2.0 / (time_ms × SR / 1000)

  But GSOT normalises to a 0–1 control signal convention where full scale = 1.0:
      slew_per_sample = 1.0 / (time_ms × SR / 1000)

  This makes `:ms` directly readable: \":ms 10\" means the output takes at most
  10 milliseconds to traverse the full control range [0, 1].

  Conversion formula (Faust):
      slew_per_sample = 1000.0 / (%ms × ma.SR)
      out = (_ + max(−rate, min(rate, in − _))) ~ _

  A guard of max(1.0, ms×SR/1000) prevents division by zero and clips the
  per-sample rate to ≤ 1.0 (instant) when time_ms → 0.

  Relationship to go.slewlimit (ex.132)
  ---------------------------------------
  go.slewlimit  (ex.132)   raw per-sample rate; direct and precise
  go.slewlimit.ms (ex.134) millisecond time; readable but sample-rate dependent

  Both are equivalent: go.slewlimit.ms simply pre-divides the rate parameter.

  Parameters
  ----------
  :ms — time in milliseconds for full-range transition (0.1–5000; default 10)

  Audio inputs / Outputs
  ----------------------
  in: signal to rate-limit  →  :out: slew-limited output"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! slewlimit-ms
  {:params {:ms {:range [0.1 5000.0] :default 10.0}}}
  (let [in   (audio-in)
        ms   (param :ms)
        ; per-sample rate: 1.0 / (time_ms × SR / 1000)
        ; guarded: max(1,ms×SR/1000) prevents division by zero and clips to ≤1
        rate (faust "1000.0/(max(1.0,%ms*ma.SR/1000.0))" {:ms ms})
        out  (faust "(_+max(-%rt,min(%rt,%in-_)))~_" {:rt rate :in in})]
    (output :out out)))
