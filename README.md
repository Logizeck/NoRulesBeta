# NO RULES — Beta B0.5.1

Build-fix release for B0.5 gameplay rework.

## Build fix
- Restored the `lastMoveX` interaction-state field used by the Rub It Out gesture handler. Its removal during the B0.5 refactor caused Java compilation to fail in GitHub Actions.
- Version bumped to 11 / B0.5.1.


Gameplay-rework build focused on variety, physical interaction, camera integration and stronger puzzle logic.

## Main changes
- Level 20: Count It now has a second decoding step instead of a trivial direct count.
- Level 23: HOLD is now truly momentary. Releasing the finger immediately closes the gate.
- Level 27: internal NO RULES camera screen with live front-camera preview, face guide, face-alignment feedback and in-app shutter. Post-shot accessories are aligned using face detection and redesigned as a neon star lens, spiral lens and curled cartoon moustache.
- Level 28: Messy Desk rebuilt as a physical clutter puzzle. Drawer, mug and notebook are draggable and reveal digits that physically exist underneath them; enter the discovered code.
- Level 29: Toy Box replaced by a tilt-controlled circular maze.
- Level 30: Kitchen Chaos replaced by Piñata Panic. The piñata swings using gravity/accelerometer plus gyroscope contribution; tap the moving target repeatedly until it breaks.
- Level 31: Sensor Vault replaced by Perspective Trick, a direct alignment puzzle with movable visual layers.
- Level 32: Lock Pins replaced by four colored direct-drag pistons with independent target notches.
- Level 33: order is now based on the number of interior angles (vertices), not the old odd/even rule.
- Level 35: Master Key mechanisms are more physical: sliding hatch, progressively cracking cover, hanging pull release, manual part assembly. No tilt requirement.
- Level 37: Safe Dial replaced by Circuit Light: battery -> four rotatable wire modules -> lamp. The lamp glows only when all four modules complete the circuit.
- Snowman easter-egg/fail work from B0.5 WIP retained: requires both eyes, localized warning line, scream, cartoon eye-pop, cleaner single-puddle melt and one simulated ad after the first meltdown of the session.
- Fake interstitial logic retained: after 5 completed levels OR 3 minutes since last ad, shown only after a level; close after 3 seconds and reset both counters.

## Camera
Camera permission is still requested after the initial language choice. The selfie level now opens an internal NO RULES photo-booth activity rather than handing control to the generic system camera UI.

## Build status
This package is source code. Upload/replace it in `Logizeck/NoRulesBeta`, run the existing GitHub Actions workflow and download the `NoRulesBeta-APK` artifact (`app-debug.apk`).

## Release target note
For a public release within one month, this should be treated as a gameplay validation build rather than a release candidate. The remaining priority work is: full localization, visual polish/animation pass, device compatibility testing, crash handling, privacy/store assets and final monetization integration.