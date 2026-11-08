package com.petpal.app.domain.model

import com.petpal.app.domain.battle.Move

enum class Stage { EGG, BABY, KID, ADULT }
enum class EvolutionPath { NONE, ATHLETIC, SCHOLAR, CHILL }
enum class Mood { HAPPY, OKAY, SAD, SLEEPY }
enum class HabitType { STEPS, FOCUS }

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
    val xp: Int = 0,
    val lastUpdated: Long, // epoch millis
    val moves: List<Move> = emptyList(), // learned moves, at most SpeciesKit.MAX_MOVES
    val seenMoves: Set<Move> = emptySet(), // moves already offered (learned or declined)
)

data class BattleRecord(
    val id: Long,
    val opponent: String,
    val outcome: BattleOutcome,
    val xp: Int,
    val timestamp: Long,
)

data class HabitLog(
    val id: Long = 0,
    val type: HabitType,
    val value: Int,
    val timestamp: Long,
)
