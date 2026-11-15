package com.petpal.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.floor

/**
 * Box with staircase corners, like a pixel-art window: each corner is cut by [steps]
 * square notches of [px] each. A CornerBasedShape so it can go in the Material theme.
 */
class PixelShape(private val px: Dp = 3.dp, private val steps: Int = 2) :
    CornerBasedShape(CornerSize(px * steps), CornerSize(px * steps), CornerSize(px * steps), CornerSize(px * steps)) {

    override fun copy(topStart: CornerSize, topEnd: CornerSize, bottomEnd: CornerSize, bottomStart: CornerSize) = this

    override fun createOutline(
        size: Size, topStart: Float, topEnd: Float, bottomEnd: Float, bottomStart: Float, layoutDirection: LayoutDirection,
    ): Outline {
        // Corner sizes arrive already clamped to the box; derive the step from them.
        val p = floor(topStart / steps).coerceAtLeast(1f)
        return Outline.Generic(pixelPath(size, p, steps.coerceAtMost((minOf(size.width, size.height) / (2 * p)).toInt())))
    }
}

/** Clockwise outline with [n] stair steps of [p] at every corner. */
private fun pixelPath(size: Size, p: Float, n: Int) = Path().apply {
    val w = size.width; val h = size.height
    moveTo(n * p, 0f)
    lineTo(w - n * p, 0f)
    for (i in 1..n) { lineTo(w - (n - i + 1) * p, i * p); lineTo(w - (n - i) * p, i * p) }
    lineTo(w, h - n * p)
    for (i in 1..n) { lineTo(w - i * p, h - (n - i + 1) * p); lineTo(w - i * p, h - (n - i) * p) }
    lineTo(n * p, h)
    for (i in 1..n) { lineTo((n - i + 1) * p, h - i * p); lineTo((n - i) * p, h - i * p) }
    lineTo(0f, n * p)
    for (i in 1..n) { lineTo(i * p, (n - i + 1) * p); lineTo(i * p, (n - i) * p) }
    close()
}

val PixelSmall = PixelShape(2.dp, 1)
val PixelMedium = PixelShape(3.dp, 2)
val PixelLarge = PixelShape(4.dp, 2)

/**
 * Pixel-art panel: a hard drop shadow (no blur), a chunky stepped border and a flat fill.
 * [lift] is how far the panel floats above its shadow; 0 = pressed flat.
 */
fun Modifier.pixelPanel(
    fill: Color,
    border: Color,
    shadow: Color = Color.Black.copy(alpha = 0.55f),
    lift: Dp = 4.dp,
    borderWidth: Dp = 3.dp,
    highlight: Boolean = true,
    steps: Int = 2,
): Modifier = drawBehind {
    val p = floor(borderWidth.toPx()).coerceAtLeast(1f)
    val off = floor(lift.toPx())
    if (off > 0f) translate(off, off) { drawPanel(shadow, p, size, steps) }
    drawPanel(border, p, size, steps)
    val inner = Size(size.width - 2 * p, size.height - 2 * p)
    translate(p, p) {
        drawPanel(fill, p, inner, steps = (steps - 1).coerceAtLeast(1))
        if (highlight) { // 1-cell bevel: light top edge, dark bottom edge
            drawRect(Color.White.copy(alpha = 0.08f), Offset(p, 0f), Size(inner.width - 2 * p, p))
            drawRect(Color.Black.copy(alpha = 0.18f), Offset(p, inner.height - p), Size(inner.width - 2 * p, p))
        }
    }
}

private fun DrawScope.drawPanel(color: Color, p: Float, size: Size, steps: Int = 2) {
    if (size.width <= 0f || size.height <= 0f) return
    drawPath(pixelPath(size, p, steps.coerceAtMost((minOf(size.width, size.height) / (2 * p)).toInt())), color)
}

/** Flat pixel-art card that replaces Material cards. */
@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides contentColorFor(containerColor).takeOrElse(MaterialTheme.colorScheme.onSurface)) {
        // Room for the shadow so it isn't clipped by neighbours
        Box(modifier.padding(end = 4.dp, bottom = 4.dp)) {
            Column(Modifier.pixelPanel(containerColor, borderColor).padding(3.dp), content = content)
        }
    }
}

private fun Color.takeOrElse(other: Color) = if (this == Color.Unspecified) other else this

/**
 * Chunky segmented bar: a dark frame, then square blocks that fill up one at a time.
 * Stand-in for LinearProgressIndicator.
 */
@Composable
fun PixelProgress(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Canvas(modifier.height(10.dp)) {
        val p = floor(2.dp.toPx()).coerceAtLeast(1f)
        val frame = Color.Black.copy(alpha = 0.6f)
        drawRect(frame, Offset(p, 0f), Size(size.width - 2 * p, size.height)) // frame with notched corners
        drawRect(frame, Offset(0f, p), Size(size.width, size.height - 2 * p))
        val inner = Size(size.width - 2 * p, size.height - 2 * p)
        drawRect(trackColor, Offset(p, p), inner)
        val cell = maxOf(inner.height, 3 * p) // blocks roughly square
        val count = maxOf(1, (inner.width / cell).toInt())
        val bw = inner.width / count
        val lit = (progress().coerceIn(0f, 1f) * count)
        for (i in 0 until count) {
            val f = (lit - i).coerceIn(0f, 1f)
            if (f <= 0f) break
            val x = p + i * bw
            drawRect(color, Offset(x, p), Size(bw * f - if (f >= 1f) p else 0f, inner.height))
            drawRect(Color.White.copy(alpha = 0.25f), Offset(x, p), Size(bw * f - if (f >= 1f) p else 0f, p))
        }
    }
}
