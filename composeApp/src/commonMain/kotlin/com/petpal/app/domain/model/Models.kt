package com.petpal.app.domain.model

import com.petpal.app.domain.battle.Move

enum class Stage { EGG, BABY, KID, TEEN, ADULT }
enum class EvolutionPath { NONE, ATHLETIC, SCHOLAR, CHILL }
enum class Mood { HAPPY, OKAY, SAD, SLEEPY }
enum class HabitType { STEPS }

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
    PENGUIN("Penguin", "Cool and dependable. You stay steady when things get icy."),
    HEDGEHOG("Hedgehog", "Shy but tough. Soft on the inside, prickly when it counts."),
    BAT("Bat", "A night owl with a big heart. You see things nobody else does."),
    DEER("Deer", "Graceful and gentle. Quick on your feet and kind to everyone."),
    OTTER("Otter", "Playful and social. You turn any chore into a game."),
    PANDA("Panda", "Laid-back and lovable. You take life one snack at a time."),
    LIZARD("Lizard", "Patient and sun-loving. You soak up the good moments."),
    RACCOON("Raccoon", "Resourceful and sneaky. You can make something out of anything."),
    HAMSTER("Hamster", "Busy and cheerful. You always have something stashed away for later."),
    PARROT("Parrot", "Chatty and bright. You light up a room and never run out of things to say."),
    AXOLOTL("Axolotl", "Gentle and resilient. No matter what happens, you bounce back."),
    MOLE("Mole", "Hardworking and curious. You dig deep until you find the answer."),
    KOALA("Koala", "Easygoing and cozy. You recharge fully, then give it your all."),
    SHARK("Shark", "Focused and fearless. Once you set a goal, you never let it go."),
    CHAMELEON("Chameleon", "Adaptable and observant. You fit in anywhere and surprise everyone."),
    BEAVER("Beaver", "Industrious and protective. You build things that keep others safe."),
    MONKEY("Monkey", "Playful and quick-witted. You swing into every adventure with a grin."),
    KANGAROO("Kangaroo", "Strong and spirited. You leap at challenges and never back down."),
    EAGLE("Eagle", "Proud and sharp-eyed. You see the big picture and strike when it counts."),
    SEAL("Seal", "Friendly and easygoing. You bring smiles wherever you go, even on cold days."),
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
    val relearnsUsed: Int = 0, // relearn points spent; see PetEngine.relearnPoints
    val stepsAtReset: Long = 0, // lifetime step total when this pet was adopted/reset; see PetEngine.ResetKind
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
