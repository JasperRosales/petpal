package com.petpal.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.petpal.app.domain.model.*
import com.petpal.app.ui.theme.*
import kotlin.math.roundToInt

/**
 * Pixel-art pet built procedurally on a 20x20 grid: a body ellipse that grows with the
 * stage, plus species features (ears, wings, shell...). No image assets needed.
 */
@Composable
fun PetSprite(
    stage: Stage,
    mood: Mood,
    species: Species,
    modifier: Modifier = Modifier,
    size: Dp = when (stage) {
        Stage.EGG -> 120.dp
        Stage.BABY -> 136.dp
        Stage.KID -> 168.dp
        Stage.ADULT -> 200.dp
    },
) {
    val transition = rememberInfiniteTransition(label = "pet")
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (mood == Mood.SLEEPY) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(if (mood == Mood.HAPPY) 450 else 900), RepeatMode.Reverse),
        label = "bounce",
    )
    val grid = remember(stage, mood, species) { PetArt.draw(species, stage, mood) }
    val palette = remember(species) { species.palette() }

    Canvas(
        modifier.size(size).graphicsLayer {
            if (stage == Stage.EGG) rotationZ = (bounce - 0.5f) * 8f
            else translationY = -bounce * 4.dp.toPx()
        },
    ) {
        val px = this.size.width / PetArt.N
        grid.forEachIndexed { y, row ->
            row.forEachIndexed { x, c ->
                val color = palette[c] ?: return@forEachIndexed
                // +0.5px overlap hides hairline seams between cells
                drawRect(color, Offset(x * px, y * px), Size(px + 0.5f, px + 0.5f))
            }
        }
    }
}

private fun Species.palette(): Map<Char, Color> {
    val (body, dark, belly) = colors()
    return mapOf(
        'o' to SpriteInk, 'e' to SpriteInk, 'm' to SpriteInk,
        'b' to body, 'd' to dark, 'l' to belly,
        'c' to SpriteCheek, 'p' to SpritePink, 'y' to SpriteYellow,
        'h' to SpriteCream, 'g' to SpriteLeaf, 'r' to SpriteRed,
    )
}

fun Species.colors(): Triple<Color, Color, Color> = when (this) {
    Species.SPROUT -> Triple(Color(0xFFB9E07A), Color(0xFF6FAE4E), SpriteCream)
    Species.KITTY -> Triple(Color(0xFFF0A868), Color(0xFFC97B3E), SpriteCream)
    Species.DRAKE -> Triple(Color(0xFF7CC98A), Color(0xFF3E8A55), Color(0xFFF3E3A6))
    Species.BUNNY -> Triple(Color(0xFFE9E1F2), Color(0xFFBDAED6), Color(0xFFFFFBF5))
    Species.PUP -> Triple(Color(0xFFE3C08D), Color(0xFF9C6B3F), SpriteCream)
    Species.OWL -> Triple(Color(0xFFA9825A), Color(0xFF6E5034), Color(0xFFF1E2C2))
    Species.TURTLE -> Triple(Color(0xFF8FCFB0), Color(0xFF7B5A3A), Color(0xFFE9F2C8))
    Species.FOX -> Triple(Color(0xFFE9824A), Color(0xFFB5552A), Color(0xFFFFF4E6))
    Species.FROG -> Triple(Color(0xFF8BD06A), Color(0xFF4E9A3E), Color(0xFFF0F2B0))
    Species.BEAR -> Triple(Color(0xFFB07A4F), Color(0xFF6E4A2E), Color(0xFFE8CFA8))
    Species.PENGUIN -> Triple(Color(0xFF3E4C6E), Color(0xFF232B40), Color(0xFFF7F7F2))
    Species.HEDGEHOG -> Triple(Color(0xFFD9B38C), Color(0xFF6B4A33), Color(0xFFF5E6D0))
    Species.BAT -> Triple(Color(0xFF8A6FA8), Color(0xFF4E3A66), Color(0xFFD8C8E8))
    Species.DEER -> Triple(Color(0xFFC98E5A), Color(0xFF8A5A32), Color(0xFFF6E7CF))
    Species.OTTER -> Triple(Color(0xFF9C6B4A), Color(0xFF6A4630), Color(0xFFEBD3B5))
    Species.PANDA -> Triple(Color(0xFFF7F5EF), Color(0xFF2B2B2B), Color(0xFFFFFFFF))
    Species.LIZARD -> Triple(Color(0xFF7FB85A), Color(0xFF4B7A33), Color(0xFFF2D08A))
    Species.RACCOON -> Triple(Color(0xFFA3A3A8), Color(0xFF55555C), Color(0xFFE6E6EA))
}

