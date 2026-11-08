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
import com.petpal.app.data.repository.HabitRepository
import com.petpal.app.domain.logic.DayMath
import com.petpal.app.domain.model.HabitType
import com.petpal.app.domain.platform.TimeProvider
import com.petpal.app.ui.home.label
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel

data class DayCount(val date: LocalDate, val count: Int)
data class StatsState(val days: List<DayCount> = emptyList(), val totals: Map<HabitType, Int> = emptyMap())

class StatsViewModel(habitRepository: HabitRepository, time: TimeProvider) : ViewModel() {
    private val tz = TimeZone.currentSystemDefault()
    private val today = DayMath.dateOf(time.nowMillis(), tz)

    val state = habitRepository.logsSinceDaysAgo(6).map { logs ->
        val byDay = logs.groupingBy { DayMath.dateOf(it.timestamp, tz) }.eachCount()
        StatsState(
            days = (6 downTo 0).map { ago ->
                val d = today.minus(ago, DateTimeUnit.DAY)
                DayCount(d, byDay[d] ?: 0)
            },
            totals = logs.groupingBy { it.type }.eachCount(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())
}

@Composable
fun StatsScreen(vm: StatsViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Last 7 days", style = MaterialTheme.typography.headlineMedium)

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Habits logged per day", style = MaterialTheme.typography.titleMedium)
                // Tap a bar to read its value (selective labelling, no number on every bar)
                val sel = selected?.let { s.days.getOrNull(it) }
                Text(
                    sel?.let { "${it.date.dayOfWeek.short()} ${it.date.dayOfMonth}: ${it.count} habits" }
                        ?: "Tap a bar for details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                if (s.days.isNotEmpty()) BarChart(s.days, selected) { selected = if (selected == it) null else it }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Totals", style = MaterialTheme.typography.titleMedium)
                HabitType.entries.forEach { t ->
                    Row {
                        Text(t.label(), Modifier.weight(1f))
                        Text("${s.totals[t] ?: 0}", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun BarChart(days: List<DayCount>, selected: Int?, onSelect: (Int) -> Unit) {
    val max = (days.maxOf { it.count }).coerceAtLeast(1)
    val bar = MaterialTheme.colorScheme.primary
    val dim = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
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
