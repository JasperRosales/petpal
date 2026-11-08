package com.petpal.app.data.repository

import com.petpal.app.data.local.*
import com.petpal.app.domain.logic.DayMath
import com.petpal.app.domain.logic.PetEngine
import com.petpal.app.domain.model.*
import com.petpal.app.domain.platform.TimeProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone

class PetRepository(
    private val petDao: PetDao,
    private val habitLogDao: HabitLogDao,
    private val battleLogDao: BattleLogDao,
    private val time: TimeProvider,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    private val mutex = Mutex()

    val pet: Flow<Pet> = petDao.observePet().filterNotNull().map { it.toDomain() }

    /** False until the personality quiz has given this device its one partner pet. */
    val hasPet: Flow<Boolean> = petDao.observePet().map { it != null }.distinctUntilChanged()

    /** Apply decay since the last save. Null before the pet is adopted. */
    suspend fun refresh(): Pet? = mutex.withLock {
        val now = time.nowMillis()
        val current = petDao.getPet()?.toDomain() ?: return@withLock null
        val decayed = PetEngine.applyDecay(current, now)
        petDao.upsert(decayed.toEntity())
        decayed
    }

    /** One pet per device: does nothing if a pet already exists. */
    suspend fun adopt(name: String, species: Species) = mutex.withLock {
        if (petDao.getPet() != null) return@withLock
        val pet = Pet(name = name.trim().ifEmpty { species.label }, species = species, lastUpdated = time.nowMillis())
        petDao.upsert(pet.toEntity())
    }

    suspend fun rename(name: String) = mutex.withLock {
        val current = petDao.getPet() ?: return@withLock
        petDao.upsert(current.copy(name = name.trim().ifEmpty { DEFAULT_NAME }))
    }

    /** Saves a finished online battle. Returns XP gained (0 once today's cap is used up). */
    suspend fun recordBattle(outcome: BattleOutcome): Int {
        refresh() ?: return 0
        return mutex.withLock {
            val now = time.nowMillis()
            val tz = zone()
            val dayStart = DayMath.startOfDay(DayMath.dateOf(now, tz), tz)
            val capped = battleLogDao.rewardedSince(dayStart) >= PetEngine.MAX_REWARDED_BATTLES_PER_DAY
            val xp = if (capped) 0 else PetEngine.battleXp(outcome)
            val pet = PetEngine.addXp(petDao.getPet()!!.toDomain(), xp)
            petDao.upsert(pet.toEntity())
            battleLogDao.insert(BattleLogEntity(outcome = outcome.name, xp = xp, timestamp = now))
            xp
        }
    }

    /** Decay, reward, save, and record the habit in one step. */
    suspend fun logHabit(type: HabitType, value: Int = 1): Pet? {
        refresh() ?: return null
        return mutex.withLock {
            val now = time.nowMillis()
            val pet = PetEngine.reward(petDao.getPet()!!.toDomain(), type)
            petDao.upsert(pet.toEntity())
            habitLogDao.insert(HabitLogEntity(type = type.name, value = value, timestamp = now))
            pet
        }
    }

    companion object {
        const val DEFAULT_NAME = "Mochi"
    }
}
