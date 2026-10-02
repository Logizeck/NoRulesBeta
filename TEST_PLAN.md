# Beta 0.5 test notes

For each level record:
- Solved without hint? Y/N
- Time to first correct insight
- Was the solution logically deducible after seeing it? Y/N
- Was any text hard to read? Y/N + screenshot
- Did any control fail on the device? Y/N + device/Android version
- Fun score 1–5

Special tests:
- L24 Screenshot: Android 14+ hardware screenshot detection.
- L26 Beat Copy: test with media volume low/high and with Bluetooth audio.
- L27 Selfie Trouble: test permission allow/deny, camera cancel, portrait selfie, different camera apps.

## Beta 0.5.1 focus

Test levels 28–35 especially for multi-step clarity. For each room note:
- Did the player understand that several sub-actions were required?
- Did they discover at least one key fragment without an hint?
- Did the final step feel earned rather than arbitrary?
- Was any hidden surprise discovered organically?

Hidden-surprise checks:
- Tap the NO RULES title repeatedly.
- Pester the balloon instead of solving it normally.
- Knock far too many times on the ninja room.
- Trigger the snowman carrot easter egg and verify: speech bubble -> blush -> melt animation -> fail screen -> Retry/Menu.
