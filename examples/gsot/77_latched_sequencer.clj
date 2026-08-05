; SPDX-License-Identifier: EPL-2.0
(ns examples.gsot.77-latched-sequencer
  "GSOT pp.126 — latched-sequencer.maxpat.

  'Sample and hold patterns'
  --------------------------
  Where mixer-sequencer (example 76) reads pitch values from fixed
  parameters, the latched sequencer CAPTURES its sequence from an audio
  input over time.  Eight independent sample-and-hold cells each latch
  the cv-in signal at the moment their step fires.

  The sequence is undefined at startup (all cells initialise at 0.0) and
  fills in as triggers arrive.  After 8 triggers the sequence is fully
  defined and repeats.  Running a random or evolving signal into cv-in
  while the sequencer cycles produces a melodic phrase that changes
  gradually — each pass around the ring potentially updates one or more
  step values with whatever cv-in happens to be at that moment.

  Signal flow
  -----------
  For step N (N = 0..7):
      fire_N = float(trig > 0.5) × float(int(counter) == N)
      held_N = track-hold(cv-in, fire_N)

  On each trigger the counter advances to a new N, fire_N pulses for one
  sample, and held_N captures cv-in.  The pitch output immediately
  reflects the new held value for that step.

  The binary tree that reads back from the 8 held cells is the same as
  example 76 (select2 on counter thresholds).

  Comparison with mixer-sequencer
  ---------------------------------
  mixer-sequencer (76):  pitches are fixed params — the sequence is set
      in advance and does not change unless the user edits the params.

  latched-sequencer (77): pitches are captured from cv-in — the sequence
      evolves continuously.  Connecting a slow random source (e.g., the
      random-smoothed output from example 57) creates a self-modifying
      melodic pattern.  Connecting a second sequencer's output creates
      a 'shadow' that copies and loops sections of the upstream sequence.

  Audio inputs
  ------------
  audio-in 0: trig   — clock input; one trigger advances one step
  audio-in 1: cv-in  — signal to sample; typically pitch CV or scaled
                        noise in [-1, 1] (use autolimit, example 71, when
                        feeding Lorenz/Liu-Chen output directly)

  Parameters
  ----------
  :steps — active step count 1–8; counter wraps at this value (default 8)

  Emitted Faust DSP (abbreviated):
      import(\"stdfaust.lib\");

      alembic_dsp(n0, n1) = n_pt, n_gt
        with {
          n_ns  = hslider(\"steps\", 8.0, 1.0, 8.0, 1.0);
          n_ct  = (select2(n0>0.5,_,float(int(_+1.0)%max(1,int(n_ns))))~_);
          n_f0  = float(n0>0.5)*float(int(n_ct)==0);
          n_h0  = (select2(n_f0>0.5,_,n1)~_);
          ...   (fire/hold repeated for steps 1-7 with int(n_ct)==N)
          n_pt  = select2(n_ct<4.0,
                    select2(n_ct<6.0,...),
                    select2(n_ct<2.0,...));
          n_gt  = float(n0>0.5);
        };
      process = alembic_dsp;

  n0 = trig (audio-in 0), n1 = cv-in (audio-in 1)"
  (:require [alembic.patch :refer [defpatch!]]))

(defpatch! latched-sequencer
  {:params {:steps {:range [1.0 8.0] :default 8.0}}}
  (let [trig   (audio-in)
        cv-in  (audio-in)
        steps  (param :steps)
        counter (faust "(select2(%{tr}>0.5,_,float(int(_+1.0)%max(1,int(%{ns}))))~_)"
                       {:tr trig :ns steps})
        ; Per-step fire: pulses for one sample when trig fires on that step
        fire0  (faust "float(%{tr}>0.5)*float(int(%{ct})==0)" {:tr trig :ct counter})
        fire1  (faust "float(%{tr}>0.5)*float(int(%{ct})==1)" {:tr trig :ct counter})
        fire2  (faust "float(%{tr}>0.5)*float(int(%{ct})==2)" {:tr trig :ct counter})
        fire3  (faust "float(%{tr}>0.5)*float(int(%{ct})==3)" {:tr trig :ct counter})
        fire4  (faust "float(%{tr}>0.5)*float(int(%{ct})==4)" {:tr trig :ct counter})
        fire5  (faust "float(%{tr}>0.5)*float(int(%{ct})==5)" {:tr trig :ct counter})
        fire6  (faust "float(%{tr}>0.5)*float(int(%{ct})==6)" {:tr trig :ct counter})
        fire7  (faust "float(%{tr}>0.5)*float(int(%{ct})==7)" {:tr trig :ct counter})
        ; Per-step S&H cells: each holds its last captured cv-in value
        held0  (track-hold cv-in fire0)
        held1  (track-hold cv-in fire1)
        held2  (track-hold cv-in fire2)
        held3  (track-hold cv-in fire3)
        held4  (track-hold cv-in fire4)
        held5  (track-hold cv-in fire5)
        held6  (track-hold cv-in fire6)
        held7  (track-hold cv-in fire7)
        ; Binary-tree readback: select the current step's held value
        pitch  (faust "select2(%{ct}<4.0,select2(%{ct}<6.0,select2(%{ct}<5.0,%{h4},%{h5}),select2(%{ct}<7.0,%{h6},%{h7})),select2(%{ct}<2.0,select2(%{ct}<1.0,%{h0},%{h1}),select2(%{ct}<3.0,%{h2},%{h3})))"
                      {:ct counter
                       :h0 held0 :h1 held1 :h2 held2 :h3 held3
                       :h4 held4 :h5 held5 :h6 held6 :h7 held7})
        gate   (faust "float(%{tr}>0.5)" {:tr trig})]
    (output :pitch pitch)
    (output :gate  gate)))
