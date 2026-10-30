# Background music for the Telebeam demo, generated locally with MusicGen (open model, runs on the PC).
# Usage: python gen_music.py <out_dir> [seconds] [model]
import os, sys, wave
if os.path.isdir(r"G:\VideoLab-Node\hf_home"): os.environ.setdefault("HF_HOME", r"G:\VideoLab-Node\hf_home")  # WORK PC model cache
import numpy as np, torch
from transformers import AutoProcessor, MusicgenForConditionalGeneration

out = sys.argv[1]; secs = float(sys.argv[2]) if len(sys.argv) > 2 else 20; name = sys.argv[3] if len(sys.argv) > 3 else "facebook/musicgen-medium"
os.makedirs(out, exist_ok=True)
dev = "cuda" if torch.cuda.is_available() else "cpu"
proc = AutoProcessor.from_pretrained(name)
model = MusicgenForConditionalGeneration.from_pretrained(name, torch_dtype=torch.float16 if dev == "cuda" else torch.float32).to(dev)
sr = model.config.audio_encoder.sampling_rate
prompts = [
    "soft minimal ambient electronic, warm analog pads, gentle plucked synth arpeggio, light brushed percussion, calm and optimistic, modern tech product demo, 100 bpm, no vocals",
    "gentle lo-fi chillhop, soft electric piano chords, mellow bass, quiet soft drums, warm and relaxed, background music for an app video, 95 bpm, no vocals",
    "airy cinematic ambient, soft piano notes over warm synth pads, subtle pulse, calm, clean, uplifting, minimal, no drums, no vocals",
]
for i, p in enumerate(prompts, 1):
    torch.manual_seed(1000 + i)
    inp = proc(text=[p], padding=True, return_tensors="pt").to(dev)
    with torch.no_grad():
        audio = model.generate(**inp, do_sample=True, guidance_scale=3.0, max_new_tokens=int(secs * 50))
    a = audio[0, 0].float().cpu().numpy()
    a = a / max(1e-6, np.abs(a).max()) * 0.89
    path = os.path.join(out, f"music-{i}.wav")
    with wave.open(path, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr)
        w.writeframes((a * 32767).astype(np.int16).tobytes())
    print(f"MC_PROGRESS {i}/{len(prompts)} take {i} done", flush=True)
    print(f"MC_OUTPUT {path}", flush=True)
