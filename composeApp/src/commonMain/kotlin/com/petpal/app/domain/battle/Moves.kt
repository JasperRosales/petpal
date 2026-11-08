package com.petpal.app.domain.battle

import com.petpal.app.domain.model.Species

enum class Effect {
    NONE,
    LUNGE,     // dashes at the target during wind-up
    DRAIN,     // heals for half the damage dealt
    BURN,      // damage over time
    SLOW,      // halves target speed
    STUN,      // target can't move or act briefly
    KNOCKBACK, // big push away
    HEAL,      // self: restore HP
    RAGE,      // self: +30% damage for a while
    GUARD,     // self: take 60% less damage for a while
}

enum class Move(
    val label: String,
    val damage: Int,
    val range: Float,
    val cooldownMs: Long,
    val windupMs: Long,
    val recoverMs: Long,
    val effect: Effect = Effect.NONE,
) {
    PUNCH("Punch", 6, 0.16f, 700, 150, 150),
    CLAW("Claw Swipe", 7, 0.18f, 850, 150, 150),
    BITE("Bite", 8, 0.15f, 1200, 200, 200, Effect.DRAIN),
    TACKLE("Tackle", 10, 0.26f, 1600, 300, 250, Effect.LUNGE),
    ROCK_SLAM("Rock Slam", 14, 0.20f, 3600, 500, 300, Effect.STUN),
    FIRE_BREATH("Fire Breath", 12, 0.50f, 3200, 450, 350, Effect.BURN),
    WATER_JET("Water Jet", 9, 0.55f, 2600, 350, 250, Effect.KNOCKBACK),
    VINE_WHIP("Vine Whip", 8, 0.40f, 2200, 300, 200, Effect.SLOW),
    ICE_SHARD("Ice Shard", 9, 0.50f, 2600, 350, 200, Effect.SLOW),
    THUNDER("Thunder", 13, 0.60f, 3800, 550, 300, Effect.STUN),
    GUST("Gust", 8, 0.50f, 2200, 300, 200, Effect.KNOCKBACK),
    HEAL("Heal", 0, 0f, 7000, 400, 200, Effect.HEAL),
    HOWL("Howl", 0, 0f, 8000, 300, 200, Effect.RAGE),
    GUARD("Guard", 0, 0f, 3500, 0, 0, Effect.GUARD),
    ;

    val isSelf get() = damage == 0
}

enum class Passive(val label: String, val description: String) {
    PHOTOSYNTHESIS("Photosynthesis", "Regenerates 0.6 HP every second."),
    NINE_LIVES("Nine Lives", "Survives the first knockout blow with 1 HP."),
    BLAZE("Blaze", "Fire Breath deals 25% more damage and burns twice as long."),
    QUICK_FEET("Quick Feet", "Moves 25% faster and all cooldowns are 10% shorter."),
    LOYAL_HEART("Loyal Heart", "Deals 25% more damage while below half HP."),
    KEEN_EYE("Keen Eye", "All moves reach 20% further."),
    HARD_SHELL("Hard Shell", "Takes 20% less damage from every hit."),
    TRICKSTER("Trickster", "20% chance to dodge an attack completely."),
    SLIPPERY("Slippery", "Immune to slow and stun."),
    THICK_FUR("Thick Fur", "20% more max HP."),
}

/** Each species' passive and its five moves (Guard is shared by everyone). */
object SpeciesKit {
    fun passive(s: Species) = when (s) {
        Species.SPROUT -> Passive.PHOTOSYNTHESIS
        Species.KITTY -> Passive.NINE_LIVES
        Species.DRAKE -> Passive.BLAZE
        Species.BUNNY -> Passive.QUICK_FEET
        Species.PUP -> Passive.LOYAL_HEART
        Species.OWL -> Passive.KEEN_EYE
        Species.TURTLE -> Passive.HARD_SHELL
        Species.FOX -> Passive.TRICKSTER
        Species.FROG -> Passive.SLIPPERY
        Species.BEAR -> Passive.THICK_FUR
    }

    fun moves(s: Species): List<Move> = when (s) {
        Species.SPROUT -> listOf(Move.TACKLE, Move.PUNCH, Move.VINE_WHIP, Move.WATER_JET, Move.HEAL)
        Species.KITTY -> listOf(Move.PUNCH, Move.CLAW, Move.BITE, Move.TACKLE, Move.HOWL)
        Species.DRAKE -> listOf(Move.PUNCH, Move.TACKLE, Move.BITE, Move.FIRE_BREATH, Move.ROCK_SLAM)
        Species.BUNNY -> listOf(Move.PUNCH, Move.TACKLE, Move.ICE_SHARD, Move.HEAL, Move.HOWL)
        Species.PUP -> listOf(Move.BITE, Move.TACKLE, Move.PUNCH, Move.ROCK_SLAM, Move.HOWL)
        Species.OWL -> listOf(Move.CLAW, Move.GUST, Move.ICE_SHARD, Move.THUNDER, Move.HEAL)
        Species.TURTLE -> listOf(Move.TACKLE, Move.BITE, Move.WATER_JET, Move.ROCK_SLAM, Move.HEAL)
        Species.FOX -> listOf(Move.BITE, Move.CLAW, Move.PUNCH, Move.FIRE_BREATH, Move.HOWL)
        Species.FROG -> listOf(Move.PUNCH, Move.TACKLE, Move.WATER_JET, Move.VINE_WHIP, Move.ICE_SHARD)
        Species.BEAR -> listOf(Move.PUNCH, Move.CLAW, Move.TACKLE, Move.ROCK_SLAM, Move.HOWL)
    }

    /** All buttons shown in battle. */
    fun loadout(s: Species) = moves(s) + Move.GUARD
}
