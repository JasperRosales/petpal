package com.petpal.app.domain.logic

import com.petpal.app.domain.battle.Move
import com.petpal.app.domain.battle.Passive
import kotlin.random.Random
import com.petpal.app.domain.battle.SpeciesKit
import com.petpal.app.domain.model.*
import kotlin.math.sqrt

object PetEngine {
    /** Pets are sleepy at night, happy when you've been active, otherwise okay. */
    fun mood(isNight: Boolean, activeToday: Boolean): Mood = when {
        isNight -> Mood.SLEEPY
        activeToday -> Mood.HAPPY
        else -> Mood.OKAY
    }

    const val HATCH_LEVEL = 3
    const val KID_LEVEL = 20
    const val TEEN_LEVEL = 35
    const val ADULT_LEVEL = 50 // every species is fully evolved by here

    fun stageFor(xp: Int): Stage = levelFor(xp).let {
        when {
            it >= ADULT_LEVEL -> Stage.ADULT
            it >= TEEN_LEVEL -> Stage.TEEN
            it >= KID_LEVEL -> Stage.KID
            it >= HATCH_LEVEL -> Stage.BABY
            else -> Stage.EGG
        }
    }

    const val MAX_LEVEL = 100

    /** Level a pet reaches [stage] at. */
    fun stageLevel(stage: Stage): Int = when (stage) {
        Stage.EGG -> 1
        Stage.BABY -> HATCH_LEVEL
        Stage.KID -> KID_LEVEL
        Stage.TEEN -> TEEN_LEVEL
        Stage.ADULT -> ADULT_LEVEL
    }

    /** The stage after [stage], or null when fully evolved. */
    fun nextStage(stage: Stage): Stage? = Stage.entries.getOrNull(stage.ordinal + 1)

    /** 0..1 progress from the pet's current stage to the next (1 when fully evolved). */
    fun stageProgress(pet: Pet): Float {
        val next = nextStage(pet.stage) ?: return 1f
        val from = xpForLevel(stageLevel(pet.stage))
        val to = xpForLevel(stageLevel(next))
        return ((pet.xp - from).toFloat() / (to - from)).coerceIn(0f, 1f)
    }

    /** XP still needed to reach the next stage, or 0 when fully evolved. */
    fun xpToNextStage(pet: Pet): Int =
        nextStage(pet.stage)?.let { (xpForLevel(stageLevel(it)) - pet.xp).coerceAtLeast(0) } ?: 0

    /** Level n needs 10 * (n-1)^2 XP: 10, 40, 90, 160 ... */
    fun levelFor(xp: Int): Int = (1 + sqrt(xp.coerceAtLeast(0) / 10.0).toInt()).coerceAtMost(MAX_LEVEL)

    fun xpForLevel(level: Int): Int = 10 * (level - 1) * (level - 1)

    /** XP needed for the next level, or null at max. */
    fun nextLevelXp(xp: Int): Int? = levelFor(xp).let { if (it >= MAX_LEVEL) null else xpForLevel(it + 1) }

    /**
     * XP only comes from things the app can verify: hardware-counted steps and battles.
     * CPU battles give reduced XP.
     */
    fun reward(pet: Pet, type: HabitType): Pet = when (type) {
        HabitType.STEPS -> addXp(pet, STEP_XP)
    }

    fun battleXp(outcome: BattleOutcome): Int = when (outcome) {
        BattleOutcome.WIN -> 30
        BattleOutcome.DRAW -> 15
        BattleOutcome.LOSS -> 10
    }

    /** CPU/practice battles give reduced XP. */
    fun cpuBattleXp(outcome: BattleOutcome): Int = when (outcome) {
        BattleOutcome.WIN -> 8
        BattleOutcome.DRAW -> 4
        BattleOutcome.LOSS -> 2
    }

    /** Pets never devolve, even if an older version evolved them earlier. */
    fun addXp(pet: Pet, xp: Int, rng: Random = Random.Default): Pet = (pet.xp + xp).let {
        rollPassives(syncMoves(pet.copy(xp = it, stage = maxOf(pet.stage, stageFor(it)))), rng)
    }

    /** One rolled passive per stage reached: Baby, Kid, Teen, Adult. */
    private fun passivesOwed(stage: Stage) = stage.ordinal

    /** Rolled passives that still match their stage's options (older versions shared passives between pets). */
    private fun validPassives(pet: Pet): List<Passive> = pet.passives.withIndex()
        .takeWhile { (i, p) -> i < Stage.entries.size - 1 && p in SpeciesKit.passiveOptions(pet.species, Stage.entries[i + 1]).toList() }
        .map { it.value }

    fun needsPassiveRoll(pet: Pet) = validPassives(pet).let { it.size < passivesOwed(pet.stage) || it.size != pet.passives.size }

    /** Rolls one of the two options for every stage the pet reached without a valid roll yet. */
    fun rollPassives(pet: Pet, rng: Random = Random.Default): Pet {
        if (!needsPassiveRoll(pet)) return pet
        val got = validPassives(pet).toMutableList()
        while (got.size < passivesOwed(pet.stage)) {
            val (a, b) = SpeciesKit.passiveOptions(pet.species, Stage.entries[got.size + 1])
            got += if (rng.nextBoolean()) a else b
        }
        return pet.copy(passives = got)
    }

    /** The pet's passive right now. */
    fun passive(pet: Pet): Passive = SpeciesKit.passiveFor(pet.species, pet.stage, passiveAt(pet, pet.stage))

