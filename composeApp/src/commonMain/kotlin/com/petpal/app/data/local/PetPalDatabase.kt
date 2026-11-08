package com.petpal.app.data.local

import androidx.room.*
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [PetEntity::class, HabitLogEntity::class, StepDayEntity::class, BattleLogEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@ConstructedBy(PetPalDatabaseConstructor::class)
abstract class PetPalDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun stepDayDao(): StepDayDao
    abstract fun battleLogDao(): BattleLogDao
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
