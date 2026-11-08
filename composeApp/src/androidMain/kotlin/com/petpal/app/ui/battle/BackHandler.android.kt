package com.petpal.app.ui.battle

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun BattleBackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)
