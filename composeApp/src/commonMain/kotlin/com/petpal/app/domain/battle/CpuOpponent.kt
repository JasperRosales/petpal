package com.petpal.app.domain.battle

import com.petpal.app.domain.model.Species
import kotlin.random.Random

/** Random wild pets for CPU battles, matched to the player's level and stage. */
object CpuOpponent {
    fun random(me: FighterInfo, rng: Random = Random.Default, prefix: String = "Wild"): FighterInfo {
        val species = Species.entries.random(rng)
        val known = SpeciesKit.learnset(species).filter { it.level <= me.level }.map { it.move }
        // Favour attacks: up to 3 damaging moves plus one utility move, picked at random.
        val (attacks, utility) = known.partition { it.damage > 0 }
        val picks = (attacks.shuffled(rng).take(3) + utility.shuffled(rng).take(1))
            .let { it + (known - it.toSet()).shuffled(rng) }
            .take(SpeciesKit.MAX_MOVES)
        val unique = SpeciesKit.uniqueOptions(species, me.stage)?.let { if (rng.nextBoolean()) it.first else it.second }
        val passive = SpeciesKit.passiveOptions(species, me.stage).let { if (rng.nextBoolean()) it.first else it.second }
        return FighterInfo("$prefix ${species.label}", species, me.stage, me.level, picks + listOfNotNull(unique), passive)
    }
}
