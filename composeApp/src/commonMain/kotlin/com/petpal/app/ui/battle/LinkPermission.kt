package com.petpal.app.ui.battle

import androidx.compose.runtime.Composable
import com.petpal.app.domain.platform.LinkKind

/** Returns a launcher that asks for the runtime permissions [kind] needs, then calls back. */
@Composable
expect fun rememberLinkPermission(kind: LinkKind, onResult: (granted: Boolean) -> Unit): () -> Unit
