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

/** One row per day; see StepMath. */
@Entity(tableName = "step_day")
data class StepDayEntity(
    @PrimaryKey val date: String, // ISO yyyy-MM-dd
    val baseline: Long,
    val offset: Long,
    val steps: Long,
    val rewardedThousands: Int,
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

    @Query("SELECT timestamp FROM habit_log ORDER BY timestamp DESC")
    fun observeAllTimestamps(): Flow<List<Long>>
}

@Dao
interface StepDayDao {
    @Query("SELECT * FROM step_day ORDER BY date DESC LIMIT 1")
    suspend fun latest(): StepDayEntity?

    @Query("SELECT * FROM step_day ORDER BY date DESC LIMIT 1")
    fun observeLatest(): Flow<StepDayEntity?>

    @Upsert
    suspend fun upsert(day: StepDayEntity)
}
