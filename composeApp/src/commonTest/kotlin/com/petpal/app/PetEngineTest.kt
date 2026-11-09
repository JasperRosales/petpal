package com.petpal.app

import com.petpal.app.domain.battle.Move
import com.petpal.app.domain.battle.Passive
import com.petpal.app.domain.battle.SpeciesKit
import com.petpal.app.domain.logic.*
import com.petpal.app.domain.model.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

class PetEngineTest {
    private val base = Pet(name = "Mochi", lastUpdated = 0L)

    @Test fun stepsGiveXpAndEvolve() {
        val pet = PetEngine.reward(base.copy(xp = 25), HabitType.STEPS)
        assertEquals(145, pet.xp)
        assertEquals(Stage.BABY, pet.stage) // Lv 3
    }

    @Test fun stageProgressTracksTheNextEvolution() {
        val kid = base.copy(xp = PetEngine.xpForLevel(PetEngine.KID_LEVEL), stage = Stage.KID)
        assertEquals(0f, PetEngine.stageProgress(kid))
        assertEquals(Stage.TEEN, PetEngine.nextStage(kid.stage))
        assertEquals(PetEngine.xpForLevel(PetEngine.TEEN_LEVEL) - kid.xp, PetEngine.xpToNextStage(kid))
        val adult = base.copy(xp = PetEngine.xpForLevel(60), stage = Stage.ADULT)
        assertEquals(1f, PetEngine.stageProgress(adult))
        assertEquals(0, PetEngine.xpToNextStage(adult))
    }

    @Test fun pickOneUniquePerStage() {
        val kid = base.copy(species = Species.DRAKE, xp = PetEngine.xpForLevel(PetEngine.KID_LEVEL), stage = Stage.KID)
        val (a, b) = PetEngine.pendingUnique(kid)!!
        val picked = PetEngine.pickUnique(kid, b)
        assertEquals(listOf(b), picked.uniquePicks)
        assertNull(PetEngine.pendingUnique(picked))
        assertEquals(picked, PetEngine.pickUnique(picked, a)) // can't take the other one too
        assertEquals(b, SpeciesKit.unique(Species.DRAKE, Stage.KID, picked.uniquePicks))
        // reaching Teen asks again
        val teen = PetEngine.addXp(picked, PetEngine.xpForLevel(PetEngine.TEEN_LEVEL))
        assertEquals(SpeciesKit.uniqueOptions(Species.DRAKE, Stage.TEEN), PetEngine.pendingUnique(teen))
    }

    @Test fun eachStageHasExactlyOnePassive() {
        val egg = base.copy(species = Species.DRAKE)
        assertEquals(Passive.BLAZE, PetEngine.passive(egg)) // Egg uses the Baby passive
        val adult = PetEngine.addXp(egg, PetEngine.xpForLevel(PetEngine.ADULT_LEVEL))
        assertEquals(Stage.ADULT, adult.stage)
        assertEquals(SpeciesKit.passive(Species.DRAKE, Stage.ADULT), PetEngine.passive(adult))
        // it reaches battles and survives the link protocol
        val info = PetEngine.fighterInfo(adult)
        assertEquals(PetEngine.passive(adult), info.passive)
        val back = com.petpal.app.domain.battle.Protocol.parse(com.petpal.app.domain.battle.Protocol.hello(info))
            as com.petpal.app.domain.battle.Protocol.Hello
        assertEquals(info.passive, back.info.passive)
    }

    @Test fun everyPassiveIsUniqueToOnePetAndStage() {
        val all = Species.entries.flatMap { s -> SpeciesKit.passives(s) }
        assertEquals(Passive.entries.size, all.size)
        assertEquals(all.size, all.toSet().size)
        val combos = Passive.entries.groupBy { it.perks.toSet() }.filterValues { it.size > 1 }
        assertTrue(combos.isEmpty(), "same effects: $combos")
        assertEquals(all.size, all.map { it.label }.toSet().size)
        Species.entries.forEach { s ->
            assertEquals(4, SpeciesKit.passives(s).size, s.name)
            listOf(Stage.BABY, Stage.KID, Stage.TEEN, Stage.ADULT).forEach { st ->
                SpeciesKit.passive(s, st).let { assertTrue(it.species == s && it.stage == st, it.name) }
            }
        }
    }

    @Test fun teenStageSitsBetweenKidAndAdult() {
        assertEquals(Stage.KID, PetEngine.stageFor(PetEngine.xpForLevel(PetEngine.TEEN_LEVEL - 1)))
        assertEquals(Stage.TEEN, PetEngine.stageFor(PetEngine.xpForLevel(PetEngine.TEEN_LEVEL)))
        assertEquals(Stage.ADULT, PetEngine.stageFor(PetEngine.xpForLevel(PetEngine.ADULT_LEVEL)))
    }

