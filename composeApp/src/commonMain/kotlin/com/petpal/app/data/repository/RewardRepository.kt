package com.petpal.app.data.repository

import com.petpal.app.data.local.RewardClaimEntity
import com.petpal.app.data.local.RewardDao
import com.petpal.app.domain.logic.DayMath
import com.petpal.app.domain.logic.Events
import com.petpal.app.domain.model.Species
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

    suspend fun claimMonthly(): Species? = today().let { if (Events.monthlyOpen(it)) claim(Events.monthlyKey(it)) else null }
}
