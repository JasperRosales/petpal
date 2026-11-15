package com.petpal.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import com.petpal.app.ui.theme.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sin

/**
 * Low-res pixel backdrop: banded, dithered sky, a blocky sun or moon, clouds or stars,
 * a stepped grassy hill and a strip of soil. Everything snaps to a 4dp grid.
 */
@Composable
fun GrassyFieldScene(
    isNight: Boolean,
    modifier: Modifier = Modifier,
    pet: @Composable () -> Unit,
) {
    val skyTop = if (isNight) NightSkyTop else DaySkyTop
    val skyBottom = if (isNight) NightSkyBottom else DaySkyBottom
    val hill = if (isNight) HillDark else HillLight
    val soil = if (isNight) NightSoil else Soil

    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.matchParentSize()) {
            val px = floor(4.dp.toPx()).coerceAtLeast(1f)
            val cols = ceil(size.width / px).toInt()
            val rows = ceil(size.height / px).toInt()
            fun cell(x: Int, y: Int, c: Color) = drawRect(c, Offset(x * px, y * px), Size(px, px))

            // Sky: 6 flat bands, with a checkerboard row where two bands meet
            val bands = 6
            val bandH = (rows * 0.75f / bands).toInt().coerceAtLeast(1)
            for (y in 0 until rows) {
                val band = (y / bandH).coerceAtMost(bands - 1)
                val col = lerp(skyTop, skyBottom, band / (bands - 1f))
                drawRect(col, Offset(0f, y * px), Size(size.width, px))
                if (y % bandH == 0 && band > 0) {
                    val prev = lerp(skyTop, skyBottom, (band - 1) / (bands - 1f))
                    for (x in 0 until cols step 2) cell(x, y, prev)
                }
            }

            // Sun or moon
            val sx = (cols * 0.8f).toInt(); val sy = (rows * 0.2f).toInt()
            val r = (minOf(cols, rows) * 0.08f).coerceAtLeast(3f)
            disc(sx, sy, r + 1, (if (isNight) Moon else Sunflower).copy(alpha = 0.35f), ::cell)
            disc(sx, sy, r, if (isNight) Moon else Sunflower, ::cell)
            if (isNight) disc(sx + (r * 0.5f).toInt(), sy - (r * 0.3f).toInt(), r * 0.7f, lerp(skyTop, skyBottom, 0.2f), ::cell) // crescent
            else disc(sx - (r * 0.35f).toInt(), sy - (r * 0.35f).toInt(), r * 0.35f, Color(0xFFFFF1B8), ::cell) // shine

            if (isNight) {
                listOf(0.1f to 0.1f, 0.3f to 0.22f, 0.45f to 0.08f, 0.6f to 0.3f, 0.2f to 0.38f, 0.92f to 0.42f).forEach { (fx, fy) ->
                    cell((cols * fx).toInt(), (rows * fy).toInt(), Color.White.copy(alpha = 0.85f))
                }
            } else {
                cloud((cols * 0.12f).toInt(), (rows * 0.16f).toInt(), ::cell)
                cloud((cols * 0.5f).toInt(), (rows * 0.3f).toInt(), ::cell)
            }

            // Stepped hill: one column of cells at a time, with a light grass lip and tufts
            val lip = lerp(hill, Color.White, 0.25f)
            val shade = lerp(hill, Color.Black, 0.18f)
            val ground = rows - (rows * 0.06f).toInt().coerceAtLeast(2)
            for (x in 0 until cols) {
                val t = x / cols.toFloat()
                val top = (rows * (0.66f - 0.08f * sin(t * 3.1f) + 0.02f * t)).toInt()
                drawRect(hill, Offset(x * px, top * px), Size(px, (ground - top) * px))
                cell(x, top, lip)
                if ((x * 7 + 3) % 11 == 0) cell(x, top - 1, hill) // grass tuft
                if ((x * 5) % 9 == 0) cell(x, top + 3 + (x % 4), shade)
            }
            if (isNight) { // fireflies
                listOf(0.15f to 0.72f, 0.32f to 0.82f, 0.68f to 0.76f, 0.88f to 0.86f).forEach { (fx, fy) ->
                    cell((cols * fx).toInt(), (rows * fy).toInt(), Moon)
                }
            }

            // Soil with pebbles
            drawRect(soil, Offset(0f, ground * px), Size(size.width, size.height - ground * px))
            for (x in 0 until cols) {
                cell(x, ground, lerp(soil, Color.Black, 0.25f))
                if ((x * 13) % 7 == 0) cell(x, ground + 1 + x % 2, lerp(soil, Color.White, 0.2f))
            }
        }
        Box(Modifier.padding(bottom = 32.dp)) { pet() }
    }
}

private fun disc(cx: Int, cy: Int, r: Float, c: Color, cell: (Int, Int, Color) -> Unit) {
    val ri = ceil(r).toInt()
    for (y in -ri..ri) for (x in -ri..ri) if (x * x + y * y <= r * r) cell(cx + x, cy + y, c)
}

private fun cloud(x: Int, y: Int, cell: (Int, Int, Color) -> Unit) {
    val white = Color.White.copy(alpha = 0.9f)
    val under = Color(0xFFD6ECF7)
    listOf(
        "..XXX.....",
        ".XXXXXXX..",
        "XXXXXXXXXX",
        ".uuuuuuuu.",
    ).forEachIndexed { dy, row ->
        row.forEachIndexed { dx, ch ->
            when (ch) { 'X' -> cell(x + dx, y + dy, white); 'u' -> cell(x + dx, y + dy, under) }
        }
    }
}
