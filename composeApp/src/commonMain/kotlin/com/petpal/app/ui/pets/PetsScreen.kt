package com.petpal.app.ui.pets

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.petpal.app.data.repository.PetRepository
import com.petpal.app.data.repository.RewardRepository
import com.petpal.app.domain.logic.Events
import com.petpal.app.domain.battle.SpeciesKit
import com.petpal.app.domain.logic.PetEngine
import com.petpal.app.domain.logic.PetEngine.UnlockKind
import com.petpal.app.domain.model.*
import com.petpal.app.ui.components.PetSprite
import com.petpal.app.ui.home.label
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

data class PetsState(
    val pets: List<Pet> = emptyList(),
    val activeId: Long? = null,
    val lifetimeSteps: Long = 0,
    /** It's the 1st, you're #1 on the leaderboard, and this month's free pet hasn't been claimed. */
    val monthlyGift: Boolean = false,
)

class PetsViewModel(private val repo: PetRepository, private val rewards: RewardRepository) : ViewModel() {
    /** Leaderboard #1 check, only made on the 1st. */
    private val champion = flow {
        emit(false)
        if (Events.monthlyOpen(rewards.today())) emit(rewards.isChampion())
    }

    val state = combine(repo.pets, repo.pet, repo.lifetimeSteps, rewards.claimed, champion) { pets, active, lifetime, claimed, champ ->
        val today = rewards.today()
        PetsState(pets, active.id, lifetime,
            monthlyGift = champ && Events.monthlyOpen(today) && Events.monthlyKey(today) !in claimed &&
                pets.size < Species.entries.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetsState())

    fun switchTo(id: Long) = viewModelScope.launch { repo.switchPet(id) }

    /** [onDone] gets the new species, or null if the unlock wasn't ready. */
    fun unlock(kind: UnlockKind, species: Species?, onDone: (Species?) -> Unit) = viewModelScope.launch {
        onDone(repo.unlockPet(kind, species))
    }

    fun claimMonthly(onDone: (Species?) -> Unit) = viewModelScope.launch { onDone(rewards.claimMonthly()) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetsScreen(snackbar: SnackbarHostState, vm: PetsViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var choosing by remember { mutableStateOf(false) }
    val owned = state.pets.map { it.species }.toSet()
    val onUnlocked: (Species?) -> Unit = { got ->
        scope.launch {
            snackbar.showSnackbar(got?.let { "${it.label} egg unlocked! Make it active to grow it." } ?: "Not enough steps yet")
        }
    }

    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Pets") },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.monthlyGift) ElevatedCard(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Champion's gift", style = MaterialTheme.typography.titleSmall)
                        Text("You're #1 on the leaderboard! Claim a free random pet, today only.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    FilledTonalButton(onClick = { vm.claimMonthly(onUnlocked) }) { Text("Claim") }
                }
            }
            UnlockCard(state, onRandom = { vm.unlock(UnlockKind.RANDOM, null, onUnlocked) }, onChoose = { choosing = true })

            Text("Your pets (${state.pets.size}/${Species.entries.size})", style = MaterialTheme.typography.titleSmall)
            Text("Only the active pet earns XP and battles. Switching keeps every pet's level and moves.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.pets.forEach { p -> PetRow(p, active = p.id == state.activeId) { vm.switchTo(p.id) } }

            val locked = Species.entries.filter { it !in owned }
            if (locked.isNotEmpty()) {
                Text("Locked", style = MaterialTheme.typography.titleSmall)
                locked.chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { s ->
                            Column(Modifier.weight(1f).alpha(0.4f), horizontalAlignment = Alignment.CenterHorizontally) {
                                PetSprite(Stage.BABY, Mood.SLEEPY, s, size = 44.dp)
                                Text(s.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                        repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (choosing) ChoosePetDialog(owned, onDismiss = { choosing = false }) { s ->
        choosing = false
        vm.unlock(UnlockKind.CHOOSE, s, onUnlocked)
    }
}

@Composable
private fun UnlockCard(state: PetsState, onRandom: () -> Unit, onChoose: () -> Unit) {
    val walked = PetEngine.stepsSinceUnlock(state.pets, state.lifetimeSteps)
    val allOwned = state.pets.size >= Species.entries.size
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Unlock a pet", style = MaterialTheme.typography.titleSmall)
            Text(if (allOwned) "You've unlocked every pet!" else "Steps since your last unlock: ${walked.withCommas()}",
                style = MaterialTheme.typography.bodySmall)
            if (!allOwned) UnlockKind.entries.forEach { kind ->
                val ready = PetEngine.canUnlock(state.pets, state.lifetimeSteps, kind)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${kind.label} · ${kind.steps.withCommas()} steps", style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            progress = { (walked.toFloat() / kind.steps).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 12.dp),
                            drawStopIndicator = {},
                        )
                    }
                    TextButton(enabled = ready, onClick = if (kind == UnlockKind.CHOOSE) onChoose else onRandom) {
                        Text(if (ready) "Unlock" else "Locked")
                    }
                }
            }
        }
    }
}

@Composable
private fun PetRow(pet: Pet, active: Boolean, onSwitch: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    ElevatedCard(
        Modifier.fillMaxWidth().then(
            if (active) Modifier.border(2.dp, accent, MaterialTheme.shapes.medium) else Modifier
        ),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PetSprite(pet.stage, Mood.HAPPY, pet.species, size = 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(pet.name, style = MaterialTheme.typography.titleSmall)
                Text("Lv ${PetEngine.levelFor(pet.xp)} ${pet.species.label} · ${pet.stage.label()}",
                    style = MaterialTheme.typography.bodySmall)
                Text(SpeciesKit.affinity(pet.species).joinToString("/") { it.label } + " · " + PetEngine.passive(pet).label,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (active) AssistChip(onClick = {}, label = { Text("Active") })
            else FilledTonalButton(onClick = onSwitch) { Text("Switch") }
        }
    }
}

@Composable
private fun ChoosePetDialog(owned: Set<Species>, onDismiss: () -> Unit, onPick: (Species) -> Unit) {
    var sel by remember { mutableStateOf<Species?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a pet to unlock") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Species.entries.filter { it !in owned }.forEach { s ->
                    Row(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { sel = s },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = sel == s, onClick = { sel = s })
                        PetSprite(Stage.BABY, Mood.HAPPY, s, size = 40.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.label, style = MaterialTheme.typography.bodyMedium)
                            Text("${SpeciesKit.affinity(s).joinToString("/") { it.label }} · ${SpeciesKit.passive(s).label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { sel?.let(onPick) }, enabled = sel != null) { Text("Unlock") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun Long.withCommas() = toString().reversed().chunked(3).joinToString(",").reversed()
