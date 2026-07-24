; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.184-wavetable-1d
  "GSOT pp.265-268 — wavetable_1D.maxpat (Chapter 9: Navigating Waves of Data).

  '1D Wavetable Oscillator with Linear Interpolation'
  ----------------------------------------------------
  A wavetable oscillator stores a single cycle of a waveform in a lookup
  table and reads it using a phasor as the read pointer.  Chapter 9 builds
  on the basic phasor+table concept from ex.08 (phasor_basic_table_oscillator)
  by adding LINEAR INTERPOLATION between adjacent table entries.

  Why a table?
  ------------
  `os.osc(fc)` in Faust computes a mathematically exact sine every sample
  at the cost of a sin() call.  A wavetable precomputes the waveform and
  reads values by index, which is:
    — faster at runtime (table lookup + interpolation vs. transcendental fn)
    — applicable to ANY single-cycle waveform (not just mathematical shapes)
    — the foundation for multi-table (2D) synthesis (ex.187)

  The basic phasor+table oscillator (ex.08) uses nearest-neighbor lookup:
  the read index is rounded to an integer, giving the closest table entry.
  This produces quantisation artefacts audible as extra harmonics, especially
  at low table sizes or high playback frequencies.

  Linear interpolation
  --------------------
  For a fractional read position ph ∈ [i, i+1):

      s0 = table[i]        — sample at floor(ph)
      s1 = table[i+1]      — sample at ceil(ph)
      fr = ph − floor(ph)  — fractional part in [0, 1)
      out = s0 + fr × (s1 − s0)

  Linear interpolation eliminates the worst quantisation artefacts at the
  cost of one extra table read and a multiply-add per sample.  It is the
  minimum acceptable quality for a production wavetable oscillator.

  Higher-order interpolation (cubic, sinc) reduces artefacts further but
  is not shown in ex.184; ex.186/187 use bilinear (2D linear) interpolation.

  Table size and wrapping
  -----------------------
  Table size N = 1024 (2^10).  Power-of-two sizes allow wrapping via
  bitmask instead of modulo: `i & (N − 1)` is faster than `i % N` and
  avoids the cost of a division.

  The read index:

      ph = os.phasor(1, fc) × N     — fractional position in [0, N)
      i0 = int(ph) & (N − 1)        — integer floor, wrapped
      i1 = (i0 + 1) & (N − 1)       — next index, wrapped (handles 1023→0)
      fr = ph − float(int(ph))       — fractional part

  Aliasing consideration
  ----------------------
  Even with interpolation, a wavetable oscillator aliases when harmonics of
  the playback pitch exceed Nyquist.  A 1D sine table at any pitch is
  alias-free (only one harmonic), but a wavetable containing a sawtooth
  with 32 harmonics at fc=1000 Hz would alias for harmonics above n=22
  (at 44100 Hz SR).

  The typical solution is band-limited wavetables: pre-compute a separate
  table for each octave with only the harmonics that fit below Nyquist.
  Chapter 9 focuses on interpolation mechanics; band-limiting is discussed
  separately.

  rdtable in Faust
  ----------------
  `rdtable(n, init, ridx)`:
  — n:    table size in samples (integer, compile-time constant)
  — init: initialisation signal; evaluated at indices 0..n-1 at startup
  — ridx: integer read index (runtime signal)

  `os.sinwaveform(N)` generates sine values sin(2π × k / N) for k=0..N-1:
      sinwaveform(tablesize) = float(time)*(2*π)/float(tablesize) : sin
          with { time = 0,1:+~_; }

  The local counter `0,1:+~_` runs from 0 to N-1 during initialisation
  and is NOT shared with the runtime sample counter — each rdtable gets
  its own init counter.

  os.sinwaveform is called twice (for i0 and i1) but the Faust compiler
  recognises the shared init expression and allocates only one table.

  Parameters
  ----------
  :fc — playback frequency in Hz (20–4000; default 220)

  Audio inputs / Outputs
  ----------------------
  (no audio input — self-contained wavetable oscillator)
    :out — linearly interpolated 1D wavetable output at :fc Hz"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! wavetable-1d
  {:params {:fc {:range [20.0 4000.0] :default 220.0}}}
  (let [fc  (param :fc)
        out (faust
              "s0+fr*(s1-s0)
               with {
                 N = 1024;
                 ph = os.phasor(1,%fc)*float(N);
                 i0 = int(ph)&(N-1);
                 i1 = (i0+1)&(N-1);
                 fr = ph-float(int(ph));
                 s0 = rdtable(N,os.sinwaveform(N),i0);
                 s1 = rdtable(N,os.sinwaveform(N),i1);
               }"
              {:fc fc})]
    (output :out out)))
