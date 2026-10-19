# Ramayana — kids' story game (Android, Kotlin)

## Versioning rule (mandatory)
Every code change must bump the app version in `app/build.gradle.kts`:
- `versionCode` — always +1.
- `versionName` — minor bump for fixes/tweaks (1.1 → 1.2), major bump for big features (1.x → 2.0).
- Add a line to the changelog below with the new version and what changed.

## Changelog
- **2.3 (6)** — New launcher and Play Store icon: Shri Ram drawing his bow on a saffron sunburst (chosen from three designs in `play-store/icon-options/`).
- **2.2 (5)** — Long button labels (e.g. "Continue – Stage 1 · Level 1") now shrink to fit. New launcher icon (Shri Ram on a saffron sunburst, adaptive + round). Debug-only hooks to export store art and 4K screenshots (`--ez promo true`, `--es shot WxH --ef delay s --ei progress n`). Play Store listing, privacy policy and graphics in `play-store/`.
- **2.1 (4)** — Swayamvar (Stage 3): Levels 1 and 2 now end with Ram holding the strung bow high; Shiva's bow only breaks on Level 3, just before the wedding.
- **2.0 (3)** — Every stage now has 3 levels unlocked strictly in order (Level 1 → 2 → 3 → next stage); each level is faster with a bigger goal. New per-level "Did you know?" knowledge page and a recap of the previous lesson. Story, missions and lessons are read aloud by an Indian-English narrator (Voice On/Off in menu and story screen). Stars now out of 90.
- **1.1 (2)** — Left/right arms now mirror each other on all characters (elbows bend outward, hands drawn as a true left/right pair).
- **1.0 (1)** — Initial release: 10 timed stages from Ram's childhood to Ravan, Sita vivah, Hanuman Shakti, Diwali finale, save/resume, procedural music and cel-shaded art.

## Build
- JDK: `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`
- `./gradlew assembleDebug --offline`
