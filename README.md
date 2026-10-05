# NO RULES — Unity B1 Preview 5

This is a clean Unity/C# rewrite of the first five approved NO RULES levels. There is no hand-written Java source in this project.

## Unity version
Open with **Unity 2022.3 LTS** (project file targets 2022.3.62f1). Android Build Support must be installed in Unity Hub.

## Included playable levels
1. **Key Problem** — pinch the large golden key smaller, then drag it to the door.
2. **Tiny Maze** — tilt the physical phone to roll the ball to the glowing centre.
3. **Color Splash** — memorize the 3×3 neon paint pattern, select a paint bucket, then repaint the grid.
4. **Zen Mode** — do not touch the play area for seven seconds; touching resets the timer.
5. **Quiet Please** — reduce the Android media volume to zero to silence the noisy room.

## Visual implementation
The approved neon concepts are used as the visual base. The upper UI is recreated in Unity, while gameplay art stays very close to the approved mockups. The HINT button is always visible; solutions are not displayed until HINT is pressed.

## Build an APK
1. Open the folder in Unity Hub.
2. If Unity asks to upgrade the exact patch version, accept within 2022.3 LTS.
3. Open `Assets/Scenes/Main.unity`.
4. Choose **NO RULES > Configure Android Preview** once (the project also runs this automatically in the Editor).
5. File > Build Settings > Android > Switch Platform.
6. Click **Build** and choose an APK filename.

The preview targets portrait orientation and Android API 23+.

## Notes
- Device tilt and Android media-volume logic must be tested on a physical Android phone.
- The current five-level preview intentionally focuses on the Unity migration and approved visual direction before the remaining levels are migrated.
