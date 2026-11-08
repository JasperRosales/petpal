package com.petpal.app.ui.focus

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petpal.app.ui.components.NeuButton
import com.petpal.app.ui.theme.PetPalIcons
import org.koin.compose.viewmodel.koinViewModel

class FocusViewModel(private val session: FocusSession) : ViewModel() {
    val state = session.state
    fun setDuration(min: Int) = session.setDuration(min)
    fun toggle() = if (state.value.running) session.pause() else session.start()
    fun reset() = session.reset()
}

@Composable
fun FocusScreen(vm: FocusViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val totalSec = (s.remainingMs + 999) / 1000

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Text("Focus", style = MaterialTheme.typography.headlineMedium)

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { s.progress },
                modifier = Modifier.size(240.dp),
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
            )
            Text(
                "${totalSec / 60}:${(totalSec % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.displayMedium,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(15, 25, 45).forEach { min ->
                NeuButton(
                    onClick = { vm.setDuration(min) },
                    text = "$min min",
                    selected = s.durationMin == min, // selected = pressed-in
                    enabled = !s.running,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            NeuButton(
                onClick = vm::toggle,
                icon = if (s.running) PetPalIcons.Pause else PetPalIcons.Play,
                text = if (s.running) "Pause" else "Start",
                filled = true,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            )
            NeuButton(
                onClick = vm::reset,
                icon = PetPalIcons.Stop,
                text = "Reset",
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            )
        }

        Text(
            "Finish a session to keep your streak going. Sessions completed: ${s.completedToday}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
