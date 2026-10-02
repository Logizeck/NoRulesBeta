# NO RULES — Beta 0.4.1

25-level Android test build.

## Changes in 0.4.1
- Persistent MENU button on every puzzle.
- Readability overhaul: Android default bold typeface, larger minimum text sizes, higher contrast.
- Hints are now persistent modal cards and remain visible until CLOSE / × is pressed.
- Reworked unclear puzzle logic, especially Room 21 and Room 22.
- 25-level selector remains available for rapid testing.
- No real billing or ads yet; monetization remains deferred until gameplay findings are collected.

## Build
GitHub Actions builds the debug APK automatically on push to main.


## Beta 0.4.3 — Screenshot puzzle
Room 24 now uses Android 14+ ScreenCaptureCallback. The player must take a real device screenshot to solve the room. The app only receives a capture event; it does not receive or inspect the screenshot image.


## New in 0.4.3
Level 21 is now **SNOWMAN FACE**: drag two stones onto the eyes, the carrot onto the nose, and draw a curved smile with a finger. Hidden gag: bringing the carrot to the snowman's backside triggers the speech bubble `no, li no!`, blush, melting animation, and a full level reset.
