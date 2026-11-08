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
import com.petpal.app.ui.components.NeuButton
import com.petpal.app.ui.components.NeuShape
import com.petpal.app.ui.theme.PetPalIcons
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel

data class HabitsState(val logs: List<HabitLog> = emptyList(), val streak: Int = 0) {
    fun count(type: HabitType) = logs.count { it.type == type }
}

class HabitsViewModel(
    private val petRepository: PetRepository,
    habitRepository: HabitRepository,
) : ViewModel() {
    val state = combine(habitRepository.todayLogs(), habitRepository.streak(), ::HabitsState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsState())

    fun log(type: HabitType) = viewModelScope.launch { petRepository.logHabit(type) }
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
            HabitCard(type, s.count(type), onLog = if (type.isManual()) ({ vm.log(type) }) else null)
        }
        if (s.logs.isNotEmpty()) {
            item { Text("Log", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            items(s.logs, key = { it.id }) { log ->
                val t = Instant.fromEpochMilliseconds(log.timestamp).toLocalDateTime(tz)
                ListItem(
                    leadingContent = { Icon(PetPalIcons.forHabit(log.type), null) },
                    headlineContent = { Text(log.type.label()) },
                    supportingContent = { Text(log.detail()) },
                    trailingContent = { Text("${t.hour.pad()}:${t.minute.pad()}") },
                )
            }
        }
    }
}

@Composable
private fun HabitCard(type: HabitType, count: Int, onLog: (() -> Unit)?) {
    val container = when (type) {
        HabitType.WATER, HabitType.FOCUS -> MaterialTheme.colorScheme.secondaryContainer
        HabitType.CUSTOM -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
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
            if (onLog != null) {
                Spacer(Modifier.width(8.dp))
                NeuButton(
                    onClick = onLog,
                    icon = PetPalIcons.Add,
                    contentDescription = "Log ${type.label()}",
                    shape = NeuShape.Circle,
                    surface = container,
                    accent = MaterialTheme.colorScheme.onSurface,
                    contentPadding = PaddingValues(10.dp),
                )
            }
        }
    }
}

private fun HabitType.isManual() = this == HabitType.WATER || this == HabitType.SLEEP || this == HabitType.CUSTOM

private fun HabitType.hint() = when (this) {
    HabitType.STEPS -> "Auto: +20 XP per 1,000 steps (max 20k/day)"
    HabitType.WATER -> "Feeds your pet (stats only, no XP)"
    HabitType.FOCUS -> "Finish a Focus session for happiness"
    HabitType.SLEEP -> "Refills energy (no XP)"
    HabitType.CUSTOM -> "Cheers your pet up (no XP)"
}

private fun HabitLog.detail() = when (type) {
    HabitType.STEPS -> "$value steps"
    HabitType.FOCUS -> "$value min"
    else -> "+stats"
}

private fun Int.pad() = toString().padStart(2, '0')
