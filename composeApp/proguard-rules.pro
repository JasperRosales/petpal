# Room: generated database implementation is looked up by name on Android.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class com.petpal.app.data.local.PetPalDatabaseConstructor { *; }
-keep @androidx.room.Entity class * { *; }

# Bundled SQLite driver uses JNI.
-keep class androidx.sqlite.driver.bundled.** { *; }

# Keep enum names used for DB storage (PetEntity.stage/path, HabitLogEntity.type).
-keepclassmembers enum com.petpal.app.domain.model.** { *; }

-dontwarn org.slf4j.**
