package com.petpal.app.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.petpal.app.MainActivity
import com.petpal.app.R
import com.petpal.app.data.repository.StepRepository
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject

/**
 * Keeps the step sensor registered while the app is closed. Many phones only
 * count steps while a listener is active, so without this steps are lost.
 */
class StepService : Service() {
    private val steps: StepRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!canTrack(this)) { stopSelf(); return START_NOT_STICKY }
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0
        try {
            ServiceCompat.startForeground(this, NOTIF_ID, notification(), type)
        } catch (e: Exception) {
            stopSelf(); return START_NOT_STICKY // background start not allowed right now
        }
        if (job?.isActive != true) job = scope.launch { steps.track() }
        return START_STICKY
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    private fun notification(): android.app.Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Step counting", NotificationManager.IMPORTANCE_MIN)
                .apply { setShowBadge(false) }
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("PetPal is counting your steps")
            .setContentIntent(open)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL = "steps"
        private const val NOTIF_ID = 1001

        fun canTrack(ctx: Context): Boolean =
            AndroidStepCounter(ctx).isAvailable && (Build.VERSION.SDK_INT < 29 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED)

        fun start(ctx: Context) {
            if (!canTrack(ctx)) return
            try {
                ContextCompat.startForegroundService(ctx, Intent(ctx, StepService::class.java))
            } catch (_: Exception) { }
        }
    }
}

/** Restarts step counting after a reboot or app update. */
class StepBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = StepService.start(context)
}
