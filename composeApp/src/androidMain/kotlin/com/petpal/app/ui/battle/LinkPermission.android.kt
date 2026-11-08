package com.petpal.app.ui.battle

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.petpal.app.domain.platform.LinkKind

@Composable
actual fun rememberLinkPermission(kind: LinkKind, onResult: (granted: Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onResult)
    val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?)?.adapter

    val enable = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        callback.value(adapter?.isEnabled == true)
    }
    val perms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        when {
            !res.values.all { it } -> callback.value(false)
            adapter?.isEnabled == true -> callback.value(true)
            else -> enable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }
    return {
        when (kind) {
            LinkKind.HOTSPOT -> callback.value(true) // sockets need no runtime permission
            LinkKind.BLUETOOTH -> perms.launch(
                if (Build.VERSION.SDK_INT >= 31) arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                ) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }
    }
}
