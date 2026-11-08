package com.petpal.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.petpal.app.domain.model.*
import com.petpal.app.ui.theme.*

/**
 * Pixel-art pet drawn from a 16x16 grid, so no PNG assets are needed yet.
 * Swap for Image(painterResource(...), filterQuality = FilterQuality.None) once sprites exist.
 */
@Composable
fun PetSprite(stage: Stage, mood: Mood, path: EvolutionPath, modifier: Modifier = Modifier) {
    val size = when (stage) {
        Stage.EGG -> 120.dp
        Stage.BABY -> 128.dp
        Stage.TEEN -> 160.dp
        Stage.ADULT -> 192.dp
    }
    val transition = rememberInfiniteTransition(label = "pet")
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (mood == Mood.SLEEPY) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(if (mood == Mood.HAPPY) 450 else 900), RepeatMode.Reverse),
        label = "bounce",
    )
    val body = when (path) {
        EvolutionPath.NONE -> SpriteBody
        EvolutionPath.ATHLETIC -> SpriteAthletic
        EvolutionPath.SCHOLAR -> SpriteScholar
        EvolutionPath.CHILL -> SpriteChill
    }
    val grid = if (stage == Stage.EGG) EGG else creature(mood, stage)

    Canvas(
        modifier.size(size).graphicsLayer {
            if (stage == Stage.EGG) rotationZ = (bounce - 0.5f) * 8f
            else translationY = -bounce * 6.dp.toPx()
        },
    ) {
        val px = this.size.width / 16f
        grid.forEachIndexed { y, row ->
            row.forEachIndexed { x, c ->
                val color = when (c) {
                    'o', 'e', 'm' -> SpriteInk
                    'b' -> body
                    'l' -> SpriteCream
                    'g' -> SpriteLeaf
                    'c' -> SpriteCheek
                    else -> Color.Transparent
                }
                if (color != Color.Transparent) {
                    // +0.5px overlap hides hairline seams between cells
                    drawRect(color, Offset(x * px, y * px), Size(px + 0.5f, px + 0.5f))
                }
            }
        }
    }
}

private val EGG = listOf(
    "................",
    "......oooo......",
    ".....obbbbo.....",
    "....obbllbbo....",
    "...obbbbbbbbo...",
    "...obllbbbbbo...",
    "..obbbbbbllbbo..",
    "..obbbbbbllbbo..",
    "..obbllbbbbbbo..",
    "..obbllbbbbbbo..",
    "..obbbbbbbbbbo..",
    "...obbbbbbllo...",
    "...obbbbbbbbo...",
    "....obbbbbbo....",
    ".....oooooo.....",
    "................",
)

private val CREATURE = listOf(
    "................",
    "................",
    ".......o........",
    ".....oooooo.....",
    "....obbbbbbo....",
    "...obbbbbbbbo...",
    "..obbbbbbbbbbo..",
    "..obbbbbbbbbbo..",
    "..obbbbbbbbbbo..",
    "..obcbbbbbbcbo..",
    "..obbbbbbbbbbo..",
    "..obbllllllbbo..",
    "...obllllllbo...",
    "....oooooooo....",
    "....ob....bo....",
    "....oo....oo....",
)

private fun creature(mood: Mood, stage: Stage): List<String> {
    val g = CREATURE.map { it.toCharArray() }
    fun set(x: Int, y: Int, c: Char) { g[y][x] = c }

    // Leaf sprout grows with the stage
    set(7, 1, 'g')
    if (stage != Stage.BABY) { set(8, 1, 'g'); set(8, 0, 'g') }
    if (stage == Stage.ADULT) { set(6, 1, 'g'); set(9, 0, 'g') }

    when (mood) {
        Mood.SLEEPY -> listOf(4, 5, 10, 11).forEach { set(it, 8, 'e') }
        else -> listOf(5 to 7, 5 to 8, 10 to 7, 10 to 8).forEach { (x, y) -> set(x, y, 'e') }
    }
    when (mood) {
        Mood.HAPPY -> { set(6, 9, 'm'); set(9, 9, 'm'); set(7, 10, 'm'); set(8, 10, 'm') }
        Mood.OKAY -> { set(7, 10, 'm'); set(8, 10, 'm') }
        Mood.SAD -> { set(7, 9, 'm'); set(8, 9, 'm'); set(6, 10, 'm'); set(9, 10, 'm') }
        Mood.SLEEPY -> set(8, 10, 'm')
    }
    if (mood == Mood.SAD) { set(4, 9, 'b'); set(11, 9, 'b') } // no blush when sad
    return g.map { it.concatToString() }
}
