package com.petpal.app.ui.components.pet

import androidx.compose.ui.graphics.Color
import com.petpal.app.domain.model.Mood
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage
import com.petpal.app.ui.components.pet.PixelGrid.Companion.N
import com.petpal.app.ui.theme.*
import kotlin.math.roundToInt

/** Everything a species hook needs while painting one sprite. */
internal class PetCanvas(
    val g: PixelGrid,
    val stage: Stage,
    val mood: Mood,
    /** Body before species tweaks (turtles tuck theirs under a shell). */
    val b0: Body,
    val b: Body,
) {
    val big = stage >= Stage.TEEN
    val grown = stage != Stage.BABY
    val c = N / 2
    val cy get() = b.cy.toInt()
    /** Pick a value by stage: baby / kid / teen+. */
    fun <T> byStage(baby: T, kid: T, big: T) = if (this.big) big else if (grown) kid else baby

    // Face anchors, filled in by PetModel before the face hooks run.
    var ex = 0
    var ey = 0
    var my = 0
}

enum class EyeStyle { TALL, SHINY, BEADY, SLIT }

/**
 * One species' look. The base class runs a fixed recipe (back parts, ears, body, belly,
 * outline, face, front details) on a 20x20 grid; subclasses fill in the hooks.
 */
internal abstract class PetModel(
    val species: Species,
    val body: Color,
    val dark: Color,
    val belly: Color,
) {
    /** Body width/height scale, so silhouettes differ before features go on. */
    open val build: Pair<Float, Float> = 1f to 1f
    /** Tail sticks out the side, so the body has to stay a bit narrower. */
    open val sideTail = false
    open val eyes = EyeStyle.TALL
    open val blush = true
    open val hasNose = false
    open val hasFeet = true

    /** A fully hand-drawn sprite for this stage, or null to use the recipe. 'E' marks eye pixels. */
    open fun handDrawn(stage: Stage): List<String>? = null

    open fun shapeBody(b0: Body): Body = b0
    open fun PetCanvas.behind() {}
    open fun PetCanvas.onTop() {}
    open fun PetCanvas.afterBody() {}
    open fun PetCanvas.feet() {
        val fx = (b.cx - b.rx * 0.5f).toInt()
        g.rect(fx - 1, b.bottom - 1, fx + 1, b.bottom, 'b')
        g.rect(N - 1 - fx - 1, b.bottom - 1, N - 1 - fx + 1, b.bottom, 'b')
    }
    /** Extra pale markings painted over the body, before outlining. */
    open fun PetCanvas.markings() {}
    /** Paint after the outline pass (masks, patches). */
    open fun PetCanvas.afterOutline() {}
    open fun PetCanvas.eyeY(default: Int) = default
    open fun PetCanvas.eyeX() = (b.cx - b.rx * 0.4f).toInt()
    open fun PetCanvas.snout() {}
    open fun PetCanvas.mouth() {
        snout()
        if (hasNose) { g.set(c - 1, my - 1, 'e'); g.set(c, my - 1, 'e') }
        when (mood) {
            Mood.HAPPY -> { g.set(c - 2, my, 'm'); g.set(c + 1, my, 'm'); g.set(c - 1, my + 1, 'm'); g.set(c, my + 1, 'm') }
            Mood.OKAY -> { g.set(c - 1, my + 1, 'm'); g.set(c, my + 1, 'm') }
            Mood.SAD -> { g.set(c - 1, my, 'm'); g.set(c, my, 'm'); g.set(c - 2, my + 1, 'm'); g.set(c + 1, my + 1, 'm') }
            Mood.SLEEPY -> g.set(c, my + 1, 'm')
        }
    }
    open fun PetCanvas.front() {}

    /** Shared beak for birds; [hooked] adds a curved tip. */
    protected fun PetCanvas.beak(hooked: Boolean) {
        g.set(c - 1, my - 1, 'y'); g.set(c, my - 1, 'y'); g.set(c - 1, my, 'y'); g.set(c, my, 'y')
        if (hooked) { g.set(c - 1, my + 1, 'y'); g.set(c, my + 1, 'e') }
    }

    fun palette(): Map<Char, Color> = mapOf(
        'o' to SpriteInk, 'e' to SpriteInk, 'm' to SpriteInk,
        'b' to body, 'd' to dark, 'l' to belly,
        'c' to SpriteCheek, 'p' to SpritePink, 'y' to SpriteYellow,
        'h' to SpriteCream, 'g' to SpriteLeaf, 'r' to SpriteRed, 'w' to Color.White,
    )

    fun draw(stage: Stage, mood: Mood): List<String> {
        if (stage == Stage.EGG) return PetArt.egg(species)
        handDrawn(stage)?.let { return PetArt.withMood(it, mood) }

        val g = PixelGrid()
        val base = bodyFor(stage)
        val (wx, hy) = build.let { (w, h) -> // babies are rounder, the shape shows more as they grow
            if (stage == Stage.BABY) (1 + (w - 1) * 0.5f) to (1 + (h - 1) * 0.5f) else w to h
        }
        val ry = base.ry * hy
        // Feet stay on one ground line; side-tailed species stay narrow enough to fit the grid
        val b0 = base.copy(
            cy = (base.cy + base.ry - ry).coerceAtMost(18.4f - ry),
            rx = minOf(base.rx * wx, if (sideTail) 7f else 7.8f),
            ry = ry,
        )
        val p = PetCanvas(g, stage, mood, b0, shapeBody(b0))
        val b = p.b

        p.behind()
        p.onTop() // drawn before the body so the body covers their base
        g.ellipse(b.cx, b.cy, b.rx, b.ry, 'b')
        p.afterBody()
        if (p.grown && hasFeet) p.feet()
        g.ellipse(b.cx, b.cy + b.ry * 0.45f, b.rx * 0.55f, b.ry * 0.45f, 'l', onlyOver = 'b')
        p.markings()
        g.outline()
        p.afterOutline()

        // Face
        p.ey = p.eyeY((b.cy - b.ry * 0.15f).roundToInt())
        p.ex = p.eyeX()
        p.my = (b.cy + b.ry * 0.25f).roundToInt()
        p.paintEyes()
        p.mouth()
        if (blush && mood != Mood.SAD) g.mirror(p.ex - 1, p.ey + 2, 'c')
        p.front()
        return g.rows()
    }

    private fun PetCanvas.paintEyes() {
        if (mood == Mood.SLEEPY) {
            g.mirror(ex - 1, ey, 'e'); g.mirror(ex, ey, 'e')
            return
        }
        when (eyes) {
            EyeStyle.SHINY -> {
                g.mirror(ex - 1, ey, 'e'); g.mirror(ex, ey, 'e'); g.mirror(ex - 1, ey - 1, 'e'); g.mirror(ex, ey - 1, 'w')
                if (big) { g.mirror(ex - 1, ey + 1, 'e'); g.mirror(ex, ey + 1, 'e') }
            }
            EyeStyle.BEADY -> { g.mirror(ex, ey, 'e'); if (big) g.mirror(ex, ey - 1, 'e') }
            EyeStyle.SLIT -> {
                g.mirror(ex - 1, ey, 'e'); g.mirror(ex, ey, 'e')
                if (big) g.mirror(ex - 1, ey - 1, 'd')
            }
            EyeStyle.TALL -> {
                g.mirror(ex, ey, 'e'); g.mirror(ex, ey - 1, 'e')
                if (big || wideEyes) { g.mirror(ex - 1, ey, 'e'); g.mirror(ex - 1, ey - 1, 'e') }
                if (wideEyes) g.mirror(ex, ey - 1, 'w')
            }
        }
    }

    /** Owls: always-wide eyes with a shine. */
    open val wideEyes = false

    private fun bodyFor(stage: Stage) = when (stage) {
        Stage.BABY -> Body(cy = 12.5f, rx = 5f, ry = 4.6f)
        Stage.KID -> Body(cy = 11.5f, rx = 6f, ry = 5.4f)
        Stage.TEEN -> Body(cy = 11.2f, rx = 6.5f, ry = 5.8f)
        else -> Body(cy = 11f, rx = 7f, ry = 6.2f)
    }
}
