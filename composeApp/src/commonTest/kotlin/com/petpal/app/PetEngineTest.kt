package com.petpal.app

import com.petpal.app.domain.battle.Move
import com.petpal.app.domain.battle.SpeciesKit
import com.petpal.app.domain.logic.*
import com.petpal.app.domain.model.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

class PetEngineTest {
    private val base = Pet(name = "Mochi", lastUpdated = 0L)

    @Test fun stepsGiveXpAndEvolve() {
        val pet = PetEngine.reward(base.copy(xp = 45), HabitType.STEPS)
        assertEquals(65, pet.xp)
        assertEquals(Stage.BABY, pet.stage)
    }

    @Test fun focusGivesNoXp() = assertEquals(0, PetEngine.reward(base, HabitType.FOCUS).xp)

    @Test fun levels() {
        assertEquals(1, PetEngine.levelFor(0))
        assertEquals(2, PetEngine.levelFor(10))
        assertEquals(16, PetEngine.levelFor(2250))
        assertEquals(PetEngine.MAX_LEVEL, PetEngine.levelFor(1_000_000))
        assertEquals(40, PetEngine.nextLevelXp(15))
        assertNull(PetEngine.nextLevelXp(1_000_000))
    }

    @Test fun newPetKnowsStarters() {
        val pet = PetEngine.syncMoves(base)
        assertEquals(SpeciesKit.learnset(Species.SPROUT).filter { it.level <= 1 }.map { it.move }, pet.moves)
        assertNull(PetEngine.pendingMove(pet))
    }

    @Test fun fullMovesetAsksBeforeLearning() {
        val pet = PetEngine.addXp(PetEngine.syncMoves(base), PetEngine.xpForLevel(3))
        assertEquals(SpeciesKit.MAX_MOVES, pet.moves.size)
        assertEquals(Move.GROWL, PetEngine.pendingMove(pet))

        val swapped = PetEngine.learn(pet, Move.GROWL, forget = Move.TACKLE)
        assertTrue(Move.GROWL in swapped.moves && Move.TACKLE !in swapped.moves)
        assertEquals(SpeciesKit.MAX_MOVES, swapped.moves.size)
        assertEquals(Move.PUNCH, PetEngine.pendingMove(swapped)) // also unlocked at Lv 3

        val skipped = PetEngine.learn(pet, Move.GROWL, forget = null)
        assertEquals(pet.moves, skipped.moves)
        assertEquals(Move.PUNCH, PetEngine.pendingMove(skipped))
    }

    @Test fun oldPetsGetAMoveset() {
        val pet = PetEngine.syncMoves(base.copy(xp = 5000))
        assertEquals(SpeciesKit.MAX_MOVES, pet.moves.size)
        assertNull(PetEngine.pendingMove(pet))
    }

    @Test fun moods() {
        assertEquals(Mood.SLEEPY, PetEngine.mood(isNight = true, activeToday = true))
        assertEquals(Mood.HAPPY, PetEngine.mood(isNight = false, activeToday = true))
        assertEquals(Mood.OKAY, PetEngine.mood(isNight = false, activeToday = false))
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
