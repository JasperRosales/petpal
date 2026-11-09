package com.petpal.app.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.petpal.app.domain.platform.StepTracking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Saved on/off switch for step counting; turning it off also stops the background service. */
class AndroidStepTracking(context: Context) : StepTracking {
    private val ctx = context.applicationContext
    private val prefs = ctx.getSharedPreferences("steps", Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ON, true))
    override val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    override var resyncPending: Boolean
        get() = prefs.getBoolean(KEY_RESYNC, false)
        set(v) { prefs.edit().putBoolean(KEY_RESYNC, v).apply() }

    override fun setEnabled(on: Boolean) {
        if (on == _enabled.value) { if (on) StepService.start(ctx); return }
        prefs.edit().putBoolean(KEY_ON, on).apply { if (!on) putBoolean(KEY_RESYNC, true) }.apply()
        _enabled.value = on
        if (on) StepService.start(ctx) else ctx.stopService(Intent(ctx, StepService::class.java))
    }

    companion object {
        private const val KEY_ON = "tracking"
        private const val KEY_RESYNC = "resync"
        fun isEnabled(ctx: Context) = ctx.getSharedPreferences("steps", Context.MODE_PRIVATE).getBoolean(KEY_ON, true)
    }
}

/** The notification's "Don't track now" button. */
class StepPauseReceiver : BroadcastReceiver(), KoinComponent {
    private val tracking: StepTracking by inject()
    override fun onReceive(context: Context, intent: Intent) = tracking.setEnabled(false)
}
