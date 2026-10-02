# NO RULES — Beta 0.5.0

Test build with 27 puzzle rooms.

## Key changes in 0.5.0
- Fixed Canvas text rendering so labels no longer inherit thick STROKE settings from game artwork.
- Reworked typography for clearer Android phone rendering.
- Added the scarlet / white / black NR pseudo-kanji launcher icon.
- Added Level 26: Beat Copy — listen to a four-tone rhythm and reproduce the timing by tapping.
- Added Level 27: Selfie Trouble — request camera access, capture a selfie, then display it with cartoon glasses and a moustache.
- Existing persistent hint overlay and MENU button retained.

## Privacy / camera test behavior
The selfie room launches the device camera through Android's camera intent. This beta uses the returned preview bitmap in memory for the puzzle and does not intentionally write the image to the app's own storage.

## Build
The included GitHub Actions workflow builds `app-debug.apk` and uploads it as `NoRulesBeta-APK`.

## Beta 0.5.1

- 35 test levels.
- New 3D-style snowman asset and expanded snowman fail/easter-egg sequence.
- New key-fragment hunt rooms: Messy Desk, Toy Box, Kitchen Chaos, Sensor Vault, Master Key Forge.
- New pitch-memory audio room, logic room and rub-to-reveal room.
- Hidden surprise reactions on the title, balloon, ninja and snowman.
- Main menu no longer advertises a fixed level count.
- Text rendering now uses scaled font density for better accessibility/readability.
- Approved scarlet/white/black NR pseudo-kanji launcher icon.
- Optional BGM integration: add `app/src/main/res/raw/bgm_main.ogg` and rebuild.
