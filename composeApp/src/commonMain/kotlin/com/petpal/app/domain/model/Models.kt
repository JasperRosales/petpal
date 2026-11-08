package com.petpal.app.domain.model

enum class Stage { EGG, BABY, KID, ADULT }
enum class EvolutionPath { NONE, ATHLETIC, SCHOLAR, CHILL }
enum class Mood { HAPPY, OKAY, SAD, SLEEPY }
enum class HabitType { STEPS, WATER, FOCUS, SLEEP, CUSTOM }

/** Partner pet species, assigned once by the personality quiz. Battle kit lives in SpeciesKit. */
enum class Species(val label: String, val trait: String) {
    SPROUT("Sprout", "Gentle and patient. You grow a little every day and help others do the same."),
    KITTY("Kitty", "Independent and curious. You do things your own way and land on your feet."),
    DRAKE("Drake", "Bold and driven. You take charge and bring the fire."),
    BUNNY("Bunny", "Energetic and free. You're always on the move and quick to bounce back."),
    PUP("Pup", "Loyal and warm. Your friends know you'll always show up for them."),
    OWL("Owl", "Thoughtful and wise. You notice what others miss."),
    TURTLE("Turtle", "Calm and steady. You finish what you start, at your own pace."),
    FOX("Fox", "Clever and playful. There's always a trick up your sleeve."),
    FROG("Frog", "Easygoing and fun. You adapt to anything life throws at you."),
    BEAR("Bear", "Strong and caring. You protect the people you love."),
}
enum class BattleOutcome { WIN, LOSS, DRAW }

data class Pet(
    val id: Long = 1,
    val name: String,
    val stage: Stage = Stage.EGG,
    val path: EvolutionPath = EvolutionPath.NONE,
    val species: Species = Species.SPROUT,
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
