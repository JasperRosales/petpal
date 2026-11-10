package com.petpal.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pet")
data class PetEntity(
    @PrimaryKey val id: Long = 1,
    val name: String,
    val stage: String,
    val path: String,
    @ColumnInfo(defaultValue = "SPROUT") val species: String = "SPROUT",
    val hunger: Double,
    val happiness: Double,
    val energy: Double,
    val xp: Int,
    val lastUpdated: Long,
    @ColumnInfo(defaultValue = "") val moves: String = "", // comma-separated Move names
    @ColumnInfo(defaultValue = "") val seenMoves: String = "",
    @ColumnInfo(defaultValue = "0") val relearnsUsed: Int = 0,
    @ColumnInfo(defaultValue = "0") val stepsAtReset: Long = 0,
    /** The one pet that earns XP and battles. Exactly one row is active. */
    @ColumnInfo(defaultValue = "1") val active: Boolean = true,
    @ColumnInfo(defaultValue = "") val uniquePicks: String = "", // comma-separated Move names
    @ColumnInfo(defaultValue = "") val passives: String = "", // unused since v0.21: one fixed passive per stage
)

@Entity(tableName = "habit_log")
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val value: Int,
    val timestamp: Long,
)

/** One row per finished battle. */
@Entity(tableName = "battle_log")
data class BattleLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val outcome: String,
    val xp: Int,
    val timestamp: Long,
    @ColumnInfo(defaultValue = "") val opponent: String = "",
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

/** Event and monthly rewards already collected, keyed like "boss:2026-11" or "monthly:2026-12". */
@Entity(tableName = "reward_claim")
data class RewardClaimEntity(
    @PrimaryKey val key: String,
    val timestamp: Long,
)

@Dao
interface RewardDao {
    @Query("SELECT `key` FROM reward_claim")
    fun observeKeys(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM reward_claim WHERE `key` = :key")
    suspend fun count(key: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(claim: RewardClaimEntity)
}

@Dao
interface PetDao {
    @Query("SELECT * FROM pet WHERE active = 1 LIMIT 1")
    fun observePet(): Flow<PetEntity?>

    @Query("SELECT * FROM pet WHERE active = 1 LIMIT 1")
    suspend fun getPet(): PetEntity?

    @Query("SELECT * FROM pet ORDER BY id")
    fun observeAll(): Flow<List<PetEntity>>

    @Query("SELECT * FROM pet ORDER BY id")
    suspend fun getAll(): List<PetEntity>

    @Query("UPDATE pet SET active = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setActive(id: Long)

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

    @Query("SELECT COALESCE(SUM(steps), 0) FROM step_day")
    fun observeLifetime(): Flow<Long>

    @Query("SELECT COALESCE(SUM(steps), 0) FROM step_day")
    suspend fun lifetime(): Long

    /** [from] is an ISO date; ISO strings sort like dates. */
    @Query("SELECT * FROM step_day WHERE date >= :from ORDER BY date")
    fun observeFrom(from: String): Flow<List<StepDayEntity>>

    @Upsert
    suspend fun upsert(day: StepDayEntity)
}

@Dao
interface BattleLogDao {
    @Insert
    suspend fun insert(log: BattleLogEntity)

    @Query("SELECT * FROM battle_log ORDER BY timestamp DESC LIMIT 50")
    fun observeRecent(): Flow<List<BattleLogEntity>>

    @Query("SELECT * FROM battle_log WHERE timestamp >= :from ORDER BY timestamp")
    fun observeSince(from: Long): Flow<List<BattleLogEntity>>

    @Query("DELETE FROM battle_log WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM battle_log")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM battle_log WHERE timestamp >= :from AND xp > 0")
    suspend fun rewardedSince(from: Long): Int
}
