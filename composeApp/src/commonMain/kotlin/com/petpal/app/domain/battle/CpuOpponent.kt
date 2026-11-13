package com.petpal.app.domain.battle

import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage
import kotlin.random.Random

/** Random wild pets for CPU battles, matched to the player's level and stage. */
object CpuOpponent {
    fun random(me: FighterInfo, rng: Random = Random.Default, prefix: String = "Wild"): FighterInfo {
        val species = Species.entries.random(rng)
        val known = SpeciesKit.learnset(species).filter { it.level <= me.level }.map { it.move }
        // Favour attacks: the 3 hardest hitters (with a little shuffle among near-equals) plus one utility move.
        val (attacks, utility) = known.partition { it.damage > 0 }
        val picks = (attacks.map { it to it.damage + rng.nextInt(4) }.sortedByDescending { it.second }.map { it.first }.take(3) + utility.shuffled(rng).take(1))
            .let { SpeciesKit.oneVanish(it + (known - it.toSet()).shuffled(rng)) }
            .take(SpeciesKit.MAX_MOVES)
        val unique = SpeciesKit.uniqueOptions(species, me.stage)?.let { if (rng.nextBoolean()) it.first else it.second }
        return FighterInfo("$prefix ${species.label}", species, me.stage, me.level, picks + listOfNotNull(unique))
    }

    const val ONLINE_BOT_LEVEL = 50
    const val ONLINE_BOT_HP_SCALE = 2

    /** Stands in online when nobody else is queueing: a random Lv 50 adult with twice the HP. */
    fun onlineBot(rng: Random = Random.Default): FighterInfo {
        val bot = random(FighterInfo("", Species.SPROUT, Stage.ADULT, ONLINE_BOT_LEVEL), rng, prefix = "Bot")
        return bot.copy(maxHpOverride = BattleStats.maxHp(bot) * ONLINE_BOT_HP_SCALE)
    }
}
