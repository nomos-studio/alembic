; SPDX-License-Identifier: EPL-2.0
(ns alembic.gsot-test
  "Validation suite for Alembic GSOT examples — Chapters 2-3, pp.22-58.

  Every example must:
    1. emit-faust — produce a non-empty Faust DSP string
    2. validate   — compile through `faust -lang cpp` without errors

  This is the ground truth for the examples.  If these tests pass, the
  emitted Faust is real and the examples are not hand-waving."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [alembic.emit :refer [emit-faust]]
            [alembic.compile :refer [validate]]
            ;; GSOT example namespaces (loaded from examples/gsot/ via dev source path)
            [examples.gsot.04-counter-play-a-buffer]
            [examples.gsot.05-counter-and-wrap]
            [examples.gsot.06-counter-and-wrap-buffer]
            [examples.gsot.07-phasor-counter]
            [examples.gsot.08-phasor-basic-table-oscillator]
            [examples.gsot.09-phasor-bpm]
            [examples.gsot.10-phasor-loop-processing]
            [examples.gsot.11-phasor-beat-slicer]
            [examples.gsot.12-ramp-from-bpm]
            [examples.gsot.13-ramp-to-steps]
            [examples.gsot.14-ramp-to-trig]
            [examples.gsot.15-ramp-phase-shift]
            [examples.gsot.16-ramp-rotate]
            [examples.gsot.17-ramp-to-slope]
            [examples.gsot.18-ramp-to-freq]
            [examples.gsot.19-ramp-to-trig-gendsp]
            [examples.gsot.20-ramp-div-simple]
            [examples.gsot.21-ramp-div]
            [examples.gsot.22-ramp-div-musical]
            [examples.gsot.23-ramp-bursts]
            [examples.gsot.24-ramp-bursts-to-trigs]
            [examples.gsot.25-ramp-bursts-shaped]
            ;; Chapter 3 — Unit shaping (p.58)
            [examples.gsot.26-unit-triangle]
            [examples.gsot.27-unit-trapezoid]
            [examples.gsot.28-unit-kink]
            [examples.gsot.29-unit-lfo]
            [examples.gsot.30-unit-pow]
            [examples.gsot.31-unit-arc]
            [examples.gsot.32-unit-cubic]
            [examples.gsot.33-unit-logistic]
            [examples.gsot.34-unit-ease-exp]
            [examples.gsot.35-unit-welch]
            [examples.gsot.36-unit-tukey]
            [examples.gsot.37-ramp-swing]
            ;; Chapter 3 — From ramps to LFOs (p.69)
            [examples.gsot.38-lfo-multi]
            ;; Chapter 3 — Smooth stepped interpolation (p.70)
            [examples.gsot.39-smooth-stepped]
            [examples.gsot.40-smooth-stepped-shaped]
            [examples.gsot.41-interpolating-lfo]
            ;; Chapter 3 — Glides and portamento (p.76)
            [examples.gsot.42-portamento]
            [examples.gsot.43-portamento-shaped]
            ;; Chapter 3 — Easing functions (p.77)
            [examples.gsot.44-easing-multi]
            ;; Chapter 3 — Window envelope functions (p.78)
            [examples.gsot.45-window-fixed]
            [examples.gsot.46-window-parametric]
            ;; Chapter 3 — Waveshaping bipolar signals (p.79)
            [examples.gsot.47-bipolar-waveshaping]
            ;; Chapter 3 — Audio waveshaping / polynomial shapers (p.81)
            [examples.gsot.48-chebyshev-waveshaping]
            ;; Chapter 3 — Sigmoid waveshaping (p.84)
            [examples.gsot.49-sigmoid-waveshaping]
            [examples.gsot.50-sigmoid-gsot]
            ;; Chapter 3 — Enveloped sigmoid waveshaping (p.86)
            [examples.gsot.51-sigmoid-enveloped]
            ;; Chapter 3 — Normalized sigmoids as unit shapers (p.87)
            [examples.gsot.52-unit-tanh]
            ;; Chapter 3 — Full set of normalized sigmoid unit shapers (p.88)
            [examples.gsot.53-unit-sigmoids]
            ;; Chapter 4 — Feel the noise (p.91)
            [examples.gsot.54-noise-basic]
            ;; Chapter 4 — Random range and random steps (p.93)
            [examples.gsot.55-random-range]
            [examples.gsot.56-random-steps]
            ;; Chapter 4 — Smooth stepped random (p.94)
            [examples.gsot.57-random-smoothed]
            ;; Chapter 4 — Spline interpolated random steps (p.95)
            [examples.gsot.58-spline-smoothed]
            ;; Chapter 4 — Probability gate (p.96)
            [examples.gsot.59-random-chance]
            ;; Chapter 4 — Bernoulli gate (p.97)
            [examples.gsot.60-bernoulli-gate]
            ;; Chapter 4 — Random periods (p.98)
            [examples.gsot.61-random-periods]
            ;; Chapter 4 — Normal distribution noise (p.100)
            [examples.gsot.62-normal-noise]
            ;; Chapter 4 — Uniform vs normal comparison (p.101)
            [examples.gsot.63-random-distributions]
            ;; Chapter 4 — Random walk (p.101-102)
            [examples.gsot.64-random-walk]
            ;; Chapter 4 — Bounded random walk with fold reflection (p.103)
            [examples.gsot.65-random-walk-bounded]
            ;; Chapter 4 — Random integers / quantized random (p.103)
            [examples.gsot.66-random-integer]
            ;; Chapter 4 — Urn model approximation (pp.104-109)
            [examples.gsot.67-random-urn]
            ;; Chapter 4 — Flexible urn: manual reshuffle + no-repeat (pp.110-111)
            [examples.gsot.68-random-urn-flexible]
            ;; Chapter 4 — Lorenz attractor as chaotic DSP source (pp.112-114)
            [examples.gsot.69-chaos-lorenz]
            ;; Chapter 4 — Running min/max tracker (pp.115-116)
            [examples.gsot.70-limits]
            ;; Chapter 4 — Auto-normalise using tracked limits (pp.116)
            [examples.gsot.71-autolimit]
            ;; Chapter 4 — Abstract chaos step; covers Lorenz/Lu-Chen family (pp.117)
            [examples.gsot.72-go-chaos]
            ;; Chapter 4 — Lu-Chen attractor, hardwired go.chaos variant (pp.118)
            [examples.gsot.73-go-chaos-liu-chen]
            ;; Chapter 4 — Chaotic tempo clock: logistic map drives phasor rate (pp.119-120)
            [examples.gsot.74-chaos-tempo-nonrobotic]
            ;; Chapter 4 — Lorenz with audio injected into x equation (pp.121)
            [examples.gsot.75-chaos-lorenz-audioinjection]
            ;; Chapter 5 — 8-step pitch sequencer with bitmask gate logic (pp.123-125)
            [examples.gsot.76-mixer-sequencer]
            ;; Chapter 5 — Step sequencer that captures its sequence from cv-in via S&H (pp.126)
            [examples.gsot.77-latched-sequencer]
            ;; Chapter 5 — 2-stage shift register subpatch (pp.127)
            [examples.gsot.78-go-shiftregister2]
            ;; Chapter 5 — 8-stage shift register subpatch; temporal canon (pp.127)
            [examples.gsot.79-go-shiftregister8]
            ;; Chapter 5 — Shift register canon with selectable imitation interval (pp.128)
            [examples.gsot.80-shift-register]
            ;; Chapter 5 — Binary shift register; Bernoulli input with fixed weight (pp.129-130)
            [examples.gsot.81-shift-register-weighted]
            ;; Chapter 5 — Binary shift register; per-trigger randomised weight (pp.130)
            [examples.gsot.82-shift-register-weighted-random]
            ;; Chapter 5 — Evolving loops: LFSR XOR feedback weighted vs random (pp.131-132)
            [examples.gsot.83-shift-register-weighted-xor]
            ;; Chapter 5 — Binary decoding: 8 shift register bits → integer index (pp.134-135)
            [examples.gsot.84-binary-decode]
            ;; Chapter 5 — Integers as patterns: counter reads integer bitmask into shift register (pp.136-139)
            [examples.gsot.85-shift-register-integer]
            ;; Chapter 5 — Bit primitives: unpack, shift, wrap, extract (pp.140-141)
            [examples.gsot.86-bit-unpack8]
            [examples.gsot.87-bit-shift]
            [examples.gsot.88-bit-wrap]
            [examples.gsot.89-bit-extract]
            ;; Chapter 5 — Rotating a bit sequence: trigger-driven accumulating rotation (pp.141-142)
            [examples.gsot.90-bit-rotate]
            ;; Chapter 5 — Euclidean rhythms via digitized ratio: floor(s*N/K) gate (pp.142-148)
            [examples.gsot.91-euclidean-rhythms]
            ;; Chapter 5 — Euclidean ratchets: R gate pulses per beat via countdown register (pp.149)
            [examples.gsot.92-euclidean-ratchets]
            ;; Chapter 5 — Euclidean LFO: fractional digitized ratio as sawtooth (pp.149)
            [examples.gsot.93-euclidean-lfo]
            ;; Chapter 5 — Pitch spaces: integer index → chromatic MIDI → Hz (pp.150-151)
            [examples.gsot.94-pitch]
            ;; Chapter 5 — Quantization: float degree → diatonic major scale → Hz (pp.151-152)
            [examples.gsot.95-pitch-quantized]
            ;; Chapter 5 — Euclidean scales: inverse digitized ratio maps N-note maximally-even scale (pp.153-154)
            [examples.gsot.96-quantizing-pitch]
            ;; Chapter 5 — Smooth-stepped quantization: lerp between hard floor and continuous ratio (pp.155-157)
            [examples.gsot.97-quantizing-pitch-smoothed]
            ;; Chapter 5 — Quantization as timbral shape: phasor → Euclidean staircase waveform (pp.157)
            [examples.gsot.98-quantizing-timbre]
            ;; Chapter 5 — Audio bitcrusher: audio [-1,1] → N-level Euclidean quantisation (pp.158)
            [examples.gsot.99-quantizing-audio-bitcrush]
            ;; Chapter 6 — One-pole lowpass filter: (1-a)*x[n] + a*y[n-1]; balance of time (pp.159-163)
            [examples.gsot.100-onepole-basic]
            ;; Chapter 6 — Vactrol LPG subpatch: asymmetric env follower drives one-pole coeff and VCA (pp.169-170)
            [examples.gsot.101-vactrol]
            ;; Chapter 6 — Lowpass gate: complete LPG patch; gate + audio → vactrol → gated filtered output (pp.171)
            [examples.gsot.102-lowpass-gate]
            ;; Chapter 6 — One-pole LP, Hz parameterization: a=exp(-2π*fc/SR); two-node coefficient+filter (pp.173)
            [examples.gsot.103-onepole-basic-hz]
            ;; Chapter 6 — Allpass filter: flat amplitude, phase-shifting; x[n-1] via @1, y[n-1] via ~_ (pp.175)
            [examples.gsot.104-allpass]
            ;; Chapter 6 — 4-stage phaser: 4 allpass in series + dry/wet mix; 2 notches per sweep (pp.176)
            [examples.gsot.105-phaser-4stage]
            ;; Chapter 6 — 8-stage phaser: 8 allpass in series + dry/wet mix; 4 notches per sweep (pp.177)
            [examples.gsot.106-phaser-8stage]
            ;; Chapter 6 — allpass Hz: bilinear transform coefficient from cutoff frequency (pp.178)
            [examples.gsot.107-allpass-hz]
            ;; Chapter 6 — biquad: second-order IIR, Direct Form II, 5 coefficients (pp.178-180)
            [examples.gsot.108-biquad]
            ;; Chapter 6 — biquad coefficient patches: 8 typed filters + type-selector (pp.181)
            [examples.gsot.109-biquad-lp]
            [examples.gsot.110-biquad-hp]
            [examples.gsot.111-biquad-bp]
            [examples.gsot.112-biquad-res]
            [examples.gsot.113-biquad-np]
            [examples.gsot.114-biquad-ap]
            [examples.gsot.115-biquad-ls]
            [examples.gsot.116-biquad-hs]
            [examples.gsot.117-biquad-coeffs]
            ;; Chapter 6 — 4th-order biquad cascades + morphing cascade (pp.182-183)
            [examples.gsot.118-biquad4-lp]
            [examples.gsot.119-biquad4-hp]
            [examples.gsot.120-biquad4-bp]
            [examples.gsot.121-biquad4-res]
            [examples.gsot.122-biquad4-np]
            [examples.gsot.123-biquad4-ap]
            [examples.gsot.124-biquad4-ls]
            [examples.gsot.125-biquad4-hs]
            [examples.gsot.126-morphing-biquad-cascade]
            ;; Chapter 6 — trapezoidal one-pole: bilinear-transform, exact -3dB at fc (pp.184-188)
            [examples.gsot.127-onepole-hz]
            ;; Chapter 6 — ZDF state variable filter: coefficients + full SVF body (pp.188-190)
            [examples.gsot.128-svf-coeffs]
            [examples.gsot.129-svf]
            ;; Chapter 6 — Crossover filters: 1st-order complement pair + SVF-based split (pp.191-192)
            [examples.gsot.130-crossover-simple]
            [examples.gsot.131-crossover]
            ;; Chapter 6 — Slew limiting: symmetric, asymmetric, millisecond-parameterized (pp.193-195)
            [examples.gsot.132-slewlimit]
            [examples.gsot.133-slewlimit2]
            [examples.gsot.134-slewlimit-ms]
            ;; Chapter 6 — Lag generator / line: one-pole IIR parameterized in samples and ms (p.196)
            [examples.gsot.135-line-samples]
            [examples.gsot.136-line-ms]
            ;; Chapter 7 — Feedforward delay: circular buffer, comb filter intro (pp.197-199)
            [examples.gsot.137-delay-feedforward-basic]
            ;; Chapter 7 — Feedback delay: IIR echo loop, decaying echoes, comb peak sharpening (p.200)
            [examples.gsot.138-delay-basic-feedback]
            ;; Chapter 7 — Decay-time reparameterization: fb = pow(0.001, delay/decay) (pp.201-202)
            [examples.gsot.139-delay-feedback-decaytime]
            ;; Chapter 7 — Filtered feedback: one-pole LP in loop; high freqs decay faster (pp.202-203)
            [examples.gsot.140-delay-feedback-filtered]
            ;; Chapter 7 — DC blocking in filtered feedback loop; zero at z=1, R=0.9999 (p.203)
            [examples.gsot.141-delay-feedback-filtered-dcblock]
            ;; Chapter 7 — Saturating feedback limiter: tanh(dr*signal) in loop; bounded, harmonic (p.204)
            [examples.gsot.142-delay-feedback-saturated]
            ;; Chapter 7 — Parameter smoothing: one-pole lag on delay time; click-free transitions (pp.206-207)
            [examples.gsot.143-delay-param-smoothed]
            ;; Chapter 7 — Doppler morph: blend two delay targets; smooth transition = pitch shift (p.208)
            [examples.gsot.144-delay-morphed-times]
            ;; Chapter 7 — Two-tap crossfade: smooth crossfade GAIN (not delay time) = no pitch shift (pp.209-210)
            [examples.gsot.145-delay-morphed-times-no-pitch-change]
            ;; Chapter 7 — Background change abstraction: auto crossfade on :tg change via _@1 detection (p.211)
            [examples.gsot.146-go-background-change]
            ;; Chapter 7 — Multi-tap capstone: slapback + echo + filtered-echo in parallel (pp.211-212)
            [examples.gsot.147-delay-multi-effect]
            ;; Chapter 8 — Feedforward comb filter: peaks/notches at 1/D Hz spacing (p.215)
            [examples.gsot.148-comb-filter]
            ;; Chapter 8 — Enharmonic comb: two feedforward taps at inharmonic delay ratios (pp.217-218)
            [examples.gsot.149-comb-enharmonic]
            ;; Alembic extension — Dispersive feedback comb: allpass inharmonicity in feedback path
            [examples.gsot.150-comb-enharmonic-dispersive]
            ;; Chapter 8 — Karplus-Strong basic: two-point averaging LP in feedback, integer delay (pp.218-219)
            [examples.gsot.151-string-basic]
            ;; Chapter 8 — K-S pitch-corrected: de.fdelay, D=SR/hz-1.5 compensates averaging group delay (p.220)
            [examples.gsot.152-string-pitched]
            ;; Chapter 8 — K-S variable feedback: (1-dp)*s+dp*s@1; dp controls brightness; pitch-corrected (p.221)
            [examples.gsot.153-string-feedback-control]
            ;; Chapter 8 — K-S inverted feedback: y=x-g*lp(y[n-D]); odd harmonics only; fundamental at :hz/2 (p.221)
            [examples.gsot.154-string-inverted-feedback]
            ;; Chapter 8 — K-S RT60 damping: fb=pow(0.001,1000/(hz×dc)); decay time independent of pitch (pp.222-223)
            [examples.gsot.155-string-damping]
            ;; Chapter 8 — Allpass interpolation: a=(1-f)/(1+f); flat magnitude, consistent timbre vs. linear (pp.223-226)
            [examples.gsot.156-string-interp-allpass]
            ;; Ch.8 Frequent Modulations — AM: in*(1+mx*osc(fc)); RM=no DC offset; sideband maths (pp.227-228)
            [examples.gsot.157-am]
            ;; Ch.8 — AM-depth: in*(bs+dp*osc); :bs=1→AM, :bs=0→RM; over-modulation (:dp>1) for distortion (pp.229-230)
            [examples.gsot.158-am-depth]
            ;; Ch.8 — AMRM: explicit AM/RM outputs + morph; AM=RM+dry; sideband amplitudes equal in both (p.232)
            [examples.gsot.159-amrm]
            ;; Ch.8 — sine: phase-explicit phasor→sin oscillator; PM audio input; building block for FM/PM (p.233)
            [examples.gsot.160-sine]
            ;; Ch.8 — FMPM: FM (mod→freq) vs PM (mod→phase) morph; Bessel sidebands; Chowning β=ix convention (pp.232-233)
            [examples.gsot.161-fmpm]
            ;; Ch.8 — FMPM-enveloped: AR envelope on β and amplitude; timbral decay from dense→pure sine (pp.234-235)
            [examples.gsot.162-fmpm-enveloped]
            ;; Ch.8 — FMPM-harmonicity: fm=fc×rt; integer rt=harmonic, irrational=metallic; C:M ratio table (p.236)
            [examples.gsot.163-fmpm-harmonicity]
            ;; Ch.8 — parallel-carriers: one mod→two carriers; fc1=fm×r1, fc2=fm×r2; :mx blend; DX7 alg.5 pattern (p.238)
            [examples.gsot.164-fmpm-parallel-carriers]
            ;; Ch.8 — parallel-modulators: two mods→one carrier; intermod at fc+p×fm1+q×fm2; per-mod index (p.238)
            [examples.gsot.165-fmpm-parallel-modulators]
            ;; Ch.8 — FMPM-blending: (1-bl)*sine+bl*sawtooth modulator; saw≡harmonic stack; Bessel cluster superposition (pp.239-241)
            [examples.gsot.166-fmpm-blending]
            ;; Ch.8 — cascade: top mod→middle FM mod→carrier; fc+n1*fm1+n1*n2*fm2 multiplicative grid; DX7 series alg (pp.241-242)
            [examples.gsot.167-fmpm-cascade-modulation]
            ;; Ch.8 — feedback: out[n-1]→own freq/phase via ~ _; sine→sawtooth→chaos with :ix; DX7 op1 self-feedback (pp.242-243)
            [examples.gsot.168-fmpm-feedback]
            ;; Ch.8 — cross-feedback: osc1↔osc2 PM/FM via si.bus(2); synchronisation, phase-locking, chaos (p.246)
            [examples.gsot.169-pm-cross-feedback]
            ;; Ch.8 — cross-feedback-filtered: unity-gain LP on each feedback path; tames chaos; extends usable :i1/:i2 range (p.247)
            [examples.gsot.170-pm-cross-feedback-filtered]
            ;; Ch.8 — FMPM-ifod: in×(bs+FM_osc); FM signal as ring/AM modulator; ring-of-rings sidebands (pp.247-248)
            [examples.gsot.171-fmpm-ifod]
            ;; Ch.8 — PM-noclicks: latch :ix and :rt at phasor reset (ph<ph@1); (x*rst+_*(1-rst))~_; click-suppressed PM (p.248)
            [examples.gsot.172-pm-noclicks-latched]
            ;; Ch.8 — waveshaping-mod: ma.tanh(dr*mo)/ma.tanh(dr) shaped mod; odd harmonics only; Bessel clusters at fm,3fm,5fm (pp.249-250)
            [examples.gsot.173-fmpm-waveshaping-modulator]
            ;; Ch.8 — PM-asymmetric: (1+mx*cos)*sin(phasor+ix*sin); quadrature AM×PM; sideband k scaled by (1+k*mx/ix) (pp.251-253)
            [examples.gsot.174-pm-asymmetric]
            ;; Ch.8 — AMRM-bandlimited: LP input at SR/2−fc before AM/RM; 0.5 gain for AM headroom; oversampling discussion (p.254)
            [examples.gsot.175-amrm-bandlimited]
            ;; Ch.8 — FMPM-carsonrule: BW=2*(ix+1)*fm; ix_safe=max(0,(SR/2−fc)/(fc*rt)−1); clamp ix to prevent aliasing (p.255)
            [examples.gsot.176-fmpm-carsonrule]
            ;; Ch.8 — FMPM-carsonrule-filtered: ix clamp + LP at fc*(1+(ic+1)*rt); Carson BW as parametric LP cutoff (p.256)
            [examples.gsot.177-fmpm-carsonrule-filtered]
            ;; Ch.8 — FMPM-antialias-filter: user :cf LP on FM output; no ix clamp; deliberate aliasing with controllable ceiling (p.257)
            [examples.gsot.178-fmpm-antialias-filter]
            ;; Ch.8 — PM-is-doppler-delay: variable delay = PM; τ(t)=ix/(2π*fc)*sin(fm*t); Doppler dτ/dt gives FM-equivalent pitch shift (pp.257-258)
            [examples.gsot.179-pm-is-doppler-delay]
            ;; Ch.8 — KP-FM: Karplus-Strong with FM-modulated delay; N=SR/(fc+ix*fm*osc(fm)); LP in feedback path; delay-PM in resonating loop (p.259)
            [examples.gsot.180-kp-fm]
            ;; Ch.8 — harmonic: shared phasor → 8 phase-locked harmonics; a_n=bl^(n-1)/n; bl=0→sine, bl=1→sawtooth; go.harmonic gendsp (pp.259-260)
            [examples.gsot.181-harmonic]
            ;; Ch.8 — AMRM-blended-harmonics: harmonic mo drives AM/RM (in*(bs+mo)) + PM (sin(phasor(fc)+ix*mo)); sideband pairs at ±n*fm (p.260)
            [examples.gsot.182-amrm-blended-harmonics]
            ;; Ch.8 — ModFM: asin(osc(fm))/(π/2) triangle modulator; odd harmonics 1/n²; β_1≈0.811*ix; FM/PM morph (pp.262-263)
            [examples.gsot.183-modfm]
            ;; Ch.9 — wavetable-1D: rdtable(1024, os.sinwaveform, i0)+linear interp; s0+fr*(s1-s0); bitmask wrap (pp.265-268)
            [examples.gsot.184-wavetable-1d]
            ;; Ch.9 — bilinear-indices: xf=xp*W−floor(xp*W), yf=wp*H−floor(wp*H); 2D grid fractional coords (p.272)
            [examples.gsot.185-bilinear-indices]
            ;; Ch.9 — bilinear-interpolation: s00*(1−xf)(1−yf)+s10*xf(1−yf)+s01*(1−xf)*yf+s11*xf*yf (p.273)
            [examples.gsot.186-bilinear-interpolation]
            ;; Ch.9 — wavetables-2D: 8-waveform bank (1..8 harmonics, 1/n rolloff); bilinear interp; :wp morphs sine→8-partial saw (p.273)
            [examples.gsot.187-wavetables-2d]
            ;; Ch.9 — wavetable-3D: W×H×D=256×4×4; Z axis=phase offset; 8-corner trilinear interp; :wp/:zp navigate (pp.273-276)
            [examples.gsot.188-wavetable-3d]
            ;; Ch.9 — trilinear-interpolation: 8-corner formula; volume-weighted; nested bilinear×2 steps + z lerp (p.276)
            [examples.gsot.189-trilinear-interpolation]
            ;; Ch.9 — trilinear-indices: xf/yf/zf fractional coords; x0/x1 bitmask wrap; y0/z0 modulo wrap (p.277)
            [examples.gsot.190-trilinear-indices]
            ;; Ch.9 — wavetable-3D-attractor: Lorenz(σ=10,ρ=28,β=8/3) drives Y/Z axes; Euler dt param; ba.impulse kick (p.278)
            [examples.gsot.191-wavetable-3d-attractor]
            ;; Ch.9 — sinc-interpolate: 4-pt Hann-windowed sinc; sum(k,4,sinc(fr-k)*hann(fr-k)); singularity via max(|x|,1e-9) (p.290)
            [examples.gsot.192-sinc-interpolate]
            ;; Ch.9 — sincmipmap-sample: auto y_mip=int(SR/(2*fc))-1 each sample; sinc X; 4 reads in mipmap row (p.291)
            [examples.gsot.193-wavetable-sincmipmap-sample]
            ;; Ch.9 — sincmipmap-wave: same but ba.sAndH(ph<ph',y_raw) freezes mipmap level per cycle (p.292)
            [examples.gsot.194-wavetable-sincmipmap-wave]
            ;; Ch.9 — sinc-interpolate-wave: sinc X + linear Y; sinc_y(y0)*(1-yf)+sinc_y(y1)*yf; 8 reads; :wp morphing (p.293)
            [examples.gsot.195-sinc-interpolate-wave]
            ;; Ch.9 — terrain-reader: wave terrain utility; bilinear read from 2D harmonic bank at audio-rate (x,yp) (p.295)
            [examples.gsot.196-terrain-reader]
            [examples.gsot.197-wave-terrain-osc]
            [examples.gsot.198-waveterrain-generate-bfg]
            [examples.gsot.199-waveterrain-2d-doubleorbit]
            [examples.gsot.200-waveterrain-2d-carom]
            [examples.gsot.201-polygonal]
            [examples.gsot.202-windowed-sync]))

