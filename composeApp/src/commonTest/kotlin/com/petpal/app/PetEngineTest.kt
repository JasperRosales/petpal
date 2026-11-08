package com.petpal.app

import com.petpal.app.domain.logic.*
import com.petpal.app.domain.model.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

class PetEngineTest {
    private val base = Pet(name = "Mochi", lastUpdated = 0L)

    @Test fun decaysOverTwoHours() {
        val pet = PetEngine.applyDecay(base, now = 2 * 3_600_000L)
        assertEquals(72.0, pet.hunger)
        assertEquals(74.0, pet.happiness)
        assertEquals(76.0, pet.energy)
    }

    @Test fun ignoresClockRollback() {
        val pet = base.copy(lastUpdated = 10_000L)
        assertEquals(pet, PetEngine.applyDecay(pet, now = 5_000L))
    }

    @Test fun neverGoesBelowZero() {
        val pet = PetEngine.applyDecay(base, now = 1_000 * 3_600_000L)
        assertEquals(0.0, pet.hunger)
    }

    @Test fun rewardCapsAt100AndEvolves() {
        val pet = PetEngine.reward(base.copy(energy = 95.0, xp = 45), HabitType.STEPS)
        assertEquals(100.0, pet.energy)
        assertEquals(Stage.BABY, pet.stage)
    }

    @Test fun selfReportedHabitsGiveNoXp() {
        listOf(HabitType.WATER, HabitType.SLEEP, HabitType.CUSTOM, HabitType.FOCUS).forEach {
            assertEquals(0, PetEngine.reward(base, it).xp)
        }
    }

    @Test fun moods() {
        assertEquals(Mood.SLEEPY, PetEngine.moodOf(base.copy(energy = 10.0)))
        assertEquals(Mood.SAD, PetEngine.moodOf(base.copy(hunger = 20.0)))
        assertEquals(Mood.HAPPY, PetEngine.moodOf(base))
        assertEquals(Mood.OKAY, PetEngine.moodOf(base.copy(happiness = 50.0)))
    }
}

class StepMathTest {
    @Test fun newDayStartsAtZero() {
        val d = StepMath.onReading(StepDay("2026-11-07", 100, steps = 5000), "2026-11-08", 9000)
        assertEquals(StepDay("2026-11-08", baseline = 9000), d)
    }

    @Test fun countsFromBaseline() {
        val d = StepMath.onReading(StepDay("2026-11-08", 9000), "2026-11-08", 10_500)
        assertEquals(1500, d.steps)
        assertEquals(1, StepMath.pendingRewards(d))
    }

    @Test fun capsDailyRewards() {
        val d = StepMath.onReading(StepDay("2026-11-08", 0), "2026-11-08", 50_000)
        assertEquals(StepMath.MAX_REWARDS_PER_DAY, StepMath.pendingRewards(d))
        assertEquals(0, StepMath.pendingRewards(d.copy(rewardedThousands = StepMath.MAX_REWARDS_PER_DAY)))
    }

    @Test fun clockRollbackKeepsCurrentDay() {
        val today = StepDay("2026-11-08", baseline = 0, steps = 20_000, rewardedThousands = 20)
        val d = StepMath.onReading(today, "2026-11-07", 21_000)
        assertEquals("2026-11-08", d.date)
        assertEquals(0, StepMath.pendingRewards(d))
    }

    @Test fun survivesReboot() {
        val before = StepDay("2026-11-08", baseline = 9000, steps = 1500)
        val after = StepMath.onReading(before, "2026-11-08", 200)
        assertEquals(1700, after.steps)
        assertEquals(2000, StepMath.onReading(after, "2026-11-08", 500).steps)
    }
}

class StreakTest {
    private val today = LocalDate(2026, 11, 8)
    private fun d(day: Int) = LocalDate(2026, 11, day)

    @Test fun countsConsecutiveDays() = assertEquals(3, DayMath.streak(setOf(d(8), d(7), d(6), d(4)), today))
    @Test fun yesterdayKeepsStreak() = assertEquals(2, DayMath.streak(setOf(d(7), d(6)), today))
    @Test fun gapBreaksStreak() = assertEquals(0, DayMath.streak(setOf(d(5)), today))
}
