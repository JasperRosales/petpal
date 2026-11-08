# PetPal: Project Setup (Kotlin Multiplatform)

> An offline-first habit pet app. Your pixel pet grows from your real habits: steps, water, focus sessions, and sleep.

This document sets up the **PetPal** codebase with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**. Android is the main target. The shared code is laid out so an iOS app can be added later without restructuring.

---

## 1. Tech Stack

| Layer | Library | Purpose |
|---|---|---|
| Language | Kotlin Multiplatform | Shared logic across platforms |
| UI | Compose Multiplatform + Material 3 | Shared UI (Android first) |
| Icons | Material Icons (Rounded) | Clean, consistent iconography |
| Theme | Custom "Grassy Field" color scheme | Light: meadow; Dark: night field |
| Database | Room (KMP) + Bundled SQLite driver | Offline local storage |
| Annotation processing | KSP | Room code generation |
| DI | Koin | Dependency injection |
| Async | kotlinx-coroutines | Flows and background work |
| Date/time | kotlinx-datetime | Day boundaries, streaks |
| ViewModel | JetBrains lifecycle-viewmodel-compose | Shared ViewModels |
| Navigation | JetBrains navigation-compose | Screen navigation |
| Android-only | SensorManager | Step counter |

No backend, no network permission, no account.

---

## 2. Prerequisites

