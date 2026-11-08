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

    fun onReading(current: StepDay?, date: String, sensorTotal: Long): StepDay {
        if (current == null || current.date != date) {
            // New day: start counting from this reading.
            return StepDay(date = date, baseline = sensorTotal)
        }
        if (sensorTotal < current.baseline) {
            // Device rebooted: keep today's progress and count from zero again.
            return current.copy(baseline = 0, offset = current.steps, steps = current.steps + sensorTotal)
        }
        return current.copy(steps = current.offset + (sensorTotal - current.baseline))
    }

    /** New 1k-step rewards earned since the last payout. */
    fun pendingRewards(day: StepDay): Int =
        (day.steps / STEPS_PER_REWARD).toInt() - day.rewardedThousands
}
