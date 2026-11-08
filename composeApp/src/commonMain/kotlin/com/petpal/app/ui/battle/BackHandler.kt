package com.petpal.app.ui.battle

import androidx.compose.runtime.Composable

/** Intercepts the system back button / gesture while [enabled]. */
@Composable
expect fun BattleBackHandler(enabled: Boolean, onBack: () -> Unit)
