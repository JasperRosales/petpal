package com.petpal.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.*
import com.petpal.app.domain.model.HabitType

object PetPalIcons {
    // Navigation
    val Home = Icons.Rounded.Home
    val Habits = Icons.Rounded.Checklist
    val Focus = Icons.Rounded.Timer
    val Wardrobe = Icons.Rounded.Checkroom
    val Stats = Icons.Rounded.BarChart
    val Battle = Icons.Rounded.SportsMma
    val Settings = Icons.Rounded.Settings

    // Habits
    val Steps = Icons.AutoMirrored.Rounded.DirectionsWalk

    // Stats
    val Xp = Icons.Rounded.AutoAwesome
    val Streak = Icons.Rounded.LocalFireDepartment

    // Actions
    val Add = Icons.Rounded.Add
    val Play = Icons.Rounded.PlayArrow
    val Pause = Icons.Rounded.Pause
    val Stop = Icons.Rounded.Stop
    val Edit = Icons.Rounded.Edit
    val Notifications = Icons.Rounded.Notifications
    val Grass = Icons.Rounded.Grass
    val Sun = Icons.Rounded.WbSunny
    val Moon = Icons.Rounded.DarkMode

    fun forHabit(type: HabitType) = when (type) {
        HabitType.STEPS -> Steps
        HabitType.FOCUS -> Focus
    }
}
