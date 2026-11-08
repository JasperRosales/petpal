# PetPal

An offline-first habit pet app. Your pixel pet grows from your real habits: steps, water, focus sessions, and sleep.

Built with **Kotlin Multiplatform** + **Compose Multiplatform** (Android first, iOS-ready).

## Features
- Personality quiz on first launch picks your one partner pet (one pet per device)
- 10 species, each with 4 stages (Egg, Baby, Kid, Adult), a passive ability and 5 moves + Guard
- Hunger / happiness / energy that decay over time
- Habits: water, sleep, treats and focus refill stats; auto step tracking gives XP
- Daily streaks and a 7-day stats chart
- "Grassy Field" Material 3 theme (light meadow / dark night field) with neumorphic buttons
- Local battles over Bluetooth or a phone hotspot (or vs CPU): pets roam, you time moves like Punch, Tackle, Fire Breath, Thunder, Heal (burn, slow, stun, drain effects)
- Fair XP: only hardware-counted steps (20 XP per 1k, max 20k/day) and online battles (3 rewarded/day)
- Fully offline: no account, no internet (INTERNET permission is only for local hotspot sockets)

## Stack
Room (KMP) · Koin · kotlinx-coroutines · kotlinx-datetime · Navigation Compose · KSP

## Build
```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug
./gradlew :composeApp:testDebugUnitTest
```
Requires JDK 17 and the Android SDK (minSdk 26). See `PETPAL_SETUP.md` for details.
