package com.petpal.app.domain.logic

/**
 * Daily steps on top of TYPE_STEP_COUNTER, which resets on reboot.
 * steps = offset + (sensorTotal - baseline)
 */
data class StepDay(
    val date: String,
    val baseline: Long,
    val offset: Long = 0,
    val steps: Long = 0,
    val rewardedThousands: Int = 0,
)

object StepMath {
    const val STEPS_PER_REWARD = 1000
    /** Daily XP cap: 20k steps. Stops shaker/clip-to-a-fan farming from paying out forever. */
    const val MAX_REWARDS_PER_DAY = 20

    fun onReading(current: StepDay?, date: String, sensorTotal: Long): StepDay {
        // Clock moved back to an earlier day: keep counting into the current day so the
        // daily cap can't be reset by changing the date.
        val day = if (current != null && date < current.date) current.date else date
        if (current == null || current.date != day) {
            // New day: start counting from this reading.
            return StepDay(date = day, baseline = sensorTotal)
        }
        if (sensorTotal < current.baseline) {
            // Device rebooted: keep today's progress and count from zero again.
            return current.copy(baseline = 0, offset = current.steps, steps = current.steps + sensorTotal)
        }
        return current.copy(steps = current.offset + (sensorTotal - current.baseline))
    }

    /** New 1k-step rewards earned since the last payout (capped per day). */
    fun pendingRewards(day: StepDay): Int =
        ((day.steps / STEPS_PER_REWARD).toInt().coerceAtMost(MAX_REWARDS_PER_DAY) - day.rewardedThousands)
            .coerceAtLeast(0)
}
