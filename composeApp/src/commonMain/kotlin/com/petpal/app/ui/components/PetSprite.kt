package com.petpal.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.petpal.app.domain.model.*
import com.petpal.app.ui.components.pet.PetArt
import com.petpal.app.ui.components.pet.model
import kotlin.math.roundToInt

/**
 * Pixel-art pet. The art comes from the species' [com.petpal.app.ui.components.pet.PetModel];
 * here it's cropped to its visible pixels and scaled so every pet fills the same box
 * (bottom-centered), with whole-screen-pixel cells so edges stay crisp.
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
        Stage.EGG -> 88.dp
        Stage.BABY -> 104.dp
        Stage.KID -> 124.dp
        Stage.TEEN -> 140.dp
        Stage.ADULT -> 156.dp
    },
) {
    // Sleepy pets don't bounce, so skip the per-frame animation entirely.
    // Read only inside graphicsLayer, so the bounce redraws the layer without recomposing.
    val bounce: State<Float> = if (mood == Mood.SLEEPY) Still else
        rememberInfiniteTransition(label = "pet").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(if (mood == Mood.HAPPY) 450 else 900), RepeatMode.Reverse),
            label = "bounce",
        )
    val sprite = remember(stage, mood, species, fried, bone) { spriteBitmap(stage, mood, species, fried, bone) }
    val egg = stage == Stage.EGG

    Canvas(
        modifier.size(size).graphicsLayer {
            // Bounce in whole pixel steps, like a sprite would
            val v = bounce.value
            if (egg) rotationZ = if (v < 0.5f) -4f else 4f
            else translationY = -(v * 2).toInt() * 2.dp.toPx()
        },
    ) {
        val image = sprite.image
        // Scale by how much the pet actually covers (not its bounding box), so a chunky blob and a
        // leggy bird look about as big as each other; then cap so it still fits the box.
        val box = minOf(this.size.width, this.size.height)
        val fit = minOf(box * MASS / sprite.mass, box / maxOf(image.width, image.height))
        val cell = if (fit >= 2f) kotlin.math.floor(fit) else fit // whole screen pixels keep edges crisp
        val w = (image.width * cell).roundToInt()
        val h = (image.height * cell).roundToInt()
        drawImage(
            image,
            dstOffset = IntOffset(((this.size.width - w) / 2).toInt(), (this.size.height - h).toInt()),
            dstSize = IntSize(w, h),
            filterQuality = FilterQuality.None,
        )
    }
}

/** Target side of a pet's "mass" (sqrt of its filled pixels) as a share of the box. */
private const val MASS = 0.78f

private class Sprite(val image: ImageBitmap, val mass: Float)

private val Still: State<Float> = mutableFloatStateOf(0f)

private val spriteCache = HashMap<Long, Sprite>()

/** Bitmaps are tiny (<= 20x20) and there are few combinations, so keep them for the app's lifetime. */
private fun spriteBitmap(stage: Stage, mood: Mood, species: Species, fried: Boolean, bone: Boolean): Sprite {
    val key = if (fried) -1L else ((species.ordinal.toLong() shl 16) or (stage.ordinal.toLong() shl 8) or mood.ordinal.toLong()) or
        (if (bone) 1L shl 40 else 0L)
    return spriteCache.getOrPut(key) {
        val grid = when {
            fried -> PetArt.friedFish()
            bone -> PetArt.SKELETON
            else -> species.model.draw(stage, mood)
        }
        val palette = when {
            fried -> PetArt.FRIED_PALETTE
            bone -> PetArt.BONE_PALETTE
            else -> species.model.palette()
        }
        rasterize(grid, palette)
    }
}

/** Paints only the bounding box of the drawn pixels; mass = sqrt of the filled pixel count. */
private fun rasterize(grid: List<String>, palette: Map<Char, Color>): Sprite {
    val filled = { x: Int, y: Int -> palette[grid[y][x]] != null }
    val ys = grid.indices.filter { y -> grid[y].indices.any { filled(it, y) } }
    val xs = grid[0].indices.filter { x -> grid.indices.any { filled(x, it) } }
    if (xs.isEmpty()) return Sprite(ImageBitmap(1, 1), 1f)
    val x0 = xs.first(); val y0 = ys.first()
    val img = ImageBitmap(xs.last() - x0 + 1, ys.last() - y0 + 1)
    val canvas = androidx.compose.ui.graphics.Canvas(img)
    val paint = Paint()
    var filledCount = 0
    for (y in ys.first()..ys.last()) for (x in x0..xs.last()) {
        paint.color = palette[grid[y][x]] ?: continue
        filledCount++
        canvas.drawRect((x - x0).toFloat(), (y - y0).toFloat(), (x - x0) + 1f, (y - y0) + 1f, paint)
    }
    return Sprite(img, kotlin.math.sqrt(filledCount.toFloat()))
}

/** Body, dark and belly colours, e.g. for tinting UI around a pet. */
fun Species.colors(): Triple<Color, Color, Color> = model.let { Triple(it.body, it.dark, it.belly) }
