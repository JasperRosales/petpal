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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
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
    /** Fried Tilapia form (battle passive). */
    fried: Boolean = false,
    /** Raid skeleton: bone-white palette. */
    bone: Boolean = false,
    size: Dp = when (stage) {
        Stage.EGG -> 120.dp
        Stage.BABY -> 136.dp
        Stage.KID -> 168.dp
        Stage.TEEN -> 184.dp
        Stage.ADULT -> 200.dp
    },
) {
    // Sleepy pets don't bounce, so skip the per-frame animation entirely.
    val bounce = if (mood == Mood.SLEEPY) 0f else {
        val v by rememberInfiniteTransition(label = "pet").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(if (mood == Mood.HAPPY) 450 else 900), RepeatMode.Reverse),
            label = "bounce",
        )
        v
    }
    // Rasterize the 20x20 grid once into a tiny bitmap, then scale it up without smoothing:
    // one drawImage per redraw instead of up to 400 drawRects.
    val image = remember(stage, mood, species, fried, bone) { spriteBitmap(stage, mood, species, fried, bone) }
    val egg = stage == Stage.EGG

    Canvas(
        modifier.size(size).graphicsLayer {
            if (egg) rotationZ = (bounce - 0.5f) * 8f
            else translationY = -bounce * 4.dp.toPx()
        },
    ) {
        drawImage(
            image,
            dstSize = IntSize(this.size.width.roundToInt(), this.size.height.roundToInt()),
            filterQuality = FilterQuality.None,
        )
    }
}

private val spriteCache = HashMap<Long, ImageBitmap>()

/** Bitmaps are tiny (20x20) and there are few combinations, so keep them for the app's lifetime. */
private fun spriteBitmap(stage: Stage, mood: Mood, species: Species, fried: Boolean, bone: Boolean): ImageBitmap {
    val key = if (fried) -1L else ((species.ordinal.toLong() shl 16) or (stage.ordinal.toLong() shl 8) or mood.ordinal.toLong()) or
        (if (bone) 1L shl 40 else 0L)
    return spriteCache.getOrPut(key) {
        val grid = when {
            fried -> PetArt.friedFish()
            bone -> PetArt.SKELETON
            else -> PetArt.draw(species, stage, mood)
        }
        val palette = when {
            fried -> FRIED_PALETTE
            bone -> BONE_PALETTE
            else -> species.palette()
        }
        val img = ImageBitmap(PetArt.N, PetArt.N)
        val canvas = androidx.compose.ui.graphics.Canvas(img)
        val paint = Paint()
        grid.forEachIndexed { y, row ->
            row.forEachIndexed { x, c ->
                val color = palette[c] ?: return@forEachIndexed
                paint.color = color
                canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
            }
        }
        img
    }
}

private val FRIED_PALETTE = mapOf(
    'o' to Color(0xFF5A2E0C), 'e' to SpriteInk,
    'b' to Color(0xFFD08A2E), 'd' to Color(0xFF8A4A16), 'l' to Color(0xFFF0C062),
    'h' to Color(0xFFEDEDED), 'y' to Color(0xFFF7E15A), 'g' to SpriteLeaf,
)

/** Raid skeletons: every body colour becomes bone, while the ink stays dark. */
private val BONE_PALETTE = mapOf(
    'o' to SpriteInk, 'e' to SpriteInk, 'm' to SpriteInk,
    'b' to Color(0xFFE7E1D1), 'd' to Color(0xFFB6AE99), 'l' to Color(0xFFF8F4E8),
    'c' to Color(0xFFD8D2C2), 'p' to Color(0xFFD8D2C2), 'y' to Color(0xFFE7E1D1),
    'h' to Color(0xFFF8F4E8), 'g' to Color(0xFFC9C2AF), 'r' to Color(0xFFB6AE99), 'w' to Color.White,
)

