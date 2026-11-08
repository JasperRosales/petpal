package com.petpal.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Neumorphic ("soft UI") surface: same color as the background, lifted by a dark
 * shadow bottom-right and a soft highlight top-left. When pressed (or [pressed] is
 * forced, e.g. a selected toggle) the shadows collapse and an inset shade appears.
 *
 * Shadows are faked with stacked translucent rounded rects so this stays pure common code.
 */
fun Modifier.neumorphic(
    cornerRadius: Dp,
    depth: Float, // 1 = raised, 0 = pressed/inset
    background: Color,
    shadow: Color,
    highlight: Color,
    elevation: Dp = 6.dp,
): Modifier = drawBehind {
    val r = cornerRadius.toPx()
    val off = elevation.toPx() * depth
    val blurSteps = 8
    val spread = elevation.toPx() * 1.2f

    if (depth > 0.01f) {
        fun layer(color: Color, dx: Float, dy: Float) {
            for (i in blurSteps downTo 1) {
                val grow = spread * i / blurSteps
                drawRoundRect(
                    color = color.copy(alpha = color.alpha * depth / (blurSteps * 0.9f)),
                    topLeft = Offset(dx - grow, dy - grow),
                    size = size.copy(width = size.width + grow * 2, height = size.height + grow * 2),
                    cornerRadius = CornerRadius(r + grow),
                )
            }
        }
        layer(highlight, -off, -off)
        layer(shadow, off, off)
    }

    drawRoundRect(background, cornerRadius = CornerRadius(r))

    if (depth < 0.99f) insetShade(r, 1f - depth, shadow, highlight)
}

private fun DrawScope.insetShade(r: Float, amount: Float, shadow: Color, highlight: Color) {
    drawRoundRect(
        brush = Brush.linearGradient(
            0f to shadow.copy(alpha = shadow.alpha * 0.55f * amount),
            0.45f to Color.Transparent,
            0.8f to Color.Transparent,
            1f to highlight.copy(alpha = highlight.alpha * 0.5f * amount),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
        cornerRadius = CornerRadius(r),
    )
}

/** Theme-derived neumorphic colors. Dark-heavy shadows in both themes. */
@Composable
fun neuColors(surface: Color = MaterialTheme.colorScheme.background): Triple<Color, Color, Color> {
    val isDark = surface.luminance() < 0.5f
    val shadow = if (isDark) Color.Black.copy(alpha = 0.85f) else Color(0xFF0B1A0E).copy(alpha = 0.32f)
    val highlight = if (isDark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.95f)
    return Triple(surface, shadow, highlight)
}

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
    surface: Color = MaterialTheme.colorScheme.background, // must match what the button sits on
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val depth by animateFloatAsState(if (isPressed || selected) 0f else 1f, label = "neuDepth")
    val (bg, shadow, highlight) = neuColors(surface)
    val fill = if (filled) accent else bg
    val content = when {
        filled -> MaterialTheme.colorScheme.onPrimary
        else -> accent
    }
    val radius = when (shape) { NeuShape.Rounded -> 18.dp; NeuShape.Circle -> 100.dp }

    Box(
        modifier
            .alpha(if (enabled) 1f else 0.45f)
            .neumorphic(radius, depth, fill, shadow, highlight)
            .clip(if (shape == NeuShape.Circle) CircleShape else RoundedCornerShape(radius))
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, contentDescription, tint = content, modifier = Modifier.size(20.dp))
            if (icon != null && text != null) Spacer(Modifier.width(8.dp))
            if (text != null) Text(text, color = content, style = MaterialTheme.typography.labelLarge)
        }
    }
}

enum class NeuShape { Rounded, Circle }
