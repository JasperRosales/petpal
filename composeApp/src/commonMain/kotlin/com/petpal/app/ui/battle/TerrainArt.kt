package com.petpal.app.ui.battle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.petpal.app.domain.battle.Terrain
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Ground colors (top, bottom) and accent for each element terrain. Kept dark so pets stay readable. */
private fun Terrain.palette(): Triple<Color, Color, Color> = when (this) {
    Terrain.ARENA -> Triple(Color(0xFF24201A), Color(0xFF181612), Color(0xFFFF6D1F))
    Terrain.VOLCANO -> Triple(Color(0xFF2E1B16), Color(0xFF170D0A), Color(0xFFFF5A1F))
    Terrain.LAKESHORE -> Triple(Color(0xFF5A4D35), Color(0xFF3B3324), Color(0xFF4FA3E0))
    Terrain.FOREST -> Triple(Color(0xFF223D25), Color(0xFF142417), Color(0xFF7BC86C))
    Terrain.TUNDRA -> Triple(Color(0xFF6E8696), Color(0xFF4C6170), Color(0xFFDDF3FF))
    Terrain.THUNDER_PLAINS -> Triple(Color(0xFF2B311F), Color(0xFF191D12), Color(0xFFFFD54F))
    Terrain.CANYON -> Triple(Color(0xFF6A3A24), Color(0xFF3F2114), Color(0xFFE0A36A))
    Terrain.WINDY_CLIFFS -> Triple(Color(0xFF4F5A64), Color(0xFF30373F), Color(0xFFBFE3FF))
}

/** Accent color of a terrain, for its HUD badge. */
fun Terrain.accent(): Color = palette().third

