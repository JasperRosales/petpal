package com.petpal.app.data.repository

import com.petpal.app.data.local.*
import com.petpal.app.domain.battle.Move
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
    private val stepDayDao: StepDayDao,
    private val time: TimeProvider,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    private val mutex = Mutex()

    val pet: Flow<Pet> = petDao.observePet().filterNotNull().map { PetEngine.syncMoves(it.toDomain()) }

    /** False until the personality quiz has given this device its one partner pet. */
    val hasPet: Flow<Boolean> = petDao.observePet().map { it != null }.distinctUntilChanged()

    /** Recent battles, newest first. */
    val battles: Flow<List<BattleRecord>> = battleLogDao.observeRecent().map { list -> list.map { it.toDomain() } }

    /** Battles since [from] (epoch ms), oldest first. Follows deletes in the battle log. */
    fun battlesSince(from: Long): Flow<List<BattleRecord>> =
        battleLogDao.observeSince(from).map { list -> list.map { it.toDomain() } }

    suspend fun deleteBattle(id: Long) = battleLogDao.delete(id)
    suspend fun clearBattles() = battleLogDao.deleteAll()

    /** Current pet, or null before it's adopted. */
    suspend fun refresh(): Pet? = mutex.withLock { load() }

    private suspend fun load(): Pet? {
        return petDao.getPet()?.toDomain()?.let(PetEngine::syncMoves)
    }

    /** Resolves the learn prompt: swap out [forget], or skip the new move when [forget] is null. */
    suspend fun learnMove(move: Move, forget: Move?) = mutex.withLock {
        val pet = load() ?: return@withLock
        petDao.upsert(PetEngine.learn(pet, move, forget).toEntity())
    }

    /** Locks in the current stage's unique. */
    suspend fun pickUnique(move: Move) = mutex.withLock {
        val pet = load() ?: return@withLock
        petDao.upsert(PetEngine.pickUnique(pet, move).toEntity())
    }

    /** Spends a relearn point to bring back an old move (swapping out [forget] if slots are full). */
    suspend fun relearnMove(move: Move, forget: Move?) = mutex.withLock {
        val pet = load() ?: return@withLock
        petDao.upsert(PetEngine.relearn(pet, move, forget).toEntity())
    }

    /** First pet from the quiz: does nothing if a pet already exists. */
    suspend fun adopt(name: String, species: Species) = mutex.withLock {
        if (petDao.getPet() != null) return@withLock
        val pet = PetEngine.syncMoves(Pet(name = name.trim().ifEmpty { species.label }, species = species,
            lastUpdated = time.nowMillis(), stepsAtReset = stepDayDao.lifetime()))
        petDao.upsert(pet.toEntity())
    }

    /** Lifetime steps ever counted on this device. */
    val lifetimeSteps: Flow<Long> = stepDayDao.observeLifetime()

    /** Every pet this device owns, in unlock order. */
    val pets: Flow<List<Pet>> = petDao.observeAll().map { list -> list.map { PetEngine.syncMoves(it.toDomain()) } }

    /**
     * Adds a new Lv 1 egg to the collection once enough steps were walked since the last unlock.
     * [species] is ignored for a random unlock. Returns the new species, or null if not unlocked.
     */
    suspend fun unlockPet(kind: PetEngine.UnlockKind, species: Species?): Species? = mutex.withLock {
        val all = petDao.getAll().map { it.toDomain() }
        if (all.isEmpty()) return@withLock null
        val lifetime = stepDayDao.lifetime()
        if (!PetEngine.canUnlock(all, lifetime, kind)) return@withLock null
        val owned = all.map { it.species }.toSet()
        val next = when (kind) {
            PetEngine.UnlockKind.RANDOM -> (Species.entries - owned).randomOrNull()
            PetEngine.UnlockKind.CHOOSE -> species?.takeIf { it !in owned }
        } ?: return@withLock null
        val pet = PetEngine.newPet(all.maxOf { it.id } + 1, next, time.nowMillis(), lifetime)
        petDao.upsert(pet.toEntity(active = false))
        next
    }

    /** Makes [id] the active pet: the one that earns XP and battles. */
    suspend fun switchPet(id: Long) = mutex.withLock {
        if (petDao.getAll().any { it.id == id }) petDao.setActive(id)
    }

    suspend fun rename(name: String) = mutex.withLock {
        val current = petDao.getPet() ?: return@withLock
        petDao.upsert(current.copy(name = name.trim().ifEmpty { DEFAULT_NAME }))
    }

    /** Saves a finished battle. CPU/practice battles give reduced XP. */
    suspend fun recordBattle(outcome: BattleOutcome, opponent: String, practice: Boolean = false): Int {
        refresh() ?: return 0
        return mutex.withLock {
            val now = time.nowMillis()
            val xp = if (practice) PetEngine.cpuBattleXp(outcome) else PetEngine.battleXp(outcome)
            val pet = PetEngine.addXp(petDao.getPet()!!.toDomain(), xp)
            petDao.upsert(pet.toEntity())
            battleLogDao.insert(BattleLogEntity(outcome = outcome.name, xp = xp, timestamp = now, opponent = opponent))
            xp
        }
    }

    /** Reward, save, and record the habit in one step. */
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
