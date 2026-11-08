package com.petpal.app.ui.battle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.petpal.app.domain.battle.Terrain
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Ground colors (top, bottom) and accent for each element terrain. Kept fairly dark so pets stay readable. */
private fun Terrain.palette(): Triple<Color, Color, Color> = when (this) {
    Terrain.ARENA -> Triple(Color(0xFF24201A), Color(0xFF181612), Color(0xFFFF6D1F))
    Terrain.VOLCANO -> Triple(Color(0xFF3A211A), Color(0xFF1A0F0B), Color(0xFFFF5A1F))
    Terrain.LAKESHORE -> Triple(Color(0xFF7A6644), Color(0xFF4A3D27), Color(0xFF4FA3E0))
    Terrain.FOREST -> Triple(Color(0xFF2C4F2C), Color(0xFF16291A), Color(0xFF8FD67C))
    Terrain.TUNDRA -> Triple(Color(0xFF8AA4B6), Color(0xFF566E80), Color(0xFFDDF3FF))
    Terrain.THUNDER_PLAINS -> Triple(Color(0xFF39432A), Color(0xFF1C2114), Color(0xFFFFD54F))
    Terrain.CANYON -> Triple(Color(0xFF8A4D2E), Color(0xFF4A2716), Color(0xFFF0B278))
    Terrain.DOJO -> Triple(Color(0xFF6B4A2E), Color(0xFF3A2716), Color(0xFFE8604A))
    Terrain.WINDY_CLIFFS -> Triple(Color(0xFF5C7058), Color(0xFF34443A), Color(0xFFBFE3FF))
}

/** Accent color of a terrain, for its HUD badge. */
fun Terrain.accent(): Color = palette().third

/** Floor for any non-arena terrain: ground texture, scenery, a soft spotlight, centre ring and vignette. */
internal fun DrawScope.drawTerrainFloor(t: Terrain) {
    val (top, bottom, accent) = t.palette()
    val w = size.width
    val h = size.height
    val rng = Random(t.ordinal * 31 + 7) // same scenery every time this map is played
    fun p(x: Float, y: Float) = Offset(x * w, y * h)
    fun d(v: Number) = v.toFloat().dp.toPx()
    fun rnd(a: Float, b: Float) = a + rng.nextFloat() * (b - a)

    drawRect(Brush.verticalGradient(listOf(top, bottom)))
    // Ground texture: soft light and dark blotches so the floor doesn't look flat.
    repeat(36) {
        val c = p(rng.nextFloat(), rng.nextFloat())
        val r = d(rnd(18f, 46f))
        val tone = if (rng.nextBoolean()) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.07f)
        drawCircle(Brush.radialGradient(listOf(tone, Color.Transparent), c, r), r, c)
    }

    when (t) {
        Terrain.VOLCANO -> volcano(rng, accent, ::p, ::d, ::rnd)
        Terrain.LAKESHORE -> lakeshore(rng, ::p, ::d, ::rnd)
        Terrain.FOREST -> forest(rng, accent, ::p, ::d, ::rnd)
        Terrain.TUNDRA -> tundra(rng, ::p, ::d, ::rnd)
        Terrain.THUNDER_PLAINS -> thunder(rng, accent, ::p, ::d, ::rnd)
        Terrain.CANYON -> canyon(rng, accent, ::p, ::d, ::rnd)
        Terrain.WINDY_CLIFFS -> cliffs(rng, accent, ::p, ::d, ::rnd)
        Terrain.DOJO -> dojo(accent, ::p, ::d)
        Terrain.ARENA -> Unit
    }

    // Soft spotlight on the fighting area, a double centre ring, a frame and a vignette.
    drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.10f), Color.Transparent), center, w * 0.42f), w * 0.42f, center)
    drawCircle(accent.copy(alpha = 0.06f), w * 0.16f, center)
    drawCircle(accent.copy(alpha = 0.30f), w * 0.16f, center, style = Stroke(d(2)))
    drawCircle(accent.copy(alpha = 0.14f), w * 0.16f + d(6), center,
        style = Stroke(d(1.5), pathEffect = PathEffect.dashPathEffect(floatArrayOf(d(6), d(6)))))
    drawRect(accent.copy(alpha = 0.22f), style = Stroke(d(3)))
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f)), center, radius = size.maxDimension * 0.75f))
}

