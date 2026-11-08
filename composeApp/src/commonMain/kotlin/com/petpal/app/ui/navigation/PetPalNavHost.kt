package com.petpal.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.petpal.app.ui.focus.FocusScreen
import com.petpal.app.ui.habits.HabitsScreen
import com.petpal.app.ui.home.HomeScreen
import com.petpal.app.ui.stats.StatsScreen
import com.petpal.app.ui.theme.PetPalIcons

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", PetPalIcons.Home),
    Habits("habits", "Habits", PetPalIcons.Habits),
    Focus("focus", "Focus", PetPalIcons.Focus),
    Stats("stats", "Stats", PetPalIcons.Stats),
}

@Composable
fun PetPalNavHost(stepPermissionGranted: Boolean) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val entry by nav.currentBackStackEntryAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = entry?.destination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.Home.route, modifier = Modifier.padding(padding)) {
            composable(Tab.Home.route) { HomeScreen(snackbar, stepPermissionGranted) }
            composable(Tab.Habits.route) { HabitsScreen() }
            composable(Tab.Focus.route) { FocusScreen() }
            composable(Tab.Stats.route) { StatsScreen() }
        }
    }
}
