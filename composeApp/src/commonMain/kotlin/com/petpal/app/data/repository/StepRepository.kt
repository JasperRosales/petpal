package com.petpal.app.data.repository

import com.petpal.app.data.local.*
import com.petpal.app.domain.logic.DayMath
import com.petpal.app.domain.logic.StepMath
import com.petpal.app.domain.model.HabitType
import com.petpal.app.domain.platform.StepCounter
import com.petpal.app.domain.platform.TimeProvider
import kotlinx.coroutines.flow.*
import kotlinx.datetime.TimeZone

class StepRepository(
    private val stepCounter: StepCounter,
    private val stepDayDao: StepDayDao,
    private val petRepository: PetRepository,
    private val time: TimeProvider,
) {
    val isAvailable: Boolean get() = stepCounter.isAvailable

    private fun todayKey() = DayMath.dateOf(time.nowMillis(), TimeZone.currentSystemDefault()).toString()

    fun todaySteps(): Flow<Long> =
        stepDayDao.observeLatest().map { if (it?.date == todayKey()) it.steps else 0L }

    /** Collect while the app is open: persists readings and pays out 1k-step rewards. */
    suspend fun track() {
        stepCounter.stepsSinceBoot().collect { total ->
            val previous = stepDayDao.latest()?.toDomain()
            var day = StepMath.onReading(previous, todayKey(), total)
            val pending = StepMath.pendingRewards(day)
            if (pending > 0) day = day.copy(rewardedThousands = day.rewardedThousands + pending)
            stepDayDao.upsert(day.toEntity()) // save first so a crash can't double-pay
            repeat(pending.coerceAtLeast(0)) {
                petRepository.logHabit(HabitType.STEPS, StepMath.STEPS_PER_REWARD)
            }
        }
    }
}
