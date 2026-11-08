package com.petpal.app.ui.focus

import com.petpal.app.data.repository.PetRepository
import com.petpal.app.domain.model.HabitType
import com.petpal.app.domain.platform.TimeProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class FocusState(
    val durationMin: Int = 25,
    val remainingMs: Long = 25 * 60_000L,
    val running: Boolean = false,
    val completedToday: Int = 0,
) {
    val progress: Float get() = remainingMs.toFloat() / (durationMin * 60_000L)
}

/**
 * App-scoped focus timer (Koin single) so a session survives tab switches.
 * Remaining time is derived from TimeProvider, not from tick counting.
 */
class FocusSession(
    private val petRepository: PetRepository,
    private val time: TimeProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _state = MutableStateFlow(FocusState())
    val state: StateFlow<FocusState> = _state
    private var endAt = 0L
    private var ticker: Job? = null

    fun setDuration(min: Int) {
        if (_state.value.running) return
        _state.update { it.copy(durationMin = min, remainingMs = min * 60_000L) }
    }

    fun start() {
        if (_state.value.running) return
        endAt = time.nowMillis() + _state.value.remainingMs
        _state.update { it.copy(running = true) }
        ticker = scope.launch {
            while (isActive) {
                val left = (endAt - time.nowMillis()).coerceAtLeast(0)
                _state.update { it.copy(remainingMs = left) }
                if (left == 0L) { complete(); break }
                delay(250)
            }
        }
    }

    fun pause() {
        ticker?.cancel()
        _state.update { it.copy(running = false) }
    }

    fun reset() {
        ticker?.cancel()
        _state.update { it.copy(running = false, remainingMs = it.durationMin * 60_000L) }
    }

    private suspend fun complete() {
        val s = _state.value
        petRepository.logHabit(HabitType.FOCUS, s.durationMin)
        _state.update {
            it.copy(running = false, remainingMs = it.durationMin * 60_000L, completedToday = it.completedToday + 1)
        }
    }
}
