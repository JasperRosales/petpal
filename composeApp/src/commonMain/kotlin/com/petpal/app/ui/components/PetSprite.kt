package com.petpal.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    val bounce = if (mood == Mood.SLEEPY) 0f else {
        val v by rememberInfiniteTransition(label = "pet").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(if (mood == Mood.HAPPY) 450 else 900), RepeatMode.Reverse),
            label = "bounce",
        )
        v
    }
    val image = remember(stage, mood, species, fried, bone) { spriteBitmap(stage, mood, species, fried, bone) }
    val egg = stage == Stage.EGG

    Canvas(
        modifier.size(size).graphicsLayer {
            // Bounce in whole pixel steps, like a sprite would
            if (egg) rotationZ = if (bounce < 0.5f) -4f else 4f
            else translationY = -(bounce * 2).toInt() * 2.dp.toPx()
        },
    ) {
        val cell = maxOf(1, (minOf(this.size.width, this.size.height) / maxOf(image.width, image.height)).toInt())
        val w = image.width * cell
        val h = image.height * cell
        drawImage(
            image,
            dstOffset = IntOffset(((this.size.width - w) / 2).toInt(), (this.size.height - h).toInt()),
            dstSize = IntSize(w, h),
            filterQuality = FilterQuality.None,
        )
    }
}

private val spriteCache = HashMap<Long, ImageBitmap>()

/** Bitmaps are tiny (<= 20x20) and there are few combinations, so keep them for the app's lifetime. */
private fun spriteBitmap(stage: Stage, mood: Mood, species: Species, fried: Boolean, bone: Boolean): ImageBitmap {
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

/** Paints only the bounding box of the drawn pixels, so every pet scales to the same size. */
private fun rasterize(grid: List<String>, palette: Map<Char, Color>): ImageBitmap {
    val filled = { x: Int, y: Int -> palette[grid[y][x]] != null }
    val ys = grid.indices.filter { y -> grid[y].indices.any { filled(it, y) } }
    val xs = grid[0].indices.filter { x -> grid.indices.any { filled(x, it) } }
    if (xs.isEmpty()) return ImageBitmap(1, 1)
    val x0 = xs.first(); val y0 = ys.first()
    val img = ImageBitmap(xs.last() - x0 + 1, ys.last() - y0 + 1)
    val canvas = androidx.compose.ui.graphics.Canvas(img)
    val paint = Paint()
    for (y in ys.first()..ys.last()) for (x in x0..xs.last()) {
        paint.color = palette[grid[y][x]] ?: continue
        canvas.drawRect((x - x0).toFloat(), (y - y0).toFloat(), (x - x0) + 1f, (y - y0) + 1f, paint)
    }
    return img
}

/** Body, dark and belly colours, e.g. for tinting UI around a pet. */
fun Species.colors(): Triple<Color, Color, Color> = model.let { Triple(it.body, it.dark, it.belly) }
