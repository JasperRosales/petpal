package com.petpal.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petpal.app.data.repository.*
import com.petpal.app.domain.logic.DayMath
import com.petpal.app.domain.logic.PetEngine
import com.petpal.app.domain.model.*
import com.petpal.app.domain.platform.TimeProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

data class HomeState(
    val pet: Pet? = null,
    val mood: Mood = Mood.OKAY,
    val streak: Int = 0,
    val todaySteps: Long = 0,
    val stepsAvailable: Boolean = false,
    val isNight: Boolean = false,
)

class HomeViewModel(
    private val petRepository: PetRepository,
    habitRepository: HabitRepository,
    private val stepRepository: StepRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val isNight = MutableStateFlow(computeNight())
    private val _rewards = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val rewards: SharedFlow<String> = _rewards
    private var stepJob: Job? = null

    val state: StateFlow<HomeState> = combine(
        petRepository.pet,
        habitRepository.streak(),
        stepRepository.todaySteps(),
        isNight,
    ) { pet, streak, steps, night ->
        HomeState(
            pet = pet,
            mood = PetEngine.moodOf(pet),
            streak = streak,
            todaySteps = steps,
            stepsAvailable = stepRepository.isAvailable,
            isNight = night,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    init {
        viewModelScope.launch {
            while (isActive) { // keep decay and day/night current while open
                petRepository.refresh()
                isNight.value = computeNight()
                delay(60_000)
            }
        }
    }

    fun log(type: HabitType) = viewModelScope.launch {
        val before = state.value.pet?.stage
        val pet = petRepository.logHabit(type)
        _rewards.tryEmit(
            if (before != null && pet.stage != before) "${pet.name} grew into a ${pet.stage.label()}!"
            else "+XP for ${type.label().lowercase()}",
        )
    }

    fun rename(name: String) = viewModelScope.launch { petRepository.rename(name) }

    /** Call once the activity-recognition permission is granted. */
    fun startStepTracking() {
        if (stepJob?.isActive == true || !stepRepository.isAvailable) return
        stepJob = viewModelScope.launch { stepRepository.track() }
    }

    private fun computeNight(): Boolean {
        val hour = DayMath.hourOf(time.nowMillis(), TimeZone.currentSystemDefault())
        return hour >= 19 || hour < 6
    }
}

fun Stage.label() = name.lowercase().replaceFirstChar { it.uppercase() }
fun HabitType.label() = when (this) {
    HabitType.STEPS -> "Steps"
    HabitType.WATER -> "Water"
    HabitType.FOCUS -> "Focus"
    HabitType.SLEEP -> "Sleep"
    HabitType.CUSTOM -> "Treat"
}
