package com.petpal.app.domain.model

enum class Stage { EGG, BABY, TEEN, ADULT }
enum class EvolutionPath { NONE, ATHLETIC, SCHOLAR, CHILL }
enum class Mood { HAPPY, OKAY, SAD, SLEEPY }
enum class HabitType { STEPS, WATER, FOCUS, SLEEP, CUSTOM }

data class Pet(
    val id: Long = 1,
    val name: String,
    val stage: Stage = Stage.EGG,
    val path: EvolutionPath = EvolutionPath.NONE,
    val hunger: Double = 80.0,
    val happiness: Double = 80.0,
    val energy: Double = 80.0,
    val xp: Int = 0,
    val lastUpdated: Long, // epoch millis
)

data class HabitLog(
    val id: Long = 0,
    val type: HabitType,
    val value: Int,
    val timestamp: Long,
)
