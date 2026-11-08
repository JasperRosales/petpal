# PetPal

An offline-first habit pet app. Your pixel pet grows from your real habits: steps, water, focus sessions, and sleep.

Built with **Kotlin Multiplatform** + **Compose Multiplatform** (Android first, iOS-ready).

## Features
- Pixel pet that evolves (Egg → Baby → Teen → Adult) and changes mood
- Hunger / happiness / energy that decay over time
- Habits: water, sleep, treats, focus timer, and auto step tracking (+XP per 1,000 steps)
- Daily streaks and a 7-day stats chart
- "Grassy Field" Material 3 theme (light meadow / dark night field) with neumorphic buttons
- Fully offline: no network permission, no account

## Stack
Room (KMP) · Koin · kotlinx-coroutines · kotlinx-datetime · Navigation Compose · KSP

## Build
```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug
./gradlew :composeApp:testDebugUnitTest
```
Requires JDK 17 and the Android SDK (minSdk 26). See `PETPAL_SETUP.md` for details.
