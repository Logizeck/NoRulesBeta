# NO RULES! — Beta 0.4 Manga Test Build

Native Android puzzle prototype with **25 test levels**.

## Test goals
- Validate readability on real phones.
- Identify puzzles that create a clean “aha!” moment versus confusion.
- Measure which device interactions are reliable across Android hardware.
- Find difficulty spikes and boring/easy levels before adding monetization.

## Major changes from Beta 0.3
- Larger phone-readable typography using Android scaled density.
- Manga/chibi presentation: speech bubbles, speed lines, exaggerated reactions.
- Removed the old shadow-order puzzle.
- Expanded from 6 to 25 levels.
- Added level select for rapid QA.
- Added 3 test hints and per-level contextual hint text.
- Stores completion status and solve time locally with SharedPreferences.
- No real billing or ads in this test build.

## Monetization scaffold (not active yet)
Planned after testing:
- 3 hints — €0.99
- larger hint bundles
- themed paid level packs
- 100-level expansion / bundle
- optional rewarded ads only as a limited alternative to buying hints

## Build
The included GitHub Actions workflow builds a debug APK on every push to `main`.
Download the `NoRulesBeta-APK` artifact from a successful run.