- **Android Studio** (latest stable) with the **Kotlin Multiplatform** plugin
- **JDK 17** or newer
- Android SDK with **minSdk 26**, **target/compileSdk = latest stable**
- A physical Android device is recommended for testing the step sensor (most emulators don't have one)

---

## 3. Create the Project

1. Open the **Kotlin Multiplatform wizard**: <https://kmp.jetbrains.com>
   (or *File → New → New Project → Kotlin Multiplatform* in Android Studio)
2. Configure:
   - **Project name:** `PetPal`
   - **Project ID:** `com.petpal.app`
   - **Targets:** ✅ Android (✅ iOS optional; leave unchecked if Android-only for now)
   - **UI:** Share UI (Compose Multiplatform)
3. Download, unzip, and open it in Android Studio.
4. Let Gradle sync, then run `composeApp` on a device to confirm the template works.

---

## 4. Project Structure

```
PetPal/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml
└── composeApp/
    ├── build.gradle.kts
    ├── schemas/                      # Room schema exports
    └── src/
        ├── commonMain/kotlin/com/petpal/app/
        │   ├── App.kt                # Root composable
        │   ├── di/
        │   │   └── AppModule.kt
        │   ├── domain/
        │   │   ├── model/            # Pet, Habit, Mood, Stage…
        │   │   ├── logic/            # PetEngine (decay, mood, growth)
        │   │   └── platform/         # StepCounter, TimeProvider interfaces
        │   ├── data/
        │   │   ├── local/            # Room database, entities, DAOs
        │   │   └── repository/       # PetRepository, HabitRepository
        │   └── ui/
        │       ├── home/             # HomeScreen + HomeViewModel
        │       ├── focus/            # Focus timer
        │       ├── habits/
        │       ├── components/       # PetSprite, StatBar…
        │       └── theme/            # Color.kt, Theme.kt, Type.kt, Shape.kt, PetPalIcons.kt
        ├── commonMain/composeResources/
        │   ├── drawable/             # Pet sprites (PNG)
        │   └── font/                 # Pixel font
        ├── commonTest/kotlin/        # Unit tests for PetEngine
        └── androidMain/kotlin/com/petpal/app/
            ├── MainActivity.kt
            ├── PetPalApplication.kt
            ├── di/AndroidModule.kt
            ├── data/DatabaseBuilder.android.kt
            └── platform/AndroidStepCounter.kt
```

---

## 5. Gradle Configuration

### 5.1 `gradle/libs.versions.toml`

> ⚠️ The versions below are **placeholders**. Keep whatever the wizard generated for Kotlin, AGP, and Compose, and update the others to their latest stable releases. KSP must match your Kotlin version.

```toml
[versions]
kotlin = "x.y.z"                  # from wizard
agp = "x.y.z"                     # from wizard
compose-multiplatform = "x.y.z"   # from wizard
ksp = "x.y.z-a.b.c"               # must match Kotlin version
room = "x.y.z"                    # 2.7+ required for KMP
sqlite = "x.y.z"
koin = "x.y.z"
coroutines = "x.y.z"
datetime = "x.y.z"
lifecycle = "x.y.z"               # org.jetbrains.androidx.lifecycle
navigation = "x.y.z"              # org.jetbrains.androidx.navigation

[libraries]
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }

koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }

kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "datetime" }

lifecycle-viewmodel-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation" }

[plugins]
# keep the wizard-generated plugin entries, then add:
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
room = { id = "androidx.room", version.ref = "room" }
```

### 5.2 Root `build.gradle.kts`

Add the new plugins with `apply false`:

```kotlin
plugins {
    // ...wizard-generated plugins
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
```

### 5.3 `composeApp/build.gradle.kts`

```kotlin
plugins {
    // ...wizard-generated plugins
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    androidTarget()
    // iosX64(); iosArm64(); iosSimulatorArm64()   // enable later for iOS

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)   // Material Icons (see §7.3)

            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.navigation.compose)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.petpal.app"
    defaultConfig {
        applicationId = "com.petpal.app"
        minSdk = 26
        // targetSdk / compileSdk: keep wizard values
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    // add("kspIosArm64", libs.room.compiler)  // when iOS is enabled
}
```

Sync Gradle before continuing.

---

## 6. Starter Code

### 6.1 Domain Models (`commonMain/domain/model/`)

```kotlin
package com.petpal.app.domain.model

enum class Stage { EGG, BABY, TEEN, ADULT }
enum class EvolutionPath { NONE, ATHLETIC, SCHOLAR, CHILL }
enum class Mood { HAPPY, OKAY, SAD, SLEEPY }
enum class HabitType { STEPS, WATER, FOCUS, SLEEP, CUSTOM }

data class Pet(
    val id: Long = 1,
    val name: String,
    val stage: Stage = Stage.EGG,
    val path: EvolutionPath = EvolutionPath.NONE,
    val hunger: Double = 80.0,
    val happiness: Double = 80.0,
    val energy: Double = 80.0,
    val xp: Int = 0,
    val lastUpdated: Long,   // epoch millis
)
```

### 6.2 Platform Interfaces (`commonMain/domain/platform/`)

```kotlin
package com.petpal.app.domain.platform

import kotlinx.coroutines.flow.Flow

interface StepCounter {
    val isAvailable: Boolean
    /** Total steps reported by the sensor since device boot. */
    fun stepsSinceBoot(): Flow<Long>
}

fun interface TimeProvider {
    fun nowMillis(): Long
}
```

### 6.3 Pet Engine (`commonMain/domain/logic/PetEngine.kt`)

```kotlin
package com.petpal.app.domain.logic

import com.petpal.app.domain.model.*

object PetEngine {
    private const val HUNGER_DECAY = 4.0      // per hour
    private const val HAPPINESS_DECAY = 3.0
    private const val ENERGY_DECAY = 2.0
    private const val MS_PER_HOUR = 3_600_000.0

    fun applyDecay(pet: Pet, now: Long): Pet {
        if (now <= pet.lastUpdated) return pet          // ignore clock rollback
        val hours = (now - pet.lastUpdated) / MS_PER_HOUR
        return pet.copy(
            hunger = (pet.hunger - HUNGER_DECAY * hours).clampStat(),
            happiness = (pet.happiness - HAPPINESS_DECAY * hours).clampStat(),
            energy = (pet.energy - ENERGY_DECAY * hours).clampStat(),
            lastUpdated = now,
        )
    }

    fun moodOf(pet: Pet): Mood = when {
        pet.energy < 15 -> Mood.SLEEPY
        minOf(pet.hunger, pet.happiness, pet.energy) < 30 -> Mood.SAD
        minOf(pet.hunger, pet.happiness, pet.energy) >= 70 -> Mood.HAPPY
        else -> Mood.OKAY
    }

    fun stageFor(xp: Int): Stage = when {
        xp >= 1500 -> Stage.ADULT
        xp >= 500 -> Stage.TEEN
        xp >= 50 -> Stage.BABY
        else -> Stage.EGG
    }

    fun reward(pet: Pet, type: HabitType): Pet {
        val updated = when (type) {
            HabitType.WATER -> pet.copy(hunger = (pet.hunger + 10).clampStat(), xp = pet.xp + 10)
            HabitType.STEPS -> pet.copy(energy = (pet.energy + 15).clampStat(), xp = pet.xp + 20)
            HabitType.FOCUS -> pet.copy(happiness = (pet.happiness + 15).clampStat(), xp = pet.xp + 25)
            HabitType.SLEEP -> pet.copy(energy = 100.0, xp = pet.xp + 20)
            HabitType.CUSTOM -> pet.copy(happiness = (pet.happiness + 10).clampStat(), xp = pet.xp + 10)
        }
        return updated.copy(stage = stageFor(updated.xp))
    }

    private fun Double.clampStat() = coerceIn(0.0, 100.0)
}
```

### 6.4 Room Database (`commonMain/data/local/`)

```kotlin
package com.petpal.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pet")
data class PetEntity(
    @PrimaryKey val id: Long = 1,
    val name: String,
    val stage: String,
    val path: String,
    val hunger: Double,
    val happiness: Double,
    val energy: Double,
    val xp: Int,
    val lastUpdated: Long,
)

@Entity(tableName = "habit_log")
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val value: Int,
    val timestamp: Long,
)

@Dao
interface PetDao {
    @Query("SELECT * FROM pet WHERE id = 1")
    fun observePet(): Flow<PetEntity?>

    @Query("SELECT * FROM pet WHERE id = 1")
    suspend fun getPet(): PetEntity?

    @Upsert
    suspend fun upsert(pet: PetEntity)
}

@Dao
interface HabitLogDao {
    @Insert
    suspend fun insert(log: HabitLogEntity)

    @Query("SELECT * FROM habit_log WHERE timestamp >= :from ORDER BY timestamp DESC")
    fun observeSince(from: Long): Flow<List<HabitLogEntity>>
}
```

```kotlin
package com.petpal.app.data.local

import androidx.room.*
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [PetEntity::class, HabitLogEntity::class], version = 1)
@ConstructedBy(PetPalDatabaseConstructor::class)
abstract class PetPalDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun habitLogDao(): HabitLogDao
}

// Room's KSP processor generates the actual implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object PetPalDatabaseConstructor : RoomDatabaseConstructor<PetPalDatabase> {
    override fun initialize(): PetPalDatabase
}

fun buildDatabase(builder: RoomDatabase.Builder<PetPalDatabase>): PetPalDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
```

**Android builder** (`androidMain/data/DatabaseBuilder.android.kt`):

```kotlin
package com.petpal.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.petpal.app.data.local.PetPalDatabase

fun databaseBuilder(context: Context): RoomDatabase.Builder<PetPalDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("petpal.db")
    return Room.databaseBuilder<PetPalDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
```

### 6.5 Android Step Counter (`androidMain/platform/AndroidStepCounter.kt`)

```kotlin
package com.petpal.app.platform

import android.content.Context
import android.hardware.*
import com.petpal.app.domain.platform.StepCounter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

class AndroidStepCounter(context: Context) : StepCounter {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    override val isAvailable: Boolean get() = sensor != null

    override fun stepsSinceBoot(): Flow<Long> = callbackFlow {
        val s = sensor ?: run { close(); return@callbackFlow }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values[0].toLong())
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, s, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
```

> `TYPE_STEP_COUNTER` resets on reboot. Store a daily baseline in the database and compute `todaySteps = current - baseline`.

### 6.6 Dependency Injection

**`commonMain/di/AppModule.kt`**

```kotlin
package com.petpal.app.di

import com.petpal.app.data.local.PetPalDatabase
import com.petpal.app.domain.platform.TimeProvider
import org.koin.dsl.module

val sharedModule = module {
    single<TimeProvider> { TimeProvider { kotlinx.datetime.Clock.System.now().toEpochMilliseconds() } }
    single { get<PetPalDatabase>().petDao() }
    single { get<PetPalDatabase>().habitLogDao() }
    // single { PetRepository(get(), get(), get()) }
    // viewModel { HomeViewModel(get()) }
}
```

> If your kotlinx-datetime version moved `Clock` to the standard library, use `kotlin.time.Clock.System` instead.

**`androidMain/di/AndroidModule.kt`**

```kotlin
package com.petpal.app.di

import com.petpal.app.data.databaseBuilder
import com.petpal.app.data.local.buildDatabase
import com.petpal.app.domain.platform.StepCounter
import com.petpal.app.platform.AndroidStepCounter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { buildDatabase(databaseBuilder(androidContext())) }
    single<StepCounter> { AndroidStepCounter(androidContext()) }
}
```

**`androidMain/PetPalApplication.kt`**

```kotlin
package com.petpal.app

import android.app.Application
import com.petpal.app.di.androidModule
import com.petpal.app.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PetPalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PetPalApplication)
            modules(sharedModule, androidModule)
        }
    }
}
```

### 6.7 Android Manifest (`androidMain/AndroidManifest.xml`)

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Step counter (runtime permission on Android 10+) -->
    <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
    <!-- Local reminders (runtime permission on Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <uses-feature android:name="android.hardware.sensor.stepcounter" android:required="false" />

    <!-- No INTERNET permission: PetPal is fully offline -->

    <application
        android:name=".PetPalApplication"
        android:label="PetPal"
        android:icon="@mipmap/ic_launcher"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### 6.8 First Unit Test (`commonTest/PetEngineTest.kt`)

```kotlin
package com.petpal.app

import com.petpal.app.domain.logic.PetEngine
import com.petpal.app.domain.model.*
import kotlin.test.*

class PetEngineTest {
    private val base = Pet(name = "Mochi", lastUpdated = 0L)

    @Test fun decaysOverTwoHours() {
        val pet = PetEngine.applyDecay(base, now = 2 * 3_600_000L)
        assertEquals(72.0, pet.hunger)
        assertEquals(74.0, pet.happiness)
        assertEquals(76.0, pet.energy)
    }

    @Test fun ignoresClockRollback() {
        val pet = base.copy(lastUpdated = 10_000L)
        assertEquals(pet, PetEngine.applyDecay(pet, now = 5_000L))
    }

    @Test fun neverGoesBelowZero() {
        val pet = PetEngine.applyDecay(base, now = 1_000 * 3_600_000L)
        assertEquals(0.0, pet.hunger)
    }
}
```

---

## 7. Design System: "Grassy Field"

### 7.1 Design Principles

- **Clean and calm:** lots of whitespace, soft rounded cards, no heavy shadows (tonal elevation only)
- **Nature palette:** meadow green, clear sky blue, warm sun yellow, soft soil brown
- **The pet is the hero:** the pixel pet sits on a grassy hill in the middle of the screen; everything else stays quiet around it
- **One icon style:** Material Icons **Rounded** everywhere, matching the rounded shapes
- **Contrast:** the pixel-art pet is the only "retro" element; the UI itself is modern Material 3

### 7.2 Color Palette

| Role | Light (Meadow) | Dark (Night Field) | Used for |
|---|---|---|---|
| Primary | `#3A7D44` Meadow Green | `#8FD694` Moonlit Grass | Main buttons, active nav, progress |
| On Primary | `#FFFFFF` | `#0B3912` | Text/icons on primary |
| Primary Container | `#C4EFC0` Fresh Leaf | `#22602D` | Selected chips, highlighted cards |
| Secondary | `#4A90B8` Clear Sky | `#9CCBEA` Night Sky | Water, focus timer accents |
| Secondary Container | `#D3EBF8` Light Sky | `#1F4D66` | Water card background |
| Tertiary | `#E8A33D` Sunflower | `#F5C97A` Moon | XP, streaks, rewards |
| Tertiary Container | `#FFE7BF` Morning Sun | `#5C3F0F` | Reward banners |
| Background | `#F6FBF2` Morning Dew | `#111A13` Night Field | Screen background |
| Surface | `#FFFFFF` | `#18231A` | Cards |
| Surface Variant | `#E4EDDD` Pale Grass | `#2C3A2E` | Stat bar tracks |
| Outline | `#B7C4B0` | `#4B5B4D` | Dividers, borders |
| Error | `#C8553D` Clay | `#F2A08E` | Very low stats |
| Soil (custom) | `#8B6B4A` | `#6E5640` | Ground under the pet |

**Stat bar colors:** Hunger → Tertiary (sun), Happiness → Primary (green), Energy → Secondary (sky). Bars switch to Error when below 30.

### 7.3 Material Icons

`compose.materialIconsExtended` gives the full Material icon set in Compose Multiplatform. Use the **Rounded** variants for a soft, clean look.

> Note: the extended icons artifact is large and is no longer updated in newer Compose Multiplatform releases. If it's unavailable or you want a smaller APK, download the icons you need as **Material Symbols (Rounded)** SVGs from <https://fonts.google.com/icons>, convert them to Android Vector XML (Android Studio → *New → Vector Asset*), and place them in `composeResources/drawable/`. Keep the same names so `PetPalIcons` below is the only file that changes.

Centralize icons in one file so the style stays consistent:

```kotlin
package com.petpal.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*

object PetPalIcons {
    // Navigation
    val Home = Icons.Rounded.Home
    val Habits = Icons.Rounded.Checklist
    val Focus = Icons.Rounded.Timer
    val Wardrobe = Icons.Rounded.Checkroom
    val Stats = Icons.Rounded.BarChart
    val Settings = Icons.Rounded.Settings

    // Habits
    val Steps = Icons.Rounded.DirectionsWalk
    val Water = Icons.Rounded.WaterDrop
    val Sleep = Icons.Rounded.Bedtime
    val Custom = Icons.Rounded.Star

    // Stats
    val Hunger = Icons.Rounded.Restaurant
    val Happiness = Icons.Rounded.Favorite
    val Energy = Icons.Rounded.Bolt
    val Xp = Icons.Rounded.AutoAwesome
    val Streak = Icons.Rounded.LocalFireDepartment

    // Actions
    val Add = Icons.Rounded.Add
    val Play = Icons.Rounded.PlayArrow
    val Pause = Icons.Rounded.Pause
    val Notifications = Icons.Rounded.Notifications
    val Grass = Icons.Rounded.Grass
    val Sun = Icons.Rounded.WbSunny
    val Moon = Icons.Rounded.DarkMode
}
```

> If a newer icon name (e.g., `DirectionsWalk`) shows a deprecation warning, use the suggested `Icons.AutoMirrored.Rounded.*` replacement.

### 7.4 `Color.kt`

```kotlin
package com.petpal.app.ui.theme

import androidx.compose.ui.graphics.Color

// Light: Meadow
val MeadowGreen = Color(0xFF3A7D44)
val FreshLeaf = Color(0xFFC4EFC0)
val ClearSky = Color(0xFF4A90B8)
val LightSky = Color(0xFFD3EBF8)
val Sunflower = Color(0xFFE8A33D)
val MorningSun = Color(0xFFFFE7BF)
val MorningDew = Color(0xFFF6FBF2)
val PaleGrass = Color(0xFFE4EDDD)
val Clay = Color(0xFFC8553D)
val Soil = Color(0xFF8B6B4A)

// Dark: Night Field
val MoonlitGrass = Color(0xFF8FD694)
val DeepLeaf = Color(0xFF22602D)
val NightSky = Color(0xFF9CCBEA)
val DeepSky = Color(0xFF1F4D66)
val Moon = Color(0xFFF5C97A)
val DuskSun = Color(0xFF5C3F0F)
val NightField = Color(0xFF111A13)
val NightSurface = Color(0xFF18231A)
val NightGrass = Color(0xFF2C3A2E)
val SoftClay = Color(0xFFF2A08E)
val NightSoil = Color(0xFF6E5640)

// Sky gradients for the pet scene
val DaySkyTop = Color(0xFFBFE3F7)
val DaySkyBottom = Color(0xFFEAF6FC)
val NightSkyTop = Color(0xFF0E1A2B)
val NightSkyBottom = Color(0xFF1C2E40)
val HillLight = Color(0xFF7CC67F)
val HillDark = Color(0xFF2F5E35)
```

### 7.5 `Theme.kt`

```kotlin
package com.petpal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MeadowColors = lightColorScheme(
    primary = MeadowGreen,
    onPrimary = Color.White,
    primaryContainer = FreshLeaf,
    onPrimaryContainer = Color(0xFF0B3912),
    secondary = ClearSky,
    onSecondary = Color.White,
    secondaryContainer = LightSky,
    onSecondaryContainer = Color(0xFF0C3448),
    tertiary = Sunflower,
    onTertiary = Color.White,
    tertiaryContainer = MorningSun,
    onTertiaryContainer = Color(0xFF4A2F00),
    background = MorningDew,
    onBackground = Color(0xFF1A2219),
    surface = Color.White,
    onSurface = Color(0xFF1A2219),
    surfaceVariant = PaleGrass,
    onSurfaceVariant = Color(0xFF424D40),
    outline = Color(0xFFB7C4B0),
    error = Clay,
)

private val NightFieldColors = darkColorScheme(
    primary = MoonlitGrass,
    onPrimary = Color(0xFF0B3912),
    primaryContainer = DeepLeaf,
    onPrimaryContainer = FreshLeaf,
    secondary = NightSky,
    onSecondary = Color(0xFF0C3448),
    secondaryContainer = DeepSky,
    onSecondaryContainer = LightSky,
    tertiary = Moon,
    onTertiary = Color(0xFF4A2F00),
    tertiaryContainer = DuskSun,
    onTertiaryContainer = MorningSun,
    background = NightField,
    onBackground = Color(0xFFE0E8DC),
    surface = NightSurface,
    onSurface = Color(0xFFE0E8DC),
    surfaceVariant = NightGrass,
    onSurfaceVariant = Color(0xFFC0CBBB),
    outline = Color(0xFF4B5B4D),
    error = SoftClay,
)

private val PetPalShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun PetPalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) NightFieldColors else MeadowColors,
        shapes = PetPalShapes,
        typography = Typography(),   // swap in a custom font family later
        content = content,
    )
}
```

> Dynamic color (Material You) is intentionally **off** so the grassy field look stays consistent on every device.

### 7.6 Pet Scene Background

The home screen draws a sky gradient with a rolling grassy hill using `Canvas`. No image assets are needed:

```kotlin
package com.petpal.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import com.petpal.app.ui.theme.*

@Composable
fun GrassyFieldScene(
    isNight: Boolean,
    modifier: Modifier = Modifier,
    pet: @Composable () -> Unit,
) {
    val skyTop = if (isNight) NightSkyTop else DaySkyTop
    val skyBottom = if (isNight) NightSkyBottom else DaySkyBottom
    val hill = if (isNight) HillDark else HillLight
    val soil = if (isNight) NightSoil else Soil

    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.matchParentSize()) {
            // Sky
            drawRect(Brush.verticalGradient(listOf(skyTop, skyBottom)))

            // Sun or moon
            drawCircle(
                color = if (isNight) Moon else Sunflower,
                radius = size.minDimension * 0.08f,
                center = Offset(size.width * 0.8f, size.height * 0.2f),
            )

            // Rolling hill
            val hillPath = Path().apply {
                moveTo(0f, size.height * 0.7f)
                cubicTo(
                    size.width * 0.3f, size.height * 0.55f,
                    size.width * 0.7f, size.height * 0.6f,
                    size.width, size.height * 0.68f,
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(hillPath, hill)

            // Soil strip
            drawRect(
                color = soil,
                topLeft = Offset(0f, size.height * 0.94f),
                size = size.copy(height = size.height * 0.06f),
            )
        }
        Box(Modifier.padding(bottom = 48.dp)) { pet() }   // pet stands on the hill
    }
}
```

Later you can add pixel-art clouds, flowers, or fireflies at night as small sprites.

### 7.7 Home Screen Layout

```
┌──────────────────────────────┐
│  PetPal              🔔  ⚙️   │  ← TopAppBar (transparent)
│ ┌──────────────────────────┐ │
│ │   ☀️       sky gradient   │ │
│ │                          │ │
│ │         [ PET ]          │ │  ← GrassyFieldScene
│ │  ~~~~~~ grassy hill ~~~~ │ │
│ └──────────────────────────┘ │
│  Mochi · Baby · ✨ 120 XP     │
│                              │
│  🍴 Hunger     ████████░░    │  ← Stat cards
│  ❤️ Happiness  ██████░░░░    │    (rounded, surface)
│  ⚡ Energy     █████████░    │
│                              │
│  [💧 Water] [🚶 Steps] [⏱ Focus] │  ← Filled tonal buttons
│                              │
├──────────────────────────────┤
│  🏠   ✅   ⏱   👕   📊        │  ← NavigationBar
└──────────────────────────────┘
```

*(Emojis here are placeholders; the app uses the matching `PetPalIcons`.)*

**Component choices**

| Element | Material 3 component |
|---|---|
| Top bar | `CenterAlignedTopAppBar` with transparent container |
| Stat rows | `ElevatedCard` + `LinearProgressIndicator` (rounded, 8dp tall) |
| Habit buttons | `FilledTonalButton` with leading icon |
| Bottom nav | `NavigationBar` + `NavigationBarItem` |
| Rewards | `Snackbar` or small `AssistChip` with XP icon |
| Focus timer | `CircularProgressIndicator` (large, primary color) |

### 7.8 Wire the Theme into `App.kt`

```kotlin
package com.petpal.app

import androidx.compose.runtime.Composable
import com.petpal.app.ui.theme.PetPalTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    PetPalTheme {
        KoinContext {
            // PetPalNavHost()
        }
    }
}
```

> In newer Koin versions `KoinContext` may not be needed when Koin is started in `Application`; follow your Koin version's docs.

---

## 8. Run & Verify

```bash
# Build the Android debug APK
./gradlew :composeApp:assembleDebug

# Install on a connected device
./gradlew :composeApp:installDebug

# Run shared unit tests
./gradlew :composeApp:allTests
```

**Setup checklist**

- [ ] Wizard project builds and runs on a device
- [ ] `libs.versions.toml` updated; Gradle sync succeeds
- [ ] Room generates code (check `build/generated/ksp/`)
- [ ] Koin starts without errors in Logcat
- [ ] `PetEngineTest` passes
- [ ] `PetPalTheme` applied; light (Meadow) and dark (Night Field) both look right
- [ ] Material Rounded icons render in the nav bar
- [ ] App works in **airplane mode**

---

## 9. Assets

| Asset | Location | Notes |
|---|---|---|
| Pet sprites | `composeResources/drawable/` | 32×32 or 64×64 PNG, upscale with nearest-neighbor |
| Pixel font | `composeResources/font/` | e.g., any OFL-licensed pixel font |
| Sound effects | `composeResources/files/` | Generate with [jsfxr](https://sfxr.me/) |

Tools: [Aseprite](https://www.aseprite.org/), [Piskel](https://www.piskelapp.com/). Free assets: [itch.io](https://itch.io/game-assets/free/tag-pixel-art), [OpenGameArt](https://opengameart.org/).

To keep pixel art crisp in Compose, draw it with `FilterQuality.None`:

```kotlin
Image(
    painter = painterResource(Res.drawable.pet_idle),
    contentDescription = "Pet",
    filterQuality = FilterQuality.None,
    modifier = Modifier.size(192.dp),
)
```

---

## 10. Conventions

- **Package root:** `com.petpal.app`
- **Business logic** goes in `commonMain/domain`, kept pure Kotlin with no Android imports
- **Platform features** (sensors, notifications) are defined as interfaces in `commonMain` and implemented in `androidMain`
- **Icons** only come from `PetPalIcons` (Rounded style); **colors** only from `MaterialTheme.colorScheme`, never hard-coded in screens
- **Time** always comes from `TimeProvider` (never call the clock directly) so logic stays testable
- **Commits:** `feat:`, `fix:`, `chore:`, `test:` prefixes

---

## 11. Next Steps After Setup

1. `PetRepository`: load the pet, apply decay, save
2. `HomeScreen` with the pet sprite, stat bars, and a water button
3. Focus timer screen
4. Step tracking with a daily baseline
5. Local notifications for low stats
