package com.petpal.app.domain.logic

import kotlinx.datetime.*

/** Day-boundary helpers. Take explicit millis + zone so they stay testable. */
object DayMath {
    fun dateOf(millis: Long, zone: TimeZone): LocalDate =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date

    fun startOfDay(date: LocalDate, zone: TimeZone): Long =
        date.atStartOfDayIn(zone).toEpochMilliseconds()

    fun hourOf(millis: Long, zone: TimeZone): Int =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).hour

    /**
     * Consecutive days with at least one log, ending today.
     * If today has no log yet, a streak ending yesterday still counts.
     */
    fun streak(logDays: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in logDays) today else today.minus(1, DateTimeUnit.DAY)
        var count = 0
        while (day in logDays) {
            count++
            day = day.minus(1, DateTimeUnit.DAY)
        }
        return count
    }
}