private typealias P = (Float, Float) -> Offset
private typealias D = (Number) -> Float
private typealias R = (Float, Float) -> Float

private fun DrawScope.rock(c: Offset, r: Float, base: Color, light: Color) {
    drawOval(Color.Black.copy(alpha = 0.3f), Offset(c.x - r, c.y - r * 0.2f), Size(r * 2.2f, r * 1.2f))
    drawCircle(base, r, c)
    drawCircle(light.copy(alpha = 0.6f), r * 0.45f, Offset(c.x - r * 0.3f, c.y - r * 0.35f))
}

private fun DrawScope.tuft(c: Offset, color: Color, d: D, height: Float = 8f) {
    for (k in -2..2) drawLine(color, c, Offset(c.x + k * d(2.5), c.y - d(height - kotlin.math.abs(k) * 1.5f)), d(1.5), StrokeCap.Round)
}

private fun DrawScope.glow(c: Offset, r: Float, color: Color) =
    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), c, r), r, c)

private fun DrawScope.dojo(accent: Color, p: P, d: D) {
    val w = size.width; val h = size.height
    // wooden floor boards
    var y = 0f
    while (y < h) {
        drawLine(Color.Black.copy(alpha = 0.22f), Offset(0f, y), Offset(w, y), d(1.5))
        y += d(22)
    }
    // tatami mats in the corners
    for ((x0, y0) in listOf(0.04f to 0.04f, 0.74f to 0.04f, 0.04f to 0.86f, 0.74f to 0.86f)) {
        val tl = p(x0, y0); val sz = Size(w * 0.22f, h * 0.1f)
        drawRect(Color(0xFFB9A86A), tl, sz)
        drawRect(Color(0xFF2E3B2A), tl, sz, style = Stroke(d(3)))
    }
    // red training circle on the floor
    drawCircle(accent.copy(alpha = 0.18f), w * 0.36f, center, style = Stroke(d(8)))
    // hanging banners along the top
    for (x in listOf(0.2f, 0.5f, 0.8f)) {
        val c = p(x, 0f)
        drawRect(accent.copy(alpha = 0.8f), Offset(c.x - d(9), 0f), Size(d(18), d(40)))
        drawCircle(Color(0xFFFFE8C2).copy(alpha = 0.8f), d(5), Offset(c.x, d(22)))
    }
}

