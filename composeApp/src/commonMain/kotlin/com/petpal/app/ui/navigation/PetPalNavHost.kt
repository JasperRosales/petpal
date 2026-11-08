package com.petpal.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.petpal.app.data.repository.PetRepository
import com.petpal.app.ui.onboarding.OnboardingScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.petpal.app.ui.battle.BattleScreen
import com.petpal.app.ui.focus.FocusScreen
import com.petpal.app.ui.habits.HabitsScreen
import com.petpal.app.ui.home.HomeScreen
import com.petpal.app.ui.stats.StatsScreen
import com.petpal.app.ui.theme.PetPalIcons

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", PetPalIcons.Home),
    Habits("habits", "Habits", PetPalIcons.Habits),
    Focus("focus", "Focus", PetPalIcons.Focus),
    Battle("battle", "Battle", PetPalIcons.Battle),
    Stats("stats", "Stats", PetPalIcons.Stats),
}

class RootViewModel(petRepository: PetRepository) : ViewModel() {
    val hasPet = petRepository.hasPet.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@Composable
fun PetPalNavHost(stepPermissionGranted: Boolean, root: RootViewModel = koinViewModel()) {
    when (root.hasPet.collectAsStateWithLifecycle().value) {
        null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        false -> OnboardingScreen()
        true -> MainTabs(stepPermissionGranted)
    }
}

@Composable
private fun MainTabs(stepPermissionGranted: Boolean) {
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
            composable(Tab.Battle.route) { BattleScreen() }
            composable(Tab.Stats.route) { StatsScreen() }
        }
    }
}
