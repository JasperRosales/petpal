package com.petpal.app

import androidx.compose.runtime.Composable
import com.petpal.app.ui.navigation.PetPalNavHost
import com.petpal.app.ui.theme.PetPalTheme
import org.koin.compose.KoinContext

@Composable
fun App(stepPermissionGranted: Boolean = false) {
    PetPalTheme {
        KoinContext {
            PetPalNavHost(stepPermissionGranted)
        }
    }
}
