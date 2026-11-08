package com.petpal.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petpal.app.data.repository.PetRepository
import com.petpal.app.domain.battle.SpeciesKit
import com.petpal.app.domain.logic.PersonalityQuiz
import com.petpal.app.domain.model.*
import com.petpal.app.ui.battle.icon
import com.petpal.app.ui.components.NeuButton
import com.petpal.app.ui.components.PetSprite
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class OnboardingViewModel(private val petRepository: PetRepository) : ViewModel() {
    var answers by mutableStateOf(listOf<Int>()); private set
    var result by mutableStateOf<Species?>(null); private set
    var started by mutableStateOf(false)

    fun answer(i: Int) {
        answers = answers + i
        if (answers.size == PersonalityQuiz.questions.size) result = PersonalityQuiz.result(answers)
    }

    fun back() { if (result == null && answers.isNotEmpty()) answers = answers.dropLast(1) }

    fun adopt(name: String) {
        val s = result ?: return
        viewModelScope.launch { petRepository.adopt(name, s) }
    }
}

@Composable
fun OnboardingScreen(vm: OnboardingViewModel = koinViewModel()) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val result = vm.result
        AnimatedContent(Triple(result, vm.answers.size, vm.started), label = "quiz") { (r, step, started) ->
            when {
                r != null -> Result(r, vm::adopt)
                !started -> Intro { vm.started = true }
                else -> Question(step, vm)
            }
        }
    }
}

@Composable
private fun Intro(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Icon(Icons.Rounded.Psychology, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Meet your partner", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Answer ${PersonalityQuiz.questions.size} quick questions about yourself. Your answers decide which pet hatches for you. You only get one partner, so answer honestly.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NeuButton(onClick = onStart, text = "Start", icon = Icons.Rounded.PlayArrow, filled = true)
    }
}

@Composable
private fun Question(step: Int, vm: OnboardingViewModel) {
    val q = PersonalityQuiz.questions[step]
    Column(
        Modifier.fillMaxSize().padding(24.dp).systemBarsPadding().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (step == 0) vm.started = false else vm.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            LinearProgressIndicator(
                progress = { step.toFloat() / PersonalityQuiz.questions.size },
                modifier = Modifier.weight(1f),
                drawStopIndicator = {},
            )
            Text("  ${step + 1}/${PersonalityQuiz.questions.size}", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
        Text(q.text, style = MaterialTheme.typography.headlineSmall)
        q.answers.forEachIndexed { i, a ->
            NeuButton(
                onClick = { vm.answer(i) },
                text = a.text,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            )
        }
    }
}

@Composable
private fun Result(species: Species, onAdopt: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val passive = SpeciesKit.passive(species)
    Column(
        Modifier.fillMaxSize().padding(24.dp).systemBarsPadding().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Your partner is a", style = MaterialTheme.typography.titleMedium)
        Text(species.label, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            listOf(Stage.EGG, Stage.BABY, Stage.KID, Stage.ADULT).forEachIndexed { i, st ->
                PetSprite(st, Mood.HAPPY, species, size = (52 + i * 10).dp)
            }
        }
        Text("Egg, Baby, Kid, Adult", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(species.trait, textAlign = TextAlign.Center)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(8.dp))
                    Text("Passive: ${passive.label}", style = MaterialTheme.typography.titleSmall)
                }
                Text(passive.description, style = MaterialTheme.typography.bodySmall)
                Text("Moves", style = MaterialTheme.typography.titleSmall)
                SpeciesKit.loadout(species).forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(m.icon(), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(m.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        OutlinedTextField(
            name, { name = it.take(16) },
            label = { Text("Name your ${species.label.lowercase()}") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        NeuButton(
            onClick = { onAdopt(name) },
            text = "Hatch my egg",
            icon = Icons.Rounded.Egg,
            filled = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Walk to hatch it: your egg hatches at 50 XP (about 3,000 steps).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
