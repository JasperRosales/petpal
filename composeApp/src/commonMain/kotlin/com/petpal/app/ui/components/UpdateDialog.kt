package com.petpal.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petpal.app.domain.platform.AppUpdater
import com.petpal.app.domain.platform.UpdateInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

data class UpdateUiState(
    val update: UpdateInfo? = null,
    val progress: Float? = null, // non-null while downloading
    val error: String? = null,
    val dismissed: Boolean = false,
)

class UpdateViewModel(private val updater: AppUpdater) : ViewModel() {
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()
    val currentVersion get() = updater.currentVersion
    private var download: Job? = null

    init {
        // Quietly check once per launch; no network means no prompt.
        viewModelScope.launch {
            runCatching { updater.check() }.getOrNull()?.let { u -> _state.update { it.copy(update = u) } }
        }
    }

    fun install() {
        val u = _state.value.update ?: return
        if (download?.isActive == true) return
        download = viewModelScope.launch {
            _state.update { it.copy(progress = 0f, error = null) }
            try {
                updater.install(u) { p -> _state.update { it.copy(progress = p) } }
                _state.update { it.copy(progress = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(progress = null, error = e.message ?: "Download failed") }
            }
        }
    }

    fun dismiss() {
        download?.cancel()
        _state.update { it.copy(dismissed = true, progress = null) }
    }
}

/** Offers a newer GitHub release and installs it without leaving the app. */
@Composable
fun UpdatePrompt(vm: UpdateViewModel = koinViewModel()) {
    val s by vm.state.collectAsState()
    val u = s.update ?: return
    if (s.dismissed) return
    AlertDialog(
        onDismissRequest = { if (s.progress == null) vm.dismiss() },
        title = { Text("Update to v${u.version}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("You have v${vm.currentVersion}.", style = MaterialTheme.typography.bodySmall)
                if (u.notes.isNotBlank()) Text(
                    u.notes, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                )
                s.progress?.let {
                    LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth())
                    Text("Downloading ${(it * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                }
                s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = vm::install, enabled = s.progress == null) { Text("Update") }
        },
        dismissButton = {
            TextButton(onClick = vm::dismiss) { Text(if (s.progress != null) "Cancel" else "Later") }
        },
    )
}
