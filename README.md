**[⬇ Download the latest release (APK)](https://github.com/JasperRosales/petpal/releases/latest)**

# PetPal

An offline-first habit pet app for Android. Raise a pixel pet that grows from your real steps, and battle friends nearby over Bluetooth or a phone hotspot. No account, and no internet needed to play.

## Docs

| Doc | What's inside |
|---|---|
| [Concept](docs/CONCEPT.md) | How the game works: stages, XP, moves, battles, 1v1 / Duo 2v2, CPU seats |
| [Raids](docs/RAIDS.md) | Co-op 10-floor dungeon: skeletons, wild pets, boss, fainting and rewards |
| [Pets](docs/PETS.md) | All species: types, passives, uniques, HP, full learnsets |
| [Move list](docs/MOVES.md) | Every move with type, damage, range, cooldown, hitbox and effect |
| [Authoring](docs/AUTHORING.md) | How to add a pet, add/change a move, or change passives — plus tests and doc regeneration |
| [Setup](PETPAL_SETUP.md) | Original project setup notes |

## Project

- **Language / UI:** Kotlin Multiplatform + Compose Multiplatform (Material 3), Android first, iOS-ready
- **Libraries:** Room (KMP) + bundled SQLite · Koin · kotlinx-coroutines · kotlinx-datetime · Navigation Compose · KSP
- **Platform:** Android, minSdk 26
- **Network:** battles are local only. The one internet call is an update check on launch against GitHub releases; if a newer APK exists the app offers to download and install it in place (silently skipped when offline)

```
composeApp/src/
  commonMain/  shared code: domain (battle engine, moves, pet logic), data (Room), ui (Compose)
  androidMain/ step sensor, Bluetooth / LAN battle links, MainActivity
  commonTest/  game logic tests
  androidUnitTest/ DocsGenerator (rebuilds docs/MOVES.md and docs/PETS.md)
```

## Build

Requires JDK 17 and the Android SDK.

```bash
./gradlew :composeApp:assembleDebug          # debug APK
./gradlew :composeApp:testDebugUnitTest      # tests
./gradlew :composeApp:assembleRelease        # signed release (needs keystore.properties)

# regenerate the move and pet docs from the game data
PETPAL_DOCS=docs ./gradlew :composeApp:testDebugUnitTest --tests '*DocsGenerator*'
```
