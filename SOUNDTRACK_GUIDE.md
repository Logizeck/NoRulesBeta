# NO RULES — Soundtrack guide

The project is already wired for an optional looping track named `bgm_main.ogg`.

## Fastest way to add music

1. Create/export a seamless OGG Vorbis loop.
2. Name it exactly `bgm_main.ogg`.
3. Put it in `app/src/main/res/raw/`.
4. Rebuild the APK. No Java edits are required.

The game automatically pauses the background track in the rhythm/pitch puzzle rooms (26 and 32), so the puzzle audio stays clear.

## Musical brief

Target: funny, mischievous, quick, memorable — not stressful.

- Tempo: 138–150 BPM.
- Loop length: 24–40 seconds.
- Meter: 4/4 with one or two deliberately awkward syncopations.
- Main motif: 4–6 notes, easy to whistle after one session.
- Instruments: pizzicato strings, marimba/xylophone, plucked shamisen-like sound, toy piano, muted brass/kazoo accents, tiny percussion, woodblocks.
- Avoid: vocals, huge bass, cinematic drums, dense harmony, long intros.
- Structure example: 8 bars motif A → 4 bars silly variation → 8 bars motif A with extra percussion → 4 bar turnaround that loops invisibly.
- Leave headroom: around -14 LUFS integrated is a reasonable mobile-game starting target; avoid clipping.
- Export: OGG Vorbis, stereo, 44.1 or 48 kHz, quality around q5–q7.

## Useful composition trick

Write the melody around a short rhythmic cell such as:

`TA-ta | TA-ta-ta | TAA — TA`

Then repeat the exact rhythm with different notes. The player remembers rhythm first, melody second. Add one deliberately cheeky chromatic note at the end of every second phrase to create the "No Rules" personality.

## Licensing

Use music you own, commission it, or use a library/license that explicitly permits commercial mobile-game distribution and modification. Keep the license/receipt in the project records.
