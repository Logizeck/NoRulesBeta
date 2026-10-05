# No Rules Beta — Godot rebuild

This branch/project is a clean rebuild for the 5-level English-only beta.

## Core rule
No functional UI is baked into background images. Backgrounds are scenery only.
Every interactive object is a real scene/control with its own behavior.

## Reusable components
- `NeonUI`: shared visual language for panels/buttons/labels.
- `DraggableScalable`: drag + multitouch pinch scaling for keys, photos, plugs, puzzle pieces, etc.
- `TiltMover`: accelerometer-driven movement for balls, marbles, droplets, vehicles, etc.
- `HoldToTrigger`: hold-duration interaction for balloons, switches, pressure pads, charging, fuses, etc.
- `IdleTrigger`: "do nothing" / inactivity mechanic reusable for doors, sleeping characters, stealth, timing puzzles.

## Beta levels
1. Key Problem — pinch the key smaller, then drag it to the lock.
2. Gravity Star — tilt the phone to move the ball into the star.
3. Pop! — press and hold to inflate and burst the balloon.
4. Zen Door — do nothing for 7 seconds.
5. Liar Chibi — ignore the instruction and press RIGHT.

## Structure
The first version creates most content in code to keep the beta portable and easy to audit.
As the project grows, reusable mechanics can be promoted to `.tscn` prefab-like scenes without changing level logic.

## Run
Open in Godot 4.3+ and run `scenes/Main.tscn`.

## Android
Install Godot Android export templates and Android SDK/JDK, then export the `Android` preset.
