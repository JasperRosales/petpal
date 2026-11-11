package com.petpal.app.ui.battle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import com.petpal.app.domain.platform.LinkKind

/** Wi-Fi/hotspot sockets need no runtime permission. */
@Composable
actual fun rememberLinkPermission(kind: LinkKind, onResult: (granted: Boolean) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onResult)
    return { callback.value(true) }
}
