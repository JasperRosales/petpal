package com.petpal.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Pixel-art button: flat fill, chunky stepped border and a hard drop shadow. Pressing it
 * (or [selected]) pushes it down onto its shadow, like an arcade button.
 */
@Composable
fun NeuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String? = null,
    contentDescription: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    selected: Boolean = false,
    filled: Boolean = false,
    shape: NeuShape = NeuShape.Rounded,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    surface: Color = MaterialTheme.colorScheme.background, // what the button sits on
    /** Flat with a border instead of raised; selected = accent border, tint and a check. */
    outlined: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val down = isPressed || (selected && !outlined)
    val base = MaterialTheme.colorScheme.surfaceContainerHigh.compositeOver(surface)
    val fill = when {
        filled -> accent
        outlined && selected -> accent.copy(alpha = 0.2f).compositeOver(base)
        down -> accent.copy(alpha = 0.12f).compositeOver(base)
        else -> base
    }
    val border = when {
        filled -> Color.Black.copy(alpha = 0.7f).compositeOver(accent)
        selected || !outlined -> accent
        else -> MaterialTheme.colorScheme.outline
    }
    val content = if (filled) MaterialTheme.colorScheme.onPrimary else accent
    val lift = if (down || outlined) 0.dp else 4.dp
    val shownIcon = if (outlined && selected) Icons.Sharp.Check else icon
    val steps = if (shape == NeuShape.Circle) 3 else 2

    // Reserve the shadow's space always, and shift the face into it when pressed.
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.45f)
            .padding(end = 4.dp, bottom = 4.dp)
            .offset(x = 4.dp - lift, y = 4.dp - lift)
            .pixelPanel(fill, border, lift = lift, borderWidth = if (outlined && !selected) 2.dp else 3.dp, steps = steps)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (shownIcon != null) Icon(shownIcon, contentDescription, tint = content, modifier = Modifier.size(20.dp))
            if (shownIcon != null && text != null) Spacer(Modifier.width(8.dp))
            if (text != null) Text(text, color = content, style = MaterialTheme.typography.labelLarge)
        }
    }
}

enum class NeuShape { Rounded, Circle }