internal object PetArt {
    const val N = 20

    private class Grid {
        val c = Array(N) { CharArray(N) { '.' } }
        operator fun get(x: Int, y: Int) = if (x in 0 until N && y in 0 until N) c[y][x] else '.'
        fun set(x: Int, y: Int, ch: Char) { if (x in 0 until N && y in 0 until N) c[y][x] = ch }
        fun mirror(x: Int, y: Int, ch: Char) { set(x, y, ch); set(N - 1 - x, y, ch) }

        fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, ch: Char, onlyOver: Char? = null) {
            for (y in 0 until N) for (x in 0 until N) {
                val dx = (x + 0.5f - cx) / rx
                val dy = (y + 0.5f - cy) / ry
                if (dx * dx + dy * dy <= 1f && (onlyOver == null || this[x, y] == onlyOver)) set(x, y, ch)
            }
        }

        fun rect(x0: Int, y0: Int, x1: Int, y1: Int, ch: Char) {
            for (y in y0..y1) for (x in x0..x1) set(x, y, ch)
        }

        /** Pointed ear/horn from [baseY] upward, centered on column [cx] (and mirrored). */
        fun spike(cx: Int, baseY: Int, h: Int, w: Int, fill: Char, inner: Char? = null) {
            for (i in 0 until h) {
                val half = ((w / 2f) * (1f - i.toFloat() / h)).roundToInt()
                for (dx in -half..half) {
                    val ch = if (inner != null && kotlin.math.abs(dx) < half && i < h - 1) inner else fill
                    mirror(cx + dx, baseY - i, ch)
                }
            }
        }

        /** Turns every filled cell that touches empty space into outline. */
        fun outline() {
            val edge = mutableListOf<Pair<Int, Int>>()
            for (y in 0 until N) for (x in 0 until N) {
                if (this[x, y] == '.') continue
                if (this[x - 1, y] == '.' || this[x + 1, y] == '.' || this[x, y - 1] == '.' || this[x, y + 1] == '.' ||
                    x == 0 || y == 0 || x == N - 1 || y == N - 1
                ) edge += x to y
            }
            edge.forEach { (x, y) -> set(x, y, 'o') }
        }