    @Test fun relearnPointBringsBackAForgottenMove() {
        val pet = PetEngine.syncMoves(base.copy(species = Species.DRAKE, xp = PetEngine.xpForLevel(25)))
        assertEquals(2, PetEngine.relearnPoints(pet)) // Lv 25 -> points at 10 and 20
        assertEquals(SpeciesKit.MAX_MOVES, pet.moves.size)
        // skip every pending move so there's something to relearn
        var p = pet
        while (PetEngine.pendingMove(p) != null) p = PetEngine.learn(p, PetEngine.pendingMove(p)!!, null)
        val old = PetEngine.relearnable(p).first()
        val forget = p.moves.first()
        val after = PetEngine.relearn(p, old, forget)
        assertTrue(old in after.moves && forget !in after.moves)
        assertEquals(1, PetEngine.relearnPoints(after))
        assertEquals(after, PetEngine.relearn(after, old, null)) // already known: no change
        assertEquals(0, PetEngine.relearnPoints(base.copy(xp = PetEngine.xpForLevel(9))))
    }

    @Test fun unlockNeedsStepsAndStartsAtLevelOne() {
        val old = PetEngine.syncMoves(base.copy(species = Species.DRAKE, xp = PetEngine.xpForLevel(40), stepsAtReset = 5_000))
        val pets = listOf(old)
        assertFalse(PetEngine.canUnlock(pets, 14_999, PetEngine.UnlockKind.RANDOM))
        assertTrue(PetEngine.canUnlock(pets, 15_000, PetEngine.UnlockKind.RANDOM))
        assertFalse(PetEngine.canUnlock(pets, 54_999, PetEngine.UnlockKind.CHOOSE))
        assertTrue(PetEngine.canUnlock(pets, 55_000, PetEngine.UnlockKind.CHOOSE))

        val fresh = PetEngine.newPet(2, Species.OTTER, now = 1L, lifetimeSteps = 55_000)
        assertEquals(1, PetEngine.levelFor(fresh.xp))
        assertEquals(Stage.EGG, fresh.stage)
        assertEquals("Otter", fresh.name)
        assertEquals(SpeciesKit.starters(Species.OTTER), fresh.moves)
        assertEquals(0, PetEngine.stepsSinceUnlock(pets + fresh, 55_000)) // counter restarts
        // nothing left to unlock once every species is owned
        val all = Species.entries.mapIndexed { i, s -> PetEngine.newPet(i.toLong(), s, 0, 0) }
        assertFalse(PetEngine.canUnlock(all, 1_000_000, PetEngine.UnlockKind.RANDOM))
    }

    @Test fun evolvesByLevelFifty() {
        assertEquals(Stage.KID, PetEngine.stageFor(PetEngine.xpForLevel(20)))
        assertEquals(Stage.TEEN, PetEngine.stageFor(PetEngine.xpForLevel(50) - 1))
        assertEquals(Stage.ADULT, PetEngine.stageFor(PetEngine.xpForLevel(50)))
        assertEquals(100, PetEngine.MAX_LEVEL)
        assertEquals(100, PetEngine.levelFor(10_000_000))
        // never devolves
        assertEquals(Stage.ADULT, PetEngine.addXp(base.copy(xp = 2000, stage = Stage.ADULT), 10).stage)
    }

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
        assertEquals(SpeciesKit.STARTERS, pet.moves.size)
        assertEquals(listOf(Move.TACKLE, Move.LEAF_TOSS), pet.moves)
        assertNull(PetEngine.pendingMove(pet))
    }

    @Test fun fullMovesetAsksBeforeLearning() {
        val pet = PetEngine.addXp(PetEngine.syncMoves(base), PetEngine.xpForLevel(4))
        assertEquals(SpeciesKit.MAX_MOVES, pet.moves.size)
        val next = PetEngine.pendingMove(pet)
        assertNotNull(next)
        val swapped = PetEngine.learn(pet, next, forget = Move.TACKLE)
        assertTrue(next in swapped.moves && Move.TACKLE !in swapped.moves)
        assertEquals(SpeciesKit.MAX_MOVES, swapped.moves.size)
        val skipped = PetEngine.learn(pet, next, forget = null)
        assertEquals(pet.moves, skipped.moves)
        assertNotEquals(next, PetEngine.pendingMove(skipped))
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
    @Test fun stepsWalkedWhilePausedDontCount() {
        val d = StepMath.onReading(StepDay("2026-11-08", 100), "2026-11-08", 1100) // 1000 today
        val resumed = StepMath.resume(d, "2026-11-08", 5100) // walked 4000 while paused
        assertEquals(1000, resumed.steps)
        assertEquals(1200, StepMath.onReading(resumed, "2026-11-08", 5300).steps)
        assertEquals(0, StepMath.resume(d, "2026-11-09", 5100).steps) // paused overnight: fresh day
    }

    @Test fun newDayKeepsStepsSinceLastReading() {
        // Last reading yesterday was at sensor 5100; 3900 steps walked since, app closed.
        val d = StepMath.onReading(StepDay("2026-11-07", 100, steps = 5000), "2026-11-08", 9000)
        assertEquals(StepDay("2026-11-08", baseline = 5100, steps = 3900), d)
        assertEquals(4000, StepMath.onReading(d, "2026-11-08", 9100).steps)
    }

    @Test fun newDayAfterRebootCountsSinceBoot() {
        val d = StepMath.onReading(StepDay("2026-11-07", 100, steps = 5000), "2026-11-08", 700)
        assertEquals(StepDay("2026-11-08", baseline = 0, steps = 700), d)
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
