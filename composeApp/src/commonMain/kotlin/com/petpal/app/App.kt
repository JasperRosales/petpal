package com.petpal.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.petpal.app.ui.components.SplashScreen
import com.petpal.app.ui.components.UpdatePrompt
import com.petpal.app.ui.navigation.PetPalNavHost
import com.petpal.app.ui.theme.PetPalTheme
import org.koin.compose.KoinContext

@Composable
fun App(stepPermissionGranted: Boolean = false, requestStepPermission: () -> Unit = {}) {
    var splash by rememberSaveable { mutableStateOf(true) }
    PetPalTheme {
        KoinContext {
            Box {
                PetPalNavHost(stepPermissionGranted, requestStepPermission)
                AnimatedVisibility(splash, exit = fadeOut(tween(400))) {
                    SplashScreen { splash = false }
                }
                if (!splash) UpdatePrompt()
            }
        }
    }
}
