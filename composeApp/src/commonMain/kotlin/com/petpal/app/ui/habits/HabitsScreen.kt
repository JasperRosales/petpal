package com.petpal.app.ui.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.petpal.app.data.repository.HabitRepository
import com.petpal.app.data.repository.PetRepository
import com.petpal.app.domain.model.*
import com.petpal.app.ui.home.label
import com.petpal.app.ui.theme.PetPalIcons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel

data class HabitsState(
    val logs: List<HabitLog> = emptyList(),
    val streak: Int = 0,
    val battles: List<BattleRecord> = emptyList(),
) {
    fun count(type: HabitType) = logs.count { it.type == type }
}

class HabitsViewModel(
    private val petRepository: PetRepository,
    habitRepository: HabitRepository,
) : ViewModel() {
    val state = combine(habitRepository.todayLogs(), habitRepository.streak(), petRepository.battles, ::HabitsState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsState())

    fun deleteBattle(id: Long) = viewModelScope.launch { petRepository.deleteBattle(id) }
    fun clearBattles() = viewModelScope.launch { petRepository.clearBattles() }
}

@Composable
fun HabitsScreen(vm: HabitsViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val tz = remember { TimeZone.currentSystemDefault() }
    var confirmClear by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Today", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                Icon(PetPalIcons.Streak, null, tint = MaterialTheme.colorScheme.tertiary)
                Text(" ${s.streak}-day streak", style = MaterialTheme.typography.labelLarge)
            }
        }
        items(HabitType.entries.toList()) { type ->
            HabitCard(type, s.count(type))
        }
        item {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Battle log", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (s.battles.isNotEmpty()) TextButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Rounded.DeleteSweep, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clear all")
                }
            }
        }
        if (s.battles.isEmpty()) item {
            Text(
                "No battles yet. CPU battles give reduced XP.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(s.battles, key = { it.id }) { b ->
            val t = Instant.fromEpochMilliseconds(b.timestamp).toLocalDateTime(tz)
            val (label, color) = when (b.outcome) {
                BattleOutcome.WIN -> "Won" to MaterialTheme.colorScheme.primary
                BattleOutcome.LOSS -> "Lost" to MaterialTheme.colorScheme.error
                BattleOutcome.DRAW -> "Draw" to MaterialTheme.colorScheme.onSurfaceVariant
            }
            ListItem(
                leadingContent = { Icon(PetPalIcons.Battle, null, tint = color) },
                headlineContent = { Text("$label vs ${b.opponent}") },
                supportingContent = {
                    val date = "${t.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${t.dayOfMonth}, ${t.hour.pad()}:${t.minute.pad()}"
                    Text("${if (b.xp > 0) "+${b.xp} XP" else "No XP"} · $date")
                },
                trailingContent = {
                    IconButton(onClick = { vm.deleteBattle(b.id) }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove this entry",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                modifier = Modifier.animateItem(),
            )
        }
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Clear battle log?") },
        text = { Text("Removes all ${s.battles.size} entries. Your XP and level stay the same.") },
        confirmButton = {
            TextButton(onClick = { vm.clearBattles(); confirmClear = false }) {
                Text("Clear all", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
    )
}

@Composable
private fun HabitCard(type: HabitType, count: Int) {
    val container = when (type) {
        HabitType.STEPS -> MaterialTheme.colorScheme.primaryContainer
    }
    Card(colors = CardDefaults.cardColors(containerColor = container)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(PetPalIcons.forHabit(type), null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(type.label(), style = MaterialTheme.typography.titleMedium)
                Text(type.hint(), style = MaterialTheme.typography.bodySmall)
            }
            Text("x$count", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun HabitType.hint() = when (this) {
    HabitType.STEPS -> "Auto: +20 XP per 1,000 steps (max 20k/day)"
}

private fun Int.pad() = toString().padStart(2, '0')
