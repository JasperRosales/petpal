package com.petpal.app.domain.logic

import com.petpal.app.domain.model.*

object PetEngine {
    private const val HUNGER_DECAY = 4.0 // per hour
    private const val HAPPINESS_DECAY = 3.0
    private const val ENERGY_DECAY = 2.0
    private const val MS_PER_HOUR = 3_600_000.0

    fun applyDecay(pet: Pet, now: Long): Pet {
        if (now <= pet.lastUpdated) return pet // ignore clock rollback
        val hours = (now - pet.lastUpdated) / MS_PER_HOUR
        return pet.copy(
            hunger = (pet.hunger - HUNGER_DECAY * hours).clampStat(),
            happiness = (pet.happiness - HAPPINESS_DECAY * hours).clampStat(),
            energy = (pet.energy - ENERGY_DECAY * hours).clampStat(),
            lastUpdated = now,
        )
    }

    fun moodOf(pet: Pet): Mood = when {
        pet.energy < 15 -> Mood.SLEEPY
        minOf(pet.hunger, pet.happiness, pet.energy) < 30 -> Mood.SAD
        minOf(pet.hunger, pet.happiness, pet.energy) >= 70 -> Mood.HAPPY
        else -> Mood.OKAY
    }

    fun stageFor(xp: Int): Stage = when {
        xp >= 1500 -> Stage.ADULT
        xp >= 500 -> Stage.TEEN
        xp >= 50 -> Stage.BABY
        else -> Stage.EGG
    }

    /** XP threshold of the next stage, or null at ADULT. */
    fun nextStageXp(xp: Int): Int? = when {
        xp < 50 -> 50
        xp < 500 -> 500
        xp < 1500 -> 1500
        else -> null
    }

    fun reward(pet: Pet, type: HabitType): Pet {
        val updated = when (type) {
            HabitType.WATER -> pet.copy(hunger = (pet.hunger + 10).clampStat(), xp = pet.xp + 10)
            HabitType.STEPS -> pet.copy(energy = (pet.energy + 15).clampStat(), xp = pet.xp + 20)
            HabitType.FOCUS -> pet.copy(happiness = (pet.happiness + 15).clampStat(), xp = pet.xp + 25)
            HabitType.SLEEP -> pet.copy(energy = 100.0, xp = pet.xp + 20)
            HabitType.CUSTOM -> pet.copy(happiness = (pet.happiness + 10).clampStat(), xp = pet.xp + 10)
        }
        return updated.copy(stage = stageFor(updated.xp))
    }

    private fun Double.clampStat() = coerceIn(0.0, 100.0)
}