    /** Passive rolled at [stage], or null if the pet hasn't reached it (Egg shows its Baby options). */
    fun passiveAt(pet: Pet, stage: Stage): Passive? = pet.passives.getOrNull((stage.ordinal - 1).coerceAtLeast(0))

    fun fighterInfo(pet: Pet, stage: Stage = pet.stage) = com.petpal.app.domain.battle.FighterInfo(
        pet.name, pet.species, stage, levelFor(pet.xp), battleMoves(pet), passiveAt(pet, stage),
    )

    /** Moves unlocked by the pet's level that it hasn't been offered yet, in unlock order. */
    fun pendingMoves(pet: Pet): List<Move> {
        val level = levelFor(pet.xp)
        return SpeciesKit.learnset(pet.species).filter { it.level <= level && it.move !in pet.seenMoves }.map { it.move }
    }

    /** The move waiting for a "forget which move?" decision, or null. */
    fun pendingMove(pet: Pet): Move? = pendingMoves(pet).firstOrNull()

    /** Learns new moves while there are free slots; once full they wait in [pendingMove]. */
    fun syncMoves(pet: Pet): Pet {
        val valid = pet.moves.filter { SpeciesKit.canLearn(pet.species, it) }.distinct().take(SpeciesKit.MAX_MOVES)
        var p = if (valid == pet.moves) pet else pet.copy(moves = valid)
        if (p.moves.isEmpty() && p.seenMoves.isEmpty() && levelFor(p.xp) > 1) {
            // Pet from before movesets: keep its newest moves, don't ask about old ones.
            val all = pendingMoves(p)
            return p.copy(moves = all.takeLast(SpeciesKit.MAX_MOVES), seenMoves = all.toSet())
        }
        for (m in pendingMoves(p)) {
            if (p.moves.size >= SpeciesKit.MAX_MOVES) break
            p = p.copy(moves = p.moves + m, seenMoves = p.seenMoves + m)
        }
        return p
    }

    /** Answer to the learn prompt: replace [forget] with [move], or skip [move] when [forget] is null. */
    fun learn(pet: Pet, move: Move, forget: Move?): Pet {
        if (move !in pendingMoves(pet)) return pet
        val moves = if (forget != null && forget in pet.moves) pet.moves.map { if (it == forget) move else it } else pet.moves
        return syncMoves(pet.copy(moves = moves, seenMoves = pet.seenMoves + move))
    }

    /** The current stage's two unique options while the player hasn't picked one yet, else null. */
    fun pendingUnique(pet: Pet): Pair<Move, Move>? =
        SpeciesKit.uniqueOptions(pet.species, pet.stage)?.takeIf { (a, b) -> a !in pet.uniquePicks && b !in pet.uniquePicks }

    /** Locks in [move] as this stage's unique. Only one of the pair can ever be picked. */
    fun pickUnique(pet: Pet, move: Move): Pet {
        val options = pendingUnique(pet) ?: return pet
        if (move != options.first && move != options.second) return pet
        return pet.copy(uniquePicks = pet.uniquePicks + move)
    }

    /** Everything a battle needs: learned moves plus picked uniques. */
    fun battleMoves(pet: Pet): List<Move> = pet.moves + pet.uniquePicks

    /** One relearn point every this many levels (Lv 10, 20, ... 100 = 10 points total). */
    const val RELEARN_EVERY = 10

    fun relearnPoints(pet: Pet): Int = (levelFor(pet.xp) / RELEARN_EVERY - pet.relearnsUsed).coerceAtLeast(0)

    /** Moves the pet already unlocked (learned before or skipped) but doesn't know right now. */
    fun relearnable(pet: Pet): List<Move> {
        val level = levelFor(pet.xp)
        return SpeciesKit.learnset(pet.species)
            .filter { it.level <= level && it.move in pet.seenMoves && it.move !in pet.moves }
            .map { it.move }
    }

    /** Spends a relearn point on [move], replacing [forget] when all slots are full. */
    fun relearn(pet: Pet, move: Move, forget: Move?): Pet {
        if (relearnPoints(pet) <= 0 || move !in relearnable(pet)) return pet
        val moves = when {
            pet.moves.size < SpeciesKit.MAX_MOVES -> pet.moves + move
            forget != null && forget in pet.moves -> pet.moves.map { if (it == forget) move else it }
            else -> return pet
        }
        return pet.copy(moves = moves, relearnsUsed = pet.relearnsUsed + 1)
    }

    /**
     * New pets are unlocked by walking: a random one every 10k steps, or one you pick at 50k.
     * The counter restarts at each unlock. Unlocked pets start as Lv 1 eggs; only the active pet grows.
     */
    enum class UnlockKind(val steps: Long, val label: String) {
        RANDOM(10_000, "Random pet"),
        CHOOSE(50_000, "Choose a pet"),
    }

    /** Steps walked since the newest pet was adopted or unlocked. */
    fun stepsSinceUnlock(pets: List<Pet>, lifetimeSteps: Long) =
        (lifetimeSteps - (pets.maxOfOrNull { it.stepsAtReset } ?: 0)).coerceAtLeast(0)

    fun canUnlock(pets: List<Pet>, lifetimeSteps: Long, kind: UnlockKind) =
        pets.size < Species.entries.size && stepsSinceUnlock(pets, lifetimeSteps) >= kind.steps

    /** A fresh Lv 1 egg of [species], named after it. */
    fun newPet(id: Long, species: Species, now: Long, lifetimeSteps: Long): Pet =
        syncMoves(Pet(id = id, name = species.label, species = species, lastUpdated = now, stepsAtReset = lifetimeSteps))

    const val STEP_XP = 120 // per 1,000 steps
}
