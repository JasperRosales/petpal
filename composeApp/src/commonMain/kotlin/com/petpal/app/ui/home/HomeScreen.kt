package com.petpal.app.ui.home

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
import com.petpal.app.domain.battle.BattleRules
import com.petpal.app.domain.battle.Move
import com.petpal.app.domain.battle.SpeciesKit
import com.petpal.app.domain.logic.PetEngine
import com.petpal.app.domain.model.*
import com.petpal.app.ui.components.*
import com.petpal.app.ui.theme.PetPalIcons
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    snackbar: SnackbarHostState,
    stepPermissionGranted: Boolean,
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
            PassiveCard(pet.species)

            if (state.stepsAvailable) {
                Text(
                    "${state.todaySteps} steps today, +${PetEngine.STEP_XP} XP every 1,000 (max 20k/day)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            MovesCard(pet)
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

@Composable
private fun PassiveCard(species: Species) {
    val passive = SpeciesKit.passive(species)
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
private fun MovesCard(pet: Pet) {
    val level = PetEngine.levelFor(pet.xp)
    val dim = MaterialTheme.colorScheme.onSurfaceVariant
    val (kidUnique, adultUnique) = SpeciesKit.uniques(pet.species)
    val learnset = SpeciesKit.learnset(pet.species)
    var showAll by remember { mutableStateOf(false) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val unique = SpeciesKit.unique(pet.species, pet.stage)
            val used = pet.moves.size + if (unique != null) 1 else 0
            Text("Moves ($used/${SpeciesKit.SLOTS})", style = MaterialTheme.typography.titleSmall)
            pet.moves.forEach { MoveRow(it, it.category.label) }
            repeat(SpeciesKit.MAX_MOVES - pet.moves.size) { LockedRow("Empty slot", "") }
            when (unique) {
                null -> LockedRow("Slot 5: ${kidUnique.label}", "at Kid")
                else -> MoveRow(unique, if (unique == kidUnique) "Unique, upgrades at Adult" else "Unique, max")
            }

            val upcoming = learnset.filter { it.level > level }.take(3)
            if (upcoming.isNotEmpty()) {
                Text("Coming up", style = MaterialTheme.typography.titleSmall)
                upcoming.forEach { (m, lv) -> LockedRow(m.label, "Lv $lv") }
            }
            Text(
                "Can learn ${learnset.size} of ${Move.entries.size} moves. Slot 5 is always the unique: " +
                    "${kidUnique.label} at Kid, ${adultUnique.label} at Adult (${BattleRules.UNIQUE_USES}x per battle). " +
                    "Special moves need a full energy bar.",
                style = MaterialTheme.typography.labelSmall,
                color = dim,
            )
            TextButton(onClick = { showAll = true }) { Text("All moves") }
        }
    }
    if (showAll) MoveListDialog(pet, onDismiss = { showAll = false })
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
