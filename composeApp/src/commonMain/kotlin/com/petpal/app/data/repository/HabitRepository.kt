package com.petpal.app.data.repository

import com.petpal.app.data.local.*
import com.petpal.app.domain.logic.DayMath
import com.petpal.app.domain.model.HabitLog
import com.petpal.app.domain.platform.TimeProvider
import kotlinx.coroutines.flow.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus

class HabitRepository(
    private val habitLogDao: HabitLogDao,
    private val time: TimeProvider,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    private fun today() = DayMath.dateOf(time.nowMillis(), zone())

    fun todayLogs(): Flow<List<HabitLog>> = logsSinceDaysAgo(0)

    /** Logs from the start of the day [days] ago until now. */
    fun logsSinceDaysAgo(days: Int): Flow<List<HabitLog>> {
        val from = DayMath.startOfDay(today().minus(days, DateTimeUnit.DAY), zone())
        return habitLogDao.observeSince(from).map { list -> list.map { it.toDomain() } }
    }

    fun streak(): Flow<Int> = habitLogDao.observeAllTimestamps().map { stamps ->
        val tz = zone()
        DayMath.streak(stamps.mapTo(HashSet()) { DayMath.dateOf(it, tz) }, today())
    }
}
