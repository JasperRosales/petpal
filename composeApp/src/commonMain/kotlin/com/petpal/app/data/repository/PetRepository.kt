package com.petpal.app.data.repository

import com.petpal.app.data.local.*
import com.petpal.app.domain.logic.PetEngine
import com.petpal.app.domain.model.*
import com.petpal.app.domain.platform.TimeProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PetRepository(
    private val petDao: PetDao,
    private val habitLogDao: HabitLogDao,
    private val time: TimeProvider,
) {
    private val mutex = Mutex()

    val pet: Flow<Pet> = petDao.observePet().filterNotNull().map { it.toDomain() }

    /** Create the pet on first launch, then apply decay since the last save. */
    suspend fun refresh(): Pet = mutex.withLock {
        val now = time.nowMillis()
        val current = petDao.getPet()?.toDomain() ?: Pet(name = DEFAULT_NAME, lastUpdated = now)
        val decayed = PetEngine.applyDecay(current, now)
        petDao.upsert(decayed.toEntity())
        decayed
    }

    suspend fun rename(name: String) = mutex.withLock {
        val current = petDao.getPet() ?: return@withLock
        petDao.upsert(current.copy(name = name.trim().ifEmpty { DEFAULT_NAME }))
    }

    /** Decay, reward, save, and record the habit in one step. */
    suspend fun logHabit(type: HabitType, value: Int = 1): Pet {
        refresh()
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
