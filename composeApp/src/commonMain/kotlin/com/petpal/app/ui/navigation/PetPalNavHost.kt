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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.petpal.app.domain.battle.Move
import com.petpal.app.domain.logic.PetEngine
import com.petpal.app.ui.home.LearnMoveDialog
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
import com.petpal.app.ui.battle.BattleViewModel
import com.petpal.app.ui.battle.LocalBattleFullscreen
import com.petpal.app.ui.habits.HabitsScreen
import com.petpal.app.ui.home.HomeScreen
import com.petpal.app.ui.pets.PetsScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pets
import com.petpal.app.ui.stats.StatsScreen
import com.petpal.app.ui.theme.PetPalIcons

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", PetPalIcons.Home),
    Pets("pets", "Pets", Icons.Rounded.Pets),
    Habits("habits", "Habits", PetPalIcons.Habits),
    Battle("battle", "Battle", PetPalIcons.Battle),
    Stats("stats", "Stats", PetPalIcons.Stats),
}

class RootViewModel(private val petRepository: PetRepository) : ViewModel() {
    val hasPet = petRepository.hasPet.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Pet plus the move it wants to learn while its moveset is full. */
    val learnPrompt = petRepository.pet
        .map { pet -> PetEngine.pendingMove(pet)?.let { pet to it } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun learn(move: Move, forget: Move?) = viewModelScope.launch { petRepository.learnMove(move, forget) }

    /** Pet plus its new stage's two unique options, until one is picked. */
    val uniquePrompt = petRepository.pet
        .map { pet -> PetEngine.pendingUnique(pet)?.let { pet to it } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun pickUnique(move: Move) = viewModelScope.launch { petRepository.pickUnique(move) }
}

@Composable
fun PetPalNavHost(stepPermissionGranted: Boolean, requestStepPermission: () -> Unit, root: RootViewModel = koinViewModel()) {
    when (root.hasPet.collectAsStateWithLifecycle().value) {
        null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        false -> OnboardingScreen()
        true -> {
            MainTabs(stepPermissionGranted, requestStepPermission)
            val uniquePrompt = root.uniquePrompt.collectAsStateWithLifecycle().value
            val prompt = root.learnPrompt.collectAsStateWithLifecycle().value
            if (uniquePrompt != null) {
                val (pet, options) = uniquePrompt
                com.petpal.app.ui.home.UniquePickDialog(pet, options) { root.pickUnique(it) }
            } else if (prompt != null) {
                val (pet, move) = prompt
                LearnMoveDialog(pet, move) { forget -> root.learn(move, forget) }
            }
        }
    }
}

@Composable
private fun MainTabs(stepPermissionGranted: Boolean, requestStepPermission: () -> Unit) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val entry by nav.currentBackStackEntryAsState()
    val battleFullscreen = remember { mutableStateOf(false) }
    // Activity-scoped so leaving the Battle tab (back gesture, tab switch) never kills a live match.
    val battleVm = koinViewModel<BattleViewModel>()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!battleFullscreen.value) NavigationBar {
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
        CompositionLocalProvider(LocalBattleFullscreen provides battleFullscreen) {
        NavHost(nav, startDestination = Tab.Home.route, modifier = Modifier.padding(padding)) {
            composable(Tab.Home.route) { HomeScreen(snackbar, stepPermissionGranted, requestStepPermission) }
            composable(Tab.Pets.route) { PetsScreen(snackbar) }
            composable(Tab.Habits.route) { HabitsScreen() }
            composable(Tab.Battle.route) { BattleScreen(battleVm) }
            composable(Tab.Stats.route) { StatsScreen() }
        }
        }
    }
}
