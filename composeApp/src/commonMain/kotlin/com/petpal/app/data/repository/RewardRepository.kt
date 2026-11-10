package com.petpal.app.data.repository

import com.petpal.app.data.local.RewardClaimEntity
import com.petpal.app.data.local.RewardDao
import com.petpal.app.domain.logic.DayMath
import com.petpal.app.domain.logic.Events
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.platform.OnlineService
import com.petpal.app.domain.platform.TimeProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/** One-off event prizes and the monthly free pet. Each is claimed once. */
class RewardRepository(
    private val dao: RewardDao,
    private val pets: PetRepository,
    private val time: TimeProvider,
    private val online: OnlineService? = null,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    private val mutex = Mutex()

    val claimed = dao.observeKeys()

    fun today(): LocalDate = DayMath.dateOf(time.nowMillis(), zone())

    /** Grants a random pet under [key] once. Null if already claimed or every pet is owned. */
    private suspend fun claim(key: String): Species? = mutex.withLock {
        if (dao.count(key) > 0) return@withLock null
        val got = pets.grantRandomPet() ?: return@withLock null
        dao.insert(RewardClaimEntity(key, time.nowMillis()))
        got
    }

    suspend fun claimBoss(): Species? = if (Events.bossOpen(today())) claim(Events.BOSS_KEY) else null

    /** True if this player is #1 on the online leaderboard right now. False offline or on errors. */
    suspend fun isChampion(): Boolean =
        online?.let { runCatching { it.leaderboard().me?.rank == 1 }.getOrDefault(false) } ?: false

    /** The #1 player on the leaderboard gets a free pet on the 1st of every month. */
    suspend fun claimMonthly(): Species? {
        val today = today()
        if (!Events.monthlyOpen(today) || !isChampion()) return null
        return claim(Events.monthlyKey(today))
    }
}
