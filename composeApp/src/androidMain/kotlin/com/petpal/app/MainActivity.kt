package com.petpal.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import com.petpal.app.platform.StepService

class MainActivity : ComponentActivity() {
    private val stepPermission = mutableStateOf(false)

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            stepPermission.value = it
            if (it) StepService.start(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val perm = Manifest.permission.ACTIVITY_RECOGNITION
            if (checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) stepPermission.value = true
            else requestPermission.launch(perm)
        } else {
            stepPermission.value = true // no runtime permission before Android 10
        }

        if (stepPermission.value) StepService.start(this)

        setContent { App(stepPermissionGranted = stepPermission.value) }
    }
}
