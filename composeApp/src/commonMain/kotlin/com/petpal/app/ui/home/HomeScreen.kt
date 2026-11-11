package com.petpal.app.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.ui.draw.alpha
import com.petpal.app.domain.battle.Move
import com.petpal.app.domain.battle.SpeciesKit
import com.petpal.app.domain.logic.PetEngine
import com.petpal.app.domain.model.*
import com.petpal.app.ui.components.*
import com.petpal.app.ui.battle.MovePreviewDialog
import com.petpal.app.ui.theme.PetPalIcons
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    snackbar: SnackbarHostState,
    stepPermissionGranted: Boolean,
    requestStepPermission: () -> Unit = {},
    vm: HomeViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var renaming by remember { mutableStateOf(false) }

    LaunchedEffect(stepPermissionGranted) { if (stepPermissionGranted) vm.startStepTracking() }

    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("PetPal") },
            actions = {
                NeuButton(
                    onClick = { renaming = true },
                    icon = PetPalIcons.Edit,
                    contentDescription = "Rename pet",
                    shape = NeuShape.Circle,
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.padding(end = 12.dp),
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        val pet = state.pet ?: return@Column Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GrassyFieldScene(
                isNight = state.isNight,
                modifier = Modifier.fillMaxWidth().height(260.dp).clip(MaterialTheme.shapes.large),
            ) { PetSprite(pet.stage, state.mood, pet.species) }

            PetHeader(pet, state.mood, state.streak)
            PassiveCard(pet)
            EvolutionCard(pet, state.mood)

            if (state.stepsAvailable) {
                val on = state.tracking && stepPermissionGranted
                Text(
                    if (on) "${state.todaySteps} steps today, +${PetEngine.STEP_XP} XP every 1,000 (max 20k/day)"
                    else "${state.todaySteps} steps today. Step tracking is off.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (on) TextButton(onClick = { vm.setTracking(false) }) { Text("Don't track now") }
                else NeuButton(
                    onClick = { if (!stepPermissionGranted) requestStepPermission(); vm.setTracking(true) },
                    text = "Track my steps", icon = PetPalIcons.Steps, filled = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            MovesCard(pet, vm::relearn)
            Spacer(Modifier.height(8.dp))
        }
    }

    if (renaming) {
        val p = state.pet
        if (p != null) RenameDialog(p.name, onDismiss = { renaming = false }) {
            vm.rename(it); renaming = false
        }
    }
}

@Composable
private fun PetHeader(pet: Pet, mood: Mood, streak: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(pet.name, style = MaterialTheme.typography.titleLarge)
            Text(
                "Lv ${PetEngine.levelFor(pet.xp)} ${pet.species.label}, ${pet.stage.label()}, ${mood.name.lowercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val next = PetEngine.nextLevelXp(pet.xp)
        AssistChip(
            onClick = {},
            label = { Text(if (next != null) "${pet.xp}/$next XP" else "${pet.xp} XP") },
            leadingIcon = { Icon(PetPalIcons.Xp, null, tint = MaterialTheme.colorScheme.tertiary) },
        )
        if (streak > 0) {
            AssistChip(
                onClick = {},
                label = { Text("$streak") },
                leadingIcon = { Icon(PetPalIcons.Streak, "Day streak", tint = MaterialTheme.colorScheme.tertiary) },
            )
        }
    }
}

@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename pet") },
        text = { OutlinedTextField(text, { text = it.take(16) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun Long.withCommas() = toString().reversed().chunked(3).joinToString(",").reversed()

/** Every stage of the pet's line, the current one highlighted, and progress to the next evolution. */
@Composable
private fun EvolutionCard(pet: Pet, mood: Mood) {
    val dim = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    var selected by remember(pet.stage) { mutableStateOf(pet.stage) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Evolution", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Stage.entries.forEach { st ->
                    val reached = st <= pet.stage
                    val isSel = st == selected
                    Column(
                        Modifier.weight(1f).clip(MaterialTheme.shapes.small)
                            .border(2.dp, if (isSel) accent else Color.Transparent, MaterialTheme.shapes.small)
                            .clickable { selected = st }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.height(56.dp), contentAlignment = Alignment.BottomCenter) {
                            PetSprite(st, if (st == pet.stage) mood else Mood.HAPPY, pet.species,
                                Modifier.alpha(if (reached) 1f else 0.35f), size = (36 + st.ordinal * 5).dp)
                        }
                        Text(st.label(), style = MaterialTheme.typography.labelSmall,
                            color = if (st == pet.stage) accent else dim)
                        Text("Lv ${PetEngine.stageLevel(st)}", style = MaterialTheme.typography.labelSmall, color = dim)
                    }
                }
            }

            val next = PetEngine.nextStage(pet.stage)
            LinearProgressIndicator(
                progress = { PetEngine.stageProgress(pet) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
            )
            Text(
                if (next == null) "Fully evolved!"
                else PetEngine.xpToNextStage(pet).let { xp ->
                    val k = (xp + PetEngine.STEP_XP - 1) / PetEngine.STEP_XP
                    "${next.label()} at Lv ${PetEngine.stageLevel(next)}: $xp XP to go (about ${k.toLong().times(1000).withCommas()} steps)"
                },
                style = MaterialTheme.typography.bodySmall, color = dim,
            )

            // Details for the tapped stage
            val power = com.petpal.app.domain.battle.BattleStats.power(selected)
            Text(
                selected.label() + (if (selected <= pet.stage) " (reached)" else " (locked)") +
                    " · HP & damage ×" + (power * 100).toInt() / 100.0,
                style = MaterialTheme.typography.bodySmall,
            )
            if (selected != Stage.EGG) {
                val p = SpeciesKit.passive(pet.species, selected)
                Text("Passive:", style = MaterialTheme.typography.bodySmall)
                Text(p.label + ": " + p.description, style = MaterialTheme.typography.labelSmall, color = dim)
            }
            SpeciesKit.uniqueOptions(pet.species, selected)?.let { (a, b) ->
                val picked = listOf(a, b).firstOrNull { it in pet.uniquePicks }
                Text("Unique (pick one):", style = MaterialTheme.typography.bodySmall)
                listOf(a, b).forEach { m ->
                    Text(
                        (if (m == picked) "✓ " else if (picked != null) "✗ " else "• ") + m.label + ": " +
                            (if (m.isBuff) "Buff · " else if (m.damage == 0) "Debuff · " else "") + m.describe(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (picked != null && m != picked) dim.copy(alpha = 0.5f) else dim,
                    )
                }
            }
        }
    }
}

@Composable
private fun PassiveCard(pet: Pet) {
    val passive = PetEngine.passive(pet)
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(PetPalIcons.Xp, null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Passive: ${passive.label}", style = MaterialTheme.typography.titleSmall)
                Text(passive.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MovesCard(pet: Pet, onRelearn: (Move, Move?) -> Unit) {
    val level = PetEngine.levelFor(pet.xp)
    val dim = MaterialTheme.colorScheme.onSurfaceVariant
    val learnset = SpeciesKit.learnset(pet.species)
    var showAll by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Move?>(null) }
    var relearning by remember { mutableStateOf(false) }
    preview?.let { MovePreviewDialog(pet, it) { preview = null } }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val unique = SpeciesKit.unique(pet.species, pet.stage, pet.uniquePicks)
            val used = pet.moves.size + if (unique != null) 1 else 0
            Text("Moves ($used/${SpeciesKit.SLOTS})", style = MaterialTheme.typography.titleSmall)
            pet.moves.forEach { MoveRow(it, it.category.label, onView = { preview = it }) }
            repeat(SpeciesKit.MAX_MOVES - pet.moves.size) { LockedRow("Empty slot", "") }
            if (unique == null) LockedRow("Slot 5: unique", "on hatch")
            else MoveRow(unique, "${pet.stage.label()} unique", onView = { preview = unique })

            val upcoming = learnset.filter { it.level > level }.take(3)
            if (upcoming.isNotEmpty()) {
                Text("Coming up", style = MaterialTheme.typography.titleSmall)
                upcoming.forEach { (m, lv) -> LockedRow(m.label, "Lv $lv") }
            }
            Text(
                "Can learn ${learnset.size} of ${Move.entries.size} moves. Slot 5 is the unique: each stage offers two " +
                    "and you keep the one you pick. See Evolution for every stage's options. " +
                    "Specials have long cooldowns. Uniques need a full energy bar, charged by dealing and taking hits. Tap ▶ to see a move in action.",
                style = MaterialTheme.typography.labelSmall,
                color = dim,
            )
            val points = PetEngine.relearnPoints(pet)
            val options = PetEngine.relearnable(pet)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showAll = true }) { Text("All moves") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { relearning = true }, enabled = points > 0 && options.isNotEmpty()) {
                    Text("Relearn ($points)")
                }
            }
            Text(
                "Earn 1 relearn point every ${PetEngine.RELEARN_EVERY} levels. Spend it to bring back a move you " +
                    "forgot or skipped." + if (points > 0 && options.isEmpty()) " Nothing to relearn yet." else "",
                style = MaterialTheme.typography.labelSmall,
                color = dim,
            )
        }
    }
    if (showAll) MoveListDialog(pet, onDismiss = { showAll = false })
    if (relearning) RelearnDialog(pet, onDismiss = { relearning = false }) { m, forget ->
        onRelearn(m, forget); relearning = false
    }
}

