package com.petpal.app.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.petpal.app.domain.battle.*
import com.petpal.app.domain.logic.PetEngine
import com.petpal.app.domain.model.Pet
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage
import com.petpal.app.ui.theme.EmberCanvas
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.math.roundToInt

/**
 * Shows [move] used by the player's own pet (its stage, level and passive) on a training dummy,
 * on a loop, using the real battle engine so hitboxes, damage and effects match a real fight.
 */
@Composable
fun MovePreviewDialog(pet: Pet, move: Move, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(move.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(move.describe(), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                MovePreview(pet, move)
                val me = remember(pet) { PetEngine.fighterInfo(pet, if (pet.stage == Stage.EGG) Stage.BABY else pet.stage) }
                Text(whatItDoes(me, move), style = MaterialTheme.typography.bodyMedium)
                Text(howItWorks(move), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
fun MovePreview(pet: Pet, move: Move, modifier: Modifier = Modifier) {
    val stage = if (pet.stage == Stage.EGG) Stage.BABY else pet.stage // eggs can't fight; show the hatchling
    val me = remember(pet, move) { PetEngine.fighterInfo(pet, stage).copy(moves = listOf(move)) }
    val dummy = remember { FighterInfo("Dummy", Species.TURTLE, Stage.ADULT, 1) }
    var fight by remember(move) { mutableStateOf<FightState?>(null) }

    LaunchedEffect(me) {
        val range = BattleStats.range(me, move)
        val gap = when {
            move.isBuff -> 0.4f
            move.shape == HitShape.MELEE -> range * 0.7f
            else -> minOf(range * 0.75f, 0.55f)
        }.coerceAtLeast(0.14f)
        val flight = if (move.shape == HitShape.PROJECTILE) (range / PROJECTILE_SPEED * 1000).toLong() else 0L
        val cycle = 500 + move.windupMs + move.recoverMs + flight + 1300
        while (true) {
            val engine = BattleEngine(listOf(me, dummy), Random(7), demo = true)
            engine.place(0, 0.5f - gap / 2, ARENA_H / 2)
            engine.place(1, 0.5f + gap / 2, ARENA_H / 2)
            val log = mutableListOf<String>()
            var fired = false
            var t = 0L
            while (t <= cycle) {
                if (!fired && t >= 500) { fired = true; engine.command(0, move, t)?.let(log::add) }
                log += engine.tick(t)
                fight = FightState(0, listOf(me, dummy), setOf(0), engine.snapshot(t), log.takeLast(2))
                delay(33)
                t += 33
            }
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.medium).background(EmberCanvas)) {
            fight?.let { Arena(it, Modifier.fillMaxSize(), alerts = false) }
        }
        Text(fight?.log?.lastOrNull() ?: " ", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

private fun secs(ms: Long) = if (ms % 1000 == 0L) "${ms / 1000}s" else "${ms / 100 / 10f}s"

/** What the move does for this pet, with real numbers (its stage, level and passive included). */
internal fun whatItDoes(me: FighterInfo, m: Move): String {
    val hp = BattleStats.maxHp(me)
    val healBoost = 1 + me.passive[Trait.HEAL_BOOST]
    fun heal(pct: Float) = "heals you ${(hp * pct * healBoost).roundToInt()} HP"
    val dmg = BattleStats.baseDamage(me, m)
    val lo = (dmg * 0.85f).roundToInt().coerceAtLeast(1)
    val hi = (dmg * 1.15f).roundToInt().coerceAtLeast(1)
    val hit = if (lo == hi) "$lo" else "$lo-$hi"
    val burnMs = (3000 * (1 + me.passive[Trait.BURN_TIME])).toLong()
    val parts = mutableListOf<String>()
    if (m.damage > 0) parts += when (m.effect) {
        Effect.MULTI -> "Deals $hit damage per hit, 2-4 hits"
        Effect.TWIN_SLASH -> "Two slashes of $hit damage each"
        else -> "Deals $hit damage" + if (m.explodes) " to every enemy in the blast" else ""
    }
    parts += when (m.effect) {
        Effect.NONE, Effect.MULTI, Effect.TWIN_SLASH -> null
        Effect.LUNGE -> "dashes at the target"
        Effect.VANISH -> "vanishes during the wind-up and reappears next to the target"
        Effect.DRAIN -> "heals you half the damage dealt"
        Effect.BURN -> "burns for ${secs(burnMs)} (${(3 * burnMs / 1000f).roundToInt()} HP over time)"
        Effect.SLOW -> "cuts the target's speed by 60% for 3s"
        Effect.STUN -> "stuns for 0.8s"
        Effect.FREEZE -> "freezes the target for 1.5s"
        Effect.KNOCKBACK -> "knocks the target back"
        Effect.PULL -> "pulls the target closer"
        Effect.RECOIL -> "you take a quarter of the damage dealt"
        Effect.LEECH -> "drains 1.5 HP/s from the target to you for 5s"
        Effect.PARALYZE -> "paralyzes for 4s (slower, locks up every second, cooldowns 50% longer)"
        Effect.WEAKEN -> "target deals 35% less damage for 5s"
        Effect.ROOT -> "roots the target in place for 2.5s"
        Effect.SIPHON -> "steals up to 40 energy"
        Effect.EXECUTE -> "double damage on targets below 30% HP"
        Effect.RECHARGE -> "you must recharge for 1.5s after"
        Effect.MARK -> "marked target takes 30% more damage for ${secs(Mechanics.MARK_MS)}"
        Effect.CHAIN -> "arcs to a nearby enemy for half damage (or zaps the same one again for a third)"
        Effect.SWAP -> "you and the target swap places"
        Effect.DISRUPT -> "adds ${secs(Mechanics.DISRUPT_MS)} to all the target's cooldowns"
        Effect.TRAP -> "lays a snare that roots the first enemy to step on it for ${secs(Trap.ROOT_MS)} (lasts ${secs(Trap.DURATION_MS)})"
        Effect.SHIELD -> "shield that absorbs ${(hp * Mechanics.shieldFraction(m) * (1 + me.passive[Trait.SHIELD_BOOST])).roundToInt()} damage for ${secs(Mechanics.SHIELD_MS)}"
        Effect.AID -> heal(Mechanics.AID_HEAL) + " (and each teammate ${(Mechanics.AID_HEAL * 100).roundToInt()}% of their HP)"
        Effect.HEAL -> heal(0.18f)
        Effect.RAGE -> "+30% damage for 4s"
        Effect.GUARD -> "take 60% less damage for 1.2s"
        Effect.HASTE -> "+50% speed and 30% shorter cooldowns for 5s"
        Effect.HARDEN -> "take 40% less damage for 5s"
        Effect.COUNTER -> "the next hit within 1.5s is reflected back"
        Effect.BLINK -> "teleports to the far side of the arena"
        Effect.CLEANSE -> "clears your debuffs and " + heal(0.10f)
        Effect.ENERGIZE -> "+50 energy"
        Effect.SUMMON -> "raises a buried teammate from the ground at half HP"
        Effect.BATTLE_CRY -> "+30% damage and +50% speed for 5s"
        Effect.PACK_CALL -> "+30% damage for 6s and " + heal(0.25f)
        Effect.FORTRESS -> "take 60% less damage for 3s and " + heal(0.15f)
        Effect.ALPHA -> "+30% damage and +50% speed for 8s, and " + heal(0.35f)
        Effect.CITADEL -> "60% less damage for 4s, 40% less for 8s, and " + heal(0.25f)
        Effect.SMOKE -> "leaves a smoke cloud that slows enemies for 4s and dashes you away"
    }.let { listOfNotNull(it) }
    val text = parts.joinToString(", ")
    return text.replaceFirstChar { it.uppercase() } + "."
}

private fun howItWorks(m: Move): String = buildString {
    append(when (m.shape) {
        HitShape.SELF -> "Used on yourself."
        HitShape.MELEE -> "Close-range hit in front of you."
        HitShape.PROJECTILE -> if (m.explodes) "Fires a fireball that explodes on the first enemy it hits, burning everyone nearby."
            else "Fires a shot at where the target stood; it hits the first enemy in its path."
        HitShape.BEAM -> "Instant beam along your aim."
        HitShape.CONE -> "A wave that spreads out in front of you."
        HitShape.STRIKE -> "Lands on the spot you aimed at."
    })
    append(" Wind-up ${m.windupMs} ms: a target that moves away in time dodges it.")
    when (m.category) {
        MoveCategory.SPECIAL -> append(" Strong, with a long cooldown.")
        MoveCategory.UNIQUE -> append(if (m.energyCost < BattleRules.MAX_ENERGY) " No cooldown: needs ${m.energyCost} energy and spends it." else " No cooldown: needs a full energy bar and empties it.")
        MoveCategory.NORMAL -> {}
    }
}