private fun DrawScope.volcano(rng: Random, accent: Color, p: P, d: D, rnd: R) {
    val w = size.width; val h = size.height
    // lava pool in the bottom-left corner
    val pool = p(0.02f, 0.98f)
    glow(pool, w * 0.32f, accent.copy(alpha = 0.35f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE08A), Color(0xFFFF7A1F), Color(0xFFB8300F)), pool, w * 0.16f), w * 0.16f, pool)
    drawCircle(Color(0xFF2A130C), w * 0.16f, pool, style = Stroke(d(5)))
    repeat(6) { // lava cracks with a hot glow
        val path = Path()
        var x = rng.nextFloat(); var y = rng.nextFloat()
        path.moveTo(x * w, y * h)
        repeat(6) {
            x += (rng.nextFloat() - 0.5f) * 0.16f; y += (rng.nextFloat() - 0.5f) * 0.12f
            path.lineTo(x * w, y * h)
        }
        drawPath(path, accent.copy(alpha = 0.18f), style = Stroke(d(14), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, accent.copy(alpha = 0.5f), style = Stroke(d(5), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, Color(0xFFFFD27A), style = Stroke(d(1.5), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    repeat(8) { rock(p(rng.nextFloat(), rng.nextFloat()), d(rnd(5f, 11f)), Color(0xFF2B1D19), Color(0xFF5A3A30)) }
    repeat(30) { // embers
        val c = p(rng.nextFloat(), rng.nextFloat())
        glow(c, d(6), accent.copy(alpha = 0.35f))
        drawCircle(Color(0xFFFFC27A), d(rnd(0.8f, 1.8f)), c)
    }
    // ash haze drifting over the top
    drawRect(Brush.verticalGradient(listOf(Color(0x33000000), Color.Transparent), 0f, h * 0.25f))
}

private fun DrawScope.lakeshore(rng: Random, p: P, d: D, rnd: R) {
    val w = size.width; val h = size.height
    fun shore(off: Float) = Path().apply {
        moveTo(0f, 0f); lineTo(w, 0f)
        var x = w
        while (x >= 0f) { lineTo(x, h * (0.22f + off + 0.03f * sin(x / w * 4 * PI).toFloat())); x -= w / 48 }
        close()
    }
    drawPath(shore(0.04f), Color(0xFF9C8A62).copy(alpha = 0.6f)) // wet sand band
    drawPath(shore(0.015f), Color.White.copy(alpha = 0.35f)) // foam line
    drawPath(shore(0f), Brush.verticalGradient(listOf(Color(0xFF123A5C), Color(0xFF2F7BAE), Color(0xFF4FA3D6)), 0f, h * 0.26f))
    repeat(10) { // shimmer
        val c = p(rnd(0.05f, 0.95f), rnd(0.03f, 0.17f))
        val len = d(rnd(14f, 30f))
        drawLine(Color.White.copy(alpha = rnd(0.2f, 0.45f)), Offset(c.x - len / 2, c.y), Offset(c.x + len / 2, c.y), d(1.5), StrokeCap.Round)
    }
    repeat(3) { // lily pads
        val c = p(rnd(0.1f, 0.9f), rnd(0.05f, 0.15f))
        val r = d(rnd(6f, 9f))
        drawArc(Color(0xFF4E9A4A), 30f, 300f, true, Offset(c.x - r, c.y - r * 0.6f), Size(r * 2, r * 1.2f))
    }
    repeat(5) { // reeds along the shore
        val base = p(rnd(0f, 1f), rnd(0.26f, 0.3f))
        repeat(4) { k ->
            val tip = Offset(base.x + (k - 1.5f) * d(3), base.y - d(rnd(14f, 22f)))
            drawLine(Color(0xFF5E7A3A), base, tip, d(1.5), StrokeCap.Round)
            if (k % 2 == 0) drawOval(Color(0xFF6B4A2B), Offset(tip.x - d(1.5), tip.y), Size(d(3), d(7)))
        }
    }
    repeat(14) { rock(p(rng.nextFloat(), rnd(0.32f, 1f)), d(rnd(2f, 5f)), Color(0xFF8C7B5A), Color(0xFFD8C9A3)) }
    repeat(4) { // shells
        val c = p(rng.nextFloat(), rnd(0.35f, 1f))
        drawArc(Color(0xFFF2D9C4), 180f, 180f, true, Offset(c.x - d(4), c.y - d(3)), Size(d(8), d(6)))
    }
}

private fun DrawScope.forest(rng: Random, accent: Color, p: P, d: D, rnd: R) {
    repeat(9) { // dappled sunlight through the leaves
        val c = p(rng.nextFloat(), rng.nextFloat())
        glow(c, d(rnd(20f, 40f)), Color(0xFFFFF3B0).copy(alpha = 0.09f))
    }
    repeat(5) { // moss patches
        val c = p(rng.nextFloat(), rng.nextFloat())
        val s = Size(d(rnd(50f, 90f)), d(rnd(20f, 34f)))
        drawOval(Color(0xFF3E6E35).copy(alpha = 0.45f), Offset(c.x - s.width / 2, c.y - s.height / 2), s)
    }
    repeat(34) { tuft(p(rng.nextFloat(), rng.nextFloat()), accent.copy(alpha = 0.35f), d) }
    val petals = listOf(Color(0xFFFFF4F4), Color(0xFFFFD54F), Color(0xFFF48FB1), Color(0xFFB39DDB))
    repeat(14) { // little flowers
        val c = p(rng.nextFloat(), rng.nextFloat())
        val col = petals[rng.nextInt(petals.size)]
        for (a in 0 until 5) {
            val ang = a * 2 * PI.toFloat() / 5
            drawCircle(col, d(1.8), Offset(c.x + cos(ang) * d(2.4), c.y + sin(ang) * d(2.4)))
        }
        drawCircle(Color(0xFFFFB300), d(1.3), c)
    }
    repeat(3) { // mushrooms
        val c = p(rnd(0.15f, 0.85f), rnd(0.15f, 0.85f))
        drawRect(Color(0xFFEDE3CF), Offset(c.x - d(1.5), c.y - d(3)), Size(d(3), d(5)))
        drawArc(Color(0xFFD9483B), 180f, 180f, true, Offset(c.x - d(5), c.y - d(7)), Size(d(10), d(8)))
        drawCircle(Color.White, d(1), Offset(c.x - d(2), c.y - d(5)))
        drawCircle(Color.White, d(0.8), Offset(c.x + d(2), c.y - d(4.5)))
    }
    // tree canopies around the edges (trunks hidden off-screen), with soft shadows
    listOf(0f to 0.1f, 0.02f to 0.45f, 0f to 0.85f, 1f to 0.2f, 0.98f to 0.6f, 1f to 0.95f, 0.5f to -0.02f, 0.3f to 1.02f, 0.75f to 1.03f)
        .forEach { (x, y) ->
            val c = p(x, y)
            val r = d(rnd(34f, 48f))
            glow(Offset(c.x + r * 0.15f, c.y + r * 0.3f), r * 1.5f, Color.Black.copy(alpha = 0.35f))
            drawCircle(Color(0xFF12281A), r * 1.05f, c)
            drawCircle(Color(0xFF2E5A2F), r, c)
            repeat(4) {
                val a = rng.nextFloat() * 2 * PI.toFloat()
                drawCircle(Color(0xFF3B7339), r * 0.45f, Offset(c.x + cos(a) * r * 0.45f, c.y + sin(a) * r * 0.45f))
            }
            drawCircle(Color(0xFF5E9E4E).copy(alpha = 0.7f), r * 0.35f, Offset(c.x - r * 0.3f, c.y - r * 0.3f))
        }
}

private fun DrawScope.tundra(rng: Random, p: P, d: D, rnd: R) {
    repeat(5) { // snowdrifts
        val c = p(rng.nextFloat(), rng.nextFloat())
        val r = d(rnd(30f, 60f))
        glow(c, r, Color.White.copy(alpha = 0.22f))
    }
    repeat(6) { // slick ice patches with a shine
        val c = p(rng.nextFloat(), rng.nextFloat())
        val s = Size(d(rnd(60f, 130f)), d(rnd(24f, 48f)))
        val tl = Offset(c.x - s.width / 2, c.y - s.height / 2)
        drawOval(Brush.linearGradient(listOf(Color(0xFFD6F1FF), Color(0xFF8FC9E8)), tl, Offset(tl.x + s.width, tl.y + s.height)),
            tl, s, alpha = 0.55f)
        drawOval(Color.White.copy(alpha = 0.5f), tl, s, style = Stroke(d(1)))
        drawLine(Color.White.copy(alpha = 0.7f), Offset(c.x - s.width * 0.25f, c.y - s.height * 0.12f),
            Offset(c.x + s.width * 0.05f, c.y - s.height * 0.28f), d(2), StrokeCap.Round)
    }
    // snowy pines in the corners
    listOf(0.04f to 0.12f, 0.1f to 0.05f, 0.95f to 0.1f, 0.03f to 0.92f, 0.96f to 0.88f, 0.9f to 0.97f).forEach { (x, y) ->
        val c = p(x, y)
        val s = d(rnd(18f, 26f))
        drawOval(Color.Black.copy(alpha = 0.25f), Offset(c.x - s * 0.6f, c.y + s * 0.7f), Size(s * 1.6f, s * 0.5f))
        for (k in 0..2) {
            val half = s * (0.9f - k * 0.22f)
            val yy = c.y + s * 0.8f - k * s * 0.55f
            val tri = Path().apply { moveTo(c.x, yy - s * 0.8f); lineTo(c.x - half, yy); lineTo(c.x + half, yy); close() }
            drawPath(tri, Color(0xFF2F5A4E))
            drawPath(Path().apply { moveTo(c.x, yy - s * 0.8f); lineTo(c.x - half * 0.5f, yy - s * 0.4f); lineTo(c.x + half * 0.3f, yy - s * 0.45f); close() },
                Color.White.copy(alpha = 0.85f))
        }
    }
    repeat(45) { drawCircle(Color.White.copy(alpha = rnd(0.4f, 0.85f)), d(rnd(0.8f, 2.2f)), p(rng.nextFloat(), rng.nextFloat())) }
}

private fun DrawScope.thunder(rng: Random, accent: Color, p: P, d: D, rnd: R) {
    val w = size.width; val h = size.height
    repeat(4) { // storm cloud shadows
        val c = p(rng.nextFloat(), rng.nextFloat())
        glow(c, d(rnd(50f, 90f)), Color.Black.copy(alpha = 0.25f))
    }
    repeat(38) { tuft(p(rng.nextFloat(), rng.nextFloat()), Color(0xFF7A8C45).copy(alpha = 0.55f), d, 7f) }
    repeat(3) { // scorch marks from old strikes
        val c = p(rnd(0.15f, 0.85f), rnd(0.15f, 0.85f))
        glow(c, d(26), Color.Black.copy(alpha = 0.45f))
        repeat(6) { k ->
            val a = k * PI.toFloat() / 3 + rng.nextFloat()
            drawLine(Color(0xFF14160E), c, Offset(c.x + cos(a) * d(18), c.y + sin(a) * d(18)), d(1.5), StrokeCap.Round)
        }
        glow(c, d(10), accent.copy(alpha = 0.3f))
    }
    repeat(5) { // charged stones
        val c = p(rng.nextFloat(), rng.nextFloat())
        glow(c, d(16), accent.copy(alpha = 0.18f))
        rock(c, d(rnd(5f, 9f)), Color(0xFF3B3F38), Color(0xFF8A8F78))
    }
    repeat(6) { // static zig-zags crackling over the ground
        val path = Path()
        var x = rng.nextFloat(); val y = rng.nextFloat()
        path.moveTo(x * w, y * h)
        repeat(6) { k -> x += 0.03f; path.lineTo(x * w, (y + if (k % 2 == 0) 0.025f else -0.025f) * h) }
        drawPath(path, accent.copy(alpha = 0.2f), style = Stroke(d(6), join = StrokeJoin.Round))
        drawPath(path, accent.copy(alpha = 0.8f), style = Stroke(d(1.5), join = StrokeJoin.Miter))
    }
}

private fun DrawScope.canyon(rng: Random, accent: Color, p: P, d: D, rnd: R) {
    val w = size.width; val h = size.height
    repeat(7) { k -> // rock strata
        val y = (k + 0.5f) / 7f
        val path = Path().apply {
            moveTo(0f, y * h)
            var x = 0f
            while (x <= w) { lineTo(x, y * h + sin(x / w * 3 * PI + k).toFloat() * d(6)); x += w / 30 }
        }
        drawPath(path, Color.Black.copy(alpha = 0.12f), style = Stroke(d(5)))
        drawPath(path, accent.copy(alpha = 0.18f), style = Stroke(d(2)))
    }
    repeat(8) { // sand ripples
        val c = p(rng.nextFloat(), rng.nextFloat())
        repeat(3) { k ->
            drawArc(Color(0xFFE8B888).copy(alpha = 0.22f), 200f, 140f, false,
                Offset(c.x - d(16), c.y + k * d(5)), Size(d(32), d(10)), style = Stroke(d(1.2)))
        }
    }
    // mesa walls along the left and right edges
    listOf(0f, 1f).forEach { side ->
        val dir = if (side == 0f) 1f else -1f
        val edge = Path().apply {
            moveTo(side * w, 0f)
            var y = 0f
            while (y <= h) { lineTo(side * w + dir * d(rnd(18f, 30f)), y); y += h / 10 }
            lineTo(side * w, h); close()
        }
        drawPath(edge, Color(0xFF5C2E18))
        drawPath(edge, Color(0xFFC77A4A).copy(alpha = 0.35f), style = Stroke(d(2)))
    }
    repeat(3) { // cacti
        val c = p(rnd(0.15f, 0.85f), rnd(0.15f, 0.85f))
        val g = Color(0xFF5F8A4A)
        drawOval(Color.Black.copy(alpha = 0.25f), Offset(c.x - d(6), c.y + d(9)), Size(d(14), d(4)))
        drawRoundRect(g, Offset(c.x - d(3), c.y - d(10)), Size(d(6), d(21)), androidx.compose.ui.geometry.CornerRadius(d(3)))
        drawRoundRect(g, Offset(c.x - d(9), c.y - d(4)), Size(d(4), d(9)), androidx.compose.ui.geometry.CornerRadius(d(2)))
        drawRoundRect(g, Offset(c.x + d(5), c.y - d(7)), Size(d(4), d(9)), androidx.compose.ui.geometry.CornerRadius(d(2)))
    }
    repeat(10) { rock(p(rng.nextFloat(), rng.nextFloat()), d(rnd(5f, 12f)), Color(0xFF8A5236), Color(0xFFD99A6C)) }
}

private fun DrawScope.cliffs(rng: Random, accent: Color, p: P, d: D, rnd: R) {
    val w = size.width; val h = size.height
    val sky = h * 0.16f
    // open sky beyond the drop along the top
    drawRect(Brush.verticalGradient(listOf(Color(0xFF6FA8D8), Color(0xFFCFE8F6)), 0f, sky), size = Size(w, sky))
    glow(p(0.82f, 0.02f), d(40), Color(0xFFFFF6C8).copy(alpha = 0.7f)) // sun
    repeat(7) {
        val c = p(rng.nextFloat(), rnd(0.06f, 0.13f))
        val r = d(rnd(12f, 22f))
        drawCircle(Color.White.copy(alpha = 0.9f), r, c)
        drawCircle(Color.White.copy(alpha = 0.9f), r * 0.7f, Offset(c.x + r, c.y + r * 0.2f))
        drawCircle(Color.White.copy(alpha = 0.9f), r * 0.6f, Offset(c.x - r * 0.9f, c.y + r * 0.3f))
    }
    val edge = Path().apply {
        moveTo(0f, sky)
        var x = 0f
        while (x <= w) { lineTo(x, sky + (rng.nextFloat() - 0.5f) * d(6)); x += w / 24 }
        lineTo(w, sky + d(10)); lineTo(0f, sky + d(10)); close()
    }
    drawPath(edge, Color(0xFF6B6157)) // rocky lip
    drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent), sky + d(10), sky + d(30)),
        Offset(0f, sky + d(10)), Size(w, d(20)))
    repeat(8) { // meadow patches
        val c = p(rng.nextFloat(), rnd(0.25f, 1f))
        val s = Size(d(rnd(50f, 90f)), d(rnd(18f, 30f)))
        drawOval(Color(0xFF7A9A62).copy(alpha = 0.35f), Offset(c.x - s.width / 2, c.y - s.height / 2), s)
    }
    repeat(26) { tuft(p(rng.nextFloat(), rnd(0.22f, 1f)), Color(0xFF9CC07E).copy(alpha = 0.5f), d, 7f) }
    repeat(12) { // curling gusts streaking across the plateau
        val c = p(rng.nextFloat(), rnd(0.24f, 0.97f))
        val len = d(rnd(40f, 80f))
        val gust = Path().apply {
            moveTo(c.x, c.y)
            quadraticBezierTo(c.x + len * 0.5f, c.y - d(6), c.x + len, c.y)
            quadraticBezierTo(c.x + len + d(8), c.y + d(4), c.x + len - d(2), c.y + d(7))
        }
        drawPath(gust, accent.copy(alpha = 0.35f), style = Stroke(d(1.8), cap = StrokeCap.Round))
    }
    repeat(8) { rock(p(rng.nextFloat(), rnd(0.25f, 1f)), d(rnd(3f, 7f)), Color(0xFF6B7681), Color(0xFFB4BEC8)) }
}
