#!/usr/bin/env python3
# Autor: Dschonas04 (https://github.com/Dschonas04)
"""Synthesises the mod's own sounds and writes them as mono WAV files.

    python3 tools/sounds.py OUTDIR

Nothing is sampled: every sound is built from sine waves, filtered noise and
envelopes, so the mod carries no third-party audio. Minecraft needs Ogg Vorbis,
so the WAVs are converted afterwards, for example with
    ffmpeg -i siren.wav -c:a libvorbis -q:a 5 siren.ogg
Mono matters: only mono sounds are placed in the world.
"""
import math
import os
import random
import struct
import sys
import wave

RATE = 44100


def write(path, samples):
    peak = max(1e-9, max(abs(s) for s in samples))
    scale = 0.89 / peak
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s * scale)) * 32767)) for s in samples))


def lowpass(samples, cutoff):
    """One-pole low-pass; cutoff in Hz, may be a function of the sample index."""
    out, y = [], 0.0
    for i, s in enumerate(samples):
        c = cutoff(i) if callable(cutoff) else cutoff
        a = 1 - math.exp(-2 * math.pi * c / RATE)
        y += a * (s - y)
        out.append(y)
    return out


def noise(n, seed):
    rnd = random.Random(seed)
    return [rnd.uniform(-1, 1) for _ in range(n)]


def fade(samples, attack=0.005, release=0.02):
    n = len(samples)
    a, r = int(attack * RATE), int(release * RATE)
    for i in range(min(a, n)):
        samples[i] *= i / a
    for i in range(min(r, n)):
        samples[n - 1 - i] *= i / r
    return samples


def siren():
    """One air-raid cycle of four seconds: the rotor tone climbs, holds and sinks again."""
    n = 4 * RATE
    out, phase = [], 0.0
    for i in range(n):
        t = i / n
        rise = math.sin(math.pi * t) ** 0.8  # 0 -> 1 -> 0
        freq = 180 + 560 * rise + 6 * math.sin(2 * math.pi * 5.5 * i / RATE)
        phase += 2 * math.pi * freq / RATE
        # a rotor siren is rich in odd harmonics, softened a little
        s = math.sin(phase) + 0.45 * math.sin(3 * phase) + 0.22 * math.sin(5 * phase) + 0.1 * math.sin(7 * phase)
        out.append(s * (0.35 + 0.65 * rise))
    out = lowpass(out, 3200)
    return fade(out, 0.15, 0.25)


def geiger(seed):
    """A single Geiger-Müller click: a sharp broadband tick with a short metallic ring."""
    n = int(0.045 * RATE)
    rnd = random.Random(seed)
    ring = 3200 + rnd.uniform(-400, 400)
    raw = noise(n, seed)
    out = []
    for i in range(n):
        t = i / RATE
        env = math.exp(-t * 900)
        out.append(raw[i] * env + 0.35 * math.sin(2 * math.pi * ring * t) * math.exp(-t * 160))
    return fade(out, 0.0005, 0.005)


def nuke_blast():
    """The bomb: a crack, the pressure wave as a sub-bass drop, then seconds of rolling thunder."""
    n = int(8.0 * RATE)
    rumble = lowpass(noise(n, 11), lambda i: 70 + 900 * math.exp(-i / RATE * 1.4))
    rumble = lowpass(rumble, 260)
    crackle_src = noise(n, 12)
    rnd = random.Random(13)
    out, phase = [], 0.0
    for i in range(n):
        t = i / RATE
        crack = crackle_src[i] * math.exp(-t * 30) * 0.9
        freq = 26 + 50 * math.exp(-t * 2.2)
        phase += 2 * math.pi * freq / RATE
        sub = math.sin(phase) * math.exp(-t * 0.9) * 1.1
        roll = rumble[i] * 9 * (1 - math.exp(-t * 12)) * math.exp(-t * 0.45) * (1 + 0.35 * math.sin(2 * math.pi * 0.7 * t))
        # debris and secondary pops in the tail
        pop = crackle_src[i] * 0.5 * math.exp(-((t % 0.37) * 40)) * math.exp(-t * 0.8) if t > 0.6 and rnd.random() < 0.6 else 0
        out.append(crack + sub + roll + pop * 0.3)
    return fade(out, 0.001, 1.2)


def beep():
    """The countdown beep of a lit bomb or a timer fuse."""
    n = int(0.11 * RATE)
    out = []
    for i in range(n):
        t = i / RATE
        s = math.sin(2 * math.pi * 1900 * t) + 0.3 * math.sin(2 * math.pi * 3800 * t)
        out.append(s)
    return fade(out, 0.003, 0.02)


def detonator():
    """The remote detonator: a hard switch click, then a rising two-tone confirmation chirp."""
    n = int(0.42 * RATE)
    click = noise(n, 21)
    out = []
    for i in range(n):
        t = i / RATE
        s = click[i] * math.exp(-t * 600) * 1.2
        if 0.08 <= t < 0.22:
            s += 0.6 * math.sin(2 * math.pi * 1300 * t)
        elif 0.24 <= t < 0.4:
            s += 0.6 * math.sin(2 * math.pi * 1950 * t)
        out.append(s)
    return fade(out, 0.0005, 0.03)


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "sounds-wav"
    os.makedirs(out, exist_ok=True)
    write(os.path.join(out, "siren.wav"), siren())
    for k in range(3):
        write(os.path.join(out, f"geiger{k + 1}.wav"), geiger(100 + k))
    write(os.path.join(out, "nuke_blast.wav"), nuke_blast())
    write(os.path.join(out, "beep.wav"), beep())
    write(os.path.join(out, "detonator.wav"), detonator())
    print("wav files in", out)


if __name__ == "__main__":
    main()