/** Floor for any non-arena terrain: ground, scenery, a faint centre ring and a vignette. */
internal fun DrawScope.drawTerrainFloor(t: Terrain) {
    val (top, bottom, accent) = t.palette()
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
    val rng = Random(t.ordinal * 31 + 7) // same scenery every time this map is played
    fun p(x: Float, y: Float) = Offset(x * w, y * h)

    when (t) {
        Terrain.VOLCANO -> {
            repeat(5) { // glowing lava cracks
                val path = Path()
                var x = rng.nextFloat(); var y = rng.nextFloat()
                path.moveTo(x * w, y * h)
                repeat(5) {
                    x += (rng.nextFloat() - 0.5f) * 0.18f; y += (rng.nextFloat() - 0.5f) * 0.14f
                    path.lineTo(x * w, y * h)
                }
                drawPath(path, accent.copy(alpha = 0.25f), style = Stroke(9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(path, Color(0xFFFFB35C).copy(alpha = 0.8f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            repeat(26) { drawCircle(accent.copy(alpha = 0.35f), (1 + rng.nextFloat() * 2).dp.toPx(), p(rng.nextFloat(), rng.nextFloat())) }
        }
        Terrain.LAKESHORE -> {
            val water = Path().apply { // lake along the top with a wavy shore
                moveTo(0f, 0f); lineTo(w, 0f)
                var x = w
                while (x >= 0f) { lineTo(x, h * (0.2f + 0.025f * sin(x / w * 4 * PI).toFloat())); x -= w / 40 }
                lineTo(0f, h * 0.2f); close()
            }
            drawPath(water, Brush.verticalGradient(listOf(Color(0xFF1E4A6E), Color(0xFF2B6A96)), 0f, h * 0.23f))
            repeat(6) {
                val c = p(0.1f + rng.nextFloat() * 0.8f, 0.04f + rng.nextFloat() * 0.12f)
                drawOval(Color.White.copy(alpha = 0.18f), Offset(c.x - 14.dp.toPx(), c.y), Size(28.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(1.dp.toPx()))
            }
            repeat(18) { drawCircle(Color(0xFF8C7B5A).copy(alpha = 0.55f), (2 + rng.nextFloat() * 3).dp.toPx(), p(rng.nextFloat(), 0.3f + rng.nextFloat() * 0.7f)) }
        }
        Terrain.FOREST -> {
            repeat(30) { // grass tufts
                val c = p(rng.nextFloat(), rng.nextFloat())
                for (k in -1..1) drawLine(accent.copy(alpha = 0.3f), c, Offset(c.x + k * 3.dp.toPx(), c.y - 7.dp.toPx()), 1.5.dp.toPx())
            }
            // tree canopies around the edges (trunks hidden off-screen)
            listOf(0f to 0.1f, 0.02f to 0.45f, 0f to 0.85f, 1f to 0.2f, 0.98f to 0.6f, 1f to 0.95f, 0.5f to -0.02f, 0.3f to 1.02f)
                .forEach { (x, y) ->
                    val c = p(x, y)
                    val r = (34 + rng.nextFloat() * 14).dp.toPx()
                    drawCircle(Color(0xFF0E1F10), r * 1.05f, c)
                    drawCircle(Color(0xFF2E5A2F), r, c)
                    drawCircle(Color(0xFF3F7A3C).copy(alpha = 0.7f), r * 0.55f, Offset(c.x - r * 0.25f, c.y - r * 0.25f))
                }
        }
        Terrain.TUNDRA -> {
            repeat(6) { // slick ice patches
                val c = p(rng.nextFloat(), rng.nextFloat())
                val s = Size((60 + rng.nextFloat() * 70).dp.toPx(), (24 + rng.nextFloat() * 24).dp.toPx())
                drawOval(Color(0xFFB8E4F8).copy(alpha = 0.35f), Offset(c.x - s.width / 2, c.y - s.height / 2), s)
                drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x - s.width * 0.25f, c.y - s.height * 0.15f),
                    Offset(c.x + s.width * 0.05f, c.y - s.height * 0.3f), 2.dp.toPx(), StrokeCap.Round)
            }
            repeat(40) { drawCircle(Color.White.copy(alpha = 0.55f), (1 + rng.nextFloat() * 2).dp.toPx(), p(rng.nextFloat(), rng.nextFloat())) }
        }
        Terrain.THUNDER_PLAINS -> {
            repeat(24) {
                val c = p(rng.nextFloat(), rng.nextFloat())
                drawLine(Color(0xFF5B6B33).copy(alpha = 0.6f), c, Offset(c.x, c.y - 6.dp.toPx()), 1.5.dp.toPx())
            }
            repeat(5) { // static zig-zags crackling over the ground
                val path = Path()
                var x = rng.nextFloat(); val y = rng.nextFloat()
                path.moveTo(x * w, y * h)
                repeat(6) { k -> x += 0.03f; path.lineTo(x * w, (y + if (k % 2 == 0) 0.025f else -0.025f) * h) }
                drawPath(path, accent.copy(alpha = 0.55f), style = Stroke(2.dp.toPx(), join = StrokeJoin.Miter))
            }
        }
        Terrain.CANYON -> {
            repeat(7) { k -> // rock strata
                val y = (k + 0.5f) / 7f
                val path = Path().apply {
                    moveTo(0f, y * h)
                    var x = 0f
                    while (x <= w) { lineTo(x, y * h + sin(x / w * 3 * PI + k).toFloat() * 6.dp.toPx()); x += w / 30 }
                }
                drawPath(path, accent.copy(alpha = 0.16f), style = Stroke(3.dp.toPx()))
            }
            repeat(9) {
                val c = p(rng.nextFloat(), rng.nextFloat())
                val r = (5 + rng.nextFloat() * 8).dp.toPx()
                drawCircle(Color(0xFF2E170D).copy(alpha = 0.5f), r, Offset(c.x + 2.dp.toPx(), c.y + 2.dp.toPx()))
                drawCircle(Color(0xFF8A5236), r, c)
            }
        }
        Terrain.WINDY_CLIFFS -> {
            // the cliff edge: open sky and clouds below the drop along the top
            drawRect(Brush.verticalGradient(listOf(Color(0xFF7FB6DE), Color(0xFFB9DDF2)), 0f, h * 0.14f), size = Size(w, h * 0.14f))
            repeat(7) {
                val c = p(rng.nextFloat(), 0.06f + rng.nextFloat() * 0.06f)
                val r = (12 + rng.nextFloat() * 12).dp.toPx()
                drawCircle(Color.White.copy(alpha = 0.85f), r, c)
                drawCircle(Color.White.copy(alpha = 0.85f), r * 0.7f, Offset(c.x + r, c.y + r * 0.2f))
            }
            val edge = Path().apply {
                moveTo(0f, h * 0.14f)
                var x = 0f
                while (x <= w) { lineTo(x, h * 0.14f + (rng.nextFloat() - 0.5f) * 6.dp.toPx()); x += w / 24 }
            }
            drawPath(edge, Color(0xFF1E2329), style = Stroke(4.dp.toPx()))
            repeat(14) { // gusts streaking across the plateau
                val c = p(rng.nextFloat(), 0.2f + rng.nextFloat() * 0.78f)
                drawLine(accent.copy(alpha = 0.22f), c, Offset(c.x + (30 + rng.nextFloat() * 50).dp.toPx(), c.y), 2.dp.toPx(), StrokeCap.Round)
            }
            repeat(10) { drawCircle(Color(0xFF6B7681), (3 + rng.nextFloat() * 4).dp.toPx(), p(rng.nextFloat(), 0.2f + rng.nextFloat() * 0.8f)) }
        }
        Terrain.ARENA -> Unit
    }

    drawCircle(accent.copy(alpha = 0.22f), w * 0.16f, center, style = Stroke(2.dp.toPx()))
    drawRect(accent.copy(alpha = 0.2f), style = Stroke(3.dp.toPx()))
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)), center, radius = size.maxDimension * 0.75f))
}
