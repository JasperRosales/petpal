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
    LaunchedEffect(Unit) { vm.rewards.collect { snackbar.showSnackbar(it) } }

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
            ) { PetSprite(pet.stage, state.mood, pet.path) }

            PetHeader(pet, state.mood, state.streak)

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val c = MaterialTheme.colorScheme
                    StatBar("Hunger", PetPalIcons.Hunger, pet.hunger, c.tertiary)
                    StatBar("Happiness", PetPalIcons.Happiness, pet.happiness, c.primary)
                    StatBar("Energy", PetPalIcons.Energy, pet.energy, c.secondary)
                }
            }

            if (state.stepsAvailable) {
                Text(
                    "${state.todaySteps} steps today · +XP every 1,000",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                listOf(HabitType.WATER, HabitType.SLEEP, HabitType.CUSTOM).forEach { type ->
                    NeuButton(
                        onClick = { vm.log(type) },
                        icon = PetPalIcons.forHabit(type),
                        text = type.label(),
                        accent = type.accent(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (renaming) {
        RenameDialog(state.pet?.name.orEmpty(), onDismiss = { renaming = false }) {
            vm.rename(it); renaming = false
        }
    }
}

@Composable
private fun HabitType.accent() = when (this) {
    HabitType.WATER, HabitType.FOCUS -> MaterialTheme.colorScheme.secondary
    HabitType.CUSTOM -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun PetHeader(pet: Pet, mood: Mood, streak: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(pet.name, style = MaterialTheme.typography.titleLarge)
            Text(
                "${pet.stage.label()} · ${mood.name.lowercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val next = PetEngine.nextStageXp(pet.xp)
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