        fun rows() = c.map { it.concatToString() }
    }

    private data class Body(val cy: Float, val rx: Float, val ry: Float) {
        val cx = N / 2f
        val top get() = (cy - ry).roundToInt()
        val bottom get() = (cy + ry).roundToInt()
        val left get() = (cx - rx).roundToInt()
    }

    private fun bodyFor(stage: Stage) = when (stage) {
        Stage.BABY -> Body(cy = 12.5f, rx = 5f, ry = 4.6f)
        Stage.KID -> Body(cy = 11.5f, rx = 6f, ry = 5.4f)
        else -> Body(cy = 11f, rx = 7f, ry = 6.2f)
    }

    fun draw(species: Species, stage: Stage, mood: Mood): List<String> {
        if (stage == Stage.EGG) return egg(species)
        val g = Grid()
        val b0 = bodyFor(stage)
        // Turtles: smaller body tucked under a shell dome
        val b = if (species == Species.TURTLE) b0.copy(cy = b0.cy + 1f, rx = b0.rx - 0.5f, ry = b0.ry - 1f) else b0
        val big = stage == Stage.ADULT
        val grown = stage != Stage.BABY

        // 1. Behind the body
        when (species) {
            Species.DRAKE -> {
                val wy = b.cy.toInt() - 2
                val span = if (big) 3 else if (grown) 2 else 1
                for (i in 0..span) for (y in wy - i..wy + 2) for (x in b.left - 1 - i..b.left) g.mirror(x, y, 'd')
            }
            Species.TURTLE -> g.ellipse(b0.cx, b0.cy - 1.2f, b0.rx + 0.8f, b0.ry, 'd')
            Species.BAT -> { // wide webbed wings
                val wy = b.cy.toInt() - 3
                val span = if (big) 4 else if (grown) 3 else 2
                for (i in 0..span) for (y in wy - i / 2..wy + 3 - i % 2) for (x in b.left - 1 - i..b.left) g.mirror(x, y, 'd')
            }
            Species.LIZARD -> { // long tail curling out to the side
                val ty = b.cy.toInt() + 1
                g.rect(b.left + 2 * b.rx.toInt() - 1, ty, b.left + 2 * b.rx.toInt() + (if (grown) 2 else 1), ty + 1, 'b')
                g.rect(b.left + 2 * b.rx.toInt() + (if (grown) 2 else 1), ty - 2, b.left + 2 * b.rx.toInt() + (if (grown) 3 else 2), ty + 1, 'b')
            }
            Species.RACCOON -> { // ringed bushy tail
                val r = if (big) 2.6f else if (grown) 2.2f else 1.8f
                g.ellipse(b.cx + b.rx + 0.6f, b.cy, r, r * 1.4f, 'b')
                g.ellipse(b.cx + b.rx + 0.6f, b.cy - r * 0.3f, r, 0.6f, 'd', onlyOver = 'b')
                g.ellipse(b.cx + b.rx + 0.6f, b.cy + r * 0.6f, r, 0.6f, 'd', onlyOver = 'b')
            }
            Species.HEDGEHOG -> { // spiky back peeking out all around the top
                g.ellipse(b.cx, b.cy - 0.6f, b.rx + 1.2f, b.ry + 1f, 'd')
                val n = if (big) 7 else 5
                for (k in 0 until n) {
                    val a = kotlin.math.PI * (0.1 + 0.8 * k / (n - 1))
                    val x = (b.cx - kotlin.math.cos(a) * (b.rx + 1.6f)).toInt()
                    val y = (b.cy - 0.6f - kotlin.math.sin(a) * (b.ry + 1.6f)).toInt()
                    g.set(x, y, 'd')
                }
            }
            Species.KITTY -> g.rect(b.left + 2 * b.rx.toInt() - 1, b.cy.toInt() - (if (grown) 4 else 2), b.left + 2 * b.rx.toInt(), b.cy.toInt() + 2, 'b')
            Species.FOX -> {
                val r = if (big) 3.2f else if (grown) 2.6f else 2f
                g.ellipse(b.cx + b.rx, b.cy + 1f, r, r * 1.3f, 'b')
                g.ellipse(b.cx + b.rx + r * 0.4f, b.cy - r * 0.6f, r * 0.6f, r * 0.6f, 'l')
            }
            else -> Unit
        }

        // 2. Ears and things on top (drawn before the body so the body covers their base)
        when (species) {
            Species.KITTY -> g.spike(b.left + 2, b.top + 1, if (big) 5 else if (grown) 4 else 3, 4, 'b', 'p')
            Species.FOX -> g.spike(b.left + 2, b.top + 1, if (big) 6 else if (grown) 5 else 4, 4, 'b', 'e')
            Species.BUNNY -> {
                val h = when (stage) { Stage.BABY -> 3; Stage.KID -> 5; else -> 6 }
                val x = b.cx.toInt() - 3
                g.rect(x - 1, b.top - h + 1, x + 1, b.top + 1, 'b')
                g.rect(N - 1 - x - 1, b.top - h + 1, N - 1 - x + 1, b.top + 1, 'b')
                g.rect(x, b.top - h + 2, x, b.top, 'p'); g.set(N - 1 - x, b.top, 'p')
                for (y in b.top - h + 2..b.top) g.set(N - 1 - x, y, 'p')
            }
            Species.BEAR -> {
                val r = if (big) 2.4f else if (grown) 2.1f else 1.8f
                g.ellipse(b.cx - b.rx * 0.62f, b.top + 0.6f, r, r, 'b'); g.ellipse(b.cx + b.rx * 0.62f, b.top + 0.6f, r, r, 'b')
            }
            Species.OWL -> g.spike(b.left + 2, b.top + 1, if (big) 4 else 3, 2, 'd')
            Species.DRAKE -> g.spike(b.left + 3, b.top + 1, if (big) 4 else 3, 2, 'h')
            Species.FROG -> {
                val r = if (big) 2.2f else 1.8f
                g.ellipse(b.cx - b.rx * 0.5f, b.top + 0.8f, r, r, 'b'); g.ellipse(b.cx + b.rx * 0.5f, b.top + 0.8f, r, r, 'b')
            }
            Species.BAT -> g.spike(b.left + 2, b.top + 1, if (big) 5 else 4, 3, 'b', 'p')
            Species.DEER -> {
                // small sideways ears, plus antlers once grown
                g.rect(b.left - 1, b.top + 1, b.left + 1, b.top + 2, 'b')
                g.rect(N - 1 - b.left - 1, b.top + 1, N - 1 - b.left + 1, b.top + 2, 'b')
                if (grown) {
                    val h = if (big) 4 else 3
                    val ax = b.cx.toInt() - 3
                    for (i in 0 until h) g.mirror(ax, b.top - i, 'h')
                    g.mirror(ax - 1, b.top - h + 1, 'h'); g.mirror(ax + 1, b.top - h + 2, 'h')
                    if (big) { g.mirror(ax - 2, b.top - h, 'h'); g.mirror(ax + 1, b.top - h, 'h') }
                }
            }
            Species.OTTER, Species.RACCOON -> {
                val r = if (big) 1.6f else 1.3f
                g.ellipse(b.cx - b.rx * 0.65f, b.top + 1f, r, r, 'b'); g.ellipse(b.cx + b.rx * 0.65f, b.top + 1f, r, r, 'b')
            }
            Species.PANDA -> {
                val r = if (big) 2.2f else if (grown) 1.9f else 1.6f
                g.ellipse(b.cx - b.rx * 0.62f, b.top + 0.6f, r, r, 'd'); g.ellipse(b.cx + b.rx * 0.62f, b.top + 0.6f, r, r, 'd')
            }
            Species.LIZARD -> if (big) g.spike(b.left + 3, b.top + 1, 2, 2, 'd')
            Species.PUP, Species.PENGUIN, Species.HEDGEHOG -> Unit
            Species.SPROUT, Species.TURTLE -> Unit
        }

        // 3. Body, belly, feet
        g.ellipse(b.cx, b.cy, b.rx, b.ry, 'b')
        if (species == Species.TURTLE && grown) {
            val sy = b0.cy - b0.ry - 0.2f
            listOf(-3f, 0f, 3f).forEach { dx -> g.ellipse(b.cx + dx, sy + if (dx == 0f) 0f else 1.2f, 1f, 0.9f, 'y', onlyOver = 'd') }
        }
        if (grown) {
            val fx = (b.cx - b.rx * 0.5f).toInt()
            g.rect(fx - 1, b.bottom - 1, fx + 1, b.bottom, 'b')
            g.rect(N - 1 - fx - 1, b.bottom - 1, N - 1 - fx + 1, b.bottom, 'b')
        }
        g.ellipse(b.cx, b.cy + b.ry * 0.45f, b.rx * 0.55f, b.ry * 0.45f, 'l', onlyOver = 'b')
        when (species) {
            Species.OWL -> listOf(-1, 1).forEach { s -> g.ellipse(b.cx + s * b.rx * 0.4f, b.cy - b.ry * 0.1f, 1.9f, 1.9f, 'l', 'b') }
            Species.BEAR, Species.PUP -> g.ellipse(b.cx, b.cy + b.ry * 0.2f, 2.2f, 1.5f, 'l', 'b')
            Species.FOX -> g.ellipse(b.cx, b.cy + b.ry * 0.25f, 2.6f, 1.6f, 'l', 'b')
            Species.PENGUIN -> g.ellipse(b.cx, b.cy + b.ry * 0.15f, b.rx * 0.72f, b.ry * 0.8f, 'l', 'b')
            Species.OTTER -> g.ellipse(b.cx, b.cy + b.ry * 0.2f, 2.6f, 1.6f, 'l', 'b')
            Species.HEDGEHOG -> g.ellipse(b.cx, b.cy + b.ry * 0.1f, b.rx * 0.7f, b.ry * 0.75f, 'l', 'b')
            else -> Unit
        }
        if (species == Species.PUP) {
            // floppy ears hang over the sides of the head
            val h = if (big) 5 else if (grown) 4 else 3
            g.rect(b.left - 1, b.top + 1, b.left + 1, b.top + h, 'd')
            g.rect(N - 1 - b.left - 1, b.top + 1, N - 1 - b.left + 1, b.top + h, 'd')
        }
        g.outline()
        val patchY = (b.cy - b.ry * 0.15f).roundToInt()
        val patchX = (b.cx - b.rx * 0.4f).toInt()
        when (species) {
            Species.PANDA -> { // eye patches
                g.mirror(patchX, patchY, 'd'); g.mirror(patchX - 1, patchY, 'd'); g.mirror(patchX, patchY - 1, 'd')
                g.mirror(patchX - 1, patchY - 1, 'd'); g.mirror(patchX, patchY + 1, 'd')
            }
            Species.RACCOON -> for (x in (patchX - 2)..(N - 1 - patchX + 2)) { // bandit mask
                if (g[x, patchY] != '.' && g[x, patchY] != 'o') g.set(x, patchY, 'd')
                if (g[x, patchY - 1] != '.' && g[x, patchY - 1] != 'o') g.set(x, patchY - 1, 'd')
            }
            else -> Unit
        }

        // 4. Face
        val ey = (b.cy - b.ry * 0.15f).roundToInt().let { if (species == Species.FROG) b.top + 1 else it }
        val ex = (b.cx - b.rx * if (species == Species.FROG) 0.5f else 0.4f).toInt()
        if (mood == Mood.SLEEPY) {
            g.mirror(ex - 1, ey, 'e'); g.mirror(ex, ey, 'e')
        } else {
            g.mirror(ex, ey, 'e'); g.mirror(ex, ey - 1, 'e')
            if (species == Species.OWL || big) { g.mirror(ex - 1, ey, 'e'); g.mirror(ex - 1, ey - 1, 'e') }
        }
        val my = (b.cy + b.ry * 0.25f).roundToInt()
        val c = N / 2
        if (species == Species.OWL || species == Species.PENGUIN) {
            g.set(c - 1, my - 1, 'y'); g.set(c, my - 1, 'y'); g.set(c - 1, my, 'y'); g.set(c, my, 'y')
        } else {
            if (species in setOf(Species.PUP, Species.BEAR, Species.FOX, Species.KITTY, Species.HEDGEHOG, Species.DEER,
                    Species.OTTER, Species.PANDA, Species.RACCOON)) {
                g.set(c - 1, my - 1, 'e'); g.set(c, my - 1, 'e') // nose
            }
            when (mood) {
                Mood.HAPPY -> { g.set(c - 2, my, 'm'); g.set(c + 1, my, 'm'); g.set(c - 1, my + 1, 'm'); g.set(c, my + 1, 'm') }
                Mood.OKAY -> { g.set(c - 1, my + 1, 'm'); g.set(c, my + 1, 'm') }
                Mood.SAD -> { g.set(c - 1, my, 'm'); g.set(c, my, 'm'); g.set(c - 2, my + 1, 'm'); g.set(c + 1, my + 1, 'm') }
                Mood.SLEEPY -> g.set(c, my + 1, 'm')
            }
        }
        if (mood != Mood.SAD && species != Species.OWL && species != Species.PENGUIN) g.mirror(ex - 1, ey + 2, 'c')

        // 5. Front details
        when (species) {
            Species.SPROUT -> {
                val t = b.top - 1
                g.set(c, t, 'g'); g.set(c - 1, t, 'g')
                if (grown) { g.set(c - 2, t - 1, 'g'); g.set(c + 1, t - 1, 'g'); g.set(c - 3, t - 1, 'g'); g.set(c + 2, t - 1, 'g') }
                if (big) {
                    g.set(c - 1, t - 2, 'p'); g.set(c, t - 2, 'p'); g.set(c - 1, t - 3, 'y'); g.set(c, t - 3, 'y')
                }
            }
            Species.KITTY -> if (big) { g.set(c - 1, b.top + 1, 'd'); g.set(c, b.top + 1, 'd'); g.set(c, b.top + 2, 'd') }
            Species.DRAKE -> if (big) listOf(0, 2, 4).forEach { g.set(c - 3 + it, b.top - 1, 'd') }
            Species.FROG -> if (grown) { g.set(c - 4, b.cy.toInt() + 2, 'd'); g.set(c + 3, b.cy.toInt() + 1, 'd') }
            Species.BEAR -> if (big) g.mirror(ex - 2, ey + 3, 'd')
            Species.PUP -> if (big) g.rect(c - 3, b.bottom - 3, c + 2, b.bottom - 3, 'r') // collar
            Species.BAT -> { g.set(c - 2, my + 1, 'h'); g.set(c + 1, my + 1, 'h') } // little fangs
            Species.OTTER -> { g.mirror(c - 3, my, 'd'); g.mirror(c - 4, my - 1, 'd') } // whiskers
            Species.LIZARD -> if (grown) listOf(-3, 0, 3).forEach { dx -> g.set(c + dx, b.cy.toInt() - 1, 'd') } // back scales
            Species.DEER -> if (!grown) listOf(-3 to -1, 2 to 0, -2 to 2, 3 to 2).forEach { (dx, dy) ->
                if (g[c + dx, b.cy.toInt() + dy] == 'b') g.set(c + dx, b.cy.toInt() + dy, 'h') // fawn spots
            }
            Species.PENGUIN -> if (grown) { g.set(b.left, b.cy.toInt() + 1, 'd'); g.set(N - 1 - b.left, b.cy.toInt() + 1, 'd') }
            else -> Unit
        }
        return g.rows()
    }

    private fun egg(species: Species): List<String> {
        val g = Grid()
        g.ellipse(N / 2f, 11f, 5.5f, 7f, 'h')
        // Species-colored spots, placed differently for each species
        val seed = species.ordinal
        val spots = listOf(7 to 7, 11 to 9, 8 to 12, 12 to 14, 6 to 15, 10 to 5, 13 to 11, 7 to 10)
        spots.filterIndexed { i, _ -> (i + seed) % 3 != 0 }.forEach { (x, y) ->
            g.ellipse(x + 0.5f, y + 0.5f, 1.2f, 1f, 'b', onlyOver = 'h')
        }
        g.outline()
        return g.rows()
    }
}
