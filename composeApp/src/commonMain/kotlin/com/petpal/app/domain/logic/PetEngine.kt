package com.petpal.app.domain.logic

import com.petpal.app.domain.battle.Move
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

    fun stageFor(xp: Int): Stage = when {
        xp >= 1500 -> Stage.ADULT
        xp >= 500 -> Stage.KID
        xp >= 50 -> Stage.BABY
        else -> Stage.EGG
    }

    const val MAX_LEVEL = 60

    /** Level n needs 10 * (n-1)^2 XP: 10, 40, 90, 160 ... */
    fun levelFor(xp: Int): Int = (1 + sqrt(xp.coerceAtLeast(0) / 10.0).toInt()).coerceAtMost(MAX_LEVEL)

    fun xpForLevel(level: Int): Int = 10 * (level - 1) * (level - 1)

    /** XP needed for the next level, or null at max. */
    fun nextLevelXp(xp: Int): Int? = levelFor(xp).let { if (it >= MAX_LEVEL) null else xpForLevel(it + 1) }

    /**
     * XP only comes from things the app can verify: hardware-counted steps and battles
     * (capped per day). Focus sessions are logged but give no XP.
     */
    fun reward(pet: Pet, type: HabitType): Pet = when (type) {
        HabitType.STEPS -> addXp(pet, STEP_XP)
        HabitType.FOCUS -> pet
    }

    fun battleXp(outcome: BattleOutcome): Int = when (outcome) {
        BattleOutcome.WIN -> 30
        BattleOutcome.DRAW -> 15
        BattleOutcome.LOSS -> 10
    }

    fun addXp(pet: Pet, xp: Int): Pet = (pet.xp + xp).let { syncMoves(pet.copy(xp = it, stage = stageFor(it))) }

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

    const val STEP_XP = 20 // per 1,000 steps
    const val MAX_REWARDED_BATTLES_PER_DAY = 3
}