/** Pick an old move to bring back; when all slots are full, also pick the move it replaces. */
@Composable
private fun RelearnDialog(pet: Pet, onDismiss: () -> Unit, onConfirm: (Move, Move?) -> Unit) {
    var pick by remember { mutableStateOf<Move?>(null) }
    var forget by remember { mutableStateOf<Move?>(null) }
    var preview by remember { mutableStateOf<Move?>(null) }
    val vanish = pick?.let { !SpeciesKit.vanishOk(pet.moves, it) } == true
    val full = pet.moves.size >= SpeciesKit.MAX_MOVES || vanish
    preview?.let { MovePreviewDialog(pet, it) { preview = null } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Relearn a move") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Uses 1 of ${PetEngine.relearnPoints(pet)} relearn points.", style = MaterialTheme.typography.bodySmall)
                PetEngine.relearnable(pet).forEach { m ->
                    MoveRow(m, "Lv ${m.level}", Modifier.fillMaxWidth().clickable { pick = m }, onView = { preview = m },
                        leading = { RadioButton(selected = pick == m, onClick = { pick = m }) })
                }
                if (full) {
                    Text(if (vanish) "Only one vanishing move allowed. Replace:" else "Forget which move?",
                        style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    pick?.let { SpeciesKit.forgettable(pet.moves, it) }.orEmpty().ifEmpty { pet.moves }.forEach { m ->
                        MoveRow(m, m.category.label, Modifier.fillMaxWidth().clickable { forget = m },
                            leading = { RadioButton(selected = forget == m, onClick = { forget = m }) })
                    }
                }
            }
        },
        confirmButton = {
            val ok = pick?.let { forget == null || SpeciesKit.forgettable(pet.moves, it).contains(forget) } == true
            TextButton(onClick = { pick?.let { onConfirm(it, forget.takeIf { full }) } }, enabled = ok && (!full || forget != null)) {
                Text("Relearn")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun LockedRow(label: String, unlock: String) {
    val dim = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(0.45f)) {
        Icon(Icons.Rounded.Lock, null, Modifier.size(20.dp), tint = dim)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(unlock, style = MaterialTheme.typography.labelSmall, color = dim)
    }
}