private fun Species.palette(): Map<Char, Color> {
    val (body, dark, belly) = colors()
    return mapOf(
        'o' to SpriteInk, 'e' to SpriteInk, 'm' to SpriteInk,
        'b' to body, 'd' to dark, 'l' to belly,
        'c' to SpriteCheek, 'p' to SpritePink, 'y' to SpriteYellow,
        'h' to SpriteCream, 'g' to SpriteLeaf, 'r' to SpriteRed, 'w' to Color.White,
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
    Species.HAMSTER -> Triple(Color(0xFFF2C27A), Color(0xFFB9824A), Color(0xFFFFF4E2))
    Species.PARROT -> Triple(Color(0xFF5CC46A), Color(0xFF2F8A44), Color(0xFFF4E27A))
    Species.AXOLOTL -> Triple(Color(0xFFF6B8CC), Color(0xFFE07A9E), Color(0xFFFFE4EC))
    Species.MOLE -> Triple(Color(0xFF6B5A6E), Color(0xFF3E3342), Color(0xFFB8A9BC))
    Species.KOALA -> Triple(Color(0xFF9EA3AB), Color(0xFF5E636B), Color(0xFFE4E6EA))
    Species.SHARK -> Triple(Color(0xFF6E8FB0), Color(0xFF3F5C7A), Color(0xFFF2F5F8))
    Species.CHAMELEON -> Triple(Color(0xFF7ACB5E), Color(0xFF3F8A3A), Color(0xFFE8F2A0))
    Species.BEAVER -> Triple(Color(0xFFA0703F), Color(0xFF5E3F22), Color(0xFFE2C49A))
    Species.MONKEY -> Triple(Color(0xFF9A6A44), Color(0xFF5E3D24), Color(0xFFF0D2AE))
    Species.KANGAROO -> Triple(Color(0xFFD08A52), Color(0xFF8E5530), Color(0xFFF6DDBF))
    Species.EAGLE -> Triple(Color(0xFF7A5434), Color(0xFF4A3220), Color(0xFFFAF7F0))
    Species.SEAL -> Triple(Color(0xFFB8C2CC), Color(0xFF6E7C8A), Color(0xFFEEF2F5))
    Species.ELEPHANT -> Triple(Color(0xFFA7AEB8), Color(0xFF6B7380), Color(0xFFD8DCE2))
    Species.TIGER -> Triple(Color(0xFFF08A2C), Color(0xFF2B2420), Color(0xFFFFF4E6))
    Species.TILAPIA -> Triple(Color(0xFF8FA8A0), Color(0xFF4F6B66), Color(0xFFE6EEDF))
    Species.CAPYBARA -> Triple(Color(0xFFB08456), Color(0xFF6E4C2E), Color(0xFFD9B98E))
    Species.CRAB -> Triple(Color(0xFFE8634A), Color(0xFFA83A2A), Color(0xFFF6C7A8))
    Species.FLAMINGO -> Triple(Color(0xFFF59AB5), Color(0xFFD9668C), Color(0xFFFFD6E2))
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
        Stage.TEEN -> Body(cy = 11.2f, rx = 6.5f, ry = 5.8f)
        else -> Body(cy = 11f, rx = 7f, ry = 6.2f)
    }

    /** Body width/height scale per species, so silhouettes differ before any features go on. */
    private fun Species.build(): Pair<Float, Float> = when (this) {
        Species.PENGUIN -> 0.78f to 1.12f
        Species.OWL -> 0.86f to 1.1f
        Species.EAGLE -> 0.84f to 1.08f
        Species.KANGAROO -> 0.8f to 1.1f
        Species.DEER -> 0.82f to 1.06f
        Species.FOX -> 0.86f to 1.02f
        Species.KITTY -> 0.9f to 0.94f
        Species.BUNNY -> 0.86f to 0.86f
        Species.BAT -> 0.76f to 0.8f
        Species.PARROT -> 0.8f to 1.04f
        Species.MONKEY -> 0.84f to 1.0f
        Species.SPROUT -> 0.92f to 0.9f
        Species.FROG -> 1.08f to 0.76f
        Species.SHARK -> 1.06f to 0.74f
        Species.TILAPIA -> 1.0f to 0.72f
        Species.LIZARD -> 1.02f to 0.74f
        Species.CHAMELEON -> 1.0f to 0.8f
        Species.SEAL -> 1.08f to 0.78f
        Species.MOLE -> 1.1f to 0.8f
        Species.HEDGEHOG -> 1.04f to 0.82f
        Species.AXOLOTL -> 1.02f to 0.8f
        Species.HAMSTER -> 1.08f to 0.94f
        Species.OTTER -> 0.88f to 1.04f
        Species.ELEPHANT -> 1.12f to 1.04f
        Species.BEAR -> 1.1f to 1.06f
        Species.PANDA -> 1.08f to 1.04f
        Species.KOALA -> 0.96f to 1.0f
        Species.BEAVER -> 1.0f to 0.96f
        Species.TIGER -> 1.04f to 0.96f
        Species.CAPYBARA -> 1.12f to 0.9f
        Species.CRAB -> 0.98f to 0.7f
        Species.FLAMINGO -> 0.8f to 1.1f
        else -> 1f to 1f
    }

    private val SIDE_TAILS = setOf(Species.LIZARD, Species.SHARK, Species.CHAMELEON, Species.MONKEY, Species.KANGAROO,
        Species.TIGER, Species.TILAPIA, Species.KITTY, Species.FOX, Species.RACCOON, Species.BEAVER)

    /** How each species' eyes look: 0 = 1x2, 1 = 2x2 with a shine, 2 = single dot, 3 = 2x1 slit. */
    private fun Species.eyeStyle() = when (this) {
        Species.BUNNY, Species.KITTY, Species.HAMSTER, Species.AXOLOTL, Species.SEAL, Species.KOALA, Species.BAT, Species.DEER -> 1
        Species.SHARK, Species.MOLE, Species.TILAPIA, Species.ELEPHANT, Species.PENGUIN, Species.CAPYBARA,
        Species.CRAB, Species.FLAMINGO -> 2
        Species.LIZARD, Species.CHAMELEON, Species.TIGER, Species.FOX, Species.EAGLE -> 3
        else -> 0
    }

    fun draw(species: Species, stage: Stage, mood: Mood): List<String> {
        if (stage == Stage.EGG) return egg(species)
        custom(species, stage)?.let { return withMood(it, mood) }
        val g = Grid()
        val base = bodyFor(stage)
        val (wx, hy) = species.build().let { (w, h) -> // babies are rounder, the shape shows more as they grow
            if (stage == Stage.BABY) (1 + (w - 1) * 0.5f) to (1 + (h - 1) * 0.5f) else w to h
        }
        val ry = base.ry * hy
        // Keep the feet on the same ground line; side-tailed species stay narrow enough to fit the grid
        val b0 = base.copy(cy = (base.cy + base.ry - ry).coerceAtMost(18.4f - ry), rx = minOf(base.rx * wx, if (species in SIDE_TAILS) 7f else 7.8f), ry = ry)
        // Turtles: smaller body tucked under a shell dome
        val b = if (species == Species.TURTLE) b0.copy(cy = b0.cy + 1f, rx = b0.rx - 0.5f, ry = b0.ry - 1f) else b0
        val big = stage >= Stage.TEEN
        val grown = stage != Stage.BABY

        // 1. Behind the body
        when (species) {
            Species.DRAKE -> {
                val wy = b.cy.toInt() - 2
                val span = if (big) 3 else if (grown) 2 else 1
                for (i in 0..span) for (y in wy - i..wy + 2) for (x in b.left - 1 - i..b.left) g.mirror(x, y, 'd')
            }
            Species.TURTLE -> g.ellipse(b0.cx, b0.cy - 1.2f, b0.rx + 0.8f, b0.ry, 'd')
            Species.PARROT -> { // folded wings and a long tail feather
                val wy = b.cy.toInt() - 1
                val span = if (big) 2 else 1
                for (i in 0..span) for (y in wy..wy + 3 + i) for (x in b.left - 1 - i..b.left) g.mirror(x, y, 'd')
                g.rect(b.cx.toInt() - 1, b.bottom - 1, b.cx.toInt(), b.bottom + (if (grown) 2 else 1), 'r')
            }
            Species.AXOLOTL -> { // feathery gills fanning out from the head
                val n = if (big) 3 else 2
                for (k in 0 until n) {
                    val y = b.top + 1 + k * 2
                    val len = if (k == 1 || n == 2) 3 else 2
                    for (x in b.left - len..b.left + 1) g.mirror(x, y, 'd')
                }
            }
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
            Species.SHARK -> { // tail fin sticking out to the side
                val tx = b.left + 2 * b.rx.toInt()
                val ty = b.cy.toInt() + 1
                g.rect(tx - 1, ty, tx + 1, ty + 1, 'b')
                val h = if (big) 3 else 2
                for (i in 0..h) { g.set(tx + 1 + i / 2, ty - i, 'd'); g.set(tx + 1 + i / 2, ty + 1 + i, 'd') }
            }
            Species.CHAMELEON -> { // curled tail
                val tx = b.left + 2 * b.rx.toInt()
                val ty = b.cy.toInt() + 2
                val r = if (big) 1.8f else 1.4f
                g.rect(tx - 1, ty, tx + 1, ty + 1, 'b')
                g.ellipse(tx + 1.5f + r * 0.5f, ty - r * 0.6f, r, r, 'b')
                g.ellipse(tx + 1.5f + r * 0.5f, ty - r * 0.6f, r * 0.45f, r * 0.45f, 'd', onlyOver = 'b')
            }
            Species.MONKEY -> { // curly tail
                val tx = b.left + 2 * b.rx.toInt()
                val ty = b.cy.toInt() + 2
                val r = if (big) 1.8f else 1.4f
                g.rect(tx - 1, ty, tx + 1, ty, 'd')
                g.ellipse(tx + 1.5f + r * 0.5f, ty - r, r, r, 'd')
                g.ellipse(tx + 1.5f + r * 0.5f, ty - r, r * 0.45f, r * 0.45f, '.', onlyOver = 'd')
            }
            Species.KANGAROO -> { // thick tail resting on the ground
                val tx = b.left + 2 * b.rx.toInt()
                val ty = b.bottom - 1
                g.rect(tx - 1, ty - 1, tx + (if (big) 3 else if (grown) 2 else 1), ty, 'b')
            }
            Species.EAGLE -> { // broad wings
                val wy = b.cy.toInt() - 2
                val span = if (big) 3 else if (grown) 2 else 1
                for (i in 0..span) for (y in wy - i..wy + 3) for (x in b.left - 1 - i..b.left) g.mirror(x, y, 'd')
            }
            Species.SEAL -> { // tail flippers
                val y = b.bottom
                val w = if (big) 3 else 2
                g.rect(b.cx.toInt() - w, y - 1, b.cx.toInt() - 1, y, 'd')
                g.rect(b.cx.toInt(), y - 1, b.cx.toInt() + w - 1, y, 'd')
            }
            Species.TIGER -> { // striped tail
                val tx = b.left + 2 * b.rx.toInt()
                g.rect(tx - 1, b.cy.toInt() - (if (grown) 4 else 2), tx, b.cy.toInt() + 2, 'b')
                g.set(tx, b.cy.toInt() - 1, 'd'); if (grown) g.set(tx, b.cy.toInt() - 3, 'd')
            }
            Species.TILAPIA -> { // fanned tail fin
                val tx = b.left + 2 * b.rx.toInt()
                val ty = b.cy.toInt()
                val h = if (big) 4 else 3
                for (i in 0..h) for (y in ty - i..ty + i) g.set(tx - 1 + i, y, 'd')
            }
            Species.BEAVER -> { // flat paddle tail
                val r = if (big) 2.4f else if (grown) 2f else 1.6f
                g.ellipse(b.cx + b.rx + r * 0.5f, b.bottom - r * 0.6f, r, r * 0.8f, 'd')
            }
            Species.CRAB -> { // big claws held up at the sides
                val r = if (big) 2.2f else if (grown) 1.9f else 1.5f
                listOf(-1, 1).forEach { s ->
                    val x = b.cx + s * (b.rx + 0.4f)
                    g.ellipse(x, b.cy - b.ry * 0.6f, r, r, 'd')
                    g.ellipse(x + s * r * 0.3f, b.cy - b.ry * 0.6f - r * 0.6f, r * 0.35f, r * 0.55f, '.', onlyOver = 'd')
                }
                for (k in 0..2) g.mirror(b.left + 1 + k, b.bottom, 'd') // little legs
            }
            Species.FLAMINGO -> { // folded wings and a short tail tuft
                val wy = b.cy.toInt() - 1
                val span = if (big) 2 else 1
                for (i in 0..span) for (y in wy..wy + 2 + i) for (x in b.left - 1 - i..b.left) g.mirror(x, y, 'd')
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
            Species.PARROT -> g.spike(b.cx.toInt() - 1, b.top + 1, if (big) 4 else 3, 2, 'r') // crest
            Species.KOALA -> { // big fluffy ears
                val r = if (big) 2.8f else if (grown) 2.4f else 2f
                listOf(-1, 1).forEach { s ->
                    g.ellipse(b.cx + s * b.rx * 0.8f, b.top + 1f, r, r, 'b')
                    g.ellipse(b.cx + s * b.rx * 0.8f, b.top + 1f, r * 0.5f, r * 0.5f, 'l')
                }
            }
            Species.SHARK -> g.spike(b.cx.toInt() - 1, b.top + 1, if (big) 5 else if (grown) 4 else 3, 2, 'd') // dorsal fin
            Species.CHAMELEON -> g.spike(b.cx.toInt() - 1, b.top + 1, if (big) 3 else 2, 3, 'd') // head crest
            Species.MONKEY -> { // big round ears out to the sides
                val r = if (big) 2f else 1.6f
                listOf(-1, 1).forEach { s ->
                    g.ellipse(b.cx + s * (b.rx + 0.2f), b.cy - b.ry * 0.3f, r, r, 'b')
                    g.ellipse(b.cx + s * (b.rx + 0.2f), b.cy - b.ry * 0.3f, r * 0.5f, r * 0.5f, 'l')
                }
            }
            Species.KANGAROO -> g.spike(b.left + 2, b.top + 1, if (big) 5 else if (grown) 4 else 3, 3, 'b', 'l')
            Species.EAGLE -> g.ellipse(b.cx, b.top + 2f, b.rx * 0.7f, 2.4f, 'l') // white head
            Species.SEAL -> Unit
            Species.ELEPHANT -> { // big flappy ears
                val r = if (big) 3.4f else if (grown) 3f else 2.5f
                listOf(-1, 1).forEach { s ->
                    g.ellipse(b.cx + s * (b.rx + 0.2f), b.cy - b.ry * 0.25f, r, r * 1.2f, 'b')
                    g.ellipse(b.cx + s * (b.rx + 0.4f), b.cy - b.ry * 0.25f, r * 0.55f, r * 0.75f, 'p')
                }
            }
            Species.TIGER -> g.spike(b.left + 2, b.top + 1, if (big) 4 else 3, 4, 'b', 'l')
            Species.TILAPIA -> for (k in -3..2) { // spiny dorsal fin
                val h = if ((k + 3) % 2 == 0) (if (big) 3 else 2) else 1
                for (i in 0..h) g.set(N / 2 + k, b.top - i + 1, 'd')
            }
            Species.OTTER, Species.RACCOON, Species.HAMSTER, Species.BEAVER -> {
                val r = if (big) 1.6f else 1.3f
                g.ellipse(b.cx - b.rx * 0.65f, b.top + 1f, r, r, 'b'); g.ellipse(b.cx + b.rx * 0.65f, b.top + 1f, r, r, 'b')
            }
            Species.PANDA -> {
                val r = if (big) 2.2f else if (grown) 1.9f else 1.6f
                g.ellipse(b.cx - b.rx * 0.62f, b.top + 0.6f, r, r, 'd'); g.ellipse(b.cx + b.rx * 0.62f, b.top + 0.6f, r, r, 'd')
            }
            Species.LIZARD -> if (big) g.spike(b.left + 3, b.top + 1, 2, 2, 'd')
            Species.CAPYBARA -> { // tiny round ears
                val r = if (big) 1.2f else 1f
                g.ellipse(b.cx - b.rx * 0.6f, b.top + 0.8f, r, r, 'd'); g.ellipse(b.cx + b.rx * 0.6f, b.top + 0.8f, r, r, 'd')
            }
            Species.CRAB -> { // eye stalks
                val h = if (big) 3 else 2
                for (i in 0..h) g.mirror(N / 2 - 3, b.top - i + 1, 'b')
            }
            Species.FLAMINGO -> if (grown) g.spike(b.cx.toInt() - 1, b.top + 1, if (big) 3 else 2, 2, 'd') // feather tuft
            Species.PUP, Species.PENGUIN, Species.HEDGEHOG, Species.AXOLOTL, Species.MOLE -> Unit
            Species.SPROUT, Species.TURTLE -> Unit
        }

        // 3. Body, belly, feet
        g.ellipse(b.cx, b.cy, b.rx, b.ry, 'b')
        if (species == Species.TURTLE && grown) {
            val sy = b0.cy - b0.ry - 0.2f
            listOf(-3f, 0f, 3f).forEach { dx -> g.ellipse(b.cx + dx, sy + if (dx == 0f) 0f else 1.2f, 1f, 0.9f, 'y', onlyOver = 'd') }
        }
        if (grown && species == Species.FLAMINGO) { // thin legs
            val fx = (b.cx - b.rx * 0.35f).toInt()
            g.rect(fx, b.bottom - 1, fx, b.bottom + 1, 'd'); g.rect(N - 1 - fx, b.bottom - 1, N - 1 - fx, b.bottom + 1, 'd')
        } else if (grown && species != Species.TILAPIA && species != Species.CRAB) {
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
            Species.HAMSTER -> listOf(-1, 1).forEach { s -> // puffy cheek pouches
                g.ellipse(b.cx + s * b.rx * 0.62f, b.cy + b.ry * 0.2f, 1.8f, 1.5f, 'l', 'b')
            }
            Species.PARROT -> g.ellipse(b.cx, b.cy + b.ry * 0.2f, b.rx * 0.6f, b.ry * 0.7f, 'l', 'b')
            Species.MONKEY -> g.ellipse(b.cx, b.cy + b.ry * 0.05f, b.rx * 0.6f, b.ry * 0.55f, 'l', 'b') // face
            Species.EAGLE -> g.ellipse(b.cx, b.top + 2.2f, b.rx * 0.75f, 2.4f, 'l', 'b') // white head
            Species.SEAL -> g.ellipse(b.cx, b.cy + b.ry * 0.25f, b.rx * 0.6f, b.ry * 0.6f, 'l', 'b')
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
        val ey = (b.cy - b.ry * 0.15f).roundToInt().let {
            when (species) { Species.FROG -> b.top + 1; Species.CRAB -> b.top - (if (big) 2 else 1); else -> it }
        }
        val ex = if (species == Species.CRAB) N / 2 - 3 else (b.cx - b.rx * if (species == Species.FROG) 0.5f else 0.4f).toInt()
        val eyes = species.eyeStyle()
        if (mood == Mood.SLEEPY) {
            g.mirror(ex - 1, ey, 'e'); g.mirror(ex, ey, 'e')
        } else when (eyes) {
            1 -> { // big shiny eyes
                g.mirror(ex - 1, ey, 'e'); g.mirror(ex, ey, 'e'); g.mirror(ex - 1, ey - 1, 'e'); g.mirror(ex, ey - 1, 'w')
                if (big) { g.mirror(ex - 1, ey + 1, 'e'); g.mirror(ex, ey + 1, 'e') }
            }
            2 -> { g.mirror(ex, ey, 'e'); if (big) g.mirror(ex, ey - 1, 'e') } // beady
            3 -> { // narrow, sharp
                g.mirror(ex - 1, ey, 'e'); g.mirror(ex, ey, 'e')
                if (big) g.mirror(ex - 1, ey - 1, 'd')
            }
            else -> {
                g.mirror(ex, ey, 'e'); g.mirror(ex, ey - 1, 'e')
                if (species == Species.OWL || big) { g.mirror(ex - 1, ey, 'e'); g.mirror(ex - 1, ey - 1, 'e') }
                if (species == Species.OWL) { g.mirror(ex, ey - 1, 'w') }
            }
        }
        val my = (b.cy + b.ry * 0.25f).roundToInt()
        val c = N / 2
        if (species == Species.FLAMINGO) { // bent beak with a dark tip
            g.set(c - 1, my - 1, 'h'); g.set(c, my - 1, 'h'); g.set(c - 1, my, 'h'); g.set(c, my, 'h')
            g.set(c - 1, my + 1, 'e'); g.set(c, my + 1, 'e')
        } else if (species == Species.OWL || species == Species.PENGUIN || species == Species.PARROT || species == Species.EAGLE) {
            g.set(c - 1, my - 1, 'y'); g.set(c, my - 1, 'y'); g.set(c - 1, my, 'y'); g.set(c, my, 'y')
            if (species == Species.PARROT || species == Species.EAGLE) { g.set(c - 1, my + 1, 'y'); g.set(c, my + 1, 'e') } // hooked beak
        } else {
            if (species == Species.MOLE) { // big pink snout
                g.set(c - 1, my - 1, 'p'); g.set(c, my - 1, 'p'); g.set(c - 2, my - 1, 'p'); g.set(c + 1, my - 1, 'p')
                g.set(c - 1, my - 2, 'p'); g.set(c, my - 2, 'p')
            }
            if (species == Species.KOALA) { // big dark nose
                g.set(c - 1, my - 2, 'e'); g.set(c, my - 2, 'e'); g.set(c - 2, my - 1, 'e'); g.set(c + 1, my - 1, 'e')
            }
            if (species in setOf(Species.PUP, Species.BEAR, Species.FOX, Species.KITTY, Species.HEDGEHOG, Species.DEER,
                    Species.OTTER, Species.PANDA, Species.RACCOON, Species.HAMSTER, Species.KOALA, Species.BEAVER,
                    Species.MONKEY, Species.KANGAROO, Species.SEAL, Species.TIGER)) {
                g.set(c - 1, my - 1, 'e'); g.set(c, my - 1, 'e') // nose
            }
            when (mood) {
                Mood.HAPPY -> { g.set(c - 2, my, 'm'); g.set(c + 1, my, 'm'); g.set(c - 1, my + 1, 'm'); g.set(c, my + 1, 'm') }
                Mood.OKAY -> { g.set(c - 1, my + 1, 'm'); g.set(c, my + 1, 'm') }
                Mood.SAD -> { g.set(c - 1, my, 'm'); g.set(c, my, 'm'); g.set(c - 2, my + 1, 'm'); g.set(c + 1, my + 1, 'm') }
                Mood.SLEEPY -> g.set(c, my + 1, 'm')
            }
        }
        if (mood != Mood.SAD && species != Species.OWL && species != Species.PENGUIN && species != Species.PARROT && species != Species.EAGLE &&
            species != Species.FLAMINGO && species != Species.CRAB) {
            g.mirror(ex - 1, ey + 2, 'c')
        }

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
            Species.MOLE -> { // digging claws
                val y = b.cy.toInt() + 2
                g.mirror(b.left, y, 'h'); g.mirror(b.left, y + 1, 'h'); g.mirror(b.left + 1, y + 1, 'h')
                if (grown) g.mirror(b.left - 1, y + 1, 'h')
            }
            Species.AXOLOTL -> if (grown) listOf(-2, 1).forEach { dx -> g.set(c + dx, b.cy.toInt() + 2, 'd') } // freckles
            Species.BEAVER -> { g.set(c - 1, my + 2, 'h'); g.set(c, my + 2, 'h') } // buck teeth
            Species.SEAL -> { // whiskers and front flippers
                g.mirror(c - 3, my, 'd'); g.mirror(c - 4, my - 1, 'd')
                g.mirror(b.left + 1, b.cy.toInt() + 2, 'd')
            }
            Species.KANGAROO -> if (grown) { // pouch
                val py = b.cy.toInt() + 2
                for (x in c - 2..c + 1) g.set(x, py, 'd')
                g.set(c - 2, py + 1, 'd'); g.set(c + 1, py + 1, 'd')
            }
            Species.EAGLE -> if (big) { g.mirror(b.left + 1, b.bottom, 'y'); g.mirror(b.left + 2, b.bottom, 'y') } // talons
            Species.MONKEY -> if (big) g.mirror(c - 3, b.top + 1, 'd') // brow tufts
            Species.ELEPHANT -> { // trunk, plus tusks once grown
                for (y in my - 1..my + (if (grown) 3 else 2)) { g.set(c - 1, y, 'd'); g.set(c, y, 'd') }
                g.set(c + 1, my + (if (grown) 3 else 2), 'd')
                if (grown) { g.set(c - 2, my + 1, 'h'); g.set(c + 1, my + 1, 'h') }
                if (big) { g.set(c - 3, my + 2, 'h'); g.set(c + 2, my + 2, 'h') }
            }
            Species.TIGER -> { // forehead and side stripes
                g.set(c - 1, b.top + 1, 'd'); g.set(c, b.top + 1, 'd'); g.set(c - 1, b.top + 2, 'd')
                if (grown) { g.mirror(c - 3, b.top + 1, 'd') }
                for (dy in 0..2) if (g[b.left + 1, b.cy.toInt() + dy] == 'b') g.mirror(b.left + 1, b.cy.toInt() + dy, 'd')
                if (big) for (dy in 0..1) if (g[b.left + 2, b.cy.toInt() - 2 + dy] == 'b') g.mirror(b.left + 2, b.cy.toInt() - 2 + dy, 'd')
            }
            Species.TILAPIA -> { // gills, faint bars and side fins
                listOf(0, 1).forEach { dy -> g.mirror(b.left + 1, b.cy.toInt() + dy, 'd') }
                if (grown) listOf(-3, 2).forEach { dx ->
                    for (dy in -2..-1) if (g[c + dx, b.cy.toInt() + dy] == 'b') g.set(c + dx, b.cy.toInt() + dy, 'd')
                }
                g.mirror(b.left + 1, b.cy.toInt() + 2, 'd')
            }
            Species.SHARK -> listOf(0, 1).forEach { dy -> g.mirror(b.left + 1, b.cy.toInt() + dy, 'd') } // gills
            Species.CAPYBARA -> { // broad dark snout, plus a yuzu on its head once grown
                for (x in c - 2..c + 1) g.set(x, my - 1, 'd')
                if (grown) { g.set(c - 1, b.top - 1, 'y'); g.set(c, b.top - 1, 'y'); g.set(c - 1, b.top - 2, 'y'); g.set(c, b.top - 2, 'y') }
                if (big) g.set(c, b.top - 3, 'g')
            }
            Species.CRAB -> {
                if (mood != Mood.SLEEPY) { g.mirror(ex - 1, ey, 'w'); g.mirror(ex, ey, 'e') } // eyes on the stalk tips
                if (grown) listOf(-3, 2).forEach { dx -> // shell spots
                    if (g[c + dx, b.cy.toInt()] == 'b') g.set(c + dx, b.cy.toInt(), 'd')
                }
            }
            Species.FLAMINGO -> if (big) { g.set(c - 1, b.top + 1, 'w'); g.set(c, b.top + 1, 'w') } // shiny crown
            Species.CHAMELEON -> if (grown) listOf(-3, 0, 3).forEach { dx -> // stripes
                for (dy in 0..1) if (g[c + dx, b.cy.toInt() + dy] == 'b') g.set(c + dx, b.cy.toInt() + dy, 'd')
            }
            else -> Unit
        }
        return g.rows()
    }

    // Hand-drawn sprites for species the blob template can't capture. 'E' marks eye pixels (they close when sleepy).
    private val EAGLE_BABY = listOf(
        "....................",
        "....................",
        "....................",
        "....................",
        "........o..o........",
        ".......ohooho.......",
        "......oohhhhoo......",
        "....oohhhhhhhhoo....",
        "...ohhhhhhhhhhhho...",
        "...ohhEEhhhhEEhho...",
        "...ohhEwoyyowEhho...",
        "...ohhhhoyyohhhho...",
        "..odhhhhheehhhhhdo..",
        "..odhhllllllllhhdo..",
        "..oddhllllllllhddo..",
        "...odhllllllllhdo...",
        "...ohhllllllllhho...",
        "....oohhhhhhhhoo....",
        "....oyyooooooyyo....",
        "....................",
    )
    private val EAGLE_KID = listOf(
        "....................",
        "....................",
        "....................",
        ".......oooooo.......",
        ".....oobbbbbboo.....",
        "....obblbbbblbbo....",
        "...obbbbbllbbbbbo...",
        "...odddbbbbbbdddo...",
        "...obbEEoyyoEEbbo...",
        "...obbEwoyyowEbbo...",
        "..oobbbboyyobbbboo..",
        ".oddobbboeeobbboddo.",
        ".odddoblbhhblbodddo.",
        "odbddobhbbbbhboddbdo",
        "oddbdbbbhbbhbbbdbddo",
        ".odbdbbhbbbbhbbdbdo.",
        ".ododbbbbbbbbbbdodo.",
        "..o.oobbllllbboo.o..",
        "....oyyoollooyyo....",
        "...oyoyooooooyoyo...",
    )
    private val EAGLE_TEEN = listOf(
        "....................",
        "....................",
        "......oooooooo......",
        "....oolllllllloo....",
        "...ollbllbbllbllo...",
        "..olllbllbbllblllo..",
        "..oldddlllllldddlo..",
        "..ollEEoyyyyoEEllo..",
        "..ollEwoyyyyowEllo..",
        ".oobllbloyyolbllboo.",
        "oddolllloeeolllloddo",
        "odddoblbboobblbodddo",
        "oddbdobbbbbbbbodbddo",
        "odbdbdbhbbbbhbdbdbdo",
        "oddbdbbbhbbhbbbdbddo",
        ".odbdbbbbbbbbbbdbdo.",
        ".ododbdbbbbbbdbdodo.",
        "..o.oobboooobboo.o..",
        "....oyyolllloyyo....",
        "...oyoyoollooyoyo...",
    )
    private val EAGLE_ADULT = listOf(
        "....................",
        "....................",
        "......oooooooo......",
        "....oolllllllloo....",
        "...ollllllllllllo...",
        "..ollllllllllllllo..",
        "..oldddlllllldddlo..",
        "..ollEEoyyyyoEEllo..",
        "..ollEwoyyyyowEllo..",
        ".oollllloyyollllloo.",
        "oddolllloeeolllloddo",
        "odddoblbboobblbodddo",
        "oddbdobbbbbbbbodbddo",
        "odbdbdbhbbbbhbdbdbdo",
        "oddbdbbbhbbhbbbdbddo",
        ".odbdbbbbbbbbbbdbdo.",
        ".ododbdbbbbbbdbdodo.",
        "..o.oobboooobboo.o..",
        "....oyyolllloyyo....",
        "...oyoyoollooyoyo...",
    )
    private val FLAMINGO_BABY = listOf(
        "....................",
        "....................",
        "....................",
        "....................",
        "....................",
        ".......oooo.........",
        "......ollllo........",
        "......olEllhoo......",
        "......ollllhheo.....",
        ".......ollooo.......",
        "......olllloooo.....",
        ".....ollllllllloo...",
        ".....olllhhhllllo...",
        ".....ollhhhhhllllo..",
        ".....olllhhhlllllo..",
        "......olllllllllo...",
        ".......oollllloo....",
        "........od..od......",
        "........od..od......",
        ".......odd.odd......",
    )
    private val FLAMINGO_ADULT = listOf(
        "....................",
        "......oooo..........",
        ".....obbbbo.........",
        ".....obEbbhoo.......",
        ".....obbbbhhho......",
        "......oobooeeo......",
        "........obo.oo......",
        ".......obo..........",
        "......obo...........",
        "......obo...........",
        "......obboooooo.....",
        ".......obbbbbbboooo.",
        "......obbbdddbbbddo.",
        "......obbdddddbbbddo",
        "......obbbdddbbbbbo.",
        ".......obbbbbbbbbo..",
        "........oobbbboooo..",
        "..........od.oddo...",
        "..........od...o....",
        ".........oddd.......",
    )

    private fun custom(species: Species, stage: Stage): List<String>? = when (species) {
        Species.EAGLE -> when (stage) {
            Stage.BABY -> EAGLE_BABY; Stage.KID -> EAGLE_KID; Stage.TEEN -> EAGLE_TEEN; else -> EAGLE_ADULT
        }
        Species.FLAMINGO -> if (stage == Stage.BABY) FLAMINGO_BABY else FLAMINGO_ADULT
        else -> null
    }

    /** Opens or closes the hand-drawn eyes: awake = ink with a shine, sleepy = a closed line. */
    private fun withMood(rows: List<String>, mood: Mood): List<String> {
        val eyeRows = rows.indices.filter { 'E' in rows[it] }
        val lid = if (mood == Mood.SLEEPY && eyeRows.size > 1) eyeRows.first() else -1
        return rows.mapIndexed { y, r ->
            val chars = r.toCharArray()
            for (x in chars.indices) when (chars[x]) {
                'E' -> chars[x] = if (y == lid) chars.getOrNull(x - 1)?.takeIf { it != 'E' } ?: chars.getOrElse(x - 2) { 'b' } else 'e'
                'w' -> if (mood == Mood.SLEEPY && 'E' in r) chars[x] = 'e'
            }
            chars.concatToString()
        }
    }

    /** Raid skeleton: skull with hollow eyes, ribcage, bony arms and legs. */
    val SKELETON = listOf(
        "....................",
        "......oooooooo......",
        ".....obbbbbbbbo.....",
        "....obbbbbbbbbbo....",
        "....obeebbbbeebo....",
        "....oeeebbbbeeeo....",
        "....obbbbeebbbbo....",
        ".....obbbbbbbbo.....",
        ".....obebeebebo.....",
        "......oooooooo......",
        "........obbo........",
        "...oooooobboooooo...",
        "..obbbbbobbobbbbbo..",
        "..ob.oooobboooo.bo..",
        "..ob.obbbbbbbbo.bo..",
        "..ob..ooobbooo..bo..",
        "..ob.obbbbbbbbo.bo..",
        "..oo..oobbbboo..oo..",
        "......obboobbo......",
        "......ob....bo......",
    )

    /** Fried Tilapia: crispy golden fish with an X eye, still steaming. */
    fun friedFish(): List<String> {
        val g = Grid()
        g.ellipse(8.5f, 11.5f, 6.5f, 3.8f, 'b')
        for (i in 0..3) for (y in 11 - i - 1..11 + i + 1) g.set(15 + i, y, 'd') // tail
        for (k in -2..2) g.set(8 + k * 2, 7, 'd') // crispy fin spikes
        g.ellipse(8.5f, 13.2f, 5f, 1.6f, 'l', onlyOver = 'b')
        g.outline()
        listOf(7, 10, 13).forEach { x -> for (d in 0..2) if (g[x - d, 10 + d] == 'b') g.set(x - d, 10 + d, 'd') } // grill marks
        listOf(4 to 10, 6 to 10, 5 to 11, 4 to 12, 6 to 12).forEach { (x, y) -> g.set(x, y, 'e') } // X eye
        listOf(6 to 5, 7 to 4, 6 to 3, 10 to 5, 11 to 4, 10 to 3).forEach { (x, y) -> g.set(x, y, 'h') } // steam
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