(defn- check [graph]
  (let [src (emit-faust graph)]
    (is (string? src) "emit-faust returns a string")
    (is (str/starts-with? src "import(\"stdfaust.lib\");") "starts with stdfaust import")
    (is (re-find #"process\s*=" src) "has a process declaration")
    (is (nil? (validate graph)) "compiles through faust -lang cpp")))

;; ---------------------------------------------------------------------------
;; Chapter 2 — Modular Arithmetic of Time
;; ---------------------------------------------------------------------------

(deftest gsot-04-counter-play-a-buffer
  (testing "p.23 counter_play_a_buffer.maxpat — accum + audio-file"
    (check examples.gsot.04-counter-play-a-buffer/counter-play-a-buffer)))

(deftest gsot-05-counter-and-wrap
  (testing "p.24 counter_and_wrap.maxpat — accum + wrap to duration"
    (check examples.gsot.05-counter-and-wrap/counter-and-wrap)))

(deftest gsot-06-counter-and-wrap-buffer
  (testing "p.24 counter_and_wrap_buffer.maxpat — accum + wrap + audio-file"
    (check examples.gsot.06-counter-and-wrap-buffer/counter-and-wrap-buffer)))

(deftest gsot-07-phasor-counter
  (testing "pp.25-27 phasor_counter.maxpat — phasor as Hz-rate buffer playhead"
    (check examples.gsot.07-phasor-counter/phasor-counter)))

(deftest gsot-08-phasor-basic-table-oscillator
  (testing "p.28 phasor_basic_table_oscillator.maxpat — phasor + table lookup = oscillator"
    (check examples.gsot.08-phasor-basic-table-oscillator/phasor-basic-table-oscillator)))

(deftest gsot-09-phasor-bpm
  (testing "pp.28-29 phasor_bpm.maxpat — BPM-clocked drum loop"
    (check examples.gsot.09-phasor-bpm/phasor-bpm)))

(deftest gsot-10-phasor-loop-processing
  (testing "pp.30-32 phasor_loop_processing.maxpat — scrub_and_rate ramp processing"
    (check examples.gsot.10-phasor-loop-processing/phasor-loop-processing)))

(deftest gsot-11-phasor-beat-slicer
  (testing "pp.33-36 phasor_beat_slicer.maxpat — random beat slicing"
    (check examples.gsot.11-phasor-beat-slicer/phasor-beat-slicer)))

(deftest gsot-12-ramp-from-bpm
  (testing "pp.37-39 ramp_from_bpm.maxpat + go.ramp.frombpm — clock multiplication"
    (check examples.gsot.12-ramp-from-bpm/ramp-from-bpm)))

(deftest gsot-13-ramp-to-steps
  (testing "p.39 go.ramp2steps — floor(x*n)/n staircase quantisation"
    (check examples.gsot.13-ramp-to-steps/ramp-to-steps)))

(deftest gsot-14-ramp-to-trig
  (testing "pp.39-41 go.ramp2trig — trigger from ramp wrap discontinuity"
    (check examples.gsot.14-ramp-to-trig/ramp-to-trig)))

(deftest gsot-15-ramp-phase-shift
  (testing "pp.41+ shifting ramps — phase rotation via offset + wrap"
    (check examples.gsot.15-ramp-phase-shift/ramp-phase-shift)))

(deftest gsot-16-ramp-rotate
  (testing "pp.41+ go.ramp.rotate — named phase-rotation processor (audio-in form)"
    (check examples.gsot.16-ramp-rotate/ramp-rotate)))

(deftest gsot-17-ramp-to-slope
  (testing "p.42 go.ramp2slope — conditioned delta; holds slope across wrap"
    (check examples.gsot.17-ramp-to-slope/ramp-to-slope)))

(deftest gsot-18-ramp-to-freq
  (testing "p.43 go.ramp2freq — slope * samplerate → Hz"
    (check examples.gsot.18-ramp-to-freq/ramp-to-freq)))

(deftest gsot-19-ramp-to-trig-gendsp
  (testing "pp.43-45 ramp_to_trig.maxpat + go.ramp2trig — processor form"
    (check examples.gsot.19-ramp-to-trig-gendsp/ramp-to-trig-gendsp)))

(deftest gsot-20-ramp-div-simple
  (testing "p.46 go.ramp.div.simple — freq-detection division, free-running phasor"
    (check examples.gsot.20-ramp-div-simple/ramp-div-simple)))

(deftest gsot-21-ramp-div
  (testing "p.47 go.ramp.div — phase-locked division via trigger counter mod N"
    (check examples.gsot.21-ramp-div/ramp-div)))

(deftest gsot-22-ramp-div-musical
  (testing "pp.48-49 go.ramp.div musical context — note-value subdivisions of beat ramp"
    (check examples.gsot.22-ramp-div-musical/ramp-div-musical)))

;; ---------------------------------------------------------------------------
;; Chapter 2 close — ramp bursts (pp.53-55)
;; ---------------------------------------------------------------------------

(deftest gsot-23-ramp-bursts
  (testing "p.53 ramp_bursts.maxpat — N fast sub-ramps within a burst window"
    (check examples.gsot.23-ramp-bursts/ramp-bursts)))

(deftest gsot-24-ramp-bursts-to-trigs
  (testing "p.54 go.ramp_bursts2trigs — trigger pulses from burst ramp"
    (check examples.gsot.24-ramp-bursts-to-trigs/ramp-bursts-to-trigs)))

(deftest gsot-25-ramp-bursts-shaped
  (testing "p.55 go.ramp_bursts_shaped — amplitude-weighted burst ramp (Chapter 2 close)"
    (check examples.gsot.25-ramp-bursts-shaped/ramp-bursts-shaped)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Unit shaping (p.58)
;; ---------------------------------------------------------------------------

(deftest gsot-26-unit-triangle
  (testing "p.58 go.unit.triangle — 1 - |2x-1|"
    (check examples.gsot.26-unit-triangle/unit-triangle)))

(deftest gsot-27-unit-trapezoid
  (testing "p.58 go.unit.trapezoid — piecewise linear rise-hold-fall"
    (check examples.gsot.27-unit-trapezoid/unit-trapezoid)))

(deftest gsot-28-unit-kink
  (testing "p.58 go.unit.kink — slope-change ramp with variable kink point"
    (check examples.gsot.28-unit-kink/unit-kink)))

(deftest gsot-29-unit-lfo
  (testing "p.58 go.unit.lfo — 0.5*(1-cos(2π·x)) raised cosine"
    (check examples.gsot.29-unit-lfo/unit-lfo)))

(deftest gsot-30-unit-pow
  (testing "p.58 go.unit.pow — x^p power-law curve"
    (check examples.gsot.30-unit-pow/unit-pow)))

(deftest gsot-31-unit-arc
  (testing "p.58 go.unit.arc — sqrt(x*(2-x)) quarter-circle arc"
    (check examples.gsot.31-unit-arc/unit-arc)))

(deftest gsot-32-unit-cubic
  (testing "p.58 go.unit.cubic — 3x²-2x³ smoothstep S-curve"
    (check examples.gsot.32-unit-cubic/unit-cubic)))

(deftest gsot-33-unit-logistic
  (testing "p.58 go.unit.logistic — 1/(1+exp(-k(x-0.5))) sigmoid"
    (check examples.gsot.33-unit-logistic/unit-logistic)))

(deftest gsot-34-unit-ease-exp
  (testing "p.58 go.unit.ease.exp — (exp(k·x)-1)/(exp(k)-1) exponential ease"
    (check examples.gsot.34-unit-ease-exp/unit-ease-exp)))

(deftest gsot-35-unit-welch
  (testing "p.58 go.unit.welch — 4x(1-x) parabolic arch"
    (check examples.gsot.35-unit-welch/unit-welch)))

(deftest gsot-36-unit-tukey
  (testing "p.58 go.unit.tukey — cosine-tapered window with flat top"
    (check examples.gsot.36-unit-tukey/unit-tukey)))

(deftest gsot-37-ramp-swing
  (testing "pp.59-61 ramp.swing.maxpat — go.unit.kink applied to beat ramp"
    (check examples.gsot.37-ramp-swing/ramp-swing)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — From ramps to LFOs (p.69)
;; ---------------------------------------------------------------------------

(deftest gsot-38-lfo-multi
  (testing "p.69 go.lfo.multi.gendsp — all 11 unit shapers on one ramp, 11 outputs"
    (check examples.gsot.38-lfo-multi/lfo-multi)))

(deftest gsot-39-smooth-stepped
  (testing "p.70 smooth-stepped-template / linear-stepped-noise — phasor-driven lerp with history feedback"
    (check examples.gsot.39-smooth-stepped/smooth-stepped)))

(deftest gsot-40-smooth-stepped-shaped
  (testing "p.70 shaped-stepped-noise — raised-cosine blend replaces linear mix factor"
    (check examples.gsot.40-smooth-stepped-shaped/smooth-stepped-shaped)))

(deftest gsot-41-interpolating-lfo
  (testing "Chapter 3 go.lfo — skewed triangle, arc-blend shape, symmetry, bipolar"
    (check examples.gsot.41-interpolating-lfo/interpolating-lfo)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Glides and portamento (p.76)
;; ---------------------------------------------------------------------------

(deftest gsot-42-portamento
  (testing "pp.76-77 accum-change-template — linear glide on input change"
    (check examples.gsot.42-portamento/portamento)))

(deftest gsot-43-portamento-shaped
  (testing "pp.76-77 smooth-stepped-noise — ease-exp shaped glide on input change"
    (check examples.gsot.43-portamento-shaped/portamento-shaped)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Easing functions (p.77)
;; ---------------------------------------------------------------------------

(deftest gsot-44-easing-multi
  (testing "pp.77-78 go.unit.ease.{pow,circle,exp,back,elastic,sine} — generalized easing structure"
    (check examples.gsot.44-easing-multi/easing-multi)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Window envelope functions (p.78)
;; ---------------------------------------------------------------------------

(deftest gsot-45-window-fixed
  (testing "pp.78-79 hann, hamming, blackman, blackman-harris, blackman-nuttall, nuttall, flat-top, welch, parzen"
    (check examples.gsot.45-window-fixed/window-fixed)))

(deftest gsot-46-window-parametric
  (testing "pp.78-79 trapezoid, tukey, plancktaper, gauss, raisedcosine — parametric window shapes"
    (check examples.gsot.46-window-parametric/window-parametric)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Waveshaping bipolar signals (p.79)
;; ---------------------------------------------------------------------------

(deftest gsot-47-bipolar-waveshaping
  (testing "pp.79-81 bipolar_waveshaping_unitshapers — symmetric (odd) and full-range mappings"
    (check examples.gsot.47-bipolar-waveshaping/bipolar-waveshaping)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Audio waveshaping / polynomial shapers (p.81)
;; ---------------------------------------------------------------------------

(deftest gsot-48-chebyshev-waveshaping
  (testing "pp.81-83 bipolar_waveshaping_chebyshev — T1..T7 via recurrence"
    (check examples.gsot.48-chebyshev-waveshaping/chebyshev-waveshaping)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Sigmoid waveshaping (p.84)
;; ---------------------------------------------------------------------------

(deftest gsot-49-sigmoid-waveshaping
  (testing "pp.84-85 sigmoid waveshaping — tanh, Padé, sqrt, hard clip with drive param"
    (check examples.gsot.49-sigmoid-waveshaping/sigmoid-waveshaping)))

(deftest gsot-50-sigmoid-gsot
  (testing "pp.84-85 go.sigmoid.{tanh,logistic,guderman,atan,softclip} + go.sigmoid2"
    (check examples.gsot.50-sigmoid-gsot/sigmoid-gsot)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Enveloped sigmoid waveshaping (p.86)
;; ---------------------------------------------------------------------------

(deftest gsot-51-sigmoid-enveloped
  (testing "p.86 bipolar_waveshaping_sigmoids_enveloped — wet/dry blend via envelope input"
    (check examples.gsot.51-sigmoid-enveloped/sigmoids-enveloped)))

;; ---------------------------------------------------------------------------
;; Chapter 3 — Normalized sigmoids as unit shapers (p.87)
;; ---------------------------------------------------------------------------

(deftest gsot-52-unit-tanh
  (testing "p.87 go.unit.tanh.gendsp — normalized tanh as unit shaper [0,1]->[0,1]"
    (check examples.gsot.52-unit-tanh/unit-tanh)))

(deftest gsot-53-unit-sigmoids
  (testing "p.88 go.unit.{logistic,sigmoid2,gundermann,ata,softclip} — normalized sigmoid unit shapers"
    (check examples.gsot.53-unit-sigmoids/unit-sigmoids)))

;; ---------------------------------------------------------------------------
;; Chapter 4 — Feel the noise (p.91)
;; ---------------------------------------------------------------------------

(deftest gsot-54-noise-basic
  (testing "p.91 noise operator — white noise source, no audio-in, amplitude param"
    (check examples.gsot.54-noise-basic/noise-basic)))

(deftest gsot-55-random-range
  (testing "p.93 random_range.maxpat — noise scaled to [lo,hi] via affine map"
    (check examples.gsot.55-random-range/random-range)))

(deftest gsot-56-random-steps
  (testing "p.93 random_steps.maxpat — track-hold of range-scaled noise on trigger"
    (check examples.gsot.56-random-steps/random-steps)))

(deftest gsot-57-random-smoothed
  (testing "p.94 random_smoothed.maxpat — linear interp between random steps via phasor phase"
    (check examples.gsot.57-random-smoothed/random-smoothed)))

(deftest gsot-58-spline-smoothed
  (testing "p.95 go.shift.spline6.gendsp — Catmull-Rom spline on 6-stage shift register"
    (check examples.gsot.58-spline-smoothed/spline-smoothed)))

(deftest gsot-59-random-chance
  (testing "p.96 go.chance.gendsp / random_chance.maxpat — probability-gated trigger"
    (check examples.gsot.59-random-chance/random-chance)))

(deftest gsot-60-bernoulli-gate
  (testing "p.97 go.bern.gendsp / random_bernoulli-gate.maxpat — two-output trigger router"
    (check examples.gsot.60-bernoulli-gate/bernoulli-gate)))

(deftest gsot-61-random-periods
  (testing "p.98 random_periods.maxpat — random-length period counter/phasor with trig-out"
    (check examples.gsot.61-random-periods/random-periods)))

(deftest gsot-62-normal-noise
  (testing "p.100 go.noise.normal.gendsp — CLT sum-of-12 approximately N(mu, sigma²)"
    (check examples.gsot.62-normal-noise/normal-noise)))

(deftest gsot-63-random-distributions
  (testing "p.101 random_distributions.maxpat — uniform vs normal side-by-side comparison"
    (check examples.gsot.63-random-distributions/random-distributions)))

(deftest gsot-64-random-walk
  (testing "p.101-102 random_walks.maxpat — trigger-gated accumulating random walk"
    (check examples.gsot.64-random-walk/random-walk)))

(deftest gsot-65-random-walk-bounded
  (testing "p.103 random_walk_bounded.maxpat — fold-reflected bounded random walk"
    (check examples.gsot.65-random-walk-bounded/random-walk-bounded)))

(deftest gsot-66-random-integer
  (testing "p.103 go.random.gendsp / random_integer.maxpat — discrete uniform integer on trigger"
    (check examples.gsot.66-random-integer/random-integer)))

(deftest gsot-67-random-urn
  (testing "pp.104-109 random_urn.maxpat — rotating-permutation urn approximation (data.deck not expressible in Faust)"
    (check examples.gsot.67-random-urn/random-urn)))

(deftest gsot-68-random-urn-flexible
  (testing "pp.110-111 random_urn.maxpat extended — manual reshuffle trigger + no-immediate-repeat"
    (check examples.gsot.68-random-urn-flexible/random-urn-flexible)))

(deftest gsot-69-chaos-lorenz
  (testing "pp.112-114 chaos_Lorenz.maxpat — Lorenz attractor Euler step (3-in / 3-out; self-oscillation via external feedback)"
    (check examples.gsot.69-chaos-lorenz/chaos-lorenz)))

(deftest gsot-70-limits
  (testing "pp.115-116 go.limits.gendsp — running min/max tracker with per-sample decay"
    (check examples.gsot.70-limits/limits)))

(deftest gsot-71-autolimit
  (testing "pp.116 go.autolimit.gendsp — auto-normalise unbounded signal to [-1,1] via tracked limits"
    (check examples.gsot.71-autolimit/autolimit)))

(deftest gsot-72-go-chaos
  (testing "pp.117 go.chaos / Liu-Chen.maxpat — abstract chaos step unifying Lorenz and Lu-Chen families"
    (check examples.gsot.72-go-chaos/go-chaos)))

(deftest gsot-73-go-chaos-liu-chen
  (testing "pp.118 go.chaos.liu_chen.gendsp — Lu-Chen attractor with hardwired coefficients (a=36, b=0, c=20, d=3)"
    (check examples.gsot.73-go-chaos-liu-chen/go-chaos-liu-chen)))

(deftest gsot-74-chaos-tempo-nonrobotic
  (testing "pp.119-120 chaos.tempo.nonrobotic.maxpat — logistic map drives chaotic phasor rate for natural tempo looseness"
    (check examples.gsot.74-chaos-tempo-nonrobotic/chaos-tempo-nonrobotic)))

(deftest gsot-75-chaos-lorenz-audioinjection
  (testing "pp.121 chaos_Lorenz_audioinjection.maxpat — audio signal injected into Lorenz x equation; chaotic nonlinear coupling"
    (check examples.gsot.75-chaos-lorenz-audioinjection/chaos-lorenz-audioinjection)))

(deftest gsot-76-mixer-sequencer
  (testing "pp.123-125 mixer-sequencer.maxpat — 8-step pitch sequencer; bitmask gate enable; binary-tree select2 pitch lookup"
    (check examples.gsot.76-mixer-sequencer/mixer-sequencer)))

(deftest gsot-77-latched-sequencer
  (testing "pp.126 latched-sequencer.maxpat — 8 per-step S&H cells capture cv-in on each step trigger; sequence fills in over time"
    (check examples.gsot.77-latched-sequencer/latched-sequencer)))

(deftest gsot-78-go-shiftregister2
  (testing "pp.127 go.shiftregister2.gendsp — 2-stage shift register; s1 lags s0 by one trigger event via @1 delay"
    (check examples.gsot.78-go-shiftregister2/go-shiftregister2)))

(deftest gsot-79-go-shiftregister8
  (testing "pp.127 go.shiftregister8.gendsp — 8-stage shift register; all stages simultaneously available for temporal canon"
    (check examples.gsot.79-go-shiftregister8/go-shiftregister8)))

(deftest gsot-80-shift-register
  (testing "pp.128 shift-register.maxpat — 2-voice canon; v1=stage 0, v2=stage :canon (1-7); selectable imitation interval"
    (check examples.gsot.80-shift-register/shift-register)))

(deftest gsot-81-shift-register-weighted
  (testing "pp.129-130 shift-register-weighted.maxpat — binary shift register; Bernoulli(weight) new-bit; 8 simultaneous gate streams"
    (check examples.gsot.81-shift-register-weighted/shift-register-weighted)))

(deftest gsot-82-shift-register-weighted-random
  (testing "pp.130-131 shift-register-weighted-random.maxpat — binary shift register; per-trigger random weight from centre±spread"
    (check examples.gsot.82-shift-register-weighted-random/shift-register-weighted-random)))

(deftest gsot-83-shift-register-weighted-xor
  (testing "pp.131-132 shift-register-weighted-xor.maxpat — LFSR XOR feedback (x^8+x^4+x^3+x^2+1, period 255) weighted vs Bernoulli random; packed integer state"
    (check examples.gsot.83-shift-register-weighted-xor/shift-register-weighted-xor)))

(deftest gsot-84-binary-decode
  (testing "pp.134-135 binary decoding — 8 shift register bits weighted-summed to integer index [0,255]; pseudo-random melodic index from LFSR stages"
    (check examples.gsot.84-binary-decode/binary-decode)))

(deftest gsot-85-shift-register-integer
  (testing "pp.136-139 shift-register-integer.maxpat — integer bitmask as step pattern; counter reads bits into shift register; deterministic canon voices"
    (check examples.gsot.85-shift-register-integer/shift-register-integer)))

(deftest gsot-86-bit-unpack8
  (testing "pp.140 go.bit.unpack8.gendsp — unpack integer to 8 binary signals; inverse of binary-decode (example 84)"
    (check examples.gsot.86-bit-unpack8/bit-unpack8)))

(deftest gsot-87-bit-shift
  (testing "pp.140 go.bit.shift.gendsp — left/right shift of 8-bit integer by signed :amount; zeros fill vacated positions"
    (check examples.gsot.87-bit-shift/bit-shift)))

(deftest gsot-88-bit-wrap
  (testing "pp.141 go.bit.wrap.gendsp — circular left rotation of 8-bit integer; bits wrap from MSB to LSB; preserves step density"
    (check examples.gsot.88-bit-wrap/bit-wrap)))

(deftest gsot-89-bit-extract
  (testing "pp.141 go.bit.extract.gendsp — extract :width-bit field from 8-bit integer at :offset; generalises single-bit extraction"
    (check examples.gsot.89-bit-extract/bit-extract)))

(deftest gsot-90-bit-rotate
  (testing "pp.141-142 go.bit.rotate.gendsp — trigger-driven accumulating circular rotation; counter 0-7 cycles through all 8 positions"
    (check examples.gsot.90-bit-rotate/bit-rotate)))

(deftest gsot-91-euclidean-rhythms
  (testing "pp.142-148 euclidean_rhythms.maxpat — digitized ratio floor(s*N/K) generates maximally-even N-beat/K-step gate; wrap detection for step 0"
    (check examples.gsot.91-euclidean-rhythms/euclidean-rhythms)))

(deftest gsot-92-euclidean-ratchets
  (testing "pp.149 euclidean_ratchets.maxpat — countdown register fires R pulses per beat; trigger-gated load-or-decrement; restarts cleanly on overlapping beats"
    (check examples.gsot.92-euclidean-ratchets/euclidean-ratchets)))

(deftest gsot-93-euclidean-lfo
  (testing "pp.149 euclidean_LFO.maxpat — fractional part of digitized ratio; sawtooth resetting at Euclidean beat positions; drop amplitude encodes gap length"
    (check examples.gsot.93-euclidean-lfo/euclidean-lfo)))

(deftest gsot-94-pitch
  (testing "pp.150-151 pitch.maxpat — chromatic pitch space: integer index → MIDI note (root + index semitones) → Hz via equal temperament"
    (check examples.gsot.94-pitch/pitch)))

(deftest gsot-95-pitch-quantized
  (testing "pp.151-152 pitch-quantized.maxpat — diatonic pitch space: float degree → rint → major scale select2 lookup → Hz; correct octave wrapping for all integer degrees"
    (check examples.gsot.95-pitch-quantized/pitch-quantized)))

(deftest gsot-96-quantizing-pitch
  (testing "pp.153-154 quantizing-pitch.maxpat — inverse digitized ratio floor(12*i/N) maps degree index to maximally-even N-note scale; N=1-8 generates octave/tritone/augmented/diminished/pentatonic/whole-tone/diatonic/octatonic"
    (check examples.gsot.96-quantizing-pitch/quantizing-pitch)))

(deftest gsot-97-quantizing-pitch-smoothed
  (testing "pp.155-157 quantizing-pitch-smoothed.maxpat — lerp(floor(ratio), ratio, smooth) blends hard-quantized Euclidean scale (smooth=0) to continuous linear interpolation (smooth=1); frac encodes position within each scale step"
    (check examples.gsot.97-quantizing-pitch-smoothed/quantizing-pitch-smoothed)))

(deftest gsot-98-quantizing-timbre
  (testing "pp.157 quantizing-timbre.maxpat — phasor [0,1) → Euclidean N-step staircase waveform; smooth=0 hard steps, smooth=1 linear sawtooth; N cancels in ratio so all N converge at smooth=1"
    (check examples.gsot.98-quantizing-timbre/quantizing-timbre)))

(deftest gsot-99-quantizing-audio-bitcrush
  (testing "pp.158 quantizing-audio-bitcrush.maxpat — audio [-1,1) → N Euclidean quantisation levels via inverse digitized ratio; smooth interpolates adjacent levels; N=12 smooth=1 is passthrough"
    (check examples.gsot.99-quantizing-audio-bitcrush/quantizing-audio-bitcrush)))

;; ---------------------------------------------------------------------------
;; Chapter 6 — Filters, Diagrams, and the Balance of Time
;; ---------------------------------------------------------------------------

(deftest gsot-100-onepole-basic
  (testing "pp.159-163 go.onepole.basic.gendsp — one-pole IIR LP: y[n]=(1-a)*x[n]+a*y[n-1]; a=balance of time; single ~_ feedback; pole at z=a"
    (check examples.gsot.100-onepole-basic/onepole-basic)))

(deftest gsot-101-vactrol
  (testing "pp.169-170 go.vactrol.gendsp — vactrol LPG subpatch: asymmetric one-pole env follower (fast attack/slow release) drives filter coeff (1-env)*0.999 and VCA (×env); 0.999 ceiling ensures drain at gate=0"
    (check examples.gsot.101-vactrol/vactrol)))

(deftest gsot-102-lowpass-gate
  (testing "pp.171 lowpass-gate.maxpat — complete LPG: gate + audio in; vactrol envelope; LP filter + VCA from same env; separate LP-only and gate-only mix controls"
    (check examples.gsot.102-lowpass-gate/lowpass-gate)))

(deftest gsot-103-onepole-basic-hz
  (testing "pp.173 go.onepole.basic_hz.gendsp — Hz-parameterized one-pole LP: a=exp(-2π*fc/ma.SR); two-node pattern (Hz→coeff, coeff→filter); same DSP as ex.100, musical interface"
    (check examples.gsot.103-onepole-basic-hz/onepole-basic-hz)))

(deftest gsot-104-allpass
  (testing "pp.175 go.allpass.gendsp — first-order allpass: y[n]=a*x[n]+x[n-1]-a*y[n-1]; x[n-1] via @1 feedforward, y[n-1] via ~_ feedback; flat amplitude, frequency-dependent phase shift"
    (check examples.gsot.104-allpass/allpass)))

(deftest gsot-105-phaser-4stage
  (testing "pp.176 phaser-4stage.maxpat — 4 allpass stages in series (8 state elements); shared coefficient; dry/wet mix; 2 notches per sweep"
    (check examples.gsot.105-phaser-4stage/phaser-4stage)))

(deftest gsot-106-phaser-8stage
  (testing "pp.177 phaser-8stage.maxpat — 8 allpass stages in series (16 state elements); shared coefficient; dry/wet mix; 4 notches per sweep; vintage thick phaser character"
    (check examples.gsot.106-phaser-8stage/phaser-8stage)))

(deftest gsot-107-allpass-hz
  (testing "pp.178 go.allpass.hz.gendsp — Hz-parameterized first-order allpass; bilinear transform a=(tan(π·fc/SR)−1)/(tan(π·fc/SR)+1); −π/2 phase shift at fc"
    (check examples.gsot.107-allpass-hz/allpass-hz)))

(deftest gsot-108-biquad
  (testing "pp.178-180 go.biquad.gendsp — second-order IIR; Direct Form II: w=(in-a1*_-a2*_@1)~_, out=b0*w+b1*w@1+b2*w@2; 5 coefficients b0/b1/b2/a1/a2"
    (check examples.gsot.108-biquad/biquad)))

(deftest gsot-109-biquad-lp
  (testing "pp.181 go.biquad.lp — RBJ LP: b0=b2=(1-cw)/(2*a0), b1=(1-cw)/a0; Butterworth at Q=0.707"
    (check examples.gsot.109-biquad-lp/biquad-lp)))

(deftest gsot-110-biquad-hp
  (testing "pp.181 go.biquad.hp — RBJ HP: b0=b2=(1+cw)/(2*a0), b1=-(1+cw)/a0; LP/HP complement"
    (check examples.gsot.110-biquad-hp/biquad-hp)))

(deftest gsot-111-biquad-bp
  (testing "pp.181 go.biquad.bp — RBJ BP constant skirt: b0=sw/(2*a0), b1=0, b2=-b0; output=b0*(w-w@2)"
    (check examples.gsot.111-biquad-bp/biquad-bp)))

(deftest gsot-112-biquad-res
  (testing "pp.181 go.biquad.res — RBJ peaking EQ: A=10^(gain/40); a0=1+alpha/A; b0=(1+alpha*A)/a0"
    (check examples.gsot.112-biquad-res/biquad-res)))

(deftest gsot-113-biquad-np
  (testing "pp.181 go.biquad.np — RBJ notch: b0=b2=1/a0, b1=a1=-2cw/a0; perfect null at fc"
    (check examples.gsot.113-biquad-np/biquad-np)))

(deftest gsot-114-biquad-ap
  (testing "pp.181 go.biquad.ap — RBJ 2nd-order allpass: b0=(1-alpha)/a0, b2=1.0; flat amplitude, phase shift"
    (check examples.gsot.114-biquad-ap/biquad-ap)))

(deftest gsot-115-biquad-ls
  (testing "pp.181 go.biquad.ls — RBJ low shelf: A=10^(gain/40), sAa=2*sqrt(A)*alpha; separate a0 formula"
    (check examples.gsot.115-biquad-ls/biquad-ls)))

(deftest gsot-116-biquad-hs
  (testing "pp.181 go.biquad.hs — RBJ high shelf: LP shelf reflected to high frequencies; LS/HS are cw-sign duals"
    (check examples.gsot.116-biquad-hs/biquad-hs)))

(deftest gsot-117-biquad-coeffs
  (testing "pp.181 go.biquad.coeffs.gendsp/biquad-coefficients.maxpat — type selector 0-7 (LP/HP/BP/RES/NP/AP/LS/HS) via select2 trees; unified DF-II biquad"
    (check examples.gsot.117-biquad-coeffs/biquad-coeffs)))

(deftest gsot-118-biquad4-lp
  (testing "pp.182-183 go.biquad4.lp — two cascaded LP biquads; H4=H2²; -80dB/decade; shared coefficients, independent DF-II state registers"
    (check examples.gsot.118-biquad4-lp/biquad4-lp)))

(deftest gsot-119-biquad4-hp
  (testing "pp.182-183 go.biquad4.hp — two cascaded HP biquads; complement of biquad4-lp"
    (check examples.gsot.119-biquad4-hp/biquad4-hp)))

(deftest gsot-120-biquad4-bp
  (testing "pp.182-183 go.biquad4.bp — two cascaded BP biquads; b1=0 per stage; narrower pass band than single stage"
    (check examples.gsot.120-biquad4-bp/biquad4-bp)))

(deftest gsot-121-biquad4-res
  (testing "pp.182-183 go.biquad4.res — two cascaded peaking EQ stages; total gain = 2×gain-per-stage in dB"
    (check examples.gsot.121-biquad4-res/biquad4-res)))

(deftest gsot-122-biquad4-np
  (testing "pp.182-183 go.biquad4.np — two cascaded notch stages; deeper null; robust to coefficient quantisation"
    (check examples.gsot.122-biquad4-np/biquad4-np)))

(deftest gsot-123-biquad4-ap
  (testing "pp.182-183 go.biquad4.ap — two cascaded 2nd-order allpass; flat magnitude; doubled phase shift (-2π at fc)"
    (check examples.gsot.123-biquad4-ap/biquad4-ap)))

(deftest gsot-124-biquad4-ls
  (testing "pp.182-183 go.biquad4.ls — two cascaded low-shelf stages; steeper transition; total gain = 2×gain-per-stage"
    (check examples.gsot.124-biquad4-ls/biquad4-ls)))

(deftest gsot-125-biquad4-hs
  (testing "pp.182-183 go.biquad4.hs — two cascaded high-shelf stages; LS/HS cw-sign dual"
    (check examples.gsot.125-biquad4-hs/biquad4-hs)))

(deftest gsot-126-morphing-biquad-cascade
  (testing "pp.182-183 morphing_biquad_cascade.maxpat — LP↔HP morph; closed-form b0=(1+cw*(2m-1))/(2a0), b1=(1-cw-2m)/a0; shared a1/a2; two-stage cascade"
    (check examples.gsot.126-morphing-biquad-cascade/morphing-biquad-cascade)))

(deftest gsot-127-onepole-hz
  (testing "pp.184-188 go.onepole.hz.gendsp — trapezoidal one-pole LP; g=tan(π·fc/SR), k=g/(1+g); y[n]=k*(x+x@1)+(1-2k)*y[n-1]; exact -3dB at fc"
    (check examples.gsot.127-onepole-hz/onepole-hz)))

(deftest gsot-128-svf-coeffs
  (testing "pp.188-190 go.svf.coeffs — SVF coefficients: g=tan(π·fc/SR), k=1/Q, dn=1+k·g+g²; outputs on ch 0-2"
    (check examples.gsot.128-svf-coeffs/svf-coeffs)))

(deftest gsot-129-svf
  (testing "pp.188-190 go.svf.gendsp — ZDF SVF; two coupled ~_ integrators via nested Faust with; LP/BP/HP via :type 0/1/2"
    (check examples.gsot.129-svf/svf)))

(deftest gsot-130-crossover-simple
  (testing "pp.191-192 crossover_simple.maxpat — 1st-order one-pole pair; hp=x-lp exact complement; LP+HP=x; two outputs ch4/ch5"
    (check examples.gsot.130-crossover-simple/crossover-simple)))

(deftest gsot-131-crossover
  (testing "pp.191-192 crossover.maxpat — SVF-based 2nd-order crossover; LP+HP from shared ZDF state; Q=0.707 Butterworth; two outputs ch4/ch5"
    (check examples.gsot.131-crossover/crossover)))

(deftest gsot-132-slewlimit
  (testing "pp.193-194 go.slewlimit — symmetric linear slew limiter; out=prev+clamp(in-prev,-slew,slew); ~_ feedback"
    (check examples.gsot.132-slewlimit/slewlimit)))

(deftest gsot-133-slewlimit2
  (testing "pp.193-194 go.slewlimit2 — asymmetric slew; separate :up/:dn rates; select2(in>_,max(-dn,delta),min(up,delta))"
    (check examples.gsot.133-slewlimit2/slewlimit2)))

(deftest gsot-134-slewlimit-ms
  (testing "p.195 go.slewlimit.ms — ms-parameterized slew; rate=1000/(ms*SR); guard max(1,ms*SR/1000) prevents div-zero"
    (check examples.gsot.134-slewlimit-ms/slewlimit-ms)))

(deftest gsot-135-line-samples
  (testing "p.196 go.line.samples — one-pole lag a=1-1/N; out=(_+(in-_)/N)~_; N in samples; reaches 63% in N samples"
    (check examples.gsot.135-line-samples/line-samples)))

(deftest gsot-136-line-ms
  (testing "p.196 go.line.ms — one-pole lag N=max(1,ms*SR/1000); same IIR as ex.135; sample-rate independent parameter"
    (check examples.gsot.136-line-ms/line-ms)))

(deftest gsot-137-delay-feedforward-basic
  (testing "pp.197-199 delay_feedforward_basic — de.delay circular buffer; y=dry*(1-mx)+wet*mx; feedforward/no-feedback comb filter intro"
    (check examples.gsot.137-delay-feedforward-basic/delay-feedforward-basic)))

(deftest gsot-138-delay-basic-feedback
  (testing "p.200 delay_basic_feedback — IIR echo loop; y=delay(in+fb*y); stable when |fb|<1; comb peaks sharpened by feedback"
    (check examples.gsot.138-delay-basic-feedback/delay-basic-feedback)))

(deftest gsot-139-delay-feedback-decaytime
  (testing "pp.201-202 delay_feedback_decaytime — fb=pow(0.001,delay_ms/decay_ms); RT60 reparameterization; delay-independent decay control"
    (check examples.gsot.139-delay-feedback-decaytime/delay-feedback-decaytime)))

(deftest gsot-140-delay-feedback-filtered
  (testing "pp.202-203 delay_feedback_filtered — one-pole LP in feedback path; fc=exp(-2pi*hz/SR); high freqs decay faster; fi.pole ~ independent of outer fdl_loop ~"
    (check examples.gsot.140-delay-feedback-filtered/delay-feedback-filtered)))

(deftest gsot-141-delay-feedback-filtered-dcblock
  (testing "p.203 delay_feedback_filtered_dcblock — LP + DC blocker in feedback; y=(x-x@1)+0.9999*y; zero at DC; 3 independent ~ states"
    (check examples.gsot.141-delay-feedback-filtered-dcblock/delay-feedback-filtered-dcblock)))

(deftest gsot-142-delay-feedback-saturated
  (testing "p.204 delay_feedback_saturated — tanh(dr*signal) soft-clip in feedback; LP+DC+sat chain; tanh stateless; 3 ~ states from LP/DC/outer loop"
    (check examples.gsot.142-delay-feedback-saturated/delay-feedback-saturated)))

(deftest gsot-143-delay-param-smoothed
  (testing "pp.206-207 delay_param_smoothed — one-pole lag on delay-time param; k=exp(-1000/(st*SR)); smoothed ms into de.delay; Doppler glide not click"
    (check examples.gsot.143-delay-param-smoothed/delay-param-smoothed)))

(deftest gsot-144-delay-morphed-times
  (testing "p.208 delay_morphed_times — two delay targets ta/tb blended by :mo; smooth transition = Doppler pitch shift; :st controls rate"
    (check examples.gsot.144-delay-morphed-times/delay-morphed-times)))

(deftest gsot-145-delay-morphed-times-no-pitch-change
  (testing "pp.209-210 delay_morphed_times_no_pitch_change — smooth crossfade GAIN not delay time; two fixed taps dla/dlb; no read-pointer movement"
    (check examples.gsot.145-delay-morphed-times-no-pitch-change/delay-morphed-times-no-pitch-change)))

(deftest gsot-146-go-background-change
  (testing "p.211 go.background.change — auto crossfade on :tg change; _@1 detection; sample-hold via select2~; xfg ramps 0→1 over :cf ms"
    (check examples.gsot.146-go-background-change/go-background-change)))

(deftest gsot-147-delay-multi-effect
  (testing "pp.211-213 delay_multi_effect — LFO-modulated delay, RT60 feedback, unity-DC-gain LP dampen, DC block, wet/dry mix"
    (check examples.gsot.147-delay-multi-effect/delay-multi-effect)))

(deftest gsot-148-comb-filter
  (testing "p.215 comb_filter — feedforward comb H(z)=1+g·z^{-D}; peaks/notches at 1/D Hz spacing"
    (check examples.gsot.148-comb-filter/comb-filter)))

(deftest gsot-149-comb-enharmonic
  (testing "pp.217-218 comb_enharmonic — two feedforward taps at inharmonic ratios; default t2=t1×φ"
    (check examples.gsot.149-comb-enharmonic/comb-enharmonic)))

(deftest gsot-150-comb-enharmonic-dispersive
  (testing "Alembic ext — dispersive feedback comb; first-order allpass in ~ _ loop; :bp controls inharmonicity"
    (check examples.gsot.150-comb-enharmonic-dispersive/comb-enharmonic-dispersive)))

(deftest gsot-151-string-basic
  (testing "pp.218-219 string_basic — K-S: 0.5*(s+s@1) averaging in ~ _ loop; integer delay D=SR/hz-1"
    (check examples.gsot.151-string-basic/string-basic)))

(deftest gsot-152-string-pitched
  (testing "p.220 string_pitched — pitch-corrected K-S: de.fdelay, D=SR/hz-1.5 compensates averaging 0.5-sample group delay"
    (check examples.gsot.152-string-pitched/string-pitched)))

(deftest gsot-153-string-feedback-control
  (testing "p.221 string_feedback_control — variable K-S LP: (1-dp)*s+dp*s@1; dp=0 bright, dp=0.5 canonical, dp=0.99 dark; pitch-corrected"
    (check examples.gsot.153-string-feedback-control/string-feedback-control)))

(deftest gsot-154-string-inverted-feedback
  (testing "p.221 string_inverted_feedback — negated K-S feedback; odd harmonics only; resonant fundamental at :hz/2; default :hz=440 → A3"
    (check examples.gsot.154-string-inverted-feedback/string-inverted-feedback)))

(deftest gsot-155-string-damping
  (testing "pp.222-223 string_damping — RT60 reparameterisation: fb=pow(0.001,1000/(hz×dc)); pitch-independent decay time"
    (check examples.gsot.155-string-damping/string-damping)))

(deftest gsot-156-string-interp-allpass
  (testing "pp.223-226 delay_interpolation_types — allpass interp: a=(1-f)/(1+f); flat magnitude; nested ~ _ for allpass state"
    (check examples.gsot.156-string-interp-allpass/string-interp-allpass)))

(deftest gsot-157-am
  (testing "pp.227-228 AM — in*(1+mx*osc(fc)); mx=0 dry, mx=1 full AM; RM=AM minus DC; sidebands at fi±fc"
    (check examples.gsot.157-am/am)))

(deftest gsot-158-am-depth
  (testing "pp.229-230 AM-depth — in*(bs+dp*osc); :bs=1→AM, :bs=0→RM; over-modulation :dp>1 adds harmonic distortion"
    (check examples.gsot.158-am-depth/am-depth)))

(deftest gsot-159-amrm
  (testing "p.232 AMRM — explicit :am/:rm/:out outputs; morph (1-mr)*am+mr*rm; AM=RM+dry; sideband amplitudes equal"
    (check examples.gsot.159-amrm/amrm)))

(deftest gsot-160-sine
  (testing "p.233 sine — phasor→sin with explicit PM audio input; sin(2π*phasor(fc)+pm); building block for FM/PM"
    (check examples.gsot.160-sine/sine)))

(deftest gsot-161-fmpm
  (testing "pp.232-233 FMPM — FM/PM morph; sin(2π*phasor(fc+(1-md)*ix*fm*mod)+md*ix*mod); Bessel sidebands; β=ix Chowning"
    (check examples.gsot.161-fmpm/fmpm)))

(deftest gsot-162-fmpm-enveloped
  (testing "pp.234-235 FMPM-enveloped — AR gate→env; iy=ix*env; env*sin(FM/PM(iy)); β decay drives timbral arc dense→sine"
    (check examples.gsot.162-fmpm-enveloped/fmpm-enveloped)))

(deftest gsot-163-fmpm-harmonicity
  (testing "p.236 FMPM-harmonicity — fm=fc×rt; integer rt=harmonic series, irrational=inharmonic/metallic; C:M ratio sweep"
    (check examples.gsot.163-fmpm-harmonicity/fmpm-harmonicity)))

(deftest gsot-164-fmpm-parallel-carriers
  (testing "p.238 parallel-carriers — one mod drives fc1=fm×r1 and fc2=fm×r2; :mx blend; sidebands interleaved by ratio"
    (check examples.gsot.164-fmpm-parallel-carriers/fmpm-parallel-carriers)))

(deftest gsot-165-fmpm-parallel-modulators
  (testing "p.238 parallel-modulators — fm1=fc×r1, fm2=fc×r2 sum into one carrier; intermod at fc+p×fm1+q×fm2; per-mod ix"
    (check examples.gsot.165-fmpm-parallel-modulators/fmpm-parallel-modulators)))

(deftest gsot-166-fmpm-blending
  (testing "pp.239-241 FMPM-blending — (1-bl)*osc+bl*sawtooth mod; saw=harmonic stack; Bessel clusters at each harmonic of fm"
    (check examples.gsot.166-fmpm-blending/fmpm-blending)))

(deftest gsot-167-fmpm-cascade-modulation
  (testing "pp.241-242 cascade — top→middle FM→carrier; multiplicative grid fc+n1*fm1+n1*n2*fm2 vs parallel additive; DX7 series"
    (check examples.gsot.167-fmpm-cascade-modulation/fmpm-cascade-modulation)))

(deftest gsot-168-fmpm-feedback
  (testing "pp.242-243 feedback — fmfb~_; sin(phasor(fc+(1-md)*ix*fc*x)+md*ix*x); sine→sawtooth→chaos; DX7 op1 self-fb"
    (check examples.gsot.168-fmpm-feedback/fmpm-feedback)))

(deftest gsot-169-pm-cross-feedback
  (testing "p.246 cross-feedback — (xfb~si.bus(2)):>*(0.5); osc1↔osc2 PM/FM; sync/phase-lock/chaos regimes"
    (check examples.gsot.169-pm-cross-feedback/pm-cross-feedback)))

(deftest gsot-170-pm-cross-feedback-filtered
  (testing "p.247 cross-feedback-filtered — LP (1-dp)*b:+~*(dp) on each coupling path; defers chaos; 6 state registers"
    (check examples.gsot.170-pm-cross-feedback-filtered/pm-cross-feedback-filtered)))

(deftest gsot-171-fmpm-ifod
  (testing "pp.247-248 FMPM-ifod — in*(bs+FM_osc); FM/PM signal as AM/RM modulator; ring-of-rings at fi±(fc+n*fm)"
    (check examples.gsot.171-fmpm-ifod/fmpm-ifod)))

(deftest gsot-172-pm-noclicks-latched
  (testing "p.248 PM-noclicks-latched — rst=ph<ph@1; (x*rst+_*(1-rst))~_ latches :ix,:rt at cycle boundary; PM click suppress"
    (check examples.gsot.172-pm-noclicks-latched/pm-noclicks-latched)))

(deftest gsot-173-fmpm-waveshaping-modulator
  (testing "pp.249-250 waveshaping-mod — ma.tanh(dr*mo)/ma.tanh(dr); odd-only harmonics; clusters at fc+n*(fm,3fm,5fm…)"
    (check examples.gsot.173-fmpm-waveshaping-modulator/fmpm-waveshaping-modulator)))

(deftest gsot-174-pm-asymmetric
  (testing "pp.251-253 PM-asymmetric — (1+mx*mc)*sin(phasor(fc)+ix*ms); shared phasor; sideband k→Jk*(1+k*mx/ix)"
    (check examples.gsot.174-pm-asymmetric/pm-asymmetric)))

(deftest gsot-175-amrm-bandlimited
  (testing "p.254 AMRM-bandlimited — LP input at SR/2−fc; 0.5 gain; 2x oversampling equivalent"
    (check examples.gsot.175-amrm-bandlimited/amrm-bandlimited)))

(deftest gsot-176-fmpm-carsonrule
  (testing "p.255 FMPM-carsonrule — ix_safe=max(0,(SR/2−fc)/(fc*rt)−1); ix clamped; Carson BW aliasing prevention"
    (check examples.gsot.176-fmpm-carsonrule/fmpm-carsonrule)))

(deftest gsot-177-fmpm-carsonrule-filtered
  (testing "p.256 FMPM-carsonrule-filtered — ix clamp + LP at fc*(1+(ic+1)*rt); parametric Carson bandwidth LP"
    (check examples.gsot.177-fmpm-carsonrule-filtered/fmpm-carsonrule-filtered)))

(deftest gsot-178-fmpm-antialias-filter
  (testing "p.257 FMPM-antialias-filter — fi.lowpass(2,:cf) on FM output; user-controlled ceiling; deliberate aliasing"
    (check examples.gsot.178-fmpm-antialias-filter/fmpm-antialias-filter)))

(deftest gsot-179-pm-is-doppler-delay
  (testing "pp.257-258 PM-is-doppler-delay — de.fdelay(2*SR, SR+da*osc(fm)); τ=ix*SR/(2π*fc); Doppler=PM equivalence"
    (check examples.gsot.179-pm-is-doppler-delay/pm-is-doppler-delay)))

(deftest gsot-180-kp-fm
  (testing "p.259 KP-FM — KS loop with N=SR/(fc+ix*fm*osc(fm)); de.fdelay+LP in feedback; FM delay = PM of KS pitch"
    (check examples.gsot.180-kp-fm/kp-fm)))

(deftest gsot-181-harmonic
  (testing "pp.259-260 harmonic — shared phasor; par(i,8,sin(2π*(i+1)*ph)*bl^i/(i+1)); bl=0→sine, bl=1→1/n sawtooth"
    (check examples.gsot.181-harmonic/harmonic)))

(deftest gsot-182-amrm-blended-harmonics
  (testing "p.260 AMRM-blended-harmonics — harmonic mo→AM/RM (in*(bs+mo)) + PM (sin(ph_fc+ix*mo)); sideband pairs ±n*fm"
    (check examples.gsot.182-amrm-blended-harmonics/amrm-blended-harmonics)))

(deftest gsot-183-modfm
  (testing "pp.262-263 ModFM — triangle mod: asin(osc(fm))/(π/2); odd sidebands 1/n²; β_1≈0.811*ix; FM/PM morph"
    (check examples.gsot.183-modfm/modfm)))

(deftest gsot-184-wavetable-1d
  (testing "pp.265-268 wavetable-1D — rdtable(1024, sinwaveform, i) + linear interp; s0+fr*(s1-s0); bitmask wrap"
    (check examples.gsot.184-wavetable-1d/wavetable-1d)))

(deftest gsot-185-bilinear-indices
  (testing "p.272 bilinear-indices — xf=xp*W−floor(xp*W); yf=wp*H−floor(wp*H); fractional 2D grid coords for bilinear interp"
    (check examples.gsot.185-bilinear-indices/bilinear-indices)))

(deftest gsot-186-bilinear-interpolation
  (testing "p.273 bilinear-interpolation — s00*(1−xf)(1−yf)+s10*xf(1−yf)+s01*(1−xf)yf+s11*xf*yf; area-weighted 4-corner blend"
    (check examples.gsot.186-bilinear-interpolation/bilinear-interpolation)))

(deftest gsot-187-wavetables-2d
  (testing "p.273 wavetables-2D — 8-waveform bank (k+1 harmonics, 1/n rolloff); ba.time init; bilinear interp; :wp morphs sine→saw"
    (check examples.gsot.187-wavetables-2d/wavetables-2d)))

(deftest gsot-188-wavetable-3d
  (testing "pp.273-276 wavetable-3D — 256×4×4 bank; Z=phase offset (z×π/4); 8-corner trilinear interp; :wp/:zp navigate"
    (check examples.gsot.188-wavetable-3d/wavetable-3d)))

(deftest gsot-189-trilinear-interpolation
  (testing "p.276 trilinear-interpolation — 8-corner volume-weighted blend; nested bilinear(z0)+bilinear(z1) then z-lerp"
    (check examples.gsot.189-trilinear-interpolation/trilinear-interpolation)))

(deftest gsot-190-trilinear-indices
  (testing "p.277 trilinear-indices — xf/yf/zf from phasor + :wp/:zp; bitmask x wrap; modulo y/z wrap; W=256 H=D=4"
    (check examples.gsot.190-trilinear-indices/trilinear-indices)))

(deftest gsot-191-wavetable-3d-attractor
  (testing "p.278 wavetable-3D-attractor — Lorenz(σ=10,ρ=28,β=8/3)~si.bus(3); ba.impulse kick; :dt speed; lx/lz→wp/zp"
    (check examples.gsot.191-wavetable-3d-attractor/wavetable-3d-attractor)))

(deftest gsot-192-sinc-interpolate
  (testing "p.290 sinc-interpolate — 4-pt Hann-windowed sinc; sum(k,4,sinc(fr-k)*hann(fr-k)); singularity via max(|x|,1e-9)"
    (check examples.gsot.192-sinc-interpolate/sinc-interpolate)))

(deftest gsot-193-wavetable-sincmipmap-sample
  (testing "p.291 sincmipmap-sample — auto y_mip=int(SR/(2*fc)-1) per sample; sinc X in mipmap row; alias-free at :fc"
    (check examples.gsot.193-wavetable-sincmipmap-sample/wavetable-sincmipmap-sample)))

(deftest gsot-194-wavetable-sincmipmap-wave
  (testing "p.292 sincmipmap-wave — ba.sAndH(ph<ph',y_raw) freezes mipmap per cycle; eliminates intra-cycle row changes"
    (check examples.gsot.194-wavetable-sincmipmap-wave/wavetable-sincmipmap-wave)))

(deftest gsot-195-sinc-interpolate-wave
  (testing "p.293 sinc-interpolate-wave — sinc X + linear Y; sinc_y(y0)*(1-yf)+sinc_y(y1)*yf; 8 reads; :wp waveform morph"
    (check examples.gsot.195-sinc-interpolate-wave/sinc-interpolate-wave)))

(deftest gsot-196-terrain-reader
  (testing "p.295 terrain-reader — wave terrain utility; bilinear T(xp,yp) on 2D harmonic bank; x=audio-rate orbit coord"
    (check examples.gsot.196-terrain-reader/terrain-reader)))

(deftest gsot-197-wave-terrain-osc
  (testing "pp.295-296 wave-terrain-osc — Lissajous orbit (cx,cy) on 2D harmonic bank; :rt ratio + :ph offset control spectrum; inferred from GSOT codebox"
    (check examples.gsot.197-wave-terrain-osc/wave-terrain-osc)))

(deftest gsot-198-waveterrain-generate-bfg
  (testing "pp.298-299 waveterrain-generate-bfg — jit.bfg BFG terrain (jit.scanwrap→jit.buffer→gen~); 4 basis modes w(m,n)=sin(mπx)·sin(nπy); Lissajous orbit"
    (check examples.gsot.198-waveterrain-generate-bfg/waveterrain-generate-bfg)))

(deftest gsot-199-waveterrain-2d-doubleorbit
  (testing "p.301 waveterrain-2d-doubleorbit — compound epicyclic orbit; r1@fc + r2@fc*:rm; limaçon at :rm=2; :ph sets secondary phase; tiling for r1+r2>0.5"
    (check examples.gsot.199-waveterrain-2d-doubleorbit/waveterrain-2d-doubleorbit)))

(deftest gsot-200-waveterrain-2d-carom
  (testing "p.302 waveterrain-2d-carom — billiard-ball reflecting orbit; triangle-wave x/y via 1-|2ph-1|; constant velocity vs Lissajous; :rt ratio :ph start angle"
    (check examples.gsot.200-waveterrain-2d-carom/waveterrain-2d-carom)))

(deftest gsot-201-polygonal
  (testing "pp.303-306 polygonal — regular :ns-gon orbit; edge-trace via vertex lerp; PM from 2× osc; sin wavefold; hard sync noted but not implemented"
    (check examples.gsot.201-polygonal/polygonal)))

(deftest gsot-202-windowed-sync
  (testing "pp.309-314 windowed-sync — resettable slave phasor (select2~); Hann fade-in over :ww ms after each master reset; eliminates sync click artefact"
    (check examples.gsot.202-windowed-sync/windowed-sync)))
