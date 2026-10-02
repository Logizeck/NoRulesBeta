# NO RULES — Beta 0.3

Native Android puzzle-game prototype with six cartoon-style rooms.

## Current rooms

1. **Oversized key** — pinch the key smaller, then drag it into the lock.
2. **Gravity maze** — tilt the phone to move the ball into the star target using the real Android accelerometer.
3. **Balloon** — long-press until it pops and reveals the key.
4. **Patience** — do not touch the screen for seven seconds.
5. **Shadows** — infer the correct character order from the cast-shadow lengths.
6. **Three eyes** — cover all three eyes simultaneously with three fingers.

The project uses native Android APIs only and locks gameplay to portrait orientation.

## Automatic APK build

A GitHub Actions workflow is included at:

`.github/workflows/build-apk.yml`

Every push to `main` builds a debug APK. In GitHub open **Actions → Build Android APK → latest successful run → Artifacts → NoRulesBeta-APK**.

The downloaded ZIP contains `app-debug.apk`, which can be installed on an Android device after allowing installation from the browser/file manager used to open it.
