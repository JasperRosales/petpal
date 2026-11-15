package com.petpal.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.DirectionsWalk
import androidx.compose.material.icons.sharp.*
import com.petpal.app.domain.model.HabitType

object PetPalIcons {
    // Navigation
    val Home = Icons.Sharp.Home
    val Habits = Icons.Sharp.Checklist
    val Wardrobe = Icons.Sharp.Checkroom
    val Stats = Icons.Sharp.BarChart
    val Battle = Icons.Sharp.SportsMma
    val Settings = Icons.Sharp.Settings

    // Habits
    val Steps = Icons.AutoMirrored.Sharp.DirectionsWalk

    // Stats
    val Xp = Icons.Sharp.AutoAwesome
    val Streak = Icons.Sharp.LocalFireDepartment

    // Actions
    val Add = Icons.Sharp.Add
    val Play = Icons.Sharp.PlayArrow
    val Pause = Icons.Sharp.Pause
    val Stop = Icons.Sharp.Stop
    val Edit = Icons.Sharp.Edit
    val Notifications = Icons.Sharp.Notifications
    val Grass = Icons.Sharp.Grass
    val Sun = Icons.Sharp.WbSunny
    val Moon = Icons.Sharp.DarkMode

    fun forHabit(type: HabitType) = when (type) {
        HabitType.STEPS -> Steps
    }
}
