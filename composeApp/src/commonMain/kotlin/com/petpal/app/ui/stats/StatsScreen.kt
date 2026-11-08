package com.petpal.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.petpal.app.data.repository.PetRepository
import com.petpal.app.data.repository.StepRepository
import com.petpal.app.domain.logic.DayMath
import com.petpal.app.domain.model.BattleOutcome
import com.petpal.app.domain.platform.TimeProvider
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel

data class DayCount(val date: LocalDate, val count: Int)

data class StatsState(
    val steps: List<DayCount> = emptyList(),
    val battles: List<DayCount> = emptyList(),
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val battleXp: Int = 0,
)

/** The two things PetPal tracks: steps and battles. */
class StatsViewModel(stepRepository: StepRepository, petRepository: PetRepository, time: TimeProvider) : ViewModel() {
    private val tz = TimeZone.currentSystemDefault()
    private val today = DayMath.dateOf(time.nowMillis(), tz)
    private val week = (6 downTo 0).map { today.minus(it, DateTimeUnit.DAY) }

    val state = combine(
        stepRepository.stepsFrom(week.first().toString()),
        petRepository.battlesSince(DayMath.startOfDay(week.first(), tz)),
    ) { steps, battles ->
        val perDay = battles.groupingBy { DayMath.dateOf(it.timestamp, tz) }.eachCount()
        StatsState(
            steps = week.map { DayCount(it, (steps[it.toString()] ?: 0L).toInt()) },
            battles = week.map { DayCount(it, perDay[it] ?: 0) },
            wins = battles.count { it.outcome == BattleOutcome.WIN },
            draws = battles.count { it.outcome == BattleOutcome.DRAW },
            losses = battles.count { it.outcome == BattleOutcome.LOSS },
            battleXp = battles.sumOf { it.xp },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())
}

@Composable
fun StatsScreen(vm: StatsViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Last 7 days", style = MaterialTheme.typography.headlineMedium)
        ChartCard("Steps per day", s.steps, unit = "steps", MaterialTheme.colorScheme.primary)
        ChartCard("Battles per day", s.battles, unit = "battles", MaterialTheme.colorScheme.tertiary)

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This week", style = MaterialTheme.typography.titleMedium)
                TotalRow("Steps", s.steps.sumOf { it.count }.withCommas())
                TotalRow("Battles", "${s.battles.sumOf { it.count }}")
                TotalRow("Won / Draw / Lost", "${s.wins} / ${s.draws} / ${s.losses}")
                TotalRow("XP from battles", "+${s.battleXp}")
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, days: List<DayCount>, unit: String, color: androidx.compose.ui.graphics.Color) {
    var selected by remember { mutableStateOf<Int?>(null) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            // Tap a bar to read its value (selective labelling, no number on every bar)
            val sel = selected?.let { days.getOrNull(it) }
            Text(
                sel?.let { "${it.date.dayOfWeek.short()} ${it.date.dayOfMonth}: ${it.count.withCommas()} $unit" }
                    ?: "Tap a bar for details",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            if (days.isNotEmpty()) BarChart(days, selected, color) { selected = if (selected == it) null else it }
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String) = Row {
    Text(label, Modifier.weight(1f))
    Text(value, style = MaterialTheme.typography.labelLarge)
}

private fun Int.withCommas() = toString().reversed().chunked(3).joinToString(",").reversed()

@Composable
private fun BarChart(days: List<DayCount>, selected: Int?, bar: androidx.compose.ui.graphics.Color, onSelect: (Int) -> Unit) {
    val max = (days.maxOf { it.count }).coerceAtLeast(1)
    val dim = bar.copy(alpha = 0.45f)
    val axis = MaterialTheme.colorScheme.outline

    Row(Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        days.forEachIndexed { i, d ->
            Column(
                Modifier.weight(1f).fillMaxHeight().clickable { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(Modifier.weight(1f).fillMaxWidth()) {
                    val w = size.width * 0.55f
                    val h = size.height * d.count / max
                    val color = if (selected == null || selected == i) bar else dim
                    if (h > 0f) roundedTopBar(Offset((size.width - w) / 2, size.height - h), Size(w, h), 4.dp.toPx(), color)
                    drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                }
                Text(
                    d.date.dayOfWeek.short(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Bar with 4dp rounded data-end, square at the baseline. */
private fun DrawScope.roundedTopBar(topLeft: Offset, size: Size, r: Float, color: androidx.compose.ui.graphics.Color) {
    val radius = r.coerceAtMost(size.height).coerceAtMost(size.width / 2)
    val path = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = topLeft.x, top = topLeft.y, right = topLeft.x + size.width, bottom = topLeft.y + size.height,
                topLeftCornerRadius = CornerRadius(radius), topRightCornerRadius = CornerRadius(radius),
            ),
        )
    }
    drawPath(path, color)
}

private fun DayOfWeek.short() = name.take(3).lowercase().replaceFirstChar { it.uppercase() }
