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
                Text(howItWorks(move), style = MaterialTheme.typography.bodySmall)
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
            fight?.let { Arena(it, Modifier.fillMaxSize()) }
        }
        Text(fight?.log?.lastOrNull() ?: " ", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

private fun howItWorks(m: Move): String = buildString {
    append(when (m.shape) {
        HitShape.SELF -> "Used on yourself."
        HitShape.MELEE -> "Close-range hit in front of you."
        HitShape.PROJECTILE -> "Fires a shot at where the target stood; it hits the first enemy in its path."
        HitShape.BEAM -> "Instant beam along your aim."
        HitShape.CONE -> "A wave that spreads out in front of you."
        HitShape.STRIKE -> "Lands on the spot you aimed at."
    })
    append(" Wind-up ${m.windupMs} ms: a target that moves away in time dodges it.")
    when (m.category) {
        MoveCategory.SPECIAL -> append(" Needs a full energy bar and empties it.")
        MoveCategory.UNIQUE -> append(" Usable ${BattleRules.UNIQUE_USES}x per battle.")
        MoveCategory.NORMAL -> if (m.energyCost > 0) append(" Costs ${m.energyCost} energy.")
    }
}
