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
import kotlinx.coroutines.flow.*
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
    petRepository: PetRepository,
    habitRepository: HabitRepository,
) : ViewModel() {
    val state = combine(habitRepository.todayLogs(), habitRepository.streak(), petRepository.battles, ::HabitsState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsState())
}

@Composable
fun HabitsScreen(vm: HabitsViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val tz = remember { TimeZone.currentSystemDefault() }

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
            Text("Battle log", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        }
        if (s.battles.isEmpty()) item {
            Text(
                "No battles with friends yet. Practice battles aren't logged.",
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
                supportingContent = { Text(if (b.xp > 0) "+${b.xp} XP" else "No XP (daily limit)") },
                trailingContent = { Text("${t.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${t.dayOfMonth}, ${t.hour.pad()}:${t.minute.pad()}") },
            )
        }
    }
}

@Composable
private fun HabitCard(type: HabitType, count: Int) {
    val container = when (type) {
        HabitType.FOCUS -> MaterialTheme.colorScheme.secondaryContainer
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
    HabitType.FOCUS -> "Finish a Focus session to log it"
}

private fun Int.pad() = toString().padStart(2, '0')
