# NO RULES — Beta B0.4

This source build expands the test campaign from 35 to 50 levels and keeps the 12-level themed-area structure.

## Main changes
- 15 new playable puzzles (levels 36–50)
- 5 themed level-select areas (12 slots each)
- additional themed backgrounds for arcade/lab puzzle families
- simulated interstitial ad logic retained: every 5 completed levels OR 3 minutes since the previous ad, shown after a level, closeable after 3 seconds; both counters reset when shown
- startup language selector: English, German, Italian, Spanish, Portuguese, French, Chinese, Japanese, Russian
- startup camera permission flow moved after language choice
- core navigation UI localized; snowman meltdown line and Retry/Menu are localized
- clearer progressive hint system: repeated hints become more explicit
- neon NR launcher icon applied to all density buckets
- stronger common-UI contrast (dark panels + high-contrast text / neon borders)

## Localization status
The localization framework is active and the core navigation / special snowman fail sequence is localized in all 9 test languages. Level-authored titles, subtitles and most puzzle-specific hint prose are still English in B0.4; these are routed through the new language infrastructure for the next translation pass.

## Build
Upload/replace these sources in `Logizeck/NoRulesBeta` and run the existing GitHub Actions workflow. Download `NoRulesBeta-APK` and install `app-debug.apk`.
