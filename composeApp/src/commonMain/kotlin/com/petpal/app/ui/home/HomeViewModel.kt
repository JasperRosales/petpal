package com.petpal.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petpal.app.data.repository.*
import com.petpal.app.domain.battle.Move
import com.petpal.app.domain.logic.DayMath
import com.petpal.app.domain.logic.PetEngine
import com.petpal.app.domain.model.*
import com.petpal.app.domain.platform.StepTracking
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
    val lifetimeSteps: Long = 0,
    val tracking: Boolean = true,
)

class HomeViewModel(
    private val petRepository: PetRepository,
    habitRepository: HabitRepository,
    private val stepRepository: StepRepository,
    private val time: TimeProvider,
    private val stepTracking: StepTracking,
) : ViewModel() {

    private val isNight = MutableStateFlow(computeNight())
    private var stepJob: Job? = null

    val state: StateFlow<HomeState> = combine(
        petRepository.pet,
        habitRepository.streak(),
        stepRepository.todaySteps(),
        isNight,
        petRepository.lifetimeSteps.combine(stepTracking.enabled, ::Pair),
    ) { pet, streak, steps, night, (lifetime, tracking) ->
        HomeState(
            lifetimeSteps = lifetime,
            pet = pet,
            mood = PetEngine.mood(night, activeToday = steps > 0 || streak > 0),
            streak = streak,
            todaySteps = steps,
            stepsAvailable = stepRepository.isAvailable,
            isNight = night,
            tracking = tracking,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    init {
        viewModelScope.launch {
            while (isActive) { // keep day/night current while open
                isNight.value = computeNight()
                delay(60_000)
            }
        }
    }

    fun rename(name: String) = viewModelScope.launch { petRepository.rename(name) }
    fun relearn(move: Move, forget: Move?) = viewModelScope.launch { petRepository.relearnMove(move, forget) }

    /** Call once the activity-recognition permission is granted. */
    fun startStepTracking() {
        if (stepJob?.isActive == true || !stepRepository.isAvailable) return
        stepJob = viewModelScope.launch { stepRepository.track() }
    }

    fun setTracking(on: Boolean) = stepTracking.setEnabled(on)

    private fun computeNight(): Boolean {
        val hour = DayMath.hourOf(time.nowMillis(), TimeZone.currentSystemDefault())
        return hour >= 19 || hour < 6
    }
}

fun Stage.label() = name.lowercase().replaceFirstChar { it.uppercase() }
fun HabitType.label() = when (this) {
    HabitType.STEPS -> "Steps"
}
